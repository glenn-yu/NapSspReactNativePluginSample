# 🚀 React Native Nap SSP 플러그인 (v0.1.1)

KT Nasmedia의 Nap SSP SDK를 React Native 앱에 쉽게 통합하여 광고 수익화를 시작할 수 있는 공식 비공식 플러그인입니다. 

Android와 iOS용 Native SDK 연동을 목표로 하며, 현재 6가지 광고 포맷을 중심으로 React Native 인터페이스를 제공합니다.

> **참고**: 이 플러그인은 아직 개발 단계입니다. 실제 SDK 연동과 플랫폼별 빌드를 위해 추가적인 네이티브 설정과 환경 구성이 필요할 수 있습니다.

---

## 📦 1. 설치하기

```bash
npm install react-native-nap-ssp
# 또는
yarn add react-native-nap-ssp
```

> **주의:** 이 플러그인은 React Native 0.72 이상 버전을 권장합니다.

---

## ⚙️ 2. 네이티브 필수 설정

광고 SDK가 정상적으로 작동하려면 플랫폼별 필수 설정이 필요합니다.

### 🤖 Android (안드로이드)
`android/app/build.gradle` 파일의 `dependencies`에 아래 내용을 추가하세요.
```gradle
dependencies {
    // (필수) Nap SSP SDK 본체
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.21'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.9.0'
    
    // (선택) 미디에이션 어댑터 (원하는 네트워크만 추가)
    implementation 'io.github.nasmedia-tech:admixer-admanager:1.0.14'
    implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.10'
    implementation 'io.github.nasmedia-tech:admixer-applovin:1.0.8'
    // ... 기타 네트워크 어댑터
}
```

### 🍎 iOS (아이폰)

iOS 환경에서는 **CocoaPods**와 **SPM(Swift Package Manager)** 두 가지 방식을 모두 지원합니다. 편한 방식을 선택하세요.

#### 옵션 A: CocoaPods 사용 (React Native 기본 권장)
`ios/Podfile` 파일에 아래 내용을 추가하고 `pod install`을 실행하세요.
```ruby
# (필수) Nap SSP SDK 본체
pod 'AdMixerMediation'

# (선택) 미디에이션 어댑터
pod 'AdMixerMediationGAM'      # Google AdManager
pod 'AdMixerMediationAdFit'    # Kakao AdFit
pod 'AdMixerMediationAppLovin' # AppLovin
# ... 기타 네트워크 어댑터
```

#### 옵션 B: SPM (Swift Package Manager) 사용
Xcode를 열고 다음 단계를 진행합니다.
1. `File` ➡️ `Add Packages...` ➡️ `Add Local Package`를 선택합니다.
2. `node_modules/react-native-nap-ssp/ios` 폴더를 선택하여 로컬 패키지를 추가합니다.
3. 앱 프로젝트의 `Package.swift`에 나스미디어 Vendor SDK XCFramework를 `binaryTarget`으로 추가하거나 수동으로 링킹합니다. (자세한 방법은 패키지 내 `docs/spm_integration.md` 참조)

---

## 💻 3. 초보자도 따라하기 쉬운 사용 가이드

앱에서 광고를 띄우려면 **"1. 초기화 ➡️ 2. 화면에 표시"** 딱 2단계만 거치면 됩니다.

### ✅ 1단계: SDK 초기화 (앱 시작 시 1번만)
앱의 최상위 파일(`App.tsx` 또는 `index.js`)에서 발급받은 미디어 키와 사용할 모든 광고 ID를 등록합니다.

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
      logLevel: 'debug', // 로그 확인용
    }).then(() => console.log('✅ SDK 초기화 성공!'));
  }, []);

  return <Home />;
}
```

---

### 🖼️ 2단계: 화면에 그리는 광고 (배너, 네이티브, 비디오)
화면의 특정 영역을 차지하는 광고입니다. 원하는 곳에 컴포넌트를 배치하기만 하면 됩니다.

#### 1) 배너 광고 (Banner)
```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd
  adUnitId="BANNER_ID"
  size="BANNER_320x50" // BANNER_300x250, SMART_BANNER 등 지원
  style={{ marginTop: 20 }}
  onAdLoaded={() => console.log('배너 로드 성공!')}
  onAdFailedToLoad={(error) => console.log('배너 로드 실패:', error.message)}
  onAdClicked={() => console.log('배너 클릭됨!')}
/>
```

#### 2) 네이티브 광고 (Native Ad)
네이티브 앱의 UI와 자연스럽게 어울리는 광고입니다. (안드로이드 XML / iOS xib 파일 필요)
```tsx
import { NativeAd } from 'react-native-nap-ssp';

