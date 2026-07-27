# ❓ FAQ, Troubleshooting & Privacy Guide

This document combines frequently asked questions, debugging solutions for known compilation and runtime issues, privacy compliance guidelines (GDPR, ATT, COPPA), and a glossary of ad-tech terms.

---

## 📋 Table of Contents
1. [Frequently Asked Questions (FAQ)](#1-frequently-asked-questions-faq)
2. [Troubleshooting & Known Issues](#2-troubleshooting--known-issues)
   - [Android Crash: NullPointerException on Banner View (#100)](#android-crash-nullpointerexception-on-banner-view-100)
   - [Android: "Failed to resolve: com.nasmedia..."](#android-failed-to-resolve-comnasmedia)
   - [iOS: Ads not displaying on iOS 14.5+](#ios-ads-not-displaying-on-ios-145)
3. [Privacy & Compliance (GDPR, ATT, COPPA)](#3-privacy--compliance-gdpr-att-coppa)
4. [Ad-Tech Glossary](#4-ad-tech-glossary)

---

## 1. Frequently Asked Questions (FAQ)

### Q1. Why am I getting error code `-1` (`NO_FILL`)?
* **Answer**: A `NO_FILL` error means the ad request successfully reached the exchange, but no ad campaigns matched your targeting criteria or floor price at that exact moment. This is normal in test environments or newly created ad units. To test integration, ensure you are using valid test unit IDs provided by your Nasmedia account manager.

### Q2. Does this SDK support React Native New Architecture (TurboModules / Fabric)?
* **Answer**: Yes! `react-native-nap-ssp` is built with a dual-mode native bridge layer (`NativeNapSspModuleSpec`, `NativeNapSspInterstitialSpec`) that works seamlessly across both legacy bridge and New Architecture environments.

### Q3. How can I cancel an ad load if the user navigates away?
* **Answer**: Use the `.cancelLoad()` method introduced in v0.3.0. Check `.isLoading()` first, then await `.cancelLoad()` to clean up background requests without affecting displaying ads.

---

## 2. Troubleshooting & Known Issues

### Android Crash: NullPointerException on Banner View (#100)
* **Symptom**: `Fatal Exception: java.lang.NullPointerException: Attempt to read from field '...AdInfo...' on a null object reference in method '...run()'` when closing or unmounting banner screens in SDK v2.0.0.
* **Solution**: This native SDK timing issue is **completely resolved in v0.3.0** by upgrading Core SDK to **`v2.1.1`** (BOM `2026.07.03`). In addition, our native view managers (`BannerViewManager`, `NapSspBannerView`) automatically execute 3-tier lifecycle defenses (`onDropViewInstance`, `onDetachedFromWindow`, `onHostDestroy`) to ensure `destroy()` is called whenever a React Native component unmounts.

### Android: "Failed to resolve: com.nasmedia..." or missing Teads/Pangle AARs
* **Symptom**: Gradle build fails with dependency resolution errors for third-party mediation networks.
* **Solution**: Ensure your root `android/build.gradle` includes `mavenCentral()`, `google()`, and that you haven't overridden repository exclusion rules. Our plugin automatically injects the required Teads (`https://sdk.teads.tv/android/repo`) and Kakao repositories.

### iOS: Ads not displaying on iOS 14.5+ or low CPM
* **Symptom**: Fill rate drops significantly on iOS devices running iOS 14.5 or newer.
* **Solution**: Ensure you have implemented App Tracking Transparency (ATT) and included `NSUserTrackingUsageDescription` in `Info.plist`. Without user consent, IDFA is zeroed out (`00000000-...`), which limits personalized ad inventory.

---

## 3. Privacy & Compliance (GDPR, ATT, COPPA)

### Apple App Tracking Transparency (ATT)
Before requesting ads on iOS, prompt the user for tracking authorization:

```typescript
import { requestTrackingPermission } from 'react-native-tracking-transparency';

const status = await requestTrackingPermission();
if (status === 'authorized') {
  console.log('IDFA access granted');
}
```

### COPPA (Children's Online Privacy Protection Act)
If your app is directed at children under 13, you must flag ad requests accordingly:

```typescript
import { setAdapterConfig } from 'react-native-nap-ssp';

// Set child-directed treatment flag
await setAdapterConfig('pangle', { coppa: 1 });
```

---

## 4. Ad-Tech Glossary
* **SSP (Supply-Side Platform)**: Software used by mobile publishers to automate selling ad impressions to programmatic exchanges.
* **CPM (Cost Per Mille)**: The price an advertiser pays for 1,000 ad impressions.
* **Mediation**: An ad-serving system that calls multiple ad networks (GAM, AdFit, Pangle, AppLovin) to find the highest paying ad for an impression.
* **Fill Rate**: The percentage of ad requests that are successfully answered with an ad (`Impressions / Requests × 100`).
* **BOM (Bill of Materials)**: A Gradle artifact that manages and aligns compatible versions of related library dependencies.
