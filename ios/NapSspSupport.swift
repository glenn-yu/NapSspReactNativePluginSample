import Foundation
import UIKit
import React

#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

#if canImport(PAGAdSDK)
import PAGAdSDK
#endif

#if canImport(AppLovinSDK)
import AppLovinSDK
#endif

#if canImport(UnityAds)
import UnityAds
#endif

#if canImport(GFPSDK)
import GFPSDK
#endif

extension Notification.Name {
  static let napSspDidInitialize = Notification.Name("com.napssp.didInitialize")
  static let napSspTrackingAuthorizationDidChange = Notification.Name("com.napssp.att.didChange")
}

// MARK: - Configuration

/// Tri-state privacy signals. `nil` means "unspecified" — the SDK leaves the vendor default (or the
/// CMP-read IAB string) untouched, which is a different state from an explicit `false`.
/// https://napmx.github.io/#/ios/native/getting-started
struct NapSspPrivacyConsent {
  var childDirected: Bool?
  var gdprConsent: Bool?
  var usSaleConsent: Bool?
  var underAgeOfConsent: Bool?

  init(
    childDirected: Bool? = nil,
    gdprConsent: Bool? = nil,
    usSaleConsent: Bool? = nil,
    underAgeOfConsent: Bool? = nil
  ) {
    self.childDirected = childDirected
    self.gdprConsent = gdprConsent
    self.usSaleConsent = usSaleConsent
    self.underAgeOfConsent = underAgeOfConsent
  }

  init(dictionary: NSDictionary?) {
    self.init(
      childDirected: dictionary?["childDirected"] as? Bool,
      gdprConsent: dictionary?["gdprConsent"] as? Bool,
      // `ccpaDoNotSell` is the cross-platform spelling; the iOS SDK calls it usSaleConsent, and a
      // do-not-sell opt-out means consent was denied.
      usSaleConsent: (dictionary?["ccpaDoNotSell"] as? Bool).map { !$0 } ?? dictionary?["usSaleConsent"] as? Bool,
      underAgeOfConsent: dictionary?["underAgeOfConsent"] as? Bool
    )
  }

  func merging(_ other: NapSspPrivacyConsent) -> NapSspPrivacyConsent {
    NapSspPrivacyConsent(
      childDirected: other.childDirected ?? childDirected,
      gdprConsent: other.gdprConsent ?? gdprConsent,
      usSaleConsent: other.usSaleConsent ?? usSaleConsent,
      underAgeOfConsent: other.underAgeOfConsent ?? underAgeOfConsent
    )
  }

  var dictionaryRepresentation: [String: Any] {
    var payload: [String: Any] = [:]
    payload["childDirected"] = childDirected as Any?
    payload["gdprConsent"] = gdprConsent as Any?
    payload["usSaleConsent"] = usSaleConsent as Any?
    payload["underAgeOfConsent"] = underAgeOfConsent as Any?
    return payload.compactMapValues { $0 }
  }

  var isEmpty: Bool {
    childDirected == nil && gdprConsent == nil && usSaleConsent == nil && underAgeOfConsent == nil
  }
}

struct NapSspConfiguration {
  let mediaKey: String
  let adUnitIds: [String]
  let mediations: [String: Any]
  let logLevel: String
  let privacy: NapSspPrivacyConsent

  init(dictionary: NSDictionary) throws {
    guard let mediaKey = dictionary["mediaKey"] as? String,
      !mediaKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    else {
      throw NapSspError.invalidConfiguration("Missing required field 'mediaKey'.")
    }

    guard let rawAdUnitIds = dictionary["adUnitIds"] as? [Any], !rawAdUnitIds.isEmpty else {
      throw NapSspError.invalidConfiguration("Missing required field 'adUnitIds'.")
    }

    self.mediaKey = mediaKey.trimmingCharacters(in: .whitespacesAndNewlines)
    self.adUnitIds = rawAdUnitIds
      .compactMap { $0 as? String }
      .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
      .filter { !$0.isEmpty }
    self.mediations = Self.normalizeMediationConfiguration(dictionary["mediations"])
    self.logLevel = (dictionary["logLevel"] as? String)?.lowercased() ?? "info"

    var privacy = NapSspPrivacyConsent(dictionary: dictionary["privacy"] as? NSDictionary)
    // `coppa` on the root config is the 0.4.x spelling of `privacy.childDirected`.
    if privacy.childDirected == nil, let coppa = dictionary["coppa"] as? Bool {
      privacy.childDirected = coppa
    }
    self.privacy = privacy

    if self.adUnitIds.isEmpty {
      throw NapSspError.invalidConfiguration("'adUnitIds' must contain at least one non-empty ad unit id.")
    }
    guard Int(self.mediaKey) != nil else {
      throw NapSspError.invalidConfiguration("'mediaKey' must be numeric on iOS (received \"\(self.mediaKey)\").")
    }
  }

