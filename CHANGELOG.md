# Changelog

## 0.5.0 - 2026-09-21

> 네이티브 SDK 최신화(Android core 2.3.0 / iOS 2.5.0)와 함께, 디버그 빌드에서 실광고를 가로채던
> placeholder 시뮬레이션을 전량 제거하고 공식 가이드의 개인정보·테스트 API 를 노출합니다.
> Refreshes both native SDKs, removes the placeholder simulation that intercepted real ads in debug
> builds, and exposes the privacy and test APIs from the official guide.

### ⚠️ BREAKING CHANGES

- **React Native 0.76 이상이 필요합니다** / **React Native 0.76+** is now required.
  - `admixer-ssp:2.3.0` 의 AAR 메타데이터가 **compileSdk 35 이상**을 요구합니다. 34 로 빌드하면
    `requires libraries and applications that depend on it to compile against version 35 or later`
    오류로 실패합니다. RN 0.76 부터 템플릿이 35(0.81 부터 36)로 컴파일합니다.
  - `peerDependencies.react-native` 를 `>=0.72.0` → `>=0.76.0` 으로 정정했습니다.
- **호스트 앱에 Kotlin 2.1 이상이 필요합니다** / The host app must build with **Kotlin 2.1+**.
  - nap mx core `admixer-ssp:2.3.0` 이 `kotlin-stdlib:2.2.10`(metadata 2.2.0)을 전이 의존으로 포함합니다.
    Kotlin 2.0 이하 컴파일러는 이 메타데이터를 읽지 못해 `Class 'kotlin.Unit' was compiled with an
    incompatible version of Kotlin` 오류로 빌드가 실패합니다.
  - React Native **0.79 이상은 기본값이 Kotlin 2.1.x** 라 추가 설정이 필요 없습니다.
    RN 0.72~0.78 은 앱의 최상위 `android/build.gradle` 에 `kotlinVersion = "2.1.21"` 을 지정하세요
    ([SETUP](./docs/SETUP.md#kotlin-요구사항)).
- **`napSsp.enableVendorSdk` 제거** / Removed the `napSsp.enableVendorSdk` Gradle flag.
  - 코어 SDK 가 항상 링크됩니다. 이 값이 남아 있으면 빌드 경고만 출력하고 무시합니다.
  - 미디에이션 어댑터는 `napSsp.mediations` 로 선택합니다(미지정 시 **코어만** 링크 — 종전에는
    `enableVendorSdk=true` + 미지정이면 전 어댑터가 포함되었습니다).
- **placeholder / 시뮬레이션 모드 전면 제거** / Removed placeholder ad simulation entirely.
  - 종전에는 디버그 빌드에서 `start()`·`show()`·배너/네이티브/동영상 뷰가 실 SDK 대신 가짜 이벤트를
    발행하거나, 로드 실패를 가짜 성공으로 덮었습니다. 이제 **모든 빌드에서 실 SDK 경로만** 사용하며
    실패는 실패로 통지됩니다.
  - 네이티브 모듈/뷰가 링크되지 않은 환경에서는 `onAdFailedToLoad` 로 `nap_ssp_view_not_linked` 가
    전달되고, 인라인 뷰는 빈 영역을 렌더링합니다(가짜 광고 카드를 그리지 않습니다).
- **`NapSspAd.setLogLevel()` / `setCoppa()` 가 `Promise` 를 반환합니다** / They now return promises.
  - 기존에는 `void` 였으며 **Android 에서는 인자 개수 불일치로 예외가 발생**해 동작하지 않았습니다.
- **`NapSspAd.initialize()` 가 `NapSspStatus` 를 resolve 합니다** / It now resolves the status object
  (이전에는 `undefined`).
- **`adUnitIds` 는 숫자 문자열만 허용합니다** / `adUnitIds` must be numeric strings, validated in JS.
- **미구현 TurboModule 스펙 파일 삭제** / Deleted the unimplemented TurboModule spec files
  (`src/NativeNapSspModuleSpec.ts`, `src/NativeNapSspInterstitialSpec.ts`). New Architecture 는
  검증되지 않았으므로 지원한다고 문서화하지 않습니다.

### Added

- **개인정보 동의 API** / Privacy consent API — `NapSspAd.setPrivacyConsent()` 및 `initialize({ privacy })`.
  - Android: `AdMixer.setTagForChildDirectedTreatment` / `setGdprConsent` / `setCcpaDoNotSell` / `setUsPrivacy`
  - iOS: `AMMConsent` + `AMMediation.shared.setConsent()`
  - 종전 `setCoppa()` 는 **양 플랫폼 모두 로컬 플래그만 저장하고 SDK 에 전달하지 않았습니다**(COPPA 미적용).
- **테스트 모드 API** / Test mode — `NapSspAd.setTestMode()` / `setTestDeviceIds()` 및 `initialize({ testMode, testDeviceIds })`.
  - Android 전용(`AdMixer.setTestMode` / `setTestDeviceIds`). iOS SDK 에는 전역 테스트 스위치가 없어
    `false` 를 resolve 합니다.
- **리워드 `transactionId`** / Reward `transactionId` — S2S 리워드 콜백의 `transaction_id` 와 동일한 값으로
  앱–서버 지급 대조에 사용합니다. `type`/`amount` 는 SDK 가 제공하지 않는 값이라 deprecated 로 표시했습니다.
- **`isReady()` / `isLoading()`** — SDK 의 실제 상태를 조회합니다(AOS `isReady`/`isLoading`, iOS `isAdReady`).
- **인라인 뷰 `reload()`** — `ref` 로 배너·네이티브·동영상을 재요청합니다.
- **`start()` / `cancelLoad()` / `isLoaded()` iOS 네이티브 구현** — 종전에는 iOS 브릿지에 없어
  `start()` 는 JS 폴백, `cancelLoad()` 는 무동작이었습니다.
- **네트워크별 키 주입** / Per-network key injection — `mediations.pangle.appId` / `mediations.appLovin.sdkKey` 가
  Android 에서 `AdInfo.Builder.setAdapterConfig()` 로 전달됩니다(서버 값 우선).
- **에러 코드 매핑** — 네이티브 코드를 안정적인 문자열 코드로 변환하고 원본을 `nativeCode` 로 함께 전달합니다.

### Fixed

- **iOS 에러 코드 전량 오매핑 수정** / Fixed the iOS error-code mapping.
  - 플러그인이 `0~6` 을 가정했으나 실제 SDK/가이드는 `-1 ~ -8` 입니다. 모든 iOS 광고 오류가
    `napssp_unknown` 으로 보고되던 문제를 수정하고 `NSUnderlyingErrorKey` 를 `details` 로 전달합니다.
- **Android 리워드 `customParams` 무음 소실 수정** / Fixed silently dropped rewarded `customParams`.
  - `getMethod("setCustomParams", HashMap.class)` 로 조회했으나 실제 시그니처는 `Map` 이라
    `NoSuchMethodException` 이 잡혀 사라졌습니다(S2S 커스텀 파라미터 전달 불가).
- **Android 전면 `closeButtonTouchAreaRatio` 미적용 수정** / The documented interstitial option was never
  applied on Android (`applyInterstitialOptions` 가 빈 함수였습니다). 이제 `setCloseButtonBound()` 로 전달합니다.
- **iOS 죽은 델리게이트 제거** / Removed dead iOS delegate methods.
  - `onRewardVideoSkipped()` / `onSkipVideoInterstitial()` 는 실제 프로토콜에 없는 메서드라 skip 이벤트가
    영구히 발생하지 않았습니다. iOS 에 skip 콜백이 없다는 사실을 타입과 문서에 명시했습니다.
- **배너 가짜 클릭 시뮬레이터 제거** / Removed the banner tap simulator that emitted
  `clicked`/`opened`/`closed` on any container tap, duplicating real SDK clicks.
- **죽은 리플렉션 호출 제거** / Removed dead reflection — `AdInfo.Builder.setIsUseMediation` 은 SDK 에
  존재하지 않습니다(3곳에서 호출 후 예외를 삼키고 있었습니다).
- **Android R 클래스 충돌 위험 제거** / The library namespace moved to `com.nasmedia.admixerssp.reactnative`
  so its generated `R`/`BuildConfig` no longer collide with the vendor SDK's own `com.nasmedia.admixerssp`
  package. 공개 클래스명(`NapSspPackage` 등)은 그대로입니다.
- **중복 podspec 제거** / Deleted the stale `ios/NapSspPlugin.podspec` (v0.2.0) that shadowed the root one.
- **JVM 타깃 불일치 수정** / Fixed a JVM-target mismatch — the library compiled Kotlin at 11 while
  modern React Native hosts compile Java at 17, failing with
  `Inconsistent JVM-target compatibility detected`. Both now default to 17 (`napSsp.javaVersion`
  overrides it).
- **미디에이션 Maven 저장소 안내 수정** / Fixed the mediation repository guidance. The library declared
  the Kakao/ByteDance/Teads repositories in its own `repositories {}` block, which does nothing for
  the host: Gradle resolves a library's *transitive* dependencies with the **consuming** project's
  repositories, so AdFit/Pangle/Teads failed with `Could not find tv.teads.sdk.android:sdk`.
  The build now prints the exact repositories to add to the app, and the docs say so.
- **`loadAd()` 무음 무시로 promise 가 영구 대기하던 문제 수정** / Fixed a hang: the SDK drops a
  `loadAd()` re-request **without a callback** when an ad is already READY or a load is in flight,
  so a caller waiting on the callback waited forever. The bridge now checks `isReady()`/`isLoading()`
  before requesting. (The official guide warns about exactly this.)
- **`package.json` 의 객체형 `react-native` 필드 제거** — Metro 의 `resolverMainFields` 는 문자열 진입점을
  기대하므로 해석을 방해할 수 있었습니다(`react-native.config.js` 가 이미 sourceDir 을 선언합니다).

### Changed

- **Android 네이티브 SDK 상향** — BOM `2026.07.06 → 2026.09.03`, core `2.1.3 → 2.3.0`,
  admanager `2.0.4 → 2.1.3`, adfit `2.0.3 → 2.0.6`, pangle `2.0.2 → 2.1.2`, applovin `2.0.2 → 2.0.5`,
  unity `2.0.2 → 2.0.6`, naveradmanager `2.0.2 → 2.1.3`, teads `2.1.0 → 2.1.2`.
- **iOS 네이티브 SDK 상향** — `AdMixerMediation` `2.4.2 → 2.5.0`.
- **Android 브릿지를 리플렉션에서 직접 호출로 전환** / Replaced reflection with direct SDK calls, so a
  signature change now breaks the build instead of failing silently at runtime.
- **iOS deprecated API 마이그레이션** — `load(...)` → `loadAd(...)`, `onTapX` → `onClickX`,
  `onSuccessBanner` → `onSuccessShowBanner`, `onRewardVideoEarned()` → `onRewardVideoEarned(rewardInfo:)`.
- **전면/리워드/전면동영상 3종을 공통 구현으로 통합** — Android `NapSspFullScreenAdHost`,
  TypeScript `FullScreenAd` 기반 클래스로 중복 약 1,400 줄을 제거했습니다.
- **`peerDependencies.react`** `^18.2.0` → `>=18.2.0` (React 19 / RN 0.78+ 설치 차단 해소).
- **Gradle 기본 Kotlin** `1.8.22` → `2.1.21` (`napSsp.kotlinVersion` 으로 재정의 가능).
- **예제 앱을 연동 레퍼런스로 재작성** — 포맷별 initialize → load → callback → show → reload → cleanup
  전 구간과 이벤트 로그를 화면에서 확인할 수 있습니다.
- **smoke test 강화** — 11개 계약 검증(인자 검증, 이벤트 라우팅/필터링, show 단계 실패 처리, 리워드 payload 등).

### Removed

- `@types/react-native` devDependency (RN 0.71 부터 타입이 본체에 포함되며 해당 패키지는 폐기되었습니다).
- `MediationConfig.mobwith` — 공식 지원 네트워크 목록에서 제외된 지 오래된 no-op 필드.

### Verification

| 항목 | 결과 |
|---|---|
| Android 라이브러리 컴파일 (Kotlin 2.1.21 / AGP 8.5.2 / RN 0.76.5, 미디에이션 7종 링크) | ✅ PASS |
| Android 호스트 앱 전체 빌드 — 리소스·매니페스트 병합 + dex + APK (AGP 8.7.2 / compileSdk 35, 미디에이션 7종) | ✅ PASS |
| 예제 앱(React Native 0.81.6) Android 빌드 | ✅ PASS |
| 예제 앱 typecheck / Jest | ✅ PASS |
| **실기기 런타임 (Galaxy SM-S947N / Android 16)** | ✅ PASS — `AdMixerSDK::2.3.0` 초기화, adUnit 5종 등록, 어댑터(GoogleAdManager·AdFit) 자동 탐색, media-conf 수신, **배너·인라인 동영상 실광고 loaded + impression**, 네이티브는 `nap_ssp_no_ads (native -2147483640)` 실제 no-fill 로 정상 통지 |
| TypeScript typecheck + build | ✅ PASS |
| smoke test (11 checks) | ✅ PASS |
| iOS 빌드 | ⛔ NOT_MEASURED — 개발 환경이 Windows 이며 Xcode 가 없습니다. API 대조는 SDK 2.5.0 의 `.swiftinterface` 전수 비교로 수행했습니다. |
| New Architecture | ⛔ NOT_MEASURED — 지원한다고 주장하지 않습니다. |

## 0.4.0 - 2026-07-28

> 공식 nap mx 가이드(Android BOM 2026.07.06 / iOS 2.4.2) 기준 네이티브 SDK 상향 및 문서 정정.
> Native SDK refresh against the current official nap mx guides, plus documentation corrections.

### Changed
- **Android 네이티브 SDK 상향** / Bumped the Android native SDK:
  - BOM `2026.07.03 → 2026.07.06`, Core (`admixer-ssp`) `2.1.1 → 2.1.3`, AdManager (`admixer-admanager`) `2.0.2 → 2.0.4`. 나머지 어댑터는 변경 없음.
- **iOS 네이티브 SDK 상향** / Bumped the iOS native SDK:
  - `AdMixerMediation` XCFramework `2.3.7 → 2.4.2` (`ios/Package.swift` URL + checksum 갱신). 2.4.0 `loadAd` API 개선, 2.4.1 시뮬레이터 실행 이슈 수정, 2.4.2 안정성 개선 및 어댑터 갱신 반영.
  - 양 버전 XCFramework 에 동봉된 `.swiftinterface`(device·simulator 슬라이스) 를 diff 하여 플러그인이 참조하는 전체 심볼의 소스 호환성을 검증. / Source compatibility verified by diffing the bundled `.swiftinterface` of both versions across device and simulator slices.
  - ⚠️ Xcode 전체 빌드는 미수행 — 배포 전 iOS 타깃 스모크 테스트 권장. / Full Xcode build not run; smoke-test before shipping.

### Fixed
- **iOS 전면 동영상 로드 컴파일 오류 수정 (2.4.2 대응)** / Fixed an iOS interstitial-video compile break under SDK 2.4.2:
  - `AMMVideoInterstitial.load(adUnitID:completion:)` 의 2-인자 클로저(`@nonobjc`) 오버로드가 2.4.2 에서 제거되어, 양 버전에 동일하게 존재하는 3-인자 `(videoInterstitial, adapterName, error)` 오버로드로 전환 (`ios/InterstitialVideoModule.swift`).
- **문서 전면 정정** / Corrected all guides:
  - v0.3.0 문서가 실제로 export 되지 않는 `initSdk()` / `setAdapterConfig()` API 와 잘못된 이벤트명(`onAdLoaded` 등), 잘못된 `AdError` 형태를 안내하고 있던 문제를 수정. 실제 export(`NapSspAd.initialize()`, 축약 이벤트명 `loaded`/`loadFailed`/…, `AdError { code, message, nativeCode?, nativeDomain?, details? }`) 기준으로 재작성.
  - `MIGRATION.md` 버전 매트릭스에서 Google Ad Manager 아티팩트를 `admixer-gma-nextgen` → `admixer-admanager` 로 정정(GMA NextGen 은 beta 이며 AdManager·NaverAd 와 공존 불가).
  - Android 네트워크별 최소 API/Kotlin 요구사항, `play-services-ads` 25.2.0 상한, Google App ID meta-data, `networkSecurityConfig` 병합 충돌 해결법, 16KB 페이지 정렬 등 공식 가이드 항목 추가.

### Added
- **iOS Teads 어댑터 지원** / Added iOS Teads support:
  - `NapSspPlugin.podspec` 에 `Teads` subspec(`AdMixerMediationTeads`) 추가. 기존에는 Android 전용이었습니다.
- **Huawei Maven 저장소 추가** / Added the Huawei Maven repository:
  - Teads 공식 설치 가이드가 Huawei 단말 호환을 위해 요구하는 `https://developer.huawei.com/repo/` 를 `android/build.gradle` 에 추가.

### Deprecated
- `MediationConfig.mobwith` — 공식 가이드의 지원 네트워크 목록에서 제외되었습니다. 타입은 유지되나 무시됩니다. / No longer a supported nap mx network; the field remains for compatibility but is ignored.

## 0.3.0 - 2026-07-27

> 공식 nap mx SDK 최신 가이드 기준 동기화 및 팩트체크 마이그레이션 / Sync and fact-checked migration with latest nap mx SDK guides.

### Changed
- **Android SDK BOM 도입 및 버전 상향** / Adopted Android SDK BOM (`io.github.nasmedia-tech:admixer-bom:2026.07.03`):
  - Core (`admixer-ssp`) `2.0.0 → 2.1.1`, AdManager `2.0.0 → 2.0.2`, AdFit `2.0.0 → 2.0.3`, Pangle/AppLovin/Unity/NaverAd `2.0.0 → 2.0.2`, Teads `2.0.0 → 2.1.0`.
- **네이티브 광고 View ID 표준화** / Updated Native Ad View ID prefixes to official standard:
  - 타 라이브러리와의 리소스 충돌 방지 및 공식 규약에 따라 `nap_ssp_native_*` → `nap_mx_*` (`nap_mx_tv_title`, `nap_mx_iv_icon`, `nap_mx_tv_adv`, `nap_mx_tv_desc`, `nap_mx_iv_main`, `nap_mx_btn_cta`) 로 갱신.
- **Android 리스너 콜백 표준화** / Aligned Android listener callbacks with v2.1.1 standard:
  - 수신 실패 콜백 `onFailedToReceiveAd(int code, String msg)` 2-인자 표준 콜백 래핑 지원 추가.

### Added
- **진행 중 로드 취소 API (`cancelLoad`) 추가** / Added `cancelLoad()` API for fullscreen ads:
  - 표시 중인 광고에는 영향을 주지 않고 진행 중인 로드 작업만 취소할 수 있는 `cancelLoad()` 메서드를 전면(`InterstitialAd`), 리워드(`RewardedAd`), 전면 동영상(`InterstitialVideoAd`) 모듈에 추가.

## 0.2.0 - 2026-06-22

> 메이저 네이티브 SDK 업그레이드 / Major native SDK upgrade.

### Changed
- **Android 벤더 SDK v1.x → v2.0.0 마이그레이션** / migrated Android vendor SDK to v2.0.0:
  - 모든 `io.github.nasmedia-tech:admixer-*` 좌표를 `2.0.0` 으로 상향, `play-services-ads-identifier` `18.3.0 → 18.2.0`.
  - 클래스명 변경 흡수(AdView→AMMBannerView, InterstitialAd→AMMInterstitial, RewardInterstitialVideoAd→AMMRewardVideo, InterstitialVideoAd→AMMVideoInterstitial, NativeAdView→AMMNativeAdView, VideoAdView→AMMVideoView) — 플러그인 내부 reflection 처리.
  - `AdListener` interface→abstract class 전환에 맞춰 이름 있는 콜백(`onAdDisplayed/Clicked/Closed/Completed/Skipped/Rewarded` + `onAdShowFailed`)으로 브리지 재구성 (`NapAdListener`/`NapListenerBridge` 추가).
  - `registerAdapter()` 수동 등록 제거(자동 등록).
  - 라이프사이클 정리 메서드 `onDestroy()` → `destroy()`.
- **iOS 벤더 SDK 2.3.3 → 2.3.7** (`ios/Package.swift` binaryTarget + checksum 갱신), 최소 배포 타겟 `13.0 → 14.0` 정렬.

### Added
- **NaverAdManager·Teads 미디에이션 추가** / added NaverAdManager & Teads mediation:
  - Android: `admixer-naveradmanager:2.0.0`, `admixer-teads:2.0.0` (Teads Maven 저장소 포함).
  - iOS: `AdMixerMediationNAM` podspec subspec 추가 (iOS 는 Teads 미지원).
  - JS: `MediationConfig.naverAdManager?`, `MediationConfig.teads?` 추가.
- 모든 가이드 문서를 **한국어+영어 2개 언어** 로 갱신, `RELEASE_NOTES.md` 추가.

### Fixed
- 인라인 뷰(배너/네이티브/비디오)의 `setAdViewListener` reflection 파라미터 타입 버그 수정 — v2 시그니처가 `Object` 인데 `AdListener` 로 조회하여 `NoSuchMethodException` 으로 광고 로드가 실패하던 문제 해결. / Fixed inline-view `setAdViewListener` reflection (v2 signature is `Object`, not `AdListener`), which broke ad loading.
- iOS 전면 광고 popup/countDown 옵션 제거(2.3.7 에서 SDK 제거) — `AMMInterstitialConfig` 컴파일 오류 해소, **Basic 전용**. / Removed iOS interstitial popup/countDown usage removed in 2.3.7 (Basic-only).
- JS `InterstitialAdOptions` 정리: 무효 옵션(type/countDownTime/buttonLeftText/buttonRightText) 제거, `closeButtonTouchAreaRatio`(iOS) 만 유지.

### Removed
- 커밋되어 있던 빌드 산출물 `example/.../index.android.bundle` 추적 해제(.gitignore 추가). / Untracked the committed `index.android.bundle` build artifact.

---

## 0.1.8 - 2026-05-08

### Fixed
- `BANNER_0x0` 등 너비/높이가 0인 배너 사이즈를 무효 처리하고 기본값(320×50)으로 폴백 (JS/iOS/Android).
- Android: `size` prop 변경 시 이미 광고가 로드된 상태(`LOADED`)면 새 사이즈로 자동 재로드.

### Added
- `docs/test/`: Android 패키지명 검증, 배너 사이즈, 신규 설치, 전 광고 타입 회귀 테스트 가이드 4종 추가.

---

## 0.1.7 - 2026-05-07

### Changed
- 버전을 0.1.7로 통일 (0.1.3/0.1.4 문서 불일치 해소).
- npm 배포 패키지 `docs/` 목록을 공개 가이드 15개로 정리
  (DEVELOPMENT, PUBLISH_GUIDE, MAESTRO_GUIDE 등 내부 문서 제외).

---

## 0.1.4 - 2026-05-07

### Added
- npm 배포 패키지에 가이드 문서 18개 포함 (`docs/` 중 사용자 대상 파일만 선별):
  API_REFERENCE, ANDROID_SETUP, IOS_SETUP, MEDIATION_GUIDE, SPM_GUIDE, MIGRATION_GUIDE,
  VERSION_MATRIX, TROUBLESHOOTING, FAQ, ADVANCED_USAGE, NATIVE_ASSETS_GUIDE, PRIVACY_GUIDE,
  EXPO_GUIDE, GLOSSARY, ROADMAP, MAESTRO_GUIDE, DEVELOPMENT, PUBLISH_GUIDE.

### Changed
- `package.json` `files`: 개별 docs 파일 명시로 내부 개발 문서(MAESTRO 실행 로그, REVIEW 보고서, png 이미지, docx 등) 배포 제외.

---

## 0.1.3 - 2026-05-07

### Changed
- iOS SPM: AdMixerMediation XCFramework 2.3.2 → 2.3.3 업데이트 (checksum 갱신).
- iOS podspec: 버전 0.1.2 → 0.1.3 (NapSspPlugin.podspec 전체 반영).

### Fixed
- iOS `destroy()` 스레드 레이스: RN 백그라운드 큐에서 `NapSspInterstitialDelegate.instances` 접근 → `DispatchQueue.main.async` 래핑으로 수정.
- iOS `InterstitialVideoModule`: `show()` 실패 시 pending reject 처리 및 `onVideoSkipped` 이벤트 추가.
- iOS `NapSspSupport`: `peekStoredInterstitialVideoDelegate()` 메서드 누락 추가.
- Android `NapSspBannerView`: 벤더 SDK 콜백 경로(`tryAttachVendorAdViewAndLoad`)에서 `markBannerState` 3곳 누락 추가 — `getStatus()` 오보 수정.
- `src/events.ts`: `setup()` 전에 등록된 이벤트 리스너를 소급 구독하는 로직 추가.
- `package.json` `files`: `android` → `android/src` + `android/build.gradle`로 세분화하여 빌드 아티팩트 npm 배포 제외 (패키지 2.1MB → 308KB).
- `NapSspPlugin.podspec` npm 배포 누락 수정 (`.npmignore` 추가).

---

## 0.1.2 - 2026-04-26

### 🚀 주요 변경 사항 (Release Notes 통합)
- **전면형 광고 API 통일**: `InterstitialVideoAd`에 `start()` 메서드를 추가하여 모든 전면 포맷의 호출 패턴(`load`+`show`)을 일원화.
- **Maestro 검증 시스템 고도화**: 광고 로드 타임아웃 상향(30s), 리워드 보상 이벤트 검증 로직 추가 및 플랫폼별 로그 형식 통일.
- **네이티브 안정성**: Android API 33+ `AD_ID` 권한 추가 및 iOS ATT 설정 가이드 보강.

### Added
- Added the full `ExampleHostApp` React Native test application for reproducible local validation.
- Restored `InterstitialAdOptions` type support for public API consistency.
- Expanded `integration-test-app` to include more format demos and robust error handling.

### Changed
- Refreshed documentation structure: All guides are now centralized in the `docs/` directory.
- Synced package metadata, README, and iOS podspec to `0.1.2`.
- Improved Android and iOS example run scripts with prerequisite checks.

### Fixed
- Fixed Metro dependency conflicts in `integration-test-app` when referencing local plugin source.
- Improved Placeholder UI visibility for unlinked native components.

---

## 0.1.1 - 2026-04-20

### Added
- Expanded React Native API surface for six ad formats: banner, native, inline video, interstitial, rewarded, and interstitial video.
- Android/iOS native modules and view managers for all formats with reflection-based vendor loading.
- Example host app under `example/ExampleHostApp` with SDK test UI.
- iOS Swift Package Manager support via `ios/Package.swift`.
- Native integration reference docs for Android and iOS vendor SDK usage.
