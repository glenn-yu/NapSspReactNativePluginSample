import type { StyleProp, ViewStyle } from 'react-native';

export type LogLevel = 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none';

/**
 * Layout hint for {@link BannerAdProps.size}.
 *
 * The served creative size is decided by the ad unit's server configuration — this value only
 * seeds a minimum height so the row does not collapse before the first fill.
 * https://napmx.github.io/#/android/native/banner
 */
export type BannerSize =
  | 'BANNER_320x50'
  | 'BANNER_320x100'
  | 'BANNER_300x250'
  | 'BANNER_320x480'
  | 'LARGE_BANNER'
  | 'MEDIUM_RECTANGLE'
  | 'SMART_BANNER'
  | (string & {});

/**
 * Per-network keys the app supplies itself.
 *
 * Normally the nap mx media-conf server delivers these. Values set here only fill in keys the
 * server did not send (Server-Precedence).
 */
export interface MediationConfig {
  /** Android reads the Google App ID from `AndroidManifest.xml`; iOS from `GADApplicationIdentifier`. */
  adManager?: {
    googleAppId?: string;
  };
  pangle?: {
    appId: string;
  };
  appLovin?: {
    sdkKey: string;
  };
  /** iOS only — Android initialises Unity Ads lazily from the server configuration. */
  unityAds?: {
    appId: string;
  };
  adFit?: boolean;
  /**
   * Naver Ad Manager. On Android the `PUBLISHER_CD` ships inside the adapter AAR, so no value is
   * needed; on iOS `GFPAdManager.setup` requires the publisher code.
   */
  naverAdManager?: boolean | { publisherCd: string };
  /** Teads. Android needs the Teads Maven repositories, iOS needs the `Teads` subspec. */
  teads?: boolean;
}

/**
 * Tri-state privacy signals. Leaving a field `undefined` means "unspecified": the SDK leaves the
 * vendor default (or the value an IAB TCF CMP wrote) untouched, which is a different state from an
 * explicit `false`.
 *
 * Set these **before** `initialize()` — AppLovin, Unity Ads and Pangle only read consent once, when
 * they start up.
 * https://napmx.github.io/#/android/native/privacy
 */
export interface PrivacyConsent {
  /**
   * COPPA / Google Play Families. `true` marks the app as child-directed.
   * Required for child-directed apps regardless of country.
   */
  childDirected?: boolean;
  /** GDPR: `true` when the user consented to personalised advertising. */
  gdprConsent?: boolean;
  /** CCPA: `true` opts the user out of the sale/sharing of personal data (do-not-sell). */
  ccpaDoNotSell?: boolean;
  /** IAB US Privacy String v1, e.g. `"1YYN"`. Android only. */
  usPrivacy?: string;
  /** GDPR: `true` when the user is below the age of consent. iOS only. */
  underAgeOfConsent?: boolean;
}

export interface NapSspConfig {
  /** Numeric media key from the nap mx partner site. One media key per app. */
  mediaKey: string;
  /** Every numeric ad unit id the app will request. */
  adUnitIds: readonly string[];
  mediations?: MediationConfig;
  logLevel?: LogLevel;
  /** Applied before the SDK initialises. */
  privacy?: PrivacyConsent;
  /**
   * @deprecated Use `privacy.childDirected`. Kept as an alias for 0.4.x compatibility.
   */
  coppa?: boolean;
  /** Global test mode for QA and store review. **Android only** — see {@link NapSspStatus.testModeSupported}. */
  testMode?: boolean;
  /** Test device advertising IDs (GAID). **Android only**. Treat these as sensitive identifiers. */
  testDeviceIds?: readonly string[];
}

export interface NapSspStatus {
  initialized: boolean;
  platform?: 'android' | 'ios';
  logLevel?: string;
  privacy?: PrivacyConsent;
  /** @deprecated Mirrors `privacy.childDirected`. */
  coppa?: boolean;
  testMode?: boolean;
  /** `false` on iOS — the iOS SDK has no global test-mode switch. */
  testModeSupported?: boolean;
  sdkCoordinates?: Record<string, unknown>;
  configuredAdUnitIds?: readonly string[];
  supportedFormats?: readonly string[];
  supportedEvents?: readonly string[];
  trackingAuthorizationStatus?: string;
  runtime?: Record<string, unknown>;
  details?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface AdError {
  /** Stable cross-platform code, e.g. `nap_ssp_no_ads` (Android) / `napssp_load_failed` (iOS). */
  code: string;
  message: string;
  /** The raw SDK error code: an `AdMixer.AX_ERR_*` int on Android, `-1`…`-8` on iOS. */
  nativeCode?: number | string;
  nativeDomain?: string;
  details?: Record<string, unknown>;
}

export interface InterstitialAdOptions {
  /**
   * Close (X) button touch-area ratio, `0.2`–`1.0`.
   * iOS maps it straight onto `AMMInterstitialConfig.closeButtonTouchAreaRatio`;
   * Android converts it to `AdInfo.Builder.setCloseButtonBound()` percent (20–100).
   * Only applies to creatives nap mx renders itself (AdMixer, AdFit).
   */
  closeButtonTouchAreaRatio?: number;
  /** Block the hardware back key while the interstitial is showing. **Android only**, default `true`. */
  disableBackKey?: boolean;
}

export interface RewardedAdOptions {
  /** Custom parameters echoed back on the S2S reward callback. */
  customParams?: Record<string, string>;
  /** Start the video muted. A request, not a guarantee — networks may override it. **Android only**. */
  mute?: boolean;
}

export interface InterstitialVideoAdOptions {
  /** Load timeout in seconds. `0` defers to the server setting; the SDK default is 20. **Android only**. */
  timeout?: number;
  /** Start the video muted. **Android only**. */
  mute?: boolean;
}

export interface InterstitialAdEventMap {
  loaded: void;
  loadFailed: AdError;
  opened: void;
  closed: void;
  clicked: void;
  impression: void;
}

export interface RewardPayload {
  /**
   * Unique id for this reward grant. Matches `transaction_id` on the S2S reward callback, so it is
   * the value to reconcile app-side and server-side grants with.
   * `undefined` on networks that do not report one.
   */
  transactionId?: string;
  /**
   * @deprecated The SDK carries no reward type — units are not comparable across mediation
   * networks. Always `'reward'`. Use your own server-side reward table.
   */
  type: string;
  /**
   * @deprecated The SDK carries no reward amount. Always `1`.
   */
  amount: number;
}

export interface RewardedAdEventMap extends InterstitialAdEventMap {
  rewarded: RewardPayload;
  completed: void;
  /** Not emitted on iOS — `AMMRewardVideoDelegate` has no skip callback. */
  skipped: void;
}

export interface InterstitialVideoAdEventMap extends InterstitialAdEventMap {
  completed: void;
  /** Not emitted on iOS — `AMMVideoInterstitialDelegate` has no skip callback. */
  skipped: void;
}

/** Imperative handle exposed by the inline ad views. */
export interface AdViewHandle {
  /** Destroys the current ad and requests a fresh one. */
  reload(): void;
}

interface InlineAdEvents {
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
}

export interface NativeAdProps extends InlineAdEvents {
  adUnitId: string;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}

export interface VideoAdProps extends InlineAdEvents {
  adUnitId: string;
  /** Retry the waterfall once when the first pass returns no fill. */
  isRetry?: boolean;
  onAdCompleted?: () => void;
  onAdSkipped?: () => void;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}
