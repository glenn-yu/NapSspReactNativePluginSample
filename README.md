# React Native Nap SSP 플러그인 (v0.1.7)

KT Nasmedia Nap SSP SDK를 React Native에서 쓰기 위한 플러그인입니다.

- 버전: `0.1.7`
- 지원 목표: Android / iOS
- 제공 형태: Native Module + Native View
- 현재 중심 기능: 초기화, 배너, 전면, 전면 동영상, 리워드, 네이티브, 비디오
- 현재 미지원: Bizboard 전용 RN surface
- 예제 앱은 위 주요 흐름을 모두 커버하면서, 네이티브 브릿지 없이도 화면이 렌더되는 플레이스홀더 모드도 보여줍니다.

## 📱 Compatibility (호환성)

| 환경 | 지원 범위 |
| :--- | :--- |
| **React Native** | `>= 0.72.0` |
| **Android** | `minSdkVersion 21`, `targetSdkVersion 34+` |
| **iOS** | `iOS 14.0+` |
| **Architecture** | Old Architecture 전용 (New Architecture 지원 예정) |

---

## 가장 먼저 할 일

1. 패키지 설치
2. Android/iOS 네이티브 설정 추가
3. `NapSspAd.initialize()` 호출
4. `BannerAd` 또는 `InterstitialAd`부터 확인

```bash
npm install react-native-nap-ssp
# 또는
yarn add react-native-nap-ssp
```

> 권장: React Native 0.72 이상

---

## 5분 안에 실행하기

### 1) 앱 시작 시 초기화

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: '발급받은_MEDIA_KEY',
      adUnitIds: [
        'BANNER_ID',
        'INTER_ID',
        'REWARD_ID',
        'NATIVE_ID',
        'VIDEO_ID',
        'INTER_VIDEO_ID',
      ],
      logLevel: 'debug',
    });
  }, []);

  return null;
}
```

### 2) 배너 하나 띄우기

```tsx
import React from 'react';
import { BannerAd } from 'react-native-nap-ssp';

export default function Screen() {
  return <BannerAd adUnitId="BANNER_ID" size="BANNER_320x50" />;
}
```

### 3) 전면 광고 띄우기

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('INTER_ID');
await interstitial.load();
await interstitial.show();
```

---

## ⚙️ 네이티브 필수 설정

광고 SDK가 정상적으로 작동하려면 플랫폼별 필수 설정이 필요합니다.

### 🤖 Android (안드로이드)

`android/build.gradle` (프로젝트 루트 수준)에 아래 리포지토리를 추가하세요.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://devrepo.kakao.com/nexus/content/groups/public/" }
        maven { url "https://artifact.bytedance.com/repository/pangle" }
    }
}
```

`android/app/build.gradle` 파일의 `dependencies`에 아래 내용을 추가하세요.

```gradle
dependencies {
    // (필수) Nap SSP SDK 본체
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.23'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.3.0'

    // (선택) 미디에이션 어댑터 (원하는 네트워크만 추가)
    implementation 'io.github.nasmedia-tech:admixer-admanager:1.0.15_delta'
    implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.12_beta'
    implementation 'io.github.nasmedia-tech:admixer-pangle:1.0.12_beta'
    implementation 'io.github.nasmedia-tech:admixer-applovin:1.0.10_beta'
    implementation 'io.github.nasmedia-tech:admixer-unity:1.0.7_beta'
}
```

### 🍎 iOS (아이폰)

iOS 환경에서는 **CocoaPods**와 **SPM(Swift Package Manager)** 두 가지 방식을 모두 지원합니다.

#### 옵션 A: CocoaPods 사용 (React Native 기본 권장)

`ios/Podfile` 파일에 아래 내용을 추가하고 `pod install`을 실행하세요.

```ruby
# (필수) Nap SSP SDK 본체
pod 'AdMixerMediation'

# (선택) 미디에이션 어댑터
pod 'AdMixerMediationGAM'      # Google AdManager
pod 'AdMixerMediationAdFit'    # Kakao AdFit
pod 'AdMixerMediationPangle'   # Pangle
pod 'AdMixerMediationAppLovin' # AppLovin
pod 'AdMixerMediationUnityAds' # Unity Ads
```

#### 옵션 B: SPM (Swift Package Manager) 사용

Xcode를 열고 다음 단계를 진행합니다.

1. `File` → `Add Packages...` 선택
2. 로컬 패키지 추가: `node_modules/react-native-nap-ssp/ios` 폴더 선택
3. AdMixerMediation XCFramework (v2.3.3)가 `ios/Package.swift`에 정의되어 있으며 자동으로 포함됩니다.

---

## 💻 사용 가이드

앱에서 광고를 띄우려면 **"1. 초기화 → 2. 화면에 표시"** 딱 2단계만 거치면 됩니다.

### ✅ 1단계: SDK 초기화 (앱 시작 시 1번만)

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

export default function App() {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: '발급받은_MEDIA_KEY',
      adUnitIds: [
        'BANNER_ID', 'INTER_ID', 'REWARD_ID',
        'NATIVE_ID', 'VIDEO_ID', 'INTER_VIDEO_ID'
      ],
      logLevel: 'debug',
    }).then(() => console.log('SDK 초기화 성공!'));
  }, []);

  return <Home />;
}
```

