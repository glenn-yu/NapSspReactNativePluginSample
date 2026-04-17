import Foundation
import React

@objc(NapSspRewarded)
class RewardedModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      do {
        let status = try NapSspRuntime.shared.registerRewardedLoad(adUnitId: adUnitId)
        resolve(status)
      } catch let error as NapSspError {
        reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
      } catch {
        reject("napssp_rewarded_load_failed", error.localizedDescription, error)
      }
    }
  }

  @objc
  func show(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      guard let payload = NapSspRuntime.shared.consumeRewardedPresentation(adUnitId: adUnitId) else {
        reject(NapSspError.adNotLoaded("No rewarded ad has been loaded yet.").errorCode, "No rewarded ad has been loaded yet.", nil)
        return
      }

      resolve(payload)
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    // The placeholder runtime only keeps in-memory state, so releasing a rewarded
    // ad is just a no-op for now. This method exists so the bridge matches the JS API.
    _ = adUnitId
  }
}
