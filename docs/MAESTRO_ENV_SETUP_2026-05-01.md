# Maestro 테스트 환경 구성 - 2026-05-01

## 구성 대상

- 프로젝트: `NapSspReactNativePluginSample`
- 앱: `integration-test-app`
- Android Maestro 검증 플로우: `integration-test-app/maestro/android-validate.sh`

## 설치/구성 완료

- Homebrew 패키지
  - `openjdk@17`
  - `android-platform-tools`
  - `android-commandlinetools`
- Maestro CLI
  - 설치 경로: `$HOME/.maestro/bin/maestro`
  - 확인 버전: `2.5.1`
- Android SDK
  - 프로젝트 표준 경로: `$HOME/Library/Android/sdk`
  - 추가 설치 경로: `/opt/homebrew/share/android-commandlinetools`
  - 설치 패키지:
    - `platform-tools`
    - `emulator`
    - `platforms;android-34`
    - `build-tools;34.0.0`
    - `system-images;android-34;google_apis;arm64-v8a`
- Android AVD
  - 이름: `Pixel_6_API_34`
  - API: Android 34
  - ABI: `arm64-v8a`
- Shell 환경 변수
  - `~/.zshrc`에 `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `PATH` 추가

## 프로젝트 조정

- `integration-test-app/maestro/android-validate.sh`
  - `JAVA_HOME`을 `openjdk@17` 경로로 수정
- `integration-test-app/maestro/ios-validate.sh`
  - `JAVA_HOME`을 `openjdk@17` 경로로 수정
- 루트 패키지 빌드 실행
  - `npm run build`
  - 목적: Metro가 참조하는 `lib/index.js` 생성

## 검증 결과

### 성공

- Java 실행 확인
- ADB 실행 확인
- SDK Manager 실행 확인
- Maestro CLI 실행 확인
- Android AVD 생성 확인
- Android emulator 부팅 확인
- React Native Metro 실행 확인
- Debug APK 빌드 성공
- Debug APK 설치 성공
- Maestro Android flow 실행 시작 확인

### Maestro 실행 결과

- 1차 실패 원인: 루트 패키지 `lib/index.js` 부재로 Metro 500 오류
  - 조치: `npm run build`로 `lib/` 생성
- 2차 실행: 앱 UI 진입 및 SDK 초기화/배너 검증 통과
- 최종 실패 위치: Native 광고 검증
  - 기대값: `NATIVE_STATUS:LOADED:IMPRESSION`
  - 실제 화면: `NATIVE_STATUS:NOT_LOADED:IMPRESSION`, `NATIVE_MSG:impression`
  - 로그: `integration-test-app/maestro/results/android-validate-20260501-104213/validate.log`
  - 스크린샷: `integration-test-app/maestro/results/android-validate-20260501-104213/fail.png`

## 현재 남은 사항

- Android Maestro 환경은 구성 완료.
- Native 광고 상태 불일치는 테스트 환경 문제가 아니라 앱/SDK/시나리오 검증 이슈로 분리해 확인 필요.
- iOS 검증은 전체 Xcode가 활성화되어 있지 않아 아직 구성 완료가 아니다.
