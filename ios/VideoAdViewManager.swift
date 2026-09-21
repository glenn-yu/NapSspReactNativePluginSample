import Foundation
import UIKit
import React

@objc(NapSspVideoAdView)
final class VideoAdViewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool { true }

  override func view() -> UIView! { VideoAdView() }

  @objc func reload(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? VideoAdView else { return }
      view.reload()
    }
  }
}
