import UIKit
import React

@objc(NapSspNativeAdView)
class NativeAdViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func view() -> UIView! {
    NativeAdView()
  }
}
