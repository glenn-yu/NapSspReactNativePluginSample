# 마이그레이션 가이드 (Migration Guide)

> KO: `react-native-nap-ssp` 플러그인의 버전 업데이트 시 필요한 대응 방법을 안내합니다. 가장 최근의 메이저 업그레이드는 **0.1.x → 0.2.0** 입니다.
> EN: How to handle each `react-native-nap-ssp` plugin upgrade. The latest major upgrade is **0.1.x → 0.2.0**.

---

## v0.1.x ➡️ v0.2.0 (메이저 / Major)

> KO: 이번 릴리스는 네이티브 SDK 대규모 업그레이드입니다 — Android **AdMixer SSP v1.x → v2.0.0**, iOS **AdMixerMediation 2.3.x → 2.3.7**. NaverAdManager / Teads 어댑터가 추가되었습니다.
> EN: This release is a major native SDK upgrade — Android **AdMixer SSP v1.x → v2.0.0** and iOS **AdMixerMediation 2.3.x → 2.3.7**, plus new NaverAdManager / Teads adapters.

### ✅ 대부분의 앱은 JS 코드 변경이 필요 없습니다 (Most apps need NO JS change)

> KO: 이 플러그인은 위 네이티브의 클래스명 변경(`AdView`→`AMMBannerView` 등)·리스너 구조 변경을 **내부 reflection으로 흡수**합니다. 따라서 JS/앱 개발자는 대부분 코드 수정 없이 업그레이드됩니다. **단, 아래 "제거된 JS 옵션"은 반드시 제거해야 합니다.**
> EN: The plugin absorbs the native class renames (`AdView`→`AMMBannerView`, etc.) and listener-structure changes **via internal reflection**. So JS/app developers usually upgrade with no code changes. **However, you must remove the JS options listed under "Removed JS options" below.**

### 1. 플러그인 설치 (Install)

```bash
npm install react-native-nap-ssp@0.2.0
cd ios && pod install   # iOS (CocoaPods 사용 시 / if using CocoaPods)
```

### 2. ⚠️ 제거된 JS 옵션 (Removed JS options — Breaking)

> KO: v2부터 **전면 광고는 Basic 전용**입니다. 네이티브에서 popup/countDown 타입이 제거되어, 관련 JS 옵션도 삭제되었습니다.
> EN: As of v2, **interstitials are Basic-only.** popup/countDown types were removed natively, so the related JS options were removed too.

`InterstitialAdOptions`에서 제거됨 / Removed from `InterstitialAdOptions`:

| 제거된 옵션 (Removed) | 대응 (What to do) |
| :--- | :--- |
| `type` (`'popup'` / `'countdown'` 등) | KO: 제거 — 전면은 항상 Basic / EN: remove — interstitials are always Basic |
| `countDownTime` | KO: 제거 / EN: remove |
| `buttonLeftText` / `buttonRightText` | KO: 제거 / EN: remove |

```tsx
// ❌ Before (v0.1.x) — 더 이상 동작하지 않는 옵션(생성자 인자) / removed constructor options
const interstitial = new InterstitialAd('INTER_ID', {
  type: 'popup',
  countDownTime: 5,
  buttonLeftText: '닫기',
  buttonRightText: '보기',
});
await interstitial.load();
await interstitial.show();

// ✅ After (v0.2.0) — Basic 전용 / Basic-only
const interstitial = new InterstitialAd('INTER_ID');
await interstitial.load();
await interstitial.show();

// iOS 전용 옵션은 생성자에서 유지 / iOS-only option stays as a constructor option
const ios = new InterstitialAd('INTER_ID', { closeButtonTouchAreaRatio: 0.5 });
```

> ℹ️ KO: `closeButtonTouchAreaRatio`(iOS 전용)는 그대로 유지됩니다. Android는 서버 설정(`AdInfo.setCloseButtonBound`)을 따릅니다.
> ℹ️ EN: `closeButtonTouchAreaRatio` (iOS-only) is unchanged. Android follows the server setting (`AdInfo.setCloseButtonBound`).

### 3. 신규 JS 옵션 (New JS options)

| 추가된 옵션 (Added) | 설명 (Description) |
| :--- | :--- |
| `MediationConfig.naverAdManager?: boolean` | KO: NaverAdManager 어댑터 활성화 / EN: enable NaverAdManager adapter |
| `MediationConfig.teads?: boolean` | KO: Teads 어댑터 활성화 (Android 전용) / EN: enable Teads adapter (Android only) |
| `RewardedAdOptions.customParams?` | KO: S2S 콜백 커스텀 파라미터 / EN: S2S callback custom params |
| `RewardedAdOptions.mute?` | KO: 동영상 음소거 (Android 전용) / EN: mute video (Android only) |

