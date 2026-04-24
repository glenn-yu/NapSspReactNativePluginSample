# React Native Plugin 개발 및 실검증 계획서

## 1. 목표

이 문서는 **Nasmedia Nap SSP Android/iOS 네이티브 가이드 기준**으로 React Native 플러그인을 보완하고,
그 결과를 **개발용/테스트용 React Native 테스트 앱(Android, iOS)** 에서 실제로 빌드 및 실행한 뒤,
**실제 mediaKey / adUnitId 입력 후 광고 노출 및 이벤트 수신까지 검증**하기 위한 실행 계획서다.

핵심 목표는 다음 4가지다.

1. React Native 플러그인 구현 범위를 공식 네이티브 가이드와 대조해 부족한 점을 식별한다.
2. Android / iOS 테스트 앱이 각각 실제로 빌드 가능한 상태가 되도록 복구 및 보완한다.
3. 실제 발급 key를 입력해 광고 노출과 주요 이벤트 수신을 검증한다.
4. 검증 결과를 문서화해, 현재 지원 범위와 미지원 범위를 혼동 없이 정리한다.

---

## 2. 기준 문서

아래 문서를 단일 기준으로 삼는다.

- `docs/nap-ssp-android-sdk-native.md`
- `docs/nap-ssp-ios-sdk-native.md`
- `README.md`
- `src/` 이하 React Native 공개 API 및 브리지 구현
- `android/`, `ios/` 네이티브 플러그인 구현
- `example/ExampleHostApp`
- `integration-test-app`

참고:
- 공식 네이티브 가이드에 있는 항목이라도 RN 공개 API에 아직 없는 경우는 “미지원”으로 명확히 표시한다.
- 추측으로 완료 판정하지 않는다.

---

## 3. 현재 상태 요약 (2026-04-25 기준)

### 3.1 이미 확인된 항목

- 루트 CI(`verify-js`, `verify-example`)는 통과했다.
- `example/ExampleHostApp` Jest는 통과한다.
- `integration-test-app` iOS scaffold는 복구되었고, `pod install` 및 `run-ios` 진입까지 확인되었다.
- 다만 iOS 실제 실행은 React Native 0.72 계열과 최신 Xcode toolchain 사이의 Yoga / Folly 호환성 이슈가 남아 있다.
- 공식 네이티브 가이드 대비 Bizboard는 RN 공개 API에 아직 없다.
- SPM은 구조는 있으나 checksum placeholder가 남아 있어 release-grade 상태가 아니다.

### 3.2 현재 주요 리스크

- Android 실제 실행 환경은 Java / emulator / device 준비 여부에 좌우된다.
- iOS 실제 실행 환경은 Xcode 버전과 RN 0.72 호환성 패치가 필요할 수 있다.
- 실제 광고 검증은 테스트용 placeholder ID가 아니라 실발급 key/adUnitId가 필요하다.

---

## 4. 최종 산출물

이 계획의 최종 산출물은 아래 5가지다.

1. **React Native 플러그인 기능/갭 정리 문서**
2. **Android 테스트 앱 실행 성공**
3. **iOS 테스트 앱 실행 성공**
4. **실제 key 입력 후 광고 노출 및 이벤트 검증 기록**
5. **지원/미지원 범위 및 후속 TODO 문서화**

---

## 5. 작업 단계

### Phase 1. 공식 가이드 대비 기능 매핑

목표:
- 네이티브 가이드 기준으로 RN 플러그인이 무엇을 지원하고, 무엇이 빠졌는지 명확히 정리한다.

