# SPM integration (Swift Package Manager)

This guide shows how to consume the iOS native piece of `react-native-nap-ssp` via Swift Package Manager.

Two options:

A) Consumer adds vendor binary (AdMixerMediation) + our source target via SPM
B) Consumer uses our Package.swift (source target) and adds vendor binary separately

Quick steps (recommended: B for plugin development)

1) Add NapSspPlugin as a local package in Xcode
   - In Xcode: File → Add Packages... → Add Local Package
   - Select the plugin `ios/` folder in this repo
   - Choose the `NapSspPlugin` product

2) Add vendor SDK (AdMixerMediation) as a binary target in your app Package.swift

Example `Package.swift` snippet for consumer that pulls vendor XCFramework:

```swift
// swift-tools-version:5.9
import PackageDescription
let package = Package(
  name: "MyApp",
  platforms: [.iOS(.v13)],
  dependencies: [
    .package(url: "https://github.com/glenn-yu/NapSspReactNativePluginSample.git", branch: "main"),
    .binaryTarget(
      name: "AdMixerMediation",
      url: "https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.1.xcframework.zip",
      checksum: "e98110e66527253b5d41474d3097fe9f9bd2f5d125a6fdd044a51876d9aa1a01"
    )
  ],
  targets: [
    .target(name: "MyApp", dependencies: ["NapSspPlugin", "AdMixerMediation"])  
  ]
)
```

3) In Xcode, add `NapSspPlugin` to the app target's Swift Packages.
4) Ensure Info.plist includes tracking strings and any required keys (e.g. GADApplicationIdentifier if using GAM).

Notes

- If the vendor does not provide a binary XCFramework, SPM integration requires the vendor to publish an SPM-compatible artifact or you to wrap their frameworks as XCFramework and host a zipped binary.
- Podspec-based subspecs (GAM / AdFit / Pangle) do not directly map to SPM; use separate binary targets or conditional compilation in the consumer app.

Troubleshooting

- If Xcode cannot find symbols from `AdMixerMediation`, confirm the binaryTarget URL and checksum are correct and reachable.
- For CI, prefer using binary XCFramework URLs (hosted on accessible storage) to avoid CocoaPods dependency on runners without Ruby/CocoaPods.
