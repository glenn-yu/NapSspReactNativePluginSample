# iOS integration notes for `NapSspPlugin`

This repository currently ships a practical iOS scaffold for the nap ssp React Native bridge.
It is still a placeholder implementation, but the exported module/component names and method shapes are ready for a real SDK hookup.

## What is exported on iOS

- `NapSspModule`
  - `initialize(config)`
  - `setLogLevel(level)`
  - `setCoppa(enabled)`
  - `getStatus()`
  - `requestTrackingAuthorization()`
- `NapSspInterstitial`
  - `load(adUnitId)`
  - `show()`
- `NapSspBannerView`
  - props: `adUnitId`, `size`
  - events: `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`

## Install / build

1. Add the pod to the app target and run:
   ```sh
   cd ios
   pod install
   ```
2. Make sure the app target includes the usual iOS ad-tracking strings in `Info.plist`:
   - `NSUserTrackingUsageDescription`
   - any mediation-specific identifiers required by your actual SDK setup
3. Link the app to a real nap ssp / AdMixerMediation SDK package before expecting live ads.

## Current behavior

- `initialize()` validates the config shape, stores it in a local runtime, and returns a normalized status payload.
- `NapSspBannerView` renders a visible placeholder card and emits load / tap / close-style events.
- `NapSspInterstitial.load()` stores the ad unit as loaded in memory.
- `NapSspInterstitial.show()` resolves only after a matching load; otherwise it rejects with a `not loaded` error.
- `requestTrackingAuthorization()` uses ATT when available and falls back to `unavailable` on older iOS versions.

## Known limitation

No real nap ssp SDK is linked yet. The current native code is intentionally written so the placeholder logic can be replaced by actual `AdMixerMediation` calls without changing the JS-facing shape.
