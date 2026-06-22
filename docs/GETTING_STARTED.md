# React Native Nap SSP — 시작 가이드 (Getting Started)

![npm](https://img.shields.io/badge/npm-react--native--nap--ssp-red)
![version](https://img.shields.io/badge/version-0.2.0-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-lightgrey)
![License](https://img.shields.io/badge/license-MIT-green)

> KO: **react-native-nap-ssp** — KT 나스미디어 Nap SSP(AdMixer) SDK를 React Native 앱에 연동하는 플러그인입니다.
> EN: **react-native-nap-ssp** is the React Native wrapper for KT Nasmedia's Nap SSP (AdMixer) SDK.

> KO: 이 릴리스(0.2.0)는 **네이티브 SDK 대규모 업그레이드**(Android v2.0.0 / iOS 2.3.7)와 **NaverAdManager·Teads 어댑터 추가**를 반영합니다.
> EN: This release (0.2.0) reflects a **major native SDK upgrade** (Android v2.0.0 / iOS 2.3.7) and adds the **NaverAdManager and Teads adapters**.

---

## 목차 (Table of Contents)

1. [사전 준비 (Prerequisites)](#1-사전-준비-prerequisites)
2. [설치 (Installation)](#2-설치-installation)
3. [Android 설정 (Android Setup)](#3-android-설정-android-setup)
4. [iOS 설정 (iOS Setup)](#4-ios-설정-ios-setup)
5. [SDK 초기화 (Initialization)](#5-sdk-초기화-initialization)
6. [광고 유형별 사용법 (Ad Formats)](#6-광고-유형별-사용법-ad-formats)
7. [디버그 vs 릴리즈 빌드 (Debug vs Release)](#7-디버그-vs-릴리즈-빌드-debug-vs-release)
8. [자주 발생하는 문제 (Troubleshooting)](#8-자주-발생하는-문제-troubleshooting)
9. [문의 및 지원 (Support)](#9-문의-및-지원-support)

---

## 1. 사전 준비 (Prerequisites)

> KO: 아래 환경 요구사항을 충족해야 합니다. 미디어 키와 광고 단위 ID는 나스미디어에서 발급받아야 합니다.
> EN: Your environment must meet the requirements below. The Media Key and Ad Unit IDs are issued by Nasmedia.

| 항목 (Item) | 요구 사항 (Requirement) |
| :--- | :--- |
| React Native | 0.72.0+ |
| Android minSdkVersion | 21+ |
| Android targetSdk | 34+ 권장 (recommended) |
| Android JDK | 17 |
| iOS | 14.0+ |
| Xcode | 15.3+ |
| **미디어 키 (Media Key)** | 나스미디어 발급 (issued by Nasmedia) |
| **광고 단위 ID (Ad Unit ID)** | 나스미디어 발급 (issued by Nasmedia) |

> KO: ⚠️ 미디어 키와 광고 단위 ID 없이는 광고가 로드되지 않습니다. 연동 전 반드시 발급받으세요. 문의: nap_mx@nasmedia.co.kr
> EN: ⚠️ Ads will not load without a Media Key and Ad Unit IDs. Obtain them before integrating. Contact: nap_mx@nasmedia.co.kr

> KO: ℹ️ 어댑터별 최소 API: Core/AdFit/Teads = 21, AdManager/Pangle/Unity/NaverAdManager = 23, AppLovin = 24.
> EN: ℹ️ Per-adapter minimum API: Core/AdFit/Teads = 21, AdManager/Pangle/Unity/NaverAdManager = 23, AppLovin = 24.

---

## 2. 설치 (Installation)

```bash
npm install react-native-nap-ssp
# 또는 / or
yarn add react-native-nap-ssp
```

---

## 3. Android 설정 (Android Setup)

### Step 1 — Maven 저장소 추가 (Add Maven repositories)

> KO: `android/build.gradle`(프로젝트 루트)에 사용할 어댑터에 맞는 저장소를 추가합니다. AdFit은 Kakao, Pangle은 ByteDance, Teads는 전용 저장소가 필요합니다.
> EN: Add the repositories required by the adapters you use to `android/build.gradle` (project root). AdFit needs Kakao, Pangle needs ByteDance, and Teads needs its own repositories.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // AdFit (Kakao)
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
        // Pangle (ByteDance)
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }
        // Teads
        maven { url 'https://sdk.teads.tv/android/repo' }
        maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }
    }
}
```

### Step 2 — 의존성 추가 (Add dependencies)

> KO: `android/app/build.gradle`에 코어와 사용할 어댑터(모두 2.0.0)를 추가합니다. 모든 어댑터를 추가할 필요는 없으며, 사용할 네트워크만 선택하세요.
> EN: Add the core and the adapters you use (all 2.0.0) to `android/app/build.gradle`. You do not need every adapter — include only the networks you use.

```gradle
dependencies {
    // Core (필수 / required)
    implementation 'io.github.nasmedia-tech:admixer-ssp:2.0.0'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.2.0'

    // 어댑터 (선택) / Adapters (optional)
    implementation 'io.github.nasmedia-tech:admixer-admanager:2.0.0'
    implementation 'io.github.nasmedia-tech:admixer-adfit:2.0.0'
    implementation 'io.github.nasmedia-tech:admixer-pangle:2.0.0'
    implementation 'io.github.nasmedia-tech:admixer-applovin:2.0.0'
    implementation 'io.github.nasmedia-tech:admixer-unity:2.0.0'
    implementation 'io.github.nasmedia-tech:admixer-naveradmanager:2.0.0' // 신규 / new
    implementation 'io.github.nasmedia-tech:admixer-teads:2.0.0'          // 신규 / new
}
```

> KO: ⚠️ AdManager 어댑터를 사용할 때 `play-services-ads`는 **25.2.0 상한**입니다(25.3.0 이상 비호환). 필요 시 강제로 고정하세요.
> EN: ⚠️ When using the AdManager adapter, `play-services-ads` is **capped at 25.2.0** (25.3.0+ is incompatible). Force the version if needed.

```gradle
configurations.all {
    resolutionStrategy {
        force 'com.google.android.gms:play-services-ads:25.2.0'
    }
}
```

### Step 3 — 어댑터 등록은 불필요 (No adapter registration needed)

> KO: ✅ **v2.0.0부터 `registerAdapter()` 호출이 더 이상 필요 없습니다.** `initialize()`가 Gradle 의존성에 포함된 어댑터를 자동으로 등록합니다. 기존 `AdMixer.registerAdapter(...)` 코드는 제거하세요.
> EN: ✅ **As of v2.0.0, `registerAdapter()` is no longer required.** `initialize()` auto-registers the adapters present in your Gradle dependencies. Remove any old `AdMixer.registerAdapter(...)` calls.

### Step 4 — 권한 추가 (Add permissions)

`android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### Step 5 — ProGuard 설정 (ProGuard rules, release builds)

> KO: 릴리즈 빌드에서 사용하는 어댑터에 해당하는 keep 규칙을 추가하세요.
> EN: Add keep rules for the adapters you use in release builds.

```proguard
-keep class com.nasmedia.admixerssp.** { *; }
# 사용하는 어댑터만 / only the adapters you use
-keep class com.nasmedia.admanager.**       { *; }
-keep class com.nasmedia.adfit.**           { *; }
-keep class com.nasmedia.pangle.**          { *; }
-keep class com.nasmedia.applovin.**        { *; }
-keep class com.nasmedia.unity.**           { *; }
-keep class com.nasmedia.naveradmanager.**  { *; }
-keep class com.nasmedia.teads.**           { *; }
```

> KO: 👉 상세 설정은 [Android Setup Guide](./ANDROID_SETUP.md)를 참고하세요.
> EN: 👉 See the [Android Setup Guide](./ANDROID_SETUP.md) for details.

---

## 4. iOS 설정 (iOS Setup)

> KO: iOS는 CocoaPods 또는 Swift Package Manager(SPM)로 연동합니다. 코어 버전은 **2.3.7**, 최소 iOS **14.0**, Xcode **15.3+**입니다. **iOS에는 Teads 어댑터가 없습니다.**
> EN: iOS integrates via CocoaPods or Swift Package Manager (SPM). The core version is **2.3.7**, minimum iOS **14.0**, Xcode **15.3+**. **There is no Teads adapter on iOS.**

### Step 1 — CocoaPods 의존성 추가 (Add CocoaPods dependencies)

`ios/Podfile`:

```ruby
platform :ios, '14.0'

target 'YourAppName' do
  use_frameworks!

  # Core (필수 / required)
  pod 'AdMixerMediation', '2.3.7'

  # 어댑터 (선택) / Adapters (optional)
  pod 'AdMixerMediationGAM'
  pod 'AdMixerMediationAdFit'
  pod 'AdMixerMediationPangle'
  pod 'AdMixerMediationAppLovin'
  pod 'AdMixerMediationUnityAds'
  pod 'AdMixerMediationNAM' # NaverAdManager, 신규 / new
end
```

```bash
cd ios && pod install
```

> KO: ⚠️ 반드시 `.xcworkspace`로 Xcode를 열어 빌드하세요.
> EN: ⚠️ Always open and build the project via the `.xcworkspace` file.

### Step 2 — Swift Package Manager(SPM) (선택 / optional)

> KO: 2.3.5+는 공식 SPM 릴리스로만 배포됩니다. 코어 바이너리는 다음 URL로 추가합니다.
> EN: 2.3.5+ is distributed only via official SPM releases. Add the core binary using the URL below.

- Package: `Nasmedia-Tech/iOS-SSP-Mediation-SPM`
- Version: `2.3.7`
- xcframework URL: `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip`
- checksum: `8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f`

### Step 3 — Info.plist 설정 (Configure Info.plist)

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

> KO: 👉 상세 설정은 [iOS Setup Guide](./IOS_SETUP.md)를 참고하세요.
> EN: 👉 See the [iOS Setup Guide](./IOS_SETUP.md) for details.

---

## 5. SDK 초기화 (Initialization)

> KO: 앱 시작 시 **한 번만** 호출합니다. `App.tsx` 최상위 `useEffect`에서 호출하는 것을 권장합니다. `mediations`로 어댑터를 활성화할 수 있으며, NaverAdManager/Teads(Android)도 여기서 켭니다.
> EN: Call this **once** at app startup, ideally in a top-level `useEffect` in `App.tsx`. Use `mediations` to enable adapters, including NaverAdManager and Teads (Android).

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: 'YOUR_MEDIA_KEY',
      adUnitIds: [
        'BANNER_AD_ID',
        'NATIVE_AD_ID',
        'INTERSTITIAL_AD_ID',
        'REWARDED_AD_ID',
      ],
      logLevel: 'info', // 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none'
      coppa: false,
      mediations: {
        adManager: { googleAppId: 'ca-app-pub-xxxxxxxx~yyyyyyyy' },
        adFit: true,
        naverAdManager: true, // v2.0.0+
        teads: true,          // v2.0.0+ (Android only)
      },
    });
  }, []);

  return (/* ... */);
}
```

> KO: ⚠️ **AdUnit ID 중복 사용 금지**: 하나의 AdUnit ID는 단 하나의 광고 객체에서만 사용해야 합니다. 동일 ID를 여러 컴포넌트에서 동시에 사용하면 광고가 정상 로드되지 않습니다.
> EN: ⚠️ **Do not reuse an Ad Unit ID**: each ID must be used by exactly one ad object. Using the same ID in multiple components simultaneously breaks loading.

---

## 6. 광고 유형별 사용법 (Ad Formats)

### 6.1 배너 광고 (Banner)

```tsx
import { BannerAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <BannerAd
      adUnitId="BANNER_AD_ID"
      size="BANNER_320x50"
      onAdLoaded={() => console.log('banner loaded')}
      onAdFailedToLoad={(error) => console.warn('banner failed', error)}
    />
  );
}
```

| 사이즈 (Size) | 크기 (Dimensions) | 비고 (Note) |
| :--- | :--- | :--- |
| `BANNER_320x50` | 320 × 50 | 기본 / default |
| `BANNER_320x100` | 320 × 100 | |
| `BANNER_300x250` | 300 × 250 | 중형 직사각형 / medium rectangle |
| `BANNER_320x480` | 320 × 480 | |
| `LARGE_BANNER` | 320 × 100 | |
| `MEDIUM_RECTANGLE` | 300 × 250 | |
| `SMART_BANNER` | 화면 너비 자동 / auto width | |
| `BANNER_WxH` | 임의 크기 / arbitrary | 서버 지정 사이즈 동적 지원 / dynamic server size |

> KO: 💡 `size`에 `'BANNER_360x230'`처럼 `BANNER_너비x높이` 형식 문자열을 그대로 전달하면, 서버에서 새 사이즈를 내려줘도 플러그인 업데이트 없이 처리됩니다. 어댑터 전용 사이즈는 해당 어댑터가 활성화된 경우에만 노출됩니다.
> EN: 💡 Passing a `BANNER_WxH` string (e.g. `'BANNER_360x230'`) lets new server-defined sizes work without a plugin update. Adapter-specific sizes only render when that adapter is enabled.

### 6.2 네이티브 광고 (Native)

```tsx
import { NativeAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <NativeAd
      adUnitId="NATIVE_AD_ID"
      style={{ width: '100%', height: 200 }}
      onAdLoaded={() => console.log('native loaded')}
      onAdFailedToLoad={(error) => console.warn('native failed', error)}
    />
  );
}
```

> KO: 💡 `style`에 반드시 `width`와 `height`를 명시해야 광고가 표시됩니다.
> EN: 💡 You must specify `width` and `height` in `style` for the ad to render.

### 6.3 인라인 비디오 광고 (Inline Video)

```tsx
import { VideoAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <VideoAd
      adUnitId="VIDEO_AD_ID"
      style={{ width: '100%', height: 250 }}
      onAdLoaded={() => console.log('video loaded')}
      onAdCompleted={() => console.log('video completed')}
      onAdSkipped={() => console.log('video skipped')}
    />
  );
}
```

### 6.4 전면 광고 (Interstitial)

> KO: `load()` 후 원하는 시점에 `show()`로 표시합니다. **v2부터 전면 광고는 Basic 전용입니다** — popup/countDown 타입과 관련 옵션(type, countDownTime, buttonLeftText 등)은 네이티브 SDK에서 제거되었습니다. iOS에서는 `closeButtonTouchAreaRatio`(닫기 버튼 터치 영역 비율, 0.2~1.0)만 옵션으로 남아 있습니다.
> EN: Call `load()`, then `show()` when ready. **As of v2, interstitials are Basic-only** — popup/countDown types and their options (type, countDownTime, buttonLeftText, etc.) were removed from the native SDKs. On iOS, only `closeButtonTouchAreaRatio` (close-button touch area ratio, 0.2–1.0) remains.

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

async function showInterstitial() {
  const interstitial = new InterstitialAd('INTERSTITIAL_AD_ID', {
    closeButtonTouchAreaRatio: 0.5, // iOS only, optional
  });

  interstitial.addAdEventListener('loaded', () => console.log('loaded'));
  interstitial.addAdEventListener('closed', () => console.log('closed'));
  interstitial.addAdEventListener('loadFailed', (error) => console.warn('failed', error));

  try {
    await interstitial.load();
    await interstitial.show();
  } catch (error) {
    console.warn('interstitial show failed', error);
  }
}
```

