# Changelog

## 0.4.0 - 2026-07-28

> 공식 nap mx 가이드(Android BOM 2026.07.06 / iOS 2.4.2) 기준 네이티브 SDK 상향 및 문서 정정.
> Native SDK refresh against the current official nap mx guides, plus documentation corrections.

### Changed
- **Android 네이티브 SDK 상향** / Bumped the Android native SDK:
  - BOM `2026.07.03 → 2026.07.06`, Core (`admixer-ssp`) `2.1.1 → 2.1.3`, AdManager (`admixer-admanager`) `2.0.2 → 2.0.4`. 나머지 어댑터는 변경 없음.
- **iOS 네이티브 SDK 상향** / Bumped the iOS native SDK:
  - `AdMixerMediation` XCFramework `2.3.7 → 2.4.2` (`ios/Package.swift` URL + checksum 갱신). 2.4.0 `loadAd` API 개선, 2.4.1 시뮬레이터 실행 이슈 수정, 2.4.2 안정성 개선 및 어댑터 갱신 반영.
  - ⚠️ Xcode 빌드 검증은 아직 수행하지 않았습니다 — 배포 전 iOS 타깃 빌드/스모크 테스트 필요. / Not yet build-verified with Xcode; verify before shipping.
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
