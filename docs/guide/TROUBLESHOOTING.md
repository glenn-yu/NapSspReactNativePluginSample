# Troubleshooting

> KO: `react-native-nap-ssp` 사용 중 발생할 수 있는 주요 문제와 해결 방법입니다. (플러그인 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7 기준)
> EN: Common issues and fixes when using `react-native-nap-ssp`. (Plugin 0.2.0 / Android admixer-ssp 2.0.0 / iOS AdMixerMediation 2.3.7)

---

## 1. 설치 및 빌드 이슈 (Installation & Build Issues)

### `NapSspXXX is not linked`

> KO: **원인** — 네이티브 모듈이 현재 앱 바이너리에 포함되어 있지 않습니다.
> EN: **Cause** — The native module is not included in the current app binary.

> KO: **해결**
> - **Android**: Android Studio에서 `Sync Project with Gradle Files`를 실행하거나 `npx react-native run-android`를 다시 실행하세요.
> - **iOS**: `cd ios && pod install`로 CocoaPods 의존성을 갱신한 뒤 다시 빌드하세요.
>
> EN: **Fix**
> - **Android**: Run `Sync Project with Gradle Files` in Android Studio, or re-run `npx react-native run-android`.
> - **iOS**: Run `cd ios && pod install` to refresh CocoaPods dependencies, then rebuild.

### `Unsupported class file major version 69` (Android)

> KO: **원인** — JDK 버전(예: JDK 25)이 현재 Gradle 버전과 호환되지 않습니다.
> EN: **Cause** — Your JDK (e.g. JDK 25) is incompatible with the current Gradle version.

> KO: **해결** — **JDK 17**을 사용하도록 프로젝트 설정을 변경하세요.
> EN: **Fix** — Configure the project to use **JDK 17**.

---

## 2. v2.0.0 업그레이드 관련 이슈 (v2.0.0 Upgrade Issues)

> KO: Android admixer-ssp가 **2.0.0**으로 마이그레이션되면서 네이티브 측에 Breaking 변경이 있었습니다. 이 RN 플러그인이 변경을 **내부적으로 흡수**하므로 JS 사용자는 대부분 코드 변경 없이 업그레이드됩니다. 다만 아래 항목은 직접 영향을 줄 수 있습니다.
> EN: The Android admixer-ssp migration to **2.0.0** introduced native breaking changes. This RN plugin **absorbs them internally**, so JS users mostly upgrade with no code changes. The items below, however, can still affect you directly.

### `registerAdapter` 더 이상 필요 없음 (No more `registerAdapter`)

> KO: v2.0.0부터 `initialize()`가 Gradle 의존성에 추가된 어댑터를 자동 등록합니다. 수동 `registerAdapter()` 호출 코드가 남아 있다면 제거하세요. 미디에이션 사용 여부는 `MediationConfig`(JS)와 Gradle 의존성으로만 제어됩니다.
> EN: From v2.0.0, `initialize()` auto-registers any adapter present in your Gradle dependencies. Remove any leftover manual `registerAdapter()` calls. Mediation is controlled only by `MediationConfig` (JS) and your Gradle dependencies.

### 전면 광고가 Basic 형태로만 표시됨 (Interstitial shows in Basic style only)

> KO: v2.0.0부터 전면 광고는 **Basic 전용**입니다. popup / countDown 형식 및 닫기 버튼 텍스트·카운트다운 시간·배경 알파 옵션은 제거되었습니다. 이전 `type`/`countDownTime`/`buttonLeftText`/`buttonRightText` 옵션을 전달하던 코드는 동작하지 않으니 제거하세요. (iOS는 `closeButtonTouchAreaRatio?`만 유지)
> EN: From v2.0.0 interstitials are **Basic-only**. The popup / countDown styles, close-button text, countdown time, and background-alpha options were removed. Code passing the old `type`/`countDownTime`/`buttonLeftText`/`buttonRightText` options no longer works — remove it. (iOS keeps only `closeButtonTouchAreaRatio?`.)

---

## 3. 디버그 빌드에서의 플레이스홀더 동작 (Placeholder Behavior in Debug Builds) — 중요 / IMPORTANT

> KO: **DEBUG 빌드**에서는 SDK 광고 로드 실패 시 실제 실패 이벤트 대신 **플레이스홀더 성공 이벤트**를 발행합니다.
> EN: In **DEBUG builds**, when an SDK ad fails to load the plugin emits a **placeholder success event** instead of a real failure event.

