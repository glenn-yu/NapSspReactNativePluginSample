import Foundation
import React

@objc(NapSspInterstitial)
class InterstitialModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      do {
        let status = try NapSspRuntime.shared.registerInterstitialLoad(adUnitId: adUnitId)
        resolve(status)
      } catch let error as NapSspError {
        reject(error.errorCode, error.errorDescription ?? error.errorCode, nil)
      } catch {
        reject("napssp_interstitial_load_failed", error.localizedDescription, error)
      }
    }
  }

  @objc
  func show(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      guard let payload = NapSspRuntime.shared.consumeInterstitialPresentation() else {
        reject(NapSspError.adNotLoaded("No interstitial has been loaded yet.").errorCode, "No interstitial has been loaded yet.", nil)
        return
      }

      resolve(payload)
    }
  }
}
