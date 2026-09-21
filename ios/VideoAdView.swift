import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// React Native host for `AMMVideoView` (inline video).
/// https://napmx.github.io/#/ios/native/video
@objc(VideoAdView)
final class VideoAdView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet {
      guard adUnitId != oldValue else { return }
      reload()
    }
  }

  /// Retry the waterfall once when the first pass returns no fill.
  @objc var isRetry: Bool = false

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?
  @objc var onAdImpression: RCTBubblingEventBlock?
  @objc var onAdCompleted: RCTBubblingEventBlock?
  @objc var onAdSkipped: RCTBubblingEventBlock?

  private var isLoading = false
  private var isLoaded = false
  private var hasRetried = false
  private var didRegisterInitializeObserver = false

  #if canImport(AdMixerMediation)
  private var videoView: AMMVideoView?
  private var sdkDelegate: NapSspVideoDelegate?
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
    videoView?.frame = bounds
    #endif
  }

  @objc func reload() {
    hasRetried = false
    release()
    loadIfNeeded()
  }

  @objc func destroyVideoAd() {
    release()
  }

  // MARK: - internals

  private func loadIfNeeded() {
    guard window != nil, !isLoading, !isLoaded else { return }

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
      emitFailure(adUnitId: unit, code: "napssp_invalid_ad_unit", message: "Video adUnitId must be numeric on iOS.")
      return
    }

    isLoading = true
    let delegate = NapSspVideoDelegate(view: self, adUnitId: unit)
    sdkDelegate = delegate

    AMMVideoView.loadAd(adUnitID: numericAdUnitId, rootViewController: rootVC) { [weak self] video, adapterType, error in
      guard let self else { return }
      self.isLoading = false

      if let error {
        if self.isRetry, !self.hasRetried, NapSspSdkErrorCode.from(error) == .loadFailed {
          self.hasRetried = true
          self.loadIfNeeded()
          return
        }
        self.emitFailure(adUnitId: unit, error: error)
        return
      }
      guard let video else {
        self.emitFailure(adUnitId: unit, code: "napssp_empty_ad", message: "The SDK returned no video ad and no error.")
        return
      }

      video.delegate = delegate
      self.videoView = video
      video.frame = self.bounds
      video.autoresizingMask = [.flexibleWidth, .flexibleHeight]
      self.addSubview(video)
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
    videoView?.stop()
    videoView?.removeFromSuperview()
    videoView = nil
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
    onAdFailedToLoad?(napSspErrorPayload(adUnitId: adUnitId, format: "video", error: error))
  }

  fileprivate func emitFailure(adUnitId: String, code: String, message: String) {
    isLoaded = false
    onAdFailedToLoad?(["adUnitId": adUnitId, "format": "video", "code": code, "message": message])
  }

  fileprivate func eventPayload(adUnitId: String, extra: [String: Any] = [:]) -> [String: Any] {
    var payload: [String: Any] = ["adUnitId": adUnitId, "format": "video"]
    extra.forEach { payload[$0.key] = $0.value }
    return payload
  }

  fileprivate func emitImpression(adUnitId: String) { onAdImpression?(eventPayload(adUnitId: adUnitId)) }
  fileprivate func emitClick(adUnitId: String) { onAdClicked?(eventPayload(adUnitId: adUnitId)) }
  fileprivate func emitCompleted(adUnitId: String) { onAdCompleted?(eventPayload(adUnitId: adUnitId)) }
  fileprivate func emitSkipped(adUnitId: String) { onAdSkipped?(eventPayload(adUnitId: adUnitId)) }
}

#if canImport(AdMixerMediation)
private final class NapSspVideoDelegate: NSObject, AMMVideoViewDelegate {
  private weak var host: VideoAdView?
  private let adUnitId: String

  init(view: VideoAdView, adUnitId: String) {
    self.host = view
    self.adUnitId = adUnitId
  }

  func onSuccessShowVideo() {
    host?.emitImpression(adUnitId: adUnitId)
  }

  func onClickVideo() {
    host?.emitClick(adUnitId: adUnitId)
  }

  func onCompleteVideo() {
    host?.emitCompleted(adUnitId: adUnitId)
  }

  func onSkipVideo() {
    host?.emitSkipped(adUnitId: adUnitId)
  }
}
#endif