### 4. 네이티브 설정 변경 (Native setup changes — 필수 / Required)

> KO: 앱의 네이티브 프로젝트(`android/`, `ios/`)에서 SDK 좌표를 v2로 갱신해야 합니다. 상세 절차는 아래 두 섹션과 [ANDROID_SETUP.md](./ANDROID_SETUP.md) / [IOS_SETUP.md](./IOS_SETUP.md)를 따르세요.
> EN: Update the SDK coordinates to v2 in your native projects (`android/`, `ios/`). Follow the two sections below and [ANDROID_SETUP.md](./ANDROID_SETUP.md) / [IOS_SETUP.md](./IOS_SETUP.md).

#### Android v1.x → v2.0.0 핵심 (Native Android summary)

- KO: 좌표 갱신 — Core·전체 어댑터 `io.github.nasmedia-tech:admixer-*` → **2.0.0** (이전 `1.0.x`, `_beta`/`_delta` 좌표는 모두 제거). / EN: bump all `io.github.nasmedia-tech:admixer-*` to **2.0.0** (remove every `1.0.x`, `_beta`/`_delta` coordinate).
- KO: `play-services-ads-identifier` → **18.2.0**. / EN: `play-services-ads-identifier` → **18.2.0**.
- KO: **`registerAdapter()` 수동 호출 제거** — `initialize()`가 의존성의 어댑터를 자동 등록. `MainApplication`의 등록 코드를 삭제하세요. / EN: **remove manual `registerAdapter()` calls** — `initialize()` auto-registers adapters from dependencies; delete the registration code in `MainApplication`.
- KO: 신규 어댑터 NaverAdManager / Teads 추가 가능. Teads는 Maven 저장소 2개 추가 필요. / EN: optional new NaverAdManager / Teads adapters; Teads needs two extra Maven repos.
- KO: AdManager의 `play-services-ads`는 **25.2.0 상한** 강제 권장(25.3.0+ 비호환). / EN: force-pin AdManager `play-services-ads` ≤ **25.2.0** (25.3.0+ incompatible).
- KO: ProGuard 규칙 — `-keep class com.nasmedia.admixerssp.** { *; }` + 사용 어댑터별 `-keep class com.nasmedia.<admanager|adfit|pangle|applovin|unity|naveradmanager|teads>.** { *; }`. / EN: ProGuard — keep core + each used adapter package.
- KO: 어댑터별 최소 Android API — Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24. / EN: per-adapter min API — Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24.

> ℹ️ KO: 클래스명 변경(`AdView`→`AMMBannerView`, `InterstitialAd`→`AMMInterstitial` 등), `AdListener`의 `interface`→`abstract class` 및 `onEventAd` → 이름 있는 콜백(`onAdDisplayed`/`onAdClicked`/`onAdClosed`/`onAdCompleted`/`onAdSkipped`/`onAdRewarded` + `onAdShowFailed`) 분리, 네이티브 View ID `nap_mx_` prefix·`setViewIds()` 제거 등은 모두 **플러그인 내부에서 처리**됩니다. RN 앱 개발자가 직접 다룰 필요는 없습니다.
> ℹ️ EN: The class renames (`AdView`→`AMMBannerView`, `InterstitialAd`→`AMMInterstitial`, etc.), `AdListener` becoming an `abstract class` with `onEventAd` split into named callbacks (`onAdDisplayed`/`onAdClicked`/`onAdClosed`/`onAdCompleted`/`onAdSkipped`/`onAdRewarded` + `onAdShowFailed`), and the native View-ID `nap_mx_` prefix / `setViewIds()` removal are **all handled inside the plugin**. RN app developers do not deal with them directly.

#### iOS 2.3.x → 2.3.7 핵심 (Native iOS summary)

- KO: `AdMixerMediation` Pod/SPM → **2.3.7** (이전 2.3.3). 최소 iOS **14.0**, Xcode 15.3+. / EN: bump `AdMixerMediation` Pod/SPM → **2.3.7** (was 2.3.3). Min iOS **14.0**, Xcode 15.3+.
- KO: 어댑터 Pod — `AdMixerMediationGAM` / `AdFit` / `Pangle` / `AppLovin` / `UnityAds` / **`NAM`(NaverAdManager, 신규)**. iOS에는 **Teads 없음**. / EN: adapter Pods — GAM / AdFit / Pangle / AppLovin / UnityAds / **NAM (NaverAdManager, new)**; **no Teads on iOS**.
- KO: 전면 popup/countDown 제거(Basic 전용), 뷰형(배너/네이티브/비디오) load API 추가. / EN: interstitial popup/countDown removed (Basic-only); view-type (banner/native/video) load APIs added.
- KO: SPM 사용 시 `ios/Package.swift`의 `binaryTarget`을 2.3.7로 갱신: / EN: For SPM, update the `binaryTarget` in `ios/Package.swift` to 2.3.7:

