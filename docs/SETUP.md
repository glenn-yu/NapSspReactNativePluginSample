# 🚀 Setup & Installation Guide

Setup instructions for `react-native-nap-ssp`, the React Native bridge for KT Nasmedia's **nap mx** (AdMixer SSP) SDK.

> Aligned with the official guides — [Android](https://napmx.github.io/#/android/native/getting-started) (core `2.1.3`, BOM `2026.07.06`) and [iOS](https://napmx.github.io/#/ios/native/getting-started) (`2.4.2`).

---

## 📋 Table of Contents
1. [Prerequisites](#1-prerequisites)
2. [Package Installation](#2-package-installation)
3. [Android Configuration](#3-android-configuration)
4. [iOS Configuration](#4-ios-configuration)
5. [Initialize the SDK](#5-initialize-the-sdk)
6. [Expo](#6-expo)
7. [Verification](#7-verification)

---

## 1. Prerequisites

### Accounts & credentials
Register your media and create ad units on the **nap mx partner site** first — the plugin cannot request ads without them.

| Item | How to obtain |
| :--- | :--- |
| **Media key** | Partner site → register media. **One media key per app.** |
| **Ad unit IDs** | Partner site → create ad units |
| **Google App ID** | Contact [nap_mx@nasmedia.co.kr](mailto:nap_mx@nasmedia.co.kr) |
| **Pangle App ID** | Contact [nap_mx@nasmedia.co.kr](mailto:nap_mx@nasmedia.co.kr) |
| **Unity Ads App ID** | Contact [nap_mx@nasmedia.co.kr](mailto:nap_mx@nasmedia.co.kr) |

### Toolchain

| | Requirement |
| :--- | :--- |
| **React Native** | `>= 0.72.0` (peer dependency). Works on both the legacy bridge and the New Architecture. |
| **React** | `^18.2.0` |
| **TypeScript** | `4.5+` recommended |
| **Android** | `minSdk 21` for the core SDK — **but adapters raise this**, see [§3](#-minimum-api-level-per-network). Gradle 7.0+. |
| **iOS** | Plugin deployment target **14.0** (the SDK itself supports 13.0+; the Kakao AdFit adapter requires 14). **Xcode 15.3+**. |

---

## 2. Package Installation

```bash
npm install react-native-nap-ssp
# or
yarn add react-native-nap-ssp
```

Autolinking handles the native modules. On iOS, run `pod install` afterwards (see [§4](#4-ios-configuration)).

---

## 3. Android Configuration

### ① Vendor SDK opt-in

The plugin compiles against the vendor SDK but **does not bundle it by default**, so a fresh install builds without network access to the ad repositories. Turn it on in `android/gradle.properties`:

```properties
# Link the real nap mx vendor SDK
napSsp.enableVendorSdk=true

# Optional — restrict which mediation adapters get bundled.
# Omit the property entirely to include all of them.
napSsp.mediations=admanager,adfit,pangle,applovin,unity,naveradmanager,teads
```

Valid `napSsp.mediations` values: `admanager`, `adfit`, `pangle`, `applovin`, `unity`, `naveradmanager`, `teads`.

Without `enableVendorSdk=true`, `NapSspAd.getStatus()` reports `placeholderMode: true` and no real ads are served.

### ② Bundled artifact versions

The plugin applies the official **Bill of Materials** so every `admixer-*` member stays version-aligned:

| Artifact | Version |
| :--- | :--- |
| `io.github.nasmedia-tech:admixer-bom` | `2026.07.06` |
| `io.github.nasmedia-tech:admixer-ssp` (core) | `2.1.3` |
| `io.github.nasmedia-tech:admixer-admanager` | `2.0.4` |
| `io.github.nasmedia-tech:admixer-adfit` | `2.0.3` |
| `io.github.nasmedia-tech:admixer-teads` | `2.1.0` |
| `admixer-pangle` · `admixer-applovin` · `admixer-unity` · `admixer-naveradmanager` | `2.0.2` |
| `com.google.android.gms:play-services-ads-identifier` | `18.2.0` |

Third-party Maven repositories (Kakao, Pangle, Teads, Huawei) are declared by the plugin's own `build.gradle`. Your root `android/build.gradle` only needs the standard repositories:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

> ⚠️ **Pin `play-services-ads` to 25.2.0.** Versions `25.3.0+` are incompatible with the AdManager adapter. If another dependency pulls a newer version transitively, force it in your app module:
> ```gradle
> configurations.all {
>     resolutionStrategy {
>         force 'com.google.android.gms:play-services-ads:25.2.0'
>     }
> }
> ```

### ③ Minimum API level per network

The core SDK is API 21, but each bundled adapter raises your app's effective `minSdkVersion`:

| Network | Minimum API |
| :--- | :--- |
| Core (`admixer-ssp`), Kakao AdFit, Teads | **21** (Android 5.0) |
| Google AdManager, Pangle, Unity Ads, Naver Ad Manager | **23** (Android 6.0) |
| AppLovin | **24** (Android 7.0) |

### ④ Kotlin toolchain

Some network SDKs pull a newer `kotlin-stdlib` transitively. Build the host app with:

| Configuration | Minimum Kotlin |
| :--- | :--- |
| Core only | 1.8+ (Java-only projects are fine) |
| \+ Google AdManager, Naver Ad Manager | **2.1+** |
| \+ Kakao AdFit | **2.0+** |
| \+ Pangle / AppLovin / Unity / Teads | no effect |

Building with an older compiler surfaces `Module was compiled with an incompatible version of Kotlin`.

### ⑤ AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- Required for advertising ID on Android 13+ (API 33+) -->
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />

    <application ...>
        <!-- Required when using the Google AdManager adapter -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="YOUR_GOOGLE_APP_ID" />
    </application>
</manifest>
```

**Naver Ad Manager** needs no manifest entry — `com.naver.gfpsdk.PUBLISHER_CD` ships inside the adapter AAR. Do **not** declare it yourself.

**If your app declares its own `networkSecurityConfig`**, the core SDK also declares one, so the manifest merge conflicts. Give your app precedence:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <application
        android:networkSecurityConfig="@xml/your_network_security_config"
        tools:replace="android:networkSecurityConfig">
    </application>
</manifest>
```

### ⑥ ProGuard / R8

The plugin ships `consumer-rules.pro` and the vendor SDK ships its own, so release builds need no manual configuration.

### ⑦ Android 15 / 16 KB page size

All bundled networks that contain native libraries (Pangle, AppLovin) ship 16 KB-aligned `.so` files. No action required.

---

## 4. iOS Configuration

### ① CocoaPods

```bash
cd ios && pod install
```

Mediation adapters are opt-in via subspecs — declare only the networks you actually serve:

```ruby
target 'YourApp' do
  use_frameworks!

  pod 'NapSspPlugin', :path => '../node_modules/react-native-nap-ssp', :subspecs => [
    'GAM',       # Google Ad Manager
    'AdFit',     # Kakao AdFit
    'Pangle',
    'AppLovin',
    'UnityAds',
    'NAM',       # Naver Ad Manager
    'Teads',
  ]
end
```

The core `AdMixerMediation` pod is a direct dependency and is always linked.

### ② Swift Package Manager

`ios/Package.swift` pins the `AdMixerMediation` XCFramework to **2.4.2**. Add the package in **Project → Package Dependencies**, or reference the adapter packages individually:

| Package | Repository URL |
| :--- | :--- |
| nap mx Mediation | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM.git` |
| nap mx Core | `https://github.com/Nasmedia-Tech/iOS-SSP-SPM.git` |
| Google Ad Manager | `https://github.com/Nasmedia-Tech/iOS-SSP-GAM-SPM.git` |
| Kakao AdFit | `https://github.com/Nasmedia-Tech/iOS-SSP-AdFit-SPM.git` |
| Pangle | `https://github.com/Nasmedia-Tech/iOS-SSP-Pangle-SPM.git` |
| Unity Ads | `https://github.com/Nasmedia-Tech/iOS-SSP-UnityAds-SPM.git` |
| AppLovin | `https://github.com/Nasmedia-Tech/iOS-SSP-AppLovin-SPM.git` |
| Naver Ad Manager | `https://github.com/Nasmedia-Tech/iOS-SSP-NAM-SPM` |
| Teads | `https://github.com/Nasmedia-Tech/iOS-SSP-Teads-SPM.git` |

### ③ Network SDK version ranges

The lower of your app's version and nap mx's range is linked; with no existing pin, the newest version in range is used.

| Adapter | Network SDK | Supported range |
| :--- | :--- | :--- |
| `AdMixerMediationGAM` | Google-Mobile-Ads-SDK | `12.7.0` – `12.14.1` |
| `AdMixerMediationAdFit` | AdFitSDK | `3.14.7` – `3.18.6` (min iOS 14) |
| `AdMixerMediationPangle` | Ads-Global | `7.4.0.8` – `7.8.8.9` |
| `AdMixerMediationUnityAds` | UnityAds | `4.15.1` – `4.16.6` |
| `AdMixerMediationAppLovin` | AppLovinSDK | `13.3.1` – `13.5.2` |
| `AdMixerMediationTeads` | TeadsSDK | `6.2` – `< 7.0` |

### ④ App Tracking Transparency

Add `NSUserTrackingUsageDescription` to `Info.plist`, then prompt before requesting ads:

```xml
<key>NSUserTrackingUsageDescription</key>
<string>This identifier will be used to deliver personalized ads to you.</string>
```

```typescript
import { NapSspAd } from 'react-native-nap-ssp';

await NapSspAd.requestTrackingAuthorization();   // resolves 'unavailable' on Android
```

Without consent the IDFA is zeroed (`00000000-…`), which materially reduces fill and CPM.

---

## 5. Initialize the SDK

Call `initialize()` **once**, before any ad is requested.

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: 'YOUR_MEDIA_KEY',
      adUnitIds: [
        'YOUR_BANNER_UNIT_ID',
        'YOUR_INTERSTITIAL_UNIT_ID',
        'YOUR_NATIVE_UNIT_ID',
        'YOUR_REWARDED_UNIT_ID',
        'YOUR_VIDEO_UNIT_ID',
      ],
      logLevel: __DEV__ ? 'verbose' : 'error',
      mediations: {
        adManager: { googleAppId: 'YOUR_GOOGLE_APP_ID' },
        adFit: true,
        naverAdManager: true,
      },
    }).catch((error) => console.warn('nap ssp init failed', error));
  }, []);

  return <YourAppRoot />;
}
```

Every ad unit you intend to use must appear in `adUnitIds`. Full options are documented in the [API Reference](./API.md#1-sdk-initialization-napsspad).

---

## 6. Expo

The plugin contains native code, so it requires a custom dev client — it does not run in Expo Go.

```bash
npx expo install react-native-nap-ssp
npx pod-install ios

npx expo run:android   # or run:ios / EAS Build
```

The Android Gradle properties from [§3①](#-vendor-sdk-opt-in) still apply; set them in `android/gradle.properties` after prebuild, or via an EAS Build config.

---

## 7. Verification

```bash
npm run verify        # typecheck + build + smoke test
```

At runtime, confirm the native layer is actually linked:

```typescript
const status = await NapSspAd.getStatus();
console.log(status.placeholderMode);  // false  → vendor SDK is linked
console.log(status.sdkCoordinates);   // resolved native artifact versions
```

If `placeholderMode` is `true`, revisit [§3①](#-vendor-sdk-opt-in) on Android or your Podfile subspecs on iOS.

Stuck? See the [FAQ & Troubleshooting Guide](./FAQ.md).
