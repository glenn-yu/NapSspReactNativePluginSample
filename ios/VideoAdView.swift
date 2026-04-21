import Foundation
import UIKit
import React

@objc(NapSspVideoAdViewImpl)
final class VideoAdView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet { reloadIfNeeded() }
  }
  @objc dynamic var isRetry: Bool = false

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?
  @objc var onAdCompleted: RCTBubblingEventBlock?
  @objc var onAdSkipped: RCTBubblingEventBlock?

  private let containerView = UIView()
  private let titleLabel = UILabel()
  private let detailLabel = UILabel()
  private var isLoaded = false
  private var hasPresentedClick = false

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupView()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    setupView()
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()
    if window == nil { return }
    reloadIfNeeded()
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    containerView.frame = bounds
    titleLabel.frame = CGRect(x: 16, y: bounds.midY - 24, width: bounds.width - 32, height: 24)
    detailLabel.frame = CGRect(x: 16, y: bounds.midY + 4, width: bounds.width - 32, height: 18)
    containerView.layer.cornerRadius = 8
  }

  private func setupView() {
    backgroundColor = .clear
    isUserInteractionEnabled = true

    containerView.backgroundColor = UIColor(red: 255/255, green: 235/255, blue: 238/255, alpha: 1.0)
    containerView.layer.borderColor = UIColor(red: 239/255, green: 154/255, blue: 154/255, alpha: 1.0).cgColor
    containerView.layer.borderWidth = 1
    addSubview(containerView)

    titleLabel.font = .systemFont(ofSize: 18, weight: .bold)
    titleLabel.textColor = UIColor(red: 198/255, green: 40/255, blue: 40/255, alpha: 1.0)
    titleLabel.text = "NapSsp Video Ad"
    titleLabel.textAlignment = .center
    containerView.addSubview(titleLabel)

    detailLabel.font = .systemFont(ofSize: 14)
    detailLabel.textColor = UIColor(red: 211/255, green: 47/255, blue: 47/255, alpha: 1.0)
    detailLabel.textAlignment = .center
    detailLabel.text = "adUnitId: <unset>"
    containerView.addSubview(detailLabel)

    let recognizer = UITapGestureRecognizer(target: self, action: #selector(handleTap))
    addGestureRecognizer(recognizer)
  }

  private func reloadIfNeeded() {
    let currentAdUnitId = adUnitId as String
    if currentAdUnitId.isEmpty { return }
    
    detailLabel.text = "adUnitId: \(currentAdUnitId)"
    
    if isLoaded { return }
    
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
      guard let self = self else { return }
      self.isLoaded = true
      self.onAdLoaded?(self.eventPayload(adUnitId: currentAdUnitId, source: "placeholder", message: "Video ad loaded"))
    }
  }

  @objc private func handleTap() {
    guard isLoaded else { return }
    let payload = eventPayload(adUnitId: adUnitId as String, source: "placeholder", message: "Video ad tapped")
    onAdClicked?(payload)

    if !hasPresentedClick {
      hasPresentedClick = true
      onAdOpened?(payload)
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
        guard let self else { return }
        self.onAdClosed?(self.eventPayload(adUnitId: self.adUnitId as String, source: "placeholder", message: "Video ad dismissed"))
      }
    }
  }

  private func eventPayload(adUnitId: String, source: String, message: String) -> [String: Any] {
    [
      "adUnitId": adUnitId,
      "format": "video",
      "source": source,
      "message": message,
    ]
  }
}
