import React
import Foundation

@objc(NapSspInterstitial)
class InterstitialModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      do {
        let status = try NapSspRuntime.shared.registerInterstitialLoad(adUnitId: adUnitId)
        resolve(status)
        NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
          "adUnitId": adUnitId,
          "format": "interstitial"
        ])
      } catch let error as NapSspError {
        reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
        NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
          "adUnitId": adUnitId,
          "format": "interstitial",
          "code": error.errorCode,
          "message": error.errorDescription ?? error.errorCode
        ])
      } catch {
        reject("napssp_interstitial_load_failed", error.localizedDescription, error)
        NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
          "adUnitId": adUnitId,
          "format": "interstitial",
          "code": "napssp_interstitial_load_failed",
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

      guard let payload = NapSspRuntime.shared.consumeInterstitialPresentation(adUnitId: adUnitId) else {
        reject(NapSspError.adNotLoaded("No interstitial has been loaded yet.").errorCode, "No interstitial has been loaded yet.", nil)
        return
      }

      resolve(payload)
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: [
        "adUnitId": adUnitId,
        "format": "interstitial"
      ])
      
      // In a real ad SDK, close event is fired when the user closes the UI.
      // Since this is a placeholder/mock SDK right now, we immediately emit the closed event to complete the cycle.
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: [
        "adUnitId": adUnitId,
        "format": "interstitial"
      ])
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    _ = adUnitId
  }
}
