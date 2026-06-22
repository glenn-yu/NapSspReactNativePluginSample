# API 레퍼런스 (API Reference)

> KO: `react-native-nap-ssp` 플러그인이 내보내는 모든 컴포넌트, 클래스, 타입에 대한 명세입니다. (v0.2.0 — Android v2.0.0 / iOS 2.3.7 기준)
> EN: Specification of every component, class, and type exported by the `react-native-nap-ssp` plugin. (v0.2.0 — Android v2.0.0 / iOS 2.3.7)

> KO: ℹ️ 네이티브 SDK v2의 클래스/리스너 변경(클래스명 변경, AdListener의 abstract 전환, 네이티브 View ID prefix 등)은 이 플러그인이 내부에서 흡수하므로, **JS/TS 사용자는 대부분 코드 변경 없이** 업그레이드됩니다.
> EN: ℹ️ Native v2 changes (renamed classes, AdListener becoming abstract, native view-ID prefixes, etc.) are absorbed internally by this plugin, so **JS/TS users upgrade with little or no code change.**

---

## 1. NapSspAd (Core)

> KO: SDK 초기화 및 전역 설정을 관리하는 정적 클래스입니다.
> EN: Static class that manages SDK initialization and global settings.

### 메서드 (Methods)

#### `initialize(config: NapSspConfig): Promise<void>`
> KO: SDK를 초기화합니다. 앱 시작 시 한 번만 호출합니다. v2.0.0부터 어댑터는 Gradle/Pod 의존성에 따라 자동 등록되므로 별도 등록 호출이 필요 없습니다.
> EN: Initializes the SDK. Call once at app startup. As of v2.0.0, adapters are auto-registered from your Gradle/Pod dependencies — no separate registration call is needed.

#### `setLogLevel(level: LogLevel): void`
> KO: 로그 레벨을 변경합니다.
> EN: Changes the log level.

#### `setCoppa(enabled: boolean): void`
> KO: COPPA(아동 대상) 처리 여부를 설정합니다.
> EN: Sets COPPA (child-directed) handling.

#### `isInitialized(): boolean`
> KO: 초기화 완료 여부를 반환합니다.
> EN: Returns whether initialization has completed.

#### `getConfig(): NapSspConfig | undefined`
> KO: 초기화에 사용된 설정의 복사본을 반환합니다.
> EN: Returns a copy of the config used for initialization.

#### `getStatus(): Promise<NapSspStatus>`
> KO: 현재 SDK 상태와 로드된 광고 정보를 가져옵니다.
> EN: Returns the current SDK status and loaded-ad information.

#### `requestTrackingAuthorization(): Promise<string>`
> KO: (iOS 전용) ATT 추적 권한을 요청하고 상태 문자열을 반환합니다.
> EN: (iOS only) Requests ATT tracking authorization and returns the status string.

---

## 2. BannerAd (Component)

> KO: 배너 광고를 표시하는 뷰 컴포넌트입니다.
> EN: View component that renders a banner ad.

### Props

- `adUnitId: string` — 광고 단위 ID / ad unit ID.
- `size?: BannerSize` — 배너 크기(기본 `BANNER_320x50`). `'BANNER_WxH'` 동적 문자열 허용 / banner size (default `BANNER_320x50`); accepts dynamic `'BANNER_WxH'` strings.
- `autoLoad?: boolean` — (Android 전용) 마운트 시 자동 로드. 기본 `true` / (Android only) auto-load on mount; default `true`.
- `onAdLoaded?: () => void`
- `onAdFailedToLoad?: (error: AdError) => void`
- `onAdClicked?: () => void`
- `onAdOpened?: () => void`
- `onAdClosed?: () => void`
- `onAdImpression?: () => void`
- `style?: ViewStyle`
- `testID?: string`

---

## 3. NativeAd / VideoAd (Component)

> KO: 네이티브 광고와 인라인 비디오 광고를 위한 뷰 컴포넌트입니다.
> EN: View components for native ads and inline video ads.

### 공통 Props (Common)

- `adUnitId: string`
- `onAdLoaded?: () => void`
- `onAdFailedToLoad?: (error: AdError) => void`
- `onAdClicked?: () => void`
- `onAdOpened?: () => void`
- `onAdClosed?: () => void`
- `onAdImpression?: () => void`
- `style?: ViewStyle` — `width`/`height` 명시 필요 / `width`/`height` must be set.
- `testID?: string`

### VideoAd 전용 (VideoAd only)

- `isRetry?: boolean` — (Android 전용) 기본 `false` / (Android only) default `false`.
- `onAdCompleted?: () => void` — 동영상 시청 완료 / video completed.
- `onAdSkipped?: () => void` — 동영상 스킵 / video skipped.

---

## 4. InterstitialAd / InterstitialVideoAd / RewardedAd (Class)

> KO: 풀스크린 광고를 위한 클래스입니다. 생성자는 `(adUnitId, options?)`이며, `load()` → `show()` 순서로 사용합니다.
> EN: Classes for full-screen ads. Constructor is `(adUnitId, options?)`; use `load()` then `show()`.

### 공통 메서드 (Common methods)

