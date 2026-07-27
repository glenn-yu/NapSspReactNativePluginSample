# react-native-nap-ssp

[![npm version](https://img.shields.io/npm/v/react-native-nap-ssp.svg?style=flat-square)](https://www.npmjs.com/package/react-native-nap-ssp)
[![Android BOM](https://img.shields.io/badge/Android%20BOM-2026.07.03-blue.svg?style=flat-square)](https://github.com/Nasmedia-Tech)
[![Core SDK](https://img.shields.io/badge/Core%20SDK-v2.1.1-brightgreen.svg?style=flat-square)](https://github.com/Nasmedia-Tech)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

The official **React Native bridge** for **KT Nasmedia's NapSSP Native & Hybrid Advertising SDK**.  
Monetize your React Native apps seamlessly with high-performance programmatic display banners, custom native ad layouts, outstream video ads, and rewarded video campaigns.

---

## 🚀 What's New in v0.3.0
* **Android BOM Adoption**: Fully migrated to the official Nasmedia AdMixer Bill of Materials (`io.github.nasmedia-tech:admixer-bom:2026.07.03`), upgrading Core SDK from `v2.0.0` to **`v2.1.1`**.
* **Issue #100 Crash Patch**: Resolved runtime `NullPointerException` during view destruction via Core SDK v2.1.1 and 3-tier React Native unmount lifecycle defenses.
* **Standardized Native View IDs**: All Android native layout resource IDs are now namespaced under `nap_mx_*` (e.g., `nap_mx_tv_title`, `nap_mx_iv_icon`, `nap_mx_btn_cta`) to prevent resource collisions with host apps or third-party libraries.
* **Asynchronous `cancelLoad()` API**: Safely abort ongoing ad load requests without dropping or disrupting displaying ads when users navigate away from screens.
* **Standardized 2-Argument Callbacks**: Full support for Kotlin `onFailedToReceiveAd(code: Int, msg: String?)` via native reflection and JS event bridges.

---

## 📚 Documentation & Guides

We have streamlined our documentation into four comprehensive guides to help you build, integrate, and troubleshoot with ease:

| Guide | Description |
| :--- | :--- |
| 🚀 **[Setup & Installation](./docs/SETUP.md)** | Step-by-step instructions for React Native, Android BOM, iOS CocoaPods/SPM, and Expo environments. |
| 📖 **[API Reference & Usage](./docs/API.md)** | Detailed documentation for `BannerAd`, `NativeAd`, `VideoAd`, fullscreen ads (`InterstitialAd`, `RewardedAd`, `InterstitialVideoAd`), `cancelLoad()`, and mediation config. |
| 🔄 **[Migration & Version Matrix](./docs/MIGRATION.md)** | Guide for upgrading to v0.3.0, breaking changes (`nap_mx_*` view IDs), and verified compatibility matrix across third-party networks (GAM, AdFit, Pangle, AppLovin, etc.). |
| ❓ **[FAQ & Troubleshooting](./docs/FAQ.md)** | Solutions for known issues (including Issue #100 crash resolution), privacy compliance (ATT, GDPR, COPPA), and ad-tech glossary. |

---

## ⚡ Quick Start

### 1. Install Package
```bash
npm install react-native-nap-ssp
# or
yarn add react-native-nap-ssp
```

### 2. Initialize SDK
In your root application entry (e.g., `App.tsx`):

```tsx
import React, { useEffect } from 'react';
import { initSdk, setAdapterConfig } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    async function setupAdMixer() {
      // Initialize core SDK
      await initSdk({ debug: __DEV__ });
      
      // Optional: Configure third-party mediation networks
      await setAdapterConfig('applovin', { sdkKey: 'YOUR_APPLOVIN_KEY' });
      await setAdapterConfig('pangle', { coppa: 0, gdpr: 1 });
    }
    setupAdMixer();
  }, []);

  return <YourAppRoot />;
}
```

### 3. Display Banner Ad
```tsx
import React from 'react';
import { View } from 'react-native';
import { BannerAd } from 'react-native-nap-ssp';

export default function HomeScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'flex-end' }}>
      <BannerAd
        adUnitId="YOUR_BANNER_AD_UNIT_ID"
        size="BANNER_320x50"
        autoLoad={true}
        onAdLoaded={() => console.log('Banner loaded')}
        onAdFailedToLoad={(err) => console.warn('Banner error:', err.message)}
      />
    </View>
  );
}
```

### 4. Load & Show Fullscreen Interstitial
```tsx
import React, { useEffect } from 'react';
import { Button } from 'react-native';
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('YOUR_INTERSTITIAL_UNIT_ID');

export default function GameScreen() {
  useEffect(() => {
    const unsubscribe = interstitial.addAdEventListener('onAdLoaded', () => {
      console.log('Interstitial ready!');
    });
    interstitial.load();

    return () => {
      unsubscribe();
      // Safely cancel loading if the user leaves the screen before ad finishes loading
      if (interstitial.isLoading()) {
        interstitial.cancelLoad();
      }
    };
  }, []);

  return (
    <Button
      title="Show Interstitial Ad"
      onPress={() => interstitial.show()}
    />
  );
}
```

---

## 🛡️ License

This library is licensed under the [MIT License](./LICENSE).  
Copyright © 2026 KT Nasmedia Tech. All rights reserved.
