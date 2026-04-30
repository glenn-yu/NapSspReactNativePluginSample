// swift-tools-version:5.6
import PackageDescription

// AdMixerMediation XCFramework coordinates.
// Latest verified SPM/CocoaPods core release: 2.3.2
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.2.xcframework.zip"
let adMixerMediationChecksum = "78c7edb00263cade63925a2e91eddfa9cc84b4a2b73c2036b87b45e97cf43ca7"

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
