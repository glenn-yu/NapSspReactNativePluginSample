# react-native-nap-ssp v0.1.7 — 광고 형식별 회귀 테스트 가이드

> **대상**: 실기기(Android / iOS)에서 직접 수행하는 수동 회귀 테스트  
> **버전**: react-native-nap-ssp v0.1.7  
> **작성일**: 2026-05-08  
> **테스터**: 이 문서만으로 추가 컨텍스트 없이 테스트를 완료할 수 있어야 합니다.

---

## 목차

1. [사전 준비 — 공통 설정](#0-사전-준비--공통-설정)
2. [BannerAd — 배너 광고](#1-bannerad--배너-광고)
3. [NativeAd — 네이티브 광고](#2-nativead--네이티브-광고)
4. [VideoAd — 인라인 비디오 광고](#3-videoad--인라인-비디오-광고)
5. [InterstitialAd — 전면 광고](#4-interstitialad--전면-광고)
6. [InterstitialVideoAd — 전면 비디오 광고](#5-interstitialvideoad--전면-비디오-광고)
7. [RewardedAd — 리워드 광고](#6-rewardedad--리워드-광고)
8. [공통 예외 시나리오](#7-공통-예외-시나리오)

---

## 0. 사전 준비 — 공통 설정

모든 광고 테스트 전에 아래 설정을 완료해야 합니다.

### 0-1. 테스트 환경 확인

- [ ] 물리 기기 준비 (에뮬레이터/시뮬레이터 불가 — 광고가 실제로 채워지지 않을 수 있음)
- [ ] 인터넷 연결 확인 (Wi-Fi 또는 LTE)
- [ ] nap SSP 파트너사이트에서 발급받은 `mediaKey` 및 각 광고 단위 ID 확인
- [ ] 앱 빌드 후 기기 설치 완료

### 0-2. iOS ATT 권한 요청 (iOS 14+ 전용)

iOS 14 이상 기기에서는 `NapSspAd.initialize()` 호출 **전에** 트래킹 권한을 요청해야 합니다.  
권한을 거부하면 광고 타게팅 정확도가 낮아지고 일부 광고가 채워지지 않을 수 있습니다.

#### 샘플 코드

```typescript
import { NapSspAd } from 'react-native-nap-ssp';
import { Platform } from 'react-native';

async function requestAttAndInit() {
  // iOS 14+에서만 ATT 권한 요청
  if (Platform.OS === 'ios') {
    const status = await NapSspAd.requestTrackingAuthorization();
    console.log('[ATT] 트래킹 권한 상태:', status);
    // 반환값: 'authorized' | 'denied' | 'restricted' | 'notDetermined'
  }

  await initSdk();
}
```

#### 절차

- [ ] 앱 최초 실행 시 ATT 권한 다이얼로그가 표시되는지 확인
- [ ] "추적 허용" 선택 → `status === 'authorized'` 로그 확인
- [ ] 앱 재설치 후 "추적 안 함" 선택 → `status === 'denied'` 로그 확인, 앱 크래시 없음 확인

#### 합격 기준

- [ ] `authorized` / `denied` / `restricted` 어느 상태에서도 앱이 크래시되지 않음
- [ ] 반환된 `status` 문자열이 콘솔에 출력됨

---

### 0-3. NapSspAd.initialize() — SDK 초기화

모든 광고 테스트 시작 전 **한 번만** 호출합니다.

#### 샘플 코드

```typescript
import { NapSspAd } from 'react-native-nap-ssp';

// Android 예시 ID (실제 발급받은 값으로 교체)
const ANDROID_CONFIG = {
  mediaKey: '10771',
  banner:            '104701',
  interstitial:      '104703',
  rewarded:          '103722',
  native:            '104588',
  video:             '104589',
  interstitialVideo: '104591',
};

// iOS 예시 ID (실제 발급받은 값으로 교체)
const IOS_CONFIG = {
  mediaKey: '10347',
  banner:            '103790',
  interstitial:      '104707',
  rewarded:          '104710',
  native:            '101626',
  video:             '104709',
  interstitialVideo: '103868',
};

const config = Platform.OS === 'android' ? ANDROID_CONFIG : IOS_CONFIG;

async function initSdk() {
  try {
    const status = await NapSspAd.initialize({
      mediaKey: config.mediaKey,
      adUnitIds: [
        config.banner,
        config.interstitial,
        config.rewarded,
        config.native,
        config.video,
        config.interstitialVideo,
      ],
      logLevel: 'debug',  // 테스트 중에는 'debug' 사용
    });
    console.log('[INIT] SDK 초기화 성공:', status.initialized);
  } catch (e) {
    console.error('[INIT] SDK 초기화 실패:', e);
  }
}
```

#### 절차

- [ ] `initSdk()` 호출
- [ ] 콘솔에서 `[INIT] SDK 초기화 성공: true` 확인
- [ ] `NapSspAd.getStatus()` 추가 호출로 상태 검증

#### getStatus() 검증 코드

```typescript
const status = await NapSspAd.getStatus();
console.log('[STATUS] initialized:', status.initialized);
console.log('[STATUS] configuredAdUnitIds:', status.configuredAdUnitIds);
console.log('[STATUS] logLevel:', status.logLevel);
// iOS 전용
console.log('[STATUS] trackingAuthorizationStatus:', status.trackingAuthorizationStatus);
```

#### 합격 기준

- [ ] `status.initialized === true`
- [ ] `status.configuredAdUnitIds`에 전달한 모든 광고 단위 ID가 포함됨
- [ ] 초기화 중 또는 이후 앱 크래시 없음

#### 실패 패턴

| 증상 | 가능한 원인 |
|------|------------|
| `mediaKey` 오류 | 파트너사이트에서 발급받지 않은 키 사용 |
| 네트워크 타임아웃 | 기기 인터넷 연결 불량 |
| `initialized === false` | `adUnitIds` 배열이 비어 있거나 잘못된 형식 |

---

## 1. BannerAd — 배너 광고

### 샘플 코드

```typescript
import React, { useState } from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { BannerAd } from 'react-native-nap-ssp';

const BANNER_UNIT_ID = '104701'; // 실제 ID로 교체

export default function BannerTestScreen() {
  const [visible, setVisible] = useState(false);
  const [key, setKey] = useState(0);
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) =>
    setLog(prev => [`${new Date().toLocaleTimeString()} ${msg}`, ...prev]);

  return (
    <View style={{ flex: 1, padding: 16 }}>
      <TouchableOpacity
        onPress={() => { setVisible(true); setKey(k => k + 1); }}
        style={{ backgroundColor: '#1565C0', padding: 12, borderRadius: 8, marginBottom: 12 }}>
        <Text style={{ color: '#fff', textAlign: 'center' }}>배너 로드</Text>
      </TouchableOpacity>

      {visible && (
        <BannerAd
          key={key}
          adUnitId={BANNER_UNIT_ID}
          size="BANNER_320x50"
          onAdLoaded={() => addLog('[BANNER] onAdLoaded')}
          onAdFailedToLoad={e => addLog(`[BANNER] onAdFailedToLoad: ${e.code} / ${e.message}`)}
          onAdClicked={() => addLog('[BANNER] onAdClicked')}
          onAdImpression={() => addLog('[BANNER] onAdImpression')}
          onAdOpened={() => addLog('[BANNER] onAdOpened')}
          onAdClosed={() => addLog('[BANNER] onAdClosed')}
        />
      )}

      {log.map((l, i) => (
        <Text key={i} style={{ fontSize: 11, color: '#333', marginTop: 4 }}>{l}</Text>
      ))}
    </View>
  );
}
```

---

### TC-BAN-01: 정상 로드 및 표시

**사전 조건**: SDK 초기화 완료 (섹션 0 참고)

#### 절차

- [ ] `size="BANNER_320x50"` 으로 `<BannerAd>` 렌더링
- [ ] 화면에 320×50 픽셀 크기의 배너 광고가 표시되는지 육안 확인
- [ ] 콘솔에서 `onAdLoaded` 이벤트 로그 확인

#### 합격 기준

- [ ] 화면에 광고 크기에 맞는 배너 이미지/소재가 표시됨
- [ ] `onAdLoaded` 콜백이 호출됨
- [ ] `onAdImpression` 콜백이 호출됨 (광고가 화면에 노출된 후)
- [ ] 앱 크래시 없음

---

### TC-BAN-02: 다양한 배너 크기

#### 절차 (각 사이즈별로 반복)

- [ ] `size="BANNER_320x50"` → 320×50 광고 표시 확인
- [ ] `size="BANNER_320x100"` → 320×100 광고 표시 확인
- [ ] `size="BANNER_300x250"` (MEDIUM_RECTANGLE) → 300×250 광고 표시 확인
- [ ] `size="SMART_BANNER"` → 화면 너비에 맞게 표시 확인
- [ ] `size="BANNER_360x230"` (NaverAdManager 전용) → 360×230 광고 표시 확인

#### 합격 기준

- [ ] 각 사이즈 문자열에서 `onAdLoaded`가 호출됨
- [ ] 렌더링된 뷰가 지정한 크기와 일치함

---

### TC-BAN-03: 이벤트 콜백 전체 확인

#### 절차

- [ ] 배너 광고 로드
- [ ] `onAdLoaded` 로그 확인
- [ ] `onAdImpression` 로그 확인 (광고 뷰가 화면에 보이면 자동 발생)
- [ ] 배너 광고 소재 탭(클릭) → `onAdClicked` 로그 확인
- [ ] 광고 클릭으로 외부 브라우저/앱으로 이동한 경우 `onAdOpened` 로그 확인
- [ ] 외부 화면에서 돌아온 경우 `onAdClosed` 로그 확인

#### 합격 기준

- [ ] `onAdLoaded`, `onAdImpression`, `onAdClicked`, `onAdOpened`, `onAdClosed` 모두 호출됨
- [ ] 각 콜백이 한 번씩만 호출됨 (중복 발화 없음)

---

### TC-BAN-04: 잘못된 adUnitId — 우아한 실패

#### 샘플 코드

```typescript
// 존재하지 않는 ID 사용
<BannerAd
  adUnitId="999999"
  size="BANNER_320x50"
  onAdFailedToLoad={e => console.log('[BANNER] 실패 코드:', e.code, '메시지:', e.message)}
/>
```

#### 절차

- [ ] 실제로 존재하지 않는 adUnitId(예: `"999999"`) 로 `BannerAd` 렌더링
- [ ] 로그에서 `onAdFailedToLoad` 이벤트 확인
- [ ] `error.code` 및 `error.message` 값 기록

#### 합격 기준

- [ ] `onAdFailedToLoad` 콜백이 호출됨
- [ ] `AdError.code`와 `AdError.message`가 비어있지 않음
- [ ] 앱이 크래시되지 않음
- [ ] 빈 공간 또는 플레이스홀더가 표시됨 (UI 레이아웃 깨짐 없음)

#### 실패 패턴

| 증상 | 원인 |
|------|------|
| `onAdFailedToLoad` 대신 앱 종료 | 네이티브 예외 처리 누락 |
| 에러 콜백 미호출 | 이벤트 리스너가 올바르게 연결되지 않음 |
| 레이아웃 뷰 크기 0 | `style` prop 미지정 |

---

### TC-BAN-05: 배너 재로드

#### 절차

- [ ] 배너 로드 후 표시 확인
- [ ] `key` prop을 변경하여 컴포넌트 재마운트 (재로드 트리거)
- [ ] 새 광고가 로드되고 `onAdLoaded` 재호출 확인

#### 합격 기준

- [ ] 재로드마다 `onAdLoaded`가 호출됨
- [ ] 이전 광고 인스턴스가 남아 메모리 누수가 없음

---

## 2. NativeAd — 네이티브 광고

### 샘플 코드

```typescript
import React, { useState } from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { NativeAd } from 'react-native-nap-ssp';

const NATIVE_UNIT_ID = '104588'; // 실제 ID로 교체

export default function NativeAdTestScreen() {
  const [visible, setVisible] = useState(false);
  const [key, setKey] = useState(0);
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) =>
    setLog(prev => [`${new Date().toLocaleTimeString()} ${msg}`, ...prev]);

  return (
    <View style={{ flex: 1, padding: 16 }}>
      <TouchableOpacity
        onPress={() => { setVisible(true); setKey(k => k + 1); }}
        style={{ backgroundColor: '#2E7D32', padding: 12, borderRadius: 8, marginBottom: 12 }}>
        <Text style={{ color: '#fff', textAlign: 'center' }}>네이티브 광고 로드</Text>
      </TouchableOpacity>

      {visible && (
        <NativeAd
          key={key}
          adUnitId={NATIVE_UNIT_ID}
          style={{ width: '100%', height: 250 }}
          onAdLoaded={() => addLog('[NATIVE] onAdLoaded')}
          onAdFailedToLoad={e => addLog(`[NATIVE] onAdFailedToLoad: ${e.code} / ${e.message}`)}
          onAdClicked={() => addLog('[NATIVE] onAdClicked')}
          onAdImpression={() => addLog('[NATIVE] onAdImpression')}
          onAdOpened={() => addLog('[NATIVE] onAdOpened')}
          onAdClosed={() => addLog('[NATIVE] onAdClosed')}
        />
      )}

      {log.map((l, i) => (
        <Text key={i} style={{ fontSize: 11, color: '#333', marginTop: 4 }}>{l}</Text>
      ))}
    </View>
  );
}
```

---

### TC-NAT-01: 정상 로드 및 표시

**사전 조건**: SDK 초기화 완료

#### 절차

- [ ] `style={{ width: '100%', height: 250 }}` 지정 후 `<NativeAd>` 렌더링
- [ ] 광고 소재(이미지, 텍스트, CTA 버튼 등)가 뷰 내에 표시되는지 육안 확인

#### 합격 기준

- [ ] `onAdLoaded` 콜백이 호출됨
- [ ] `onAdImpression` 콜백이 호출됨
- [ ] 네이티브 광고 소재가 지정한 뷰 영역 안에 표시됨
- [ ] 앱 크래시 없음

---

### TC-NAT-02: 이벤트 콜백 전체 확인

#### 절차

- [ ] 네이티브 광고 로드 → `onAdLoaded` 로그 확인
- [ ] 뷰가 화면에 보임 → `onAdImpression` 로그 확인
- [ ] 광고 소재 탭 → `onAdClicked` 로그 확인
- [ ] 외부 이동 발생 시 → `onAdOpened` 로그 확인
- [ ] 복귀 시 → `onAdClosed` 로그 확인

#### 합격 기준

- [ ] 위 이벤트 5종이 각 시나리오에서 한 번씩 호출됨

---

### TC-NAT-03: 잘못된 adUnitId — 우아한 실패

#### 절차

- [ ] 잘못된 ID로 `NativeAd` 렌더링
- [ ] `onAdFailedToLoad` 호출 확인
- [ ] 앱 크래시 없음 확인

#### 합격 기준

- [ ] `onAdFailedToLoad(error)` 호출, `error.code` 및 `error.message` 비어있지 않음
- [ ] 뷰 영역이 빈 상태이거나 플레이스홀더로 처리됨
- [ ] 앱 크래시 없음

---

## 3. VideoAd — 인라인 비디오 광고

### 샘플 코드

```typescript
import React, { useState } from 'react';
import { View, Text, TouchableOpacity, Platform } from 'react-native';
import { VideoAd } from 'react-native-nap-ssp';

const VIDEO_UNIT_ID = '104589'; // 실제 ID로 교체

export default function VideoAdTestScreen() {
  const [visible, setVisible] = useState(false);
  const [key, setKey] = useState(0);
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) =>
    setLog(prev => [`${new Date().toLocaleTimeString()} ${msg}`, ...prev]);

  return (
    <View style={{ flex: 1, padding: 16 }}>
      <TouchableOpacity
        onPress={() => { setVisible(true); setKey(k => k + 1); }}
        style={{ backgroundColor: '#E65100', padding: 12, borderRadius: 8, marginBottom: 12 }}>
        <Text style={{ color: '#fff', textAlign: 'center' }}>비디오 로드</Text>
      </TouchableOpacity>

      {visible && (
        <VideoAd
          key={key}
          adUnitId={VIDEO_UNIT_ID}
          style={{ width: '100%', height: 200 }}
          // Android 전용: 실패 시 자동 재시도 여부
          isRetry={Platform.OS === 'android' ? false : undefined}
          onAdLoaded={() => addLog('[VIDEO] onAdLoaded')}
          onAdFailedToLoad={e => addLog(`[VIDEO] onAdFailedToLoad: ${e.code} / ${e.message}`)}
          onAdImpression={() => addLog('[VIDEO] onAdImpression')}
          onAdClicked={() => addLog('[VIDEO] onAdClicked')}
          onAdCompleted={() => addLog('[VIDEO] onAdCompleted ✅')}
          onAdSkipped={() => addLog('[VIDEO] onAdSkipped')}
          onAdOpened={() => addLog('[VIDEO] onAdOpened')}
          onAdClosed={() => addLog('[VIDEO] onAdClosed')}
        />
      )}

      {log.map((l, i) => (
        <Text key={i} style={{ fontSize: 11, color: '#333', marginTop: 4 }}>{l}</Text>
      ))}
    </View>
  );
}
```

---

### TC-VID-01: 정상 로드 및 재생

**사전 조건**: SDK 초기화 완료

#### 절차

- [ ] `<VideoAd>` 렌더링 (style 지정 필수: `width: '100%', height: 200`)
- [ ] 비디오 광고 소재가 뷰 안에서 자동 재생 시작하는지 확인

#### 합격 기준

- [ ] `onAdLoaded` 콜백 호출됨
- [ ] `onAdImpression` 콜백 호출됨
- [ ] 비디오가 뷰 안에서 재생됨
- [ ] 앱 크래시 없음

---

### TC-VID-02: 비디오 완료 이벤트

#### 절차

- [ ] 비디오 광고 재생 시작 확인
- [ ] 스킵 버튼을 누르지 않고 영상 끝까지 시청
- [ ] 콘솔에서 `onAdCompleted` 로그 확인

#### 합격 기준

- [ ] 영상 끝까지 시청 시 `onAdCompleted` 콜백이 호출됨
- [ ] `onAdSkipped`는 호출되지 않음

---

### TC-VID-03: 비디오 스킵 이벤트

#### 절차

- [ ] 비디오 광고 재생 중 스킵 버튼 탭
- [ ] 콘솔에서 `onAdSkipped` 로그 확인

#### 합격 기준

- [ ] 스킵 버튼 탭 시 `onAdSkipped` 콜백이 호출됨
- [ ] `onAdCompleted`는 호출되지 않음

---

### TC-VID-04: isRetry prop — Android 전용

**대상 플랫폼**: Android 전용

#### 절차

- [ ] `isRetry={true}` 로 `VideoAd` 렌더링
- [ ] 의도적으로 광고 채움이 낮은 환경(네트워크 제한 또는 잘못된 ID)에서 테스트
- [ ] 재시도 여부에 따라 `onAdLoaded` 또는 `onAdFailedToLoad` 확인

- [ ] `isRetry={false}` (기본값)로 동일 테스트 반복
- [ ] 실패 시 재시도 없이 즉시 `onAdFailedToLoad` 호출 확인

#### 합격 기준

- [ ] `isRetry={true}`: 실패 후 SDK 내부에서 재시도하며 앱 크래시 없음
- [ ] `isRetry={false}`: 실패 시 즉시 `onAdFailedToLoad` 호출, 재시도 없음

---

### TC-VID-05: 이벤트 콜백 전체 확인

#### 절차

- [ ] 비디오 광고 로드 → `onAdLoaded` 확인
- [ ] 재생 시작 → `onAdImpression` 확인
- [ ] 비디오 소재 탭 → `onAdClicked` 확인
- [ ] 영상 완료 → `onAdCompleted` 확인

#### 합격 기준

- [ ] 위 이벤트 4종이 각각의 상황에서 정확히 호출됨

---

## 4. InterstitialAd — 전면 광고

### 샘플 코드

```typescript
import React from 'react';
import { View, Text, TouchableOpacity, Alert, Platform } from 'react-native';
import { InterstitialAd } from 'react-native-nap-ssp';

const INTER_UNIT_ID = '104703'; // 실제 ID로 교체

async function showInterstitial() {
  const ad = new InterstitialAd(INTER_UNIT_ID, {
    type: 'popup',       // 'default' | 'popup' | 'countdown'
    countDownTime: 5,    // type: 'countdown' 일 때 카운트다운 초(기본: 5)
  });

  ad.addAdEventListener('loaded',      () => console.log('[INTER] loaded'));
  ad.addAdEventListener('loadFailed',  e  => console.log('[INTER] loadFailed:', e.code, e.message));
  ad.addAdEventListener('opened',      () => console.log('[INTER] opened'));
  ad.addAdEventListener('closed',      () => { console.log('[INTER] closed'); ad.destroy(); });
  ad.addAdEventListener('clicked',     () => console.log('[INTER] clicked'));
  ad.addAdEventListener('impression',  () => console.log('[INTER] impression'));

  try {
    if (Platform.OS === 'android') {
      // Android: start() 가 load + show 를 한 번에 처리
      await ad.start();
    } else {
      // iOS: load() 후 show() 순서로 호출
      await ad.load();
      await ad.show();
    }
  } catch (e: any) {
    Alert.alert('전면 광고 오류', e?.message ?? String(e));
    ad.destroy();
  }
}

export default function InterstitialTestScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <TouchableOpacity
        onPress={showInterstitial}
        style={{ backgroundColor: '#1565C0', padding: 16, borderRadius: 8 }}>
        <Text style={{ color: '#fff', fontSize: 16 }}>전면 광고 표시</Text>
      </TouchableOpacity>
    </View>
  );
}
```

---

### TC-INT-01: load() → show() 순서 테스트 (iOS)

**대상 플랫폼**: iOS 전용

#### 절차

- [ ] `new InterstitialAd(adUnitId)` 인스턴스 생성
- [ ] `await ad.load()` 호출 → `loaded` 이벤트 확인
- [ ] `await ad.show()` 호출 → 전면 광고 화면 표시 확인
- [ ] `opened`, `impression` 이벤트 확인
- [ ] 광고 닫기 → `closed` 이벤트 확인

#### 합격 기준

- [ ] `loaded` → `opened` → `impression` → `closed` 순서대로 이벤트 발생
- [ ] 전면 광고가 전체 화면으로 표시됨
- [ ] 앱 크래시 없음

---

### TC-INT-02: start() 통합 호출 테스트 (Android)

**대상 플랫폼**: Android 전용

#### 절차

- [ ] `await ad.start()` 한 번 호출
- [ ] 내부적으로 load + show 가 자동 처리됨
- [ ] 전면 광고 표시 확인

#### 합격 기준

- [ ] `start()` 이후 전면 광고가 표시됨
- [ ] `opened`, `impression` 이벤트 호출됨

---

### TC-INT-03: 이벤트 6종 전체 확인

#### 절차

- [ ] `loaded` — `ad.load()` 또는 `ad.start()` 성공 후 확인
- [ ] `loadFailed` — 잘못된 ID로 테스트 (TC-INT-06 참고)
- [ ] `opened` — 광고 화면이 나타날 때 확인
- [ ] `impression` — 광고가 화면에 노출될 때 확인
- [ ] `clicked` — 광고 소재 탭 후 확인
- [ ] `closed` — 닫기 버튼 탭 후 확인

#### 합격 기준

- [ ] 6개 이벤트 모두 해당 시나리오에서 한 번씩 호출됨
- [ ] 이벤트 핸들러 내에서 예외 발생 시에도 앱 크래시 없음

---

### TC-INT-04: 광고 타입 옵션

#### 절차 (각 type별 반복)

- [ ] `type: 'default'` → 기본 전면 광고 표시 확인
- [ ] `type: 'popup'` → 팝업형 전면 광고 표시 확인
- [ ] `type: 'countdown'`, `countDownTime: 3` → 3초 카운트다운 후 닫기 버튼 활성화 확인

#### 합격 기준

- [ ] 각 타입별로 광고 UI가 다르게 표시됨
- [ ] `countdown` 타입에서 지정한 초(`countDownTime`) 동안 닫기 불가능

---

### TC-INT-05: iOS closeButtonTouchAreaRatio 옵션

**대상 플랫폼**: iOS 전용

#### 절차

- [ ] `closeButtonTouchAreaRatio: 0.5` 로 전면 광고 표시
- [ ] 닫기 버튼의 터치 영역이 늘어났는지 확인

#### 합격 기준

- [ ] 광고가 정상 표시됨
- [ ] 앱 크래시 없음

---

### TC-INT-06: show() before load() — 크래시 없음

#### 샘플 코드

```typescript
const ad = new InterstitialAd('104703');
try {
  // load() 없이 show() 바로 호출 (의도적 오용)
  await ad.show();
} catch (e: any) {
  console.log('[INTER] 예상된 오류:', e.message);
  // 'has not been loaded' 오류 메시지 기대
}
```

#### 절차

- [ ] `load()` 없이 `show()` 바로 호출
- [ ] try-catch로 예외 수신 확인

#### 합격 기준

- [ ] 앱이 크래시되지 않음
- [ ] `Error: Interstitial ad "..." has not been loaded.` 예외가 발생함
- [ ] `catch` 블록에서 오류 메시지가 출력됨

---

### TC-INT-07: 잘못된 adUnitId — 우아한 실패

#### 절차

- [ ] 존재하지 않는 ID로 `InterstitialAd` 생성 후 `load()` 호출
- [ ] `loadFailed` 이벤트 수신 확인

#### 합격 기준

- [ ] `loadFailed` 이벤트 호출됨
- [ ] `AdError.code`, `AdError.message` 비어있지 않음
- [ ] 앱 크래시 없음

---

### TC-INT-08: destroy() 호출 후 메모리 정리

#### 절차

- [ ] 광고 표시 후 `closed` 이벤트에서 `ad.destroy()` 호출
- [ ] 동일 인스턴스에서 `ad.load()` 재호출 시도
- [ ] 에러 또는 아무 동작 없음 확인

#### 합격 기준

- [ ] `destroy()` 이후 앱 크래시 없음
- [ ] 이벤트 리스너가 중복 발화되지 않음

---

## 5. InterstitialVideoAd — 전면 비디오 광고

### 샘플 코드

```typescript
import React from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { InterstitialVideoAd } from 'react-native-nap-ssp';

const IV_UNIT_ID = '104591'; // 실제 ID로 교체

async function showInterstitialVideo() {
  const ad = new InterstitialVideoAd(IV_UNIT_ID, {
    timeout: 20,               // 로드 타임아웃(초), 0 = 서버 정의값 사용
    maxRetryCountInSlot: 0,    // -1: 무한 재시도, 0: 재시도 없음, n: n회
  });

  ad.addAdEventListener('loaded',     () => console.log('[IV] loaded'));
  ad.addAdEventListener('loadFailed', e  => console.log('[IV] loadFailed:', e.code, e.message));
  ad.addAdEventListener('opened',     () => console.log('[IV] opened'));
  ad.addAdEventListener('impression', () => console.log('[IV] impression'));
  ad.addAdEventListener('completed',  () => console.log('[IV] completed ✅'));
  ad.addAdEventListener('skipped',    () => console.log('[IV] skipped'));
  ad.addAdEventListener('closed',     () => { console.log('[IV] closed'); ad.destroy(); });
  ad.addAdEventListener('clicked',    () => console.log('[IV] clicked'));

  try {
    await ad.start(); // load + show 통합
  } catch (e: any) {
    Alert.alert('전면 동영상 오류', e?.message ?? String(e));
    ad.destroy();
  }
}

export default function InterstitialVideoTestScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <TouchableOpacity
        onPress={showInterstitialVideo}
        style={{ backgroundColor: '#BF360C', padding: 16, borderRadius: 8 }}>
        <Text style={{ color: '#fff', fontSize: 16 }}>전면 비디오 광고 표시</Text>
      </TouchableOpacity>
    </View>
  );
}
```

---

### TC-IV-01: load() → show() 정상 흐름

#### 절차

- [ ] `ad.start()` 호출 (내부적으로 load + show 처리)
- [ ] 전면 비디오 광고가 전체 화면으로 표시되는지 확인
- [ ] 비디오 재생 시작 확인

#### 합격 기준

- [ ] `loaded` → `opened` → `impression` 이벤트 순서대로 호출됨
- [ ] 전면 비디오 광고가 전체 화면으로 표시됨
- [ ] 앱 크래시 없음

---

### TC-IV-02: 비디오 완료 이벤트

#### 절차

- [ ] 전면 비디오 광고 재생 시작
- [ ] 스킵 없이 영상 끝까지 시청
- [ ] `completed` 이벤트 확인

#### 합격 기준

- [ ] `completed` 콜백이 호출됨
- [ ] `skipped`는 호출되지 않음
- [ ] 이후 `closed` 이벤트 호출됨

---

### TC-IV-03: 비디오 스킵 이벤트

#### 절차

- [ ] 전면 비디오 광고 재생 중 스킵 버튼 탭
- [ ] `skipped` 이벤트 확인

#### 합격 기준

- [ ] `skipped` 콜백이 호출됨
- [ ] `completed`는 호출되지 않음
- [ ] 이후 `closed` 이벤트 호출됨

---

### TC-IV-04: timeout 옵션

#### 절차

- [ ] `timeout: 5` (5초)로 광고 인스턴스 생성
- [ ] 네트워크 속도가 느린 환경에서 5초 이내 광고가 로드되지 않는 경우
- [ ] `loadFailed` 이벤트 수신 확인

#### 합격 기준

- [ ] 타임아웃 경과 시 앱 크래시 없이 `loadFailed` 이벤트가 발생함

---

### TC-IV-05: maxRetryCountInSlot 옵션

#### 절차

- [ ] `maxRetryCountInSlot: 2` 로 설정 후 광고 실패 환경에서 테스트
- [ ] 내부 재시도가 2회 수행된 후 `loadFailed` 이벤트 발생 확인

- [ ] `maxRetryCountInSlot: -1` (무한 재시도) 로 설정
- [ ] 광고 로드 실패 시 재시도가 계속 발생하며 앱 크래시 없음 확인

#### 합격 기준

- [ ] 각 설정에 맞게 재시도 횟수가 제한됨
- [ ] 앱 크래시 없음

---

### TC-IV-06: 이벤트 전체 확인 (InterstitialVideoAdEventMap)

확인해야 할 이벤트 목록:

- [ ] `loaded` — 광고 로드 성공
- [ ] `loadFailed` — 광고 로드 실패
- [ ] `opened` — 광고 화면 오픈
- [ ] `closed` — 광고 화면 닫힘
- [ ] `clicked` — 광고 클릭
- [ ] `impression` — 광고 노출
- [ ] `completed` — 영상 완료 시청
- [ ] `skipped` — 영상 스킵

#### 합격 기준

- [ ] 모든 이벤트가 해당 상황에서 정확히 1회 호출됨

---

## 6. RewardedAd — 리워드 광고

### 샘플 코드

```typescript
import React from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { RewardedAd } from 'react-native-nap-ssp';
import type { RewardPayload } from 'react-native-nap-ssp';

const REWARD_UNIT_ID = '103722'; // 실제 ID로 교체

async function showRewarded() {
  const ad = new RewardedAd(REWARD_UNIT_ID, {
    // S2S 보상 콜백에 함께 전달될 파라미터
    customParams: {
      useid: 'user_12345',    // 사용자 고유 ID
      name:  '홍길동',         // 사용자 이름 (선택)
      phone: '010-1234-5678', // 사용자 연락처 (선택)
    },
    mute: false, // Android 전용: 비디오 음소거 여부
  });

  ad.addAdEventListener('loaded',     () => console.log('[REWARD] loaded'));
  ad.addAdEventListener('loadFailed', e  => console.log('[REWARD] loadFailed:', e.code, e.message));
  ad.addAdEventListener('opened',     () => console.log('[REWARD] opened'));
  ad.addAdEventListener('impression', () => console.log('[REWARD] impression'));
  ad.addAdEventListener('clicked',    () => console.log('[REWARD] clicked'));
  ad.addAdEventListener('closed',     () => { console.log('[REWARD] closed'); ad.destroy(); });

  // 보상 이벤트 — 두 가지 방식 모두 동일하게 동작
  ad.addAdEventListener('rewarded',   (payload: RewardPayload) => {
    console.log(`[REWARD] rewarded! type=${payload.type} amount=${payload.amount}`);
    Alert.alert('보상 지급!', `type: ${payload.type}\namount: ${payload.amount}`);
  });
  // 또는 레거시 방식
  // ad.addAdEventListener('onRewarded', (payload) => { ... });

  ad.addAdEventListener('completed',  () => console.log('[REWARD] completed ✅'));
  ad.addAdEventListener('skipped',    () => console.log('[REWARD] skipped'));

  try {
    await ad.start(); // load + show 통합
  } catch (e: any) {
    Alert.alert('리워드 오류', e?.message ?? String(e));
    ad.destroy();
  }
}

export default function RewardedTestScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <TouchableOpacity
        onPress={showRewarded}
        style={{ backgroundColor: '#6A1B9A', padding: 16, borderRadius: 8 }}>
        <Text style={{ color: '#fff', fontSize: 16 }}>리워드 광고 표시</Text>
      </TouchableOpacity>
    </View>
  );
}
```

---

### TC-REW-01: load() → show() 정상 흐름

#### 절차

- [ ] `ad.start()` 호출 (내부적으로 load + show 처리)
- [ ] 리워드 비디오 광고가 전체 화면으로 표시되는지 확인
- [ ] 비디오 재생 시작 확인

#### 합격 기준

- [ ] `loaded` → `opened` → `impression` 이벤트 순서대로 호출됨
- [ ] 리워드 비디오 광고가 전체 화면으로 표시됨
- [ ] 앱 크래시 없음

---

### TC-REW-02: rewarded 이벤트 — 보상 지급 확인

#### 절차

- [ ] 리워드 광고 재생 시작
- [ ] 영상을 끝까지 시청하거나 SDK가 보상 지급 기준에 도달
- [ ] `rewarded` 이벤트 수신 확인
- [ ] `payload.type` 및 `payload.amount` 값 기록

#### 합격 기준

- [ ] `rewarded` 콜백이 호출됨
- [ ] `payload.type` — 비어있지 않은 문자열 (예: `"reward"`)
- [ ] `payload.amount` — 양수 숫자 (예: `1` 이상)

---

### TC-REW-03: completed 이벤트 — 영상 완료

#### 절차

- [ ] 스킵 없이 영상 끝까지 시청
- [ ] `completed` 이벤트 확인

#### 합격 기준

- [ ] `completed` 콜백이 호출됨
- [ ] `skipped`는 호출되지 않음

---

### TC-REW-04: skipped 이벤트

#### 절차

- [ ] 리워드 광고 재생 중 스킵 버튼(있는 경우) 탭
- [ ] `skipped` 이벤트 확인

#### 합격 기준

- [ ] `skipped` 콜백이 호출됨
- [ ] `completed`는 호출되지 않음
- [ ] **스킵 시 보상 미지급**: `rewarded` 이벤트가 호출되지 않음을 확인 (SDK/서버 정책에 따라 다를 수 있음)

---

### TC-REW-05: customParams 전달 확인

#### 절차

- [ ] `customParams: { useid: 'user_12345', name: '홍길동', phone: '010-1234-5678' }` 설정
- [ ] 리워드 광고 로드 및 표시
- [ ] 광고 완료 후 보상 이벤트 수신

#### 합격 기준

- [ ] 광고가 정상적으로 로드 및 표시됨
- [ ] `rewarded` 이벤트가 호출됨
- [ ] 앱 크래시 없음

---

### TC-REW-06: S2S 콜백 검증

**사전 조건**: 매체사 서버에 S2S 콜백 URL이 설정되어 있어야 합니다.

#### 절차

- [ ] `customParams.useid` 에 고유한 테스트 사용자 ID를 설정 (예: `'s2s_test_001'`)
- [ ] 리워드 광고 표시 및 영상 완료 시청
- [ ] `rewarded` 이벤트 수신 확인 (클라이언트 측)
- [ ] 매체사 서버 로그에서 S2S 콜백 수신 확인
  - `useid`, `name`, `phone` 파라미터가 콜백 요청에 포함되어 있는지 확인
- [ ] 보상 처리 로직(포인트 지급 등)이 정상 동작하는지 확인

#### 합격 기준

- [ ] 클라이언트: `rewarded` 이벤트에서 `payload.type`, `payload.amount` 수신
- [ ] 서버: S2S 콜백 URL에 `customParams`가 포함된 요청이 도달함
- [ ] 서버: 보상 처리가 중복 없이 1회만 처리됨

#### 실패 패턴

| 증상 | 원인 |
|------|------|
| S2S 콜백 미수신 | 파트너사이트에 콜백 URL 미설정 |
| `useid` 누락 | `customParams`에 `useid` 키 없음 |
| 보상 중복 지급 | 서버 측 멱등성 처리 누락 |

---

### TC-REW-07: mute 옵션 — Android 전용

**대상 플랫폼**: Android 전용

#### 절차

- [ ] `mute: true` 로 리워드 광고 표시
- [ ] 비디오 재생 시 소리 없이(음소거 상태) 재생되는지 확인
- [ ] `mute: false` 로 변경 후 소리가 나는지 확인

#### 합격 기준

- [ ] `mute: true` → 소리 없이 재생됨
- [ ] `mute: false` → 정상 소리와 함께 재생됨

---

### TC-REW-08: 이벤트 전체 확인 (RewardedAdEventMap)

확인해야 할 이벤트 목록:

- [ ] `loaded` — 광고 로드 성공
- [ ] `loadFailed` — 광고 로드 실패
- [ ] `opened` — 광고 화면 오픈
- [ ] `closed` — 광고 화면 닫힘
- [ ] `clicked` — 광고 클릭
- [ ] `impression` — 광고 노출
- [ ] `rewarded` — 보상 지급 (payload: `{ type, amount }`)
- [ ] `completed` — 영상 완료 시청
- [ ] `skipped` — 영상 스킵

#### 합격 기준

- [ ] 모든 이벤트가 해당 상황에서 정확히 1회 호출됨

---

## 7. 공통 예외 시나리오

### TC-CMN-01: 초기화 전 광고 로드 시도

#### 절차

- [ ] `NapSspAd.initialize()` 호출 없이 바로 `BannerAd` 렌더링 또는 `InterstitialAd.load()` 호출
- [ ] 동작 확인

#### 합격 기준

- [ ] 앱 크래시 없음
- [ ] 에러 콜백(`onAdFailedToLoad` 또는 `loadFailed`) 또는 예외가 발생함

---

### TC-CMN-02: 빠른 연속 광고 요청

#### 절차

- [ ] 배너 광고를 100ms 간격으로 5회 연속 `key` 변경으로 재마운트
- [ ] 전면 광고를 2초 간격으로 3회 연속 `start()` 호출

#### 합격 기준

- [ ] 앱 크래시 없음
- [ ] 이전 요청이 취소되거나 마지막 요청만 정상 처리됨
- [ ] 메모리 과다 사용(OOM) 없음

---

### TC-CMN-03: 30분 Soak Test (통합 부하 테스트)

통합 테스트 앱(integration-test-app)에는 30분 자동화 Soak Test 기능이 내장되어 있습니다.

#### 절차

- [ ] SDK 초기화 완료 후 "30분 자동 통합 부하 테스트 시작" 버튼 탭
- [ ] 15초 간격으로 배너/비디오 재로드 및 전면 광고 표시가 자동 반복됨
- [ ] 30분 후 "30분 부하 테스트 완료" 로그 확인

#### 합격 기준

- [ ] 30분 동안 앱 크래시 없음
- [ ] 메모리 사용량이 지속적으로 증가하지 않음 (Android: Android Studio Profiler / iOS: Instruments 활용)
- [ ] 이벤트 로그에 반복적인 `loaded`, `impression` 기록 확인

---

### TC-CMN-04: 네트워크 오프라인 → 온라인 복구

#### 절차

- [ ] 광고 로드 전 비행기 모드 활성화
- [ ] `BannerAd` 렌더링 → `onAdFailedToLoad` 확인
- [ ] 비행기 모드 해제 후 배너 재로드 (`key` 변경)
- [ ] 정상 로드 확인

#### 합격 기준

- [ ] 오프라인 시 `onAdFailedToLoad` 호출됨, 앱 크래시 없음
- [ ] 네트워크 복구 후 정상 광고 로드됨

---

### TC-CMN-05: 앱 백그라운드 → 포그라운드 전환

#### 절차

- [ ] 전면 광고 또는 리워드 광고 표시 중 홈 버튼으로 앱을 백그라운드로 전환
- [ ] 다시 앱 포그라운드로 복귀
- [ ] 광고 상태 확인

#### 합격 기준

- [ ] 백그라운드 전환 중 앱 크래시 없음
- [ ] 포그라운드 복귀 후 광고 화면이 올바른 상태를 유지하거나 자동으로 닫힘

---

## 테스트 결과 기록 양식

| 테스트 케이스 | 플랫폼 | 결과 | 비고 |
|--------------|--------|------|------|
| TC-BAN-01 | Android / iOS | PASS / FAIL | |
| TC-BAN-02 | Android / iOS | PASS / FAIL | |
| TC-BAN-03 | Android / iOS | PASS / FAIL | |
| TC-BAN-04 | Android / iOS | PASS / FAIL | |
| TC-BAN-05 | Android / iOS | PASS / FAIL | |
| TC-NAT-01 | Android / iOS | PASS / FAIL | |
| TC-NAT-02 | Android / iOS | PASS / FAIL | |
| TC-NAT-03 | Android / iOS | PASS / FAIL | |
| TC-VID-01 | Android / iOS | PASS / FAIL | |
| TC-VID-02 | Android / iOS | PASS / FAIL | |
| TC-VID-03 | Android / iOS | PASS / FAIL | |
| TC-VID-04 | Android 전용 | PASS / FAIL | |
| TC-VID-05 | Android / iOS | PASS / FAIL | |
| TC-INT-01 | iOS 전용 | PASS / FAIL | |
| TC-INT-02 | Android 전용 | PASS / FAIL | |
| TC-INT-03 | Android / iOS | PASS / FAIL | |
| TC-INT-04 | Android / iOS | PASS / FAIL | |
| TC-INT-05 | iOS 전용 | PASS / FAIL | |
| TC-INT-06 | Android / iOS | PASS / FAIL | |
| TC-INT-07 | Android / iOS | PASS / FAIL | |
| TC-INT-08 | Android / iOS | PASS / FAIL | |
| TC-IV-01 | Android / iOS | PASS / FAIL | |
| TC-IV-02 | Android / iOS | PASS / FAIL | |
| TC-IV-03 | Android / iOS | PASS / FAIL | |
| TC-IV-04 | Android / iOS | PASS / FAIL | |
| TC-IV-05 | Android / iOS | PASS / FAIL | |
| TC-IV-06 | Android / iOS | PASS / FAIL | |
| TC-REW-01 | Android / iOS | PASS / FAIL | |
| TC-REW-02 | Android / iOS | PASS / FAIL | |
| TC-REW-03 | Android / iOS | PASS / FAIL | |
| TC-REW-04 | Android / iOS | PASS / FAIL | |
| TC-REW-05 | Android / iOS | PASS / FAIL | |
| TC-REW-06 | Android / iOS | PASS / FAIL | S2S 서버 설정 필요 |
| TC-REW-07 | Android 전용 | PASS / FAIL | |
| TC-REW-08 | Android / iOS | PASS / FAIL | |
| TC-CMN-01 | Android / iOS | PASS / FAIL | |
| TC-CMN-02 | Android / iOS | PASS / FAIL | |
| TC-CMN-03 | Android / iOS | PASS / FAIL | |
| TC-CMN-04 | Android / iOS | PASS / FAIL | |
| TC-CMN-05 | Android / iOS | PASS / FAIL | |

---

*이 문서는 react-native-nap-ssp v0.1.7 기준으로 작성되었습니다.*
