# Release notes

## 0.1.2 overview
This release packages the repository in a more reproducible state, with the full example host app committed and release metadata aligned for the next publish step.

### Included in 0.1.2
- Full `example/ExampleHostApp` React Native host app committed to the repository
- Refreshed changelog and release notes to match the current project state
- `InterstitialAdOptions` restored for the public TypeScript API
- Example rewarded flow aligned with the current event contract
- Example Android and iOS helper scripts improved with prerequisite checks
- Package metadata, README, and podspec version alignment to `0.1.2`

### Verified in this repository
- Root package typecheck and build via `npm run verify`
- Example host app Jest smoke test via `cd example/ExampleHostApp && npm test -- --runInBand`

### Known environment requirements
- Android local execution requires Java/JDK, Android SDK, and `adb`
- iOS local execution requires macOS, Xcode, and CocoaPods

### Remaining cautions before wider release
- Confirm real-device ad loading with valid production or test ad unit IDs
- Verify mediation-specific SDK dependencies in host apps for both Android and iOS
- Run final npm publish steps only after registry credentials are prepared
