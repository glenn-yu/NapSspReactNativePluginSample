import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// React Native host for `AMMBannerView`.
///
/// The creative size is decided by the ad unit's server configuration; `size` only seeds the
/// intrinsic content size so the row does not collapse before the first fill.
/// https://napmx.github.io/#/ios/native/banner
@objc(BannerView)
final class BannerView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet {
      guard adUnitId != oldValue else { return }
      reload()
    }
  }

  @objc dynamic var size: NSString = "BANNER_320x50" {
    didSet {
      guard size != oldValue else { return }
      invalidateIntrinsicContentSize()
      setNeedsLayout()
    }
  }

  @objc var autoLoad: Bool = true

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?
  @objc var onAdImpression: RCTBubblingEventBlock?

  private var isLoading = false
  private var isLoaded = false
  private var didRegisterInitializeObserver = false

  #if canImport(AdMixerMediation)
  private var bannerView: AMMBannerView?
  private var sdkDelegate: NapSspBannerDelegate?
  #endif

  override init(frame: CGRect) {
    super.init(frame: frame)
    backgroundColor = .clear
    registerForInitializationNotifications()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    backgroundColor = .clear
    registerForInitializationNotifications()
  }

  deinit {
    if didRegisterInitializeObserver {
      NotificationCenter.default.removeObserver(self, name: .napSspDidInitialize, object: nil)
    }
  }

  override var intrinsicContentSize: CGSize {
    let bannerSize = NapSspBannerSize.parse(size as String)
    return CGSize(width: bannerSize.width, height: bannerSize.height)
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()
    if window == nil {
      release()
    } else {
      loadIfNeeded()
    }
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    #if canImport(AdMixerMediation)
    bannerView?.frame = bounds
    #endif
  }

  /// Destroys the current ad and requests a fresh one.
  @objc func reload() {
    release()
    loadIfNeeded()
  }

  @objc func destroyBanner() {
    release()
  }

  // MARK: - internals

  private func loadIfNeeded() {
    guard autoLoad, window != nil, !isLoading, !isLoaded else { return }

    let unit = (adUnitId as String).trimmingCharacters(in: .whitespacesAndNewlines)
    guard !unit.isEmpty else { return }

    guard NapSspRuntime.shared.isInitialized else {
      emitFailure(adUnitId: unit, code: NapSspError.notInitialized.errorCode, message: NapSspError.notInitialized.errorDescription ?? "")
      return
    }

    #if canImport(AdMixerMediation)
    guard let rootVC = NapSspRuntime.activeRootViewController() else {
      emitFailure(adUnitId: unit, code: "napssp_no_view_controller", message: "No root view controller is available yet.")
      return
    }
    guard let numericAdUnitId = Int(unit) else {
      emitFailure(adUnitId: unit, code: "napssp_invalid_ad_unit", message: "Banner adUnitId must be numeric on iOS.")
      return
    }

    isLoading = true
    let delegate = NapSspBannerDelegate(view: self, adUnitId: unit)
    sdkDelegate = delegate

    AMMBannerView.loadAd(adUnitID: numericAdUnitId, rootViewController: rootVC) { [weak self] banner, adapterType, error in
      guard let self else { return }
      self.isLoading = false

      if let error {
        self.emitFailure(adUnitId: unit, error: error)
        return
      }
      guard let banner else {
        self.emitFailure(adUnitId: unit, code: "napssp_empty_ad", message: "The SDK returned no banner and no error.")
        return
      }

      banner.delegate = delegate
      self.bannerView = banner
      banner.frame = self.bounds
      banner.autoresizingMask = [.flexibleWidth, .flexibleHeight]
      self.addSubview(banner)
      self.isLoaded = true
      self.setNeedsLayout()

      self.onAdLoaded?(self.eventPayload(adUnitId: unit, extra: ["network": adapterType.adapterName]))
    }
    #else
    emitFailure(
      adUnitId: unit,
      code: "napssp_sdk_not_linked",
      message: "AdMixerMediation is not linked. Run `pod install` (or add the Swift package) and rebuild."
    )
    #endif
  }

  private func release() {
    #if canImport(AdMixerMediation)
    bannerView?.stop()
    bannerView?.removeFromSuperview()
    bannerView = nil
    sdkDelegate = nil
    #endif
    isLoading = false
    isLoaded = false
  }

  private func registerForInitializationNotifications() {
    guard !didRegisterInitializeObserver else { return }
    didRegisterInitializeObserver = true
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleRuntimeInitialized),
      name: .napSspDidInitialize,
      object: nil
    )
  }

  @objc private func handleRuntimeInitialized() {
    DispatchQueue.main.async { [weak self] in self?.loadIfNeeded() }
  }

  fileprivate func emitFailure(adUnitId: String, error: Error) {
    isLoaded = false
    onAdFailedToLoad?(napSspErrorPayload(adUnitId: adUnitId, format: "banner", error: error))
  }

  fileprivate func emitFailure(adUnitId: String, code: String, message: String) {
    isLoaded = false
    onAdFailedToLoad?([
      "adUnitId": adUnitId,
      "format": "banner",
      "size": size as String,
      "code": code,
      "message": message,
    ])
  }

  fileprivate func eventPayload(adUnitId: String, extra: [String: Any] = [:]) -> [String: Any] {
    var payload: [String: Any] = [
      "adUnitId": adUnitId,
      "format": "banner",
      "size": size as String,
    ]
    extra.forEach { payload[$0.key] = $0.value }
    return payload
  }

  fileprivate func emitImpression(adUnitId: String) {
    onAdImpression?(eventPayload(adUnitId: adUnitId))
  }

  fileprivate func emitClick(adUnitId: String) {
    onAdClicked?(eventPayload(adUnitId: adUnitId))
  }
}

#if canImport(AdMixerMediation)
private final class NapSspBannerDelegate: NSObject, AMMBannerViewDelegate {
  private weak var bannerHost: BannerView?
  private let adUnitId: String

  init(view: BannerView, adUnitId: String) {
    self.bannerHost = view
    self.adUnitId = adUnitId
  }

  func onSuccessShowBanner() {
    bannerHost?.emitImpression(adUnitId: adUnitId)
  }

  func onClickBanner() {
    bannerHost?.emitClick(adUnitId: adUnitId)
  }
}
#endif
