# Example app

This folder contains a minimal React Native app that demonstrates the public JS/TS API exposed by the sample package.

## What it shows
- `NapSspAd.initialize()` usage
- `BannerAd` rendering and placeholder fallback
- `InterstitialAd` load/show calls with safe error handling
- Native availability checks via `isNativeModuleAvailable()`

## Run notes

The example is intentionally lightweight and does not pretend the native SDK bridge is finished yet.

1. Install dependencies in the repo root.
2. Build the package so `lib/` exists.
3. Launch Metro from the example app when the native modules are ready.

Until the Android/iOS native bridge is wired, the app will display placeholder messaging instead of real ads.
