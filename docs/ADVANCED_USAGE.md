# ⚙️ Advanced Usage & Best Practices

`react-native-nap-ssp`의 고급 활용 방법 및 모범 사례를 안내합니다.

---

## 목차

1. [광고 미리 불러오기 (Pre-loading)](#1-광고-미리-불러오기-pre-loading)
2. [광고 상태 관리](#2-광고-상태-관리)
3. [에러 핸들링 전략](#3-에러-핸들링-전략)
4. [메모리 관리](#4-메모리-관리)
5. [보상형 광고 S2S 파라미터](#5-보상형-광고-s2s-파라미터)
6. [전면 광고 팝업 옵션](#6-전면-광고-팝업-옵션-interstitialadinterstitialvideoad)
7. [디버그 vs 릴리즈 빌드 동작 차이](#7-디버그-vs-릴리즈-빌드-동작-차이)

---

## 1. 광고 미리 불러오기 (Pre-loading)

사용자가 광고를 보게 될 시점보다 미리 `load()`를 호출하면 대기 시간을 줄이고 사용자 경험을 향상시킬 수 있습니다.

```tsx
const interstitial = new InterstitialAd('전면_광고_ID');

// 광고가 필요한 화면에 진입할 때 미리 로드
useEffect(() => {
  interstitial.load();
}, []);

const handleFinishStage = async () => {
  // 이미 로드되어 있어 즉시 표시됨
  await interstitial.show();
};
```

> 💡 `closed` 이벤트를 수신한 후 다음 광고를 위해 바로 `load()`를 호출해 두는 패턴을 권장합니다.

---

## 2. 광고 상태 관리

- **배너/네이티브**: 화면 언마운트 시 컴포넌트가 자동 정리됩니다. 복잡한 네비게이션 환경에서는 조건부 렌더링으로 명시적으로 관리하세요.
- **전면/보상형**: `closed` 이벤트 후 다음 광고를 위해 `load()`를 미리 호출해 두는 패턴을 추천합니다.

---

## 3. 에러 핸들링 전략

네트워크 상황이나 광고 물량 부족(No Fill)으로 광고 로드가 실패할 수 있습니다.

```tsx
<BannerAd
  onAdFailedToLoad={(error) => {
    console.warn(error.message);
    // 즉시 재시도보다 30초 후 재시도를 권장합니다
  }}
/>
```

---

## 4. 메모리 관리

동영상 광고나 이미지 자산이 많은 네이티브 광고를 자주 사용하는 경우, 더 이상 필요하지 않은 광고 객체는 리스너를 제거하고 참조를 해제하여 메모리 누수를 방지하세요.

---

## 5. 보상형 광고 S2S 파라미터

보상형 광고를 서버-to-서버(S2S) 방식으로 검증할 때, 사용자 식별값을 SDK에 함께 전달할 수 있습니다.

```tsx
const rewarded = new RewardedAd('보상형_광고_ID', {
  customParams: {
    useid: 'user_unique_id',   // 유저 식별자
    name: '홍길동',             // (선택) 유저 이름
    phone: '010-0000-0000',    // (선택) 유저 연락처
  },
});
```

### S2S 콜백 자동 포함 파라미터

파트너 사이트에서 콜백 URL을 설정하면 아래 파라미터가 자동으로 포함됩니다.

| 파라미터 | 설명 | 예시 |
| :--- | :--- | :--- |
| `media_key` | 미디어 키 | `12345678` |
| `adunit_id` | 광고 단위 ID | `87654321` |
| `adid` | Android 광고 식별자 | `xxxx-xxxx` |
| `ifa` | iOS 기기 식별자 | `860635ea-...` |
| `earnedreward` | 리워드 지급 여부 | `1` |
| `timestamp` | 리워드 지급 시간 | `1546300800` |

**콜백 URL 예시:**

```
{매체사_콜백_URL}?media_key={mediakey}&adunit_id={adunitid}&adid={adid}&timestamp={timestamp}&useid=user_id&name=홍길동
```

> 💡 S2S 방식은 클라이언트 콜백보다 신뢰성이 높습니다. 보안이 중요한 서비스에서는 S2S 콜백을 우선 사용하세요.
> 콜백 URL은 파트너 사이트 → **미디어 관리 → 애드유닛 광고 설정**에서 입력합니다.

---

## 6. 전면 광고 팝업 옵션 (InterstitialAd / InterstitialVideoAd)

팝업 형식의 전면 광고에서 종료 버튼과 카운트다운을 커스터마이징할 수 있습니다.

### 팝업 형식 종류

| 형식 | 특징 |
| :--- | :--- |
| `basic` | 우측 상단 X 버튼 |
| `popup` | 하단 텍스트 닫기 버튼 (색상 커스터마이징 가능) |
| `countDown` | 설정 시간 경과 후 닫기 버튼 노출 (2~5초) |

```tsx
const interstitial = new InterstitialAd('전면_광고_ID', {
  type: 'popup',             // 'default' | 'popup' | 'countdown'
  buttonLeftText: '닫기',    // 팝업 형식의 좌측 버튼 텍스트
  buttonRightText: '계속',   // 팝업 형식의 우측 버튼 텍스트 (선택)
  countDownTime: 5,          // countdown 형식의 대기 시간 (초)
});
```

---

## 7. 디버그 vs 릴리즈 빌드 동작 차이

v0.1.5부터 빌드 타입에 따라 광고 실패 시 동작이 달라집니다.

### DEBUG 빌드 (개발/시뮬레이터 환경)

SDK 광고 로드 실패 또는 응답 없음 시 **플레이스홀더 성공 이벤트**를 발행합니다.

- `onAdLoaded` / `onAdImpression` 이 발행됨 (실제 광고 소재 없음)
- 12초 이내 SDK 응답이 없으면 타임아웃 폴백으로 플레이스홀더 이벤트 발행
- 전면/보상형 광고는 `show()` 호출 시 플레이스홀더 경로로 즉시 성공 처리
- iOS 전면 광고는 `show()` 직후 `onAdOpened` / `onAdImpression` 즉시 발행

이벤트 payload의 `source` 필드로 실제 광고와 플레이스홀더를 구분할 수 있습니다.

```tsx
<BannerAd
  onAdLoaded={(e) => {
    if (e?.source?.startsWith('debug') || e?.source === 'placeholder') {
      console.log('플레이스홀더 — 실제 광고 아님');
    } else {
      console.log('실제 광고 로드 성공');
    }
  }}
/>
```

### RELEASE 빌드 (프로덕션 환경)

- SDK 광고 로드 실패 시 `onAdFailedToLoad` 이벤트 발행 (실제 에러 코드/메시지 포함)
- 전면/보상형 광고는 SDK를 통해 실제 광고를 표시
- 플레이스홀더 폴백 없음

> ⚠️ RN 이벤트 연결은 디버그 빌드로 확인하고, 실제 광고 소재 노출 및 수익 집계는 반드시 **RELEASE 빌드 + 실기기**로 최종 확인하세요.
