# 📖 API Reference & Advanced Usage Guide

Complete API reference for every component, class, method and event exported by `react-native-nap-ssp`.

> Every symbol on this page is exported from the package root (`import { ... } from 'react-native-nap-ssp'`) and is verified against `src/index.ts`.

---

## 📋 Table of Contents
1. [SDK Initialization (`NapSspAd`)](#1-sdk-initialization-napsspad)
2. [Ad Components (UI Views)](#2-ad-components-ui-views)
   - [BannerAd](#bannerad)
   - [NativeAd](#nativead)
   - [VideoAd](#videoad)
3. [Fullscreen Ad Classes](#3-fullscreen-ad-classes)
   - [InterstitialAd](#interstitialad)
   - [RewardedAd](#rewardedad)
   - [InterstitialVideoAd](#interstitialvideoad)
   - [cancelLoad()](#cancelload)
4. [Events & Error Handling](#4-events--error-handling)
5. [Mediation Configuration](#5-mediation-configuration)
6. [Utilities](#6-utilities)

---

## 1. SDK Initialization (`NapSspAd`)

Initialization is **required exactly once** before any ad is requested. Call it as early as possible — typically in your root component's mount effect or in `index.js`.

```typescript
import { NapSspAd } from 'react-native-nap-ssp';

await NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',            // required — 앱당 1개만 사용 가능 / one media key per app
  adUnitIds: [                            // required — register every ad unit you will use
    'YOUR_BANNER_UNIT_ID',
    'YOUR_INTERSTITIAL_UNIT_ID',
    'YOUR_NATIVE_UNIT_ID',
    'YOUR_REWARDED_UNIT_ID',
    'YOUR_VIDEO_UNIT_ID',
  ],
  logLevel: __DEV__ ? 'verbose' : 'error',
  coppa: false,
  mediations: {
    adManager: { googleAppId: 'ca-app-pub-XXXXXXXX~YYYYYYYY' },
    appLovin:  { sdkKey: 'YOUR_APPLOVIN_SDK_KEY' },
    pangle:    { appId: 'YOUR_PANGLE_APP_ID' },
    unityAds:  { appId: 'YOUR_UNITY_APP_ID' },
    adFit: true,
    naverAdManager: true,
    teads: true,
  },
});
```

### `NapSspConfig`

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `mediaKey` | `string` | ✅ | Media key issued by the nap mx partner site. Must be non-empty. |
| `adUnitIds` | `readonly string[]` | ✅ | Every ad unit id used by the app. Must contain at least one entry. |
| `mediations` | `MediationConfig` | | Third-party network keys — see [§5](#5-mediation-configuration). |
| `logLevel` | `LogLevel` | | `'verbose' \| 'debug' \| 'info' \| 'warn' \| 'error' \| 'none'`. |
| `coppa` | `boolean` | | Child-directed treatment flag. |

`initialize()` throws synchronously if `mediaKey` is empty or `adUnitIds` is empty.

### Static methods

| Method | Returns | Description |
| :--- | :--- | :--- |
| `NapSspAd.initialize(config)` | `Promise<void>` | Initializes the native SDK and wires the global event bridges. |
| `NapSspAd.isInitialized()` | `boolean` | Whether `initialize()` has completed successfully. |
| `NapSspAd.getConfig()` | `NapSspConfig \| undefined` | A defensive copy of the config passed to `initialize()`. |
| `NapSspAd.getStatus()` | `Promise<NapSspStatus>` | Runtime diagnostics — see below. |
| `NapSspAd.setLogLevel(level)` | `void` | Changes the native log level at runtime. |
| `NapSspAd.setCoppa(enabled)` | `void` | Toggles child-directed treatment at runtime. |
| `NapSspAd.requestTrackingAuthorization()` | `Promise<string>` | iOS ATT prompt. Resolves to `'unavailable'` on Android. |

### `NapSspStatus`

Returned by `getStatus()`; useful for debug screens and integration checks.

```typescript
interface NapSspStatus {
  initialized: boolean;
  placeholderMode?: boolean;          // true when the vendor SDK is not linked
  vendorSdkEnabled?: boolean;
  logLevel?: string;
  coppa?: boolean;
  sdkCoordinates?: Record<string, string>;   // resolved native SDK artifact versions
  configuredAdUnitIds?: readonly string[];
  loadedInterstitialAdUnitIds?: readonly string[];
  loadedRewardedAdUnitIds?: readonly string[];
  trackingAuthorizationStatus?: string;
  runtime?: Record<string, unknown>;
  details?: Record<string, unknown>;
}
```

> ℹ️ **`placeholderMode: true`** means the native vendor SDK is not linked yet. On Android, enable it with the `napSsp.enableVendorSdk=true` Gradle property — see [Setup Guide](./SETUP.md#-vendor-sdk-opt-in).

---

## 2. Ad Components (UI Views)

### BannerAd

```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd
  adUnitId="YOUR_BANNER_UNIT_ID"
  size="BANNER_320x50"
  autoLoad={true}
  onAdLoaded={() => console.log('loaded')}
  onAdFailedToLoad={(error) => console.warn(error.code, error.message)}
  onAdClicked={() => {}}
  onAdImpression={() => {}}
/>
```

| Prop | Type | Description |
| :--- | :--- | :--- |
| `adUnitId` | `string` | **Required.** |
| `size` | `BannerSize` | See table below. |
| `autoLoad` | `boolean` | **Android only.** `false` suppresses the automatic load on mount. Defaults to `true`. |
| `onAdLoaded` | `() => void` | |
| `onAdFailedToLoad` | `(error: AdError) => void` | |
| `onAdClicked` / `onAdOpened` / `onAdClosed` / `onAdImpression` | `() => void` | |
| `style` | `StyleProp<ViewStyle>` | |
| `testID` | `string` | |

**`BannerSize`** — the named sizes below get autocomplete, and any `'BANNER_WxH'` string is also accepted, so a new server-side size works without a plugin update.

| Value | Dimensions |
| :--- | :--- |
| `BANNER_320x50` | 320 × 50 |
| `BANNER_320x100` | 320 × 100 |
| `BANNER_300x250` | 300 × 250 |
| `BANNER_320x480` | 320 × 480 |
| `LARGE_BANNER` | 320 × 100 |
| `MEDIUM_RECTANGLE` | 300 × 250 |
| `SMART_BANNER` | 320 × 50 |

### NativeAd

Renders the native ad template supplied by the plugin.

```tsx
import { NativeAd } from 'react-native-nap-ssp';

<NativeAd
  adUnitId="YOUR_NATIVE_UNIT_ID"
  style={{ width: 320, height: 250 }}
  onAdLoaded={() => {}}
  onAdFailedToLoad={(error) => console.warn(error.message)}
/>
```

Props: `adUnitId` (required), `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdOpened`, `onAdClosed`, `onAdImpression`, `style`, `testID`.

> **Android layout IDs**: since v0.3.0 the bundled layout uses the official `nap_mx_*` resource IDs (`nap_mx_tv_title`, `nap_mx_iv_icon`, `nap_mx_tv_adv`, `nap_mx_tv_desc`, `nap_mx_iv_main`, `nap_mx_btn_cta`). If you override `nap_ssp_native_ad.xml`, match these IDs.
>
> **AdChoices**: Android core SDK v2.1.0 removed `NativeAdViewBinder.Builder.setPrivacyViewId(int)` in favour of `setAdChoicesPosition(...)` (default: top-right). The `nap_mx_privacy_container` slot is no longer needed — the SDK overlays the icon automatically.

### VideoAd

Inline (outstream) video player.

```tsx
import { VideoAd } from 'react-native-nap-ssp';

<VideoAd
  adUnitId="YOUR_VIDEO_UNIT_ID"
  onAdLoaded={() => {}}
  onAdCompleted={() => console.log('playback finished')}
  onAdSkipped={() => {}}
/>
```

| Prop | Type | Description |
| :--- | :--- | :--- |
| `adUnitId` | `string` | **Required.** |
| `isRetry` | `boolean` | **Android only.** Defaults to `false`. |
| `onAdLoaded` / `onAdClicked` / `onAdOpened` / `onAdClosed` / `onAdImpression` / `onAdCompleted` / `onAdSkipped` | `() => void` | |
| `onAdFailedToLoad` | `(error: AdError) => void` | |
| `style` | `StyleProp<ViewStyle>` | |
| `testID` | `string` | |

The container defaults to `minWidth: 300`, `minHeight: 200`, `width: '100%'`.

---

## 3. Fullscreen Ad Classes

`InterstitialAd`, `RewardedAd` and `InterstitialVideoAd` share the same shape:

```typescript
new XxxAd(adUnitId: string, options?: XxxAdOptions)

load():        Promise<void>
show():        Promise<void>   // throws if not loaded
start():       Promise<void>   // native load & show in one step; falls back to load() + show()
cancelLoad():  Promise<void>
isLoaded():    boolean
addAdEventListener(event, handler): () => void   // returns an unsubscribe function
destroy():     void
```

The constructor throws if `adUnitId` is empty.

### InterstitialAd

```typescript
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('YOUR_INTERSTITIAL_UNIT_ID', {
  closeButtonTouchAreaRatio: 0.5,   // iOS only (0.2 ~ 1.0)
});

const unsubscribe = interstitial.addAdEventListener('loaded', () => {
  interstitial.show();
});
interstitial.addAdEventListener('loadFailed', (error) => {
  console.warn(error.code, error.message);
});

await interstitial.load();

// on unmount
unsubscribe();
interstitial.destroy();
```

**`InterstitialAdOptions`**

| Option | Type | Notes |
| :--- | :--- | :--- |
| `closeButtonTouchAreaRatio` | `number` | **iOS only**, `0.2`–`1.0`. Android follows the server-side `AdInfo.setCloseButtonBound(20~100%)`. |

> Since v2 (Android 2.0.0 / iOS 2.3.7) interstitials are **Basic-only**. The `popup` / `countDown` types and their options (`type`, `countDownTime`, `buttonLeftText`, `buttonRightText`) were removed from the native SDKs.

### RewardedAd

```typescript
import { RewardedAd } from 'react-native-nap-ssp';

const rewarded = new RewardedAd('YOUR_REWARDED_UNIT_ID', {
  customParams: { userId: 'abc123' },
  mute: false,   // Android only
});

rewarded.addAdEventListener('rewarded', (reward) => {
  console.log(`earned ${reward.amount} ${reward.type}`);
});
rewarded.addAdEventListener('completed', () => {});
rewarded.addAdEventListener('skipped', () => {});

await rewarded.load();
```

`'onRewarded'` is accepted as an alias for `'rewarded'` and receives the same `RewardPayload` (`{ type: string; amount: number }`).

> ⚠️ **Do not grant the reward twice.** Android core SDK v2.1.1 made the reward channels mutually exclusive: registering a dedicated reward listener suppresses `AdListener.onAdRewarded()`, so exactly one notification is delivered per grant. For server-side verification, the SSV postback now carries a unique `transaction_id` per grant, and `ifa` is omitted when COPPA is on.

### InterstitialVideoAd

```typescript
import { InterstitialVideoAd } from 'react-native-nap-ssp';

const video = new InterstitialVideoAd('YOUR_INTERSTITIAL_VIDEO_UNIT_ID', {
  timeout: 20,            // Android: 0 = server-defined, default 20
  maxRetryCountInSlot: 0, // Android: -1 infinite, 0 none, n times
});

video.addAdEventListener('completed', () => {});
await video.load();
```

### `cancelLoad()`

Added in **v0.3.0**. Aborts an in-flight load without touching an ad that is already displaying — use it when the user leaves the screen before the load resolves.

```typescript
useEffect(() => {
  const interstitial = new InterstitialAd(UNIT_ID);
  interstitial.load();

  return () => {
    interstitial.cancelLoad();   // no-op if nothing is in flight
    interstitial.destroy();
  };
}, []);
```

`cancelLoad()` resolves silently when the native module does not implement it, so it is always safe to call in a cleanup path.

---

## 4. Events & Error Handling

### Event names

Listener names are the **short form** — not the native `onAdXxx` callback names.

| Event | Payload | Interstitial | Rewarded | InterstitialVideo |
| :--- | :--- | :---: | :---: | :---: |
| `loaded` | `void` | ✅ | ✅ | ✅ |
| `loadFailed` | `AdError` | ✅ | ✅ | ✅ |
| `opened` | `void` | ✅ | ✅ | ✅ |
| `closed` | `void` | ✅ | ✅ | ✅ |
| `clicked` | `void` | ✅ | ✅ | ✅ |
| `impression` | `void` | ✅ | ✅ | ✅ |
| `rewarded` | `RewardPayload` | | ✅ | |
| `completed` | `void` | | ✅ | ✅ |
| `skipped` | `void` | | ✅ | ✅ |

`addAdEventListener` returns an unsubscribe function. `destroy()` removes every listener registered on that instance.

### `AdError`

```typescript
interface AdError {
  code: string;                       // plugin-level code, always a string
  message: string;
  nativeCode?: number | string;       // raw code from the native SDK
  nativeDomain?: string;              // originating network / domain
  details?: Record<string, unknown>;  // { details?, userInfo? } passthrough
}
```

Plugin-level fallback codes: `nap_ssp_error`, `interstitial_load_failed`, `interstitial_start_failed`, `rewarded_load_failed`, `rewarded_start_failed`. Network failures surface the SDK's own value in `nativeCode`.

> `AX_ERR_NO_ADS` is the no-fill code on Android. `AdMixer.AX_ERR_NO_FILL` was deprecated in core v2.1.3 — the SDK never emits it, so do not branch on it.

Use `normalizeAdError` to coerce an arbitrary thrown value into an `AdError`:

```typescript
import { normalizeAdError } from 'react-native-nap-ssp';

try {
  await interstitial.load();
} catch (error) {
  const adError = normalizeAdError(error, 'my_fallback_code');
}
```

---

## 5. Mediation Configuration

Mediation is configured **once, inside `NapSspAd.initialize()`** — there is no separate per-network setter. Adapters that ship in the build register themselves automatically.

```typescript
interface MediationConfig {
  adManager?:      { googleAppId?: string };
  pangle?:         { appId: string };
  appLovin?:       { sdkKey: string };
  unityAds?:       { appId: string };
  adFit?:          boolean;
  naverAdManager?: boolean;   // PUBLISHER_CD is provided by the SDK — no host setup
  teads?:          boolean;   // Android: extra Maven repos; iOS: 'Teads' subspec
  mobwith?:        boolean;   // @deprecated — no longer a supported network
}
```

Supported networks per the official guide: **Google AdManager, Kakao AdFit, Pangle, AppLovin, Unity Ads, Naver Ad Manager, Teads** (plus GMA NextGen in beta on Android — see [Migration Guide](./MIGRATION.md)).

Build-side wiring (Gradle properties, CocoaPods subspecs) is covered in the [Setup Guide](./SETUP.md).

---

## 6. Utilities

```typescript
import {
  isNativeModuleAvailable,
  isNativeViewAvailable,
  NativeModuleNames,
  normalizeAdError,
} from 'react-native-nap-ssp';
```

| Export | Description |
| :--- | :--- |
| `isNativeModuleAvailable(name)` | `true` when the given native module is registered — use it to guard optional features. |
| `isNativeViewAvailable(name)` | `true` when the given native view component is registered (e.g. `'NapSspNativeAdView'`, `'NapSspVideoAdView'`). |
| `NativeModuleNames` | The candidate module/view name lists the bridge probes. |
| `normalizeAdError(error, fallbackCode?)` | Coerces any thrown value into an `AdError`. |

---

For version compatibility and upgrade steps, see the [Migration & Version Matrix](./MIGRATION.md).