세부 작업:
1. Android/iOS 가이드의 광고 포맷, 초기화 요구사항, 옵션, 이벤트, 제약사항 추출
2. RN 공개 API(`src/index.ts`, `types.ts`, 각 Ad 클래스`)와 1:1 비교
3. 항목별 상태 분류
   - IMPLEMENTED
   - PARTIAL
   - MISSING
4. 특별 관리 항목 별도 표시
   - Bizboard
   - SPM 배포 완성도
   - host app native 설정 요구사항
   - mediation 의존성

완료 기준:
- 문서만 읽어도 “무엇이 되고 무엇이 안 되는지” 명확해야 한다.

---

### Phase 2. Android 테스트 앱 복구 및 빌드 성공

목표:
- Android용 React Native 테스트 앱이 실제로 빌드되고 실행될 수 있어야 한다.

세부 작업:
1. `integration-test-app/android` 구조 점검
2. Gradle wrapper / settings / build files / local run prerequisites 정리
3. Java / Android SDK / emulator 또는 device 준비
4. `npx react-native run-android` 또는 동등한 빌드 성공 확인
5. 앱 기동 후 초기화 화면 및 광고 테스트 UI 진입 확인

완료 기준:
- Android 테스트 앱이 실제 에뮬레이터 또는 기기에서 실행된다.

#### Android 환경 세팅 체크리스트

- [ ] Homebrew Java 설치 여부 확인
- [ ] Android 실행 시 사용할 JDK를 **openjdk@17**로 고정
- [ ] `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` 설정
- [ ] `PATH="$JAVA_HOME/bin:$PATH"` 적용
- [ ] `android/gradlew` 실행권한 확인 (`chmod +x android/gradlew`)
- [ ] `./android/gradlew -p android tasks --all` 성공 확인
- [ ] Android SDK / platform-tools 접근 가능 확인
- [ ] `adb devices`에서 emulator 또는 실제 기기 인식 확인
- [ ] 인식된 대상이 없으면 emulator 기동 또는 기기 USB 연결
- [ ] 그 다음 `npx react-native run-android` 실행

참고:
- JDK 25는 현재 Gradle 8.0.1 조합에서 `Unsupported class file major version 69`로 실패할 수 있다.
- 이 저장소의 Android 실행 검증 기준 Java는 현재 **JDK 17**이다.

---

### Phase 3. iOS 테스트 앱 복구 및 빌드 성공

목표:
- iOS용 React Native 테스트 앱이 실제로 빌드되고 실행될 수 있어야 한다.

세부 작업:
1. iOS scaffold / workspace / Podfile / scheme 유지 상태 확인
2. `pod install` 성공 유지
3. 최신 Xcode와 RN 0.72 간 Yoga / Folly 호환성 문제 해결
4. `npx react-native run-ios --simulator "..."` 또는 동등한 빌드 성공 확인
5. 앱 기동 후 초기화 화면 및 광고 테스트 UI 진입 확인

완료 기준:
- iOS 테스트 앱이 시뮬레이터 또는 실기기에서 실행된다.

---

### Phase 4. 실제 key 입력 후 광고 노출 검증

목표:
- placeholder가 아닌 실제 발급 key/adUnitId를 넣고 광고가 실제로 로드/노출되는지 검증한다.

세부 작업:
1. 실제 `mediaKey` 및 포맷별 `adUnitId` 입력
2. 아래 포맷별 검증 수행
   - Banner
   - Native
   - Video
   - Interstitial
   - Interstitial Video
   - Rewarded
3. 각 포맷에 대해 아래 이벤트 검증
   - load / loadFailed
   - opened
   - impression
   - clicked
   - completed / skipped (해당 포맷)
   - rewarded (리워드)
   - closed
4. Android / iOS 각각 결과 기록

완료 기준:
- 최소 1회 이상 실제 광고 노출 또는 실패 사유가 플랫폼 로그와 함께 명확히 기록된다.

---

### Phase 5. 결과 문서화 및 지원 범위 확정

목표:
- 검증 결과를 사용자/개발자가 혼동 없이 이해할 수 있게 문서화한다.

세부 작업:
1. README 지원 범위 업데이트
2. 미지원 항목 명시
   - 예: Bizboard 미지원, SPM 제한사항
3. 테스트 앱 상태 업데이트
   - Android/iOS 각각 빌드 성공 여부
   - 실광고 검증 여부
4. 남은 blocker와 추후 작업 분리
   - toolchain compatibility
   - 실제 배포 전 필요 작업

완료 기준:
- 저장소 문서가 실제 상태보다 과장되지 않고, 다음 액션이 명확하다.

---

## 6. 테스트 체크리스트

### 6.1 공통
- [ ] `NapSspAd.initialize()` 성공
- [ ] 잘못된 key / adUnitId에서 실패 로그 확인
- [ ] 중복 호출 시 앱 비정상 종료 없음
- [ ] 광고 이벤트 로그 UI 또는 native log 확인

### 6.2 Banner
- [ ] 로드 성공
- [ ] 노출 확인
- [ ] 클릭 이벤트 확인
- [ ] 실패 시 에러 로그 확인

### 6.3 Native
- [ ] 로드 성공
- [ ] UI 바인딩 정상
- [ ] 클릭 이벤트 확인

### 6.4 Video
- [ ] 로드 성공
- [ ] 재생 시작
- [ ] completed / skipped 이벤트 확인

### 6.5 Interstitial
- [ ] load/show 성공
- [ ] opened / impression / clicked / closed 확인

### 6.6 Interstitial Video
- [ ] load/show 성공
- [ ] opened / completed or skipped / closed 확인

### 6.7 Rewarded
- [ ] load/show 성공
- [ ] rewarded 이벤트 확인
- [ ] closed 이벤트 확인
- [ ] S2S customParams 전달 전략 기록

---

## 7. 판정 기준

### 앱 복구 판정
- FAIL: 프로젝트 자체가 없거나 build command 진입 불가
- PARTIAL: 프로젝트는 있으나 최신 툴체인/환경 문제로 빌드 실패
- PASS: 시뮬레이터/에뮬레이터 또는 실기기에서 실제 앱 실행 성공

### 광고 실검증 판정
- FAIL: 초기화 또는 광고 로드 자체가 불가
- PARTIAL: 앱은 실행되나 실광고 노출 또는 이벤트 검증 미완료
- PASS: 실제 광고 노출 및 핵심 이벤트 확인 완료

---

## 8. 실행 우선순위

1. iOS toolchain compatibility 해결
2. Android 실제 실행 환경 준비
3. 실제 key/adUnitId 확보 후 실광고 검증
4. 결과 문서 반영

---

## 9. 운영 원칙

- 추측 금지
- 빌드 성공과 실광고 성공을 분리해서 기록
- 문서에는 현재 상태만 적고, 희망 상태를 완료로 쓰지 않음
- 공식 네이티브 가이드에 있는 기능이라도 RN surface가 없으면 미지원으로 기록
- 실광고 검증은 반드시 실제 key / adUnitId 기준으로만 PASS 처리
