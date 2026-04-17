# Changelog

## 0.1.1

### Added
- Expanded React Native API surface for six ad formats: banner, native, inline video, interstitial, rewarded, and interstitial video
- Android native modules and view managers for banner, native, video, interstitial, rewarded, and interstitial video flows
- iOS native modules and view managers for banner, native, video, interstitial, rewarded, and interstitial video flows
- Example host app under `example/ExampleHostApp` with SDK test UI and platform run scripts
- iOS Swift Package Manager support via `ios/Package.swift`
- Native integration reference docs for Android and iOS vendor SDK usage
- Jest smoke-test setup for the example host app

### Changed
- Restored `InterstitialAdOptions` type support used by the public interstitial API
- Aligned rewarded example usage with the current event contract
- Improved Android and iOS example run scripts with prerequisite checks
- Updated README wording to better reflect current implementation maturity
- Synced iOS podspec version to `0.1.1`

### Verified
- `npm run verify`
- `cd example/ExampleHostApp && npm test -- --runInBand`

### Known limitations
- Android example execution requires a local JDK/Java runtime and Android SDK setup
- iOS example execution requires CocoaPods and Xcode command line tools
- Real ad loading behavior still depends on valid media keys, ad unit IDs, and vendor SDK installation in the host app