  private static func normalizeMediationConfiguration(_ value: Any?) -> [String: Any] {
    guard let dictionary = value as? [String: Any] else { return [:] }

    var result: [String: Any] = [:]
    for (key, rawValue) in dictionary where !(rawValue is NSNull) {
      switch rawValue {
      case let nested as [String: Any]:
        let sanitized = nested.filter { !($0.value is NSNull) }
        if !sanitized.isEmpty { result[key] = sanitized }
      case let string as String:
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { result[key] = trimmed }
      default:
        result[key] = rawValue
      }
    }
    return result
  }
}

// MARK: - Errors

/// nap mx iOS SDK error codes. Delivered as `NSError` whose `domain` is the ad view class name.
/// https://napmx.github.io/#/ios/native/getting-started (Step 4)
enum NapSspSdkErrorCode: Int {
  case loadFailed = -1
  case invalidAdUnit = -2
  case adapterNotFound = -3
  case invalidNetwork = -4
  case showFailed = -5
  case invalidAdUnitSize = -6
  case cancelled = -7
  case timeout = -8
  case unknown = 0

  var stringCode: String {
    switch self {
    case .loadFailed: return "napssp_load_failed"
    case .invalidAdUnit: return "napssp_invalid_ad_unit"
    case .adapterNotFound: return "napssp_adapter_not_found"
    case .invalidNetwork: return "napssp_invalid_network"
    case .showFailed: return "napssp_show_failed"
    case .invalidAdUnitSize: return "napssp_invalid_ad_unit_size"
    case .cancelled: return "napssp_load_cancelled"
    case .timeout: return "napssp_timeout"
    case .unknown: return "napssp_error"
    }
  }

  static func from(_ error: Error?) -> NapSspSdkErrorCode {
    guard let nsError = error as NSError? else { return .unknown }
    return NapSspSdkErrorCode(rawValue: nsError.code) ?? .unknown
  }
}

func napSspErrorPayload(adUnitId: String, format: String, error: Error?) -> [String: Any] {
  let sdkCode = NapSspSdkErrorCode.from(error)
  let nsError = error as NSError?

  var payload: [String: Any] = [
    "adUnitId": adUnitId,
    "format": format,
    "code": sdkCode.stringCode,
    "message": error?.localizedDescription ?? sdkCode.stringCode,
  ]
  if let nsError {
    payload["nativeCode"] = nsError.code
    payload["nativeDomain"] = nsError.domain
    if let underlying = nsError.userInfo[NSUnderlyingErrorKey] as? NSError {
      payload["details"] = [
        "underlyingCode": underlying.code,
        "underlyingDomain": underlying.domain,
        "underlyingMessage": underlying.localizedDescription,
      ]
    }
  }
  return payload
}

enum NapSspError: LocalizedError {
  case invalidConfiguration(String)
  case notInitialized
  case adNotLoaded(String)
  case unsupported(String)

  var errorCode: String {
    switch self {
    case .invalidConfiguration: return "napssp_invalid_configuration"
    case .notInitialized: return "napssp_not_initialized"
    case .adNotLoaded: return "napssp_ad_not_loaded"
    case .unsupported: return "napssp_unsupported"
    }
  }

  var errorDescription: String? {
    switch self {
    case .invalidConfiguration(let message), .adNotLoaded(let message), .unsupported(let message):
      return message
    case .notInitialized:
      return "NapSsp has not been initialized yet. Await NapSspAd.initialize() first."
    }
  }
}

// MARK: - Banner sizing

struct NapSspBannerSize {
  let width: CGFloat
  let height: CGFloat

  static let banner = NapSspBannerSize(width: 320, height: 50)
  static let mediumRectangle = NapSspBannerSize(width: 300, height: 250)
  static let largeBanner = NapSspBannerSize(width: 320, height: 100)

