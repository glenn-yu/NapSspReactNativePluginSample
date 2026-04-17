# Example app

This folder contains a minimal React Native app that demonstrates the public JS/TS API exposed by the sample package.

## What it shows
- `NapSspAd.initialize()` usage
- `BannerAd` rendering and placeholder fallback
- `InterstitialAd` load/show calls with safe error handling
- Native availability checks via `isNativeModuleAvailable()`

## Run notes

The example is intentionally lightweight and does not pretend the native SDK bridge is finished yet.

Android emulator (recommended for local testing)

1. Ensure Android SDK, emulator, and `adb` are installed and available on PATH.
2. From repo root:

```bash
npm ci
npm run build
cd example
npx react-native run-android
```

To test with the vendor SDK enabled in Android (host app build):

```bash
# from the Android host app project that consumes this plugin
./gradlew assembleDebug -PnapSsp.enableVendorSdk=true -PnapSsp.mediations=admanager,adfit
```

iOS simulator (requires macOS + Xcode)

1. From repo root:

```bash
npm ci
npm run build
cd example/ios
pod install
cd ..
npx react-native run-ios --simulator "iPhone 14"
```

Notes

- The example app uses placeholder native modules by default. To exercise real vendor SDK flows, enable vendor SDK in the host Android build and add appropriate subspecs for iOS in the Podfile.
- If you do not have real mediaKey/adUnitIds, the placeholder runtime will simulate ad load/open events so you can verify JS event paths and UI behavior.
