# Changelog

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
