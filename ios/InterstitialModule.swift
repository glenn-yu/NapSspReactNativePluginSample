import React
import Foundation
import UIKit
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// Bridges `AMMInterstitial`.
/// https://napmx.github.io/#/ios/native/banner (전면 배너)
@objc(NapSspInterstitial)
final class InterstitialModule: NSObject {
  private static let format = "interstitial"

  #if canImport(AdMixerMediation)
  private let registry = NapSspFullScreenRegistry<AMMInterstitial>()
  #endif

  @objc static func requiresMainQueueSetup() -> Bool { false }

  @objc
  func load(
    _ adUnitId: String,
    options: NSDictionary?,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    performLoad(adUnitId, options: options, autoShow: false, resolve: resolve, reject: reject)
  }

  /// Load and present in one call.
  @objc
  func start(
    _ adUnitId: String,
    options: NSDictionary?,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    performLoad(adUnitId, options: options, autoShow: true, resolve: resolve, reject: reject)
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
      guard let interstitial = self.registry.ad(for: key), interstitial.isAdReady else {
        reject(
          NapSspError.adNotLoaded("No interstitial is ready for \"\(key)\".").errorCode,
          "No interstitial is ready for \"\(key)\". Await load() first.",
          nil
        )
        return
      }
      guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
      interstitial.show(rootViewController: rootVC)
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
    let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
    resolve(registry.ad(for: key)?.isAdReady ?? false)
    #else
    resolve(false)
    #endif
  }

  /// The iOS SDK cancels an in-flight load through `stop()`; a showing ad is already detached.
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
    options: NSDictionary?,
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
      // Replace any previous instance so a re-load never leaks the old one.
      self.registry.remove(key)?.stop()

      let config = AMMInterstitialConfig()
      if let ratio = options?["closeButtonTouchAreaRatio"] as? Double {
        config.closeButtonTouchAreaRatio = Float(min(max(ratio, 0.2), 1.0))
      }

      let delegate = NapSspInterstitialDelegate(adUnitId: key, module: self)

      AMMInterstitial.loadAd(adUnitID: numericAdUnitId, config: config) { interstitial, adapterType, error in
        if let error {
          let payload = napSspErrorPayload(adUnitId: key, format: Self.format, error: error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
          reject(payload["code"] as? String ?? "napssp_load_failed", error.localizedDescription, error)
          return
        }
        guard let interstitial else {
          reject("napssp_empty_ad", "The SDK returned no interstitial and no error.", nil)
          return
        }

        interstitial.delegate = delegate
        self.registry.store(interstitial, delegate: delegate, for: key)

        NapSspModule.shared?.emitEvent(
          name: "onAdLoaded",
          payload: ["adUnitId": key, "format": Self.format, "network": adapterType.adapterName]
        )

        guard shouldAutoShow else {
          resolve(nil)
          return
        }
        guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
        interstitial.show(rootViewController: rootVC)
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
private final class NapSspInterstitialDelegate: NSObject, AMMInterstitialDelegate {
  private let adUnitId: String
  private weak var module: InterstitialModule?

  init(adUnitId: String, module: InterstitialModule) {
    self.adUnitId = adUnitId
    self.module = module
  }

  private var basePayload: [String: Any] { ["adUnitId": adUnitId, "format": "interstitial"] }

  func onSuccessShowInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: basePayload)
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: basePayload)
  }

  func onFailShowInterstitial(error: Error?) {
    var payload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial", error: error)
    payload["phase"] = "show"
    NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
    module?.releaseAd(adUnitId)
  }

  func onClickInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: basePayload)
  }

  func onCloseInterstitial() {
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: basePayload)
    module?.releaseAd(adUnitId)
  }
}
#endif
