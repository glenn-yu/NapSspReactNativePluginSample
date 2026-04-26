# Maestro Setup and Run Guide

This guide explains how to install Maestro locally, prepare the integration test app, and run Android and iOS validation/soak tests.

## 1. Install prerequisites

### macOS tools
- Homebrew
- Node.js / npm
- Java (OpenJDK)
- Android Studio SDK / platform-tools
- Xcode + iOS Simulator

Recommended Java:

```bash
brew install openjdk
```

Recommended Android platform tools:

```bash
brew install android-platform-tools
```

### Maestro CLI
If Homebrew install is unreliable, use the official installer:

```bash
curl -fsSL https://get.maestro.mobile.dev | bash
```

Then reload your shell and verify:

```bash
maestro --version
which maestro
```

## 2. Prepare the repo

```bash
cd NapSspReactNativePluginSample
npm install
cd integration-test-app
npm install
```

## 3. Prepare Android app

Build the Android debug app:

```bash
cd integration-test-app/android
./gradlew assembleDebug
./gradlew installDebug
```

Make sure an emulator is running and visible in adb:

```bash
adb devices
```

Start Metro for debug builds:

```bash
cd integration-test-app
npx react-native start --port 8081
```

Reverse Metro port:

```bash
adb -s emulator-5554 reverse tcp:8081 tcp:8081
```

## 4. Prepare iOS app

Install pods:

```bash
cd integration-test-app/ios
pod install --repo-update
```

Build the simulator app:

```bash
xcodebuild \
  -workspace IntegrationTestApp.xcworkspace \
  -scheme IntegrationTestApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath /tmp/NapSspIntegrationDerivedData \
  build
```

Boot the simulator and install the app:

```bash
xcrun simctl boot 'iPhone 17 Pro'
xcrun simctl install booted /tmp/NapSspIntegrationDerivedData/Build/Products/Debug-iphonesimulator/IntegrationTestApp.app
xcrun simctl launch booted org.reactjs.native.example.IntegrationTestApp
```

## 5. Run one-shot validation

### Android
If Android Maestro transport seems stuck, do a clean restart first:

```bash
pkill -f maestro || true
adb kill-server
adb start-server
lsof -i :7001
```

Then run:

```bash
cd integration-test-app
maestro test maestro/android-ad-validation.yaml
```

### iOS
Use explicit simulator targeting:

```bash
cd integration-test-app
maestro --device <SIMULATOR_UDID> test maestro/ios-ad-validation.yaml
```

## 6. Run soak tests

### Android Maestro soak

```bash
cd integration-test-app/maestro
SOAK_DURATION_SECONDS=1800 ./android-soak-60m.sh
```

### iOS Maestro soak

```bash
cd integration-test-app/maestro
SOAK_DURATION_SECONDS=1800 ./ios-soak-60m.sh
```

### Android fallback soak
Use this if Android Maestro transport is unstable:

```bash
cd integration-test-app/maestro
SOAK_DURATION_SECONDS=1800 ./android-adb-soak-60m.sh
```

## 7. Stop-on-repeat behavior

The soak runners stop early if the same failure key occurs 3 times in a row.
This is intended to avoid wasting retries on the same broken state.

## 8. Results and reporting

Outputs are written under:

```text
integration-test-app/maestro/results/
```

Tracked policy:
- commit summarized findings and process updates
- do not commit raw screenshots, logs, or timestamped artifact directories unless explicitly requested

## 9. Parallel execution notes

Android and iOS can be run in parallel in separate terminals or tmux sessions.
Maestro also supports sharding for parallel orchestration, but Android Maestro transport should be healthy first before relying on shard-based parallel runs.

## 10. Known recovery step for Android Maestro

A clean Maestro + ADB restart has proven effective when Android Maestro fails with transport/bootstrap errors such as localhost:7001 connection refused:

```bash
pkill -f maestro || true
adb kill-server
adb start-server
lsof -i :7001
```

After that, rerun the Android validation flow.
