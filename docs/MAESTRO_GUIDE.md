# 🎭 Maestro Validation & Stabilization Guide

이 문서는 **Nap SSP React Native 플러그인**의 안정성을 검증하기 위해 `integration-test-app`과 **Maestro**를 활용한 환경 설정, 빌드, 단계별 검증 계획 및 디버깅 가이드를 정의합니다.

---

## 1. 환경 설정 (Prerequisites)

### 1.1 필수 도구
- **Java**: OpenJDK 17 추천 (`brew install openjdk@17`)
- **Android**: SDK / platform-tools (`brew install android-platform-tools`)
- **iOS**: Xcode + iOS Simulator
- **Maestro CLI**:
  ```bash
  curl -fsSL https://get.maestro.mobile.dev | bash
  ```

### 1.2 저장소 준비
```bash
npm install
cd integration-test-app
npm install
```

---

## 2. 앱 빌드 및 준비

### 2.1 Android
```bash
cd integration-test-app/android
./gradlew assembleDebug
./gradlew installDebug
# Metro 실행 및 포트 리버스
adb reverse tcp:8081 tcp:8081
```

### 2.2 iOS
```bash
cd integration-test-app/ios
pod install --repo-update
# xcodebuild를 이용한 시뮬레이터 빌드 (예시)
xcodebuild -workspace IntegrationTestApp.xcworkspace -scheme IntegrationTestApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

---

## 3. 검증 계획 및 시나리오 (Validation Plan)

### 3.1 단계별 검증 절차
1.  **Smoke Test**: 앱 런칭 및 SDK 초기화 (`SDK: success` 확인).
2.  **Inline Ads**: 배너 및 네이티브 광고 로드, `IMPRESSION` 이벤트 발생 확인.
3.  **Fullscreen Ads**: 전면/리워드 노출 후 `OPENED`, `IMPRESSION`, `onRewarded` 순차 수신 확인.
4.  **Soak Test**: 동일 광고 단위 반복 로드(10회 이상) 및 예외 상황 테스트.

### 3.2 상세 테스트 케이스 (Soak Tests)
- **Android**: `android-ad-validation.yaml` 기반, `android-soak-60m.sh` 실행.
- **iOS**: `ios-ad-validation.yaml` 기반, `ios-soak-60m.sh` 실행.

---

## 4. 디버깅 및 트러블슈팅

### 4.1 주요 이슈 해결 사례 (Worklog)
- **JS 이벤트 래핑**: `NativeAd`, `VideoAd`의 `onAdImpression` 누락 보완.
- **브릿지 연결 안정화**: iOS `NapSspModule` 링크 오류 해결 및 합성 이벤트(`synthetic events`) 활용.
- **Android 런타임**: `DeadObjectException` 분석 및 Metro 포트 대기 로직 추가.

### 4.2 장애 복구 가이드
- **Maestro 연결 오류**: `pkill -f maestro` 및 `adb kill-server` 후 재시도.
- **iOS 브릿지 확인**: `nm -gU` 명령으로 바이너리 내 `NapSspModule` 기호 확인.

---

## 5. 결과 리포팅 정책 (Results Policy)
- **요약 리포트**: 통과/실패 횟수 등 요약 정보만 커밋 (`maestro-soak-history.md`).
- **로컬 아티팩트**: 대용량 로그 및 스크린샷은 로컬 `results/` 폴더에만 유지하며 커밋하지 않음.
- **자동 중단**: 동일 유형 실패 3회 연속 발생 시 테스트 자동 중단.
