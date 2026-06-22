# React Native Nap SSP Plugin (v0.2.0)

> 🇰🇷 한국어와 🇺🇸 English 를 한 문서에 함께 제공합니다. / This document is bilingual (Korean + English).

KT Nasmedia **nap mx (AdMixer SSP) SDK** 를 React Native 에서 사용하기 위한 플러그인입니다.
A React Native plugin that wraps the KT Nasmedia **nap mx (AdMixer SSP) SDK**.

- 버전 / Version: `0.2.0`
- 지원 플랫폼 / Platforms: Android · iOS
- 제공 형태 / Surface: Native Module + Native View
- 지원 포맷 / Formats: 초기화·배너·전면·전면 동영상·리워드·네이티브·인라인 비디오 / init, banner, interstitial, interstitial-video, rewarded, native, inline video
- 미지원 / Not yet: Bizboard 전용 RN surface / dedicated Bizboard RN surface

> **v0.2.0 핵심 / Highlights**
> - Android 벤더 SDK **v1.x → v2.0.0** 마이그레이션 / migrated the Android vendor SDK from v1.x to **v2.0.0**.
> - iOS 벤더 SDK **2.3.3 → 2.3.7**.
> - **NaverAdManager·Teads** 미디에이션 추가(Teads 는 Android 전용) / added **NaverAdManager & Teads** mediation (Teads = Android only).
> - 전면 광고 **Basic 전용**(popup/countDown 제거), `registerAdapter()` 자동화 / interstitial is now **Basic-only** and adapters auto-register.
> - ⚠️ 대부분의 네이티브 변경은 플러그인이 내부에서 흡수하므로 **앱 코드 변경은 거의 없습니다**. / Most native changes are absorbed internally, so **app code rarely needs changes** — see [Migration Guide](./docs/MIGRATION_GUIDE.md).

---

## 📱 호환성 (Compatibility)

| 항목 / Item | 지원 범위 / Support |
| :--- | :--- |
| **React Native** | `>= 0.72.0` |
| **Android** | `minSdkVersion 21`, `targetSdkVersion 34+` (어댑터에 따라 23/24 필요 / some adapters require 23/24) |
| **iOS** | `iOS 14.0+`, Xcode 15.3+ |
| **Architecture** | Old Architecture 전용 / Old Architecture only (New Architecture 예정 / planned) |

---

## 🚀 시작하기 (Getting Started)

설치 / Install:

```bash
npm install react-native-nap-ssp
# 또는 / or
yarn add react-native-nap-ssp
```

순서 / Steps:
1. 패키지 설치 / install the package
2. Android·iOS 네이티브 설정 추가 / add the native setup below
3. `NapSspAd.initialize()` 호출 / call `NapSspAd.initialize()`
4. `BannerAd` 또는 `InterstitialAd` 부터 확인 / try `BannerAd` or `InterstitialAd` first

### 1) 초기화 / Initialize (앱 시작 시 1회 / once at app start)

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: '발급받은_MEDIA_KEY',           // issued media key
      adUnitIds: ['BANNER_ID', 'INTER_ID', 'REWARD_ID', 'NATIVE_ID', 'VIDEO_ID', 'INTER_VIDEO_ID'],
      logLevel: 'debug',
      // 선택: 사용할 미디에이션만 / optional: enable only the mediations you use
      mediations: {
        adFit: true,
        naverAdManager: true,  // v2.0.0+
        teads: true,           // v2.0.0+ (Android only)
      },
    });
  }, []);
  return null;
}
```

### 2) 배너 / Banner

```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd adUnitId="BANNER_ID" size="BANNER_320x50" />;
```

### 3) 전면 / Interstitial

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('INTER_ID'); // v2: Basic 전용 / Basic-only
await interstitial.load();
await interstitial.show();
```

---

## ⚙️ 네이티브 필수 설정 (Native Setup)

광고 SDK 동작을 위해 플랫폼별 설정이 필요합니다. / Platform-specific setup is required for the ad SDK to work.

### 🤖 Android

프로젝트 루트 `android/build.gradle` 의 저장소 / Repositories in root `android/build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://devrepo.kakao.com/nexus/content/groups/public/" } // AdFit
        maven { url "https://artifact.bytedance.com/repository/pangle/" }       // Pangle
        // Teads 사용 시 / when using Teads
        maven { url "https://sdk.teads.tv/android/repo" }
        maven { url "https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod" }
    }
}
```

`android/app/build.gradle` 의 `dependencies` / dependencies in `android/app/build.gradle`:

