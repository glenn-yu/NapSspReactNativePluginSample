# API Reference

`react-native-nap-ssp` 플러그인의 모든 컴포넌트와 클래스에 대한 상세 명세입니다.

---

## 1. NapSspAd (Core Module)

SDK의 초기화 및 전역 설정을 관리합니다.

### Methods

#### `initialize(config: NapSspConfig): Promise<NapSspStatus>`
SDK를 초기화합니다. 앱 시작 시 한 번만 호출하면 됩니다.
- **config**:
  - `mediaKey`: (string) 나스미디어에서 발급받은 미디어 키.
  - `adUnitIds`: (string[]) 사용할 모든 광고 단위 ID 목록.
  - `logLevel`: ('verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none') 로그 레벨.
  - `mediations`: (MediationConfig) 각 네트워크별 앱 ID/키 설정.
- **Returns**: 초기화 성공 여부 및 상태를 포함한 Promise.

#### `getStatus(): Promise<NapSspStatus>`
현재 SDK의 초기화 상태 및 로드된 광고 정보를 가져옵니다.

#### `requestTrackingAuthorization(): Promise<string>`
(iOS 전용) ATT 권한 요청을 수행합니다.

---

## 2. BannerAd (Component)

배너 광고를 표시하기 위한 뷰 컴포넌트입니다.

### Props
- `adUnitId`: (string) 광고 단위 ID.
- `size`: (BannerSize) 배너 크기.
  - `BANNER_320x50`, `BANNER_320x100`, `BANNER_300x250`, `SMART_BANNER` 등.
- `onAdLoaded`: () => void. 광고 로드 성공 시 호출.
- `onAdFailedToLoad`: (error: AdError) => void. 광고 로드 실패 시 호출.
- `onAdClicked`: () => void. 광고 클릭 시 호출.
- `onAdImpression`: () => void. 광고 노출(Impression) 발생 시 호출.
- `style`: ViewStyle. 뷰의 크기 및 스타일 설정.

---

## 3. NativeAd / VideoAd (Component)

네이티브 광고 및 인라인 비디오 광고를 위한 뷰 컴포넌트입니다.

### Props (공통)
- `adUnitId`: (string) 광고 단위 ID.
- `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onAdImpression`: 배너와 동일.
- `style`: ViewStyle.

### VideoAd 전용 Props
- `onAdCompleted`: () => void. 동영상 시청 완료 시 호출.
- `onAdSkipped`: () => void. 동영상 스킵 시 호출.
- `isRetry`: (boolean, Android 전용) 광고 실패 시 재시도 여부.

---

## 4. InterstitialAd / RewardedAd / InterstitialVideoAd (Class)

전면 및 리워드 광고를 위한 클래스 인터페이스입니다.

### Common Methods
- `load(): Promise<void>`: 광고를 서버로부터 불러옵니다.
- `show(): Promise<void>`: 불러온 광고를 화면에 표시합니다.
- `addAdEventListener(event, handler)`: 이벤트를 리스닝합니다.
  - Events: `loaded`, `loadFailed`, `opened`, `closed`, `clicked`, `impression`.

### RewardedAd 전용
- Event: `onRewarded`. 보상 지급 시점에 발생. (S2S 콜백 권장)
- Option: `customParams`. 보상 데이터와 함께 매체사 서버로 전달할 파라미터.

### InterstitialVideoAd 전용
- Events: `completed`, `skipped`.

---

## 5. Types & Enums

### AdError
- `code`: 에러 구분 코드.
- `message`: 상세 에러 메시지.
- `nativeCode`: (optional) 네이티브 SDK에서 전달된 원본 에러 코드.
