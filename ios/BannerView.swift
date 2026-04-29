import Foundation
import UIKit
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

@objc(BannerView)
final class BannerView: UIView {
  @objc dynamic var adUnitId: NSString = "" {
    didSet {
      reloadIfNeeded()
    }
  }

  @objc dynamic var size: NSString = "BANNER_320x50" {
    didSet {
      configureSize()
      reloadIfNeeded()
    }
  }

  @objc var onAdLoaded: RCTBubblingEventBlock?
  @objc var onAdFailedToLoad: RCTBubblingEventBlock?
  @objc var onAdClicked: RCTBubblingEventBlock?
  @objc var onAdOpened: RCTBubblingEventBlock?
  @objc var onAdClosed: RCTBubblingEventBlock?
  @objc var onAdImpression: RCTBubblingEventBlock?

  private let containerView = UIView()
  private let titleLabel = UILabel()
  private let detailLabel = UILabel()
  private let badgeLabel = UILabel()
  private let placeholderButton = UIButton(type: .system)
  private var loadWorkItem: DispatchWorkItem?
  private var isLoaded = false
  private var hasPresentedClick = false
  private var didRegisterInitializeObserver = false

  #if canImport(AdMixerMediation)
  private var bannerView: AMMBannerView?
  private var sdkDelegate: NapSspBannerDelegate?
  #endif

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupView()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    setupView()
  }

  deinit {
    loadWorkItem?.cancel()
    if didRegisterInitializeObserver {
      NotificationCenter.default.removeObserver(self, name: .napSspDidInitialize, object: nil)
    }
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()
    if window == nil {
      loadWorkItem?.cancel()
      return
    }
    reloadIfNeeded()
  }

  override func willMove(toWindow newWindow: UIWindow?) {
    super.willMove(toWindow: newWindow)
    if newWindow == nil {
      #if canImport(AdMixerMediation)
      bannerView?.stop()
      bannerView = nil
      sdkDelegate = nil
      #endif
    }
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    containerView.frame = bounds
    
    #if canImport(AdMixerMediation)
    bannerView?.frame = bounds
    #endif

    let inset: CGFloat = 12
    badgeLabel.sizeToFit()
    badgeLabel.frame = CGRect(
      x: inset,
      y: inset,
      width: min(bounds.width - inset * 2, badgeLabel.bounds.width),
      height: badgeLabel.bounds.height
    )

    titleLabel.frame = CGRect(x: inset, y: badgeLabel.frame.maxY + 8, width: bounds.width - inset * 2, height: 20)
    detailLabel.frame = CGRect(x: inset, y: titleLabel.frame.maxY + 4, width: bounds.width - inset * 2, height: 18)
    placeholderButton.frame = CGRect(
      x: inset,
      y: max(detailLabel.frame.maxY + 8, bounds.height - 34),
      width: bounds.width - inset * 2,
      height: 24
    )

    let sizeInfo = NapSspRuntime.shared.bannerSize(for: size as String)
    containerView.layer.cornerRadius = max(8, min(bounds.height, sizeInfo.height) / 16)
  }

  private func setupView() {
    backgroundColor = .clear
    isUserInteractionEnabled = true

    containerView.backgroundColor = UIColor.systemGray6
    containerView.layer.borderColor = UIColor.systemGray3.cgColor
    containerView.layer.borderWidth = 1
    addSubview(containerView)

    badgeLabel.font = .systemFont(ofSize: 11, weight: .semibold)
    badgeLabel.textColor = .systemBlue
    badgeLabel.text = "NapSsp iOS"
    containerView.addSubview(badgeLabel)

    titleLabel.font = .systemFont(ofSize: 16, weight: .semibold)
    titleLabel.textColor = .label
    titleLabel.text = "Banner placeholder ready"
    containerView.addSubview(titleLabel)

    detailLabel.font = .systemFont(ofSize: 13)
    detailLabel.textColor = .secondaryLabel
    detailLabel.numberOfLines = 2
    detailLabel.text = "Waiting for native SDK integration"
    containerView.addSubview(detailLabel)

    placeholderButton.isUserInteractionEnabled = false
    placeholderButton.setTitle("Tap callback wired", for: .normal)
    placeholderButton.setTitleColor(.systemBlue, for: .normal)
    placeholderButton.titleLabel?.font = .systemFont(ofSize: 12, weight: .medium)
    containerView.addSubview(placeholderButton)

    let recognizer = UITapGestureRecognizer(target: self, action: #selector(handleTap))
    addGestureRecognizer(recognizer)
    configureSize()
    registerForInitializationNotifications()
  }

  private func configureSize() {
    let bannerSize = NapSspRuntime.shared.bannerSize(for: size as String)
    detailLabel.text = "adUnitId: \(adUnitId as String) • size: \(Int(bannerSize.width))×\(Int(bannerSize.height))"
    invalidateIntrinsicContentSize()
    setNeedsLayout()
  }

  override var intrinsicContentSize: CGSize {
    let bannerSize = NapSspRuntime.shared.bannerSize(for: size as String)
    return CGSize(width: bannerSize.width, height: bannerSize.height)
  }

  private func reloadIfNeeded() {
    loadWorkItem?.cancel()

    guard window != nil else { return }

    let currentAdUnitId = (adUnitId as String).trimmingCharacters(in: .whitespacesAndNewlines)
    guard !currentAdUnitId.isEmpty else {
      titleLabel.text = "Banner placeholder ready"
      detailLabel.text = "Set adUnitId to trigger loading"
      containerView.backgroundColor = UIColor.systemGray6
      containerView.layer.borderColor = UIColor.systemGray3.cgColor
      return
    }

    guard NapSspRuntime.shared.isInitialized else {
      emitFailure(code: .notInitialized, adUnitId: currentAdUnitId)
      return
    }

    if isLoaded { return }

    #if canImport(AdMixerMediation)
    loadWithSdk(adUnitId: currentAdUnitId)
    #else
    hasPresentedClick = false
    titleLabel.text = "Loading banner…"
    detailLabel.text = "adUnitId: \(currentAdUnitId)"
    containerView.backgroundColor = UIColor.systemGray6
    containerView.layer.borderColor = UIColor.systemGray3.cgColor

    let workItem = DispatchWorkItem { [weak self] in
      self?.finishLoading(adUnitId: currentAdUnitId)
    }
    loadWorkItem = workItem
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.18, execute: workItem)
    #endif
  }

  #if canImport(AdMixerMediation)
  private func loadWithSdk(adUnitId: String) {
    guard let rootVC = NapSspRuntime.activeRootViewController() else { return }
    guard let numericAdUnitId = Int(adUnitId) else {
      emitSdkFailure(adUnitId: adUnitId, code: "napssp_invalid_ad_unit", message: "Banner adUnitId must be numeric on iOS.")
      return
    }

    bannerView?.stop()
    bannerView?.removeFromSuperview()
    bannerView = nil
    
    let delegate = NapSspBannerDelegate(view: self, adUnitId: adUnitId)
    sdkDelegate = delegate

    let sizeInfo = NapSspRuntime.shared.bannerSize(for: size as String)

    let view = AMMBannerView(rootViewController: rootVC)
    view.adUnitID = numericAdUnitId
    view.delegate = delegate
    view.frame = CGRect(x: 0, y: 0, width: sizeInfo.width, height: sizeInfo.height)
    
    bannerView = view
    addSubview(view)
    view.load()
  }

  func attachSdkView() {
    containerView.isHidden = true
    if let view = bannerView {
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
      "size": size as String,
      "code": code,
      "message": message
    ])
  }
  #endif

  private func finishLoading(adUnitId: String) {
    guard loadWorkItem?.isCancelled == false else { return }
    isLoaded = true
    titleLabel.text = "Banner loaded"
    detailLabel.text = "adUnitId: \(adUnitId) • source: placeholder"
    containerView.backgroundColor = UIColor.systemGreen.withAlphaComponent(0.12)
    containerView.layer.borderColor = UIColor.systemGreen.cgColor

    onAdLoaded?(eventPayload(adUnitId: adUnitId, source: "placeholder", message: "Banner placeholder loaded"))
    onAdImpression?(eventPayload(adUnitId: adUnitId, source: "placeholder", message: "Banner impression"))
  }

  private func registerForInitializationNotifications() {
    guard !didRegisterInitializeObserver else { return }
    didRegisterInitializeObserver = true
    NotificationCenter.default.addObserver(self, selector: #selector(handleRuntimeInitialized(_:)), name: .napSspDidInitialize, object: nil)
  }

  @objc private func handleRuntimeInitialized(_ notification: Notification) {
    guard !isLoaded else { return }
    reloadIfNeeded()
  }

  private func emitFailure(code: NapSspError, adUnitId: String) {
    isLoaded = false
    titleLabel.text = "Banner load failed"
    let message = code.errorDescription ?? "Banner load failed"
    detailLabel.text = message
    containerView.backgroundColor = UIColor.systemRed.withAlphaComponent(0.08)
    containerView.layer.borderColor = UIColor.systemRed.cgColor
    onAdFailedToLoad?([
      "adUnitId": adUnitId,
      "size": size as String,
      "code": code.errorCode,
      "message": message
    ])
  }

  @objc private func handleTap() {
    guard isLoaded else { return }
    let payload = eventPayload(adUnitId: adUnitId as String, source: "placeholder", message: "Banner tapped")
    onAdClicked?(payload)

    if !hasPresentedClick {
      hasPresentedClick = true
      onAdOpened?(payload)
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
        guard let self else { return }
        self.onAdClosed?(self.eventPayload(adUnitId: self.adUnitId as String, source: "placeholder", message: "Banner dismissed"))
      }
    }
  }

  func eventPayload(adUnitId: String, source: String, message: String) -> [String: Any] {
    return [
      "adUnitId": adUnitId,
      "size": size as String,
      "source": source,
      "message": message
    ]
  }
}

#if canImport(AdMixerMediation)
private final class NapSspBannerDelegate: NSObject, AMMBannerViewDelegate {
  private weak var bannerView: BannerView?
  private let adUnitId: String

  init(view: BannerView, adUnitId: String) {
    self.bannerView = view
    self.adUnitId = adUnitId
  }

  func onSuccessBanner() {
    guard let view = bannerView else { return }
    view.attachSdkView()
    view.onAdLoaded?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Banner loaded"))
    view.onAdImpression?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Banner impression"))
  }

  func onFailBanner() {
    guard let view = bannerView else { return }
    view.emitSdkFailure(adUnitId: adUnitId, code: "napssp_banner_load_failed", message: "unknown")
  }

  func onTapBanner() {
    guard let view = bannerView else { return }
    view.onAdClicked?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Banner tapped"))
    view.onAdOpened?(view.eventPayload(adUnitId: adUnitId, source: "sdk", message: "Banner opened"))
  }
}
#endif