  static func parse(_ rawValue: String?) -> NapSspBannerSize {
    let normalized = rawValue?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
    switch normalized {
    case "MEDIUM_RECTANGLE", "MREC", "300X250", "BANNER_300X250":
      return .mediumRectangle
    case "LARGE_BANNER", "320X100", "BANNER_320X100":
      return .largeBanner
    case "SMART_BANNER", "":
      return .banner
    default:
      let parts = normalized.components(separatedBy: "_")
      if parts.count == 2, let dims = parts.last?.components(separatedBy: "X"),
        dims.count == 2, let w = Double(dims[0]), let h = Double(dims[1]),
        w > 0, h > 0
      {
        return NapSspBannerSize(width: CGFloat(w), height: CGFloat(h))
      }
      return .banner
    }
  }
}

// MARK: - Runtime

final class NapSspRuntime {
  static let shared = NapSspRuntime()

  private let stateQueue = DispatchQueue(label: "com.napssp.runtime.state")
  private var configuration: NapSspConfiguration?
  private var initializedAt: Date?
  private var logLevel: String = "info"
  private var privacy = NapSspPrivacyConsent()
  private var trackingAuthorizationStatus: String?

  private init() {}

  var isInitialized: Bool {
    stateQueue.sync { configuration != nil }
  }

  func initialize(with configDictionary: NSDictionary) throws -> [String: Any] {
    let config = try NapSspConfiguration(dictionary: configDictionary)

    #if canImport(AdMixerMediation)
    // Consent must reach the SDK before any network initialises — AppLovin, Unity Ads and Pangle
    // only read it once, at their own start-up.
    let mergedPrivacy = stateQueue.sync { privacy }.merging(config.privacy)
    applyConsent(mergedPrivacy)

    AMMediation.shared.setDebugEnabled(isEnabled: Self.isDebugLevel(config.logLevel))
    AMMediation.shared.initialize(
      mediaKey: Int(config.mediaKey) ?? 0,
      adunitID: Set(config.adUnitIds.compactMap(Int.init))
    )

    startNetworkSdks(with: config, privacy: mergedPrivacy)
    #endif

    let status = stateQueue.sync { () -> [String: Any] in
      configuration = config
      initializedAt = Date()
      logLevel = config.logLevel
      privacy = privacy.merging(config.privacy)
      return currentStatusLocked(extra: ["message": "NapSsp runtime initialized."])
    }

    DispatchQueue.main.async {
      NotificationCenter.default.post(name: .napSspDidInitialize, object: nil, userInfo: status)
    }

    return status
  }

  func setLogLevel(_ newValue: String) {
    let normalized = newValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    stateQueue.sync { logLevel = normalized.isEmpty ? "info" : normalized }
    #if canImport(AdMixerMediation)
    AMMediation.shared.setDebugEnabled(isEnabled: Self.isDebugLevel(normalized))
    #endif
  }

  func setPrivacyConsent(_ consent: NapSspPrivacyConsent) {
    let merged = stateQueue.sync { () -> NapSspPrivacyConsent in
      privacy = privacy.merging(consent)
      return privacy
    }
    #if canImport(AdMixerMediation)
    applyConsent(merged)
    #endif
  }

  func currentStatus() -> [String: Any] {
    stateQueue.sync { currentStatusLocked(extra: [:]) }
  }

  func validateInitialized() throws {
    if !isInitialized { throw NapSspError.notInitialized }
  }

