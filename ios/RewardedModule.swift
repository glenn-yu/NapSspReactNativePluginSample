import React
import Foundation
import UIKit
#if canImport(AdMixerMediation)
import AdMixerMediation
#endif

/// Bridges `AMMRewardVideo`.
/// https://napmx.github.io/#/ios/native/rewarded-video
@objc(NapSspRewarded)
final class RewardedModule: NSObject {
  private static let format = "rewarded"

  #if canImport(AdMixerMediation)
  private let registry = NapSspFullScreenRegistry<AMMRewardVideo>()
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
      guard let reward = self.registry.ad(for: key), reward.isAdReady else {
        reject(
          NapSspError.adNotLoaded("No rewarded ad is ready for \"\(key)\".").errorCode,
          "No rewarded ad is ready for \"\(key)\". Await load() first.",
          nil
        )
        return
      }
      guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
      reward.show(rootViewController: rootVC)
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
      self.registry.remove(key)?.stop()
      let request = NapSspFullScreenRequest(resolve: resolve, reject: reject)
      self.registry.begin(request, for: key)

      // Echoed back on the S2S reward callback.
      let customParam = (options?["customParams"] as? [String: String])?.isEmpty == false
        ? options?["customParams"] as? [String: String]
        : nil

      let delegate = NapSspRewardedDelegate(adUnitId: key, generation: request.generation, module: self)

      AMMRewardVideo.loadAd(adUnitID: numericAdUnitId, customParam: customParam) { reward, adapterType, error in
        DispatchQueue.main.async {
          guard self.registry.complete(request, for: key) else {
            if let reward, self.registry.ad(for: key) !== reward { reward.stop() }
            return
          }
          if let error {
            self.registry.remove(key)
            reward?.stop()
            let payload = napSspErrorPayload(adUnitId: key, format: Self.format, error: error)
            NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
            request.reject(payload["code"] as? String ?? "napssp_load_failed", error.localizedDescription, error)
            return
          }
          guard let reward else {
            self.registry.remove(key)
            NapSspModule.shared?.emitEvent(
              name: "onAdFailedToLoad",
              payload: ["adUnitId": key, "format": Self.format, "code": "napssp_empty_ad",
                        "message": "The SDK returned no rewarded ad and no error."]
            )
            request.reject("napssp_empty_ad", "The SDK returned no rewarded ad and no error.", nil)
            return
          }

          reward.delegate = delegate
          self.registry.store(reward, delegate: delegate, for: key)

          NapSspModule.shared?.emitEvent(
            name: "onAdLoaded",
            payload: ["adUnitId": key, "format": Self.format, "network": adapterType.adapterName]
          )

          guard shouldAutoShow else {
            request.resolve(nil)
            return
          }
          guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: { request.reject($0, $1, $2) }) else { return }
          reward.show(rootViewController: rootVC)
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
private final class NapSspRewardedDelegate: NSObject, AMMRewardVideoDelegate {
  private let adUnitId: String
  private let generation: UUID
  private weak var module: RewardedModule?

  init(adUnitId: String, generation: UUID, module: RewardedModule) {
    self.adUnitId = adUnitId
    self.generation = generation
    self.module = module
  }

  private var basePayload: [String: Any] { ["adUnitId": adUnitId, "format": "rewarded"] }

  func onSuccessShowReward() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: basePayload)
      NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: basePayload)
    }
  }

  func onFailShowReward(error: Error?) {
    module?.handleDelegate(adUnitId, generation: generation, terminal: true) { [self] in
      var payload = napSspErrorPayload(adUnitId: adUnitId, format: "rewarded", error: error)
      payload["phase"] = "show"
      payload["isReady"] = false
      NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
    }
  }

  func onClickRewardVideo() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: basePayload)
    }
  }

  func onRewardVideoComplete() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: basePayload)
    }
  }

  // Implementing the rewardInfo overload suppresses the deprecated no-argument one, so the reward
  // is notified exactly once and carries the id used by the S2S callback.
  func onRewardVideoEarned(rewardInfo: RewardInfo) {
    module?.handleDelegate(adUnitId, generation: generation, terminal: false) { [self] in
      var payload = basePayload
      payload["transactionId"] = rewardInfo.transactionId as Any?
      // The SDK carries no amount or currency — units are not comparable across mediation networks.
      // Kept for 0.4.x compatibility; reconcile with transactionId instead.
      payload["type"] = "reward"
      payload["amount"] = 1
      NapSspModule.shared?.emitEvent(name: "onRewarded", payload: payload.compactMapValues { $0 })
    }
  }

  func onCloseRewardVideo() {
    module?.handleDelegate(adUnitId, generation: generation, terminal: true) { [self] in
      NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: basePayload)
    }
  }
}
#endif