### 6.5 전면 동영상 광고 (Interstitial Video)

```tsx
import { InterstitialVideoAd } from 'react-native-nap-ssp';

async function showInterstitialVideo() {
  const video = new InterstitialVideoAd('INTERSTITIAL_VIDEO_AD_ID');

  video.addAdEventListener('loaded', () => console.log('loaded'));
  video.addAdEventListener('completed', () => console.log('completed'));
  video.addAdEventListener('skipped', () => console.log('skipped'));
  video.addAdEventListener('closed', () => console.log('closed'));

  try {
    await video.load();
    await video.show();
  } catch (error) {
    console.warn('interstitial video show failed', error);
  }
}
```

### 6.6 보상형 광고 (Rewarded)

```tsx
import { RewardedAd } from 'react-native-nap-ssp';

async function showRewarded() {
  const rewarded = new RewardedAd('REWARDED_AD_ID', {
    mute: false, // Android only, optional
  });

  rewarded.addAdEventListener('loaded', () => console.log('loaded'));
  rewarded.addAdEventListener('rewarded', (reward) => {
    // 여기서 보상 지급 / grant the reward here
    console.log('rewarded', reward.type, reward.amount);
  });
  rewarded.addAdEventListener('closed', () => console.log('closed'));
  rewarded.addAdEventListener('loadFailed', (error) => console.warn('failed', error));

  try {
    await rewarded.load();
    await rewarded.show();
  } catch (error) {
    console.warn('rewarded show failed', error);
  }
}
```