| 상황 / Situation | DEBUG | RELEASE |
| :--- | :--- | :--- |
| SDK 정상 응답 / SDK responds normally | 실제 광고 노출 / real ad | 실제 광고 노출 / real ad |
| No Fill | `onAdLoaded` (placeholder) | `onAdFailedToLoad` |
| 로드 오류 / load error | `onAdLoaded` (placeholder) | `onAdFailedToLoad` |
| 응답 없음 12초 / no response (12s) | `onAdLoaded` (timeout fallback) | N/A |
| 전면·보상형 `show()` (Android) | 즉시 성공 처리 / instant success | 실제 광고 표시 / real ad |
| 전면 `show()` 후 (iOS) | `onAdOpened`·`onAdImpression` 즉시 / instant | SDK 콜백 대기 / awaits SDK callback |

> KO: **의도** — 시뮬레이터에 SDK가 없거나 광고 물량이 없어도 RN 이벤트 파이프라인 자체를 검증할 수 있도록 하기 위함입니다.
> EN: **Intent** — lets you verify the RN event pipeline even when the SDK is absent or there is no ad inventory in the simulator.

> KO: **주의** — 디버그에서 `onAdLoaded`가 발행되어도 실제 광고 소재가 표시되지 않을 수 있습니다. 이벤트 payload의 `source` 필드를 확인하세요.
> EN: **Note** — even when `onAdLoaded` fires in debug, no real creative may be shown. Check the `source` field in the event payload.
>
> - `source: "placeholder"` / `"sdk-unavailable"` / `"debug-no-fill"` / `"debug-sdk-timeout"` → 플레이스홀더 / placeholder
> - `source` 없음 / absent → 실제 SDK 광고 / real SDK ad

> KO: **실제 광고 동작 검증은 반드시 RELEASE 빌드 + 실기기에서 하세요.**
> EN: **Always verify real ad behavior with a RELEASE build on a physical device.**

---

## 4. 광고 로드 실패 (Failed to Load)

### `mediaKey` 또는 `adUnitId` 오류 (Invalid `mediaKey` / `adUnitId`)

> KO: **원인** — 유효하지 않거나 등록되지 않은 키를 사용 중입니다. **해결** — 나스미디어 파트너 사이트에서 발급받은 실제 미디어 키와 광고 단위 ID를 다시 확인하세요.
> EN: **Cause** — using an invalid or unregistered key. **Fix** — re-check the media key and ad unit IDs issued from the Nasmedia partner site.

### 네트워크 연결 문제 (Network connectivity)

> KO: **원인** — 광고 요청은 실제 서버와 통신해야 합니다. **해결** — 기기/에뮬레이터의 인터넷 연결을 확인하세요. 비행기 모드나 오프라인에서는 광고가 로드되지 않습니다.
> EN: **Cause** — ad requests must reach the real server. **Fix** — check device/emulator connectivity. Ads will not load in airplane mode or offline.

### 실기기 vs 시뮬레이터 (Physical device vs simulator)

