# FAQ & troubleshooting

## Build

### `Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin`

nap mx core 2.3.0 ships `kotlin-stdlib:2.2.10`. Build the host app with **Kotlin 2.1 or newer** —
see [Setup](./SETUP.md#kotlin-요구사항). React Native 0.81+ already defaults to 2.1.20.

### `requires libraries and applications that depend on it to compile against version 35 or later`

Raise the app's `compileSdkVersion` to 35 (or 36). React Native 0.76+ already does; older templates
compile against 34 and cannot consume this version.

### `Could not find tv.teads.sdk.android:sdk` / `com.kakao.adfit:ads-base` / `com.pangle.global:pag-sdk`

Those SDKs are not on Maven Central. Add the repositories **to your app** — Gradle resolves a
library's transitive dependencies with the consuming project's repositories, so declaring them
inside the plugin would not help:

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

### `Manifest merger failed: android:networkSecurityConfig`

The core SDK declares its own network security config. If your app declares one too, let yours win:

```xml
<application
    android:networkSecurityConfig="@xml/your_network_security_config"
    tools:replace="android:networkSecurityConfig">
```

### `uses-sdk:minSdkVersion N cannot be smaller than version 24`

The AppLovin adapter requires API 24, Google Ad Manager and Naver Ad Manager require 23. Raise the
app's `minSdkVersion` or drop that adapter from `napSsp.mediations`.

### `pod install` fails on the Teads subspec

`AdMixerMediationTeads` requires **Xcode 26 or newer**. Remove `pod 'NapSspPlugin/Teads'` if you
are on an older toolchain.

### `Duplicate class com.google.android.gms.ads...`

You already ship the Google Mobile Ads SDK. Exclude it from the adapter:

```groovy
implementation("io.github.nasmedia-tech:admixer-admanager:2.1.3") {
    exclude group: "com.google.android.gms", module: "play-services-ads"
}
```

Keep `play-services-ads` at 25.2.0 or below — 25.3.0+ is incompatible.

---

## Runtime

### No ads at all

1. Confirm `await NapSspAd.initialize(...)` **resolved** — every ad call rejects with
   `nap_ssp_not_initialized` until it does.
2. `NapSspAd.setLogLevel('verbose')`, then `adb logcat | grep AdMixerSDK`.
3. Check the media key and ad unit IDs against the partner site. Both must be numeric.
4. Confirm the ad unit is enabled and provisioned on the partner site.

### Everything returns `nap_ssp_no_ads`

That is the waterfall reporting no fill — every network was tried and none had inventory. It is
also the code you get when the ad unit has no networks assigned. Check the ad unit's configuration
first, then retry after a delay.

Do not branch on `AX_ERR_NO_FILL`: the SDK never sends it, so that branch never runs.

### It used to work in debug and now reports failures

Versions up to 0.4.x replaced the SDK with a simulation in debug builds and reported load failures
as successes. 0.5.0 removed that. The failures you see now were always happening — they were being
hidden. See the [migration notes](./MIGRATION.md#placeholder-simulation-is-gone).

### One network never serves

Look for these in the log:

| Log line | Cause |
| :--- | :--- |
| `[SKIP] configuration is invalid (Missing Keys).` | A required key is missing. Pangle needs `placement_id` (and `app_id` if its SDK is not yet started); AppLovin needs `zone_id`. |
| `[SKIP] Adapter instantiation failed for:` | The adapter module is not in the build — add it to `napSsp.mediations`. |

You can inject a missing key yourself; the server value always wins:

```ts
await NapSspAd.initialize({
  mediaKey, adUnitIds,
  mediations: {
    pangle: {appId: 'YOUR_PANGLE_APP_ID'},
    appLovin: {sdkKey: 'YOUR_APPLOVIN_SDK_KEY'},
  },
});
```

### The banner is invisible or clipped

The served size comes from the ad unit's server configuration, not from the `size` prop. Give the
container a full width and let the height follow the creative; do not lock a height that disagrees
with the served size.

### `nap_ssp_activity_required`

Android full-screen ads need a foreground Activity. Do not call `show()` from a background task or
while the app is backgrounded.

### `nap_ssp_view_not_linked` / `napssp_sdk_not_linked`

The native side is not in the build. Rebuild the app after installing the package — Metro's cache
alone is not enough. On iOS, run `pod install` again.

### Rewarded ads pay out twice / not at all

Grant from the `rewarded` event only, and deduplicate on `transactionId`. Do not also grant from
`completed` — playback completion and reward accrual are separate signals, and `completed` can fire
without a reward.

### `skipped` never fires on iOS

Correct, and not fixable from this plugin: `AMMRewardVideoDelegate` and
`AMMVideoInterstitialDelegate` have no skip callback. Android delivers it through
`AdListener.onAdSkipped()`. Do not build reward logic on `skipped`.

---

## Privacy & compliance

### Is my app child-directed?

If it targets children, Google Play's Families policy requires the flag regardless of country:

```ts
await NapSspAd.initialize({
  mediaKey, adUnitIds,
  privacy: {childDirected: true},
});
```

Consequences, by design: **AppLovin is skipped entirely** (their policy forbids SDK use for child
users), **Pangle is downgraded** to non-personalised (their COPPA API was removed — a mitigation,
not compliance), and Google Ad Manager / Naver Ad Manager / AdFit / Unity Ads receive the flag.

Unity Ads also needs the app-level age setting in the Unity Monetization dashboard.

### Why call `setPrivacyConsent()` before `initialize()`?

AppLovin, Unity Ads and Pangle read consent when their SDK starts, which happens lazily inside the
waterfall. Pass `privacy` to `initialize()` so the value is in place first. A later change may not
take effect until the next app launch.

### `childDirected: false` vs. not setting it

They are different states. Unset leaves each network's own default (or a value an IAB TCF CMP
wrote) untouched. Once you have set `true`, switch back with an explicit `false` — Unity's metadata
API has no clear operation, so unsetting cannot undo it.

### GDPR / CCPA

`gdprConsent`, `ccpaDoNotSell` and `usPrivacy` are propagated where a network exposes an API.
Collecting consent is your responsibility — the SDK does not present a CMP. If you use an IAB TCF
CMP, Google, Pangle, Teads and Naver read the stored strings directly, independently of these
settings.

### ATT on iOS

The SDK does not prompt. Your app calls `NapSspAd.requestTrackingAuthorization()` and waits for the
result before the first ad request. Add `NSUserTrackingUsageDescription` to `Info.plist`.

---

## Testing

### How do I get test ads?

Android only:

```ts
await NapSspAd.setTestMode(true);
await NapSspAd.setTestDeviceIds(['YOUR-GAID']);
```

Test device IDs are Google Advertising IDs — sensitive identifiers. Keep them out of logs, tickets
and support mail.

iOS has no global test switch; register test devices in each network's own dashboard. `setTestMode()`
resolves `false` there so you can branch on it.

### Can I run the example app without real ad units?

It will run, but every request will fail with `nap_ssp_no_ads` or `nap_ssp_config_failed` — which is
exactly what the event log is for. Replace `adConfig.ts` with your own IDs to see fills.

---

## Compatibility

### Does this support the New Architecture?

**Not verified, and therefore not claimed.** The plugin ships legacy `ReactPackage` modules and
`SimpleViewManager` views. React Native's interop layer is expected to handle them, but no build or
runtime check has been run against Fabric/TurboModules. The example app pins
`newArchEnabled=false` for that reason. If you need New Architecture support, test it in your own
app before shipping.

### Does it work with Expo?

Only with a development build or a prebuild — it contains native code, so Expo Go cannot load it.
Add it as a plugin-free native dependency and run `expo prebuild`.

### Which React Native versions?

0.76 and newer (`compileSdk 35` is required by the native SDK). 0.81+ needs no extra Kotlin
configuration; 0.76–0.80 need `kotlinVersion = "2.1.21"`.
