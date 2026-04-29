import React
import Foundation
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

@objc(NapSspInterstitial)
class InterstitialModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(_ adUnitId: String, options: NSDictionary?, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      NSLog("[NapSspInterstitial] load requested adUnitId=%@ options=%@", adUnitId, options ?? [:])
      #if canImport(AdMixerMediation)
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription, nil)
        return
      }
      guard let adUnit = Int(adUnitId) else {
        reject("napssp_invalid_ad_unit", "Interstitial adUnitId must be numeric on iOS.", nil)
        return
      }
      let config = AMMInterstitialConfig()
      let adType = options?["type"] as? String ?? "default"
      switch adType {
      case "popup":
        config.viewType = .popup
        let buttonTitle = options?["buttonLeftText"] as? String ?? "닫기"
        config.popupOption = AMMInterstitialPopupOption(
          buttonTitle: buttonTitle,
          buttonTextColor: .white,
          buttonBackgroundColor: .black
        )
      case "countdown":
        config.viewType = .countDown
        let countDownTime = options?["countDownTime"] as? Int ?? 5
        config.countDownOption = AMMInterstitialCountDownOption(
          countDownTime: max(2, min(5, countDownTime)),
          countDownType: .gauge
        )
      default:
        config.viewType = .basic
      }
      if let ratio = options?["closeButtonTouchAreaRatio"] as? Double {
        config.closeButtonTouchAreaRatio = Float(max(0.2, min(1.0, ratio)))
      }
      AMMInterstitial.load(adUnitID: adUnit, config: config) { [weak self] interstitial, error in
        guard let _ = self else { return }
        if let error = error {
          NSLog("[NapSspInterstitial] load failed adUnitId=%@ error=%@", adUnitId, error.localizedDescription)
          let errPayload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial", error: error)
          reject(errPayload["code"] as? String ?? "napssp_interstitial_load_failed", error.localizedDescription, error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: errPayload)
          return
        }
        if let interstitial = interstitial {
          NSLog("[NapSspInterstitial] load succeeded adUnitId=%@ storing instance", adUnitId)
          NapSspRuntime.shared.storeInterstitial(adUnitId: adUnitId, instance: interstitial)
        } else {
          NSLog("[NapSspInterstitial] load completed adUnitId=%@ with nil instance and no error", adUnitId)
        }
        resolve(nil)
        NapSspModule.shared?.emitEvent(name: "onAdLoaded", payload: ["adUnitId": adUnitId, "format": "interstitial"])
      }
      #else
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
      #endif
    }
  }

  @objc
  func show(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      NSLog("[NapSspInterstitial] show requested adUnitId=%@", adUnitId)
      guard NapSspRuntime.shared.isInitialized else {
        reject(NapSspError.notInitialized.errorCode, NapSspError.notInitialized.errorDescription ?? "NapSsp has not been initialized yet.", nil)
        return
      }

      #if canImport(AdMixerMediation)
      guard let interstitial = NapSspRuntime.shared.peekStoredInterstitial(adUnitId: adUnitId) else {
        NSLog("[NapSspInterstitial] show missing stored interstitial adUnitId=%@", adUnitId)
        reject(NapSspError.adNotLoaded("No interstitial has been loaded yet.").errorCode, "No interstitial has been loaded yet.", nil)
        return
      }
      guard let rootVC = NapSspRuntime.activeRootViewController() else {
        NSLog("[NapSspInterstitial] show missing rootVC adUnitId=%@", adUnitId)
        reject("napssp_no_view_controller", "No root view controller found.", nil)
        return
      }
      interstitial.delegate = NapSspInterstitialDelegate.shared(adUnitId: adUnitId)
      NSLog("[NapSspInterstitial] calling show on SDK adUnitId=%@ rootVC=%@", adUnitId, String(describing: type(of: rootVC)))
      interstitial.show(rootViewController: rootVC)
      resolve(nil)
      #else
      guard let payload = NapSspRuntime.shared.consumeInterstitialPresentation(adUnitId: adUnitId) else {
        reject(NapSspError.adNotLoaded("No interstitial has been loaded yet.").errorCode, "No interstitial has been loaded yet.", nil)
        return
      }
      resolve(payload)
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "interstitial"])
      NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "interstitial"])
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "interstitial"])
      #endif
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    #if canImport(AdMixerMediation)
    NapSspRuntime.shared.removeStoredInterstitial(adUnitId: adUnitId)
    #endif
  }
}

#if canImport(AdMixerMediation)
private final class NapSspInterstitialDelegate: NSObject, AMMInterstitialDelegate {
  private let adUnitId: String
  private static var instances: [String: NapSspInterstitialDelegate] = [:]

  private init(adUnitId: String) { self.adUnitId = adUnitId }

  static func shared(adUnitId: String) -> NapSspInterstitialDelegate {
    if let existing = instances[adUnitId] { return existing }
    let delegate = NapSspInterstitialDelegate(adUnitId: adUnitId)
    instances[adUnitId] = delegate
    return delegate
  }

  func onSuccessShowInterstitial() {
    NSLog("[NapSspInterstitial] delegate success show adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: ["adUnitId": adUnitId, "format": "interstitial"])
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: ["adUnitId": adUnitId, "format": "interstitial"])
  }

  func onFailShowInterstitial(error: Error?) {
    NSLog("[NapSspInterstitial] delegate fail show adUnitId=%@ error=%@", adUnitId, error?.localizedDescription ?? "unknown")
    NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: [
      "adUnitId": adUnitId, "format": "interstitial",
      "code": "napssp_interstitial_show_failed",
      "message": error?.localizedDescription ?? "unknown"
    ])
    NapSspRuntime.shared.removeStoredInterstitial(adUnitId: adUnitId)
    NapSspInterstitialDelegate.instances.removeValue(forKey: adUnitId)
  }

  func onTapInterstitial() {
    NSLog("[NapSspInterstitial] delegate tap adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: ["adUnitId": adUnitId, "format": "interstitial"])
  }

  func onCloseInterstitial() {
    NSLog("[NapSspInterstitial] delegate close adUnitId=%@", adUnitId)
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: ["adUnitId": adUnitId, "format": "interstitial"])
    NapSspRuntime.shared.removeStoredInterstitial(adUnitId: adUnitId)
    NapSspInterstitialDelegate.instances.removeValue(forKey: adUnitId)
  }
}
#endif
