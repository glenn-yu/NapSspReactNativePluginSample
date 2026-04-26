// swift-tools-version:5.6
import PackageDescription

// AdMixerMediation XCFramework coordinates.
// Replace url and checksum with the official release when available.
// Current version: 2.2.1
let adMixerMediationURL = "https://github.com/nasmedia-tech/admixer-ios-sdk/releases/download/2.2.1/AdMixerMediation.xcframework.zip"
let adMixerMediationChecksum = "REPLACE_WITH_OFFICIAL_CHECKSUM"

let package = Package(
    name: "NapSspPluginSPM",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "NapSspPlugin", targets: ["NapSspPlugin"]),
    ],
    dependencies: [],
    targets: [
        // Vendor SDK binary — consumers include this target to link AdMixerMediation via SPM
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
            exclude: ["NapSspPlugin.podspec"],
            sources: [
                "NapSspModule.swift",
                "NapSspSupport.swift",
                "BannerView.swift",
                "BannerViewManager.swift",
                "InterstitialModule.swift",
                "RewardedModule.swift",
                "NativeAdView.swift",
                "NativeAdViewManager.swift",
                "VideoAdView.swift",
                "VideoAdViewManager.swift",
                "InterstitialVideoModule.swift",
                "NapSspBridge.m"
            ],
            resources: [
                .process("AMMNativeAdView.xib")
            ],
            publicHeadersPath: "."
        ),
    ]
)
