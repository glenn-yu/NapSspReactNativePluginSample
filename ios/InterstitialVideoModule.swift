import Foundation
import React
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

@objc(NapSspInterstitialVideo)
class InterstitialVideoModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      let timeout = options?["timeout"] as? Int ?? 30
      let maxRetry = options?["maxRetryCountInSlot"] as? Int ?? 3
      let customParams = options?["customParams"] as? [String: String]

      AMMVideoInterstitial.load(adUnitID: adUnitId, timeout: timeout, maxRetryCountInSlot: maxRetry, customParam: customParams) { [weak self] result in
        switch result {
        case .success(let ad):
          NapSspRuntime.shared.storeInterstitialVideo(adUnitId: adUnitId, instance: ad)
          resolve(nil)
          NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
            "adUnitId": adUnitId, "format": "interstitial_video"
          ])
        case .failure(let error):
          let payload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial_video", error: error)
          reject(payload["code"] as? String ?? "LOAD_FAILED", payload["message"] as? String ?? "Load failed", error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
        }
      }
      #else
      resolve(nil)
      NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
        "adUnitId": adUnitId,
        "format": "interstitial_video"
      ])
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
      guard let ad = NapSspRuntime.shared.consumeStoredInterstitialVideo(adUnitId: adUnitId) else {
        reject("NOT_LOADED", "InterstitialVideo ad '\(adUnitId)' is not loaded.", nil)
        return
      }

      guard let rootVC = UIApplication.shared.keyWindow?.rootViewController else {
        reject("NO_ROOT_VC", "No root view controller available.", nil)
        return
      }

      let delegate = NapSspInterstitialVideoDelegate(adUnitId: adUnitId)
      delegate.resolve = resolve
      NapSspRuntime.shared.storeInterstitialVideoDelegate(adUnitId: adUnitId, delegate: delegate)
      ad.delegate = delegate
      ad.show(rootViewController: rootVC)
      #else
      resolve(nil)
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
      NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])

      DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
        NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
        NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
      }
      #endif
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    #if canImport(AdMixerMediation)
    NapSspRuntime.shared.removeStoredInterstitialVideo(adUnitId: adUnitId)
    #endif
  }
}

#if canImport(AdMixerMediation)
private final class NapSspInterstitialVideoDelegate: NSObject, AMMVideoInterstitialDelegate {
  private let adUnitId: String
  var resolve: RCTPromiseResolveBlock?

  init(adUnitId: String) { self.adUnitId = adUnitId }

  func videoInterstitialDidOpen(_ ad: AMMVideoInterstitial) {
    resolve?(nil)
    resolve = nil
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }

  func videoInterstitialDidClose(_ ad: AMMVideoInterstitial) {
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
    NapSspRuntime.shared.removeStoredInterstitialVideoDelegate(adUnitId: adUnitId)
  }

  func videoInterstitialDidComplete(_ ad: AMMVideoInterstitial) {
    NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }

  func videoInterstitialDidSkip(_ ad: AMMVideoInterstitial) {
    NapSspModule.shared?.emitEvent(name: "onVideoSkipped", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }

  func videoInterstitialDidClick(_ ad: AMMVideoInterstitial) {
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }
}
#endif
