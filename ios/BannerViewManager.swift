import Foundation
import UIKit
import React

@objc(NapSspBannerView)
class BannerViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func view() -> UIView! {
    BannerView()
  }
}
