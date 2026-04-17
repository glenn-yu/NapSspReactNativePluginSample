import Foundation
import UIKit
import React
#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

extension Notification.Name {
  static let napSspDidInitialize = Notification.Name("com.napssp.didInitialize")
  static let napSspInterstitialDidLoad = Notification.Name("com.napssp.interstitial.didLoad")
  static let napSspInterstitialDidPresent = Notification.Name("com.napssp.interstitial.didPresent")
  static let napSspRewardedDidLoad = Notification.Name("com.napssp.rewarded.didLoad")
  static let napSspRewardedDidPresent = Notification.Name("com.napssp.rewarded.didPresent")
  static let napSspRewardedDidEarn = Notification.Name("com.napssp.rewarded.didEarn")
  static let napSspTrackingAuthorizationDidChange = Notification.Name("com.napssp.att.didChange")
}

struct NapSspConfiguration {
  let mediaKey: String
  let adUnitIds: [String]
  let mediations: [String: Any]
  let logLevel: String
  let coppa: Bool

  init(dictionary: NSDictionary) throws {
    guard let mediaKey = dictionary["mediaKey"] as? String,
      !mediaKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    else {
      throw NapSspError.invalidConfiguration("Missing required field 'mediaKey'.")
    }

    guard let rawAdUnitIds = dictionary["adUnitIds"] as? [Any], !rawAdUnitIds.isEmpty else {
      throw NapSspError.invalidConfiguration("Missing required field 'adUnitIds'.")
    }

    let mediations = Self.normalizeMediationConfiguration(dictionary["mediations"])
    let logLevel = (dictionary["logLevel"] as? String)?.lowercased() ?? "info"
    let coppa = dictionary["coppa"] as? Bool ?? false

    self.mediaKey = mediaKey.trimmingCharacters(in: .whitespacesAndNewlines)
    self.adUnitIds = rawAdUnitIds
      .compactMap { $0 as? String }
      .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
      .filter { !$0.isEmpty }
    self.mediations = mediations
    self.logLevel = logLevel
    self.coppa = coppa

    if self.adUnitIds.isEmpty {
      throw NapSspError.invalidConfiguration("'adUnitIds' must contain at least one non-empty ad unit id.")
    }
  }

  var dictionaryRepresentation: [String: Any] {
    [
      "mediaKey": mediaKey,
      "adUnitIds": adUnitIds,
      "mediations": mediations,
      "logLevel": logLevel,
      "coppa": coppa,
    ]
  }

  private static func normalizeMediationConfiguration(_ value: Any?) -> [String: Any] {
    guard let dictionary = value as? [String: Any] else { return [:] }

    var result: [String: Any] = [:]
    for (key, rawValue) in dictionary {
      switch rawValue {
      case let nested as [String: Any]:
        let sanitized = nested.reduce(into: [String: Any]()) { partialResult, element in
          if !(element.value is NSNull) {
            partialResult[element.key] = element.value
          }
        }
        if !sanitized.isEmpty {
          result[key] = sanitized
        }
      case let bool as Bool:
        result[key] = bool
      case let string as String:
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty {
          result[key] = trimmed
        }
      case let array as [Any]:
        let cleaned = array.compactMap { $0 as? String }.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        if !cleaned.isEmpty {
          result[key] = cleaned
        }
      case is NSNull:
        continue
      default:
        result[key] = rawValue
      }
    }

    return result
  }
}

enum NapSspError: LocalizedError {
  case invalidConfiguration(String)
  case notInitialized
  case adNotLoaded(String)
  case unsupported(String)

  var errorCode: String {
    switch self {
    case .invalidConfiguration:
      return "napssp_invalid_configuration"
    case .notInitialized:
      return "napssp_not_initialized"
    case .adNotLoaded:
      return "napssp_ad_not_loaded"
    case .unsupported:
      return "napssp_unsupported"
    }
  }

  var errorDescription: String? {
    switch self {
    case .invalidConfiguration(let message), .adNotLoaded(let message), .unsupported(let message):
      return message
    case .notInitialized:
      return "NapSsp has not been initialized yet. Call initialize() first."
    }
  }
}

struct NapSspBannerSize {
  let width: CGFloat
  let height: CGFloat

  static let banner = NapSspBannerSize(width: 320, height: 50)
  static let mediumRectangle = NapSspBannerSize(width: 300, height: 250)
  static let largeBanner = NapSspBannerSize(width: 320, height: 100)
  static let smartBanner = NapSspBannerSize(width: 320, height: 50)

