// swift-tools-version:5.6
import PackageDescription

// AdMixerMediation XCFramework coordinates.
// 최신 코어: 2.3.7 (공식 Swift Package 릴리스 Nasmedia-Tech/iOS-SSP-Mediation-SPM 에서 배포).
// 전면 popup/countDown 타입 제거 + 뷰형 load API 추가 + NAM 어댑터 v1.1.0 반영.
// CocoaPods 사용자는 podspec 의 버전 미고정 의존성으로 동일하게 최신 pod 를 받습니다.
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip"
let adMixerMediationChecksum = "8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f"

let package = Package(
    name: "NapSspPluginSPM",
    platforms: [.iOS(.v14)],
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
