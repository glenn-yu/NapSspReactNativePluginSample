# iOS integration notes for `NapSspPlugin`

This repository currently ships a practical iOS scaffold for the nap ssp React Native bridge.
It is intentionally honest about what is still placeholder and what is ready to swap to the real vendor SDK.

## What is exported on iOS

- `NapSspModule`
  - `initialize(config)`
  - `setLogLevel(level)`
  - `setCoppa(enabled)`
  - `getStatus()`
  - `requestTrackingAuthorization()`
- `NapSspInterstitial`
  - `load(adUnitId)`
  - `show(adUnitId)`
  - `destroy(adUnitId)`
- `NapSspRewarded`
  - `load(adUnitId)`
  - `show(adUnitId)`
  - `destroy(adUnitId)`
- `NapSspInterstitialVideo`
  - `load(adUnitId)`
  - `show(adUnitId)`
  - `destroy(adUnitId)`
- `NapSspBannerView`
  - props: `adUnitId`, `size`
  - events: `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`
- `NapSspNativeAdView`
  - props: `adUnitId`
  - events: `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`
- `NapSspVideoAdView`
  - props: `adUnitId`, `isRetry`
  - events: `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`, `onAdCompleted`, `onAdSkipped`

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
- `NapSspInterstitial.load()` and `NapSspRewarded.load()` store the ad unit as loaded in memory.
- `NapSspInterstitial.show(adUnitId)` and `NapSspRewarded.show(adUnitId)` only resolve after a matching load; otherwise they reject with a `not loaded` error.
- `NapSspRewarded.show(adUnitId)` also returns a placeholder reward payload so the bridge shape matches the future vendor callback flow.
- `requestTrackingAuthorization()` uses ATT when available and falls back to `unavailable` on older iOS versions.

## ATT helper assumptions

- ATT is treated as iOS 14.5+.
- The helper only requests tracking authorization when the OS supports it.
- If you want the prompt to appear at the right moment, call the helper from app code after your own onboarding / consent flow.
- The app still needs `NSUserTrackingUsageDescription` in `Info.plist`.

## Podspec / bridge assumptions

The podspec is wired for a future vendor SDK swap:

- `React-Core` is the only hard dependency today.
- `AdSupport`, `StoreKit`, and `AppTrackingTransparency` are linked as platform/framework assumptions for future ad SDK integration.
- The native module names match the JS fallbacks already present in `src/nativeBridge.ts`.

## Known limitation

No real nap ssp SDK is linked yet. The current native code is intentionally written so the placeholder logic can be replaced by actual `AdMixerMediation` calls without changing the JS-facing shape.
