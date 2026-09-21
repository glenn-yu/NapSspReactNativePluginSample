# Migration

## 0.4.x → 0.5.0

0.5.0 fixes defects that changed behaviour, so the upgrade is not a drop-in. Work through the four
required steps below; everything after them is optional cleanup.

---

### Required 1 — Kotlin 2.1+ in the host app

nap mx core `admixer-ssp:2.3.0` ships `kotlin-stdlib:2.2.10` (metadata 2.2.0). Kotlin 2.0 and older
compilers cannot read it:

```
Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.2.0, but the compiler version 2.0.21 can read versions up to 2.1.0.
```

| React Native | Template Kotlin | Action |
| :--- | :--- | :--- |
| 0.81+ | 2.1.20 | none |
| 0.76 – 0.80 | 2.0.21 | set `kotlinVersion = "2.1.21"` |

```groovy
// android/build.gradle
buildscript {
    ext {
        kotlinVersion = "2.1.21"
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
```

### Required 2 — `compileSdk 35` and React Native 0.76+

`admixer-ssp:2.3.0` publishes AAR metadata requiring API 35. Building against 34 fails with:

```
Dependency 'io.github.nasmedia-tech:admixer-ssp:2.3.0' requires libraries and applications that
depend on it to compile against version 35 or later of the Android APIs.
```

React Native 0.76 and newer already compile against 35 or 36. The package's
`peerDependencies` now require `react-native >= 0.76.0` for this reason.

### Required 3 — replace `napSsp.enableVendorSdk`

The flag is gone; the core SDK is always linked. Mediation adapters are now opt-in:

```diff
  # android/gradle.properties
- napSsp.enableVendorSdk=true
+ # comma separated, or "all"
+ napSsp.mediations=admanager,adfit
```

If you previously set `enableVendorSdk=true` **without** `napSsp.mediations`, every adapter was
linked. Use `napSsp.mediations=all` to keep that, or list only the networks you sell.

A leftover `napSsp.enableVendorSdk` only logs a warning.

### Required 4 — declare the mediation Maven repositories in your app

AdFit, Pangle and Teads pull network SDKs that are not on Maven Central. Earlier versions declared
those repositories inside the library, which does not work: Gradle resolves a library's transitive
dependencies with the **consuming** project's repositories. Add them to your app:

```groovy
// android/settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }   // AdFit
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }         // Pangle
        maven { url 'https://sdk.teads.tv/android/repo' }                         // Teads
        maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }  // Teads
        maven { url 'https://developer.huawei.com/repo/' }                        // Teads: Huawei
    }
}
```

The Gradle build prints the exact list for the adapters you enabled.

---

## Behaviour changes to re-test

### Placeholder simulation is gone

This is the change most likely to alter what you see.

Up to 0.4.x, debug builds replaced the real SDK: `start()` and `show()` emitted fake
`opened`/`impression`/`closed` events without calling the SDK, and the banner, native and video
views reported a fake `loaded` + `impression` when the SDK returned no fill, failed, or timed out.

In 0.5.0 every build takes the real SDK path and failures are reported as failures. If your debug
build "always showed ads" before and now reports `nap_ssp_no_ads`, that is the real state of your
waterfall — it was being hidden.

### Privacy settings now reach the SDK

`setCoppa()` used to store a local flag and nothing else, on both platforms. It now calls
`AdMixer.setTagForChildDirectedTreatment` (Android) and `AMMConsent.childDirected` (iOS).

**If your app is child-directed, verify your inventory after upgrading** — AppLovin is now skipped
in the waterfall and Pangle is downgraded to non-personalised, which is the correct behaviour but
will change fill and revenue.

```diff
- NapSspAd.setCoppa(true);
+ await NapSspAd.initialize({
+   mediaKey, adUnitIds,
+   privacy: {childDirected: true},
+ });
```

Pass `privacy` to `initialize()` rather than calling it afterwards: AppLovin, Unity Ads and Pangle
read consent only when they start up.

### `setLogLevel()` / `setCoppa()` return promises

Both were `void`, and on Android both **threw** because the JS call passed one argument where the
native method expected two. They now return `Promise<void>` and work on both platforms.

```diff
- NapSspAd.setLogLevel('verbose');
+ await NapSspAd.setLogLevel('verbose');
```

### `initialize()` resolves the status

```diff
- await NapSspAd.initialize(config);
+ const status = await NapSspAd.initialize(config);
+ console.log(status.initialized, status.platform);
```

