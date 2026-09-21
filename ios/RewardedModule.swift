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

      // Echoed back on the S2S reward callback.
      let customParam = (options?["customParams"] as? [String: String])?.isEmpty == false
        ? options?["customParams"] as? [String: String]
        : nil

      let delegate = NapSspRewardedDelegate(adUnitId: key, module: self)

      AMMRewardVideo.loadAd(adUnitID: numericAdUnitId, customParam: customParam) { reward, adapterType, error in
        if let error {
          let payload = napSspErrorPayload(adUnitId: key, format: Self.format, error: error)
          NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
          reject(payload["code"] as? String ?? "napssp_load_failed", error.localizedDescription, error)
          return
        }
        guard let reward else {
          reject("napssp_empty_ad", "The SDK returned no rewarded ad and no error.", nil)
          return
        }

        reward.delegate = delegate
        self.registry.store(reward, delegate: delegate, for: key)

        NapSspModule.shared?.emitEvent(
          name: "onAdLoaded",
          payload: ["adUnitId": key, "format": Self.format, "network": adapterType.adapterName]
        )

        guard shouldAutoShow else {
          resolve(nil)
          return
        }
        guard let rootVC = NapSspFullScreenGuard.rootViewController(reject: reject) else { return }
        reward.show(rootViewController: rootVC)
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
private final class NapSspRewardedDelegate: NSObject, AMMRewardVideoDelegate {
  private let adUnitId: String
  private weak var module: RewardedModule?

  init(adUnitId: String, module: RewardedModule) {
    self.adUnitId = adUnitId
    self.module = module
  }

  private var basePayload: [String: Any] { ["adUnitId": adUnitId, "format": "rewarded"] }

  func onSuccessShowReward() {
    NapSspModule.shared?.emitEvent(name: "onAdOpened", payload: basePayload)
    NapSspModule.shared?.emitEvent(name: "onAdImpression", payload: basePayload)
  }

  func onFailShowReward(error: Error?) {
    var payload = napSspErrorPayload(adUnitId: adUnitId, format: "rewarded", error: error)
    payload["phase"] = "show"
    NapSspModule.shared?.emitEvent(name: "onAdFailedToLoad", payload: payload)
    module?.releaseAd(adUnitId)
  }

  func onClickRewardVideo() {
    NapSspModule.shared?.emitEvent(name: "onAdClicked", payload: basePayload)
  }

  func onRewardVideoComplete() {
    NapSspModule.shared?.emitEvent(name: "onVideoCompleted", payload: basePayload)
  }

  // Implementing the rewardInfo overload suppresses the deprecated no-argument one, so the reward
  // is notified exactly once and carries the id used by the S2S callback.
  func onRewardVideoEarned(rewardInfo: RewardInfo) {
    var payload = basePayload
    payload["transactionId"] = rewardInfo.transactionId as Any?
    // The SDK carries no amount or currency — units are not comparable across mediation networks.
    // Kept for 0.4.x compatibility; reconcile with transactionId instead.
    payload["type"] = "reward"
    payload["amount"] = 1
    NapSspModule.shared?.emitEvent(name: "onRewarded", payload: payload.compactMapValues { $0 })
  }

  func onCloseRewardVideo() {
    NapSspModule.shared?.emitEvent(name: "onAdClosed", payload: basePayload)
    module?.releaseAd(adUnitId)
  }
}
#endif