---

### 🖼️ 2단계: 화면에 그리는 광고 (배너, 네이티브, 비디오)

#### 1) 배너 광고 (Banner)

```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd
  adUnitId="BANNER_ID"
  size="BANNER_320x50"   // BANNER_300x250, SMART_BANNER 등 지원
  style={{ marginTop: 20 }}
  onAdLoaded={() => console.log('배너 로드 성공!')}
  onAdFailedToLoad={(error) => console.log('배너 로드 실패:', error.message)}
  onAdClicked={() => console.log('배너 클릭됨!')}
/>
```

#### 2) 네이티브 광고 (Native Ad)

```tsx
import { NativeAd } from 'react-native-nap-ssp';

<NativeAd
  adUnitId="NATIVE_ID"
  style={{ width: '100%', height: 250 }}
  onAdLoaded={() => console.log('네이티브 광고 로드됨')}
  onAdClicked={() => console.log('네이티브 광고 클릭됨!')}
/>
```

#### 3) 인라인 동영상 광고 (Video Ad)

```tsx
import { VideoAd } from 'react-native-nap-ssp';

<VideoAd
  adUnitId="VIDEO_ID"
  style={{ width: '100%', height: 200 }}
  onAdLoaded={() => console.log('동영상 광고 준비 완료')}
  onAdCompleted={() => console.log('동영상 끝까지 시청 완료!')}
  onAdSkipped={() => console.log('사용자가 동영상을 스킵함')}
  onAdClicked={() => console.log('동영상 더보기 버튼 눌림')}
/>
```

---

### 🎬 3단계: 화면 전체를 덮는 광고 (전면, 리워드)

#### 1) 전면 광고 (Interstitial)

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

const showInterstitial = async () => {
  const inter = new InterstitialAd('INTER_ID', {
    type: 'popup',
    countDownTime: 5,
    buttonLeftText: '닫기',
  });

  inter.addAdEventListener('closed', () => console.log('전면 광고가 닫혔습니다.'));
  inter.addAdEventListener('clicked', () => console.log('전면 광고가 클릭되었습니다.'));

  try {
    await inter.load();
    await inter.show();
  } catch (error) {
    console.error('전면 광고 실패:', error);
  }
};
```

#### 2) 전면 동영상 광고 (Interstitial Video)

```tsx
import { InterstitialVideoAd } from 'react-native-nap-ssp';

const showInterstitialVideo = async () => {
  const interVideo = new InterstitialVideoAd('INTER_VIDEO_ID');

  interVideo.addAdEventListener('completed', () => console.log('전면 동영상 끝까지 시청함!'));
  interVideo.addAdEventListener('skipped', () => console.log('전면 동영상 스킵됨.'));
  interVideo.addAdEventListener('closed', () => console.log('전면 동영상 창 닫힘.'));

  await interVideo.load();
  await interVideo.show();
};
```

#### 3) 리워드 동영상 광고 (Rewarded)

```tsx
import { RewardedAd } from 'react-native-nap-ssp';

