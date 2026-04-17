# Release notes

## 0.1.1 overview
This update moves the project beyond an initial scaffold and into a usable React Native integration sample for Nap SSP across Android and iOS.

### Included in 0.1.1
- JS/TS API for initialization, banner, native, inline video, interstitial, rewarded, and interstitial video ads
- Android native package updates for modules and view managers across supported ad formats
- iOS native bridge updates for modules and view managers across supported ad formats
- Example host app (`example/ExampleHostApp`) for local validation
- iOS SPM support and expanded native integration documentation
- Example Jest smoke test and improved local run scripts

### Verified in this repository
- Root package typecheck and build via `npm run verify`
- Example host app Jest smoke test via `cd example/ExampleHostApp && npm test -- --runInBand`

### Known environment requirements
- Android local execution requires Java/JDK, Android SDK, and `adb`
- iOS local execution requires macOS, Xcode, and CocoaPods

### Remaining cautions before wider release
- Confirm real-device ad loading with valid production or test ad unit IDs
- Verify mediation-specific SDK dependencies in host apps for both Android and iOS
- Keep package, podspec, changelog, and release metadata version-aligned before publishing the next release
