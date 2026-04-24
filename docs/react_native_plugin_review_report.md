# Nap SSP React Native Plugin 전수 조사 및 검증 보고서
> 기준 계획서: `docs/action_plan.md` | 작성일: 2026-04-20 | 최종 재검토: 2026-04-25

본 보고서는 `docs/action_plan.md`에 정의된 4단계 작업 계획(Phase 1~4)의 이행 현황과 샘플 테스트 앱의 완성도를 전수 조사한 결과입니다. 다만 2026-04-25 재검토 기준으로, 일부 항목은 문서상 완료로 표시되었으나 실제 저장소 상태와 차이가 있음을 확인했습니다. 아래 평가는 코드, CI, 실행 가능성 기준으로 보수적으로 정리한 최신 상태입니다.

---

## 1. 종합 검증 요약

| 단계 | 중요도 | 상태 | 요약 |
|------|--------|------|------|
| **Phase 1** | CRITICAL | ✅ 완료 | iOS 초기화 연결 및 ObjC 클래스 충돌 방지(명칭 변경) 완료 |
| **Phase 2** | HIGH | ✅ 완료 | 안드로이드 ProGuard, 옵션 연동 및 네이티브 광고 바인딩 완료 |
| **Phase 3** | MEDIUM | ✅ 완료 | 생명주기, 로그 연동, 임프레션 발행 및 iOS 에러코드 매핑 완료 |
| **Phase 4** | LOW | ⚠️ 부분 완료 | New Architecture Spec 작성 및 일부 배포/테스트 자산은 존재하지만, SPM은 placeholder 상태이고 integration-test-app은 즉시 실행 가능한 검증 앱으로 완성되지 않음 |

---

## 2. 세부 이행 현황 (Phase 1-4)

### 2.1 Phase 1 — CRITICAL (런타임 안정성)
- **Task 1-1 (iOS 초기화)**: `NapSspSupport.swift` 내 `AMMediation.shared.initialize` 호출 확인.
- **Task 1-2 & 1-3 (ObjC 명칭)**: `NativeAdView.swift` -> `@objc(NapSspNativeAdUIView)`, `VideoAdView.swift` -> `@objc(NapSspVideoAdUIView)` 반영 완료. `NapSspBridge.m`과의 정합성 확보.

### 2.2 Phase 2 — HIGH (기능 완성도)
- **Task 2-1 (ProGuard)**: `android/consumer-rules.pro`에 6종 keep 규칙 및 `-dontwarn` 반영 확인.
- **Task 2-2 (Android Options)**: `Interstitial`, `Rewarded`, `InterstitialVideo` 모듈에서 `AdInfo.Builder` 옵션(popup, mute, timeout 등) 연동 확인.
- **Task 2-3 (iOS Options)**: 전면/리워드 옵션 파싱 유틸 및 SDK API 전달 확인.
- **Task 2-4 (NativeAd 바인딩)**: 안드로이드(Reflection 기반 에셋 바인딩) 및 iOS(Container 기반 v2.2.1 대응) 실제 SDK 바인딩 구현 완료.

### 2.3 Phase 3 — MEDIUM (품질 및 정합성)
- **Task 3-1 (Android 배너 생명주기)**: `LifecycleEventListener`를 통한 `onResume/onPause` 연동 확인.
- **Task 3-2 (Impression 이벤트)**: 양 플랫폼 전 포맷에서 `onAdImpression` 발행 확인.
- **Task 3-3 (Log Level)**: JS `logLevel` 설정이 네이티브 SDK(`AdMixerLog`, `setDebugEnabled`)로 전파됨을 확인.
- **Task 3-4 (iOS 에러코드)**: SDK 에러코드(0~6) 매핑 및 JS `normalizeAdError`를 통한 데이터 정규화 확인.

### 2.4 Phase 4 — LOW (아키텍처 및 도구)
- **Task 4-1 (Android 미디에이션 초기화)**: Pangle/AppLovin/UnityAds SDK에 대한 별도 초기화 로직 확인.
- **Task 4-2 (iOS SPM)**: `Package.swift` 구성과 `binaryTarget` 선언은 존재하나, checksum이 `REPLACE_WITH_OFFICIAL_CHECKSUM`으로 남아 있어 release-grade 상태는 아님.
- **Task 4-3 (테스트 앱)**: `integration-test-app`에 광고 포맷 데모 코드는 있으나, iOS 네이티브 프로젝트 파일이 없어 즉시 실행 가능한 통합 검증 앱으로 보기 어려움.
- **Task 4-4 (New Architecture)**: JSI Spec 파일(`NativeNapSspModuleSpec.ts` 등) 작성 완료.

---

## 3. 샘플 테스트 앱 및 가이드 감사

### 3.1 초보자 가이드 (README.md)
- **친절도**: "5분 안에 실행하기" 및 "2단계 사용 가이드" 섹션을 통해 초기화부터 표시까지의 과정을 직관적으로 설명함.
- **복붙 가능성**: 설치 명령어, 네이티브 설정(Gradle/Podfile), 초기화, 각 광고 포맷별 호출 코드가 **완벽한 코드 블록**으로 제공되어 그대로 복사해서 사용 가능함.
- **상세 가이드**: Android/iOS 필수 의존성 설정과 CocoaPods vs SPM 선택 가이드가 포함되어 실무 적용이 용이함.

### 3.2 기능 및 옵션 커버리지 (integration-test-app)
- **데모 코드 범위**: 배너, 전면, 리워드, 네이티브, 인라인 비디오, 전면 비디오 데모 코드가 포함되어 있음.
- **옵션 예시 포함**: 팝업형 전면 광고의 카운트다운 시간 설정, 리워드 광고의 S2S 커스텀 파라미터 전달, 음소거 옵션 등 예시가 포함되어 있음.
- **중요한 제한사항**: 현재 저장소 상태만으로는 iOS 프로젝트 파일이 없어 `react-native run-ios`로 즉시 실행되지 않으며, 실제 단말/에뮬레이터 기반 통합 검증이 완료되었다고 단정할 수 없음.

---

## 4. 최종 결론
핵심 React Native 플러그인 기능과 기본 CI 검증 경로는 유의미하게 정비되었고, `verify-js` 및 `verify-example` GitHub Actions는 2026-04-25 기준 통과했습니다. 다만 아래 항목은 아직 후속 작업이 필요합니다.

- `ios/Package.swift` checksum placeholder 해소 전까지 SPM은 참고용 수준
- `integration-test-app`은 문서상 설명과 달리 즉시 실행 가능한 완성형 통합 테스트 앱이 아님
- 공식 네이티브 가이드에 등장하는 Bizboard는 RN surface에 아직 노출되지 않음
- 일부 문서는 실제 코드/실행 상태보다 readiness를 높게 표현하고 있었음

따라서 본 프로젝트는 **CI 기준 기본 품질은 확보했지만, 배포/문서/실기기 검증 관점에서는 추가 정리가 필요한 상태**로 평가하는 것이 정확합니다.
