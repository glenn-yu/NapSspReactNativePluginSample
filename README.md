# react-native-nap-ssp

[![npm version](https://img.shields.io/npm/v/react-native-nap-ssp.svg?style=flat-square)](https://www.npmjs.com/package/react-native-nap-ssp)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-v2.1.3-brightgreen.svg?style=flat-square)](https://napmx.github.io/#/android/)
[![iOS SDK](https://img.shields.io/badge/iOS%20SDK-v2.4.2-brightgreen.svg?style=flat-square)](https://napmx.github.io/#/ios/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

The **React Native bridge** for **KT Nasmedia's nap mx (AdMixer SSP) SDK**.
Monetize React Native apps with banners, native ads, inline video, interstitials, interstitial video, and rewarded video.

---

## 🚀 What's New in v0.4.0

* **Android SDK `2.1.1` → `2.1.3`** (BOM `2026.07.03` → `2026.07.06`, AdManager adapter `2.0.2` → `2.0.4`) — stability fixes plus deterministic failure reporting for unregistered ad units.
* **iOS SDK `2.3.7` → `2.4.2`** — improved `loadAd` handling, a simulator launch fix, and adapter refreshes.
* **iOS Teads support** — `AdMixerMediationTeads` is now available as the `Teads` subspec. Teads was previously Android-only here.
* **Huawei Maven repository** added on Android, required by the official Teads installation guide.
* **Documentation corrected** — the guides now match the actual exported API surface. See the note below if you followed the v0.3.0 docs.

> ⚠️ **If you copied code from the v0.3.0 README or API guide**, it referenced `initSdk()` and `setAdapterConfig()`, which this package has never exported. Initialization is `NapSspAd.initialize({ mediaKey, adUnitIds, ... })`, and mediation keys go in that same `mediations` object. See the [API Reference](./docs/API.md).

> ℹ️ **iOS 2.4.2 was source-verified** by diffing the shipped `.swiftinterface` of both SDK versions against every symbol this plugin calls. One breaking change was found and fixed (`AMMVideoInterstitial.load`, see the [Migration Guide](./docs/MIGRATION.md#1-v040--native-sdk-refresh)). A full Xcode build has not been run, so smoke-test your iOS target before shipping.

---

## 📚 Documentation

| Guide | Contents |
| :--- | :--- |
| 🚀 **[Setup & Installation](./docs/SETUP.md)** | React Native, Android Gradle & BOM, iOS CocoaPods/SPM, Expo, per-network minSdk and Kotlin requirements. |
| 📖 **[API Reference](./docs/API.md)** | `NapSspAd`, `BannerAd`, `NativeAd`, `VideoAd`, `InterstitialAd`, `RewardedAd`, `InterstitialVideoAd`, events, errors, mediation config. |
| 🔄 **[Migration & Version Matrix](./docs/MIGRATION.md)** | Upgrade steps, the verified version matrix, and native SDK breaking changes. |
| ❓ **[FAQ & Troubleshooting](./docs/FAQ.md)** | Build and runtime fixes, privacy compliance (ATT, COPPA), glossary. |

Official native SDK guides: [Android](https://napmx.github.io/#/android/) · [iOS](https://napmx.github.io/#/ios/)

---

## ⚡ Quick Start

### 1. Install

```bash
npm install react-native-nap-ssp
# or
yarn add react-native-nap-ssp
```

**Android** — enable the vendor SDK in `android/gradle.properties`:

```properties
napSsp.enableVendorSdk=true
```

**iOS** — `cd ios && pod install`.

### 2. Initialize

Call this once, before requesting any ad. Every ad unit you use must be registered here.

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: 'YOUR_MEDIA_KEY',
      adUnitIds: ['YOUR_BANNER_UNIT_ID', 'YOUR_INTERSTITIAL_UNIT_ID'],
      logLevel: __DEV__ ? 'verbose' : 'error',
      mediations: {
        adManager: { googleAppId: 'YOUR_GOOGLE_APP_ID' },
        appLovin: { sdkKey: 'YOUR_APPLOVIN_SDK_KEY' },
        adFit: true,
      },
    }).catch((error) => console.warn('nap ssp init failed', error));
  }, []);

  return <YourAppRoot />;
}
```

### 3. Show a banner

```tsx
import { View } from 'react-native';
import { BannerAd } from 'react-native-nap-ssp';

export default function HomeScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'flex-end' }}>
      <BannerAd
        adUnitId="YOUR_BANNER_UNIT_ID"
        size="BANNER_320x50"
        onAdLoaded={() => console.log('banner loaded')}
        onAdFailedToLoad={(error) => console.warn(error.code, error.message)}
      />
    </View>
  );
}
```

### 4. Load and show an interstitial

```tsx
import React, { useEffect, useRef } from 'react';
import { Button } from 'react-native';
import { InterstitialAd } from 'react-native-nap-ssp';

export default function GameScreen() {
  const adRef = useRef<InterstitialAd>();

  useEffect(() => {
    const interstitial = new InterstitialAd('YOUR_INTERSTITIAL_UNIT_ID');
    adRef.current = interstitial;

    const unsubscribe = interstitial.addAdEventListener('loaded', () => {
      console.log('interstitial ready');
    });
    interstitial.addAdEventListener('loadFailed', (error) => {
      console.warn(error.code, error.message);
    });

    interstitial.load();

    return () => {
      unsubscribe();
      interstitial.cancelLoad();  // abort an in-flight load
      interstitial.destroy();
    };
  }, []);

  return <Button title="Show Ad" onPress={() => adRef.current?.show()} />;
}
```

Event names are the short form (`loaded`, `loadFailed`, `opened`, `closed`, `clicked`, `impression`, `rewarded`, `completed`, `skipped`) — see the [full event table](./docs/API.md#event-names).

---

## 📦 Supported Ad Formats

| Format | Export | Android | iOS |
| :--- | :--- | :---: | :---: |
| Banner | `BannerAd` | ✅ | ✅ |
| Native | `NativeAd` | ✅ | ✅ |
| Inline video | `VideoAd` | ✅ | ✅ |
| Interstitial | `InterstitialAd` | ✅ | ✅ |
| Interstitial video | `InterstitialVideoAd` | ✅ | ✅ |
| Rewarded video | `RewardedAd` | ✅ | ✅ |

**Mediation networks**: Google Ad Manager, Kakao AdFit, Pangle, AppLovin, Unity Ads, Naver Ad Manager, Teads.

---

## 🛡️ License

[MIT](./LICENSE) — Copyright © 2026 KT Nasmedia.
