# 🚀 React Native Nap SSP 플러그인 (v0.1.2)

KT Nasmedia Nap SSP SDK를 React Native에서 쓰기 위한 플러그인입니다.

- 버전: `0.1.2`
- 지원 목표: Android / iOS
- 제공 형태: Native Module + Native View
- 현재 중심 기능: 초기화, 배너, 전면, 전면 동영상, 리워드, 네이티브, 비디오
- 현재 미지원: Bizboard 전용 RN surface
- 예제 앱은 위 주요 흐름을 모두 커버하면서, 네이티브 브리지 없이도 화면이 렌더되는 플레이스홀더 모드도 보여줍니다.

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

## 초보자를 위한 필수 설정

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

> 주의: 현재 SPM 경로는 구조는 갖춰져 있지만 `ios/Package.swift` checksum placeholder가 아직 남아 있어, CocoaPods보다 덜 검증된 경로입니다.

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
  
  // 가장 중요한 '보상 지급' 이벤트 리스너! 
  // 주의: 공식 SDK 스펙에 따라 item의 상세 데이터(type, amount)는 제공되지 않거나 기본값일 수 있습니다.
  reward.addAdEventListener('onRewarded', (item) => {
    console.log(`🎉 보상 지급 이벤트 발생! (데이터는 네이티브 SDK 스펙에 의존합니다)`);
    // 정확한 보상 처리는 S2S 콜백 사용을 강력 권장합니다.

  });

  reward.addAdEventListener('closed', () => {
    // 주의: 보상을 받지 않고 중간에 닫아도 호출됩니다.
    console.log('리워드 광고 창이 닫혔습니다.');
  });

  await reward.load();
  await reward.show();
};
```

---

## ✅ 테스트와 검증

이 저장소에는 네이티브 없이도 JS API가 깨지지 않는지 확인하는 smoke test가 들어 있습니다.

```bash
npm run verify
```

- `typecheck`: TypeScript 타입 확인
- `build`: `lib/` 생성 확인
- `smoke:test`: 공개 API 및 기본 초기화 흐름 확인

## 📚 Documentation (가이드 문서)

프로젝트의 상세 가이드와 검증 리포트는 `docs/` 디렉토리에 체계적으로 정리되어 있습니다.

- **[Maestro 검증 가이드](./docs/MAESTRO_GUIDE.md)**: 자동화 테스트 환경 설정 및 실행 방법
- **[전수 조사 보고서](./docs/REVIEW_REPORT.md)**: 플러그인 구현 현황 및 기술 부채 요약
- **[검증 계획서](./docs/REVIEW_PLAN.md)**: SDK 가이드 대비 기능 체크리스트 및 검증 단계
- **[SPM 통합 가이드](./docs/SPM_GUIDE.md)**: Swift Package Manager 연동 및 제약 사항
- **[NPM 배포 가이드](./docs/PUBLISH_GUIDE.md)**: 플러그인 배포 절차 및 체크리스트
- **[네이티브 SDK 가이드](./docs/native_guides/)**: 나스미디어 네이티브 SDK 원본 가이드 (Android/iOS)

---

## 지원 범위 메모

- 공식 Android/iOS 네이티브 가이드에는 Bizboard 관련 내용이 포함되어 있지만, 현재 React Native 공개 API에서는 Bizboard 전용 타입이나 컴포넌트를 아직 제공하지 않습니다.
- 즉, 현재 RN 패키지 기준 지원 범위는 배너, 전면, 전면 동영상, 리워드, 네이티브, 인라인 비디오입니다.

## ❓ 자주 묻는 질문 (FAQ)

- **Q. `NapSsp XXX is not linked` 에러가 발생합니다.**
  - A. 패키지 설치 후 네이티브 빌드를 다시 해야 합니다. 안드로이드는 안드로이드 스튜디오에서 `Sync Project with Gradle Files`를 실행하거나 `npx react-native run-android`를 다시 실행하세요. iOS는 `cd ios && pod install`을 꼭 해주세요.
  
- **Q. 광고 로드가 안 되고 실패(Failed to load)가 뜹니다.**
  - A. 발급받은 `mediaKey`와 `adUnitId`가 정확한지 확인하세요. 파트너 사이트에 등록된 정보와 일치해야 합니다. 또한, 테스트 중에는 실제 인터넷 연결이 필수입니다.

- **Q. iOS에서 시뮬레이터로 돌리는데 안 나옵니다.**
  - A. 광고 SDK(특히 미디에이션 된 타사 네트워크들) 중 일부는 실기기(Real Device) 환경에서만 정상적으로 로드되는 경우가 많습니다. 가급적 실기기에서 테스트해 주세요.

- **Q. 저장소의 integration-test-app은 바로 실행 가능한가요?**
  - A. 현재 데모 코드와 Podfile은 포함되어 있지만, 저장소 상태만으로는 iOS 네이티브 프로젝트 파일이 없어 즉시 `run-ios`가 되는 완성형 앱은 아닙니다. 통합 검증용 베이스로 보고 추가 scaffold 정리가 필요합니다.

---

> **문의:** nap_adx@nasmedia.co.kr (나스미디어 SDK 운영팀)
