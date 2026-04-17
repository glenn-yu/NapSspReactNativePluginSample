# Vendor integration guide

This document captures the exact next step for moving the plugin from placeholder runtime to real nap ssp SDK integration.

## Current truth

- JS/TS API is build-verified.
- Android and iOS bridge shapes are in place.
- Native code is still placeholder-backed.
- The next milestone is wiring the placeholder runtime to real vendor SDK classes and callbacks.

## Android

### Confirmed SDK symbols from the official sample app

The Android sample confirms these concrete APIs/classes:

- Initialization
  - `com.nasmedia.admixerssp.common.AdMixer`
  - `AdMixer.getInstance().initialize(context, mediaKey, adUnits)`
  - `AdMixer.registerAdapter(AdMixer.ADAPTER_ADMANAGER)` and related adapter constants
  - `AdMixer.setTagForChildDirectedTreatment(...)`
  - `com.nasmedia.admixerssp.common.AdMixerLog`
- Banner
  - `com.nasmedia.admixerssp.ads.AdView`
  - `com.nasmedia.admixerssp.ads.AdInfo.Builder(adUnitId)`
  - `setAdInfo(...)`, `setAlwaysShowAdView(...)`, `setAdViewListener(...)`
  - lifecycle: `onResume()`, `onPause()`, `onDestroy()`
- Interstitial
  - `com.nasmedia.admixerssp.ads.InterstitialAd`
  - `loadInterstitial()`, `showInterstitial()`, `closeInterstitial()`, `stopInterstitial()`
  - `hasInterstitial`
  - optional popup config: `PopupInterstitialAdOption`
- Rewarded video
  - `com.nasmedia.admixerssp.ads.RewardInterstitialVideoAd`
  - `loadRewardVideoAd()`, `showRewardVideoAd()`, `closeRewardVideoAd()`, `stopRewardVideoAd()`
  - `hasInterstitial`
  - reward custom params through `AdInfo.Builder(...).setCustomParams(...)`
- Shared callbacks
  - `AdListener`
  - `onReceivedAd(...)`
  - `onFailedToReceiveAd(...)`
  - `onEventAd(...)`
  - `AdEvent.CLICK`, `AdEvent.DISPLAYED`, `AdEvent.CLOSE`, `AdEvent.SKIPPED`, etc.

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

- Replace `NapSspSdkBridge` placeholder state with real SDK initialization using:
  - `AdMixerLog.logLevel`
  - `AdMixer.setTagForChildDirectedTreatment(...)`
  - `AdMixer.getInstance().initialize(...)`
  - `AdMixer.registerAdapter(...)`
- Build `AdInfo` from JS config per format and wire:
  - `AdView`
  - `InterstitialAd`
  - `RewardInterstitialVideoAd`
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

### Confirmed SDK symbols from the official sample app

The iOS sample confirms these concrete APIs/classes:

- Initialization
  - `AMMediation.shared.setDebugEnabled(isEnabled:)`
  - `AMMediation.shared.initialize(mediaKey:adunitID:)`
- Optional mediation SDK bootstrap at app level
  - `MobileAds.shared.start()`
  - `PAGSdk.start(with:completionHandler:)`
  - `ALSdk.shared().initialize(with:completionHandler:)`
  - `UnityAds.initialize(...)`
- Banner
  - `AMMBannerView(rootViewController:)`
  - `adUnitID`
  - `delegate`
  - `load()`
  - delegate callbacks: `onSuccessBanner()`, `onFailBanner()`

The sample repo should still be mined for concrete interstitial/rewarded symbols next, but the initialization path is now confirmed.

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

- Replace `NapSspRuntime` placeholder state with real `AdMixerMediation` initialization via:
  - `AMMediation.shared.setDebugEnabled(isEnabled:)`
  - `AMMediation.shared.initialize(mediaKey:adunitID:)`
- Replace the placeholder banner with `AMMBannerView` and bridge banner delegate callbacks.
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

## AppBridge sample notes (local developer samples)

There is an AppBridge sample in the workspace that contains additional integration guidance and hybrid webview bridging examples. Useful files:

- `/Users/gwangy.claw/Developer/NapSspAppBridgeSample/docs/quickstart.md`
- `/Users/gwangy.claw/Developer/NapSspAppBridgeSample/docs/hybrid-webview.md`
- `/Users/gwangy.claw/Developer/NapSspAppBridgeSample/ios/Sources/NapSspIOSSample/HybridEventBridge.swift`
- `/Users/gwangy.claw/Developer/NapSspAppBridgeSample/ios/Sources/NapSspIOSSample/NapSspInitializer.swift`
- Android samples under `/Users/gwangy.claw/Developer/_nap_refs/AOS-AdMixerSSP-TestApp/` (AdView/Interstitial/Rewarded examples)

Actionable takeaways from AppBridge sample

- Hybrid bridge message format: events are posted as JSON objects with fields like `event`, `adUnitId`, `payload`, and `timestamp`. Follow the same minimal shape when emitting RN DeviceEvents so hybrid/web consumers can reuse the same handlers.
- Initializer pattern: build a single `AdInfo`/config object from JS config then call the platform initialize method once with the media key + ad unit list.
- Lifecycle: Respect host activity/view controller lifecycle and call banner `onResume`/`onPause`/`onDestroy` equivalents from RN view manager lifecycle hooks.
- ATT: AppBridge has utilities to request ATT; reuse the same timing guidance and expose `requestTrackingAuthorization()` from JS.

Add these references to your app-level integration docs and ensure RN bridge events follow the simple JSON envelope for easier hybrid compatibility.