const showRewardedAd = async () => {
  const reward = new RewardedAd('REWARD_ID', {
    customParams: { useid: 'user123', name: '홍길동' },
    mute: true  // (안드로이드 전용) 시작 시 음소거
  });

  reward.addAdEventListener('rewarded', (item) => {
    console.log('보상 지급 이벤트 발생!', item);
    // 정확한 보상 처리는 S2S 콜백 사용을 강력 권장합니다.
  });

  reward.addAdEventListener('closed', () => {
    console.log('리워드 광고 창이 닫혔습니다.');
  });

  await reward.load();
  await reward.show();
};
```

---


## 🧪 DEBUG 빌드 플레이스홀더 동작

v0.1.5부터 DEBUG 빌드에서는 시뮬레이터/에뮬레이터의 no-fill, SDK timeout, 일부 미디에이션 callback 누락 상황에서도 RN 이벤트 파이프라인을 검증할 수 있도록 플레이스홀더 성공 이벤트를 발행합니다. 실제 광고 노출 검증은 RELEASE 빌드 + 실기기에서 진행하세요. 자세한 기준은 [Troubleshooting](./docs/TROUBLESHOOTING.md)을 확인하세요.

---
## ✅ 테스트와 검증

```bash
npm run verify
```

- `typecheck`: TypeScript 타입 확인
- `build`: `lib/` 생성 확인
- `smoke:test`: 공개 API 및 기본 초기화 흐름 확인

---

## 📚 Documentation (가이드 문서)

프로젝트의 상세 가이드와 검증 리포트는 `docs/` 디렉토리에 정리되어 있습니다.

- **[Getting Started](./docs/GETTING_STARTED.md)**: 처음 연동할 때 보는 빠른 시작 가이드
- **[API Reference](./docs/API_REFERENCE.md)**: 컴포넌트 및 클래스 상세 명세
- **[Troubleshooting](./docs/TROUBLESHOOTING.md)**: 설치 및 런타임 주요 에러 해결 방법
- **[FAQ](./docs/FAQ.md)**: 자주 묻는 질문
- **[Android Setup Guide](./docs/ANDROID_SETUP.md)**: 안드로이드 프로젝트 상세 설정
- **[iOS Setup Guide](./docs/IOS_SETUP.md)**: iOS 프로젝트 상세 설정 (Podfile 포함)
- **[Mediation Setup Guide](./docs/MEDIATION_GUIDE.md)**: 광고 네트워크별 네이티브 세부 설정
- **[SPM 통합 가이드](./docs/SPM_GUIDE.md)**: Swift Package Manager 연동 및 제약 사항
- **[Version Matrix](./docs/VERSION_MATRIX.md)**: 플러그인 및 네이티브 SDK 버전 맵
- **[Migration Guide](./docs/MIGRATION_GUIDE.md)**: 버전별 변경 사항 및 업그레이드 가이드
- **[NPM 배포 가이드](./docs/PUBLISH_GUIDE.md)**: 플러그인 배포 절차 및 체크리스트
- **[Privacy & Compliance](./docs/PRIVACY_GUIDE.md)**: COPPA, ATT, AD_ID 설정 가이드
- **[Native Assets Guide](./docs/NATIVE_ASSETS_GUIDE.md)**: 네이티브 광고 레이아웃 구성 방법
- **[Expo Compatibility Guide](./docs/EXPO_GUIDE.md)**: Expo 환경에서 사용하는 방법
- **[Advanced Usage](./docs/ADVANCED_USAGE.md)**: 광고 사전 로딩 및 캐싱 최적화 팁
- **[Maestro 검증 가이드](./docs/MAESTRO_GUIDE.md)**: 자동화 테스트 환경 설정 및 실행 방법
- **[Development & Architecture](./docs/DEVELOPMENT.md)**: 내부 구조 설명 및 기여 가이드
- **[Roadmap](./docs/ROADMAP.md)**: 향후 업데이트 및 기능 추가 계획

---

## 지원 범위 메모

- 공식 Android/iOS 네이티브 가이드에는 Bizboard 관련 내용이 포함되어 있지만, 현재 RN 패키지 기준 지원 범위는 배너, 전면, 전면 동영상, 리워드, 네이티브, 인라인 비디오입니다.

## ❓ 자주 묻는 질문 (FAQ)

- **Q. `NapSsp XXX is not linked` 에러가 발생합니다.**
  - A. 패키지 설치 후 네이티브 빌드를 다시 해야 합니다. 안드로이드는 `npx react-native run-android`를 다시 실행하세요. iOS는 `cd ios && pod install`을 꼭 해주세요.

- **Q. 광고 로드가 안 되고 실패(Failed to load)가 뜹니다.**
  - A. 발급받은 `mediaKey`와 `adUnitId`가 정확한지 확인하세요. 또한, 테스트 중에는 실제 인터넷 연결이 필수입니다.

- **Q. iOS에서 시뮬레이터로 돌리는데 안 나옵니다.**
  - A. 광고 SDK 중 일부는 실기기(Real Device) 환경에서만 정상적으로 로드됩니다. 가급적 실기기에서 테스트해 주세요.

- **Q. 이벤트 리스너의 이름이 헷갈립니다.**
  - A. JS 클래스의 `addAdEventListener()`는 `loaded`, `loadFailed`, `opened`, `closed`, `clicked`, `impression`, `rewarded`, `completed`, `skipped` 같은 정규화된 이벤트명을 사용합니다. 컴포넌트 props는 `onAdLoaded`처럼 `on` 접두사를 사용합니다. 상세 목록은 [API Reference](./docs/API_REFERENCE.md)를 참조하세요.

---

> **문의:** nap_adx@nasmedia.co.kr (나스미디어 SDK 운영팀)
