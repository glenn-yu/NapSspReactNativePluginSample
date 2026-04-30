# Project Testing Structure

이 문서는 `NapSspReactNativePluginSample` 프로젝트에서 **사람이 직접 테스트를 수행할 때** 어디를 봐야 하는지, 어떤 앱/스크립트를 실행해야 하는지 빠르게 이해하도록 돕기 위한 안내서입니다.

---

## 1. 프로젝트를 한 문장으로 설명하면

이 저장소는 **React Native용 nap SSP 플러그인 본체**와, 그것을 실제로 검증하기 위한 **예제 앱 / 통합 테스트 앱 / Maestro 자동화 스크립트**를 함께 담고 있습니다.

즉:
- `src`, `android`, `ios` = 플러그인 본체
- `integration-test-app` = 실제 광고 동작을 검증하는 테스트 앱
- `integration-test-app/maestro` = 자동화 시나리오와 soak 스크립트
- `example/ExampleHostApp` = 사람이 수동으로 붙여보는 호스트 예제 앱

---

## 2. 최상위 주요 폴더

### `src/`
React Native에서 사용하는 TypeScript 소스입니다.
- `BannerAd.tsx`, `NativeAd.tsx`, `VideoAd.tsx`
- `InterstitialAd.ts`, `RewardedAd.ts`, `InterstitialVideoAd.ts`
- JS/TS 레벨 API와 네이티브 브릿지 연결이 여기 있습니다.

### `lib/`
빌드된 JS 산출물입니다.
보통 직접 수정하지 않고 `src/`를 기준으로 봅니다.

### `android/`
플러그인 Android 네이티브 구현입니다.
- Gradle 의존성
- Android bridge / module / native view 구현
- 최신 NAP SSP Android SDK 좌표 반영 위치: `android/build.gradle`

### `ios/`
플러그인 iOS 네이티브 구현입니다.
- Swift / Objective-C bridge
- Podspec / SPM Package
- 최신 iOS SDK 코어/SPM 좌표 반영 위치: `ios/Package.swift`, `NapSspPlugin.podspec`

### `integration-test-app/`
가장 중요한 **실전 검증용 앱**입니다.
이 앱에서 실제 광고 포맷들을 띄우고, Maestro가 이 앱을 자동으로 조작합니다.

### `example/ExampleHostApp/`
플러그인을 일반 RN 앱에 붙였을 때 어떻게 보이는지 확인하는 샘플 호스트 앱입니다.
자동화의 중심은 아니고, 사람이 수동 점검할 때 유용합니다.

### `docs/`
운영 문서, 설치 문서, 가이드, 점검 결과 문서가 모여 있습니다.
이번 테스트 상태 요약은 아래 문서를 보면 됩니다.
- `docs/MAESTRO_READINESS_2026-04-30.md`

---

## 3. 실제 테스트할 때 가장 많이 보는 위치

### A. 테스트 앱 화면 정의
`integration-test-app/App.tsx`

이 파일이 테스트 UI의 중심입니다.
여기서 아래를 확인할 수 있습니다.
- SDK 초기화 버튼/상태
- 배너 / 네이티브 / 동영상 / 전면 / 리워드 버튼
- 각 광고 포맷의 상태 텍스트
- Maestro가 읽는 `testID`, `accessibilityLabel`

즉, **테스트가 실패하면 가장 먼저 이 파일의 상태 신호와 testID를 봐야 합니다.**

---

### B. Android Maestro 시나리오
`integration-test-app/maestro/android-ad-validation.yaml`

Android에서 Maestro가 어떤 순서로 앱을 조작하는지 적혀 있습니다.
예:
- 앱 실행
- SDK 초기화
- 배너 로드 확인
- 네이티브 로드 확인
- 동영상 광고 확인
- interstitial 확인

Android 자동화가 깨지면 이 파일을 먼저 봅니다.

---

### C. iOS Maestro 시나리오
`integration-test-app/maestro/ios-ad-validation.yaml`

iOS용 자동화 흐름입니다.
현재는 interstitial 검증을 위해 앱 안에 명시적 proof marker를 사용합니다.
예:
- `summary-inter-opened-yes`
- `summary-inter-impression-yes`

