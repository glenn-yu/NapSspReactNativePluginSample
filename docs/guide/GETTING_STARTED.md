# React Native Nap SSP — 시작 가이드

![npm](https://img.shields.io/badge/npm-react--native--nap--ssp-red)
![version](https://img.shields.io/badge/version-0.1.6-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-lightgrey)
![License](https://img.shields.io/badge/license-MIT-green)

> **react-native-nap-ssp** — KT 나스미디어 Nap SSP SDK를 React Native 앱에 연동하는 플러그인입니다.

---

## 목차

1. [사전 준비](#1-사전-준비)
2. [설치](#2-설치)
3. [Android 설정](#3-android-설정)
4. [iOS 설정](#4-ios-설정)
5. [SDK 초기화](#5-sdk-초기화)
6. [광고 유형별 사용법](#6-광고-유형별-사용법)
   - [배너 광고 (Banner)](#61-배너-광고-banner)
   - [네이티브 광고 (Native)](#62-네이티브-광고-native)
   - [동영상 광고 (Video)](#63-동영상-광고-video)
   - [전면 광고 (Interstitial)](#64-전면-광고-interstitial)
   - [전면 동영상 광고 (Interstitial Video)](#65-전면-동영상-광고-interstitial-video)
   - [보상형 광고 (Rewarded)](#66-보상형-광고-rewarded)
7. [디버그 vs 릴리즈 빌드 동작 차이](#7-디버그-vs-릴리즈-빌드-동작-차이)
8. [자주 발생하는 문제](#8-자주-발생하는-문제)
9. [문의 및 지원](#9-문의-및-지원)

---

## 1. 사전 준비

| 항목 | 요구 사항 |
| :--- | :--- |
| React Native | 0.72.0 이상 |
| Android minSdk | 21 이상 |
| Android targetSdk | 34 이상 권장 |
| Android JDK | 17 |
| iOS | 13.0 이상 |
| Xcode | 15.3 이상 |
| **미디어 키 (Media Key)** | 나스미디어 파트너 운영팀 발급 |
| **광고 단위 ID (Ad Unit ID)** | 나스미디어 파트너 운영팀 발급 |

> ⚠️ 미디어 키와 광고 단위 ID 없이는 광고가 로드되지 않습니다. 연동 전 반드시 발급받으세요.  
> **문의**: nap_adx@nasmedia.co.kr

---

## 2. 설치

```bash
npm install react-native-nap-ssp
# 또는
yarn add react-native-nap-ssp
```

---

## 3. 🤖 Android 설정

### Step 1 — Maven 리포지토리 추가

`android/build.gradle` (프로젝트 루트 레벨):

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

### Step 2 — 의존성 추가

`android/app/build.gradle`:

```gradle
dependencies {
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.23'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.9.0'
}
```

### Step 3 — 미디에이션 어댑터 등록

`MainApplication.kt`에서 사용할 어댑터를 등록합니다.

```kotlin
import com.nasmedia.admixerssp.common.AdMixer

class MainApplication : Application(), ReactApplication {
    override fun onCreate() {
        super.onCreate()
        // 사용할 미디에이션 어댑터만 등록
        AdMixer.registerAdapter(AdMixer.ADAPTER_ADMANAGER)
        AdMixer.registerAdapter(AdMixer.ADAPTER_ADFIT)
        AdMixer.registerAdapter(AdMixer.ADAPTER_PANGLE)
    }
}
```

### Step 4 — 권한 추가

`android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### Step 5 — ProGuard 설정 (릴리즈 빌드 필수)

`android/app/proguard-rules.pro`:

```proguard
-keep class com.nasmedia.admixerssp.** { *; }
-keep class com.nasmedia.admanager.**  { *; }
-keep class com.nasmedia.adfit.**      { *; }
-keep class com.nasmedia.pangle.**     { *; }
-keep class com.google.android.gms.ads.** { *; }
```

👉 상세 설정은 [Android Setup Guide](./ANDROID_SETUP.md) 참조

---

## 4. 🍎 iOS 설정

### Step 1 — CocoaPods 의존성 추가

`ios/Podfile`:

```ruby
platform :ios, '13.0'

target 'YourAppName' do
  use_frameworks!
  pod 'AdMixerMediation'
end
```

```bash
cd ios && pod install
```

> ⚠️ 반드시 `.xcworkspace` 파일로 Xcode를 열어 빌드하세요.

### Step 2 — Info.plist 설정

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

👉 상세 설정은 [iOS Setup Guide](./IOS_SETUP.md) 참조

---

## 5. SDK 초기화

앱이 시작될 때 **한 번만** 호출합니다. `App.tsx` 최상위 `useEffect`에서 호출하는 것을 권장합니다.

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: '여기에_미디어_키_입력',
      adUnitIds: [
        '배너_광고_ID',
        '네이티브_광고_ID',
        '전면_광고_ID',
        '보상형_광고_ID',
      ],
      logLevel: 'info', // 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none'
    });
  }, []);

  return (/* ... */);
}
```

> ⚠️ **AdUnit ID 중복 사용 금지**: 하나의 AdUnit ID는 단 하나의 광고 객체에서만 사용해야 합니다. 동일한 ID를 여러 컴포넌트에서 동시에 사용하면 광고가 정상적으로 로드되지 않습니다.

---

## 6. 광고 유형별 사용법

### 6.1 배너 광고 (Banner)

```tsx
import { BannerAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <BannerAd
      adUnitId="배너_광고_ID"
      size="BANNER_320x50"
      onAdLoaded={() => console.log('배너 광고 로드 성공')}
      onAdFailedToLoad={(error) => console.warn('배너 광고 로드 실패', error)}
    />
  );
}
```

| 사이즈 상수 | 크기 |
| :--- | :--- |
| `BANNER_320x50` | 320 × 50 (기본 배너) |
| `BANNER_320x100` | 320 × 100 |
| `BANNER_300x250` | 300 × 250 (중형 직사각형) |
| `BANNER_320x480` | 320 × 480 |
| `LARGE_BANNER` | 320 × 100 (대형 배너) |
| `MEDIUM_RECTANGLE` | 300 × 250 (미디엄 직사각형) |
| `SMART_BANNER` | 화면 너비에 맞게 자동 조절 |

---

### 6.2 네이티브 광고 (Native)

```tsx
import { NativeAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <NativeAd
      adUnitId="네이티브_광고_ID"
      style={{ width: '100%', height: 200 }}
      onAdLoaded={() => console.log('네이티브 광고 로드 성공')}
      onAdFailedToLoad={(error) => console.warn('네이티브 광고 로드 실패', error)}
    />
  );
}
```

> 💡 `style`에 반드시 `width`와 `height`를 명시해야 광고가 표시됩니다.

---

### 6.3 동영상 광고 (Video)

```tsx
import { VideoAd } from 'react-native-nap-ssp';

function MyScreen() {
  return (
    <VideoAd
      adUnitId="동영상_광고_ID"
      style={{ width: '100%', height: 250 }}
      onAdLoaded={() => console.log('동영상 광고 로드 성공')}
      onAdCompleted={() => console.log('동영상 시청 완료')}
      onAdSkipped={() => console.log('동영상 스킵')}
    />
  );
}
```

---

### 6.4 전면 광고 (Interstitial)

`load()` 후 원하는 시점에 `show()`로 표시합니다.

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

async function showInterstitial() {
  const interstitial = new InterstitialAd('전면_광고_ID');

  interstitial.addAdEventListener('loaded', () => console.log('광고 로드 완료'));
  interstitial.addAdEventListener('closed', () => console.log('광고 닫힘'));
  interstitial.addAdEventListener('loadFailed', (error) => console.warn('로드 실패', error));

  try {
    await interstitial.load();
    await interstitial.show();
  } catch (error) {
    console.warn('전면 광고 표시 실패', error);
  }
}
```

---

### 6.5 전면 동영상 광고 (Interstitial Video)

```tsx
import { InterstitialVideoAd } from 'react-native-nap-ssp';

async function showInterstitialVideo() {
  const video = new InterstitialVideoAd('전면동영상_광고_ID');

  video.addAdEventListener('loaded', () => console.log('광고 로드 완료'));
  video.addAdEventListener('completed', () => console.log('동영상 시청 완료'));
  video.addAdEventListener('skipped', () => console.log('동영상 스킵'));
  video.addAdEventListener('closed', () => console.log('광고 닫힘'));

  try {
    await video.load();
    await video.show();
  } catch (error) {
    console.warn('전면 동영상 광고 표시 실패', error);
  }
}
```

---

### 6.6 보상형 광고 (Rewarded)

```tsx
import { RewardedAd } from 'react-native-nap-ssp';

async function showRewarded() {
  const rewarded = new RewardedAd('보상형_광고_ID');

  rewarded.addAdEventListener('loaded', () => console.log('광고 로드 완료'));
  rewarded.addAdEventListener('rewarded', () => {
    // 여기서 사용자에게 보상을 지급하세요
    console.log('보상 지급!');
  });
  rewarded.addAdEventListener('closed', () => console.log('광고 닫힘'));
  rewarded.addAdEventListener('loadFailed', (error) => console.warn('로드 실패', error));

  try {
    await rewarded.load();
    await rewarded.show();
  } catch (error) {
    console.warn('보상형 광고 표시 실패', error);
  }
}
```

> 💡 보안이 중요한 서비스에서는 클라이언트 콜백 대신 **S2S(Server-to-Server) 보상 콜백**을 권장합니다. 자세한 내용은 [Advanced Usage](./ADVANCED_USAGE.md)를 참조하세요.

---

## 7. 디버그 vs 릴리즈 빌드 동작 차이

v0.1.6부터 DEBUG / RELEASE 빌드에 따라 광고 실패 처리 방식이 다릅니다.

| 상황 | DEBUG 빌드 | RELEASE 빌드 |
| :--- | :--- | :--- |
| SDK 광고 로드 실패 | `onAdLoaded` (플레이스홀더) | `onAdFailedToLoad` (실제 에러) |
| SDK 12초 응답 없음 | `onAdLoaded` (타임아웃 폴백) | 해당 없음 |
| 전면/보상형 `show()` | 플레이스홀더로 즉시 성공 처리 | SDK 통해 실제 광고 표시 |

이벤트 payload의 `source` 필드가 `"placeholder"`, `"debug-no-fill"`, `"debug-sdk-timeout"` 이면 플레이스홀더입니다.

> ⚠️ 실제 광고 소재 노출과 수익 집계는 반드시 **RELEASE 빌드 + 실기기**에서 검증하세요.

---

## 8. 자주 발생하는 문제

**광고가 전혀 로드되지 않아요**
- `mediaKey`와 `adUnitId`가 올바른지 확인하세요.
- 인터넷 연결 상태를 확인하세요.
- 시뮬레이터 대신 실기기로 테스트해 보세요.

**Android: `NapSspXXX is not linked` 에러**  
Android 스튜디오에서 **Sync Project with Gradle Files**를 실행하거나 `npx react-native run-android`를 재실행하세요.

**Android: `Unsupported class file major version` 에러**  
JDK 17을 사용하도록 환경을 설정하세요.

**Android: 릴리즈 빌드에서 광고가 안 보여요**  
ProGuard 설정을 확인하세요. → [Step 5](#step-5--proguard-설정-릴리즈-빌드-필수)

**iOS: 빌드 후에도 광고가 안 나와요**  
`.xcworkspace`로 빌드하고 있는지 확인하세요.

**iOS: ATT 팝업이 안 나와요**  
`Info.plist`에 `NSUserTrackingUsageDescription`이 있는지 확인하세요.

**배너/네이티브 광고가 안 보여요**  
`style`에 `width`와 `height`를 명시했는지 확인하세요.

---

## 9. 문의 및 지원

| 항목 | 내용 |
| :--- | :--- |
| **기술 문의** | nap_adx@nasmedia.co.kr |
| **미디어 키 / 광고 ID 발급** | 나스미디어 파트너 운영팀 |
| **플러그인 버전** | 0.1.6 |
| **npm 패키지** | `react-native-nap-ssp` |

---

> 📚 더 자세한 내용은 아래 문서를 참고하세요.
>
> - [API Reference](./API_REFERENCE.md) — 전체 API 명세
> - [Android Setup (상세)](./ANDROID_SETUP.md) — 미디에이션 어댑터 포함 상세 설정
> - [iOS Setup (상세)](./IOS_SETUP.md) — Xcode 상세 설정
> - [Mediation Guide](./MEDIATION_GUIDE.md) — 미디에이션 네트워크 연동
> - [Advanced Usage](./ADVANCED_USAGE.md) — S2S 콜백, 고급 설정
> - [Troubleshooting](./TROUBLESHOOTING.md) — 문제 해결
> - [FAQ](./FAQ.md) — 자주 묻는 질문
