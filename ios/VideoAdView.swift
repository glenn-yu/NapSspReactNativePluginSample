import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

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
  @objc var onAdImpression: RCTBubblingEventBlock?
  @objc var onAdCompleted: RCTBubblingEventBlock?
  @objc var onAdSkipped: RCTBubblingEventBlock?

  private let containerView = UIView()
  private let titleLabel = UILabel()
  private let detailLabel = UILabel()
  private var isLoaded = false
  private var hasPresentedClick = false

  #if canImport(AdMixerMediation)
  private var videoAdView: AMMVideoView?
  private var sdkDelegate: NapSspVideoDelegate?
  #endif

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupView()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    setupView()
  }

  override func willMove(toWindow newWindow: UIWindow?) {
    super.willMove(toWindow: newWindow)
    if newWindow == nil {
      #if canImport(AdMixerMediation)
      videoAdView?.stop()
      videoAdView = nil
      sdkDelegate = nil
      #endif
    }
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
    
    #if canImport(AdMixerMediation)
    videoAdView?.frame = bounds
    #endif
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
    
    #if canImport(AdMixerMediation)
    loadWithSdk(adUnitId: currentAdUnitId)
    #else
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
      guard let self = self else { return }
      self.isLoaded = true
      self.onAdLoaded?(self.eventPayload(adUnitId: currentAdUnitId, source: "placeholder", message: "Video ad loaded"))
      self.onAdImpression?(self.eventPayload(adUnitId: currentAdUnitId, source: "placeholder", message: "Video ad impression"))
    }
    #endif
  }

  #if canImport(AdMixerMediation)
  private func loadWithSdk(adUnitId: String) {
    guard let rootVC = NapSspRuntime.activeRootViewController() else { return }
    guard let numericAdUnitId = Int(adUnitId) else {
      emitSdkFailure(adUnitId: adUnitId, code: "napssp_invalid_ad_unit", message: "Video adUnitId must be numeric on iOS.")
      return
    }

    videoAdView?.stop()
    videoAdView?.removeFromSuperview()
    videoAdView = nil
    
    let delegate = NapSspVideoDelegate(view: self, adUnitId: adUnitId)
    sdkDelegate = delegate

    let view = AMMVideoView(rootViewController: rootVC)
    view.adUnitID = numericAdUnitId
    view.delegate = delegate
    
    videoAdView = view
    addSubview(view)
    view.load()
  }

  func attachSdkView() {
    containerView.isHidden = true
    if let view = videoAdView {
      view.frame = bounds
      view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
      if view.superview == nil {
        addSubview(view)
      }
      setNeedsLayout()
      layoutIfNeeded()
    }
    isLoaded = true
  }

  func emitSdkFailure(adUnitId: String, code: String, message: String) {
    onAdFailedToLoad?([
      "adUnitId": adUnitId,
      "format": "video",
      "code": code,
      "message": message
    ])
  }
  #endif

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

  func eventPayload(adUnitId: String, source: String, message: String) -> [String: Any] {
    return [
      "adUnitId": adUnitId,
      "format": "video",
      "source": source,
      "message": message
    ]
  }
}

#if canImport(AdMixerMediation)
private final class NapSspVideoDelegate: NSObject, AMMVideoViewDelegate {
  private weak var videoView: VideoAdView?
  private let adUnitId: String

  init(view: VideoAdView, adUnitId: String) {
    self.videoView = view
    self.adUnitId = adUnitId
  }

  func onSuccessVideo() {
    guard let view = videoView else { return }
    view.attachSdkView()
    view.onAdLoaded?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video loaded"))
    view.onAdImpression?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video impression"))
  }

  func onFailVideo() {
    guard let view = videoView else { return }
    view.emitSdkFailure(adUnitId: adUnitId, code: "napssp_video_load_failed", message: "unknown")
  }

  func onTapVideoViewMore() {
    guard let view = videoView else { return }
    view.onAdClicked?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video tapped"))
    view.onAdOpened?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video opened"))
  }
  
  func onCompleteVideo() {
    guard let view = videoView else { return }
    view.onAdCompleted?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video completed"))
  }

  func onSkipVideo() {
    guard let view = videoView else { return }
    view.onAdSkipped?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Video skipped"))
  }
}
#endif