> KO: 💡 보안이 중요한 서비스에서는 클라이언트 콜백 대신 **S2S(Server-to-Server) 보상 콜백**을 권장합니다. 자세한 내용은 [Advanced Usage](./ADVANCED_USAGE.md)를 참조하세요.
> EN: 💡 For security-sensitive services, prefer **S2S (server-to-server) reward callbacks** over the client callback. See [Advanced Usage](./ADVANCED_USAGE.md).

---

## 7. 디버그 vs 릴리즈 빌드 (Debug vs Release)

> KO: DEBUG / RELEASE 빌드에 따라 광고 실패 처리 방식이 다릅니다.
> EN: Failure handling differs between DEBUG and RELEASE builds.

| 상황 (Situation) | DEBUG | RELEASE |
| :--- | :--- | :--- |
| SDK 로드 실패 / load failure | `onAdLoaded` (placeholder) | `onAdFailedToLoad` (real error) |
| 12초 무응답 / 12s timeout | `onAdLoaded` (timeout fallback) | 해당 없음 / N/A |
| 전면·보상형 `show()` | 플레이스홀더 즉시 성공 / placeholder success | 실제 광고 표시 / real ad |

> KO: 이벤트 payload의 `source` 필드가 `"placeholder"`, `"debug-no-fill"`, `"debug-sdk-timeout"`이면 플레이스홀더입니다. 실제 노출과 수익 집계는 반드시 **RELEASE 빌드 + 실기기**에서 검증하세요.
> EN: A `source` field of `"placeholder"`, `"debug-no-fill"`, or `"debug-sdk-timeout"` in the event payload indicates a placeholder. Verify real impressions and revenue only on a **RELEASE build + physical device**.

