import Foundation
import UIKit
import React

@objc(BannerView)
class BannerView: UIView {
  private var adUnitId: String? = nil

  @objc
  func setAdUnitId(_ id: NSString) {
    adUnitId = id as String
    // Placeholder: load ad using native SDK
  }

  override init(frame: CGRect) {
    super.init(frame: frame)
    backgroundColor = .clear
  }

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
  }
}
