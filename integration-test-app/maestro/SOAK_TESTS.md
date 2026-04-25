# Maestro Soak Test Cases and Process

## Covered flows

### Android
- Flow file: `android-ad-validation.yaml`
- Device target: `emulator-5554`
- Steps:
  1. launch app with cleared state
  2. wait for `SDK 초기화`
  3. initialize SDK and confirm `SDK: success`
  4. exercise banner load/unload
  5. exercise native ad load
  6. exercise inline video load/reload path
  7. exercise interstitial popup path
  8. exercise rewarded video path
  9. exercise interstitial video path
  10. scroll to and verify `이벤트 로그`

### iOS
- Flow file: `ios-ad-validation.yaml`
- Simulator target: `iPhone 17 Pro` (`8D90B616-14A9-4A49-A1A7-0470FF80A9F9`)
- Steps:
  1. launch app with cleared state
  2. wait for `SDK 초기화`
  3. initialize SDK and confirm first ad section is visible
  4. exercise banner load/unload
  5. exercise native ad load
  6. exercise inline video load/reload path
  7. exercise interstitial popup path
  8. exercise rewarded video path
  9. exercise interstitial video path
  10. scroll to and verify `이벤트 로그`

## 1-hour soak runners
- Android: `android-soak-60m.sh`
- iOS: `ios-soak-60m.sh`

Both runners:
- create a timestamped output directory under `integration-test-app/maestro/results/`
- store per-iteration logs as `run-N.log`
- store a per-run `summary.txt`
- append final run summaries into cumulative history:
  - `integration-test-app/maestro/results/maestro-soak-history.md`

## Execution notes
- Android debug flow requires Metro on port `8081` and `adb reverse tcp:8081 tcp:8081`
- iOS flow requires explicit Maestro simulator targeting with `--device <UDID>`
- iOS runner assumes the simulator app has already been built to:
  - `/tmp/NapSspIntegrationDerivedData/Build/Products/Debug-iphonesimulator/IntegrationTestApp.app`
- Failure artifacts are captured when possible:
  - Android: logcat + screenshot
  - iOS: simulator screenshot