  static func parse(_ rawValue: String?) -> NapSspBannerSize {
    switch rawValue?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
    case "MEDIUM_RECTANGLE", "MREC", "300X250":
      return .mediumRectangle
    case "LARGE_BANNER", "320X100":
      return .largeBanner
    case "SMART_BANNER":
      return .smartBanner
    default:
      return .banner
    }
  }
}

struct NapSspReward {
  let type: String
  let amount: Double
  let currency: String?

  static let placeholder = NapSspReward(type: "placeholder-points", amount: 1, currency: "points")

  var dictionaryRepresentation: [String: Any] {
    var payload: [String: Any] = [
      "type": type,
      "amount": amount,
    ]

    if let currency {
      payload["currency"] = currency
    }

    return payload
  }
}

final class NapSspRuntime {
  static let shared = NapSspRuntime()

  private let stateQueue = DispatchQueue(label: "com.napssp.runtime.state")
  private var configuration: NapSspConfiguration?
  private var initializedAt: Date?
  private var logLevel: String = "info"
  private var coppaEnabled: Bool = false
  private var trackingAuthorizationStatus: String?
  private var lastInterstitialAdUnitId: String?
  private var loadedInterstitialAdUnitIds: Set<String> = []
  private var lastRewardedAdUnitId: String?
  private var loadedRewardedAdUnitIds: Set<String> = []

  private init() {}

  var isInitialized: Bool {
    stateQueue.sync { configuration != nil }
  }

  func initialize(with configDictionary: NSDictionary) throws -> [String: Any] {
    let config = try NapSspConfiguration(dictionary: configDictionary)

    let status = stateQueue.sync {
      configuration = config
      initializedAt = Date()
      logLevel = config.logLevel
      coppaEnabled = config.coppa
      loadedInterstitialAdUnitIds.removeAll()
      loadedRewardedAdUnitIds.removeAll()
      lastInterstitialAdUnitId = nil
      lastRewardedAdUnitId = nil

      return currentStatusLocked(extra: [
        "message": "NapSsp placeholder runtime initialized.",
        "configuration": config.dictionaryRepresentation,
      ])
    }

    DispatchQueue.main.async {
      NotificationCenter.default.post(name: .napSspDidInitialize, object: nil, userInfo: status)
    }

    return status
  }

  func setLogLevel(_ newValue: String) {
    let normalized = newValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    stateQueue.sync {
      logLevel = normalized.isEmpty ? "info" : normalized
    }
  }

  func setCoppa(_ enabled: Bool) {
    stateQueue.sync {
      coppaEnabled = enabled
    }
  }

  func currentStatus() -> [String: Any] {
    stateQueue.sync { currentStatusLocked(extra: [:]) }
  }

  func validateInitialized() throws {
    if !isInitialized {
      throw NapSspError.notInitialized
    }
  }

  func registerInterstitialLoad(adUnitId: String) throws -> [String: Any] {
    try validateInitialized()
    let trimmed = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
      throw NapSspError.invalidConfiguration("Interstitial adUnitId must not be empty.")
    }

    let payload = stateQueue.sync {
      lastInterstitialAdUnitId = trimmed
      loadedInterstitialAdUnitIds.insert(trimmed)
      return [
        "adUnitId": trimmed,
        "loaded": true,
        "source": "placeholder",
        "loadedAt": Self.iso8601String(from: Date()),
      ]
    }