```gradle
dependencies {
    // (필수 / required) nap mx Core
    implementation 'io.github.nasmedia-tech:admixer-ssp:2.0.0'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.2.0'

    // (선택 / optional) 사용하는 미디에이션만 추가 / add only the mediations you use
    implementation 'io.github.nasmedia-tech:admixer-admanager:2.0.0'      // Google AdManager
    implementation 'io.github.nasmedia-tech:admixer-adfit:2.0.0'          // Kakao AdFit
    implementation 'io.github.nasmedia-tech:admixer-pangle:2.0.0'         // Pangle
    implementation 'io.github.nasmedia-tech:admixer-applovin:2.0.0'       // AppLovin
    implementation 'io.github.nasmedia-tech:admixer-unity:2.0.0'          // Unity Ads
    implementation 'io.github.nasmedia-tech:admixer-naveradmanager:2.0.0' // Naver Ad Manager (신규 / new)
    implementation 'io.github.nasmedia-tech:admixer-teads:2.0.0'          // Teads (신규 / new)
}
```

> ℹ️ **v2.0.0 변경 / changes**
> - `registerAdapter()` 수동 호출 불필요 — `initialize()` 시 자동 등록. / No manual `registerAdapter()`; adapters auto-register on `initialize()`.
> - Google AdManager 사용 시 `play-services-ads` 는 **25.2.0 상한** 권장(25.3.0+ 비호환). / Pin `play-services-ads` to **≤ 25.2.0** when using AdManager.
> - 어댑터별 최소 Android API: Core/AdFit/Teads = 21, AdManager/Pangle/Unity/NaverAdManager = 23, AppLovin = 24. / Per-adapter minimum Android API.
> 자세한 내용 / details: [Android Setup](./docs/ANDROID_SETUP.md) · [Mediation Guide](./docs/MEDIATION_GUIDE.md)

### 🍎 iOS

CocoaPods 와 SPM 둘 다 지원합니다. / Both CocoaPods and SPM are supported.

#### 옵션 A: CocoaPods

```ruby
# (필수 / required)
pod 'AdMixerMediation'             # 2.3.7 (버전 미고정 시 최신 / latest when unpinned)

# (선택 / optional)
pod 'AdMixerMediationGAM'          # Google AdManager
pod 'AdMixerMediationAdFit'        # Kakao AdFit
pod 'AdMixerMediationPangle'       # Pangle
pod 'AdMixerMediationAppLovin'     # AppLovin
pod 'AdMixerMediationUnityAds'     # Unity Ads
pod 'AdMixerMediationNAM'          # Naver Ad Manager (신규 / new)
```

그 후 / then: `cd ios && pod install`

#### 옵션 B: SPM (Swift Package Manager)

1. Xcode → `File` → `Add Packages...`
2. 로컬 패키지 추가 / add local package: `node_modules/react-native-nap-ssp/ios`
3. AdMixerMediation XCFramework **2.3.7** 가 `ios/Package.swift` 에 정의되어 자동 포함됩니다. / The AdMixerMediation XCFramework **2.3.7** is declared in `ios/Package.swift` and included automatically.

> ℹ️ iOS 에는 **Teads 어댑터가 없습니다.** / There is **no Teads adapter on iOS.** 자세히 / details: [iOS Setup](./docs/IOS_SETUP.md) · [SPM Guide](./docs/SPM_GUIDE.md)

---

## 💻 사용 가이드 (Usage)

광고는 **"1. 초기화 → 2. 표시"** 2단계입니다. / Ads are a two-step flow: **initialize → show**.

### 🖼️ 뷰형 광고 / View-based ads (배너·네이티브·비디오)

```tsx
import { BannerAd, NativeAd, VideoAd } from 'react-native-nap-ssp';

// 배너 / Banner
<BannerAd
  adUnitId="BANNER_ID"
  size="BANNER_320x50"            // BANNER_300x250, SMART_BANNER 등 / etc.
  onAdLoaded={() => console.log('banner loaded')}
  onAdFailedToLoad={(e) => console.log('banner failed:', e.message)}
  onAdClicked={() => console.log('banner clicked')}
/>

// 네이티브 / Native
<NativeAd adUnitId="NATIVE_ID" style={{ width: '100%', height: 250 }} />

// 인라인 비디오 / Inline video
<VideoAd
  adUnitId="VIDEO_ID"
  style={{ width: '100%', height: 200 }}
  onAdCompleted={() => console.log('video completed')}
  onAdSkipped={() => console.log('video skipped')}
/>
```

### 🎬 풀스크린 광고 / Full-screen ads (전면·전면 동영상·리워드)

