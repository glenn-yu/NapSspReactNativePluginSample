# Nap SSP React Native Plugin 작업 계획서
> 기준 보고서: `docs/review_plan_report.md` | 작성일: 2026-04-20

---

## 1. 작업 우선순위 개요

| 단계 | 심각도 | 이슈 수 | 목표 |
|------|--------|---------|------|
| Phase 1 | CRITICAL | 3건 | 런타임 크래시 / iOS 완전 불능 해소 |
| Phase 2 | HIGH | 4건 | 릴리즈 블로커 제거 및 핵심 기능 완성 |
| Phase 3 | MEDIUM | 4건 | 기능 품질 개선 |
| Phase 4 | LOW | 4건 | 장기 개선 및 아키텍처 업그레이드 |

---

## Phase 1 — CRITICAL 수정 (즉시 착수)

### ✅ Task 1-1. iOS SDK 실제 초기화 연결
**대상 파일**: `ios/NapSspSupport.swift` (`NapSspRuntime.initialize()`)  
**문제**: `NapSspRuntime.initialize()`가 config 저장만 하고 `AMMediation.shared.initialize()` 미호출 → 모든 iOS 광고 동작 불가  
**작업 내용**:
- `NapSspRuntime.initialize()` 내부에 `AMMediation.shared.initialize(mediaKey:adUnitIds:)` 호출 추가
- SDK 미링크 환경(placeholder 모드)에서는 `#if canImport(AdMixerMediation)` 조건부 컴파일 처리
- 초기화 성공/실패 콜백을 `resolve` / `reject`로 연결

**완료 기준**: iOS 실기기에서 `initialize()` 호출 후 실제 광고 요청 전송 확인

---

### ✅ Task 1-2. iOS ObjC 이름 충돌 해소 — NativeAdView
**대상 파일**: `ios/NativeAdView.swift`  
**문제**: `NativeAdView.swift`와 `NativeAdViewManager.swift` 둘 다 `@objc(NapSspNativeAdView)` 선언 → 런타임 크래시  
**작업 내용**:
- `NativeAdView.swift`의 클래스 선언을 `@objc(NapSspNativeAdUIView)` 로 변경
- `NapSspBridge.m` 의 `RCT_EXTERN_MODULE` / `RCT_EXTERN__REMAP_MODULE` 선언 확인 및 정합성 맞추기

**완료 기준**: iOS 빌드 경고/에러 없이 NativeAd 뷰 컴포넌트 정상 등록

---

### ✅ Task 1-3. iOS ObjC 이름 충돌 해소 — VideoAdView
**대상 파일**: `ios/VideoAdView.swift`  
**문제**: `VideoAdView.swift`와 `VideoAdViewManager.swift` 둘 다 `@objc(NapSspVideoAdView)` 선언 → 런타임 크래시  
**작업 내용**:
- `VideoAdView.swift`의 클래스 선언을 `@objc(NapSspVideoAdUIView)` 로 변경
- `NapSspBridge.m` 선언 정합성 확인

**완료 기준**: iOS 빌드 경고/에러 없이 VideoAd 뷰 컴포넌트 정상 등록

---

## Phase 2 — HIGH 수정 (릴리즈 전 필수)

### ✅ Task 2-1. Android ProGuard 규칙 추가
**대상 파일**: `android/consumer-rules.pro`  
**문제**: 공식 가이드 6종 keep 규칙 전부 누락 → 릴리즈 빌드(minify) 시 SDK 클래스 제거되어 동작 불가  
**작업 내용**: 다음 규칙 추가
```
-keep class com.nasmedia.admixerssp.** { *; }
-keep class com.nasmedia.admanager.** { *; }
-keep class com.nasmedia.adfit.** { *; }
-keep class com.pangle.** { *; }
-keep class com.applovin.** { *; }
-keep interface com.nasmedia.** { *; }
```

**완료 기준**: 릴리즈 빌드(`minifyEnabled true`) 후 광고 SDK 정상 동작 확인

---

