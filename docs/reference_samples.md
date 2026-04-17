# Reference sample findings

This file records concrete symbols confirmed from the official public sample repositories so the RN bridge can be wired with less guesswork.

## Android sample findings

Sample repo:
- `https://github.com/Nasmedia-Tech/AOS-AdMixerSSP-TestApp`

Confirmed classes and usage:

### Initialization
- `AdMixerLog.logLevel = AdMixerLog.LogLevel.VERBOSE`
- `AdMixer.setTagForChildDirectedTreatment(...)`
- `AdMixer.getInstance().initialize(this, MEDIA_KEY, adUnits)`
- `AdMixer.registerAdapter(...)`

Adapter constants seen in sample:
- `AdMixer.ADAPTER_ADMANAGER`
- `AdMixer.ADAPTER_ADFIT`
- `AdMixer.ADAPTER_MOBWITH`
- `AdMixer.ADAPTER_PANGLE`
- `AdMixer.ADAPTER_APPLOVIN`
- `AdMixer.ADAPTER_UNITY`

### Banner
- `AdView(context)`
- `AdInfo.Builder(adUnitId).setIsUseMediation(true).build()`
- `setAdInfo(adInfo)`
- `setAlwaysShowAdView(true)`
- `setAdViewListener(object : AdListener { ... })`
- add banner view to a layout container
- lifecycle methods: `onResume()`, `onPause()`, `onDestroy()`

### Interstitial
- `InterstitialAd(activity)`
- `setAdInfo(adInfo)`
- `setAdListener(object : AdListener { ... })`
- `loadInterstitial()`
- `showInterstitial()`
- `closeInterstitial()`
- `stopInterstitial()`
- `hasInterstitial`

### Rewarded video
- `RewardInterstitialVideoAd(activity)`
- `setAdInfo(adInfo)`
- `setListener(object : AdListener { ... })`
- `loadRewardVideoAd()`
- `showRewardVideoAd()`
- `closeRewardVideoAd()`
- `stopRewardVideoAd()`
- `hasInterstitial`
- custom params via `AdInfo.Builder(...).setCustomParams(map)`

### Shared callbacks
- `AdListener.onReceivedAd(...)`
- `AdListener.onFailedToReceiveAd(...)`
- `AdListener.onEventAd(...)`
- `AdEvent.CLICK`
- `AdEvent.DISPLAYED`
- `AdEvent.CLOSE`
- `AdEvent.SKIPPED`
- popup-only interstitial events such as `LEFT_CLICK`, `RIGHT_CLICK`

## iOS sample findings

Sample repo:
- `https://github.com/Nasmedia-Tech/iOS-AdMixerSSP-TestApp`

Confirmed classes and usage:

### Initialization
- `AMMediation.shared.setDebugEnabled(isEnabled:)`
- `AMMediation.shared.initialize(mediaKey:adunitID:)`

### Optional mediation SDK bootstrap in app code
- `MobileAds.shared.start()`
- `PAGSdk.start(with:completionHandler:)`
- `ALSdk.shared().initialize(with:completionHandler:)`
- `UnityAds.initialize(...)`

### Banner
- `AMMBannerView(rootViewController:)`
- `banner.adUnitID = ...`
- `banner.delegate = self`
- `banner.load()`
- delegate callbacks:
  - `onSuccessBanner()`
  - `onFailBanner()`

## Immediate implications for the RN plugin

- Android wiring can now move from broad placeholders to concrete class-shaped wrappers for banner/interstitial/rewarded.
- iOS initialization and banner bridging can be made much more concrete immediately.
- iOS interstitial/rewarded still need one more sample-file pass before hard-wiring method names.
