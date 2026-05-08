# Changelog

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
