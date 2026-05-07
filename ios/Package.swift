// swift-tools-version:5.6
import PackageDescription

// AdMixerMediation XCFramework coordinates.
// Latest verified SPM/CocoaPods core release: 2.3.3
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.3.xcframework.zip"
let adMixerMediationChecksum = "b40eb8ae2eff354e56de68ad11de0030002d17ba66a48b2df2bad461c1a6049f"

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
