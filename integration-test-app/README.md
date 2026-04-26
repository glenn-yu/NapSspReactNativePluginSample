# Nap SSP Integration Test App

이 앱은 `react-native-nap-ssp` 플러그인의 모든 광고 포맷과 이벤트를 실제 환경(Android/iOS)에서 검증하기 위한 통합 테스트 전용 애플리케이션입니다.

---

## 🚀 빠른 시작

### 1. 의존성 설치
```bash
cd integration-test-app
npm install
```

### 2. 네이티브 빌드 준비
- **Android**: `JDK 17` 환경 확인. `./android/gradlew installDebug`
- **iOS**: `cd ios && pod install`. Xcode에서 빌드 및 실행.

### 3. 광고 테스트
앱 실행 후 상단의 **[SDK 초기화]** 버튼을 먼저 누르세요. 이후 각 섹션의 **[로드]** 및 **[표시]** 버튼을 통해 광고를 테스트할 수 있습니다. 하단의 **이벤트 로그** 영역에서 실시간 콜백 수신 여부를 확인하십시오.

---

## 🎭 자동화 검증 (Maestro)

이 앱은 Maestro 기반의 자동화 시나리오를 지원합니다.

### 실행 방법
```bash
# Android 검증
maestro test maestro/android-ad-validation.yaml

# iOS 검증
maestro --device <SIMULATOR_UDID> test maestro/ios-ad-validation.yaml
```

상세한 자동화 가이드는 루트의 [docs/MAESTRO_GUIDE.md](../../docs/MAESTRO_GUIDE.md)를 참조하십시오.

---

## 🛠️ 주요 구조
- `App.tsx`: 모든 광고 포맷 테스트 UI 및 상태 관리 로직.
- `maestro/`: 플랫폼별 YAML 테스트 시나리오 및 부하 테스트 스크립트.
- `ios/NapSspLocalPod`: 로컬 플러그인 소스를 직접 참조하여 빌드하기 위한 Pod 설정.
