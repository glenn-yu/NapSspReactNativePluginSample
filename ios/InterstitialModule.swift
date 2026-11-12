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
  func isLoaded(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock,
                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
      resolve(self.registry.ad(for: key)?.isAdReady ?? false)
      #else
      resolve(false)
      #endif
    }
  }

  @objc
  func isLoading(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock,
                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
      resolve(self.registry.isLoading(key))
      #else
      resolve(false)
      #endif
    }
  }

  /// Invalidates the request. No SDK cancellation handle exists until completion.
  @objc
  func cancelLoad(_ adUnitId: String, resolver resolve: @escaping RCTPromiseResolveBlock,
                  rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      let key = adUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
      self.registry.cancelLoad(key)?.stop()
      #endif
      resolve(nil)
    }
  }

  @objc
  func destroy(_ adUnitId: String) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      self.registry.remove(adUnitId.trimmingCharacters(in: .whitespacesAndNewlines))?.stop()
      #endif
    }
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
      let request = NapSspFullScreenRequest(resolve: resolve, reject: reject)
      self.registry.begin(request, for: key)

      let config = AMMInterstitialConfig()
      if let ratio = options?["closeButtonTouchAreaRatio"] as? Double {
        config.closeButtonTouchAreaRatio = Float(min(max(ratio, 0.2), 1.0))
      }

      let delegate = NapSspInterstitialDelegate(adUnitId: key, generation: request.generation, module: self)

      AMMInterstitial.loadAd(adUnitID: numericAdUnitId, config: config) { interstitial, adapterType, error in
        DispatchQueue.main.async {
          guard self.registry.complete(request, for: key) else {
            if let interstitial, self.registry.ad(for: key) !== interstitial { interstitial.stop() }
            return
          }
          if let error {
            self.registry.remove(key)
            interstitial?.stop()
            let payload = napSspErrorPayload(adUnitId: key, format: Self.format, error: error)
            NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
            request.reject(payload["code"] as? String ?? "napssp_load_failed", error.localizedDescription, error)
            return
          }
          guard let interstitial else {
            self.registry.remove(key)
            NapSspModule.shared?.emitEvent(
              name: "onAdFailedToLoad",
              payload: ["adUnitId": key, "format": Self.format, "code": "napssp_empty_ad",
                        "message": "The SDK returned no interstitial and no error."]
            )
            request.reject("napssp_empty_ad", "The SDK returned no interstitial and no error.", nil)
            return
          }

          interstitial.delegate = delegate
          self.registry.store(interstitial, delegate: delegate, for: key)

          NapSspModule.shared?.emitEvent(
            name: "onAdLoaded",
            payload: ["adUnitId": key, "format": Self.format, "network": adapterType.adapterName]
          )

          guard shouldAutoShow else {
            request.resolve(nil)
            return
          }
          guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: { request.reject($0, $1, $2) }) else { return }
          interstitial.show(rootViewController: rootVC)
          request.resolve(nil)
        }
      }
      #else
      NapSspFullScreenGuard.sdkNotLinked(reject)
      #endif
    }
  }

  fileprivate func handleDelegate(_ adUnitId: String, generation: UUID, terminal: Bool = false,
                                  action: @escaping () -> Void) {
    DispatchQueue.main.async {
      #if canImport(AdMixerMediation)
      guard self.registry.isCurrent(generation, for: adUnitId) else { return }
      if terminal { self.registry.remove(adUnitId) }
      action()
      #endif
    }
  }
}

#if canImport(AdMixerMediation)
private final class NapSspInterstitialDelegate: NSObject, AMMInterstitialDelegate {
  private let adUnitId: String
  private let generation: UUID
  private weak var module: InterstitialModule?

  init(adUnitId: String, generation: UUID, module: InterstitialModule) {
    self.adUnitId = adUnitId
    self.generation = generation
    self.module = module
  }

  private var basePayload: [String: Any] { ["adUnitId": adUnitId, "format": "interstitial"] }

  func onSuccessShowInterstitial() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: basePayload)
      NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: basePayload)
    }
  }

  func onFailShowInterstitial(error: Error?) {
    module?.handleDelegate(adUnitId, generation: generation, terminal: true) { [self] in
      var payload = napSspErrorPayload(adUnitId: adUnitId, format: "interstitial", error: error)
      payload["phase"] = "show"
      payload["isReady"] = false
      NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
    }
  }

  func onClickInterstitial() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: basePayload)
    }
  }

  func onCloseInterstitial() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: true) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: basePayload)
    }
  }
}
#endif
