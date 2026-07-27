# 🔄 Migration Guide & Version Matrix

This document covers upgrading to **`react-native-nap-ssp v0.3.0`**, breaking changes from previous versions, and the verified compatibility matrix between React Native, Android/iOS SDKs, and third-party mediation networks.

---

## 📋 Table of Contents
1. [v0.3.0 Migration Guide](#1-v030-migration-guide)
   - [BOM Adoption & SDK Version Bump](#bom-adoption--sdk-version-bump)
   - [Standardized View ID Prefixes (`nap_mx_*`)](#standardized-view-id-prefixes-nap_mx_)
   - [New `cancelLoad()` Method](#new-cancelload-method)
   - [Standardized 2-Argument Error Callbacks](#standardized-2-argument-error-callbacks)
2. [Verified Version Matrix](#2-verified-version-matrix)
3. [Roadmap & Future Support](#3-roadmap--future-support)

---

## 1. v0.3.0 Migration Guide

### BOM Adoption & SDK Version Bump
In **v0.3.0**, the Android build system transitioned from hardcoded individual coordinates to the official Nasmedia AdMixer Bill of Materials (**BOM `io.github.nasmedia-tech:admixer-bom:2026.07.03`**).
* Core SDK is upgraded from `v2.0.0` to **`v2.1.1`**.
* This upgrade completely patches GitHub issue **#100** (runtime `NullPointerException` during view destruction).

### Standardized View ID Prefixes (`nap_mx_*`)
To prevent resource ID collisions with host applications or third-party libraries, all native ad layout IDs have been migrated to the official `nap_mx_*` namespace:

| Old Resource ID (v0.2.x) | New Official ID (v0.3.0+) | Description |
| :--- | :--- | :--- |
| `nap_ssp_tv_title` | **`nap_mx_tv_title`** | Native ad headline text |
| `nap_ssp_iv_icon` | **`nap_mx_iv_icon`** | Native ad app icon image |
| `nap_ssp_tv_adv` | **`nap_mx_tv_adv`** | Advertiser name / label |
| `nap_ssp_tv_desc` | **`nap_mx_tv_desc`** | Body / description text |
| `nap_ssp_iv_main` | **`nap_mx_iv_main`** | Main media content view |
| `nap_ssp_btn_cta` | **`nap_mx_btn_cta`** | Call-to-action button |

If you override `nap_ssp_native_ad.xml` in your Android project, update your XML view IDs to match the `nap_mx_*` naming convention.

### New `cancelLoad()` Method
You can now safely cancel pending requests on `InterstitialAd`, `RewardedAd`, and `InterstitialVideoAd`:

```typescript
// Old way (No cancellation support)
interstitial.load();

// New in v0.3.0: Cancel loading if user closes modal or leaves screen
if (interstitial.isLoading()) {
  await interstitial.cancelLoad();
}
```

### Standardized 2-Argument Error Callbacks
Android SDK v2.1.1 standardized failure callbacks to `onFailedToReceiveAd(code: Int, msg: String?)`. The React Native bridge (`NapListenerBridge`) seamlessly wraps both legacy 4-argument and modern 2-argument callbacks, ensuring zero breaking changes for your JS event listeners.

---

## 2. Verified Version Matrix

Below is the verified SDK compatibility table for `react-native-nap-ssp` v0.3.0:

| Module / Network | Android Artifact | Android Version | iOS Subspec / Pod | iOS Version |
| :--- | :--- | :--- | :--- | :--- |
| **Core SDK** | `admixer-ssp` | **`2.1.1`** | `AdMixerMediation` | `2.3.x` |
| **Google Ad Manager** | `admixer-gma-nextgen` | `2.0.2` | `AdMixerMediationGAM` | Latest |
| **Kakao AdFit** | `admixer-adfit` | `2.0.3` | `AdMixerMediationAdFit` | Latest |
| **Pangle** | `admixer-pangle` | `2.0.2` | `AdMixerMediationPangle` | Latest |
| **AppLovin** | `admixer-applovin` | `2.0.2` | `AdMixerMediationAppLovin`| Latest |
| **Unity Ads** | `admixer-unity` | `2.0.2` | `AdMixerMediationUnityAds`| Latest |
| **Naver Ad Manager**| `admixer-naveradmanager`| `2.0.2` | `AdMixerMediationNAM` | Latest |
| **Teads** | `admixer-teads` | `2.1.0` | N/A | N/A |
| **Mobwith** | `admixer-mobwith` | `2.0.0` | N/A | N/A |

---

## 3. Roadmap & Future Support
* **Q3 2026**: Full Fabric / TurboModule native C++ layer optimizations.
* **Q4 2026**: Enhanced outstream video bidding analytics and automated layout inspectors.
