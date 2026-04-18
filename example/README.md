# Example app

This folder contains a minimal React Native app that demonstrates the public JS/TS API exposed by the sample package.

## What it shows
- `NapSspAd.initialize()` usage plus status checks
- `NapSspAd.getStatus()` status checks and placeholder-mode visibility
- `BannerAd`, `NativeAd`, and `VideoAd` rendering with placeholder fallback
- `InterstitialAd` and `InterstitialVideoAd` load/show calls with safe error handling
- `RewardedAd` load/show calls with safe error handling and `onRewarded` callbacks without payloads
- Native availability checks via `isNativeModuleAvailable()`

## Structure
- `ExampleHostApp/`: standalone React Native test app inside this repository
- `ExampleHostApp/scripts/run-android-emulator.sh`: Android build, install, launch helper
- `ExampleHostApp/scripts/run-ios-sim.sh`: iOS CocoaPods + simulator helper

## Prerequisites

### Android
- Java/JDK installed and `java -version` works
- Android SDK + platform-tools installed
- `adb` available on `PATH`
- Running emulator or connected Android device

### iOS
- macOS
- Xcode and command line tools (`xcodebuild` available)
- CocoaPods installed (`pod` available)

## Quick start for beginners

If you just want to see the sample app run, use this order:

1. From repo root, install packages and build the package.
2. Run the example host app on Android or iOS.
3. If Android fails first, check Java/JDK.
4. If iOS fails first, check CocoaPods.

```bash
npm ci
npm run build
cd example/ExampleHostApp
```

## Run notes

The example is intentionally lightweight and should be treated as a host verification app, not as proof that every native dependency is already installed on your machine.

### Android emulator (recommended for local testing)

From repo root:

```bash
npm ci
npm run build
cd example/ExampleHostApp
./scripts/run-android-emulator.sh
```

If you want to test vendor SDK mode in the Android host app build:

```bash
cd example/ExampleHostApp/android
./gradlew assembleDebug -PnapSsp.enableVendorSdk=true -PnapSsp.mediations=admanager,adfit
```

### iOS simulator

From repo root:

```bash
npm ci
npm run build
cd example/ExampleHostApp
./scripts/run-ios-sim.sh
```

## Testing

Run the example app Jest smoke test:

```bash
cd example/ExampleHostApp
npm test -- --runInBand
```

Note: the Jest test uses lightweight mocks for the plugin exports so the example UI can render without a native bridge in CI or local Node-only environments, and it now checks the initialize call covers all sample ad unit IDs.

## Current verification status
- Android example host app: build verified, emulator install verified, app launch verified, screenshot capture verified.
- iOS example host app: `pod install` verified, simulator build verified, simulator launch and screenshot capture verified.
- Real ad delivery: requires valid server-side test inventory and native module registration on each platform.
- Current direct API spot-check: Android 320x50 and iOS 320x50 test endpoints returned HTTP 200 with `ads` length `0`, so real fill is not yet confirmed.

## Feature checklist
- App initialization flow: verified
- Banner / Native / Video placeholder-safe rendering: verified
- Interstitial / Interstitial Video / Rewarded JS flow: wired in sample app
- Android local build/install/run: verified
- iOS local pod install/build/run: verified
- Real ad fill from provided test endpoints: not yet verified, server response currently empty in spot-check

## Notes
- The example app uses placeholder/native-safe behavior by default. To exercise real vendor SDK flows, enable vendor SDK in the host Android build and add the required iOS pods/packages.
- If you do not have real `mediaKey` or `adUnitId` values, placeholder mode is still useful for verifying JS event paths and UI behavior.
- If Android fails immediately, check JDK setup first. If iOS fails immediately, check CocoaPods first.
