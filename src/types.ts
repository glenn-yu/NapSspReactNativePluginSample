export type LogLevel = 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none';

export type BannerSize =
  | 'BANNER_320x50'
  | 'BANNER_320x100'
  | 'BANNER_300x250'
  | 'LARGE_BANNER'
  | 'MEDIUM_RECTANGLE'
  | 'SMART_BANNER';

export interface NativeAdProps {
  adUnitId: string;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  style?: import('react-native').StyleProp<import('react-native').ViewStyle>;
  testID?: string;
}

export interface MediationConfig {
  adManager?: {
    googleAppId?: string;
  };
  pangle?: {
    appId: string;
  };
  appLovin?: {
    sdkKey: string;
  };
  unityAds?: {
    appId: string;
  };
  adFit?: boolean;
  mobwith?: boolean;
}

export interface NapSspConfig {
  mediaKey: string;
  adUnitIds: readonly string[];
  mediations?: MediationConfig;
  logLevel?: LogLevel;
  coppa?: boolean;
}

export interface NapSspStatus {
  initialized: boolean;
  placeholderMode?: boolean;
  vendorMode?: boolean;
  mediationFlags?: Record<string, boolean>;
  loadedAds?: {
    banners?: readonly string[];
    interstitials?: readonly string[];
    rewarded?: readonly string[];
  };
  attStatus?: string;
  details?: Record<string, unknown>;
}

export interface AdError {
  code: string;
  message: string;
  nativeCode?: number | string;
  nativeDomain?: string;
  details?: Record<string, unknown>;
}

export interface InterstitialAdOptions {
  type?: 'default' | 'popup';
  countDownTime?: number;
  buttonLeftText?: string;
  buttonRightText?: string;
}

export interface RewardedAdOptions {
  customParams?: Record<string, string>;
  mute?: boolean; // Android only
}

export interface InterstitialAdEventMap {
  loaded: void;
  loadFailed: AdError;
  opened: void;
  closed: void;
  clicked: void;
  impression: void;
}

export interface RewardedAdEventMap extends InterstitialAdEventMap {
  rewarded: void;
}

export interface VideoAdProps {
  adUnitId: string;
  isRetry?: boolean; // Android only, false by default
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdCompleted?: () => void;
  onAdSkipped?: () => void;
  style?: import('react-native').StyleProp<import('react-native').ViewStyle>;
  testID?: string;
}

export interface InterstitialVideoAdOptions {
  timeout?: number; // Android: 0 means server defined, default 20
  maxRetryCountInSlot?: number; // Android: -1 infinite, 0 none, n times
}

export interface InterstitialVideoAdEventMap extends InterstitialAdEventMap {
  completed: void;
  skipped: void;
}