> KO: **원인** — 일부 미디에이션 SDK(Pangle, Teads 등)는 가상 환경에서 노출이 제한될 수 있습니다. **해결** — 정확한 검증은 반드시 **실기기**에서 하세요. 디버그 빌드는 플레이스홀더 동작이 적용되므로([섹션 3](#3-디버그-빌드에서의-플레이스홀더-동작-placeholder-behavior-in-debug-builds--중요--important) 참조), RELEASE 빌드 기준으로 최종 검증하세요.
> EN: **Cause** — some mediation SDKs (Pangle, Teads, etc.) limit serving in virtual environments. **Fix** — verify on a **physical device**. Debug builds use placeholder behavior (see [section 3](#3-디버그-빌드에서의-플레이스홀더-동작-placeholder-behavior-in-debug-builds--중요--important)); finalize on RELEASE builds.

---

## 5. iOS 특이사항 (iOS-Specific)

### ATT 권한 팝업 미노출 (ATT prompt not shown)

> KO: **원인** — `Info.plist`에 `NSUserTrackingUsageDescription`이 누락되었거나 추적 권한이 비활성화됨. **해결** — `Info.plist`를 확인하고, 시뮬레이터는 `설정 > 개인정보 보호 > 추적`에서 '앱이 추적을 요청하도록 허용'이 켜져 있는지 확인하세요.
> EN: **Cause** — `NSUserTrackingUsageDescription` missing from `Info.plist`, or tracking disabled. **Fix** — verify `Info.plist`; on simulator, ensure `Settings > Privacy > Tracking > Allow Apps to Request to Track` is enabled.

### 최소 버전 / Pod 갱신 (Minimum version / Pod refresh)

> KO: iOS는 **최소 14.0**, Xcode **15.3+**가 필요합니다. AdMixerMediation 2.3.7로 올린 뒤 빌드 오류가 나면 `pod deintegrate && pod install`로 캐시를 정리하세요. SPM을 쓰는 경우 `ios/Package.swift`의 binaryTarget이 **2.3.7**을 가리키는지 확인하세요. (2.3.5+는 공식 SPM 릴리스로만 배포되며, 레거시 iOS-AdMixerDownload 채널은 2.3.4까지만 제공됩니다.)
> EN: iOS requires **min 14.0** and Xcode **15.3+**. After moving to AdMixerMediation 2.3.7, if you hit build errors run `pod deintegrate && pod install` to clear caches. If using SPM, ensure the `ios/Package.swift` binaryTarget points to **2.3.7**. (2.3.5+ ships only via the official SPM releases; the legacy iOS-AdMixerDownload channel stops at 2.3.4.)

---

## 6. Android 특이사항 (Android-Specific)

### 릴리즈 빌드에서 광고 미노출 — ProGuard/R8 (Ads missing in release — ProGuard/R8)

> KO: **원인** — ProGuard/R8이 SDK 클래스를 제거(난독화)했습니다. **해결** — `proguard-rules.pro`에 코어 규칙과 **사용하는 어댑터별** keep 규칙을 추가하세요.
> EN: **Cause** — ProGuard/R8 stripped/obfuscated SDK classes. **Fix** — add the core rule plus a keep rule **per adapter you use** to `proguard-rules.pro`.

```
-keep class com.nasmedia.admixerssp.** { *; }

# 사용하는 어댑터만 / only the adapters you use
-keep class com.nasmedia.admanager.** { *; }
-keep class com.nasmedia.adfit.** { *; }
-keep class com.nasmedia.pangle.** { *; }
-keep class com.nasmedia.applovin.** { *; }
-keep class com.nasmedia.unity.** { *; }
-keep class com.nasmedia.naveradmanager.** { *; }
-keep class com.nasmedia.teads.** { *; }
```

### `Duplicate class` 오류 (Duplicate class)

> KO: **원인** — 다른 광고 SDK(예: AdMob)와 라이브러리 버전 충돌. **해결** — `build.gradle`의 `resolutionStrategy`로 버전을 고정하세요. 특히 **AdManager(GAM) 어댑터**를 쓰는 경우 `play-services-ads`는 **25.2.0 상한**으로 강제하세요. 25.3.0+ 는 비호환입니다.
> EN: **Cause** — version clash with another ad SDK (e.g. AdMob). **Fix** — pin versions via `resolutionStrategy` in `build.gradle`. In particular, when using the **AdManager (GAM) adapter**, force `play-services-ads` to a **25.2.0 ceiling**; 25.3.0+ is incompatible.

```gradle
configurations.all {
    resolutionStrategy {
        force 'com.google.android.gms:play-services-ads:25.2.0'
    }
}
```

### 어댑터별 최소 Android API (Per-adapter minimum Android API)

> KO: 플러그인 minSdkVersion은 **21**이지만 어댑터마다 요구 API가 다릅니다. 사용하는 어댑터가 앱 minSdk보다 높은 API를 요구하면 광고가 로드되지 않거나 머지(manifest merge) 오류가 납니다.
> EN: The plugin minSdkVersion is **21**, but each adapter has its own requirement. If an adapter you use needs a higher API than your app minSdk, ads won't load or you'll hit manifest-merge errors.

| 최소 API / Min API | 어댑터 / Adapters |
| :--- | :--- |
| 21 | Core, AdFit, Teads |
| 23 | AdManager, Pangle, Unity, NaverAdManager |
| 24 | AppLovin |

### Teads Maven 저장소 누락 (Missing Teads Maven repos)

> KO: **원인** — Teads 어댑터를 추가했지만 전용 Maven 저장소가 없어 의존성 해석에 실패합니다. **해결** — 루트 또는 settings `repositories`에 Teads 저장소 두 곳을 추가하세요. (다른 어댑터의 저장소: AdFit = devrepo.kakao.com, Pangle = artifact.bytedance.com)
> EN: **Cause** — the Teads adapter was added but its dedicated Maven repos are missing, so resolution fails. **Fix** — add the two Teads repos to your root/settings `repositories`. (Other adapter repos: AdFit = devrepo.kakao.com, Pangle = artifact.bytedance.com.)

```gradle
repositories {
    google()
    mavenCentral()
    maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }      // AdFit
    maven { url 'https://artifact.bytedance.com/repository/pangle/' }           // Pangle
    maven { url 'https://sdk.teads.tv/android/repo' }                           // Teads
    maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }    // Teads
}
```

---

> KO: 위 항목으로 해결되지 않으면 nap_mx@nasmedia.co.kr 로 문의하거나 공식 가이드(https://napmx.github.io/)를 참고하세요.
> EN: If none of the above resolves your issue, contact nap_mx@nasmedia.co.kr or see the official guide at https://napmx.github.io/.
