export type LogLevel = 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none';

// 자주 쓰는 사이즈는 자동완성 제공, 그 외 'BANNER_WxH' 형식 문자열도 허용
export type BannerSize =
  | 'BANNER_320x50'
  | 'BANNER_320x100'
  | 'BANNER_300x250'
  | 'BANNER_320x480'
  | 'LARGE_BANNER'
  | 'MEDIUM_RECTANGLE'
  | 'SMART_BANNER'
  | (string & {});

export interface NativeAdProps {
  adUnitId: string;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
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
  vendorSdkEnabled?: boolean;
  logLevel?: string;
  coppa?: boolean;
  sdkCoordinates?: Record<string, string>;
  configuredAdUnitIds?: readonly string[];
  loadedInterstitialAdUnitIds?: readonly string[];
  loadedRewardedAdUnitIds?: readonly string[];
  trackingAuthorizationStatus?: string;
  runtime?: Record<string, unknown>;
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
  type?: 'default' | 'popup' | 'countdown';
  countDownTime?: number;
  buttonLeftText?: string;
  buttonRightText?: string;
  closeButtonTouchAreaRatio?: number; // iOS only: 0.2~1.0 (basic/countdown only)
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

export interface RewardPayload {
  type: string;
  amount: number;
}

export interface RewardedAdEventMap extends InterstitialAdEventMap {
  rewarded: RewardPayload;
  completed: void;
  skipped: void;
}

export interface VideoAdProps {
  adUnitId: string;
  isRetry?: boolean; // Android only, false by default
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
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

