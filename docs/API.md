# API reference

Everything the package exports. Types are shipped with the package, so your editor has the same
information.

```ts
import {
  NapSspAd,
  BannerAd,
  NativeAd,
  VideoAd,
  InterstitialAd,
  InterstitialVideoAd,
  RewardedAd,
  isNativeModuleAvailable,
  isNativeViewAvailable,
  NativeModuleNames,
  normalizeAdError,
} from 'react-native-nap-ssp';
```

---

## NapSspAd

Static entry point for the SDK.

### `initialize(config): Promise<NapSspStatus>`

Call once, before any ad request. Rejects when the config is invalid or the native module is not
linked.

```ts
interface NapSspConfig {
  mediaKey: string;                 // numeric, one per app
  adUnitIds: readonly string[];     // numeric, every unit the app will request
  mediations?: MediationConfig;
  logLevel?: LogLevel;              // 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none'
  privacy?: PrivacyConsent;
  testMode?: boolean;               // Android only
  testDeviceIds?: readonly string[];// Android only
  /** @deprecated use privacy.childDirected */
  coppa?: boolean;
}
```

`mediaKey` and `adUnitIds` must be numeric strings — the plugin validates this in JS and rejects
with the offending values named, because the native SDKs parse them as integers.

Privacy and test settings passed here are applied **before** the SDK starts, which is what the
network adapters require.

### `setPrivacyConsent(consent): Promise<void>`

```ts
interface PrivacyConsent {
  childDirected?: boolean;    // COPPA / Google Play Families
  gdprConsent?: boolean;      // true = consented to personalised ads
  ccpaDoNotSell?: boolean;    // true = opted out of sale/sharing
  usPrivacy?: string;         // IAB US Privacy String v1, e.g. '1YYN'. Android only
  underAgeOfConsent?: boolean;// GDPR. iOS only
}
```

Every field is tri-state: omitting it means "unspecified", which leaves the vendor default (or a
value written by an IAB TCF CMP) untouched. That is a different state from an explicit `false`.

Prefer `initialize({ privacy })` — AppLovin, Unity Ads and Pangle read consent only at start-up, so
a later change may not take effect until the next app launch.

> **Unity Ads:** once you set `childDirected: true`, switch back with an explicit `false`. Unity's
> metadata API has no clear operation, so "unspecified" cannot undo it.
>
> **AppLovin:** with `childDirected: true` the SDK skips AppLovin in the waterfall entirely — that
> network earns nothing on child-directed inventory, by AppLovin's own policy.

