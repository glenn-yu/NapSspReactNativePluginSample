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
  /**
   * @deprecated 공식 nap mx 가이드의 지원 네트워크 목록에서 제외되었습니다. 설정해도 무시됩니다.
   * Deprecated: no longer listed as a supported network in the official nap mx guide; ignored.
   */
  mobwith?: boolean;
  /** Naver Ad Manager 어댑터 활성화 (v2.0.0+). PUBLISHER_CD는 SDK가 제공하므로 추가 설정 불필요 / Enable Naver Ad Manager adapter (v2.0.0+); PUBLISHER_CD is provided by the SDK. */
  naverAdManager?: boolean;
  /**
   * Teads 어댑터 활성화. Android 는 Teads Maven 저장소 추가 필요, iOS 는 `Teads` subspec 필요.
   * Enable the Teads adapter; Android needs the Teads Maven repositories, iOS needs the `Teads` subspec.
   */
  teads?: boolean;
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
  /**
   * 전면 광고 닫기(X) 버튼 터치 영역 비율 (0.2~1.0). iOS 전용 / iOS only.
   * Android 는 AdInfo.setCloseButtonBound(20~100%) 로 서버 설정을 따릅니다.
   */
  closeButtonTouchAreaRatio?: number;
  // NOTE: v2(AOS 2.0.0 / iOS 2.3.7)부터 전면 광고는 Basic 전용입니다.
  // popup/countDown 타입 및 관련 옵션(type/countDownTime/buttonLeftText 등)은
  // 네이티브 SDK 에서 제거되어 더 이상 지원하지 않습니다.
  // v2 (AOS 2.0.0 / iOS 2.3.7) makes interstitials Basic-only; popup/countDown
  // types and their options were removed from the native SDKs.
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