iOS interstitial 자동화가 흔들리면 이 파일과 `App.tsx`를 같이 봐야 합니다.

---

### D. 실행 스크립트
`integration-test-app/maestro/`

주요 스크립트:
- `android-validate.sh` : Android 1회 검증
- `ios-validate.sh` : iOS 1회 검증
- `android-soak-30m.sh` : Android 30분 soak
- `ios-soak-30m.sh` : iOS 30분 soak
- `validate-all.sh` : 양 플랫폼 검증 묶음
- `soak-all.sh` : soak 묶음

공통 함수/슬랙 알림 처리:
- `common.sh`

---

### E. 결과 위치
`integration-test-app/maestro/results/`

실행 결과가 여기에 쌓입니다.
예:
- `android-validate-YYYYMMDD-HHMMSS/`
- `ios-validate-YYYYMMDD-HHMMSS/`
- `soak-30m-...`
- `ios-soak-30m-...`

실패 시 확인 포인트:
- `validate.log`
- `summary.txt`
- `fail.png`, `fail-*.png`
- `run-*.log`

---

## 4. 사람이 직접 테스트하는 기본 순서

## 4-1. Android 수동/자동 점검
1. `integration-test-app` 기준으로 Android 앱 빌드/실행
2. 앱에서 SDK 초기화 성공 확인
3. 배너 / 네이티브 / 동영상 / 전면 광고 버튼 수동 확인
4. 자동 검증 필요 시:
   - `./integration-test-app/maestro/android-validate.sh`
5. 안정성 확인 필요 시:
   - `SOAK_DURATION_SECONDS=600 ./integration-test-app/maestro/android-soak-30m.sh`
   - 또는 기본 30분 soak 실행

## 4-2. iOS 수동/자동 점검
1. `integration-test-app` iOS 앱 빌드/실행
2. SDK 초기화 성공 확인
3. interstitial 클릭 후 닫힘/복귀/상태 텍스트 확인
4. 자동 검증 필요 시:
   - `./integration-test-app/maestro/ios-validate.sh`
5. 안정성 확인 필요 시:
   - `SOAK_DURATION_SECONDS=600 ./integration-test-app/maestro/ios-soak-30m.sh`
   - 또는 기본 30분 soak 실행

---

## 5. 어떤 앱을 써야 하나?

### `integration-test-app`
**광고 검증용 메인 앱**입니다.
- 자동화 기준 앱
- 광고 포맷별 상태 신호가 잘 노출됨
- Maestro 테스트는 거의 전부 이 앱 기준

### `example/ExampleHostApp`
**호스트 연동 예시 앱**입니다.
- 플러그인을 소비자 입장에서 붙여볼 때 유용
- 수동 연동 확인용
- 자동화 메인 대상은 아님

---

## 6. 문제가 생기면 어디부터 볼까?

### UI가 바뀌어 Maestro가 실패한다
1. `integration-test-app/App.tsx`
2. 해당 플랫폼 YAML (`android-ad-validation.yaml` / `ios-ad-validation.yaml`)
3. `results/.../fail.png`

### SDK 버전/의존성 문제 같다
1. `android/build.gradle`
2. `ios/Package.swift`
3. `NapSspPlugin.podspec`
4. `integration-test-app/ios/Podfile.lock`

### iOS interstitial 검증이 흔들린다
1. `App.tsx`의 interstitial proof marker 확인
2. `ios-ad-validation.yaml`에서 해당 marker를 읽는지 확인
3. 최신 `ios-validate-*` 결과의 `fail.png`, `validate.log` 확인

---

## 7. 지금 시점에서 기억하면 좋은 핵심

- **플러그인 본체**는 `src/`, `android/`, `ios/`
- **실전 테스트 앱**은 `integration-test-app/`
- **자동화 핵심**은 `integration-test-app/maestro/`
- **실패 분석**은 `integration-test-app/maestro/results/`
- **최신 안정화 상태 요약**은 `docs/MAESTRO_READINESS_2026-04-30.md`

이 다섯 군데만 알아도 테스트 흐름은 거의 따라갈 수 있습니다.
