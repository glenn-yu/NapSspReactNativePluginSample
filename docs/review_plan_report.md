# Nap SSP React Native Plugin 전수 조사 보고서
> 기준 문서: `docs/review_plan.md` | 조사 일자: 2026-04-20

---

## 2.1 SDK 초기화 및 공통 설정 (Core)

### [ FAIL ] Initialization — mediaKey / adUnitIds 네이티브 전달

- **Android**: `NapSspSdkBridge.initialize()` 에서 reflection으로 `AdMixer.getInstance().initialize()` 호출. `mediaKey`와 `adUnitIds` 전달 코드 존재. `BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED == false`(기본값) 일 때 실제 SDK 미호출.
- **iOS**: `NapSspModule.initialize()` → `NapSspRuntime.shared.initialize()` 호출. `NapSspRuntime.initialize()`는 설정값을 저장하고 `isInitialized = true`로 전환할 뿐, **실제 `AMMediation.shared.initialize()`를 호출하지 않음**.
- **판정**: Android PARTIAL (빌드 플래그 조건부), iOS FAIL (실제 SDK 미호출)

---

### [ FAIL ] Log Level — setDebugEnabled / AdMixerLog.setLogLevel JS 연동

- **Android**: `NapSspModule.setLogLevel()` → `NapSspSdkBridge.setLogLevel()` 호출. SdkBridge 내부에서 로그 레벨 값을 저장하기만 하고, reflection을 통한 `AdMixerLog.setLogLevel()` 호출 코드 없음.
- **iOS**: `NapSspModule.setLogLevel()` → `NapSspRuntime.shared.config.logLevel` 저장만 함. `AMMediation.shared.setDebugEnabled()` 호출 코드 없음.
- **판정**: Android FAIL, iOS FAIL

---

### [ PARTIAL ] Mediation Adapters — registerAdapter (Android) 초기화 포함 여부

- **Android**: `NapSspSdkBridge.initialize()` 내에서 `AdMixer.registerAdapter()`를 6종 (ADMANAGER, ADFIT, MOBWITH, PANGLE, APPLOVIN, UNITY) reflection 호출. ✅
- **미흡**: Pangle `PAGSdk.init()`, AppLovin `ALSdk.initialize()`, UnityAds `UnityAds.initialize()` 별도 초기화 코드 없음. 공식 Android 가이드의 "각 미디에이션 SDK 초기화" 단계 누락.
- **iOS**: `AMMediation.shared.initialize()` 자체가 미호출이므로 어댑터 등록 불가.
- **판정**: Android PARTIAL, iOS FAIL

---

### [ PARTIAL ] Pangle / AppLovin Key — appId / sdkKey 네이티브 반영

- **Android**: `MediationConfig`에 `pangleAppId`, `appLovinSdkKey` 필드 정의됨 (`src/types.ts`). 그러나 `NapSspSdkBridge`에서 해당 값을 각 SDK 초기화 API에 전달하는 코드 없음.
- **iOS**: `NapSspConfiguration` struct에서 파싱하지 않음.
- **판정**: Android PARTIAL (타입 정의만 존재), iOS FAIL

---

## 2.2 광고 포맷별 구현

### 배너 광고 (BannerAd)

#### [ FAIL ] Lifecycle — onResume / onPause / onDestroy (Android) / stop (iOS)

- **Android**: `NapSspBannerView`는 `FrameLayout` 상속. `onResume()` / `onPause()` 메서드 없음. `onDetachedFromWindow()` 내 `adView?.let { reflectCall(it, "onDestroy") }` 호출로 destroy만 존재.
- **iOS**: `BannerView.swift` — `deinit`에서 observer 제거 및 DispatchWorkItem 취소. 실제 SDK `stop()` 호출 없음 (placeholder 구현).
- **판정**: Android PARTIAL (destroy만), iOS PARTIAL (placeholder)

#### [ PARTIAL ] Events — onSuccess / onFail / onTap → JS 콜백

- **Android**: reflection proxy로 `onReceivedAd`→`onAdLoaded`, `onFailedToReceiveAd`→`onAdFailedToLoad`, `onEventAd(CLICK)`→`onAdClicked` 매핑. `onAdImpression` 이벤트 **미발행**.
- **iOS**: `BannerViewManager`에서 `RCTBubblingEventBlock`으로 `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked` 전달. `onAdImpression` **미발행**.
- **판정**: Android/iOS 모두 onAdImpression 누락으로 PARTIAL

#### [ OK ] Size Mapping — 320x50 / 300x250 등 사이즈 상수