```tsx
import { InterstitialAd, InterstitialVideoAd, RewardedAd } from 'react-native-nap-ssp';

// 전면 / Interstitial — v2: Basic 전용 / Basic-only
const inter = new InterstitialAd('INTER_ID');
inter.addAdEventListener('closed', () => console.log('interstitial closed'));
inter.addAdEventListener('clicked', () => console.log('interstitial clicked'));
await inter.load();
await inter.show();

// 전면 동영상 / Interstitial video
const interVideo = new InterstitialVideoAd('INTER_VIDEO_ID');
interVideo.addAdEventListener('completed', () => console.log('completed'));
await interVideo.load();
await interVideo.show();

// 리워드 / Rewarded
const reward = new RewardedAd('REWARD_ID', {
  customParams: { useid: 'user123' },
  mute: true, // Android 전용 / Android only
});
reward.addAdEventListener('rewarded', (item) => {
  console.log('rewarded', item); // 정확한 지급은 S2S 콜백 권장 / prefer S2S callback for real grants
});
await reward.load();
await reward.show();
```

> ℹ️ 전면 광고 popup/countDown 옵션은 v2 에서 제거되었습니다(Basic 전용). / Interstitial popup/countDown options were removed in v2 (Basic-only).

---

## 🧪 DEBUG 빌드 플레이스홀더 (DEBUG placeholder behavior)

DEBUG 빌드에서는 시뮬레이터/에뮬레이터의 no-fill·timeout·일부 미디에이션 콜백 누락 상황에서도 RN 이벤트 파이프라인을 검증하도록 플레이스홀더 성공 이벤트를 발행합니다. 실제 노출 검증은 RELEASE 빌드 + 실기기에서 진행하세요.
In DEBUG builds, placeholder success events are emitted so the RN event pipeline can be validated even on simulators/emulators (no-fill, timeout, missing mediation callbacks). Validate real impressions on RELEASE builds + real devices.

---

## ✅ 테스트와 검증 (Test & Verify)

```bash
npm run verify   # typecheck + build + smoke:test
```

- `typecheck`: TypeScript 타입 확인 / type check
- `build`: `lib/` 생성 / build output
- `smoke:test`: 공개 API 및 초기화 흐름 / public API + init flow

---

## 📚 문서 (Documentation)

상세 가이드는 `docs/` 디렉토리에 있으며 모든 문서가 한/영 2개 언어로 제공됩니다. / Detailed guides live in `docs/` and every guide is bilingual (KR + EN).

- **[Getting Started](./docs/GETTING_STARTED.md)** — 빠른 시작 / quick start
- **[API Reference](./docs/API_REFERENCE.md)** — 컴포넌트·클래스 명세 / component & class spec
- **[Android Setup](./docs/ANDROID_SETUP.md)** · **[iOS Setup](./docs/IOS_SETUP.md)**
- **[Mediation Guide](./docs/MEDIATION_GUIDE.md)** — 네트워크별 설정 / per-network setup
- **[SPM Guide](./docs/SPM_GUIDE.md)** · **[Version Matrix](./docs/VERSION_MATRIX.md)**
- **[Migration Guide](./docs/MIGRATION_GUIDE.md)** — 0.1.x → 0.2.0 / v2 업그레이드
- **[Privacy & Compliance](./docs/PRIVACY_GUIDE.md)** — COPPA·ATT·AD_ID·GDPR/CCPA
- **[Native Assets](./docs/NATIVE_ASSETS_GUIDE.md)** · **[Expo](./docs/EXPO_GUIDE.md)** · **[Advanced Usage](./docs/ADVANCED_USAGE.md)**
- **[Troubleshooting](./docs/TROUBLESHOOTING.md)** · **[FAQ](./docs/FAQ.md)** · **[Glossary](./docs/GLOSSARY.md)** · **[Roadmap](./docs/ROADMAP.md)**

공식 가이드 / Official guide: https://napmx.github.io/

---

## ❓ 자주 묻는 질문 (FAQ)

- **Q. `NapSsp XXX is not linked` 에러 / error**
  A. 네이티브 빌드를 다시 하세요. Android: `npx react-native run-android` 재실행. iOS: `cd ios && pod install`.
  Rebuild natively — re-run `run-android`, and run `pod install` for iOS.
- **Q. 광고 로드 실패 / Ad fails to load**
  A. `mediaKey`·`adUnitId` 값과 인터넷 연결을 확인하세요. / Verify `mediaKey`/`adUnitId` and network connectivity.
- **Q. iOS 시뮬레이터에서 안 나옴 / Nothing on iOS simulator**
  A. 일부 네트워크는 실기기에서만 로드됩니다. / Some networks only fill on real devices.
- **Q. 이벤트 이름 / Event names**
  A. JS 클래스는 `addAdEventListener('loaded'|'loadFailed'|'opened'|'closed'|'clicked'|'impression'|'rewarded'|'completed'|'skipped')`, 컴포넌트 props 는 `onAdLoaded` 처럼 `on` 접두사. / Classes use normalized event names; component props use the `on` prefix.

---

> **문의 / Contact:** nap_mx@nasmedia.co.kr (나스미디어 nap mx 운영팀 / nap mx operations team)