- `constructor(adUnitId: string, options?)` — 빈 `adUnitId`는 예외 발생 / throws on empty `adUnitId`.
- `load(): Promise<void>` — 광고를 불러옵니다 / loads the ad.
- `show(): Promise<void>` — 불러온 광고를 표시합니다(미로드 시 예외) / shows a loaded ad (throws if not loaded).
- `start(): Promise<void>` — 네이티브가 지원하면 load+show를 한 번에, 아니면 `load()`+`show()` 폴백 / single-call load+show where natively supported, otherwise falls back to `load()`+`show()`.
- `isLoaded(): boolean`
- `destroy(): void` — 리스너와 네이티브 리소스를 정리합니다 / releases listeners and native resources.
- `addAdEventListener(event, handler): () => void` — 구독 해제 함수를 반환합니다 / returns an unsubscribe function.

### InterstitialAd

> KO: 이벤트: `loaded`, `loadFailed`, `opened`, `closed`, `clicked`, `impression`.
> EN: Events: `loaded`, `loadFailed`, `opened`, `closed`, `clicked`, `impression`.

> KO: 옵션 `InterstitialAdOptions` — `closeButtonTouchAreaRatio?`(iOS 전용, 0.2~1.0)만 지원합니다. **v2부터 전면 광고는 Basic 전용**이며 popup/countDown 타입과 관련 옵션(type, countDownTime, buttonLeftText, buttonRightText 등)은 제거되었습니다.
> EN: Options `InterstitialAdOptions` — only `closeButtonTouchAreaRatio?` (iOS only, 0.2–1.0) is supported. **Interstitials are Basic-only as of v2**; popup/countDown types and their options (type, countDownTime, buttonLeftText, buttonRightText, etc.) were removed.

### InterstitialVideoAd

> KO: 이벤트: 위 공통 이벤트 + `completed`, `skipped`. 옵션 `InterstitialVideoAdOptions` — `timeout?`(Android, 0이면 서버 설정, 기본 20), `maxRetryCountInSlot?`(Android, -1 무한 / 0 없음 / n회).
> EN: Events: the common set + `completed`, `skipped`. Options `InterstitialVideoAdOptions` — `timeout?` (Android; 0 = server-defined, default 20), `maxRetryCountInSlot?` (Android; -1 infinite / 0 none / n times).

### RewardedAd

> KO: 이벤트: 위 공통 이벤트 + `rewarded`(payload `{ type, amount }`), `completed`, `skipped`. 옵션 `RewardedAdOptions` — `customParams?`(매체 서버로 전달할 파라미터), `mute?`(Android 전용). 보상은 S2S 콜백 사용을 권장합니다.
> EN: Events: the common set + `rewarded` (payload `{ type, amount }`), `completed`, `skipped`. Options `RewardedAdOptions` — `customParams?` (passed to the publisher server), `mute?` (Android only). Prefer S2S callbacks for rewards.

> KO: `addAdEventListener('rewarded', cb)`를 사용하세요. 레거시 별칭 `'onRewarded'`도 동일하게 동작합니다.
> EN: Use `addAdEventListener('rewarded', cb)`. The legacy alias `'onRewarded'` also works.

---

## 5. 타입 (Types)

### MediationConfig

> KO: 초기화 시 어댑터를 활성화/설정합니다.
> EN: Enables/configures adapters at initialization.

```ts
interface MediationConfig {
  adManager?: { googleAppId?: string };
  pangle?: { appId: string };
  appLovin?: { sdkKey: string };
  unityAds?: { appId: string };
  adFit?: boolean;
  mobwith?: boolean;
  naverAdManager?: boolean; // v2.0.0+
  teads?: boolean;          // v2.0.0+ (Android only)
}
```

### NapSspConfig

```ts
interface NapSspConfig {
  mediaKey: string;
  adUnitIds: readonly string[];
  mediations?: MediationConfig;
  logLevel?: LogLevel;
  coppa?: boolean;
}
```

### LogLevel

```ts
type LogLevel = 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none';
```

### AdError

```ts
interface AdError {
  code: string;
  message: string;
  nativeCode?: number | string;
  nativeDomain?: string;
  details?: Record<string, unknown>;
}
```

### RewardPayload

```ts
interface RewardPayload {
  type: string;
  amount: number;
}
```

### NapSspStatus

> KO: `getStatus()`가 반환하는 상태 객체입니다(주요 필드).
> EN: Status object returned by `getStatus()` (key fields).

```ts
interface NapSspStatus {
  initialized: boolean;
  placeholderMode?: boolean;
  logLevel?: string;
  coppa?: boolean;
  sdkCoordinates?: Record<string, string>;
  configuredAdUnitIds?: readonly string[];
  loadedInterstitialAdUnitIds?: readonly string[];
  loadedRewardedAdUnitIds?: readonly string[];
  trackingAuthorizationStatus?: string;
  // ... runtime / details
}
```

---

> KO: 📚 연동 절차는 [Getting Started](./GETTING_STARTED.md)를 참고하세요. 문의: nap_mx@nasmedia.co.kr · 공식 가이드: https://napmx.github.io/
> EN: 📚 See [Getting Started](./GETTING_STARTED.md) for integration steps. Contact: nap_mx@nasmedia.co.kr · Official guide: https://napmx.github.io/
