import Foundation
import UIKit
import React

@objc(NapSspNativeAdView)
final class NativeAdView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet {
      reloadIfNeeded()
    }
  }

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?

  private let containerView = UIView()
  private let titleLabel = UILabel()
  private let detailLabel = UILabel()
  private let badgeLabel = UILabel()
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

    let inset: CGFloat = 16
    badgeLabel.sizeToFit()
    badgeLabel.frame = CGRect(
      x: inset,
      y: inset,
      width: min(bounds.width - inset * 2, badgeLabel.bounds.width),
      height: badgeLabel.bounds.height
    )

    titleLabel.frame = CGRect(x: inset, y: badgeLabel.frame.maxY + 12, width: bounds.width - inset * 2, height: 24)
    detailLabel.frame = CGRect(x: inset, y: titleLabel.frame.maxY + 8, width: bounds.width - inset * 2, height: 18)
    
    containerView.layer.cornerRadius = 8
  }

  private func setupView() {
    backgroundColor = .clear
    isUserInteractionEnabled = true

    // Match Android Native Ad light green styling
    containerView.backgroundColor = UIColor(red: 232/255, green: 245/255, blue: 233/255, alpha: 1.0)
    containerView.layer.borderColor = UIColor(red: 165/255, green: 214/255, blue: 167/255, alpha: 1.0).cgColor
    containerView.layer.borderWidth = 1
    addSubview(containerView)

    badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
    badgeLabel.textColor = UIColor(red: 56/255, green: 142/255, blue: 60/255, alpha: 1.0)
    badgeLabel.text = "NapSsp iOS"
    containerView.addSubview(badgeLabel)

    titleLabel.font = .systemFont(ofSize: 18, weight: .bold)
    titleLabel.textColor = UIColor(red: 46/255, green: 125/255, blue: 50/255, alpha: 1.0)
    titleLabel.text = "Native Ad placeholder ready"
    containerView.addSubview(titleLabel)

    detailLabel.font = .systemFont(ofSize: 14)
    detailLabel.textColor = UIColor(red: 76/255, green: 175/255, blue: 80/255, alpha: 1.0)
    detailLabel.numberOfLines = 1
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
    
    // Mock loading delay
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
      guard let self = self else { return }
      self.isLoaded = true
      self.onAdLoaded?(self.eventPayload(adUnitId: currentAdUnitId, source: "placeholder", message: "Native ad loaded"))
    }
  }

  @objc private func handleTap() {
    guard isLoaded else { return }
    let payload = eventPayload(adUnitId: adUnitId as String, source: "placeholder", message: "Native ad tapped")
    onAdClicked?(payload)

    if !hasPresentedClick {
      hasPresentedClick = true
      onAdOpened?(payload)
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
        guard let self else { return }
        self.onAdClosed?(self.eventPayload(adUnitId: self.adUnitId as String, source: "placeholder", message: "Native ad dismissed"))
      }
    }
  }

  private func eventPayload(adUnitId: String, source: String, message: String) -> [String: Any] {
    [
      "adUnitId": adUnitId,
      "format": "native",
      "source": source,
      "message": message,
    ]
  }
}
