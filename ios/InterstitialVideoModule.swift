import React
import Foundation
import UIKit
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// Bridges `AMMVideoInterstitial` (full-screen video).
/// https://napmx.github.io/#/ios/native/video
@objc(NapSspInterstitialVideo)
final class InterstitialVideoModule: NSObject {
  private static let format = "interstitial_video"

  #if canImport(AdMixerMediation)
  private let registry = NapSspFullScreenRegistry<AMMVideoInterstitial>()
  #endif

  @objc static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(
    _ adUnitId: String,
    options: NSDictionary?,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    performLoad(adUnitId, autoShow: false, resolve: resolve, reject: reject)
  }

  @objc
  func start(
    _ adUnitId: String,
    options: NSDictionary?,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    performLoad(adUnitId, autoShow: true, resolve: resolve, reject: reject)
  }

  @objc
  func show(
    _ adUnitId: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
      guard let ad = self.registry.ad(for: key), ad.isAdReady else {
        reject(
          NapSspError.adNotLoaded("No interstitial video is ready for \"\(key)\".").errorCode,
          "No interstitial video is ready for \"\(key)\". Await load() first.",
          nil
        )
        return
      }
      guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
      ad.show(rootViewController: rootVC)
      resolve(nil)
      #else
      NapSspFullScreenGuard.sdkNotLinked(reject)
      #endif
    }
  }

  @objc
  func isLoaded(
    _ adUnitId: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    #if canImport(AdMixerMediation)
    resolve(registry.ad(for: adUnitId.trimmingCharacters(in: .whitespacesAndNewlines))?.isAdReady ?? false)
    #else
    resolve(false)
    #endif
  }

  @objc
  func cancelLoad(
    _ adUnitId: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    destroy(adUnitId)
    resolve(nil)
  }

  @objc
  func destroy(_ adUnitId: String) {
    #if canImport(AdMixerMediation)
    let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
    DispatchQueue.main.async { self.registry.remove(key)?.stop() }
    #endif
  }

  // MARK: private

  private func performLoad(
    _ adUnitId: String,
    autoShow shouldAutoShow: Bool,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      guard let numericAdUnitId = NapSspFullScreenGuard.numericAdUnitId(
        adUnitId, format: Self.format, reject: reject
      ) else { return }

      let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
      self.registry.remove(key)?.stop()

      let delegate = NapSspInterstitialVideoDelegate(adUnitId: key, module: self)

      AMMVideoInterstitial.loadAd(adUnitID: numericAdUnitId) { ad, adapterType, error in
        if let error {
          let payload = napSspErrorPayload(adUnitId: key, format: Self.format, error: error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
          reject(payload["code"] as? String ?? "napssp_load_failed", error.localizedDescription, error)
          return
        }
        guard let ad else {
          reject("napssp_empty_ad", "The SDK returned no interstitial video and no error.", nil)
          return
        }

        ad.delegate = delegate
        self.registry.store(ad, delegate: delegate, for: key)

        NapSspModule.shared?.emitEvent(
          name: "onAdLoaded",
          payload: ["adUnitId": key, "format": Self.format, "network": adapterType.adapterName]
        )

        guard shouldAutoShow else {
          resolve(nil)
          return
        }
        guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
        ad.show(rootViewController: rootVC)
        resolve(nil)
      }
      #else
      NapSspFullScreenGuard.sdkNotLinked(reject)
      #endif
    }
  }

  fileprivate func releaseAd(_ adUnitId: String) {
    #if canImport(AdMixerMediation)
    registry.remove(adUnitId)
    #endif
  }
}

#if canImport(AdMixerMediation)
private final class NapSspInterstitialVideoDelegate: NSObject, AMMVideoInterstitialDelegate {
  private let adUnitId: String
  private weak var module: InterstitialVideoModule?

  init(adUnitId: String, module: InterstitialVideoModule) {
    self.adUnitId = adUnitId
    self.module = module
  }

  private var basePayload: [String: Any] { ["adUnitId": adUnitId, "format": "interstitial_video"] }

  func onSuccessShowVideoInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: basePayload)
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: basePayload)
  }

  func onFailShowVideoInterstitial(error: Error?) {
    var payload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial_video", error: error)
    payload["phase"] = "show"
    NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
    module?.releaseAd(adUnitId)
  }

  func onClickVideoInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: basePayload)
  }

  func onCompleteVideoInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: basePayload)
  }

  func onCloseVideoInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: basePayload)
    module?.releaseAd(adUnitId)
  }

  // NOTE: AMMVideoInterstitialDelegate has no skip callback, so `skipped` never fires for this
  // format on iOS. Android delivers it through AdListener.onAdSkipped().
}
#endif