- `src/types.ts`에 `BannerSize` 타입: `BANNER_320x50`, `BANNER_300x250`, `LARGE_BANNER_320x100`, `SMART_BANNER`, `MEDIUM_RECTANGLE_300x250` 정의. ✅
- Android `BannerViewManager`, iOS `NapSspBannerSize.parse()` 에서 파싱 구현. ✅
- **판정**: OK

---

### 전면 광고 (InterstitialAd)

#### [ PARTIAL ] Config — basic / popup / countDown 3종 옵션 네이티브 전달

- **타입 정의**: `InterstitialAdOptions` — `type: 'basic' | 'popup' | 'countdown'`, `countDownTime`, `popupOption`, `buttonLeftText`, `buttonRightText` 정의. ✅
- **Android**: `InterstitialModule.load()` 에서 `options` ReadableMap 수신 후 `AdInfo.Builder`에 적용 코드 없음. 옵션이 **무시됨**.
- **iOS**: `InterstitialModule.load()` 에서 options 파싱/적용 코드 없음. 옵션 **무시됨**.
- **판정**: PARTIAL (타입 정의 존재, 실제 적용 안 됨)

#### [ OK ] Method — load() / show() 비동기 Promise 분리

- Android: `load()` / `show()` 각각 `@ReactMethod ... Promise` ✅
- iOS: `load()` / `show()` 각각 `resolve`/`reject` 패턴 ✅
- JS: `async load()` / `async show()` ✅
- **판정**: OK

#### [ PARTIAL ] Resource Recovery — 광고 닫힘 시 stop / nil 처리

- **Android**: `onEventAd(CLOSE)` 수신 시 이벤트 발행 후 `adMap`에서 인스턴스 제거 (`adMap.remove(adUnitId)`). `onDestroy` reflection 호출 없음.
- **iOS**: `show()` 직후 `onAdClosed` 바로 발행 (placeholder). 실제 AMMInterstitial nil 처리 없음.
- **판정**: Android PARTIAL, iOS PARTIAL

---

### 네이티브 광고 (NativeAd)

#### [ FAIL ] Asset Mapping — icon / headline / advertiser / description / media / cta

- **Android**: `NapSspNativeAdView.kt` 전체가 placeholder. 300ms 후 `onAdLoaded` 발행만 함. 실제 NativeAdViewBinder 바인딩 코드 없음.
- **iOS**: `NativeAdView.swift` 전체가 placeholder. 300ms 후 `onAdLoaded` 발행만 함.
- 6종 에셋 매핑 **미구현**.
- **판정**: FAIL

#### [ FAIL ] iOS v2.2.1 대응 — loadAD() 시 removeView 로직

- iOS `NativeAdView.swift`가 placeholder이므로 실제 `AMMNativeAdViewContainer.loadAD()` 호출 없음. removeView 로직 적용 불가.
- **판정**: FAIL

#### [ FAIL ] Layout — Android RelativeLayout 기반 컨테이너

- `NapSspNativeAdView.kt`: `FrameLayout` 상속. 설계서의 RelativeLayout 기반 구조 미적용.
- **판정**: FAIL

---

### 리워드 동영상 (RewardedAd)

#### [ OK ] Reward Callback — EARNEDREWARD (Android) / onRewardVideoEarned (iOS) → onRewarded 통합

- **Android**: `RewardedAdModule` — `onEventAd(EARNEDREWARD)` → `EVENT_REWARDED("onRewarded")` 발행. ✅
- **iOS**: `RewardedModule.show()` → `onRewarded` 이벤트 발행. ✅
- JS `RewardedAd`: `onRewarded` 이벤트 리스닝. ✅
- **판정**: OK

#### [ PARTIAL ] Custom Params — customParam (iOS) / setCustomParams (Android) 딕셔너리 전달

- **타입 정의**: `RewardedAdOptions.customParams: Record<string, string>` 정의. ✅
- **Android**: `RewardedAdModule.load()` 에서 `options` 수신하나 `AdInfo.Builder`에 `customParams` 적용 코드 없음.
- **iOS**: `RewardedModule.load()` 에서 options 파싱/적용 코드 없음.
- **판정**: PARTIAL (타입 정의만 존재)

---

## 2.3 네이티브 플랫폼 특이사항

### [ OK ] Android Gradle — 필수 의존성 및 미디에이션 repositories

- `android/build.gradle`: Kakao (`devrepo.kakao.com`), Pangle (`artifact.bytedance.com`) repositories 포함. ✅
- SDK 좌표: core `1.0.21`, adManager `1.0.14`, adFit `1.0.10`, pangle `1.0.10`, appLovin `1.0.8`, unity `1.0.6`, adsIdentifier 포함. ✅
- `enableVendorSdk` 플래그로 조건부 포함.
- **판정**: OK

