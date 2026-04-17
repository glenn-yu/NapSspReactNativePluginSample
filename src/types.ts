export type LogLevel = 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none';

export type BannerSize =
  | 'BANNER_320x50'
  | 'BANNER_320x100'
  | 'BANNER_300x250'
  | 'LARGE_BANNER'
  | 'MEDIUM_RECTANGLE'
  | 'SMART_BANNER';

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

export interface AdError {
  code: string;
  message: string;
  nativeCode?: number | string;
  nativeDomain?: string;
  details?: Record<string, unknown>;
}

export interface RewardItem {
  type: string;
  amount: number;
  currency?: string;
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
  rewarded: RewardItem;
}
