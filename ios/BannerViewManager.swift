import Foundation
import UIKit
import React

@objc(NapSspBannerView)
final class BannerViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool { true }

  override func view() -> UIView! { BannerView() }

  /// Imperative `reload()` dispatched from JS through the view ref.
  @objc func reload(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? BannerView else { return }
      view.reload()
    }
  }
}