<NativeAd
  adUnitId="NATIVE_ID"
  style={{ width: '100%', height: 250 }} // 높이를 넉넉하게 주세요
  onAdLoaded={() => console.log('네이티브 광고 로드됨')}
  onAdClicked={() => console.log('네이티브 광고 클릭됨!')}
/>
```

#### 3) 인라인 동영상 광고 (Video Ad)
앱 화면 안에서 바로 재생되는 동영상 광고입니다.
```tsx
import { VideoAd } from 'react-native-nap-ssp';

<VideoAd
  adUnitId="VIDEO_ID"
  style={{ width: '100%', height: 200 }}
  onAdLoaded={() => console.log('동영상 광고 준비 완료')}
  onAdCompleted={() => console.log('동영상 끝까지 시청 완료! (보상 지급 아님)')}
  onAdSkipped={() => console.log('사용자가 동영상을 스킵함')}
  onAdClicked={() => console.log('동영상 더보기(클릭) 버튼 눌림')}
/>
```

---

### 🎬 3단계: 화면 전체를 덮는 광고 (전면, 리워드)
버튼을 눌렀을 때나 스테이지가 끝났을 때 띄우는 팝업형 광고입니다.

#### 1) 전면 광고 (Interstitial) - 팝업/카운트다운 옵션 지원!
```tsx
import { InterstitialAd } from 'react-native-nap-ssp';

const showInterstitial = async () => {
  // 고급 옵션: 팝업 형태로 5초 카운트다운 후 닫기 버튼 표시
  const inter = new InterstitialAd('INTER_ID', {
    type: 'popup', 
    countDownTime: 5,
    buttonLeftText: '닫기',
  });

  // 이벤트 리스너 등록 (닫힘, 클릭 등)
  inter.addAdEventListener('closed', () => console.log('전면 광고가 닫혔습니다.'));
  inter.addAdEventListener('clicked', () => console.log('전면 광고가 클릭되었습니다.'));
  
  try {
    await inter.load(); // 1. 광고 불러오기
    await inter.show(); // 2. 화면에 띄우기
  } catch (error) {
    console.error('전면 광고 실패:', error);
  }
};
```

#### 2) 전면 동영상 광고 (Interstitial Video)
화면 전체를 덮고 재생되는 동영상 광고입니다. (리워드와 달리 보상은 없습니다)
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

#### 3) 리워드 동영상 광고 (Rewarded) - S2S 콜백 지원!
광고를 끝까지 보면 유저에게 아이템이나 포인트를 지급할 때 사용합니다.
```tsx
import { RewardedAd } from 'react-native-nap-ssp';

const showRewardedAd = async () => {
  // 고급 옵션: 매체사 서버(S2S) 콜백용 커스텀 파라미터 전달
  const reward = new RewardedAd('REWARD_ID', {
    customParams: { useid: 'user123', name: '홍길동' },
    mute: true // (안드로이드 전용) 시작 시 음소거
  });
  
  // 보상 지급 이벤트 (SDK 스펙에 따라 item 데이터는 제공되지 않을 수 있음)
  reward.addAdEventListener('onRewarded', () => {
    console.log('🎉 보상 지급 이벤트 발생!');
    // 정확한 보상 처리는 S2S 콜백 이용 권장
  });

  reward.addAdEventListener('closed', () => {
    // 주의: 보상을 받지 않고 중간에 닫아도 호출됩니다. 
    // 보상 처리는 반드시 'onRewarded' 이벤트에서 해야 합니다.
    console.log('리워드 광고 창이 닫혔습니다.');
  });

  await reward.load();
  await reward.show();
};
```

---

## ❓ 자주 묻는 질문 (FAQ)

- **Q. `NapSsp XXX is not linked` 에러가 발생합니다.**
  - A. 패키지 설치 후 네이티브 빌드를 다시 해야 합니다. 안드로이드는 안드로이드 스튜디오에서 `Sync Project with Gradle Files`를 실행하거나 `npx react-native run-android`를 다시 실행하세요. iOS는 `cd ios && pod install`을 꼭 해주세요.
  
- **Q. 광고 로드가 안 되고 실패(Failed to load)가 뜹니다.**
  - A. 발급받은 `mediaKey`와 `adUnitId`가 정확한지 확인하세요. 파트너 사이트에 등록된 정보와 일치해야 합니다. 또한, 테스트 중에는 실제 인터넷 연결이 필수입니다.

- **Q. iOS에서 시뮬레이터로 돌리는데 안 나옵니다.**
  - A. 광고 SDK(특히 미디에이션 된 타사 네트워크들) 중 일부는 실기기(Real Device) 환경에서만 정상적으로 로드되는 경우가 많습니다. 가급적 실기기에서 테스트해 주세요.

---

> **문의:** nap_adx@nasmedia.co.kr (나스미디어 SDK 운영팀)