  func requestTrackingAuthorization(completion: @escaping (String) -> Void) {
    #if canImport(AppTrackingTransparency)
    if #available(iOS 14.5, *) {
      let finish: (ATTrackingManager.AuthorizationStatus) -> Void = { [weak self] status in
        let stringStatus = Self.string(from: status)
        self?.stateQueue.sync { self?.trackingAuthorizationStatus = stringStatus }
        DispatchQueue.main.async {
          NotificationCenter.default.post(
            name: .napSspTrackingAuthorizationDidChange,
            object: nil,
            userInfo: ["status": stringStatus]
          )
          completion(stringStatus)
        }
      }

      DispatchQueue.main.async {
        let status = ATTrackingManager.trackingAuthorizationStatus
        guard status == .notDetermined else {
          finish(status)
          return
        }
        ATTrackingManager.requestTrackingAuthorization(completionHandler: finish)
      }
      return
    }
    #endif
    DispatchQueue.main.async { completion("unavailable") }
  }

  static func activeRootViewController() -> UIViewController? {
    let scenes = UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .filter { $0.activationState == .foregroundActive }
    let window = scenes.flatMap(\.windows).first(where: \.isKeyWindow)
      ?? UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap(\.windows)
        .first(where: \.isKeyWindow)

    var controller = window?.rootViewController
    while let presented = controller?.presentedViewController {
      controller = presented
    }
    return controller
  }

  // MARK: private

  #if canImport(AdMixerMediation)
  private func applyConsent(_ consent: NapSspPrivacyConsent) {
    guard !consent.isEmpty else { return }

    let ammConsent = AMMConsent()
    ammConsent.gdprConsent = Self.status(consent.gdprConsent)
    ammConsent.usSaleConsent = Self.status(consent.usSaleConsent)
    ammConsent.childDirected = Self.status(consent.childDirected)
    ammConsent.underAgeOfConsent = Self.status(consent.underAgeOfConsent)
    AMMediation.shared.setConsent(ammConsent)
  }

  private static func status(_ value: Bool?) -> AMMConsentStatus {
    guard let value else { return .unspecified }
    return value ? .granted : .denied
  }

  private func startNetworkSdks(with config: NapSspConfiguration, privacy: NapSspPrivacyConsent) {
    #if canImport(GoogleMobileAds)
    if let gadAppId = Bundle.main.object(forInfoDictionaryKey: "GADApplicationIdentifier") as? String,
      !gadAppId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    {
      MobileAds.shared.start(completionHandler: nil)
    }
    #endif

    #if canImport(PAGAdSDK)
    if let pangle = config.mediations["pangle"] as? [String: Any],
      let appId = pangle["appId"] as? String
    {
      let pagConfig = PAGConfig.share()
      pagConfig.appID = appId
      PAGSdk.start(with: pagConfig) { _, _ in }
    }
    #endif

    #if canImport(AppLovinSDK)
    if let appLovin = config.mediations["appLovin"] as? [String: Any],
      let sdkKey = appLovin["sdkKey"] as? String
    {
      ALSdk.shared().initialize(with: ALSdkInitializationConfiguration(sdkKey: sdkKey)) { _ in }
    }
    #endif

    #if canImport(UnityAds)
    if let unity = config.mediations["unityAds"] as? [String: Any],
      let appId = unity["appId"] as? String
    {
      UnityAds.initialize(appId)
    }
    #endif

    #if canImport(GFPSDK)
    if let nam = config.mediations["naverAdManager"] as? [String: Any],
      let publisherCd = nam["publisherCd"] as? String
    {
      GFPAdManager.setup(withPublisherCd: publisherCd, target: nil) { [weak self] _ in
        // GFPAdManager.setup can replace the shared settings object, so the consent has to be
        // re-applied once setup finishes.
        self?.applyConsent(privacy)
      }
    }
    #endif
  }
  #endif

  private static func isDebugLevel(_ level: String) -> Bool {
    level == "debug" || level == "verbose"
  }

  private func currentStatusLocked(extra: [String: Any]) -> [String: Any] {
    var payload: [String: Any] = [
      "initialized": configuration != nil,
      "platform": "ios",
      "logLevel": logLevel,
      "privacy": privacy.dictionaryRepresentation,
      // Mirrors privacy.childDirected; kept for backwards compatibility with 0.4.x.
      "coppa": privacy.childDirected ?? false,
      // The iOS SDK exposes no global test-mode switch — register test devices per network instead.
      "testMode": false,
      "testModeSupported": false,
    ]

    if let configuration {
      payload["mediaKey"] = Self.redactedMediaKey(configuration.mediaKey)
      payload["configuredAdUnitIds"] = configuration.adUnitIds
      payload["mediations"] = configuration.mediations
    }
    if let initializedAt {
      payload["initializedAt"] = Self.iso8601String(from: initializedAt)
    }
    if let trackingAuthorizationStatus {
      payload["trackingAuthorizationStatus"] = trackingAuthorizationStatus
    }

    extra.forEach { payload[$0.key] = $0.value }
    return payload
  }

  private static func redactedMediaKey(_ value: String) -> String {
    guard value.count > 8 else { return String(repeating: "*", count: value.count) }
    return "\(value.prefix(4))…\(value.suffix(4))"
  }

  private static func iso8601String(from date: Date) -> String {
    ISO8601DateFormatter().string(from: date)
  }

  #if canImport(AppTrackingTransparency)
  @available(iOS 14.5, *)
  private static func string(from status: ATTrackingManager.AuthorizationStatus) -> String {
    switch status {
    case .authorized: return "authorized"
    case .denied: return "denied"
    case .restricted: return "restricted"
    case .notDetermined: return "notDetermined"
    @unknown default: return "unknown"
    }
  }
  #endif
}
