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
  var nativeAdContainer: AMMNativeAdViewContainer?
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

    #if DEBUG
    loadPlaceholder(adUnitId: currentAdUnitId)
    #elseif canImport(AdMixerMediation)
    loadWithSdk(adUnitId: currentAdUnitId)
    #else
    loadPlaceholder(adUnitId: currentAdUnitId)
    #endif
  }


  private func loadPlaceholder(adUnitId: String) {
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
      guard let self = self, !self.isLoaded else { return }
      self.isLoaded = true
      self.titleLabel.text = "Native Ad loaded"
      self.detailLabel.text = "adUnitId: \(adUnitId) • source: placeholder"
      self.onAdLoaded?(self.eventPayload(adUnitId: adUnitId, message: "Native ad loaded (placeholder)"))
      self.onAdImpression?(self.eventPayload(adUnitId: adUnitId, message: "Native ad impression"))
    }
  }

  #if canImport(AdMixerMediation)
  private func loadWithSdk(adUnitId: String) {
    guard let rootVC = NapSspRuntime.activeRootViewController() else { return }
    guard let numericAdUnitId = Int(adUnitId) else {
      onAdFailedToLoad?([
        "adUnitId": adUnitId, "format": "native",
        "code": "napssp_invalid_ad_unit", "message": "Native adUnitId must be numeric on iOS."
      ])
      return
    }

    // v2.2.1: remove existing container before loading new one
    nativeAdContainer?.stop()
    nativeAdContainer = nil

    let delegate = NapSspNativeDelegate(view: self, adUnitId: adUnitId)
    sdkDelegate = delegate

    let container = AMMNativeAdViewContainer(rootViewController: rootVC)
    container.adUnitID = numericAdUnitId
    container.delegate = delegate
    nativeAdContainer = container

    let nativeAdView = loadNativeAdView() ?? makeProgrammaticNativeAdView()
    nativeAdView.viewController = rootVC
    nativeAdView.adUnitID = numericAdUnitId
    nativeAdView.frame = bounds
    nativeAdView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    container.nativeAdView = nativeAdView

    container.load()
  }

  private func loadNativeAdView() -> AMMNativeAdView? {
    let nibName = "AMMNativeAdView"
    let candidateBundles: [Bundle] = [
      Bundle(for: NativeAdView.self),
      Bundle.main
    ] + Bundle.allFrameworks

    for bundle in candidateBundles {
      if bundle.path(forResource: nibName, ofType: "nib") != nil || bundle.path(forResource: nibName, ofType: "xib") != nil {
        if let view = bundle.loadNibNamed(nibName, owner: nil, options: nil)?.first as? AMMNativeAdView {
          return view
        }
      }
    }

    return nil
  }

  private func makeProgrammaticNativeAdView() -> AMMNativeAdView {
    let nativeAdView = AMMNativeAdView()
    nativeAdView.backgroundColor = UIColor(red: 1.0, green: 0.983, blue: 0.929, alpha: 1.0)

    let iconView = UIImageView()
    iconView.contentMode = .scaleAspectFit
    iconView.translatesAutoresizingMaskIntoConstraints = false

    let headlineLabel = UILabel()
    headlineLabel.font = .systemFont(ofSize: 17)
    headlineLabel.textColor = .label
    headlineLabel.text = "Headline"
    headlineLabel.translatesAutoresizingMaskIntoConstraints = false

    let advertiserLabel = UILabel()
    advertiserLabel.font = .systemFont(ofSize: 14)
    advertiserLabel.textColor = .label
    advertiserLabel.text = "Advertiser"
    advertiserLabel.translatesAutoresizingMaskIntoConstraints = false

    let descriptionLabel = UILabel()
    descriptionLabel.font = .systemFont(ofSize: 14)
    descriptionLabel.textColor = .label
    descriptionLabel.numberOfLines = 0
    descriptionLabel.text = "Body"
    descriptionLabel.translatesAutoresizingMaskIntoConstraints = false

    let mediaView = UIView()
    mediaView.translatesAutoresizingMaskIntoConstraints = false

    let ctaButton = UIButton(type: .system)
    ctaButton.setTitle("cta", for: .normal)
    ctaButton.titleLabel?.font = .systemFont(ofSize: 18)
    ctaButton.translatesAutoresizingMaskIntoConstraints = false

    [iconView, headlineLabel, advertiserLabel, descriptionLabel, mediaView, ctaButton].forEach { nativeAdView.addSubview($0) }

    NSLayoutConstraint.activate([
      iconView.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 15),
      iconView.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 15),
      iconView.widthAnchor.constraint(equalToConstant: 40),
      iconView.heightAnchor.constraint(equalToConstant: 40),

      headlineLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 8),
      headlineLabel.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 10),
      headlineLabel.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -15),
      headlineLabel.heightAnchor.constraint(equalToConstant: 20.5),

      advertiserLabel.leadingAnchor.constraint(equalTo: headlineLabel.leadingAnchor),
      advertiserLabel.topAnchor.constraint(equalTo: headlineLabel.bottomAnchor, constant: 10),

      descriptionLabel.leadingAnchor.constraint(equalTo: iconView.leadingAnchor),
      descriptionLabel.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -10),
      descriptionLabel.topAnchor.constraint(equalTo: iconView.bottomAnchor),

      mediaView.centerXAnchor.constraint(equalTo: nativeAdView.centerXAnchor),
      mediaView.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor),
      mediaView.widthAnchor.constraint(equalToConstant: 250),
      mediaView.heightAnchor.constraint(equalToConstant: 150),

      ctaButton.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 15.5),
      ctaButton.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -10),
      nativeAdView.bottomAnchor.constraint(greaterThanOrEqualTo: ctaButton.bottomAnchor, constant: 30)
    ])

    nativeAdView.iv_icon = iconView
    nativeAdView.l_headline = headlineLabel
    nativeAdView.l_advertiser = advertiserLabel
    nativeAdView.l_description = descriptionLabel
    nativeAdView.media = mediaView
    nativeAdView.b_cta = ctaButton
    return nativeAdView
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
