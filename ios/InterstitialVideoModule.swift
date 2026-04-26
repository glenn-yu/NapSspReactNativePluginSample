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
      NSLog("[NapSspInterstitialVideo] load requested adUnitId=%@ options=%@", adUnitId, options ?? [:])
      #if canImport(AdMixerMediation)
      guard let adUnit = Int(adUnitId) else {
        reject("napssp_invalid_ad_unit", "Interstitial video adUnitId must be numeric on iOS.", nil)
        return
      }

      AMMVideoInterstitial.load(adUnitID: adUnit) { [weak self] ad, error in
        guard let _ = self else { return }
        if let error = error {
          NSLog("[NapSspInterstitialVideo] load failed adUnitId=%@ error=%@", adUnitId, error.localizedDescription)
          let payload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial_video", error: error)
          reject(payload["code"] as? String ?? "LOAD_FAILED", payload["message"] as? String ?? "Load failed", error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
          return
        }
        if let ad = ad {
          NSLog("[NapSspInterstitialVideo] load succeeded adUnitId=%@ storing instance", adUnitId)
          NapSspRuntime.shared.storeInterstitialVideo(adUnitId: adUnitId, instance: ad)
        } else {
          NSLog("[NapSspInterstitialVideo] load completed adUnitId=%@ with nil instance and no error", adUnitId)
        }
        resolve(nil)
        NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
          "adUnitId": adUnitId, "format": "interstitial_video"
        ])
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
      NSLog("[NapSspInterstitialVideo] show requested adUnitId=%@", adUnitId)
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      #if canImport(AdMixerMediation)
      guard let ad = NapSspRuntime.shared.consumeStoredInterstitialVideo(adUnitId: adUnitId) else {
        NSLog("[NapSspInterstitialVideo] show missing stored ad adUnitId=%@", adUnitId)
        reject("NOT_LOADED", "InterstitialVideo ad '\(adUnitId)' is not loaded.", nil)
        return
      }

      guard let rootVC = NapSspRuntime.activeRootViewController() else {
        NSLog("[NapSspInterstitialVideo] show missing rootVC adUnitId=%@", adUnitId)
        reject("NO_ROOT_VC", "No root view controller available.", nil)
        return
      }

      let delegate = NapSspInterstitialVideoDelegate(adUnitId: adUnitId)
      delegate.resolve = resolve
      NapSspRuntime.shared.storeInterstitialVideoDelegate(adUnitId: adUnitId, delegate: delegate)
      ad.delegate = delegate
      NSLog("[NapSspInterstitialVideo] calling show on SDK adUnitId=%@ rootVC=%@", adUnitId, String(describing: type(of: rootVC)))
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

  func onSuccessShowVideoInterstitial() {
    NSLog("[NapSspInterstitialVideo] delegate success show adUnitId=%@", adUnitId)
    resolve?(nil)
    resolve = nil
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }

  func onCloseVideoInterstitial() {
    NSLog("[NapSspInterstitialVideo] delegate close adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
    NapSspRuntime.shared.removeStoredInterstitialVideoDelegate(adUnitId: adUnitId)
  }

  func onCompleteVideoInterstitial() {
    NSLog("[NapSspInterstitialVideo] delegate completed adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }

  func onTapVideoInterstitialViewMore() {
    NSLog("[NapSspInterstitialVideo] delegate tap adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: ["adUnitId": adUnitId, "format": "interstitial_video"])
  }
}
#endif
