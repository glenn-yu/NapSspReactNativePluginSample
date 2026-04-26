# Migration Guide

이 문서는 `react-native-nap-ssp` 플러그인의 버전 업데이트 시 필요한 대응 방법을 안내합니다.

---

## v0.1.1 ➡️ v0.1.2

### 1. InterstitialVideoAd API 변경
`InterstitialVideoAd` 클래스에 `start()` 메서드가 추가되었습니다. 이전의 `load()` 후 `show()`를 호출하던 방식도 유지되지만, 신규 코드는 `start()` 사용을 권장합니다.

```tsx
// AS-IS
await interVideo.load();
await interVideo.show();

// TO-BE (Recommended)
await interVideo.start();
```

---

## v0.1.0 ➡️ v0.1.1

### 1. 네이티브 광고 (NativeAd) 연동
기존 Placeholder 방식에서 실제 SDK 바인딩 방식으로 업그레이드되었습니다. Android의 경우 `res/layout` 설정이, iOS의 경우 `.xib` 설정이 필요합니다. 상세 내용은 [Native Assets Guide](./NATIVE_ASSETS_GUIDE.md)를 참조하십시오.

### 2. 초기화 옵션 추가
`NapSspAd.initialize` 시 `mediations` 옵션을 통해 각 네트워크별 세부 설정을 전달할 수 있도록 확장되었습니다.
