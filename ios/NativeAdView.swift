import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

@objc(NapSspNativeAdViewImpl)
final class NativeAdView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet { reloadIfNeeded() }
  }

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?
  @objc var onAdImpression: RCTBubblingEventBlock?

  #if canImport(AdMixerMediation)
  private var nativeAdContainer: AMMNativeAdViewContainer?
  private var sdkDelegate: NapSspNativeDelegate?
  #endif

  // Placeholder views
  private let containerView = UIView()
  private let titleLabel = UILabel()
  private let detailLabel = UILabel()
  private let badgeLabel = UILabel()
  private var isLoaded = false

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupPlaceholderView()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    setupPlaceholderView()
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
    badgeLabel.frame = CGRect(x: inset, y: inset, width: min(bounds.width - inset * 2, badgeLabel.bounds.width), height: badgeLabel.bounds.height)
    titleLabel.frame = CGRect(x: inset, y: badgeLabel.frame.maxY + 12, width: bounds.width - inset * 2, height: 24)
    detailLabel.frame = CGRect(x: inset, y: titleLabel.frame.maxY + 8, width: bounds.width - inset * 2, height: 18)
    containerView.layer.cornerRadius = 8
  }

  private func setupPlaceholderView() {
    backgroundColor = .clear
    isUserInteractionEnabled = true

    containerView.backgroundColor = UIColor(red: 232/255, green: 245/255, blue: 233/255, alpha: 1.0)
    containerView.layer.borderColor = UIColor(red: 165/255, green: 214/255, blue: 167/255, alpha: 1.0).cgColor
    containerView.layer.borderWidth = 1
    addSubview(containerView)

    badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
    badgeLabel.textColor = UIColor(red: 56/255, green: 142/255, blue: 60/255, alpha: 1.0)
    badgeLabel.text = "NapSsp Native Ad"
    containerView.addSubview(badgeLabel)

    titleLabel.font = .systemFont(ofSize: 18, weight: .bold)
    titleLabel.textColor = UIColor(red: 46/255, green: 125/255, blue: 50/255, alpha: 1.0)
    titleLabel.text = "Native Ad placeholder ready"
    containerView.addSubview(titleLabel)

    detailLabel.font = .systemFont(ofSize: 14)
    detailLabel.textColor = UIColor(red: 76/255, green: 175/255, blue: 80/255, alpha: 1.0)
    detailLabel.text = "adUnitId: <unset>"
    containerView.addSubview(detailLabel)

    addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleTap)))
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
      self.onAdLoaded?(self.eventPayload(adUnitId: currentAdUnitId, message: "Native ad loaded (placeholder)"))
      self.onAdImpression?(self.eventPayload(adUnitId: currentAdUnitId, message: "Native ad impression"))
    }
    #endif
  }

  #if canImport(AdMixerMediation)
  private func loadWithSdk(adUnitId: String) {
    guard let rootVC = NapSspRuntime.activeRootViewController() else { return }

    // v2.2.1: remove existing container before loading new one
    nativeAdContainer?.stop()
    nativeAdContainer = nil

    let delegate = NapSspNativeDelegate(view: self, adUnitId: adUnitId)
    sdkDelegate = delegate

    let container = AMMNativeAdViewContainer(rootViewController: rootVC)
    container.adUnitID = adUnitId
    container.delegate = delegate
    nativeAdContainer = container

    // Attempt to load xib-based view from SDK bundle, fall back to programmatic
    if let nibView = Bundle.main.loadNibNamed("AMMNativeAdView", owner: nil, options: nil)?.first as? AMMNativeAdView {
      container.nativeAdView = nibView
    }

    container.load()
  }

  func attachSdkView(_ sdkView: UIView) {
    subviews.forEach { $0.removeFromSuperview() }
    sdkView.frame = bounds
    sdkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    addSubview(sdkView)
    isLoaded = true
  }
  #endif

  @objc private func handleTap() {
    guard isLoaded else { return }
    let adUnitId = adUnitId as String
    onAdClicked?(eventPayload(adUnitId: adUnitId, message: "Native ad tapped"))
    onAdOpened?(eventPayload(adUnitId: adUnitId, message: "Native ad opened"))
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
      guard let self else { return }
      self.onAdClosed?(self.eventPayload(adUnitId: adUnitId, message: "Native ad dismissed"))
    }
  }

  private func eventPayload(adUnitId: String, message: String) -> [String: Any] {
    ["adUnitId": adUnitId, "format": "native", "message": message]
  }
}

#if canImport(AdMixerMediation)
private final class NapSspNativeDelegate: NSObject, AMMNativeDelegate {
  private weak var nativeAdView: NativeAdView?
  private let adUnitId: String

  init(view: NativeAdView, adUnitId: String) {
    self.nativeAdView = view
    self.adUnitId = adUnitId
  }

  func onSuccessNative() {
    guard let view = nativeAdView else { return }
    if let container = view.nativeAdContainer, let adView = container.nativeAdView {
      view.attachSdkView(adView)
    }
    view.onAdLoaded?(["adUnitId": adUnitId, "format": "native", "message": "Native ad loaded"])
    view.onAdImpression?(["adUnitId": adUnitId, "format": "native", "message": "Native ad impression"])
  }

  func onFailNative() {
    nativeAdView?.onAdFailedToLoad?([
      "adUnitId": adUnitId, "format": "native",
      "code": "napssp_native_load_failed", "message": "Native ad failed to load"
    ])
  }

  func onTapNative() {
    nativeAdView?.onAdClicked?(["adUnitId": adUnitId, "format": "native", "message": "Native ad tapped"])
  }
}
#endif
