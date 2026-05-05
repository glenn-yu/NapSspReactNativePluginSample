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

## 4. 테스트 실행 가이드

### 4.1 일회성 검증 (One-shot Validation)
플랫폼별 또는 전체 플랫폼의 건강 상태를 즉시 확인합니다.
- **전체 플랫폼**: `integration-test-app/maestro/validate-all.sh`
- **Android 전용**: `integration-test-app/maestro/android-validate.sh`
  - 기본 Android 대상은 `emulator-5554`입니다.
  - 다른 디바이스를 사용할 때는 `ANDROID_DEVICE_ID=<adb device id>`를 지정합니다.
- **iOS 전용**: `integration-test-app/maestro/ios-validate.sh`

### 4.2 부하 테스트 (Soak Tests)
장시간(기본 1시간) 동안 반복적으로 광고 로직을 실행하여 안정성을 테스트합니다.
- **전체 플랫폼**: `integration-test-app/maestro/soak-all.sh`
- **Android**: `android-soak-60m.sh` (또는 ADB 전용 `android-adb-soak-60m.sh`)
- **iOS**: `ios-soak-60m.sh`

---

## 5. 슬랙 알림 및 리포팅 (Slack Notification)

모든 자동화 스크립트는 실행 종료 시 슬랙으로 요약 결과를 전송하는 기능을 포함하고 있습니다.

### 5.1 설정 방법
환경 변수 또는 `common.sh` 파일을 통해 Webhook URL을 설정하십시오.
```bash
# 환경변수로 설정 (Crontab 권장)
export MAESTRO_SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
```

### 5.2 리포트 내용
- **Result**: PASS / FAIL 여부
- **Stats**: 총 시도 횟수 대비 성공/실패 수
- **Log Path**: 상세 분석을 위한 로컬 로그 파일 경로
- **Stop Reason**: 3회 연속 실패 시 자동 중단된 사유 (부하 테스트 전용)

---

## 6. 디버깅 및 트러블슈팅

### 6.1 주요 이슈 해결 사례 (Worklog)
- **JS 이벤트 래핑**: `NativeAd`, `VideoAd`의 `onAdImpression` 누락 보완.
- **브릿지 연결 안정화**: iOS `NapSspModule` 링크 오류 해결 및 합성 이벤트(`synthetic events`) 활용.
- **Android 런타임**: `DeadObjectException` 분석 및 Metro 포트 대기 로직 추가.

### 6.2 장애 복구 가이드
- **Maestro 연결 오류**: `pkill -f maestro` 및 `adb kill-server` 후 재시도.
- **iOS 브릿지 확인**: `nm -gU` 명령으로 바이너리 내 `NapSspModule` 기호 확인.

---

## 7. 결과 리포팅 정책 (Results Policy)
- **요약 리포트**: 통과/실패 횟수 등 요약 정보만 커밋 (`maestro-soak-history.md`).
- **로컬 아티팩트**: 대용량 로그 및 스크린샷은 로컬 `results/` 폴더에만 유지하며 커밋하지 않음.
- **자동 중단**: 동일 유형 실패 3회 연속 발생 시 테스트 자동 중단.