### [ FAIL ] Android Proguard — consumer-rules.pro 공식 keep 규칙 6종

- 현재 `android/consumer-rules.pro` 내용:
  ```
  -keep class com.gwangy.** { *; }
  -keep class com.facebook.react.** { *; }
  ```
- 공식 가이드의 6종 keep 규칙 **전부 누락**:
  - `-keep class com.nasmedia.admixerssp.** { *; }` ❌
  - `-keep class com.nasmedia.admanager.** { *; }` ❌
  - `-keep class com.nasmedia.adfit.** { *; }` ❌
  - `-keep class com.pangle.** { *; }` ❌
  - `-keep class com.applovin.** { *; }` ❌
  - `-keep interface com.nasmedia.** { *; }` ❌
- **판정**: FAIL

### [ OK ] iOS Info.plist — GADApplicationIdentifier / SKAdNetwork README 명시

- `README.md`에 `GADApplicationIdentifier`, `NSUserTrackingUsageDescription` 설정 안내 포함. ✅
- `docs/ios_integration.md`에 SKAdNetworkIdentifier 가이드 존재.
- **판정**: OK

### [ OK ] ATT Handling — requestTrackingAuthorization 유틸리티

- `NapSspRuntime.requestTrackingAuthorization()` — `ATTrackingManager.requestTrackingAuthorization` 호출 구현. ✅
- `NapSspAd.requestTrackingAuthorization()` JS API로 노출. ✅
- `src/types.ts`에 `NapSspStatus.attStatus` 필드 정의. ✅
- **판정**: OK

---

## 2.4 아키텍처 및 New Architecture (Fabric/JSI)

### [ FAIL ] TurboModules — NapSspModule JSI 동작

- `android/src/main/java/com/gwangy/NapSspModule.kt`: `ReactContextBaseJavaModule` 상속. TurboModule (`NativeNapSspModuleSpec`) 미구현.
- iOS: `RCTEventEmitter` 상속. TurboModule 미구현.
- `integration-test-app/android/MainApplication.kt`: `isNewArchEnabled = false` 확인.
- 설계서 Phase 4에 TurboModules 계획이 있으나 **미구현**.
- **판정**: FAIL

### [ FAIL ] Fabric — 배너 / 네이티브 뷰 Fabric Renderer 지원

- Android View Managers: `SimpleViewManager` 상속. Fabric(`ViewManagerDelegate`) 미구현.
- iOS View Managers: `RCTViewManager` 상속. Fabric 미구현.
- **판정**: FAIL

### [ OK ] Compatibility — RN 0.72~0.74+ 하위 호환성

- `package.json`: `peerDependencies "react-native": ">=0.72.0"` ✅
- Old Architecture 전용이므로 0.72~0.74 범위에서 동작.
- **판정**: OK

---

## 3. 공식 가이드 대비 누락 사항 분석 (Gap Analysis)

| 항목 | 공식 가이드 내용 | 구현 여부 | 비고 |
|------|----------------|-----------|------|
| **에러 코드** | iOS 7종 에러코드 (0~6) 매핑 | ❌ MISSING | `NapSspError` enum 4종(invalidConfiguration, notInitialized, adNotLoaded, unsupported)으로만 구성. 0~6 코드 미매핑 |
| **비즈보드** | Kakao AdFit 비즈보드 단독 지면 정책 | ❌ MISSING | 타입 정의 없음. 별도 adUnitType/bizboard 설정 미구현 |
| **음소거** | 리워드 광고 `setMute(true)` | ⚠️ PARTIAL | `RewardedAdOptions.mute: boolean` 타입 정의됨. Android `AdInfo.Builder`에 전달 코드 없음. iOS 미구현 |
| **타임아웃** | 전면 광고 `interstitialTimeout` (Android) | ⚠️ PARTIAL | `InterstitialVideoAdOptions.timeout` 정의됨. `AdInfo.Builder` 적용 없음 |
| **클릭 영역** | iOS 전면 광고 `closeButtonTouchAreaRatio` (0.2~1.0) | ❌ MISSING | 타입 정의 없음. 구현 없음 |

---

## 4. 테스트 앱 (Example App) 검증

### [ PARTIAL ] Integration Test — 모든 포맷 단일 앱 테스트

- `integration-test-app/App.tsx` 검증 결과:
  - ✅ `NapSspAd.initialize()` 호출
  - ✅ `BannerAd` 렌더링
  - ✅ `InterstitialAd` load/show 흐름
  - ❌ `RewardedAd` — 미포함
  - ❌ `NativeAd` — 미포함
  - ❌ `VideoAd` — 미포함
  - ❌ `InterstitialVideoAd` — 미포함
