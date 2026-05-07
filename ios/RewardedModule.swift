import Foundation
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

@objc(NapSspRewarded)
class RewardedModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription, nil)
        return
      }
      guard let adUnit = Int(adUnitId) else {
        reject("napssp_invalid_ad_unit", "Rewarded adUnitId must be numeric on iOS.", nil)
        return
      }
      var customParam: [String: String]? = nil
      if let rawParams = options?["customParams"] as? [String: String], !rawParams.isEmpty {
        customParam = rawParams
      }
      AMMRewardVideo.load(adUnitID: adUnit, customParam: customParam) { [weak self] reward, error in
        guard let _ = self else {
          reject("napssp_module_released", "Module was released during load.", nil)
          return
        }
        if let error = error {
          let errPayload = napSspErrorPayload(adUnitId: adUnitId, format: "rewarded", error: error)
          reject(errPayload["code"] as? String ?? "napssp_rewarded_load_failed", error.localizedDescription, error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: errPayload)
          return
        }
        if let reward = reward {
          NapSspRuntime.shared.storeRewardedAd(adUnitId: adUnitId, instance: reward)
        }
        resolve(nil)
        NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: ["adUnitId": adUnitId, "format": "rewarded"])
      }
      #else
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
      #endif
    }
  }

  @objc
  func show(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      #if canImport(AdMixerMediation)
      guard let rewardVideo = NapSspRuntime.shared.consumeStoredRewardedAd(adUnitId: adUnitId) else {
        reject(NapSspError.adNotLoaded("No rewarded ad has been loaded yet.").errorCode, "No rewarded ad has been loaded yet.", nil)
        return
      }
      guard let rootVC = NapSspRuntime.activeRootViewController() else {
        reject("napssp_no_view_controller", "No root view controller found.", nil)
        return
      }
      rewardVideo.delegate = NapSspRewardedDelegate.shared(adUnitId: adUnitId)
      rewardVideo.show(rootViewController: rootVC)
      resolve(nil)
      #else
      guard let payload = NapSspRuntime.shared.consumeRewardedPresentation(adUnitId: adUnitId) else {
        reject(NapSspError.adNotLoaded("No rewarded ad has been loaded yet.").errorCode, "No rewarded ad has been loaded yet.", nil)
        return
      }
      resolve(payload)
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "rewarded"])
      NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "rewarded"])
      let rewardData = payload["reward"] as? [String: Any]
      NapSspModule.shared?.emitEvent(name: "onRewarded", payload: [
        "adUnitId": adUnitId, "format": "rewarded",
        "type": rewardData?["type"] as? String ?? "reward",
        "amount": (rewardData?["amount"] as? Int) ?? Int((rewardData?["amount"] as? Double) ?? 1.0)
      ])
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "rewarded"])
      #endif
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      NapSspRuntime.shared.removeStoredRewardedAd(adUnitId: adUnitId)
      NapSspRewardedDelegate.instances.removeValue(forKey: adUnitId)
      #endif
    }
  }
}

#if canImport(AdMixerMediation)
private final class NapSspRewardedDelegate: NSObject, AMMRewardVideoDelegate {
  private let adUnitId: String
  private static var instances: [String: NapSspRewardedDelegate] = [:]

  private init(adUnitId: String) { self.adUnitId = adUnitId }

  static func shared(adUnitId: String) -> NapSspRewardedDelegate {
    if let existing = instances[adUnitId] { return existing }
    let delegate = NapSspRewardedDelegate(adUnitId: adUnitId)
    instances[adUnitId] = delegate
    return delegate
  }

  func onSuccessShowReward() {
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "rewarded"])
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "rewarded"])
  }

  func onRewardVideoEarned() {
    NapSspModule.shared?.emitEvent(name: "onRewarded", payload: ["adUnitId": adUnitId, "format": "rewarded", "type": "reward", "amount": 1])
  }

  func onCloseRewardVideo() {
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "rewarded"])
    NapSspRewardedDelegate.instances.removeValue(forKey: adUnitId)
  }

  func onRewardVideoComplete() {
    NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: ["adUnitId": adUnitId, "format": "rewarded"])
  }

  func onRewardVideoSkipped() {
    NapSspModule.shared?.emitEvent(name: "onVideoSkipped", payload: ["adUnitId": adUnitId, "format": "rewarded"])
  }

  func onTapRewardVideo() {
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: ["adUnitId": adUnitId, "format": "rewarded"])
  }

  func onFailShowReward(error: Error?) {
    NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
      "adUnitId": adUnitId, "format": "rewarded",
      "code": "napssp_rewarded_show_failed",
      "message": error?.localizedDescription ?? "unknown"
    ])
    NapSspRewardedDelegate.instances.removeValue(forKey: adUnitId)
  }
}
#endif
