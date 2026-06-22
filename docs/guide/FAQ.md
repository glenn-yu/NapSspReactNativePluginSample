# FAQ (자주 묻는 질문 / Frequently Asked Questions)

> KO: `react-native-nap-ssp` 사용자들이 자주 겪는 상황에 대한 답변입니다. (플러그인 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7)
> EN: Answers to common situations for `react-native-nap-ssp` users. (Plugin 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7)

---

## 1. 초기화 및 설정 (Initialization & Setup)

### Q. `NapSspAd.initialize`는 언제 호출하나요? / When should I call `NapSspAd.initialize`?

> KO: **A.** 앱 라이프사이클 중 가장 빠른 시점(`App.tsx`의 최상위 `useEffect` 또는 `index.js`)에서 한 번만 호출하세요.
> EN: **A.** Call it once at the earliest point in the app lifecycle (a top-level `useEffect` in `App.tsx`, or `index.js`).

```tsx
NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['...'],
  mediations: { naverAdManager: true, teads: true }, // 사용하는 것만 / only what you use
});
```

### Q. 미디에이션 어댑터를 따로 등록(registerAdapter)해야 하나요? / Do I need to call registerAdapter for mediation?

> KO: **A.** 아니요. v2.0.0부터 `registerAdapter()`는 **제거**되었습니다. Gradle/Pod 의존성에 어댑터를 추가하고 `initialize()`의 `mediations` 설정만 켜면 자동 등록됩니다. `MediationConfig`에는 기존 어댑터 외에 `naverAdManager?`, `teads?`가 추가되었습니다. (Teads는 **Android 전용**, iOS에는 없습니다.)
> EN: **A.** No. `registerAdapter()` was **removed** in v2.0.0. Just add the adapter to your Gradle/Pod dependencies and enable it via the `mediations` config of `initialize()` — it is registered automatically. `MediationConfig` now also exposes `naverAdManager?` and `teads?` in addition to the existing adapters. (Teads is **Android-only**; it does not exist on iOS.)

### Q. 미디어 키와 광고 ID는 어디서 발급받나요? / Where do I get the media key and ad IDs?

> KO: **A.** 나스미디어 파트너 센터(또는 담당 운영팀)를 통해 발급받을 수 있습니다.
> EN: **A.** Issued via the Nasmedia partner center (or your account manager).

---

## 2. 빌드 및 런타임 오류 (Build & Runtime Errors)

### Q. Android에서 `Duplicate class` 오류가 납니다. / I get a `Duplicate class` error on Android.

> KO: **A.** 다른 광고 SDK(예: AdMob)와 버전 충돌입니다. `build.gradle`의 `resolutionStrategy`로 버전을 고정하세요. **AdManager(GAM) 어댑터**를 쓰면 `play-services-ads`를 **25.2.0 상한**으로 강제하세요(25.3.0+ 비호환). 자세한 내용은 [Troubleshooting](./TROUBLESHOOTING.md) 참고.
> EN: **A.** A version clash with another ad SDK (e.g. AdMob). Pin versions via `resolutionStrategy` in `build.gradle`. If you use the **AdManager (GAM) adapter**, force `play-services-ads` to a **25.2.0 ceiling** (25.3.0+ is incompatible). See [Troubleshooting](./TROUBLESHOOTING.md).

### Q. Teads 광고를 추가했더니 Gradle 의존성 해석이 실패합니다. / Teads dependency fails to resolve in Gradle.

> KO: **A.** Teads는 전용 Maven 저장소 두 곳(`https://sdk.teads.tv/android/repo`, `https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod`)이 필요합니다. `repositories`에 추가하세요. Teads 어댑터 자체의 최소 Android API는 21입니다.
> EN: **A.** Teads needs two dedicated Maven repos (`https://sdk.teads.tv/android/repo` and `https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod`). Add them to `repositories`. The Teads adapter itself requires min Android API 21.

### Q. iOS 빌드에서 `AdMixerMediation`을 찾을 수 없다고 나옵니다. / Xcode can't find `AdMixerMediation` on iOS.

> KO: **A.** `pod install`이 정상 완료됐는지 확인하고, `.xcodeproj`가 아닌 **`.xcworkspace`**를 열어 빌드하세요. 2.3.7로 올린 직후라면 `pod deintegrate && pod install`로 캐시를 정리하세요. SPM 사용 시 `ios/Package.swift`의 binaryTarget이 2.3.7인지 확인하세요.
> EN: **A.** Confirm `pod install` finished, and build from the **`.xcworkspace`** (not `.xcodeproj`). Right after moving to 2.3.7, run `pod deintegrate && pod install` to clear caches. If using SPM, verify the `ios/Package.swift` binaryTarget is 2.3.7.

---

## 3. 광고 표시 관련 (Ad Display)

### Q. 광고는 로드됐는데 화면에 안 보입니다. (배너/네이티브) / The ad loaded but isn't visible. (Banner/Native)

> KO: **A.** 배너·네이티브 컴포넌트의 `style`에 `width`와 `height`가 명시되어 있는지 확인하세요. 특히 네이티브 광고는 콘텐츠 양에 따라 충분한 높이가 필요합니다.
> EN: **A.** Ensure the Banner/Native component's `style` sets an explicit `width` and `height`. Native ads in particular need enough height for their content.

### Q. 전면 광고에서 popup/카운트다운 닫기 버튼을 커스터마이징하고 싶어요. / I want to customize the popup/countdown close button on interstitials.

