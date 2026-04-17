# Android integration notes for `react-native-nap-ssp`

This repository currently ships a structured Android bridge for the nap ssp React Native plugin. It is still a placeholder implementation, but the exported module names, event names, and build-time assumptions are now explicit and stable.

## Exported Android surface

### Native modules
- `NapSspModule`
  - `initialize(config)`
  - `setLogLevel(level)`
  - `setCoppa(enabled)`
  - `getStatus()`
- `NapSspInterstitial`
  - `load(adUnitId)`
  - `show(adUnitId)`
  - `isLoaded(adUnitId)`
  - `destroy(adUnitId)`
- `NapSspRewarded`
  - `load(adUnitId)`
  - `show(adUnitId)`
  - `isLoaded(adUnitId)`
  - `destroy(adUnitId)`

### Native view
- `NapSspBannerView`
  - props: `adUnitId`, `size`, `autoLoad`
  - direct events: `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`

## Build-time behavior

The Android Gradle file now exposes a placeholder-first configuration:

- `napSsp.enableVendorSdk=false` by default
- When that property is set to `true`, the Gradle script adds the documented nap ssp / mediation Maven coordinates
- BuildConfig values expose the intended SDK coordinates and whether the vendor SDK path is enabled

This lets the package compile and ship a truthful placeholder bridge before the private SDK artifacts are available.

## Runtime behavior

- `initialize()` validates and normalizes the config before storing it in the local runtime bridge
- `getStatus()` reports:
  - initialization state
  - placeholder/vendor mode
  - configured ad units
  - banner/interstitial/rewarded load-state snapshots
  - mediation flags from the parsed config
- Banner view events are emitted from the placeholder view state machine instead of being faked as successful SDK calls
- Interstitial and rewarded modules keep local load/show state and emit the expected JS event names

## Host app responsibilities

The library manifest stays intentionally empty. Host apps should still provide their own ad network metadata, for example:

- Google App ID / `com.google.android.gms.ads.APPLICATION_ID`
- any mediation-specific identifiers required by the real SDK setup

## Known limitation

No real nap ssp Android SDK is linked yet. Once the official classes and callbacks are known, the placeholder state machine in `android/src/main/java/com/napsspplugin/` can be replaced with direct vendor API calls without changing the public JS API.
