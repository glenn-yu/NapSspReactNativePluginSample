import Foundation
import UIKit
import React
#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

extension Notification.Name {
  static let napSspDidInitialize = Notification.Name("com.napssp.didInitialize")
}

struct NapSspConfiguration {
  let mediaKey: String
  let adUnitIds: [String]
  let mediations: [String: Any]
  let logLevel: String
  let coppa: Bool

  init(dictionary: NSDictionary) throws {
    guard let mediaKey = dictionary["mediaKey"] as? String, !mediaKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
      throw NapSspError.invalidConfiguration("Missing required field 'mediaKey'.")
    }

    guard let adUnitIds = dictionary["adUnitIds"] as? [String], !adUnitIds.isEmpty else {
      throw NapSspError.invalidConfiguration("Missing required field 'adUnitIds'.")
    }

    let mediations = (dictionary["mediations"] as? [String: Any]) ?? [:]
    let logLevel = (dictionary["logLevel"] as? String)?.lowercased() ?? "info"
    let coppa = dictionary["coppa"] as? Bool ?? false

    self.mediaKey = mediaKey.trimmingCharacters(in: .whitespacesAndNewlines)
    self.adUnitIds = adUnitIds.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
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

final class NapSspRuntime {
  static let shared = NapSspRuntime()

  private let stateQueue = DispatchQueue(label: "com.napssp.runtime.state")
  private var configuration: NapSspConfiguration?
  private var initializedAt: Date?
  private var logLevel: String = "info"
  private var coppaEnabled: Bool = false
  private var lastInterstitialAdUnitId: String?
  private var loadedInterstitialAdUnitIds: Set<String> = []

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
      lastInterstitialAdUnitId = nil

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

    return stateQueue.sync {
      lastInterstitialAdUnitId = trimmed
      loadedInterstitialAdUnitIds.insert(trimmed)
      return [
        "adUnitId": trimmed,
        "loaded": true,
        "source": "placeholder",
        "loadedAt": ISO8601DateFormatter().string(from: Date()),
      ]
    }
  }

  func canPresentInterstitial(adUnitId: String? = nil) -> Bool {
    stateQueue.sync {
      if let adUnitId, !adUnitId.isEmpty {
        return loadedInterstitialAdUnitIds.contains(adUnitId)
      }
      if let last = lastInterstitialAdUnitId {
        return loadedInterstitialAdUnitIds.contains(last)
      }
      return false
    }
  }

  func consumeInterstitialPresentation(adUnitId: String? = nil) -> [String: Any]? {
    stateQueue.sync {
      let target = (adUnitId?.isEmpty == false ? adUnitId : lastInterstitialAdUnitId)
      guard let target, loadedInterstitialAdUnitIds.contains(target) else {
        return nil
      }

      loadedInterstitialAdUnitIds.remove(target)
      return [
        "adUnitId": target,
        "presented": true,
        "source": "placeholder",
        "presentedAt": ISO8601DateFormatter().string(from: Date()),
      ]
    }
  }

  func bannerSize(for rawValue: String?) -> NapSspBannerSize {
    NapSspBannerSize.parse(rawValue)
  }

  func requestTrackingAuthorization(completion: @escaping (String) -> Void) {
    #if canImport(AppTrackingTransparency)
    if #available(iOS 14, *) {
      let status = ATTrackingManager.trackingAuthorizationStatus
      guard status == .notDetermined else {
        DispatchQueue.main.async {
          completion(Self.string(from: status))
        }
        return
      }

      DispatchQueue.main.async {
        ATTrackingManager.requestTrackingAuthorization { newStatus in
          DispatchQueue.main.async {
            completion(Self.string(from: newStatus))
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
    ]

    if let configuration {
      payload["mediaKey"] = Self.redactedMediaKey(configuration.mediaKey)
      payload["adUnitIds"] = configuration.adUnitIds
      payload["mediations"] = configuration.mediations
    }

    if let initializedAt {
      payload["initializedAt"] = ISO8601DateFormatter().string(from: initializedAt)
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

  #if canImport(AppTrackingTransparency)
  @available(iOS 14, *)
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
