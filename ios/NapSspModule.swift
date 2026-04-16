import Foundation
import React

@objc(NapSspModule)
class NapSspModule: NSObject {
  @objc
  static func requiresMainQueueSetup() -> Bool {
    return false
  }

  @objc
  func initialize(_ config: String, resolver resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    // Placeholder: parse config JSON and call native SDK initialization
    resolve(nil)
  }

  @objc
  func setLogLevel(_ level: String) {
    // Placeholder
  }
}