### ✅ Task 2-2. 전면 / 리워드 / 전면동영상 options → AdInfo.Builder 연결 (Android)
**대상 파일**:
- `android/src/main/java/com/gwangy/InterstitialModule.kt`
- `android/src/main/java/com/gwangy/RewardedAdModule.kt`
- `android/src/main/java/com/gwangy/InterstitialVideoModule.kt`

**문제**: `load()` 수신 `options` ReadableMap이 `AdInfo.Builder`에 전혀 적용 안 됨  
**작업 내용**:

| 모듈 | 적용 옵션 | AdInfo.Builder API |
|------|----------|--------------------|
| InterstitialModule | `type`, `countDownTime`, `popupOption`, `buttonLeftText/Right` | `setInterstitialType()`, `setCountDownTime()`, `setPopupOption()` |
| RewardedAdModule | `customParams`, `mute` | `setCustomParams()`, `setMute()` |
| InterstitialVideoModule | `timeout`, `maxRetryCountInSlot`, `customParams` | `setTimeout()`, `setMaxRetryCountInSlot()`, `setCustomParams()` |

**완료 기준**: 각 옵션 전달 후 네이티브 SDK 동작 변경 확인

---

### ✅ Task 2-3. 전면 / 리워드 options → iOS 네이티브 적용
**대상 파일**:
- `ios/InterstitialModule.swift`
- `ios/RewardedModule.swift`
- `ios/InterstitialVideoModule.swift`

**문제**: options 파싱/적용 코드 없음  
**작업 내용**:
- `NapSspConfiguration` 또는 별도 파싱 유틸로 options NSDictionary 파싱
- `AMMInterstitial`, `AMMRewardVideo`, `AMMVideoInterstitial` API에 옵션 전달
- `RewardedAdOptions.customParams` → `setCustomParams(_:)` 연결
- iOS 전면 광고 `closeButtonTouchAreaRatio` 옵션 타입 추가 및 적용 (`src/types.ts` + Swift)

**완료 기준**: popup 타입 전면 광고 / customParams 포함 리워드 광고 정상 동작

---

### ✅ Task 2-4. NativeAd 실제 SDK 바인딩 구현
**대상 파일**:
- `android/src/main/java/com/gwangy/NapSspNativeAdView.kt`
- `ios/NativeAdView.swift`

**문제**: 양 플랫폼 모두 300ms placeholder만 존재. 에셋 바인딩 미구현  
**작업 내용**:

**Android**:
- `FrameLayout` → `RelativeLayout` 기반으로 컨테이너 재구성
- reflection으로 `NativeAdView` 생성, `NativeAdViewBinder` 빌더 구성
- 에셋 6종 (icon, headline, advertiser, description, media, cta) View 바인딩
- `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked` 이벤트 발행

**iOS**:
- `AMMNativeAdViewContainer` 로드 후 v2.2.1 대응: `loadAD()` 전 기존 subview `removeView` 처리
- 에셋 6종 UIView 바인딩
- `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked` 이벤트 발행

**완료 기준**: 실기기에서 네이티브 광고 에셋(이미지, 텍스트, CTA 버튼) 정상 렌더링

---

## Phase 3 — MEDIUM 개선 (QA 전 완료)

### ✅ Task 3-1. Android 배너 onResume / onPause 생명주기 연결
**대상 파일**: `android/src/main/java/com/gwangy/NapSspBannerView.kt`  
**작업 내용**:
- `LifecycleEventListener` 구현 (`onHostResume`, `onHostPause`, `onHostDestroy`)
- `onHostResume` → `adView?.resume()`, `onHostPause` → `adView?.pause()` reflection 호출

---

### ✅ Task 3-2. onAdImpression 이벤트 발행 (전 포맷)
**대상 파일**: Android 각 모듈 / iOS 각 모듈  
**작업 내용**:
- Android: reflection proxy `onEventAd(IMPRESSION)` 이벤트 → `EVENT_AD_IMPRESSION` 발행
- iOS: 각 fullscreen 모듈 `show()` 내 `onAdOpened` 직후 `onAdImpression` 발행
- 배너: Android `onReceivedAd` 직후, iOS `onAdLoaded` 직후 impression 발행

---