---

## 8. 자주 발생하는 문제 (Troubleshooting)

> KO: **광고가 전혀 로드되지 않아요** — `mediaKey`/`adUnitId`가 올바른지, 인터넷 연결이 되는지 확인하고 시뮬레이터 대신 실기기로 테스트하세요.
> EN: **Ads never load** — verify `mediaKey`/`adUnitId`, check connectivity, and test on a physical device instead of a simulator.

> KO: **Android: `NapSspXXX is not linked`** — Android Studio에서 Sync Project with Gradle Files를 실행하거나 `npx react-native run-android`를 재실행하세요.
> EN: **Android: `NapSspXXX is not linked`** — run "Sync Project with Gradle Files" in Android Studio, or re-run `npx react-native run-android`.

> KO: **Android: `Unsupported class file major version`** — JDK 17을 사용하도록 환경을 설정하세요.
> EN: **Android: `Unsupported class file major version`** — configure your environment to use JDK 17.

> KO: **Android: 릴리즈에서 광고가 안 보여요** — 사용하는 어댑터의 ProGuard keep 규칙을 확인하세요([Step 5](#step-5--proguard-설정-proguard-rules-release-builds)).
> EN: **Android: ads missing in release** — check the ProGuard keep rules for the adapters you use ([Step 5](#step-5--proguard-설정-proguard-rules-release-builds)).

> KO: **Android: 특정 네트워크 광고가 안 나와요** — 해당 어댑터 의존성을 `build.gradle`에 추가했는지, AdManager 사용 시 `play-services-ads`가 25.2.0인지 확인하세요.
> EN: **Android: a specific network has no fill** — confirm the adapter dependency is in `build.gradle`, and that `play-services-ads` is 25.2.0 when using AdManager.

> KO: **iOS: 빌드 후에도 광고가 안 나와요** — `.xcworkspace`로 빌드하고 있는지 확인하세요.
> EN: **iOS: no ads after build** — confirm you are building via `.xcworkspace`.

> KO: **iOS: ATT 팝업이 안 나와요** — `Info.plist`에 `NSUserTrackingUsageDescription`이 있는지 확인하세요.
> EN: **iOS: ATT prompt does not appear** — confirm `NSUserTrackingUsageDescription` is in `Info.plist`.

> KO: **배너/네이티브 광고가 안 보여요** — `style`에 `width`와 `height`를 명시했는지 확인하세요.
> EN: **Banner/Native not visible** — confirm `width` and `height` are set in `style`.

---

## 9. 문의 및 지원 (Support)

| 항목 (Item) | 내용 (Detail) |
| :--- | :--- |
| **기술 문의 / Tech support** | nap_mx@nasmedia.co.kr |
| **미디어 키·광고 ID 발급 / Key & ID issuance** | 나스미디어 파트너 운영팀 (Nasmedia partner ops) |
| **공식 가이드 / Official guide** | https://napmx.github.io/ |
| **플러그인 버전 / Plugin version** | 0.2.0 |
| **npm 패키지 / npm package** | `react-native-nap-ssp` |

---

> KO: 📚 더 자세한 내용은 아래 문서를 참고하세요. / EN: 📚 See the documents below for more detail.
>
> - [API Reference](./API_REFERENCE.md)
> - [Android Setup](./ANDROID_SETUP.md)
> - [iOS Setup](./IOS_SETUP.md)
> - [Mediation Guide](./MEDIATION_GUIDE.md)
> - [Advanced Usage](./ADVANCED_USAGE.md)
> - [Troubleshooting](./TROUBLESHOOTING.md)
> - [FAQ](./FAQ.md)
