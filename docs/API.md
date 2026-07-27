# 📖 API Reference & Advanced Usage Guide

This document provides complete API descriptions for all components, classes, methods, and event listeners available in `react-native-nap-ssp`.

---

## 📋 Table of Contents
1. [SDK Initialization](#1-sdk-initialization)
2. [Ad Components (UI Views)](#2-ad-components-ui-views)
   - [BannerAd](#bannerad)
   - [NativeAd](#nativead)
   - [VideoAd](#videoad)
3. [Fullscreen Ad Classes](#3-fullscreen-ad-classes)
   - [InterstitialAd](#interstitialad)
   - [RewardedAd](#rewardedad)
   - [InterstitialVideoAd](#interstitialvideoad)
   - [cancelLoad() API](#cancelload-api)
4. [Event Emitter & Error Handling](#4-event-emitter--error-handling)
5. [Mediation Adapter Configuration](#5-mediation-adapter-configuration)

---

## 1. SDK Initialization

Before loading any ads, initialize the core SDK early in your application lifecycle (e.g., in `App.tsx` or `index.js`).

```typescript
import { initSdk, setAdapterConfig } from 'react-native-nap-ssp';

// Initialize SDK with your media key or app identifier
await initSdk({
  appId: 'your-app-id', // Optional
  debug: __DEV__,       // Enable verbose logging in development mode
});

// Configure third-party mediation adapter settings (e.g., AppLovin SDK Key, Pangle App ID)
await setAdapterConfig('applovin', { sdkKey: 'YOUR_APPLOVIN_KEY' });
```

---

## 2. Ad Components (UI Views)

### BannerAd
Displays a standard HTML or rich-media banner ad.

```tsx
import React from 'react';
import { BannerAd } from 'react-native-nap-ssp';

export default function MyBannerComponent() {
  return (
    <BannerAd
      adUnitId="YOUR_BANNER_AD_UNIT_ID"
      size="BANNER_320x50" // Supported sizes: BANNER_320x50, BANNER_300x250, LARGE_BANNER, MEDIUM_RECTANGLE, etc.
      autoLoad={true}
      onAdLoaded={() => console.log('Banner loaded successfully')}
      onAdFailedToLoad={(err) => console.warn('Banner failed:', err.message)}
      onAdClicked={() => console.log('Banner clicked')}
    />
  );
}
```

### NativeAd
Renders a custom native ad template integrated seamlessly into your UI design.
> **Note**: In v0.3.0+, all native view resource IDs use the official `nap_mx_*` prefix to prevent resource collisions.

```tsx
import { NativeAd } from 'react-native-nap-ssp';

<NativeAd
  adUnitId="YOUR_NATIVE_AD_UNIT_ID"
  style={{ width: 320, height: 250 }}
  onAdLoaded={() => console.log('Native ad ready')}
/>
```

### VideoAd
Embeds an outstream video ad player inside your component hierarchy.

```tsx
import { VideoAd } from 'react-native-nap-ssp';

<VideoAd
  adUnitId="YOUR_VIDEO_AD_UNIT_ID"
  autoPlay={true}
  muted={true}
  onVideoCompleted={() => console.log('Video playback completed')}
/>
```

---

## 3. Fullscreen Ad Classes

All fullscreen ads (`InterstitialAd`, `RewardedAd`, `InterstitialVideoAd`) share an identical, promise-based API pattern.

### InterstitialAd
```typescript
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('YOUR_INTERSTITIAL_UNIT_ID');

// Listen to lifecycle events
const unsubscribe = interstitial.addAdEventListener('onAdLoaded', () => {
  console.log('Interstitial ready to display');
  interstitial.show();
});

// Start loading the ad
interstitial.load();
```

### RewardedAd
```typescript
import { RewardedAd } from 'react-native-nap-ssp';

const rewarded = new RewardedAd('YOUR_REWARDED_UNIT_ID');

rewarded.addAdEventListener('onRewarded', (reward) => {
  console.log(`User earned reward: ${reward.amount} ${reward.type}`);
});

rewarded.load();
```

### InterstitialVideoAd
```typescript
import { InterstitialVideoAd } from 'react-native-nap-ssp';

const interstitialVideo = new InterstitialVideoAd('YOUR_VIDEO_UNIT_ID');
interstitialVideo.load();
```

### `cancelLoad()` API
Added in **v0.3.0**, `cancelLoad()` safely aborts an ongoing ad load request without dropping or disrupting currently displaying ads. Useful when a user navigates away from a screen before an ad finishes loading.

```typescript
// If the ad is currently in a loading state, cancel it safely
if (interstitial.isLoading()) {
  await interstitial.cancelLoad();
  console.log('Ad loading aborted successfully.');
}
```

---

## 4. Event Emitter & Error Handling

When an ad fails to load, the SDK returns a structured `AdError` object:

```typescript
export interface AdError {
  code: number | string; // Numeric error code or standardized error string
  message: String;       // Human-readable description of the failure
  domain?: string;       // Origin network (e.g., 'admixer', 'applovin')
}
```

Common error codes include:
* `-1` / `NO_FILL`: Ad network returned a successful response but no ad inventory was available.
* `NAP_SSP_INVALID_AD_UNIT`: Missing or invalid adUnitId.
* `NAP_SSP_LOAD_IN_PROGRESS`: Attempted to call `.load()` while a previous request is active.

---

## 5. Mediation Adapter Configuration

When using third-party networks (e.g., Kakao AdFit, AppLovin, Pangle, Google Ad Manager), configure network-specific keys prior to initialization:

```typescript
import { setAdapterConfig } from 'react-native-nap-ssp';

// Set Kakao AdFit publisher configuration
await setAdapterConfig('adfit', { publisherId: 'YOUR_ADFIT_ID' });

// Set Pangle COPPA / GDPR privacy flags
await setAdapterConfig('pangle', { coppa: 0, gdpr: 1 });
```

For platform migration details and version matrix, see [Migration & Versions Guide](./MIGRATION.md).
