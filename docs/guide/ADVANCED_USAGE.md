# Advanced Usage & Best Practices (고급 활용 및 모범 사례)

> KO: `react-native-nap-ssp`의 고급 활용 방법 및 모범 사례입니다. (플러그인 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7)
> EN: Advanced usage and best practices for `react-native-nap-ssp`. (Plugin 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7)

---

## 목차 (Table of Contents)

1. [광고 미리 불러오기 (Pre-loading)](#1-광고-미리-불러오기-pre-loading)
2. [광고 상태 관리 (Ad State Management)](#2-광고-상태-관리-ad-state-management)
3. [에러 핸들링 전략 (Error-Handling Strategy)](#3-에러-핸들링-전략-error-handling-strategy)
4. [메모리 관리 (Memory Management)](#4-메모리-관리-memory-management)
5. [미디에이션 구성 (Mediation Configuration)](#5-미디에이션-구성-mediation-configuration)
6. [보상형 광고 S2S 파라미터 (Rewarded S2S Parameters)](#6-보상형-광고-s2s-파라미터-rewarded-s2s-parameters)
7. [전면 광고 (Interstitial Ads — v2)](#7-전면-광고-interstitial-ads--v2)
8. [디버그 vs 릴리즈 빌드 동작 차이 (Debug vs Release Behavior)](#8-디버그-vs-릴리즈-빌드-동작-차이-debug-vs-release-behavior)

---

## 1. 광고 미리 불러오기 (Pre-loading)

> KO: 사용자가 광고를 보게 될 시점보다 미리 `load()`를 호출하면 대기 시간을 줄이고 사용자 경험을 향상시킬 수 있습니다.
> EN: Calling `load()` before the user reaches the ad moment reduces wait time and improves UX.

```tsx
const interstitial = new InterstitialAd('전면_광고_ID');

// 광고가 필요한 화면 진입 시 미리 로드 / preload on entering the screen that needs the ad
useEffect(() => {
  interstitial.load();
}, []);

const handleFinishStage = async () => {
  // 이미 로드되어 있어 즉시 표시됨 / already loaded, shows instantly
  await interstitial.show();
};
```

> KO: 💡 `closed` 이벤트 수신 후 다음 광고를 위해 바로 `load()`를 호출해 두는 패턴을 권장합니다.
> EN: 💡 After receiving the `closed` event, immediately call `load()` again to prepare the next ad.

---

## 2. 광고 상태 관리 (Ad State Management)

> KO:
> - **배너/네이티브**: 화면 언마운트 시 컴포넌트가 자동 정리됩니다. 복잡한 네비게이션 환경에서는 조건부 렌더링으로 명시적으로 관리하세요.
> - **전면/보상형**: `closed` 이벤트 후 다음 광고를 위해 `load()`를 미리 호출하세요.
>
> EN:
> - **Banner/Native**: components clean up automatically on unmount. In complex navigation, manage them explicitly with conditional rendering.
> - **Interstitial/Rewarded**: after the `closed` event, preload the next ad with `load()`.

---

## 3. 에러 핸들링 전략 (Error-Handling Strategy)

> KO: 네트워크 상황이나 광고 물량 부족(No Fill)으로 로드가 실패할 수 있습니다. 즉시 재시도보다 일정 지연 후 재시도를 권장합니다.
> EN: Loads can fail due to network conditions or No Fill. Prefer a delayed retry over an immediate one.

```tsx
<BannerAd
  onAdFailedToLoad={(error) => {
    console.warn(error.message);
    // 즉시 재시도보다 30초 후 재시도 권장 / retry after ~30s rather than immediately
  }}
/>
```

---

## 4. 메모리 관리 (Memory Management)

> KO: 동영상 광고나 이미지 자산이 많은 네이티브 광고를 자주 사용하는 경우, 더 이상 필요 없는 광고 객체는 리스너를 제거하고 참조를 해제하여 메모리 누수를 방지하세요.
> EN: When frequently using video ads or image-heavy native ads, remove listeners and release references to ad objects you no longer need to avoid memory leaks.

---

## 5. 미디에이션 구성 (Mediation Configuration)

> KO: v2.0.0부터 `registerAdapter()`는 제거되었습니다. `initialize()`가 Gradle/Pod 의존성에 있는 어댑터를 자동 등록하므로, JS에서는 `mediations`로 사용할 네트워크만 켜면 됩니다. v2에서 **NaverAdManager**와 **Teads(Android 전용)** 어댑터가 추가되었습니다.
> EN: From v2.0.0, `registerAdapter()` was removed. `initialize()` auto-registers any adapter present in your Gradle/Pod dependencies, so in JS you only enable the networks you want via `mediations`. v2 adds the **NaverAdManager** and **Teads (Android-only)** adapters.

```tsx
NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['...'],
  mediations: {
    admanager: true,       // AdManager(GAM)
    adfit: true,
    pangle: true,
    applovin: true,
    unity: true,
    naverAdManager: true,  // v2 신규 / new in v2
    teads: true,           // v2 신규, Android 전용 / new in v2, Android-only
  },
});
```

> KO: 어댑터별 주의사항:
> - **AdManager(GAM)**: `play-services-ads`를 **25.2.0 상한**으로 강제하세요(25.3.0+ 비호환). 최소 Android API 23.
> - **Teads(Android 전용)**: 전용 Maven 저장소 2곳 필요(`https://sdk.teads.tv/android/repo`, `https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod`). iOS에는 Teads가 없습니다.
> - **최소 Android API**: Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24.
>
> EN: Per-adapter notes:
> - **AdManager (GAM)**: force `play-services-ads` to a **25.2.0 ceiling** (25.3.0+ incompatible). Min Android API 23.
> - **Teads (Android-only)**: needs two dedicated Maven repos (`https://sdk.teads.tv/android/repo`, `https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod`). Teads does not exist on iOS.
> - **Min Android API**: Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24.

---

## 6. 보상형 광고 S2S 파라미터 (Rewarded S2S Parameters)

> KO: 보상형 광고를 서버-to-서버(S2S)로 검증할 때 사용자 식별값을 함께 전달할 수 있습니다. `RewardedAdOptions`는 `customParams?`와 `mute?`(Android 전용)를 지원합니다.
> EN: When validating rewarded ads server-to-server (S2S) you can pass user identifiers. `RewardedAdOptions` supports `customParams?` and `mute?` (Android-only).

```tsx
const rewarded = new RewardedAd('보상형_광고_ID', {
  mute: true, // Android 전용 / Android-only
  customParams: {
    useid: 'user_unique_id',   // 유저 식별자 / user id
    name: '홍길동',             // (선택) 유저 이름 / (optional) name
    phone: '010-0000-0000',    // (선택) 연락처 / (optional) phone
  },
});
```

### S2S 콜백 자동 포함 파라미터 (Auto-included S2S Callback Parameters)

> KO: 파트너 사이트에서 콜백 URL을 설정하면 아래 파라미터가 자동 포함됩니다.
> EN: When you configure a callback URL in the partner site, the parameters below are auto-included.

| 파라미터 / Param | 설명 / Description | 예시 / Example |
| :--- | :--- | :--- |
| `media_key` | 미디어 키 / media key | `12345678` |
| `adunit_id` | 광고 단위 ID / ad unit ID | `87654321` |
| `adid` | Android 광고 식별자 / Android advertising ID | `xxxx-xxxx` |
| `ifa` | iOS 기기 식별자 / iOS IDFA | `860635ea-...` |
| `earnedreward` | 리워드 지급 여부 / reward granted | `1` |
| `timestamp` | 리워드 지급 시간 / grant time | `1546300800` |

> KO: **콜백 URL 예시:**
> EN: **Callback URL example:**

```
{매체사_콜백_URL}?media_key={mediakey}&adunit_id={adunitid}&adid={adid}&timestamp={timestamp}&useid=user_id&name=홍길동
```

> KO: 💡 S2S 방식은 클라이언트 콜백보다 신뢰성이 높습니다. 보안이 중요한 서비스에서는 S2S를 우선 사용하세요. 콜백 URL은 파트너 사이트 → **미디어 관리 → 애드유닛 광고 설정**에서 입력합니다.
> EN: 💡 S2S is more reliable than client callbacks. Prefer S2S for security-sensitive services. Set the callback URL in the partner site under **Media Management → Ad Unit Settings**.

---

## 7. 전면 광고 (Interstitial Ads — v2)

> KO: v2.0.0부터 전면 광고는 **Basic 전용**입니다. 이전 버전의 popup·countDown 형식과 닫기 버튼 텍스트(`buttonLeftText`/`buttonRightText`)·카운트다운 시간(`countDownTime`)·`type`·배경 알파 옵션은 모두 **제거**되었습니다. 별도 옵션 없이 사용하세요.
> EN: From v2.0.0, interstitials are **Basic-only**. The previous popup/countDown styles and the close-button text (`buttonLeftText`/`buttonRightText`), countdown time (`countDownTime`), `type`, and background-alpha options were all **removed**. Use it without those options.

```tsx
// Basic 전용 — 옵션 없이 생성 / Basic-only — create without style options
const interstitial = new InterstitialAd('전면_광고_ID');
await interstitial.load();
await interstitial.show();
```

> KO: iOS에서는 닫기 버튼 터치 영역만 `closeButtonTouchAreaRatio?`로 조정할 수 있습니다(iOS 전용).
> EN: On iOS you can adjust only the close-button touch area via `closeButtonTouchAreaRatio?` (iOS-only).

```tsx
const interstitial = new InterstitialAd('전면_광고_ID', {
  closeButtonTouchAreaRatio: 1.5, // iOS 전용 / iOS-only
});
```

---

## 8. 디버그 vs 릴리즈 빌드 동작 차이 (Debug vs Release Behavior)

> KO: 빌드 타입에 따라 광고 실패 시 동작이 달라집니다.
> EN: Behavior on ad failure differs by build type.

### DEBUG 빌드 (개발/시뮬레이터) / DEBUG build (development/simulator)

> KO: SDK 광고 로드 실패 또는 응답 없음 시 **플레이스홀더 성공 이벤트**를 발행합니다.
> - `onAdLoaded` / `onAdImpression`가 발행됨 (실제 소재 없음)
> - 12초 내 응답이 없으면 타임아웃 폴백으로 플레이스홀더 이벤트 발행
> - 전면·보상형은 `show()` 시 플레이스홀더 경로로 즉시 성공 처리
> - iOS 전면은 `show()` 직후 `onAdOpened` / `onAdImpression` 즉시 발행
>
> EN: On load failure or no response, emits a **placeholder success event**.
> - `onAdLoaded` / `onAdImpression` fire (no real creative)
> - if no response within 12s, a timeout fallback emits a placeholder event
> - interstitial/rewarded resolve instantly via the placeholder path on `show()`
> - iOS interstitials emit `onAdOpened` / `onAdImpression` immediately after `show()`

> KO: payload의 `source` 필드로 실제 광고와 플레이스홀더를 구분하세요.
> EN: Distinguish real ads from placeholders via the `source` field.

```tsx
<BannerAd
  onAdLoaded={(e) => {
    if (e?.source?.startsWith('debug') || e?.source === 'placeholder') {
      console.log('플레이스홀더 — 실제 광고 아님 / placeholder — not a real ad');
    } else {
      console.log('실제 광고 로드 성공 / real ad loaded');
    }
  }}
/>
```

### RELEASE 빌드 (프로덕션) / RELEASE build (production)

> KO:
> - SDK 로드 실패 시 `onAdFailedToLoad` 발행 (실제 에러 코드/메시지 포함)
> - 전면·보상형은 SDK를 통해 실제 광고 표시
> - 플레이스홀더 폴백 없음
>
> EN:
> - on load failure, emits `onAdFailedToLoad` (with real error code/message)
> - interstitial/rewarded show real ads via the SDK
> - no placeholder fallback

> KO: ⚠️ RN 이벤트 연결은 디버그로 확인하되, 실제 소재 노출 및 수익 집계는 반드시 **RELEASE 빌드 + 실기기**로 최종 확인하세요.
> EN: ⚠️ Verify RN event wiring in debug, but always finalize real creative serving and revenue tracking on a **RELEASE build + physical device**.

---

> KO: 추가 문의: nap_mx@nasmedia.co.kr · 공식 가이드: https://napmx.github.io/
> EN: Further questions: nap_mx@nasmedia.co.kr · Official guide: https://napmx.github.io/
