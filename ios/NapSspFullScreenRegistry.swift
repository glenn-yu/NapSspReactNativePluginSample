import Foundation
import React

/// Thread-safe store for loaded full-screen ads and their delegates.
///
/// The SDK holds its delegate weakly, so the delegate has to be retained here for as long as the
/// ad is alive.
final class NapSspFullScreenRegistry<Ad: AnyObject> {
  private let lock = NSLock()
  private var ads: [String: Ad] = [:]
  private var delegates: [String: NSObject] = [:]

  func store(_ ad: Ad, delegate: NSObject, for adUnitId: String) {
    lock.lock()
    defer { lock.unlock() }
    ads[adUnitId] = ad
    delegates[adUnitId] = delegate
  }

  func ad(for adUnitId: String) -> Ad? {
    lock.lock()
    defer { lock.unlock() }
    return ads[adUnitId]
  }

  func delegate(for adUnitId: String) -> NSObject? {
    lock.lock()
    defer { lock.unlock() }
    return delegates[adUnitId]
  }

  @discardableResult
  func remove(_ adUnitId: String) -> Ad? {
    lock.lock()
    defer { lock.unlock() }
    delegates.removeValue(forKey: adUnitId)
    return ads.removeValue(forKey: adUnitId)
  }

  func removeAll() -> [Ad] {
    lock.lock()
    defer { lock.unlock() }
    let values = Array(ads.values)
    ads.removeAll()
    delegates.removeAll()
    return values
  }

  var loadedAdUnitIds: [String] {
    lock.lock()
    defer { lock.unlock() }
    return Array(ads.keys)
  }
}

/// Shared argument validation for the full-screen modules.
enum NapSspFullScreenGuard {
  /// Returns the numeric ad unit id, or rejects the promise and returns `nil`.
  static func numericAdUnitId(
    _ rawAdUnitId: String,
    format: String,
    reject: RCTPromiseRejectBlock
  ) -> Int? {
    let trimmed = rawAdUnitId.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
      reject("napssp_invalid_ad_unit", "A non-empty \(format) adUnitId is required.", nil)
      return nil
    }
    guard NapSspRuntime.shared.isInitialized else {
      reject(
        NapSspError.notInitialized.errorCode,
        NapSspError.notInitialized.errorDescription ?? "NapSsp is not initialized.",
        nil
      )
      return nil
    }
    guard let numeric = Int(trimmed) else {
      reject("napssp_invalid_ad_unit", "\(format) adUnitId must be numeric on iOS (received \"\(trimmed)\").", nil)
      return nil
    }
    return numeric
  }

  static func rootViewController(reject: RCTPromiseRejectBlock) -> UIViewController? {
    guard let rootVC = NapSspRuntime.activeRootViewController() else {
      reject("napssp_no_view_controller", "No root view controller is available to present the ad.", nil)
      return nil
    }
    return rootVC
  }

  static func sdkNotLinked(_ reject: RCTPromiseRejectBlock) {
    reject(
      "napssp_sdk_not_linked",
      "AdMixerMediation is not linked. Run `pod install` (or add the Swift package) and rebuild.",
      nil
    )
  }
}
