import Foundation
import React

@objc(NapSspModule)
final class NapSspModule: RCTEventEmitter {
  /// Set on construction so the ad modules and views can emit through the one registered emitter.
  public private(set) static weak var shared: NapSspModule?

  private var hasListeners = false

  override init() {
    super.init()
    NapSspModule.shared = self
  }

  @objc
  override static func requiresMainQueueSetup() -> Bool { false }

  override func supportedEvents() -> [String]! {
    [
      "onAdLoaded",
      "onAdFailedToLoad",
      "onAdOpened",
      "onAdClosed",
      "onAdClicked",
      "onAdImpression",
      "onRewarded",
      "onVideoCompleted",
      "onVideoSkipped",
      "napSsp_status",
    ]
  }

  override func startObserving() { hasListeners = true }

  override func stopObserving() { hasListeners = false }

  @objc
  func initialize(
    _ config: NSDictionary,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      let status = try NapSspRuntime.shared.initialize(with: config)
      resolve(status)
      emitEvent(name: "napSsp_status", payload: status)
    } catch let error as NapSspError {
      reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
    } catch {
      reject("napssp_initialize_failed", error.localizedDescription, error)
    }
  }

  @objc
  func setLogLevel(
    _ level: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    NapSspRuntime.shared.setLogLevel(level)
    resolve(nil)
  }

  /// Applies GDPR / CCPA / COPPA signals. Call before `initialize` — AppLovin, Unity Ads and
  /// Pangle only read consent once, when they start up.
  @objc
  func setPrivacyConsent(
    _ consent: NSDictionary,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    NapSspRuntime.shared.setPrivacyConsent(NapSspPrivacyConsent(dictionary: consent))
    resolve(nil)
  }

  /// Backwards-compatible alias for `setPrivacyConsent({ childDirected })`.
  @objc
  func setCoppa(
    _ enabled: Bool,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    NapSspRuntime.shared.setPrivacyConsent(NapSspPrivacyConsent(childDirected: enabled))
    resolve(nil)
  }

  /// The iOS SDK exposes no global test-mode switch — register test devices in each network
  /// dashboard instead. Resolves `false` so callers can branch on the platform capability.
  @objc
  func setTestMode(
    _ enabled: Bool,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    resolve(false)
  }

  @objc
  func setTestDeviceIds(
    _ ids: NSArray,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    resolve(false)
  }

  @objc
  func getStatus(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    resolve(NapSspRuntime.shared.currentStatus())
  }

  @objc
  func requestTrackingAuthorization(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    NapSspRuntime.shared.requestTrackingAuthorization { status in resolve(status) }
  }

  /// Internal helper used by the ad modules and views.
  func emitEvent(name: String, payload: [String: Any]) {
    guard bridge != nil, hasListeners else { return }
    sendEvent(withName: name, body: payload)
  }
}