### ✅ Task 3-3. Log Level 실제 SDK API 연결
**대상 파일**:
- `android/src/main/java/com/gwangy/NapSspSdkBridge.kt`
- `ios/NapSspSupport.swift` (`NapSspRuntime`)

**작업 내용**:
- Android: `setLogLevel()` 내부에서 reflection으로 `AdMixerLog.setLogLevel(level)` 호출
- iOS: `setLogLevel()` 내부에서 `AMMediation.shared.setDebugEnabled(_:)` 호출

---

### ✅ Task 3-4. iOS 에러코드 7종 매핑
**대상 파일**: `ios/NapSspSupport.swift`, `src/errors.ts`  
**작업 내용**:
- `NapSspError` enum에 SDK 에러코드 0~6 케이스 추가 (또는 rawValue Int 부여)
- `onAdFailedToLoad` payload에 `code: Int` 필드 포함하여 발행
- JS `AdError` 타입에 `code?: number` 필드 추가 (이미 있으면 확인)
- `normalizeAdError()` 함수에서 코드 매핑 처리

---

## Phase 4 — LOW / 장기 개선

### ✅ Task 4-1. Pangle / AppLovin / UnityAds 별도 초기화 추가 (Android)
**대상 파일**: `android/src/main/java/com/gwangy/NapSspSdkBridge.kt`  
**작업 내용**:
- `MediationConfig`의 `pangleAppId` → `PAGSdk.init()` reflection 호출
- `appLovinSdkKey` → `ALSdk.initializeSdk()` reflection 호출
- UnityAds → `UnityAds.initialize()` reflection 호출

---

### ✅ Task 4-2. iOS Package.swift SPM binaryTarget 추가
**대상 파일**: `ios/Package.swift`  
**작업 내용**:
- `AdMixerMediation.xcframework` binaryTarget 추가 또는 원격 URL 참조
- `docs/spm_integration.md` 업데이트

---

### ✅ Task 4-3. 통합 테스트 앱 커버리지 확장
**대상 파일**: `integration-test-app/App.tsx`  
**작업 내용**:
- RewardedAd, VideoAd, NativeAd, InterstitialVideoAd 테스트 섹션 추가
- AdUnit ID 입력 TextField 추가 (동적 테스트 지원)
- 각 광고 포맷별 이벤트 로그 표시 UI 추가

---

### ✅ Task 4-4. TurboModules / Fabric 지원 (New Architecture) — JS Spec 작성 완료
**대상 파일**: 전체 플러그인 소스  
**작업 내용**:
- `NativeNapSspModuleSpec.ts` (JS spec) 작성
- Android: `TurboReactPackage` 기반으로 모듈 교체
- iOS: `NativeNapSspModuleSpec` ObjC 헤더 생성, Swift 구현체 연결
- View Managers: Fabric `ViewManagerDelegate` 구현
> ⚠️ RN 버전 및 Expo 호환성 사전 검토 필요. 별도 스프린트로 진행 권장.

---

## 작업 일정 (예상)

| Phase | 예상 소요 | 담당 범위 |
|-------|----------|----------|
| Phase 1 (CRITICAL 3건) | 1~2일 | iOS 초기화 연결, ObjC 충돌 2건 |
| Phase 2 (HIGH 4건) | 3~5일 | ProGuard, options 연결, NativeAd 구현 |
| Phase 3 (MEDIUM 4건) | 2~3일 | 생명주기, impression, 로그, 에러코드 |
| Phase 4 (LOW 4건) | 별도 스프린트 | 아키텍처 업그레이드, SPM, 테스트 앱 |

---

## 작업 전 확인 사항

1. **Android `enableVendorSdk`**: `android/build.gradle`의 기본값 `false` → 실제 SDK 테스트 시 `true`로 설정 필요
2. **iOS SDK 링킹**: CocoaPods 환경에서 `AdMixerMediation` pod 설치 확인 후 Phase 1 작업 진행
3. **실기기 테스트**: Phase 2 완료 후 Android / iOS 실기기에서 각 광고 포맷별 동작 확인
4. **릴리즈 빌드 검증**: Task 2-1(ProGuard) 완료 후 반드시 `minifyEnabled true` 빌드로 검증
