import Foundation
import UIKit
import React

@objc(NapSspNativeAdView)
final class NativeAdViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool { true }

  override func view() -> UIView! { NativeAdView() }

  @objc func reload(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? NativeAdView else { return }
      view.reload()
    }
  }
}
