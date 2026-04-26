# Advanced Usage & Best Practices

이 문서는 `react-native-nap-ssp`를 사용하여 광고 수익을 극대화하고 앱 성능을 최적화하기 위한 고급 활용 방법을 안내합니다.

---

## 1. 광고 미리 불러오기 (Pre-loading)

사용자가 광고를 보게 될 시점보다 조금 일찍 광고를 로드하면 대기 시간을 줄이고 사용자 경험을 향상시킬 수 있습니다.

```tsx
const interstitial = new InterstitialAd('INTER_ID');

// 전면 광고가 필요한 화면에 진입할 때 미리 load()
useEffect(() => {
  interstitial.load();
}, []);

const handleFinishStage = async () => {
  // 실제 표시 시점에는 이미 로드되어 있어 즉시 표시됨
  await interstitial.show();
};
```

---

## 2. 광고 상태 관리

배너 광고나 네이티브 광고의 경우, 앱이 백그라운드로 갈 때 로딩을 멈추거나 화면에서 사라질 때 리소스를 해제하는 것이 좋습니다.

- **배너 광고**: 화면이 언마운트될 때 컴포넌트가 자동으로 정리되지만, 복잡한 네비게이션 환경에서는 조건부 렌더링을 통해 명시적으로 관리하는 것이 좋습니다.
- **전면 광고**: `closed` 이벤트를 수신한 후 다음 광고를 위해 미리 다시 `load()` 해두는 패턴을 추천합니다.

---

## 3. 에러 핸들링 전략

네트워크 상황이나 광고 물량 부족(No Fill)으로 광고 로드가 실패할 수 있습니다.

```tsx
<BannerAd
  onAdFailedToLoad={(error) => {
    console.warn(error.message);
    // 팁: 실패 시 즉시 재시도하기보다, 지수 백오프(Exponential Backoff)를 사용하거나 
    // 일정 시간(예: 30초) 후에 재시도하는 것을 권장합니다.
  }}
/>
```

---

## 4. 메모리 관리

특히 동영상 광고나 이미지 자산이 많은 네이티브 광고를 자주 사용하는 경우, 더 이상 필요하지 않은 광고 객체는 리스너를 제거하고 참조를 해제하여 메모리 누수를 방지하십시오.
