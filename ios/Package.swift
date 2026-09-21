// swift-tools-version:5.9
import PackageDescription

// AdMixerMediation XCFramework coordinates — keep in sync with NapSspPlugin.podspec.
// Latest core: 2.5.0 (Nasmedia-Tech/iOS-SSP-Mediation-SPM release).
// The checksum is the SHA256 of the published xcframework zip.
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.5.0/AdMixerMediation2.5.0.xcframework.zip"
let adMixerMediationChecksum = "6c99935bb26118b051352c32eecf4b3404d1e9e8ff61471bed930eacad46b6cf"

let package = Package(
    name: "NapSspPluginSPM",
    // The core SDK supports iOS 13; the AdFit and Teads adapters require iOS 14.
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "NapSspPlugin", targets: ["NapSspPlugin"]),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "AdMixerMediation",
            url: adMixerMediationURL,
            checksum: adMixerMediationChecksum
        ),
        .target(
            name: "NapSspPlugin",
            dependencies: [
                .target(name: "AdMixerMediation"),
            ],
            path: "./",
            sources: [
                "NapSspModule.swift",
                "NapSspSupport.swift",
                "NapSspFullScreenRegistry.swift",
                "BannerView.swift",
                "BannerViewManager.swift",
                "InterstitialModule.swift",
                "InterstitialVideoModule.swift",
                "RewardedModule.swift",
                "NativeAdView.swift",
                "NativeAdViewManager.swift",
                "VideoAdView.swift",
                "VideoAdViewManager.swift",
                "NapSspBridge.m",
            ],
            resources: [
                .process("AMMNativeAdView.xib")
            ],
            publicHeadersPath: "."
        ),
    ]
)