- **판정**: PARTIAL

### [ FAIL ] Mediation Test — 실제 AdUnit ID 입력 및 네트워크별 광고 호출 시뮬레이션

- 앱에 AdUnit ID 입력 UI(TextField 등) 없음. 하드코딩된 테스트 ID만 사용.
- 네트워크별 호출 선택 UI 없음.
- **판정**: FAIL

### [ N/A ] Memory Leak — Profiler 기반 메모리 검증

- 정적 분석 범위 외. 실기기/에뮬레이터 Profiler 실행 필요.
- **판정**: N/A (정적 분석 불가)

---

## 5. 심각도별 이슈 목록

| # | 심각도 | 분류 | 내용 |
|---|--------|------|------|
| 1 | **CRITICAL** | iOS Core | `NapSspRuntime.initialize()`가 실제 `AMMediation.shared.initialize()` 호출 안 함 — 모든 iOS 광고 동작 불가 |
| 2 | **CRITICAL** | iOS ObjC 충돌 | `NativeAdView.swift`와 `NativeAdViewManager.swift` 모두 `@objc(NapSspNativeAdView)` — 런타임 크래시 위험 |
| 3 | **CRITICAL** | iOS ObjC 충돌 | `VideoAdView.swift`와 `VideoAdViewManager.swift` 모두 `@objc(NapSspVideoAdView)` — 런타임 크래시 위험 |
| 4 | **HIGH** | Android ProGuard | `consumer-rules.pro`에 SDK vendor keep 규칙 6종 전부 누락 — 릴리즈 빌드 시 SDK 동작 불가 |
| 5 | **HIGH** | Android/iOS | NativeAd 전체 placeholder — 실제 NativeAdView 바인딩 미구현 |
| 6 | **HIGH** | Android/iOS | Log Level 설정이 실제 SDK API(`AdMixerLog.setLogLevel` / `setDebugEnabled`)에 전달 안 됨 |
| 7 | **HIGH** | Android | InterstitialAd / RewardedAd / InterstitialVideoAd `options` 파싱 후 AdInfo.Builder 적용 없음 |
| 8 | **MEDIUM** | Android | `NapSspBannerView` onResume/onPause 미구현 — 화면 전환 시 배너 갱신 안 됨 |
| 9 | **MEDIUM** | Android/iOS | `onAdImpression` 이벤트 모든 포맷에서 미발행 |
| 10 | **MEDIUM** | iOS | `InterstitialModule.show()` — `onAdOpened` → `onAdClosed` 즉시 발행 (실제 광고 표시 없음) |
| 11 | **MEDIUM** | Gap | iOS 에러코드 7종(0~6) 매핑 없음 — `onAdFailedToLoad` payload에 에러코드 미포함 |
| 12 | **LOW** | Android | `BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED` 기본값 `false` — 실 SDK 미포함 빌드가 기본값 |
| 13 | **LOW** | iOS | `Package.swift`에 AdMixerMediation binaryTarget 없음 — SPM 방식 사용 시 SDK 링킹 불가 |
| 14 | **LOW** | Architecture | TurboModules / Fabric 미구현 (RN New Architecture 미지원) |
| 15 | **LOW** | Integration | 테스트 앱에 RewardedAd / NativeAd / VideoAd / InterstitialVideoAd 테스트 코드 없음 |

---

## 6. 요약

| 분류 | 총 항목 | OK | PARTIAL | FAIL | N/A |
|------|--------|----|---------|------|-----|
| 2.1 SDK 초기화 | 4 | 0 | 1 | 3 | 0 |
| 2.2 배너 | 3 | 1 | 2 | 0 | 0 |
| 2.2 전면 | 3 | 1 | 2 | 0 | 0 |
| 2.2 네이티브 | 3 | 0 | 0 | 3 | 0 |
| 2.2 리워드 | 2 | 1 | 1 | 0 | 0 |
| 2.3 플랫폼 특이사항 | 4 | 3 | 0 | 1 | 0 |
| 2.4 아키텍처 | 3 | 1 | 0 | 2 | 0 |
| 3. Gap Analysis | 5 | 0 | 2 | 3 | 0 |
| 4. 테스트 앱 | 3 | 0 | 1 | 1 | 1 |
| **합계** | **30** | **7** | **9** | **13** | **1** |

**전체 준수율: 7/30 (23%) OK, 추가 16/30 PARTIAL 또는 FAIL**

> 핵심 결론: iOS는 실제 SDK 초기화가 전혀 이루어지지 않아 모든 광고가 placeholder 동작. Android는 ProGuard 누락 및 옵션 미전달이 주요 릴리즈 블로커. NativeAd는 Android/iOS 모두 미구현 상태.