    NotificationCenter.default.post(name: .napSspInterstitialDidLoad, object: nil, userInfo: payload)
    return payload
  }

  func consumeInterstitialPresentation(adUnitId: String? = nil) -> [String: Any]? {
    let payload = stateQueue.sync {
      let target = adUnitId?.trimmingCharacters(in: .whitespacesAndNewlines)
      let resolvedTarget = (target?.isEmpty == false ? target : lastInterstitialAdUnitId)
      guard let resolvedTarget, loadedInterstitialAdUnitIds.contains(resolvedTarget) else {
        return nil
      }

      loadedInterstitialAdUnitIds.remove(resolvedTarget)
      return [
        "adUnitId": resolvedTarget,
        "presented": true,
        "source": "placeholder",
        "presentedAt": Self.iso8601String(from: Date()),
      ]
    }

    guard let payload else { return nil }

    NotificationCenter.default.post(name: .napSspInterstitialDidPresent, object: nil, userInfo: payload)
    return payload
  }

  func registerRewardedLoad(adUnitId: String) throws -> [String: Any] {
    try validateInitialized()
    let trimmed = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
      throw NapSspError.invalidConfiguration("Rewarded adUnitId must not be empty.")
    }

    let payload = stateQueue.sync {
      lastRewardedAdUnitId = trimmed
      loadedRewardedAdUnitIds.insert(trimmed)
      return [
        "adUnitId": trimmed,
        "loaded": true,
        "source": "placeholder",
        "loadedAt": Self.iso8601String(from: Date()),
      ]
    }

    NotificationCenter.default.post(name: .napSspRewardedDidLoad, object: nil, userInfo: payload)
    return payload
  }

  func consumeRewardedPresentation(adUnitId: String? = nil) -> [String: Any]? {
    let payload = stateQueue.sync {
      let target = adUnitId?.trimmingCharacters(in: .whitespacesAndNewlines)
      let resolvedTarget = (target?.isEmpty == false ? target : lastRewardedAdUnitId)
      guard let resolvedTarget, loadedRewardedAdUnitIds.contains(resolvedTarget) else {
        return nil
      }

      loadedRewardedAdUnitIds.remove(resolvedTarget)
      return [
        "adUnitId": resolvedTarget,
        "presented": true,
        "reward": NapSspReward.placeholder.dictionaryRepresentation,
        "source": "placeholder",
        "presentedAt": Self.iso8601String(from: Date()),
      ]
    }

    guard let payload else { return nil }

    NotificationCenter.default.post(name: .napSspRewardedDidPresent, object: nil, userInfo: payload)
    if let adUnitId = payload["adUnitId"] as? String {
      NotificationCenter.default.post(
        name: .napSspRewardedDidEarn,
        object: nil,
        userInfo: [
          "adUnitId": adUnitId,
          "reward": NapSspReward.placeholder.dictionaryRepresentation,
        ]
      )
    }
    return payload
  }

  func bannerSize(for rawValue: String?) -> NapSspBannerSize {
    NapSspBannerSize.parse(rawValue)
  }

  func requestTrackingAuthorization(completion: @escaping (String) -> Void) {
    #if canImport(AppTrackingTransparency)
    if #available(iOS 14.5, *) {
      let status = ATTrackingManager.trackingAuthorizationStatus
      guard status == .notDetermined else {
        let stringStatus = Self.string(from: status)
        stateQueue.sync { trackingAuthorizationStatus = stringStatus }
        DispatchQueue.main.async {
          NotificationCenter.default.post(name: .napSspTrackingAuthorizationDidChange, object: nil, userInfo: ["status": stringStatus])
          completion(stringStatus)
        }
        return
      }

      DispatchQueue.main.async {
        ATTrackingManager.requestTrackingAuthorization { newStatus in
          let stringStatus = Self.string(from: newStatus)
          self.stateQueue.sync {
            self.trackingAuthorizationStatus = stringStatus
          }
          DispatchQueue.main.async {
            NotificationCenter.default.post(name: .napSspTrackingAuthorizationDidChange, object: nil, userInfo: ["status": stringStatus])
            completion(stringStatus)
          }
        }
      }
    } else {
      DispatchQueue.main.async {
        completion("unavailable")
      }
    }
    #else
    DispatchQueue.main.async {
      completion("unavailable")
    }
    #endif
  }

  private func currentStatusLocked(extra: [String: Any]) -> [String: Any] {
    var payload: [String: Any] = [
      "initialized": configuration != nil,
      "logLevel": logLevel,
      "coppa": coppaEnabled,
      "loadedInterstitialAdUnitIds": Array(loadedInterstitialAdUnitIds),
      "loadedRewardedAdUnitIds": Array(loadedRewardedAdUnitIds),
    ]

    if let configuration {
      payload["mediaKey"] = Self.redactedMediaKey(configuration.mediaKey)
      payload["adUnitIds"] = configuration.adUnitIds
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
    let prefix = value.prefix(4)
    let suffix = value.suffix(4)
    return "\(prefix)…\(suffix)"
  }

  private static func iso8601String(from date: Date) -> String {
    ISO8601DateFormatter().string(from: date)
  }

  #if canImport(AppTrackingTransparency)
  @available(iOS 14.5, *)
  private static func string(from status: ATTrackingManager.AuthorizationStatus) -> String {
    switch status {
    case .authorized:
      return "authorized"
    case .denied:
      return "denied"
    case .restricted:
      return "restricted"
    case .notDetermined:
      return "notDetermined"
    @unknown default:
      return "unknown"
    }
  }
  #endif
}
