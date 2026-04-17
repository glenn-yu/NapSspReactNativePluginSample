// swift-tools-version:5.6
import PackageDescription

let package = Package(
    name: "NapSspPluginSPM",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "NapSspPlugin", targets: ["NapSspPlugin"]),
    ],
    targets: [
        .target(
            name: "NapSspPlugin",
            path: "./",
            exclude: ["NapSspPlugin.podspec"],
            sources: ["NapSspModule.swift","NapSspSupport.swift","BannerView.swift","BannerViewManager.swift","InterstitialModule.swift","RewardedModule.swift","NapSspBridge.m"],
            publicHeadersPath: "."
        ),
        // If you prefer to reference the vendor binary via SPM, consumers can add a binaryTarget in their Package.swift:
        // .binaryTarget(name: "AdMixerMediation", url: "https://.../AdMixerMediation.xcframework.zip", checksum: "...")
    ]
)
