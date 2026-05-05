# Maestro 실행 점검 - 2026-05-05

## 요청

React Native 통합 테스트 앱의 Android/iOS Maestro 검증을 실제 실행 가능하게 만들고 결과를 확인했다.

## 변경 요약

- Android 검증 스크립트가 Android 디바이스가 없을 때 iOS 시뮬레이터에 잘못 붙던 문제를 수정했다.
- `ANDROID_DEVICE_ID` 환경변수로 Android 대상 디바이스를 지정할 수 있게 했다. 기본값은 `emulator-5554`다.
- `validate-all.sh`가 Android 또는 iOS 중 하나라도 실패하면 전체 exit code를 실패로 반환하게 했다.
- Android AppLovin 전체화면 테스트 광고에서 최초 시스템 안내(`Got it`)가 떠도 흐름이 막히지 않도록 대응했다.
- AppLovin 광고 내부 텍스트는 Maestro 접근성 텍스트로 안정적으로 잡히지 않아 optional 검증으로 조정했다.

## 실행 환경

- Android AVD: `Pixel_6_API_34`
- Android device id: `emulator-5554`
- iOS Simulator: `iPhone 17 Pro` / iOS 26.4 / `F5390915-AD8B-47EC-9C54-4B892FFDF011`
- Java: OpenJDK 17 (`/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`)
- Maestro CLI: 2.5.1
- Metro: `localhost:8081` running

## 확인한 명령

```bash
ANDROID_DEVICE_ID=emulator-5554 ./integration-test-app/maestro/android-validate.sh
ANDROID_DEVICE_ID=emulator-5554 ./integration-test-app/maestro/validate-all.sh
```

## 결과

- Android 단독 검증: PASS
  - 로그: `integration-test-app/maestro/results/android-validate-20260505-171217/validate.log`
- Android + iOS 통합 검증: PASS
  - exit: `validate_all_exit=0`
  - Android 로그: `integration-test-app/maestro/results/android-validate-20260505-171533/validate.log`
  - iOS 로그: `integration-test-app/maestro/results/ios-validate-20260505-171807/validate.log`

## 주의사항

- Android AVD가 꺼져 있으면 `android-validate.sh`는 즉시 실패하며 명확한 로그를 남긴다.
- Android 테스트 전에 AVD 기동 및 앱 설치가 필요하다.
- AppLovin 테스트 광고 화면은 실제로 표시되지만, 광고 내부 텍스트가 접근성 트리에 안정적으로 노출되지 않아 optional로만 확인한다.
