# react-native-nap-ssp

[![npm version](https://img.shields.io/npm/v/react-native-nap-ssp.svg?style=flat-square)](https://www.npmjs.com/package/react-native-nap-ssp)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-2.5.1-brightgreen.svg?style=flat-square)](https://napmx.github.io/#/android/)
[![iOS SDK](https://img.shields.io/badge/iOS%20SDK-2.6.1-brightgreen.svg?style=flat-square)](https://napmx.github.io/#/ios/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)

React Native bridge for **nap mx** (KT Nasmedia's AdMixer SSP) — banner, native, inline video,
interstitial, interstitial video and rewarded video, with the mediation waterfall handled by the
native SDK.

Official native guides: [Android](https://napmx.github.io/#/android/) · [iOS](https://napmx.github.io/#/ios/)

---

## What's new in 0.6.1

* Correct CocoaPods adapter installation guidance and improve the initialization and upgrade examples.

## What's new in 0.6.0

* **Native SDK update** — Android core `2.5.1` (BOM `2026.11.02`); iOS Mediation `2.6.1` or newer in the 2.x series with Core `1.5.1` or newer.
* **iOS installation changes** — adapters are explicitly opt-in; install the React Native bridge with CocoaPods. The unsupported standalone SPM manifest was removed.
* Android SDK error numbers changed; rebuild the app and use the documented string error codes.

## What's new in 0.5.0

0.5.0 is a correctness release. If you are on 0.4.x, read the
[migration notes](./docs/MIGRATION.md) — there are breaking changes.

* **Placeholder simulation is gone.** Earlier versions replaced the real SDK with fake events in
  debug builds and reported load failures as successes. Every build now takes the real SDK path and
  failures surface as failures.
* **Privacy signals actually reach the SDK.** `setCoppa()` only ever stored a local flag. There is
  now a real `setPrivacyConsent()` (COPPA / GDPR / CCPA) wired to
  `AdMixer.setTagForChildDirectedTreatment` on Android and `AMMConsent` on iOS.
* **Test mode** — `setTestMode()` / `setTestDeviceIds()` (Android; the iOS SDK has no equivalent).
* **Native SDKs refreshed** — Android core `2.1.3 → 2.3.0` (BOM `2026.09.03`), iOS `2.4.2 → 2.5.0`.
* **Android Kotlin compatibility** — check your host configuration against [Setup](./docs/SETUP.md#3-3-kotlin-compatibility).

Full details in the [CHANGELOG](./CHANGELOG.md).

---

## Documentation

| Guide | Contents |
| :--- | :--- |
| 🚀 **[Setup](./docs/SETUP.md)** | Install, Android Gradle & mediation adapters, iOS CocoaPods, Kotlin and minSdk requirements, ATT. |
| 📖 **[API reference](./docs/API.md)** | Every export, option, event and error code. |
| 🔄 **[Migration](./docs/MIGRATION.md)** | 0.5.0 → 0.6.0 and 0.4.x → 0.5.0 upgrade steps and validation limits. |
| ❓ **[FAQ](./docs/FAQ.md)** | Build and runtime troubleshooting, privacy compliance, known platform gaps. |

A runnable integration reference lives in [`example/ExampleHostApp`](https://github.com/glenn-yu/NapSspReactNativePluginSample/tree/main/example/ExampleHostApp) — it exercises
initialize → load → callback → show → reload → cleanup for all six formats with a live event log.

---

## Quick start

### 1. Install

```bash
npm install react-native-nap-ssp
```

**Android** — pick the mediation adapters you sell, in `android/gradle.properties`:

```properties
# comma separated, or "all". Unset links the nap mx core SDK only.
napSsp.mediations=admanager,adfit
```

Check your host app’s Kotlin and Android build settings as described in [Setup](./docs/SETUP.md#3-3-kotlin-compatibility).

**iOS** — in the existing Podfile target, declare the local plugin and selected adapters before
`use_native_modules!`, then install pods (adjust the path for hoisted dependencies):

```ruby
pod 'NapSspPlugin', :path => '../node_modules/react-native-nap-ssp',
    :subspecs => ['GAM', 'AdFit']
config = use_native_modules!
```

See [Setup](./docs/SETUP.md#4-1-cocoapods) for details and validation limits.

```bash
cd ios && pod install
```

### 2. Initialize

Call this once, before requesting any ad. Every ad unit you will use must be registered here, and
privacy signals belong in this call — several networks read consent only when they start up.

```tsx
import React, {useEffect, useState} from 'react';
import {Platform} from 'react-native';
import {NapSspAd} from 'react-native-nap-ssp';

// Share the startup promise across mounts, including React Strict Mode's development remount.
let startup: Promise<unknown> | undefined;
function startAds() {
  if (!startup) {
    startup = (async () => {
      if (Platform.OS === 'ios') {
        await NapSspAd.requestTrackingAuthorization();
      }
      return NapSspAd.initialize({
        mediaKey: '10771',
        adUnitIds: ['104701', '104703', '103722'],
        logLevel: __DEV__ ? 'verbose' : 'error',
        privacy: {childDirected: false},
        testMode: __DEV__, // Android only
      });
    })();
  }
  return startup;
}

export default function App() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let active = true;
    startAds().then(() => {
      if (active) setReady(true);
    }).catch((error) => console.warn('nap mx init failed', error));
    return () => { active = false; };
  }, []);

  // YourAppRoot may render ads only after initialization succeeds.
  return ready ? <YourAppRoot /> : null;
}
```

This startup example does not retry a failed initialization. Implement retry and error UI in your app.

> `mediaKey` and `adUnitIds` are the **numeric** values issued on the
> [partner site](https://publisher.admixer.co.kr). One media key per app.

### 3. Show a banner

Render this component only inside the initialized app tree above. The served size comes from the ad unit's server configuration; `size` is only a layout hint.

```tsx
import {useRef} from 'react';
import {BannerAd, type AdViewHandle} from 'react-native-nap-ssp';

function Footer() {
  const bannerRef = useRef<AdViewHandle>(null);

  return (
    <BannerAd
      ref={bannerRef}
      adUnitId="104701"
      size="BANNER_320x50"
      onAdLoaded={() => console.log('banner loaded')}
      onAdFailedToLoad={(error) => console.warn(error.code, error.message)}
    />
  );
  // bannerRef.current?.reload() requests a fresh ad.
}
```

### 4. Load and show an interstitial

Render this screen only after initialization succeeds. This example loads one ad; load another after it closes to show again.

```tsx
import {useEffect, useRef, useState} from 'react';
import {Button} from 'react-native';
import {InterstitialAd} from 'react-native-nap-ssp';

function GameScreen() {
  const adRef = useRef<InterstitialAd | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const ad = new InterstitialAd('104703');
    adRef.current = ad;

    const unsubscribe = ad.addAdEventListener('loadFailed', (error) =>
      console.warn(error.code, error.message),
    );

    let active = true;
    ad.load().then(() => {
      if (active) setReady(true);
    }).catch((error) => console.warn('load rejected', error.code));

    return () => {
      active = false;
      adRef.current = null;
      unsubscribe();
      ad.destroy(); // cancels an in-flight load and releases the native ad
    };
  }, []);

  return <Button title="Show ad" disabled={!ready} onPress={() => {
    const ad = adRef.current;
    if (!ad || !ready) return;
    setReady(false);
    ad.show().catch((error) => console.warn('show rejected', error.code));
  }} />;
}
```

`load()` resolves when the SDK reports a fill and rejects with an [`AdError`](./docs/API.md#aderror)
when the whole waterfall fails. Event names are the short form — `loaded`, `loadFailed`, `opened`,
`closed`, `clicked`, `impression`, `rewarded`, `completed`, `skipped`.

### 5. Reward a user

After initialization succeeds, grant the reward from the `rewarded` event and reconcile it with your
server using `transactionId`, which matches `transaction_id` on the S2S reward callback. Handle
rejections and release the listener and ad when the owning screen unmounts. `show()` does not
wait for the ad to close, so keep the listener active until cleanup:

```tsx
import {RewardedAd} from 'react-native-nap-ssp';

// Call after initialization; userId and grantReward come from your app.
function prepareRewardedAd(userId: string) {
  const ad = new RewardedAd('103722', {customParams: {userId}});
  const unsubscribe = ad.addAdEventListener('rewarded', ({transactionId}) =>
    grantReward(userId, transactionId),
  );

  return {
    async show() {
      try {
        await ad.load();
        await ad.show();
      } catch (error) {
        console.warn('Rewarded ad failed', error);
      }
    },
    // Call from the owning screen's unmount cleanup, not immediately after show().
    cleanup() {
      unsubscribe();
      ad.destroy();
    },
  };
}

```

---

## Supported formats

| Format | Export | Android | iOS |
| :--- | :--- | :---: | :---: |
| Banner | `BannerAd` | ✅ | ✅ |
| Native | `NativeAd` | ✅ | ✅ |
| Inline video | `VideoAd` | ✅ | ✅ |
| Interstitial | `InterstitialAd` | ✅ | ✅ |
| Interstitial video | `InterstitialVideoAd` | ✅ | ✅ |
| Rewarded video | `RewardedAd` | ✅ | ✅ |

**Mediation networks**: Google Ad Manager, Kakao AdFit, Pangle, AppLovin, Unity Ads,
Naver Ad Manager, Teads.

### Known platform differences

| Behaviour | Android | iOS |
| :--- | :--- | :--- |
| `skipped` event (rewarded, interstitial video) | ✅ | ❌ — the iOS delegates have no skip callback |
| `setTestMode()` / `setTestDeviceIds()` | ✅ | ❌ — resolves `false`; register test devices per network |
| `privacy.usPrivacy` (IAB US Privacy string) | ✅ | ❌ |
| `privacy.underAgeOfConsent` | ❌ | ✅ |
| `InterstitialAdOptions.disableBackKey` | ✅ | n/a |
| `RewardedAdOptions.mute`, `InterstitialVideoAdOptions.timeout` | ✅ | ❌ |

### Not supported

The New Architecture (TurboModules/Fabric) has **not** been verified against this plugin. It ships
legacy `ReactPackage` modules and `SimpleViewManager` views, which the interop layer is expected to
handle, but no build or runtime check has been run — so it is not claimed as supported.

---

## License

[MIT](./LICENSE)