### `adUnitIds` must be numeric strings

`initialize()` now rejects non-numeric ad unit IDs in JS with the offending values named. The native
SDKs always parsed them as integers, so a non-numeric ID never worked — it just failed later and
less clearly.

### Reward payload

```diff
  ad.addAdEventListener('rewarded', (reward) => {
-   grant(reward.type, reward.amount);
+   grant(userId, reward.transactionId);
  });
```

`type` and `amount` were fabricated by the plugin — the SDK does not report them, because units are
not comparable across mediation networks. They are still present (always `'reward'` / `1`) and
marked deprecated. `transactionId` matches `transaction_id` on the S2S reward callback.

### Removed exports

`src/NativeNapSspModuleSpec.ts` and `src/NativeNapSspInterstitialSpec.ts` were unimplemented
TurboModule specs that were never exported from the package entry point. They are deleted.

`MediationConfig.mobwith` is removed — MobWith has not been in the supported network list for
several SDK releases and the field was a no-op.

---

## New capabilities worth adopting

| API | Why |
| :--- | :--- |
| `NapSspAd.setPrivacyConsent()` | GDPR / CCPA / COPPA, tri-state. |
| `NapSspAd.setTestMode()` / `setTestDeviceIds()` | Test ads for QA and store review (Android). |
| `ad.isReady()` / `ad.isLoading()` | Ask the SDK instead of tracking state yourself. |
| `ref.reload()` on inline views | Request a fresh banner/native/video without remounting. |
| `mediations.pangle.appId`, `mediations.appLovin.sdkKey` | Injected via `AdInfo.setAdapterConfig` when the server does not send the key. |
| `AdError.nativeCode` | The raw SDK code alongside the stable string code. |

---

## Version matrix

| Component | 0.4.0 | 0.5.0 |
| :--- | :--- | :--- |
| Android BOM | `2026.07.06` | **`2026.09.03`** |
| `admixer-ssp` (core) | `2.1.3` | **`2.3.0`** |
| `admixer-admanager` | `2.0.4` | **`2.1.3`** |
| `admixer-adfit` | `2.0.3` | **`2.0.6`** |
| `admixer-pangle` | `2.0.2` | **`2.1.2`** |
| `admixer-applovin` | `2.0.2` | **`2.0.5`** |
| `admixer-unity` | `2.0.2` | **`2.0.6`** |
| `admixer-naveradmanager` | `2.0.2` | **`2.1.3`** |
| `admixer-teads` | `2.1.0` | **`2.1.2`** |
| iOS `AdMixerMediation` | `2.4.2` | **`2.5.0`** |
| React Native | `>= 0.72` | **`>= 0.76`** |
| React | `^18.2` | **`>= 18.2`** (React 19 supported) |
| Android `compileSdk` | 34 | **35** |
| Android Kotlin | 1.8+ | **2.1+** |
| iOS deployment target | 14.0 | 14.0 |

Every Android coordinate was checked against Maven Central and the
[official guide](https://napmx.github.io/#/android/native/getting-started); the iOS xcframework
checksum was recomputed from the published 2.5.0 zip.

### Google Ad Manager ceiling

`play-services-ads` must stay at 25.2.0 or below — 25.3.0+ is incompatible:

```groovy
configurations.all {
    resolutionStrategy { force 'com.google.android.gms:play-services-ads:25.2.0' }
}
```

### Per-network minSdk

`admixer-applovin` requires API 24 and `admixer-admanager` / `admixer-naveradmanager` require 23.
React Native 0.76+ already sets `minSdkVersion = 24`.

---

## What was verified for this release

| Check | Result |
| :--- | :--- |
| Android library compiles against nap mx core 2.3.0 with all 7 adapters | ✅ Kotlin 2.1.21 / AGP 8.5.2 |
| Android host app assembles, merges resources and dexes the plugin + adapters | ✅ AGP 8.7.2 / compileSdk 35, APK produced |
| TypeScript typecheck and build | ✅ |
| Package contract smoke test (11 checks) | ✅ |
| On-device runtime (Galaxy SM-S947N, Android 16) | ✅ SDK 2.3.0 initialised, banner and inline video served impressions, native returned a genuine `nap_ssp_no_ads` |
| iOS build | ⛔ not measured — no macOS/Xcode in the development environment. The API was cross-checked symbol by symbol against the `.swiftinterface` shipped in AdMixerMediation 2.5.0. |
| New Architecture | ⛔ not measured, not claimed |
