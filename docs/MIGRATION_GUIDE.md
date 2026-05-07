# Migration Guide

이 문서는 `react-native-nap-ssp` 플러그인의 버전 업데이트 시 필요한 대응 방법을 안내합니다.

---

## v0.1.2 ➡️ v0.1.3

### 1. iOS SPM 체크섬 업데이트

SPM을 통해 직접 패키지를 참조하는 경우, `Package.swift`의 AdMixerMediation 체크섬을 업데이트해야 합니다.

```swift
// 변경 전 (2.3.2)
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.2.xcframework.zip"
let adMixerMediationChecksum = "78c7edb00263cade63925a2e91eddfa9cc84b4a2b73c2036b87b45e97cf43ca7"

// 변경 후 (2.3.3)
let adMixerMediationURL = "https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.3.xcframework.zip"
let adMixerMediationChecksum = "b40eb8ae2eff354e56de68ad11de0030002d17ba66a48b2df2bad461c1a6049f"
```

### 2. 이벤트 이름 통일 (확인 필요)

이벤트 리스너에서 이벤트 이름이 `on` 접두사 형식으로 통일되었습니다. 기존 코드에서 `'closed'`, `'clicked'` 등 단축 형태를 사용하고 있었다면 `'onAdClosed'`, `'onAdClicked'` 등으로 업데이트하세요.

```tsx
// 기존
inter.addAdEventListener('closed', () => {});

// 최신
inter.addAdEventListener('onAdClosed', () => {});
```

### 3. 그 외 변경 없음

Android Maven SDK 버전은 0.1.2와 동일합니다. CocoaPods 사용자는 별도 변경이 필요하지 않습니다.

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
