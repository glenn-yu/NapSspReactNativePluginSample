import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// React Native host for `AMMNativeAdViewContainer`.
///
/// The creative is rendered into `AMMNativeAdView.xib` when the app ships one, otherwise into a
/// programmatic fallback layout with the same outlets.
/// https://napmx.github.io/#/ios/native/native-ad
@objc(NativeAdView)
final class NativeAdView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet {
      guard adUnitId != oldValue else { return }
      reload()
    }
  }

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
  fileprivate var nativeAdContainer: AMMNativeAdViewContainer?
  private var sdkDelegate: NapSspNativeDelegate?
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

  @objc func reload() {
    release()
    loadIfNeeded()
  }

  @objc func destroyNativeAd() {
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
      emitFailure(adUnitId: unit, code: "napssp_invalid_ad_unit", message: "Native adUnitId must be numeric on iOS.")
      return
    }

    isLoading = true
    let delegate = NapSspNativeDelegate(view: self, adUnitId: unit)
    sdkDelegate = delegate

    let template = loadNativeAdViewFromNib() ?? makeProgrammaticNativeAdView()
    template.viewController = rootVC

    AMMNativeAdViewContainer.loadAd(
      adUnitID: numericAdUnitId,
      rootViewController: rootVC,
      nativeAdView: template
    ) { [weak self] container, adapterType, error in
      guard let self else { return }
      self.isLoading = false

      if let error {
        self.emitFailure(adUnitId: unit, error: error)
        return
      }
      guard let container else {
        self.emitFailure(adUnitId: unit, code: "napssp_empty_ad", message: "The SDK returned no native ad and no error.")
        return
      }

      container.delegate = delegate
      self.nativeAdContainer = container
      self.attach(container)
      self.isLoaded = true

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

  #if canImport(AdMixerMediation)
  private func attach(_ container: AMMNativeAdViewContainer) {
    subviews.forEach { $0.removeFromSuperview() }
    container.frame = bounds
    container.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    addSubview(container)
    setNeedsLayout()
  }

  private func loadNativeAdViewFromNib() -> AMMNativeAdView? {
    let nibName = "AMMNativeAdView"
    let candidates: [Bundle] = [Bundle.main, Bundle(for: NativeAdView.self)] + Bundle.allFrameworks

    for bundle in candidates {
      guard bundle.path(forResource: nibName, ofType: "nib") != nil
        || bundle.path(forResource: nibName, ofType: "xib") != nil
      else { continue }
      if let view = bundle.loadNibNamed(nibName, owner: nil, options: nil)?
        .compactMap({ $0 as? AMMNativeAdView }).first
      {
        return view
      }
    }
    return nil
  }

  private func makeProgrammaticNativeAdView() -> AMMNativeAdView {
    let nativeAdView = AMMNativeAdView()
    nativeAdView.backgroundColor = .secondarySystemBackground

    let iconView = UIImageView()
    iconView.contentMode = .scaleAspectFit

    let headlineLabel = UILabel()
    headlineLabel.font = .systemFont(ofSize: 16, weight: .semibold)
    headlineLabel.numberOfLines = 2

    let advertiserLabel = UILabel()
    advertiserLabel.font = .systemFont(ofSize: 12)
    advertiserLabel.textColor = .secondaryLabel

    let descriptionLabel = UILabel()
    descriptionLabel.font = .systemFont(ofSize: 13)
    descriptionLabel.numberOfLines = 3

    let mediaView = UIView()

    let ctaButton = UIButton(type: .system)
    ctaButton.titleLabel?.font = .systemFont(ofSize: 15, weight: .semibold)

    let subviews = [iconView, headlineLabel, advertiserLabel, descriptionLabel, mediaView, ctaButton]
    subviews.forEach {
      $0.translatesAutoresizingMaskIntoConstraints = false
      nativeAdView.addSubview($0)
    }

    NSLayoutConstraint.activate([
      iconView.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 12),
      iconView.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 12),
      iconView.widthAnchor.constraint(equalToConstant: 40),
      iconView.heightAnchor.constraint(equalToConstant: 40),

      headlineLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 8),
      headlineLabel.topAnchor.constraint(equalTo: iconView.topAnchor),
      headlineLabel.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -12),

      advertiserLabel.leadingAnchor.constraint(equalTo: headlineLabel.leadingAnchor),
      advertiserLabel.trailingAnchor.constraint(equalTo: headlineLabel.trailingAnchor),
      advertiserLabel.topAnchor.constraint(equalTo: headlineLabel.bottomAnchor, constant: 4),

      descriptionLabel.leadingAnchor.constraint(equalTo: iconView.leadingAnchor),
      descriptionLabel.trailingAnchor.constraint(equalTo: headlineLabel.trailingAnchor),
      descriptionLabel.topAnchor.constraint(equalTo: iconView.bottomAnchor, constant: 8),

      mediaView.leadingAnchor.constraint(equalTo: descriptionLabel.leadingAnchor),
      mediaView.trailingAnchor.constraint(equalTo: descriptionLabel.trailingAnchor),
      mediaView.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 8),
      mediaView.heightAnchor.constraint(equalToConstant: 150),

      ctaButton.trailingAnchor.constraint(equalTo: descriptionLabel.trailingAnchor),
      ctaButton.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 8),
      nativeAdView.bottomAnchor.constraint(equalTo: ctaButton.bottomAnchor, constant: 12),
    ])

    nativeAdView.iv_icon = iconView
    nativeAdView.l_headline = headlineLabel
    nativeAdView.l_advertiser = advertiserLabel
    nativeAdView.l_description = descriptionLabel
    nativeAdView.media = mediaView
    nativeAdView.b_cta = ctaButton
    return nativeAdView
  }
  #endif

  private func release() {
    #if canImport(AdMixerMediation)
    nativeAdContainer?.stop()
    nativeAdContainer?.removeFromSuperview()
    nativeAdContainer = nil
    sdkDelegate = nil
    #endif
    subviews.forEach { $0.removeFromSuperview() }
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
    onAdFailedToLoad?(napSspErrorPayload(adUnitId: adUnitId, format: "native", error: error))
  }

  fileprivate func emitFailure(adUnitId: String, code: String, message: String) {
    isLoaded = false
    onAdFailedToLoad?(["adUnitId": adUnitId, "format": "native", "code": code, "message": message])
  }

  fileprivate func eventPayload(adUnitId: String, extra: [String: Any] = [:]) -> [String: Any] {
    var payload: [String: Any] = ["adUnitId": adUnitId, "format": "native"]
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
private final class NapSspNativeDelegate: NSObject, AMMNativeDelegate {
  private weak var host: NativeAdView?
  private let adUnitId: String

  init(view: NativeAdView, adUnitId: String) {
    self.host = view
    self.adUnitId = adUnitId
  }

  func onSuccessShowNative() {
    host?.emitImpression(adUnitId: adUnitId)
  }

  func onClickNative() {
    host?.emitClick(adUnitId: adUnitId)
  }
}
#endif
