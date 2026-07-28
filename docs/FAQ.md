# ❓ FAQ, Troubleshooting & Privacy Guide

Frequently asked questions, fixes for known build and runtime issues, privacy compliance notes, and an ad-tech glossary.

---

## 📋 Table of Contents
1. [Frequently Asked Questions](#1-frequently-asked-questions)
2. [Troubleshooting](#2-troubleshooting)
3. [Privacy & Compliance](#3-privacy--compliance)
4. [Ad-Tech Glossary](#4-ad-tech-glossary)

---

## 1. Frequently Asked Questions

### Q1. No ads appear and everything reports success. What's wrong?
Check whether the vendor SDK is actually linked:

```typescript
const status = await NapSspAd.getStatus();
console.log(status.placeholderMode);   // true → vendor SDK is NOT linked
```

On Android, set `napSsp.enableVendorSdk=true` in `android/gradle.properties` and re-sync. On iOS, confirm the `NapSspPlugin` pod (and its subspecs) is in your Podfile. See [Setup §3①](./SETUP.md#-vendor-sdk-opt-in).

### Q2. Why do I get a no-fill error?
No-fill means the request reached the exchange but nothing matched your targeting or floor price. It is normal for new ad units and in test environments. On Android the code is **`AX_ERR_NO_ADS`** — do not branch on `AX_ERR_NO_FILL`, which core v2.1.3 deprecated because the SDK never emits it.

### Q3. Does this support the New Architecture (TurboModules / Fabric)?
Yes. The bridge layer (`NativeNapSspModuleSpec`, `NativeNapSspInterstitialSpec`) works under both the legacy bridge and the New Architecture.

### Q4. How do I cancel a load when the user navigates away?
Use `cancelLoad()` in your cleanup path. It is a no-op if nothing is in flight and never disturbs an ad that is already displaying:

```typescript
return () => {
  interstitial.cancelLoad();
  interstitial.destroy();
};
```

### Q5. My event listener never fires.
Listener names are the short form — `'loaded'`, `'loadFailed'`, `'opened'`, `'closed'`, `'clicked'`, `'impression'` (plus `'rewarded'`, `'completed'`, `'skipped'`) — **not** the native `onAdLoaded` style names. The one exception is `'onRewarded'`, kept as an alias for `'rewarded'`. See the [event table](./API.md#event-names).

### Q6. Do I need to list every ad unit in `adUnitIds`?
Yes. `NapSspAd.initialize()` registers the ad units up front; loading a unit that was not registered fails. Since core v2.1.1 that failure is reported deterministically through `loadFailed` instead of silently hanging.

### Q7. Is the user rewarded twice if I listen on both channels?
No. Android core v2.1.1 made the reward channels mutually exclusive — exactly one notification per grant. If you previously guarded against double-granting, that workaround can be removed.

---

## 2. Troubleshooting

### Android: `NullPointerException` when unmounting a banner (#100)
* **Symptom**: `java.lang.NullPointerException: Attempt to read from field '...AdInfo...' on a null object reference` when closing or unmounting a banner screen on SDK v2.0.0.
* **Fix**: Resolved since v0.3.0 by moving to core v2.1.1+. The plugin's view managers also run three unmount defenses (`onDropViewInstance`, `onDetachedFromWindow`, `onHostDestroy`) so `destroy()` always runs.

### Android: `Failed to resolve: io.github.nasmedia-tech:...`
* **Fix**: Confirm `napSsp.enableVendorSdk=true` and that `google()` / `mavenCentral()` are present in your root `build.gradle`. The plugin declares the Kakao, Pangle, Teads and Huawei repositories itself. If your project uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle`, module-level repositories are ignored — declare them in `dependencyResolutionManagement` instead. See [Setup §3②](./SETUP.md#-bundled-artifact-versions).

### Android: duplicate class / `play-services-ads` conflicts
* **Symptom**: Build failure or runtime crashes in the AdManager adapter after another library upgrades Google Play Services Ads.
* **Fix**: `play-services-ads` **25.3.0+ is incompatible**. Force the supported version:
  ```gradle
  configurations.all {
      resolutionStrategy { force 'com.google.android.gms:play-services-ads:25.2.0' }
  }
  ```

### Android: `Module was compiled with an incompatible version of Kotlin`
* **Fix**: AdManager and Naver Ad Manager require the host app to build with **Kotlin 2.1+**, Kakao AdFit with **2.0+**. See [Setup §3④](./SETUP.md#-kotlin-toolchain).

### Android: manifest merger fails on `networkSecurityConfig`
* **Symptom**: `Attribute application@networkSecurityConfig value=(...) is also present at [admixer-ssp]`.
* **Fix**: The core SDK declares its own network security config. Add `tools:replace="android:networkSecurityConfig"` to your `<application>` — see [Setup §3⑤](./SETUP.md#-androidmanifestxml).

### Android: `minSdkVersion` conflict after adding an adapter
* **Fix**: Adapters raise the floor above the core's API 21 — AppLovin needs **24**, and AdManager / Pangle / Unity / Naver Ad Manager need **23**. Either raise your `minSdk` or drop the adapter from `napSsp.mediations`.

### iOS: low fill rate or missing ads on iOS 14.5+
* **Fix**: Implement App Tracking Transparency. Add `NSUserTrackingUsageDescription` to `Info.plist` and call `NapSspAd.requestTrackingAuthorization()` before requesting ads. Without consent the IDFA is zeroed and personalized inventory is unavailable.

### iOS: app will not launch in the simulator
* **Fix**: A simulator launch bug in SDK 2.4.0 was fixed in 2.4.1. Plugin v0.4.0 ships 2.4.2. Run `pod install --repo-update`, or re-resolve SPM packages.

### iOS: Teads adapter fails to build
* **Fix**: `AdMixerMediationTeads` requires **TeadsSDK 6.2+** as of SDK 2.4.2. Add the `Teads` subspec (or SPM package) rather than linking TeadsSDK directly.

---

## 3. Privacy & Compliance

### App Tracking Transparency (iOS)
```typescript
import { NapSspAd } from 'react-native-nap-ssp';

const status = await NapSspAd.requestTrackingAuthorization();
// 'authorized' | 'denied' | 'restricted' | 'notDetermined' | 'unavailable' (Android)
```
Prompt **before** your first ad request. `NSUserTrackingUsageDescription` is mandatory or the prompt never shows.

### COPPA / child-directed treatment
```typescript
await NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: [...],
  coppa: true,
});

// or at runtime
NapSspAd.setCoppa(true);
```
With COPPA on, reward SSV postbacks omit the `ifa` field.

### Android advertising ID
Android 13+ (API 33+) requires the `com.google.android.gms.permission.AD_ID` permission for the advertising ID. Declare it in your manifest — see [Setup §3⑤](./SETUP.md#-androidmanifestxml). Omit it deliberately if your app must not access the advertising ID; expect reduced fill.

---

## 4. Ad-Tech Glossary
* **SSP (Supply-Side Platform)** — Software publishers use to sell ad impressions programmatically.
* **CPM (Cost Per Mille)** — Price paid per 1,000 impressions.
* **Mediation** — Calling multiple networks in sequence (a waterfall) to find the best-paying ad for an impression.
* **Fill Rate** — Share of ad requests answered with an ad (`Impressions / Requests × 100`).
* **No-fill** — A successful request that returned no ad.
* **BOM (Bill of Materials)** — A Gradle artifact that aligns versions across a family of libraries.
* **SSV (Server-Side Verification)** — A server-to-server postback confirming a reward was earned, so the grant cannot be spoofed by the client.
* **IDFA / GAID** — Per-device advertising identifiers on iOS and Android.
* **AdChoices** — The regulatory icon marking an ad and linking to its privacy disclosure.
