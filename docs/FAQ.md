# FAQ (자주 묻는 질문)

`react-native-nap-ssp` 사용자들이 자주 겪는 상황에 대한 답변입니다.

---

## 1. 초기화 및 설정

### Q. `NapSspAd.initialize`는 언제 호출해야 하나요?
**A.** 앱의 라이프사이클 중 가장 빠른 시점인 `App.tsx`의 최상위 `useEffect` 또는 `index.js`에서 한 번만 호출하면 됩니다.

### Q. 미디어 키와 광고 ID를 어디서 발급받나요?
**A.** 나스미디어 파트너 센터(또는 담당 운영팀)를 통해 발급받을 수 있습니다.

---

## 2. 빌드 및 런타임 오류

### Q. Android에서 `Duplicate class` 오류가 발생합니다.
**A.** 다른 광고 SDK(예: admob 등)와 라이브러리 버전 충돌이 발생할 수 있습니다. `build.gradle`에서 `resolutionStrategy`를 사용해 버전을 고정하거나 담당팀에 문의하십시오.

### Q. iOS 빌드 시 `AdMixerMediation.h`를 찾을 수 없다고 나옵니다.
**A.** `pod install`이 정상적으로 완료되었는지 확인하고, `.xcodeproj`가 아닌 `.xcworkspace` 파일을 열어 빌드하고 있는지 확인하십시오.

---

## 3. 광고 표시 관련

### Q. 광고가 로드되었는데 화면에 보이지 않습니다. (Banner/Native)
**A.** 배너나 네이티브 광고 컴포넌트의 `style`에 `width`와 `height`가 명시적으로 지정되어 있는지 확인하십시오. 특히 네이티브 광고는 컨텐츠 양에 따라 충분한 높이가 필요합니다.

### Q. 특정 미디에이션 광고만 노출되지 않습니다.
**A.** 해당 미디에이션 네트워크(예: Pangle)의 별도 설정(Key 입력, SDK 링킹)이 누락되지 않았는지 [Mediation Guide](./MEDIATION_GUIDE.md)를 통해 확인하십시오.

---

## 4. 기타

### Q. New Architecture(Fabric/TurboModule)를 지원하나요?
**A.** 현재는 Old Architecture 전용입니다. New Architecture 지원은 로드맵에 포함되어 있으며 향후 업데이트 예정입니다.