Per-network propagation is documented in the
[official privacy guide](https://napmx.github.io/#/android/native/privacy).

### `setCoppa(enabled): Promise<void>`

**Deprecated** alias for `setPrivacyConsent({ childDirected: enabled })`.

### `setTestMode(enabled): Promise<boolean>` · `setTestDeviceIds(ids): Promise<boolean>`

Android only. Both resolve `false` on iOS, where the SDK has no global test switch — register test
devices in each network's own dashboard instead.

Test device IDs are Google Advertising IDs (GAID). Treat them as sensitive identifiers: keep them
out of logs, tickets and support mail.

### `setLogLevel(level): Promise<void>`

`'verbose'` during development, `'error'` in production.

### `getStatus(): Promise<NapSspStatus>`

```ts
interface NapSspStatus {
  initialized: boolean;
  platform?: 'android' | 'ios';
  logLevel?: string;
  privacy?: PrivacyConsent;
  testMode?: boolean;
  testModeSupported?: boolean;   // false on iOS
  sdkCoordinates?: Record<string, unknown>;
  configuredAdUnitIds?: readonly string[];
  trackingAuthorizationStatus?: string;
  runtime?: Record<string, unknown>;
}
```

### `requestTrackingAuthorization(): Promise<string>`

Presents the iOS ATT prompt and resolves `'authorized' | 'denied' | 'restricted' | 'notDetermined'`.
Resolves `'unavailable'` on Android and on iOS below 14.5. Call it before the first ad request.

### `isInitialized(): boolean` · `getConfig(): NapSspConfig | undefined`

Synchronous JS-side state. `getConfig()` returns a copy.

---

## Inline ad views

`BannerAd`, `NativeAd` and `VideoAd` load when they are attached to the window and release the
native ad when they unmount. All three accept a `ref` of type `AdViewHandle`:

```ts
interface AdViewHandle {
  reload(): void; // destroys the current ad and requests a fresh one
}
```

### `BannerAd`

```ts
interface BannerAdProps {
  adUnitId: string;
  size?: BannerSize;      // layout hint only — see below
  autoLoad?: boolean;     // default true
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}
```

> **`size` does not request a size.** The served creative size comes from the ad unit's server
> configuration. The prop only seeds the view's default width/height so the row does not collapse
> before the first fill; anything in `style` wins. Known names (`BANNER_320x50`, `LARGE_BANNER`,
> `MEDIUM_RECTANGLE`, …) and any `BANNER_WxH` string are understood.

### `NativeAd`

`{ adUnitId, style, testID }` plus the six inline callbacks. The SDK renders the creative into the
plugin's layout — see [Setup](./SETUP.md#3-8-customising-the-native-ad-layout) to restyle it.

### `VideoAd`

`NativeAd`'s props plus:

* `isRetry?: boolean` — retry the waterfall once on no-fill
* `onAdCompleted?: () => void`
* `onAdSkipped?: () => void`

---

## Full-screen ads

`InterstitialAd`, `InterstitialVideoAd` and `RewardedAd` share one interface.

```ts
const ad = new InterstitialAd(adUnitId, options?);
```

| Method | Description |
| :--- | :--- |
| `load(): Promise<void>` | Requests an ad. Resolves on fill, rejects with an `AdError` when the waterfall is exhausted. |
| `show(): Promise<void>` | Presents a loaded ad. Rejects when nothing is ready. |
| `start(): Promise<void>` | `load()` then present as soon as the ad arrives. |
| `isLoaded(): boolean` | Last known readiness from the native events. Synchronous, safe during render. |
| `isReady(): Promise<boolean>` | Asks the SDK directly. |
| `isLoading(): Promise<boolean>` | Whether a load is still in flight. |
| `cancelLoad(): Promise<void>` | Cancels an in-flight load. A showing ad is left alone. |
| `addAdEventListener(event, handler): () => void` | Subscribe. Returns an unsubscribe function. |
| `destroy(): void` | Releases the native ad and every listener. The instance cannot be reused. |

Always `destroy()` on unmount — that is what releases the native ad and the server config listener.

> The native side keeps **one ad per ad unit ID**. Two JS instances built for the same ad unit share
> it: both receive the events, and `destroy()` on either releases it. Use one instance per ad unit.

```ts
useEffect(() => {
  const ad = new RewardedAd('103722');
  const off = ad.addAdEventListener('rewarded', grant);
  return () => {
    off();
    ad.destroy();
  };
}, []);
```

### Options

```ts
interface InterstitialAdOptions {
  closeButtonTouchAreaRatio?: number; // 0.2–1.0. Android converts it to setCloseButtonBound() %
  disableBackKey?: boolean;           // Android only, default true
}

interface RewardedAdOptions {
  customParams?: Record<string, string>; // echoed on the S2S reward callback
  mute?: boolean;                        // Android only. A request, not a guarantee
}

interface InterstitialVideoAdOptions {
  timeout?: number; // seconds. 0 = server-defined, SDK default 20. Android only
  mute?: boolean;   // Android only
}
```

`closeButtonTouchAreaRatio` only affects creatives nap mx renders itself (AdMixer, AdFit) —
full-screen network creatives draw their own close button.

---

## Events

Listeners receive the short event name, not the native one.

| Event | Payload | Banner | Native | Inline video | Interstitial | Interstitial video | Rewarded |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `loaded` | – | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `loadFailed` | `AdError` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `impression` | – | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `clicked` | – | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `opened` | – | – | – | – | ✅ | ✅ | ✅ |
| `closed` | – | – | – | – | ✅ | ✅ | ✅ |
| `completed` | – | – | – | ✅ | – | ✅ | ✅ |
| `skipped` | – | – | – | ✅ | – | ✅ Android only | ✅ Android only |
| `rewarded` | `RewardPayload` | – | – | – | – | – | ✅ |

Inline views deliver these through props (`onAdLoaded`, `onAdFailedToLoad`, …) instead.

> `skipped` never fires on iOS for the two full-screen video formats —
> `AMMRewardVideoDelegate` and `AMMVideoInterstitialDelegate` have no skip callback.

A **show-phase** failure (the ad loaded but could not be presented) also arrives as `loadFailed`,
but it does not clear `isLoaded()` — the loaded ad is still there. Android reports it with
`nap_ssp_show_failed` / `AX_ERR_ADAPTER`, iOS with `napssp_show_failed` (`-5`).

### `RewardPayload`

```ts
interface RewardPayload {
  transactionId?: string;
  /** @deprecated always 'reward' */
  type: string;
  /** @deprecated always 1 */
  amount: number;
}
```

`transactionId` is the only authoritative field: it matches `transaction_id` on the S2S reward
callback, so use it to reconcile app-side and server-side grants. The SDK deliberately carries no
amount or currency — units are not comparable across mediation networks, so keep your reward table
on your own server.

---

## AdError

```ts
interface AdError {
  code: string;                    // stable cross-platform code
  message: string;
  nativeCode?: number | string;    // raw SDK code
  nativeDomain?: string;           // iOS: the ad view class that failed
  details?: Record<string, unknown>;
}
```

### Android codes

`nativeCode` is an `AdMixer.AX_ERR_*` integer.

| `code` | Native | Meaning |
| :--- | :--- | :--- |
| `nap_ssp_no_ads` | `AX_ERR_NO_ADS` | Waterfall exhausted. **Every no-fill reports this** — branch on it first. |
| `nap_ssp_config_failed` | `AX_ERR_CONFIG_FAIL` | Server config missing or the ad unit is not provisioned. |
| `nap_ssp_sdk_not_initialized` | `AX_ERR_INIT` | Ad requested before `initialize()` resolved. |
| `nap_ssp_invalid_ad_unit` | `AX_ERR_ADUNIT` | Missing ad unit ID, or a non-Activity context. |
| `nap_ssp_no_adapter` | `AX_ERR_NO_ADAPTER` | The selected adapter cannot serve this format. |
| `nap_ssp_adapter_error` | `AX_ERR_ADAPTER` | Adapter-internal failure — read `message`. |
| `nap_ssp_timeout` | `AX_ERR_TIMEOUT` | Load or show timed out. |
| `nap_ssp_invalid_request` | `AX_ERR_INVALID_REQUEST` | Malformed request parameter. |
| `nap_ssp_network_error` | `AX_ERR_NETWORK` | Network failure. |

Mediation absorbs per-network failures, so a single `nap_ssp_no_ads` usually stands in for several
adapter errors. The network's own code and message are in `message`, not `nativeCode`.

Do not branch on `AX_ERR_NO_FILL` — the SDK never sends it.

### iOS codes

`nativeCode` is the `NSError.code`; `nativeDomain` is the ad view class name.

| `code` | Native | Meaning |
| :--- | :--- | :--- |
| `napssp_load_failed` | `-1` | Ad (or house ad) load failed. |
| `napssp_invalid_ad_unit` | `-2` | Ad unit ID or required info missing. |
| `napssp_adapter_not_found` | `-3` | The assigned network's adapter is not in the app. |
| `napssp_invalid_network` | `-4` | No requestable network — check the ad unit configuration. |
| `napssp_show_failed` | `-5` | Failed at show time. |
| `napssp_invalid_ad_unit_size` | `-6` | Invalid banner size. |
| `napssp_load_cancelled` | `-7` | `cancelLoad()` / `stop()` during the load. |
| `napssp_timeout` | `-8` | Load timed out. |

When a network SDK supplied the underlying error, it is in `details.underlyingCode` /
`details.underlyingDomain` / `details.underlyingMessage`.

### Plugin-level codes

| `code` | Meaning |
| :--- | :--- |
| `nap_ssp_not_initialized` / `napssp_not_initialized` | Ad requested before `initialize()` resolved. |
| `nap_ssp_activity_required` | Android: no foreground Activity for a full-screen ad. |
| `nap_ssp_ad_not_ready` | `show()` with nothing loaded. |
| `nap_ssp_load_cancelled` | Superseded by another load, or cancelled. |
| `nap_ssp_destroyed` | `destroy()` ran before the load finished. |
| `nap_ssp_view_not_linked` | The native view is not registered — rebuild the app. |
| `napssp_sdk_not_linked` | iOS: `AdMixerMediation` is missing — run `pod install`. |
| `napssp_no_view_controller` | iOS: no root view controller available. |

### `normalizeAdError(error, fallbackCode?): AdError`

Coerces anything thrown by the bridge into an `AdError`. Applied automatically to the promises and
events above; exported for your own error paths.

---

## Helpers

| Export | Description |
| :--- | :--- |
| `isNativeModuleAvailable(name \| names)` | Whether a native module is registered. |
| `isNativeViewAvailable(name \| names)` | Whether a native view manager is registered. |
| `NativeModuleNames` | The module/view names this plugin looks for. |
