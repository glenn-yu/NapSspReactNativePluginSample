import UIKit
import React

@objc(NapSspVideoAdView)
class VideoAdViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func view() -> UIView! {
    VideoAdView()
  }
}
