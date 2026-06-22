# react-native-nap-ssp

> **KT 나스미디어 nap mx (AdMixer SSP) SDK** × **React Native** 공식 플러그인 — Android / iOS 통합 광고 연동 가이드
> Official **React Native** plugin for the **KT Nasmedia nap mx (AdMixer SSP) SDK** — unified Android / iOS ad integration guide.

![npm](https://img.shields.io/badge/npm-react--native--nap--ssp-red)
![version](https://img.shields.io/badge/version-0.2.0-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-lightgrey)
![RN](https://img.shields.io/badge/React%20Native-0.72%2B-61DAFB)
![License](https://img.shields.io/badge/license-MIT-green)

> 🇰🇷 한국어 + 🇺🇸 English. 모든 가이드는 한 파일에 두 언어를 함께 제공합니다. / Every guide is bilingual in a single file.

---

## 개요 (Overview)

React Native 앱에서 JavaScript 몇 줄로 nap mx 광고를 요청하면, 네이티브 레이어(Android / iOS)가 실제 광고를 렌더링하는 **플러그인 패턴** 구현체입니다.
Request nap mx ads from a few lines of JavaScript; the native layer (Android / iOS) renders the real ads — a **plugin-pattern** implementation.

```
┌───────────────────────────────────────────────┐
│             React Native (JS/TS)              │
│  <BannerAd adUnitId="…" />              ──►  │──► Android / iOS Native Module
│  new InterstitialAd('…').load().show()  ──►  │──► nap mx (AdMixer SSP) SDK
│  onAdLoaded / onAdFailed / rewarded     ◄──  │◄── SDK events
└───────────────────────────────────────────────┘
```

### 지원 광고 포맷 (Supported formats)

| 포맷 / Format | 컴포넌트·클래스 / Component·Class | 설명 / Description |
|---|---|---|
| `Banner` | `<BannerAd>` | 배너 광고 / banner (320×50 등 / etc.) |
| `Native` | `<NativeAd>` | 네이티브 광고 / native (custom layout) |
| `Video` | `<VideoAd>` | 인라인 동영상 / inline video |
| `Interstitial` | `InterstitialAd` | 전면 광고 (Basic 전용) / interstitial (Basic-only) |
| `Interstitial Video` | `InterstitialVideoAd` | 전면 동영상 / interstitial video |
| `Rewarded` | `RewardedAd` | 보상형 광고 (S2S 콜백) / rewarded (S2S callback) |

---

## 빠른 시작 (Quick Start)

```bash
npm install react-native-nap-ssp   # 또는 / or: yarn add react-native-nap-ssp
```

```tsx
import { NapSspAd, BannerAd, InterstitialAd, RewardedAd } from 'react-native-nap-ssp';

// 1) 초기화 / initialize
NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['BANNER_ID', 'NATIVE_ID', 'INTERSTITIAL_ID', 'REWARD_ID'],
  logLevel: 'info',
  mediations: { naverAdManager: true, teads: true /* Android only */ },
});

// 2) 배너 / banner
<BannerAd adUnitId="YOUR_BANNER_ID" size="BANNER_320x50"
  onAdLoaded={() => console.log('loaded')} onAdFailedToLoad={(e) => console.warn('failed', e)} />

// 3) 전면 / interstitial (Basic-only)
const ad = new InterstitialAd('INTERSTITIAL_ID');
ad.addAdEventListener('loaded', () => ad.show());
await ad.load();

// 4) 보상형 / rewarded
const rewarded = new RewardedAd('REWARD_ID');
rewarded.addAdEventListener('rewarded', () => { /* 보상 지급 / grant reward */ });
await rewarded.load();
await rewarded.show();
```

---

## 🤖 Android 연동 요약 (Android summary)

**1. `android/build.gradle` — Maven 저장소 / repositories**

```gradle
allprojects {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' } // AdFit
    maven { url "https://artifact.bytedance.com/repository/pangle/" }       // Pangle
    // Teads 사용 시 / when using Teads
    maven { url "https://sdk.teads.tv/android/repo" }
    maven { url "https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod" }
  }
}
```

**2. `android/app/build.gradle` — 의존성 / dependencies**

```gradle
dependencies {
  implementation 'io.github.nasmedia-tech:admixer-ssp:2.0.0'
  implementation 'com.google.android.gms:play-services-ads-identifier:18.2.0'
  // 미디에이션 어댑터 (사용할 것만) / mediation adapters (only those you use)
  implementation 'io.github.nasmedia-tech:admixer-admanager:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-adfit:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-pangle:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-applovin:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-unity:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-naveradmanager:2.0.0'
  implementation 'io.github.nasmedia-tech:admixer-teads:2.0.0'
}
```

> ℹ️ v2.0.0 부터 `registerAdapter()` 수동 호출이 필요 없습니다(자동 등록). AdManager 사용 시 `play-services-ads` 는 25.2.0 이하로 고정하세요.
> Since v2.0.0, no manual `registerAdapter()` is needed (auto-registration). Pin `play-services-ads` ≤ 25.2.0 when using AdManager.

**3. `AndroidManifest.xml` — 권한 / permissions**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

**4. `proguard-rules.pro`**

```proguard
-keep class com.nasmedia.admixerssp.** { *; }
# 사용하는 어댑터만 / only the adapters you use
-keep class com.nasmedia.admanager.** { *; }
-keep class com.nasmedia.adfit.** { *; }
-keep class com.nasmedia.naveradmanager.** { *; }
-keep class com.nasmedia.teads.** { *; }
```

👉 상세 / details: [ANDROID_SETUP.md](./ANDROID_SETUP.md)

---

## 🍎 iOS 연동 요약 (iOS summary)

**1-A. CocoaPods** — `ios/Podfile`

```ruby
platform :ios, '14.0'
target 'YourAppName' do
  use_frameworks!
  pod 'AdMixerMediation'          # 2.3.7
  pod 'AdMixerMediationGAM'       # GAM
  pod 'AdMixerMediationAdFit'     # AdFit
  pod 'AdMixerMediationPangle'    # Pangle
  pod 'AdMixerMediationNAM'       # Naver Ad Manager (신규 / new)
end
```

```bash
cd ios && pod install
```

**1-B. Swift Package Manager** — Xcode → File → Add Package Dependencies

```
https://github.com/Nasmedia-Tech/iOS-SSP-SPM.git           (AdMixerMediation 2.3.7)
https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM.git (Mediation)
```

> ℹ️ iOS 에는 **Teads 어댑터가 없습니다.** / There is **no Teads adapter on iOS.**

**2. `Info.plist` — ATT**

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

**3. `.xcworkspace` 로 열어 빌드 / open the `.xcworkspace` and build.**

👉 상세 / details: [IOS_SETUP.md](./IOS_SETUP.md)

---

## 문서 목록 (Documents)

### 시작하기 / Getting started
| 파일 / File | 설명 / Description |
|---|---|
| [GETTING_STARTED.md](./GETTING_STARTED.md) | 처음 연동 시작 / start here |
| [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) | 업그레이드(0.1.x → 0.2.0 / v2) / upgrade |
| [VERSION_MATRIX.md](./VERSION_MATRIX.md) | 버전 호환성 표 / version matrix |

### 플랫폼 설정 / Platform setup
| 파일 / File | 설명 / Description |
|---|---|
| [ANDROID_SETUP.md](./ANDROID_SETUP.md) | Android 상세 설정 / Android setup |
| [IOS_SETUP.md](./IOS_SETUP.md) | iOS 상세 설정 / iOS setup |
| [SPM_GUIDE.md](./SPM_GUIDE.md) | Swift Package Manager |
| [EXPO_GUIDE.md](./EXPO_GUIDE.md) | Expo |

### 기능 가이드 / Feature guides
| 파일 / File | 설명 / Description |
|---|---|
| [API_REFERENCE.md](./API_REFERENCE.md) | API 명세 / API reference |
| [MEDIATION_GUIDE.md](./MEDIATION_GUIDE.md) | 미디에이션 / mediation |
| [NATIVE_ASSETS_GUIDE.md](./NATIVE_ASSETS_GUIDE.md) | 네이티브 광고 레이아웃 / native ad layout |
| [ADVANCED_USAGE.md](./ADVANCED_USAGE.md) | 고급 사용법 (S2S 콜백, 사전 로딩) / advanced (S2S, preloading) |
| [PRIVACY_GUIDE.md](./PRIVACY_GUIDE.md) | 개인정보·ATT·GDPR/CCPA / privacy |

### 참고 / Reference
| 파일 / File | 설명 / Description |
|---|---|
| [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) | 문제 해결 / troubleshooting |
| [FAQ.md](./FAQ.md) | 자주 묻는 질문 / FAQ |
| [GLOSSARY.md](./GLOSSARY.md) | 용어 / glossary |
| [ROADMAP.md](./ROADMAP.md) | 로드맵 / roadmap |

---

## 이벤트 종류 (Events)

`loaded` · `loadFailed` · `opened` · `closed` · `clicked` · `impression` · `rewarded` · `completed` · `skipped`

---

## 주의사항 (Notes)

- **AdUnit ID 중복 사용 금지** — 하나의 AdUnit ID는 하나의 광고 객체에서만 사용. / Do not reuse a single AdUnit ID across multiple ad objects.
- **DEBUG 빌드** — 로드 실패 시 플레이스홀더 이벤트 발행(실제 광고 아님). / DEBUG emits placeholder events (not real ads).
- **RELEASE 빌드** — 실기기에서 반드시 검증. / Always validate RELEASE on real devices.
- **미디어 키 / 광고 ID** — 나스미디어 nap mx 운영팀에서 발급. / Issued by the nap mx operations team.

---

## 문의 (Contact)

| 항목 / Item | 내용 / Value |
|---|---|
| **기술/발급 문의 / Tech & issuance** | nap_mx@nasmedia.co.kr |
| **공식 가이드 / Official guide** | https://napmx.github.io/ |
| **GitHub** | [Nasmedia-Tech](https://github.com/Nasmedia-Tech) |
| **npm** | `react-native-nap-ssp` |
