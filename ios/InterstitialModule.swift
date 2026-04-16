import Foundation
import React

@objc(InterstitialModule)
class InterstitialModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool { return false }

  @objc
  func load(_ adUnitId: String, resolver resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    // Placeholder: call native SDK to load interstitial
    resolve(true)
  }

  @objc
  func show(_ resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    // Placeholder: present interstitial if loaded
    resolve(true)
  }
}
