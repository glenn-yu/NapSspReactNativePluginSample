import Foundation
import React

@objc(NapSspModule)
class NapSspModule: RCTEventEmitter {
  public static var shared: NapSspModule?

  override init() {
    super.init()
    NapSspModule.shared = self
  }

  @objc
  override static func requiresMainQueueSetup() -> Bool {
    false
  }

  override func supportedEvents() -> [String]! {
    return [
      "onAdLoaded",
      "onAdFailedToLoad",
      "onAdOpened",
      "onAdClosed",
      "onAdClicked",
      "onAdImpression",
      "onRewarded",
      "onVideoCompleted",
      "onVideoSkipped",
      "napSsp_status"
    ]
  }

  @objc
  func initialize(_ config: NSDictionary, resolver resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    do {
      let status = try NapSspRuntime.shared.initialize(with: config)
      resolve(status)
      
      // Emit status event
      sendEvent(withName: "napSsp_status", body: status)
    } catch let error as NapSspError {
      reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
    } catch {
      reject("napssp_initialize_failed", error.localizedDescription, error)
    }
  }

  @objc
  func setLogLevel(_ level: String) {
    NapSspRuntime.shared.setLogLevel(level)
  }

  @objc
  func setCoppa(_ enabled: Bool) {
    NapSspRuntime.shared.setCoppa(enabled)
  }

  @objc
  func getStatus(_ resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    resolve(NapSspRuntime.shared.currentStatus())
  }

  @objc
  func requestTrackingAuthorization(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    NapSspRuntime.shared.requestTrackingAuthorization { status in
      resolve(status)
    }
  }

  // Internal helper to emit events from other modules
  func emitEvent(name: String, payload: [String: Any]) {
    if bridge != nil {
      sendEvent(withName: name, body: payload)
    }
  }
}
