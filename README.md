# 🚀 React Native Nap SSP 플러그인 사용 가이드

KT Nasmedia의 Nap SSP SDK를 React Native 앱에 쉽게 통합하여 광고 수익화를 시작할 수 있습니다.

> **참고**: 이 플러그인은 현재 개발 단계이며, 실제 SDK 연동을 위해서는 추가적인 네이티브 설정이 필요할 수 있습니다.

---

## 📦 1. 설치하기

프로젝트 루트 디렉토리에서 아래 명령어를 실행하여 패키지를 설치합니다.

```bash
npm install react-native-nap-ssp
# 또는 yarn을 사용하는 경우
yarn add react-native-nap-ssp
```

---

## ⚙️ 2. 플랫폼별 추가 설정 (필수)

광고 SDK가 정상적으로 작동하려면 각 플랫폼별 설정이 필요합니다.

### 🤖 Android (안드로이드)

1. `android/app/build.gradle` 파일의 `dependencies` 섹션에 아래 내용을 추가하세요.
```gradle
dependencies {
    // Nap SSP SDK 본체
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.21'
    
    // 필요한 미디에이션 어댑터 (선택 사항)
    implementation 'io.github.nasmedia-tech:admixer-admanager:1.0.14'
    implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.10'
}
```

2. `gradle.properties` 파일에 아래 설정을 추가하여 실제 SDK 기능을 활성화합니다.
```properties
napSsp.enableVendorSdk=true
```

### 🍎 iOS (아이폰)

1. `ios/Podfile` 파일에 아래 내용을 추가하세요.
```ruby
pod 'AdMixerMediation'
pod 'AdMixerMediationGAM' # Google Ad Manager 사용 시
```

2. 터미널에서 `pod install` 명령어를 실행합니다.
```bash
cd ios && pod install && cd ..
```

---

## 💻 3. 사용 방법 (예제 코드)

초보 개발자도 바로 따라 할 수 있는 기본적인 사용 패턴입니다.

### ✅ 1단계: SDK 초기화
앱의 최상위 컴포넌트(예: `App.tsx`)에서 앱 시작 시점에 한 번만 호출합니다.

```tsx
import React, { useEffect } from 'react';
import { NapSspAd } from 'react-native-nap-ssp';

const App = () => {
  useEffect(() => {
    const initSDK = async () => {
      try {
        await NapSspAd.initialize({
          mediaKey: '발급받은_미디어_키',
          adUnitIds: ['배너_ID', '전면_ID'],
          logLevel: 'debug', // 개발 중에는 debug로 설정하면 로그를 볼 수 있습니다.
        });
        console.log('Nap SSP SDK 초기화 성공!');
      } catch (error) {
        console.error('초기화 실패:', error);
      }
    };

    initSDK();
  }, []);

  return (
    // ... 앱 화면 ...
  );
};
```

### 🖼️ 2단계: 배너 광고 노출
원하는 화면의 적절한 위치에 `BannerAd` 컴포넌트를 배치합니다.

```tsx
import { BannerAd } from 'react-native-nap-ssp';

const MyScreen = () => {
  return (
    <BannerAd
      adUnitId="발급받은_배너_단위_ID"
      size="BANNER_320x50" // 광고 크기 설정
      onAdLoaded={() => console.log('배너 광고 로드됨')}
      onAdFailedToLoad={(error) => console.log('광고 로드 실패:', error.message)}
    />
  );
};
```

### 🎬 3단계: 전면 광고 띄우기
버튼 클릭이나 화면 전환 시점에 전면 광고를 실행합니다.

```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

const showFullAd = async () => {
  const interstitial = new InterstitialAd('발급받은_전면_단위_ID');
  
  try {
    await interstitial.load(); // 광고를 먼저 불러옵니다.
    await interstitial.show(); // 불러온 광고를 화면에 띄웁니다.
  } catch (error) {
    console.error('전면 광고 오류:', error);
  }
};
```

---

## 🛠️ 주요 API 요약

| 기능 | 메서드 / 컴포넌트 | 설명 |
| :--- | :--- | :--- |
| **초기화** | `NapSspAd.initialize()` | 앱 시작 시 미디어 키와 설정을 등록합니다. |
| **배너** | `<BannerAd />` | 뷰 형태의 배너 광고를 화면에 표시합니다. |
| **전면** | `InterstitialAd` | 화면 전체를 덮는 광고를 제어합니다. |
| **보상형** | `RewardedAd` | 광고 시청 후 보상을 주는 광고를 제어합니다. |

---

## ❓ 도움말 (FAQ)

- **광고가 왜 안 나오나요?**
  - 발급받은 `mediaKey`와 `adUnitId`가 정확한지 확인해 주세요.
  - 실제 기기에서 테스트 중인지, 인터넷이 연결되어 있는지 확인하세요.
- **TypeScript 에러가 발생해요.**
  - 본 패키지는 TypeScript를 기본으로 지원합니다. `lib` 폴더가 생성되었는지 확인해 주세요.
