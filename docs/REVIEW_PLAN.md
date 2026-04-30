# Nap SSP React Native Plugin Review & Verification Plan

이 문서는 개발된 `react-native-nap-ssp` 플러그인과 테스트 앱이 초기 설계 및 최신 네이티브 SDK 가이드(Android v1.0.23 / iOS v2.3.2)를 준수하고 있는지 전수 조사하고, 실제 광고 노출까지 검증하기 위한 가이드라인입니다.

---

## 1. 검증 개요

- **검증 대상**: `react-native-nap-ssp` 패키지 소스, `example` 앱, `integration-test-app`, 네이티브 브릿지 코드
- **기준 문서**:
  - `nap-ssp-android-sdk-native.md` (Android 최신 가이드)
  - `nap-ssp-ios-sdk-native.md` (iOS 최신 가이드)
  - `nap_ssp_react_native_plan.docx` (초기 설계서)

---

## 2. 전수 조사 핵심 체크리스트

### 2.1 SDK 초기화 및 공통 설정 (Core)
- [ ] **Initialization**: mediaKey와 adUnitIds 목록이 네이티브 SDK로 정상 전달되는가?
- [ ] **Log Level**: `setDebugEnabled` (iOS) 및 `AdMixerLog.setLogLevel` (Android)이 JS 설정에 따라 연동되는가?
- [ ] **Mediation Adapters**: `registerAdapter` (Android) 로직이 초기화 시점에 포함되어 있는가? (Google, AdFit, Pangle, AppLovin, Unity)
- [ ] **Pangle/AppLovin Key**: 초기화 시 appId 및 sdkKey가 각 네트워크 SDK 설정에 반영되는가?

### 2.2 광고 포맷별 구현 (Ad Formats)
#### 배너 광고 (BannerAd)
- [ ] **Lifecycle**: `onResume`, `onPause`, `onDestroy` (Android) 및 `stop` (iOS) 호출이 RN 생명주기에 매핑되었는가?
- [ ] **Events**: `onSuccess`, `onFail`, `onTap` 이벤트가 JS 콜백으로 전달되는가?
- [ ] **Size Mapping**: 설계서에 정의된 320x50, 300x250 등의 사이즈 상수가 정의되었는가?

#### 전면 광고 (InterstitialAd)
- [ ] **Config**: basic, popup, countDown 3종 옵션이 네이티브로 전달되는가?
- [ ] **Method**: `load()`와 `show()`가 비동기(Promise)로 분리되어 있는가?
- [ ] **Resource Recovery**: 광고 닫힘 시 `stop()` 또는 nil 처리가 수행되는가?

#### 네이티브 광고 (NativeAd)
- [ ] **Asset Mapping**: icon, headline, advertiser, description, media, cta 6종 에셋이 렌더링되는가?
- [ ] **iOS v2.3.2 대응**: `loadAD()` 시 기존 뷰를 제거하는 `removeView` 로직이 반영되었는가?
- [ ] **Layout**: Android에서 `RelativeLayout` 기반의 컨테이너 구조를 유지하는가?

#### 리워드 동영상 (RewardedAd)
- [ ] **Reward Callback**: Android `EARNEDREWARD`와 iOS `onRewardVideoEarned`가 동일한 JS 이벤트(`onRewarded`)로 통합되었는가?
- [ ] **Custom Params**: `customParam` (iOS) 및 `setCustomParams` (Android) 전달 기능이 포함되었는가?

### 2.3 네이티브 플랫폼 특이사항 (Platform Specifics)
- [ ] **Android Gradle**: `build.gradle`에 필수 의존성 및 미디에이션 repositories가 포함되었는가?
- [ ] **Android Proguard**: `consumer-rules.pro`에 공식 가이드의 -keep 규칙 6종이 포함되었는가?
- [ ] **iOS Info.plist**: `GADApplicationIdentifier` 및 `SKAdNetwork ID` 가이드라인이 명시되었는가?
- [ ] **ATT Handling**: iOS 13.0+ 대응을 위한 `requestTrackingAuthorization` 유틸리티가 제공되는가?

---

## 3. 공식 가이드 대비 누락 사항 분석 (Gap Analysis)

| 항목 | 공식 가이드 내용 (Android/iOS) | RN 플러그인 구현 여부 | 비고 |
| :--- | :--- | :---: | :--- |
| **에러 코드** | iOS 7종 (0~6) 에러 매핑 | [ ] | JS 에러 객체에 포함 필요 |
| **비즈보드** | Kakao AdFit 비즈보드 단독 지면 정책 | [ ] | 미디에이션 불가 설정 확인 |
| **음소거** | 리워드 광고 `setMute(true)` 옵션 | [ ] | |
| **타임아웃** | 전면 광고 `interstitialTimeout` (Android) | [ ] | |
| **클릭 영역** | iOS 전면 광고 `closeButtonTouchAreaRatio` | [ ] | 0.2~1.0 범위 설정 |

---

## 4. 실검증 작업 단계 (Phases)

### Phase 1. 공식 가이드 대비 기능 매핑
- 네이티브 가이드 기준으로 RN 플러그인의 지원 범위를 IMPLEMENTED / PARTIAL / MISSING으로 분류.

### Phase 2. Android 테스트 앱 빌드 및 실행
- JDK 17 기준 Gradle 빌드 성공 확인.
- 에뮬레이터 또는 실기기에서 앱 기동 및 초기화 UI 확인.

### Phase 3. iOS 테스트 앱 빌드 및 실행
- `pod install` 및 Xcode 빌드 성공 확인.
- 시뮬레이터에서 앱 기동 및 초기화 UI 확인.

### Phase 4. 실제 Key 입력 후 광고 노출 검증
- 실제 `mediaKey` / `adUnitId`를 사용하여 모든 포맷(Banner, Interstitial, Native, Video, Rewarded)의 로드 및 이벤트를 검증.

### Phase 5. 결과 문서화 및 지원 범위 확정
- 최종 검증 결과를 바탕으로 README 및 지원 범위 문서를 업데이트.

---

## 5. 최종 판정 기준

### 앱 복구 판정
- **FAIL**: 빌드 불가 또는 실행 즉시 크래시.
- **PARTIAL**: 빌드는 성공하나 특정 플랫폼에서 실행 불가.
- **PASS**: Android/iOS 모두 실기기/시뮬레이터에서 정상 실행.

### 광고 실검증 판정
- **FAIL**: 초기화 또는 광고 로드 자체가 불가.
- **PARTIAL**: 앱은 실행되나 실광고 노출 또는 이벤트 검증 미완료.
- **PASS**: 실제 광고 노출 및 핵심 이벤트(Impression, Click, Reward 등) 확인 완료.
