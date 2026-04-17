import Foundation
import React

@objc(NapSspModule)
class NapSspModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool {
    false
  }

  @objc
  func initialize(_ config: NSDictionary, resolver resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    do {
      let status = try NapSspRuntime.shared.initialize(with: config)
      resolve(status)
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
}
