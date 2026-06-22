# Version Matrix (버전 매트릭스)

> KO: 이 문서는 `react-native-nap-ssp` 플러그인 버전과 대응하는 Android/iOS 네이티브 SDK 버전 정보를 관리합니다.
> EN: This document tracks the `react-native-nap-ssp` plugin versions and their corresponding Android/iOS native SDK versions.

---

## 1. SDK 버전 호환성 (SDK Version Compatibility)

| Plugin 버전 (version) | Android Core SDK | iOS Core SDK | 주요 변경 사항 (Key changes) |
| :--- | :--- | :--- | :--- |
| **0.2.0** | `2.0.0` | `2.3.7` | Android v2 마이그레이션 + NaverAdManager/Teads, iOS 2.3.7 (Android v2 migration + NaverAdManager/Teads, iOS 2.3.7) |
| **0.1.8** | `1.0.23` | `2.3.3` | 버전 통일, 공개 가이드 문서 npm 배포 포함 (version alignment, public guide docs in npm) |
| **0.1.3** | `1.0.23` | `2.3.3` | iOS SPM 2.3.3 업데이트, 스레드 안전성 수정 (iOS SPM 2.3.3, thread-safety fix) |
| **0.1.2** | `1.0.23` | `2.3.2` | 전면형 API 통일, Maestro 검증 강화 (interstitial API alignment, Maestro hardening) |
| **0.1.1** | `1.0.21` | `2.2.0` | 네이티브 전 포맷 브릿지 연결 (all-format native bridge) |
| **0.1.0** | `1.0.18` | `2.1.0` | 초기 스캐폴딩 및 배너/전면 지원 (initial scaffold, banner/interstitial) |

---

## 2. 미디에이션 어댑터 버전 (Mediation Adapter Versions — 0.2.0 기준 / as of 0.2.0)

### Android (Maven Central: `io.github.nasmedia-tech`)

| 어댑터 (Adapter) | artifact ID | 버전 (Version) |
| :--- | :--- | :--- |
| **Core** | `admixer-ssp` | `2.0.0` |
| **Google AdManager** | `admixer-admanager` | `2.0.0` |
| **Kakao AdFit** | `admixer-adfit` | `2.0.0` |
| **Pangle** | `admixer-pangle` | `2.0.0` |
| **AppLovin** | `admixer-applovin` | `2.0.0` |
| **UnityAds** | `admixer-unity` | `2.0.0` |
| **Naver AdManager** (신규/new) | `admixer-naveradmanager` | `2.0.0` |
| **Teads** (신규/new) | `admixer-teads` | `2.0.0` |
| **Ads Identifier** | `play-services-ads-identifier` | `18.2.0` |

> KO: Android 어댑터별 최소 API 레벨: Core/AdFit/Teads = 21, AdManager/Pangle/Unity/NaverAdManager = 23, AppLovin = 24. (플러그인 minSdkVersion 21)
> EN: Minimum API level per Android adapter: Core/AdFit/Teads = 21, AdManager/Pangle/Unity/NaverAdManager = 23, AppLovin = 24. (Plugin minSdkVersion 21.)

> KO: AdManager의 `play-services-ads`는 **25.2.0 상한**입니다(force 권장, 25.3.0+ 비호환).
> EN: AdManager's `play-services-ads` is **capped at 25.2.0** (force recommended; 25.3.0+ incompatible).

> KO: Android 미디에이션 어댑터는 추가 리포지토리가 필요합니다:
> EN: Android mediation adapters require these additional repositories:
> - `https://devrepo.kakao.com/nexus/content/groups/public/` (AdFit)
> - `https://artifact.bytedance.com/repository/pangle/` (Pangle)
> - `https://sdk.teads.tv/android/repo` (Teads)
> - `https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod` (Teads)

### iOS (CocoaPods / SPM)

> KO: **iOS에는 Teads 어댑터가 없습니다.**
> EN: **There is no Teads adapter on iOS.**

| 어댑터 (Adapter) | Pod 이름 (Pod name) | 버전 (Version) |
| :--- | :--- | :--- |
| **Core** | `AdMixerMediation` | `2.3.7` |
| **Google AdManager** | `AdMixerMediationGAM` | `1.1.0` |
| **Kakao AdFit** | `AdMixerMediationAdFit` | `1.1.0` |
| **Pangle** | `AdMixerMediationPangle` | `1.1.0` |
| **Naver AdManager** (신규/new) | `AdMixerMediationNAM` | `1.1.0` |
| **AppLovin** | `AdMixerMediationAppLovin` | CocoaPods 자동 최신 (latest via CocoaPods) |
| **UnityAds** | `AdMixerMediationUnityAds` | CocoaPods 자동 최신 (latest via CocoaPods) |

### 번들 네트워크 SDK 버전 범위 (Bundled Network SDK Version Ranges — iOS)

| 어댑터 (Adapter) | 네트워크 SDK (Network SDK) | 범위 (Range) |
| :--- | :--- | :--- |
| `AdMixerMediationGAM` | Google-Mobile-Ads-SDK | `12.7.0` ~ `12.14.1` |
| `AdMixerMediationAdFit` | AdFitSDK | `3.14.7` ~ `3.18.6` |
| `AdMixerMediationPangle` | Ads-Global | `7.4.0.8` ~ `7.8.8.9` |
| `AdMixerMediationUnityAds` | UnityAds | `4.15.1` ~ `4.16.6` |
| `AdMixerMediationAppLovin` | AppLovinSDK | `13.3.1` ~ `13.5.2` |

> KO: 기존 사용 중인 네트워크 버전이 있으면 매체 버전과 nap mx 버전 중 더 낮은 버전으로 탑재되며, 없으면 범위 내 최신 버전으로 탑재됩니다.
> EN: If a network version is already in use, the lower of your version and the nap mx version is used; otherwise the latest within range is used.

### iOS SPM Binary Target

| 항목 (Item) | 값 (Value) |
| :--- | :--- |
| **버전 (version)** | `2.3.7` |
| **URL** | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip` |
| **SHA256 Checksum** | `8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f` |

> KO: 2.3.5+ 버전은 공식 SPM 릴리스 채널로만 배포됩니다(레거시 `iOS-AdMixerDownload` 바이너리 채널은 2.3.4까지).
> EN: Versions 2.3.5+ are distributed only via the official SPM release channel (the legacy `iOS-AdMixerDownload` binary channel only goes up to 2.3.4).

---

## 3. 업데이트 가이드 (Update Guide)

> KO: 플러그인 버전을 올릴 때에는 `build.gradle` 및 `Podfile`/`Package.swift`에서 해당 네이티브 SDK 버전도 함께 업데이트해야 할 수 있습니다. 상세 내역은 [CHANGELOG.md](../CHANGELOG.md)를 참조하십시오.
> EN: When bumping the plugin version, you may also need to update the corresponding native SDK versions in `build.gradle` and `Podfile`/`Package.swift`. See [CHANGELOG.md](../CHANGELOG.md) for details.