> KO: **A.** v2.0.0부터 전면 광고는 **Basic 전용**입니다. popup·countDown 형식과 닫기 버튼 텍스트·카운트다운 시간 옵션은 **제거**되었습니다. 더 이상 커스터마이징할 수 없으며 우측 상단 X 버튼만 제공됩니다. (iOS는 `closeButtonTouchAreaRatio?`로 닫기 버튼 터치 영역만 조정 가능)
> EN: **A.** From v2.0.0 interstitials are **Basic-only**. The popup/countDown styles and the close-button text / countdown-time options were **removed** and can no longer be customized — only the top-right X button is provided. (On iOS you can adjust just the close-button touch area via `closeButtonTouchAreaRatio?`.)

### Q. 특정 미디에이션 광고만 노출되지 않습니다. / Only one mediation network never serves.

> KO: **A.** 해당 네트워크(예: Pangle, Teads, NaverAdManager)의 별도 설정(키 입력, SDK 링킹, Maven 저장소, 최소 API)이 누락되지 않았는지 [Mediation Guide](./MEDIATION_GUIDE.md)에서 확인하세요.
> EN: **A.** Check that the network's extra setup (key entry, SDK linking, Maven repos, minimum API) is complete — see the [Mediation Guide](./MEDIATION_GUIDE.md). This applies to Pangle, Teads, NaverAdManager, etc.

### Q. 시뮬레이터/디버그 빌드에서 `onAdLoaded`는 오는데 실제 소재가 안 보입니다. / In simulator/debug, `onAdLoaded` fires but no real creative shows.

> KO: **A.** 정상 동작입니다. **DEBUG 빌드**에서는 SDK 광고 로드 실패 시 `onAdFailedToLoad` 대신 플레이스홀더 `onAdLoaded`를 발행해, 시뮬레이터에서도 RN 이벤트 파이프라인을 테스트할 수 있게 합니다. payload의 `source` 필드로 구분하세요.
> EN: **A.** This is expected. In **DEBUG builds**, on load failure the plugin emits a placeholder `onAdLoaded` instead of `onAdFailedToLoad`, so you can test the RN event pipeline even in a simulator. Distinguish via the `source` field.
>
> - `source: "placeholder"` / `"debug-no-fill"` / `"debug-sdk-timeout"` → 플레이스홀더 / placeholder (debug only)
> - `source` 없음 / absent → 실제 광고 / real ad

> KO: 실제 소재 노출은 **RELEASE 빌드 + 실기기**에서 검증하세요.
> EN: Verify real creatives on a **RELEASE build + physical device**.

### Q. 디버그에서 전면/보상형 `show()` 후 이벤트가 즉시 발행됩니다. / In debug, events fire instantly after interstitial/rewarded `show()`.

> KO: **A.** 정상 동작입니다. 시뮬레이터에서 일부 미디에이션 SDK가 presentation 콜백을 전달하지 않는 문제를 보완하기 위해, DEBUG 빌드에서는 `show()` 직후 `onAdOpened` / `onAdImpression`를 즉시 발행합니다. RELEASE 빌드에서는 SDK 콜백으로 정상 처리됩니다.
> EN: **A.** Expected. To work around some mediation SDKs not delivering presentation callbacks in the simulator, DEBUG builds emit `onAdOpened` / `onAdImpression` immediately after `show()`. RELEASE builds process these via real SDK callbacks.

---

## 4. 기타 (Other)

### Q. v0.1.x에서 0.2.0으로 올릴 때 JS 코드를 바꿔야 하나요? / Do I need to change JS code upgrading from v0.1.x to 0.2.0?

> KO: **A.** 대부분 필요 없습니다. Android admixer-ssp 2.0.0 / iOS 2.3.7의 네이티브 Breaking 변경을 플러그인이 내부 reflection으로 흡수합니다. 단, **전면 광고 popup/countdown 옵션**을 쓰던 코드는 제거해야 하고(Basic 전용), 수동 `registerAdapter()` 호출이 있다면 삭제하세요. 신규로 `naverAdManager` / `teads` 미디에이션을 켤 수 있습니다.
> EN: **A.** Usually not. The plugin absorbs the Android admixer-ssp 2.0.0 / iOS 2.3.7 native breaking changes via internal reflection. However, remove any code using the **interstitial popup/countdown options** (now Basic-only) and any manual `registerAdapter()` calls. You can additionally enable the new `naverAdManager` / `teads` mediations.

### Q. 최소 플랫폼 버전은 무엇인가요? / What are the minimum platform versions?

> KO: **A.** Android는 minSdkVersion **21**(어댑터별 최소 API는 다름 — AdManager/Pangle/Unity/NaverAdManager 23, AppLovin 24), iOS는 **14.0** + Xcode **15.3+**입니다.
> EN: **A.** Android minSdkVersion **21** (per-adapter minimums differ — AdManager/Pangle/Unity/NaverAdManager 23, AppLovin 24); iOS **14.0** with Xcode **15.3+**.

### Q. New Architecture(Fabric/TurboModule)를 지원하나요? / Is New Architecture (Fabric/TurboModule) supported?

> KO: **A.** 현재는 Old Architecture 전용입니다. New Architecture 지원은 로드맵에 포함되어 있습니다.
> EN: **A.** Currently Old Architecture only. New Architecture support is on the roadmap.

---

> KO: 추가 문의: nap_mx@nasmedia.co.kr · 공식 가이드: https://napmx.github.io/
> EN: Further questions: nap_mx@nasmedia.co.kr · Official guide: https://napmx.github.io/
