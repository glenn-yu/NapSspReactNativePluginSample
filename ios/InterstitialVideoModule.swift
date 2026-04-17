import Foundation
import React

@objc(NapSspInterstitialVideo)
class InterstitialVideoModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      // Mocking load success
      resolve(nil)
      NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: [
        "adUnitId": adUnitId,
        "format": "interstitial_video"
      ])
    }
  }

  @objc
  func show(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      // Mocking show success
      resolve(nil)
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: [
        "adUnitId": adUnitId,
        "format": "interstitial_video"
      ])
      
      // Simulate video completion after 1 second
      DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
        NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: [
          "adUnitId": adUnitId,
          "format": "interstitial_video"
        ])
        
        NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: [
          "adUnitId": adUnitId,
          "format": "interstitial_video"
        ])
      }
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    _ = adUnitId
  }
}
