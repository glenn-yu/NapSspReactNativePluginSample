# Vendor integration guide

This document captures the exact next step for moving the plugin from placeholder runtime to real nap ssp SDK integration.

## Current truth

- JS/TS API is build-verified.
- Android and iOS bridge shapes are in place.
- Native code is still placeholder-backed.
- The next milestone is wiring the placeholder runtime to real vendor SDK classes and callbacks.

## Android

### Current dependency model

The Android library supports two modes:

1. Placeholder mode (default)
2. Vendor SDK mode

Enable vendor mode with Gradle properties:

```sh
./gradlew -PnapSsp.enableVendorSdk=true
```

Optional mediation selection:

```sh
./gradlew -PnapSsp.enableVendorSdk=true -PnapSsp.mediations=admanager,adfit,pangle
```

Supported mediation keys:
- `admanager`
- `adfit`
- `pangle`
- `applovin`
- `unity`

### Planned SDK coordinates

- Core: `io.github.nasmedia-tech:admixer-ssp:1.0.21`
- Google App ID helper: `com.google.android.gms:play-services-ads-identifier:18.9.0`
- Mediation:
  - `io.github.nasmedia-tech:admixer-admanager:1.0.14`
  - `io.github.nasmedia-tech:admixer-adfit:1.0.10`
  - `io.github.nasmedia-tech:admixer-pangle:1.0.10`
  - `io.github.nasmedia-tech:admixer-applovin:1.0.8`
  - `io.github.nasmedia-tech:admixer-unity:1.0.6`

### Exact remaining Android work

- Replace `NapSspSdkBridge` placeholder state with real SDK initialization.
- Implement mediation adapter registration logic from the parsed JS config.
- Map native banner callbacks to RN events:
  - `onAdLoaded`
  - `onAdFailedToLoad`
  - `onAdClicked`
  - `onAdOpened`
  - `onAdClosed`
- Replace interstitial/rewarded placeholder state transitions with real SDK objects and callbacks.
- Bridge earned reward callbacks to JS `onRewarded`.

### Host app requirements

The consuming Android app will still need app-level metadata such as:

- Google App ID when Google mediation is used
- Any network-specific IDs/keys required by the chosen mediation stack

## iOS

### Current dependency model

The podspec now expresses the planned dependency shape:

- Required:
  - `React-Core`
  - `AdMixerMediation`
- Optional subspecs:
  - `NapSspPlugin/GAM`
  - `NapSspPlugin/AdFit`
  - `NapSspPlugin/Pangle`
  - `NapSspPlugin/AppLovin`
  - `NapSspPlugin/UnityAds`

### Example Podfile

```ruby
target 'MyApp' do
  use_frameworks!

  pod 'NapSspPlugin'
  pod 'NapSspPlugin/GAM'
  pod 'NapSspPlugin/Pangle'
end
```

### Exact remaining iOS work

- Replace `NapSspRuntime` placeholder state with real `AdMixerMediation` initialization and ad object lifecycle.
- Bind interstitial/rewarded load and present callbacks to the JS event surface.
- Wire reward callbacks to the JS `onRewarded` event.
- Confirm actual pod names / version compatibility against vendor docs and samples.
- Add host-app integration verification in a real iOS app target.

### Host app requirements

The consuming iOS app will still need:

- `NSUserTrackingUsageDescription`
- Any required mediation identifiers such as `GADApplicationIdentifier`
- Appropriate ATT request timing from app code

## Recommended next implementation order

1. Confirm vendor Android SDK symbols from official docs/sample app.
2. Wire Android initialize/banner/interstitial/rewarded against real classes.
3. Confirm iOS `AdMixerMediation` symbols and sample app flow.
4. Wire iOS initialize/banner/interstitial/rewarded against real classes.
5. Verify both inside a host React Native app, not the library in isolation.
