// swift-tools-version:5.6
import PackageDescription

// AdMixerMediation XCFramework coordinates.
// 최신 코어: 2.4.2 (공식 Swift Package 릴리스 Nasmedia-Tech/iOS-SSP-Mediation-SPM 에서 배포).
// 2.4.0 loadAd API 개선 · 2.4.1 시뮬레이터 실행 이슈 수정 · 2.4.2 안정성 개선 + Teads 어댑터(TeadsSDK 6.2+) 반영.
// CocoaPods 사용자는 podspec 의 버전 미고정 의존성으로 동일하게 최신 pod 를 받습니다.
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.4.2/AdMixerMediation2.4.2.xcframework.zip"
let adMixerMediationChecksum = "5e98227d00ed2c97e825fc2882bedee48d60da14d61af31045af2dee7cf4ce01"

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
