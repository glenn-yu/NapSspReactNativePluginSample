import Foundation
import React

@objc(NapSspRewarded)
class RewardedModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      do {
        let status = try NapSspRuntime.shared.registerRewardedLoad(adUnitId: adUnitId)
        resolve(status)
        NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
          "adUnitId": adUnitId,
          "format": "rewarded"
        ])
      } catch let error as NapSspError {
        reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
        NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
          "adUnitId": adUnitId,
          "format": "rewarded",
          "code": error.errorCode,
          "message": error.errorDescription ?? error.errorCode
        ])
      } catch {
        reject("napssp_rewarded_load_failed", error.localizedDescription, error)
        NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
          "adUnitId": adUnitId,
          "format": "rewarded",
          "code": "napssp_rewarded_load_failed",
          "message": error.localizedDescription
        ])
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
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: [
        "adUnitId": adUnitId,
        "format": "rewarded"
      ])
      
      let rewardData = payload["reward"] as? [String: Any]
      let type = rewardData?["type"] as? String ?? "reward"
      let amount = rewardData?["amount"] as? Double ?? 1.0
      
      NapSspModule.shared?.emitEvent(name: "onRewarded", payload: [
        "adUnitId": adUnitId,
        "format": "rewarded",
        "type": type,
        "amount": amount
      ])
      
      // Emit close right after opened and rewarded since this is a placeholder/mock SDK.
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: [
        "adUnitId": adUnitId,
        "format": "rewarded"
      ])
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    _ = adUnitId
  }
}
