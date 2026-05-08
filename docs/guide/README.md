# react-native-nap-ssp

> **KT 나스미디어 Nap SSP SDK** × **React Native** 공식 플러그인 — Android / iOS 통합 광고 연동 가이드

![npm](https://img.shields.io/badge/npm-react--native--nap--ssp-red)
![version](https://img.shields.io/badge/version-0.1.6-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-lightgrey)
![RN](https://img.shields.io/badge/React%20Native-0.72%2B-61DAFB)
![License](https://img.shields.io/badge/license-MIT-green)

---

## 개요

React Native 앱에서 JavaScript 몇 줄로 Nap SSP 광고를 요청하면, 네이티브 레이어(Android / iOS)가 실제 광고를 렌더링하는 **플러그인 패턴**의 레퍼런스 구현체입니다.

```
┌───────────────────────────────────────────────┐
│             React Native (JS/TS)              │
│  <BannerAd adUnitId="…" />              ──►  │──► Android / iOS Native Module
│  new InterstitialAd('…').load().show()  ──►  │──► NapSSP SDK
│  onAdLoaded / onAdFailed / rewarded     ◄──  │◄── SDK 이벤트
└───────────────────────────────────────────────┘
```

### 지원 광고 포맷

| 포맷 | 컴포넌트 / 클래스 | 설명 |
|---|---|---|
| `Banner` | `<BannerAd>` | 배너 광고 (320×50 등 다양한 사이즈) |
| `Native` | `<NativeAd>` | 네이티브 광고 (커스텀 레이아웃) |
| `Video` | `<VideoAd>` | 인라인 동영상 광고 |
| `Interstitial` | `InterstitialAd` | 전면 광고 |
| `Interstitial Video` | `InterstitialVideoAd` | 전면 동영상 광고 |
| `Rewarded` | `RewardedAd` | 보상형 광고 (S2S 콜백 지원) |

---

## 빠른 시작

### 1. 설치

```bash
npm install react-native-nap-ssp
# 또는
yarn add react-native-nap-ssp
```

### 2. SDK 초기화

```tsx
import { NapSspAd } from 'react-native-nap-ssp';

NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['BANNER_ID', 'NATIVE_ID', 'INTERSTITIAL_ID', 'REWARD_ID'],
  logLevel: 'info',
});
```

### 3. 배너 광고

```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd
  adUnitId="YOUR_BANNER_ID"
  size="BANNER_320x50"
  onAdLoaded={() => console.log('로드 성공')}
  onAdFailedToLoad={(e) => console.warn('로드 실패', e)}
/>
```

### 4. 전면 / 보상형 광고

```tsx
import { InterstitialAd, RewardedAd } from 'react-native-nap-ssp';

// 전면 광고
const ad = new InterstitialAd('INTERSTITIAL_ID');
ad.addAdEventListener('loaded', () => ad.show());
await ad.load();

// 보상형 광고
const rewarded = new RewardedAd('REWARD_ID');
rewarded.addAdEventListener('rewarded', () => { /* 보상 지급 */ });
await rewarded.load();
await rewarded.show();
```

---

## 🤖 Android 연동 요약

**1. `android/build.gradle` — Maven 저장소 추가**

```gradle
allprojects {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
    maven { url "https://artifact.bytedance.com/repository/pangle/" }
  }
}
```

**2. `android/app/build.gradle` — SDK 의존성 추가**

```gradle
dependencies {
  implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.23'
  implementation 'com.google.android.gms:play-services-ads-identifier:18.9.0'
  // 미디에이션 어댑터 (사용할 것만 선택)
  implementation 'io.github.nasmedia-tech:admixer-admanager:1.0.15_delta'
  implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.12_beta'
  implementation 'io.github.nasmedia-tech:admixer-pangle:1.0.12_beta'
}
```

**3. `MainApplication.kt` — 어댑터 등록**

```kotlin
import com.nasmedia.admixerssp.common.AdMixer

AdMixer.registerAdapter(AdMixer.ADAPTER_ADMANAGER)
AdMixer.registerAdapter(AdMixer.ADAPTER_ADFIT)
AdMixer.registerAdapter(AdMixer.ADAPTER_PANGLE)
```

**4. `AndroidManifest.xml` — 권한 추가**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

**5. `proguard-rules.pro` — ProGuard 규칙 추가**

```proguard
-keep class com.nasmedia.admixerssp.** { *; }
-keep class com.google.android.gms.ads.** { *; }
```

👉 상세 설정: [ANDROID_SETUP.md](./ANDROID_SETUP.md)

---

## 🍎 iOS 연동 요약

**1-A. CocoaPods 방식** — `ios/Podfile`

```ruby
platform :ios, '14.0'
target 'YourAppName' do
  use_frameworks!
  pod 'AdMixerMediation'
  pod 'AdMixerMediationGAM'       # GAM 사용 시
  pod 'AdMixerMediationAdFit'     # AdFit 사용 시
  pod 'AdMixerMediationPangle'    # Pangle 사용 시
end
```

```bash
cd ios && pod install
```

**1-B. Swift Package Manager 방식** — Xcode → File → Add Package Dependencies

```
https://github.com/Nasmedia-Tech/iOS-SSP-SPM.git           (AdMixerMediation 2.3.3)
https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM.git (미디에이션)
```

**2. `Info.plist` — ATT 권한 추가**

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

**3. `.xcworkspace`로 Xcode를 열어 빌드**

👉 상세 설정: [IOS_SETUP.md](./IOS_SETUP.md)

---

## 문서 목록

### 시작하기

| 파일 | 설명 |
|---|---|
| [GETTING_STARTED.md](./GETTING_STARTED.md) | **처음 연동 시 여기서 시작** — 설치부터 첫 광고 노출까지 |
| [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) | 이전 버전에서 업그레이드 |
| [VERSION_MATRIX.md](./VERSION_MATRIX.md) | 플러그인 버전별 SDK 호환성 표 |

### 플랫폼 설정

| 파일 | 설명 |
|---|---|
| [ANDROID_SETUP.md](./ANDROID_SETUP.md) | Android 상세 설정 (Maven, ProGuard, 어댑터) |
| [IOS_SETUP.md](./IOS_SETUP.md) | iOS 상세 설정 (CocoaPods, Xcode, ATT) |
| [SPM_GUIDE.md](./SPM_GUIDE.md) | iOS Swift Package Manager 통합 가이드 |
| [EXPO_GUIDE.md](./EXPO_GUIDE.md) | Expo 프로젝트에서 사용하는 방법 |

### 기능 가이드

| 파일 | 설명 |
|---|---|
| [API_REFERENCE.md](./API_REFERENCE.md) | 전체 API 명세 (컴포넌트, 클래스, 타입) |
| [MEDIATION_GUIDE.md](./MEDIATION_GUIDE.md) | 미디에이션 네트워크 연동 (AdFit, Pangle 등) |
| [NATIVE_ASSETS_GUIDE.md](./NATIVE_ASSETS_GUIDE.md) | 네이티브 광고 레이아웃 커스터마이징 |
| [ADVANCED_USAGE.md](./ADVANCED_USAGE.md) | 고급 사용법 (S2S 콜백, 전면 팝업 옵션) |
| [PRIVACY_GUIDE.md](./PRIVACY_GUIDE.md) | 개인정보 보호 및 ATT 대응 |

### 참고

| 파일 | 설명 |
|---|---|
| [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) | 문제 해결 가이드 |
| [FAQ.md](./FAQ.md) | 자주 묻는 질문 |
| [GLOSSARY.md](./GLOSSARY.md) | 용어 설명 |
| [ROADMAP.md](./ROADMAP.md) | 향후 업데이트 계획 |

---

## 이벤트 종류

`loaded` · `loadFailed` · `opened` · `closed` · `clicked` · `impression` · `rewarded` · `completed` · `skipped`

---

## 주의사항

- **AdUnit ID 중복 사용 금지** — 하나의 AdUnit ID는 단 하나의 광고 객체에서만 사용해야 합니다.
- **DEBUG 빌드** — SDK 광고 로드 실패 시 플레이스홀더 이벤트가 발행됩니다 (실제 광고 아님).
- **RELEASE 빌드** — 실기기에서 반드시 검증하세요. 플레이스홀더 폴백이 없습니다.
- **미디어 키 / 광고 단위 ID** — 나스미디어 파트너 운영팀에서 발급받아야 합니다.

---

## 문의

| 항목 | 내용 |
|---|---|
| **기술 문의** | nap_adx@nasmedia.co.kr |
| **미디어 키 / 광고 ID 발급** | 나스미디어 파트너 운영팀 |
| **GitHub** | [Nasmedia-Tech](https://github.com/Nasmedia-Tech) |
| **npm 패키지** | `react-native-nap-ssp` |

---
