import Foundation
import UIKit
import React

/// All registry and promise operations run on the main queue, alongside SDK access.
/// Invalidating a request does not cancel the SDK's static load operation: no handle exists yet.
final class NapSspFullScreenRequest {
  let generation = UUID()
  private var resolveBlock: RCTPromiseResolveBlock?
  private var rejectBlock: RCTPromiseRejectBlock?

  init(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    resolveBlock = resolve
    rejectBlock = reject
  }

  func resolve(_ value: Any?) {
    let callback = resolveBlock
    resolveBlock = nil
    rejectBlock = nil
    callback?(value)
  }

  func reject(_ code: String?, _ message: String?, _ error: Error?) {
    let callback = rejectBlock
    resolveBlock = nil
    rejectBlock = nil
    callback?(code, message, error)
  }
}

/// Retains weak SDK delegates and tracks loads before the SDK supplies an ad object.
final class NapSspFullScreenRegistry<Ad: AnyObject> {
  private var ads: [String: Ad] = [:]
  private var delegates: [String: NSObject] = [:]
  private var generations: [String: UUID] = [:]
  private var requests: [String: NapSspFullScreenRequest] = [:]

  func begin(_ request: NapSspFullScreenRequest, for key: String) {
    generations[key] = request.generation
    requests[key] = request
  }

  func isCurrent(_ generation: UUID, for key: String) -> Bool {
    generations[key] == generation
  }

  func isLoading(_ key: String) -> Bool { requests[key] != nil }

  /// Claims a completion once. A stale or duplicate completion must discard its ad.
  func complete(_ request: NapSspFullScreenRequest, for key: String) -> Bool {
    guard requests[key] === request, isCurrent(request.generation, for: key) else { return false }
    requests.removeValue(forKey: key)
    return true
  }

  func store(_ ad: Ad, delegate: NSObject, for key: String) {
    ads[key] = ad
    delegates[key] = delegate
  }

  func ad(for key: String) -> Ad? { ads[key] }

  /// Cancelling an idle unit preserves an already loaded or showing ad.
  @discardableResult
  func cancelLoad(_ key: String) -> Ad? {
    guard isLoading(key) else { return nil }
    return remove(key)
  }

  @discardableResult
  func remove(_ key: String) -> Ad? {
    // Invalidate before settling or stopping, since either may trigger callbacks.
    generations.removeValue(forKey: key)
    let request = requests.removeValue(forKey: key)
    let ad = ads.removeValue(forKey: key)
    delegates.removeValue(forKey: key)
    request?.reject("napssp_load_cancelled", "The ad load was cancelled, destroyed, or superseded.", nil)
    return ad
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