```swift
// AdMixerMediation 2.3.7 (binaryTarget)
.binaryTarget(
    name: "AdMixerMediation",
    url: "https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip",
    checksum: "8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f"
)
```

> ℹ️ KO: 2.3.5+는 공식 SPM 릴리스(`Nasmedia-Tech/iOS-SSP-*-SPM`)로만 배포됩니다. 레거시 `iOS-AdMixerDownload` 채널은 2.3.4까지만 제공됩니다.
> ℹ️ EN: 2.3.5+ is distributed only via the official SPM releases (`Nasmedia-Tech/iOS-SSP-*-SPM`). The legacy `iOS-AdMixerDownload` channel only goes up to 2.3.4.

### 5. 업그레이드 후 검증 (Post-upgrade checklist)

- [ ] KO: 빌드 성공 (Android/iOS) / EN: build succeeds (Android & iOS)
- [ ] KO: `MainApplication`에서 `registerAdapter()` 호출 모두 제거 / EN: removed all `registerAdapter()` calls in `MainApplication`
- [ ] KO: 제거된 전면 popup/countdown JS 옵션 삭제 / EN: removed the deprecated interstitial popup/countdown JS options
- [ ] KO: 각 포맷(배너·네이티브·인라인 비디오·전면·전면 동영상·리워드) 수신·노출 확인 / EN: verify each format loads & shows
- [ ] KO: (AdManager 사용 시) `play-services-ads` 25.2.0 고정, Google App ID 매니페스트/Info.plist 설정 / EN: (if using AdManager) pin `play-services-ads` 25.2.0, set Google App ID

---

## v0.1.3 ➡️ v0.1.4

> KO: API 변경 없음. npm 패키지에 가이드 문서가 추가된 것이 전부입니다.
> EN: No API changes. The only difference is bundled guide docs in the npm package.

---

## v0.1.2 ➡️ v0.1.3

### iOS SPM 체크섬 업데이트 (iOS SPM checksum)

> KO: SPM으로 직접 참조하는 경우 `Package.swift`의 AdMixerMediation 체크섬(2.3.3)을 갱신해야 했습니다. (현재는 0.2.0의 2.3.7로 다시 갱신 필요 — 위 0.2.0 섹션 참고.)
> EN: If referencing via SPM, the AdMixerMediation checksum (2.3.3) in `Package.swift` had to be updated. (Now re-bump to 2.3.7 for 0.2.0 — see the 0.2.0 section above.)

> KO: Android Maven 버전은 0.1.2와 동일. CocoaPods 사용자는 변경 불필요.
> EN: Android Maven versions unchanged from 0.1.2. CocoaPods users needed no change.

---

## v0.1.1 ➡️ v0.1.2

### InterstitialVideoAd API

> KO: `InterstitialVideoAd`에 `start()`가 추가되었습니다. 기존 `load()` 후 `show()` 방식도 유지되지만 신규 코드는 `start()` 권장.
> EN: `start()` was added to `InterstitialVideoAd`. The `load()` then `show()` flow still works, but new code should prefer `start()`.

```tsx
// AS-IS
await interVideo.load();
await interVideo.show();

// TO-BE (권장 / recommended)
await interVideo.start();
```

---

## v0.1.0 ➡️ v0.1.1

### 1. 네이티브 광고 연동 (NativeAd)

> KO: Placeholder 방식에서 실제 SDK 바인딩으로 전환. Android는 `res/layout`, iOS는 `.xib` 설정이 필요합니다. 상세는 [NATIVE_ASSETS_GUIDE.md](./NATIVE_ASSETS_GUIDE.md) 참고.
> EN: Switched from placeholder to real SDK binding. Android needs `res/layout`, iOS needs `.xib`. See [NATIVE_ASSETS_GUIDE.md](./NATIVE_ASSETS_GUIDE.md).

### 2. 초기화 옵션 추가 (mediations)

> KO: `NapSspAd.initialize`에 `mediations` 옵션이 추가되어 네트워크별 세부 설정을 전달할 수 있습니다.
> EN: `NapSspAd.initialize` gained a `mediations` option for per-network configuration.

---

## 문의 (Contact)

> KO: nap_mx@nasmedia.co.kr — 공식 가이드: https://napmx.github.io/
> EN: nap_mx@nasmedia.co.kr — Official guide: https://napmx.github.io/
