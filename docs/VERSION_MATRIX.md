# Version Matrix

이 문서는 `react-native-nap-ssp` 플러그인 버전과 대응하는 Android/iOS 네이티브 SDK 버전 정보를 관리합니다.

---

## 1. SDK 버전 호환성

| Plugin 버전 | Android SDK | iOS SDK | 주요 변경 사항 |
| :--- | :--- | :--- | :--- |
| **0.1.2** | `1.0.23` | `2.3.2` | 전면형 API 통일, Maestro 검증 강화 |
| **0.1.1** | `1.0.21` | `2.2.0` | 네이티브 전 포맷 브릿지 연결 |
| **0.1.0** | `1.0.18` | `2.1.0` | 초기 스캐폴딩 및 배너/전면 지원 |

---

## 2. 미디에이션 어댑터 버전 (0.1.2 기준)

### Android
- **Core**: `1.0.23`
- **Google AdManager**: `1.0.15_delta`
- **Kakao AdFit**: `1.0.12_beta`
- **Pangle**: `1.0.12_beta`
- **AppLovin**: `1.0.10_beta`
- **UnityAds**: `1.0.7_beta`

### iOS
- **Core**: `2.3.2`
- **Google AdManager**: `1.0.8`
- **Kakao AdFit**: `1.0.7`
- **Pangle**: `1.0.6`
- **AppLovin**: `1.0.5`
- **UnityAds**: `1.0.6`

---

## 3. 업데이트 가이드
플러그인 버전을 올릴 때에는 `build.gradle` 및 `Podfile`에서 해당 네이티브 SDK 버전도 함께 업데이트해야 할 수 있습니다. 상세 내역은 [CHANGELOG.md](../../CHANGELOG.md)를 참조하십시오.
