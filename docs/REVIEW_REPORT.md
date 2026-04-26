# Nap SSP React Native Plugin 전수 조사 및 검증 보고서

> **최종 재검토 일자**: 2026-04-25 | **최초 조사 일자**: 2026-04-20
> **기준 문서**: `docs/REVIEW_PLAN.md`, `nap-ssp-android-sdk-native.md`, `nap-ssp-ios-sdk-native.md`

---

## 1. 종합 검증 요약 (2026-04-25 기준)

| 단계 | 중요도 | 상태 | 요약 |
| :--- | :--- | :---: | :--- |
| **Phase 1 (런타임 안정성)** | CRITICAL | ✅ 완료 | iOS 초기화 연결 및 ObjC 클래스 충돌 방지 완료 |
| **Phase 2 (기능 완성도)** | HIGH | ✅ 완료 | Android ProGuard, 옵션 연동 및 네이티브 광고 바인딩 완료 |
| **Phase 3 (품질 및 정합성)** | MEDIUM | ✅ 완료 | 생명주기, 로그 연동, 임프레션 발행 및 iOS 에러코드 매핑 완료 |
| **Phase 4 (아키텍처 및 도구)** | LOW | ⚠️ 부분 완료 | JSI Spec 완료, SPM은 placeholder 상태, 통합 테스트 앱 보완 중 |

---

## 2. 세부 이행 현황 및 개선 사항

### 2.1 SDK 초기화 및 공통 설정 (Core)
- **iOS 초기화**: `NapSspSupport.swift` 내 `AMMediation.shared.initialize` 호출 연결 완료. (기존 FAIL 항목 개선)
- **Log Level**: JS의 `logLevel` 설정이 네이티브 SDK(`AdMixerLog`, `setDebugEnabled`)로 정상 전파됨을 확인.
- **Mediation Adapters**: Android에서 Pangle/AppLovin/UnityAds SDK의 별도 초기화 로직 구현 확인.

### 2.2 광고 포맷별 구현
- **배너 (BannerAd)**: `onResume/onPause` 생명주기 연동(Android) 및 임프레션 이벤트 발행 확인.
- **네이티브 (NativeAd)**: 기존 Placeholder 구현에서 실제 SDK 바인딩(Android: Reflection 기반, iOS: Container 기반)으로 고도화 완료.
- **옵션 연동**: `Interstitial`, `Rewarded` 등 전 포맷에서 `AdInfo.Builder` 옵션(popup, mute, timeout 등) 파싱 및 전달 로직 확인.

### 2.3 플랫폼 특이사항 및 아키텍처
- **Android ProGuard**: `consumer-rules.pro`에 공식 가이드의 keep 규칙 6종 및 `-dontwarn` 반영 완료.
- **iOS SPM**: `Package.swift` 구조는 확보했으나, checksum이 placeholder 상태여서 실제 배포 시에는 공식 체크섬 업데이트가 필요함.
- **New Architecture**: JSI Spec 파일(`NativeNapSspModuleSpec.ts` 등) 작성을 통해 향후 TurboModules 대응 기반 마련.

---

## 3. 상세 이슈 추적 리스트 (최초 조사 시 식별된 기술 부채)

초기 조사(2026-04-20) 당시 발견된 주요 이슈들의 해결 여부 및 상태입니다.

| 심각도 | 분류 | 내용 | 상태 |
| :--- | :--- | :--- | :---: |
| **CRITICAL** | iOS Core | `AMMediation.shared.initialize()` 호출 누락 | ✅ 해결 |
| **CRITICAL** | iOS ObjC | `NativeAdView`/`VideoAdView` 클래스 명칭 충돌 | ✅ 해결 |
| **HIGH** | Android | `consumer-rules.pro` 내 SDK keep 규칙 누락 | ✅ 해결 |
| **HIGH** | Android/iOS | NativeAd 전체 Placeholder 동작 | ✅ 해결 |
| **HIGH** | Android | 전면/리워드 옵션(AdInfo.Builder) 미전달 | ✅ 해결 |
| **MEDIUM** | Android/iOS | `onAdImpression` 이벤트 누락 | ✅ 해결 |
| **LOW** | iOS | `Package.swift` checksum placeholder | ⚠️ 잔존 |
| **LOW** | Gap | Kakao Bizboard 지원 (RN Surface 미노출) | ⚠️ 잔존 |

---

## 4. 샘플 테스트 앱 (`integration-test-app`) 감사

- **친절도**: `README.md`를 통해 5분 안에 실행 가능한 가이드와 완벽한 코드 블록 제공.
- **커버리지**: 배너, 전면, 리워드, 네이티브, 비디오 등 전 포맷의 데모 코드 포함.
- **현재 상태**: 2026-04-25 기준 Android/iOS 모두 빌드 및 시뮬레이터 실행이 가능하도록 복구되었으나, **실제 광고 Key(`mediaKey`)를 이용한 실광고 노출 검증** 단계가 최종 완료되어야 함.

---

## 5. 결론 및 향후 과제

본 프로젝트는 2026-04-20 최초 조사 당시의 많은 결함들을 Phase 1~3 작업을 통해 대부분 해소하였습니다. 현재 CI 기준 기본 품질은 확보되었으며, 다음 단계를 통해 최종 안정성을 검증해야 합니다.

### 5.1 즉시 실행 과제
1. **실광고 Key 검증**: 실제 `mediaKey` 및 `adUnitId`를 입력하여 광고 노출 및 이벤트 수집을 최종 확정.
2. **SPM 완성**: 배포 직전 공식 SDK 체크섬을 반영하여 SPM 설치가 가능하도록 수정. (`docs/SPM_GUIDE.md` 참조)

### 5.2 중장기 과제 (TODO)
- **최종 네이티브 콜백 확인**: 최종 벤더 SDK 버전과 모든 미디에이션/이벤트 엣지의 실제 콜백 명칭 정합성 최종 확인.
- **실기기 E2E 테스트**: 실제 기기에서 광고 응답(Real SDK Response) 기반의 엔드투엔드 네이티브 테스트 수행.
- **배포 자동화**: 패키지 메타데이터 확정 후 배포 자동화(CI/CD) 파이프라인 구축.
- **비즈보드 대응**: 향후 비즈보드 단독 지면 정책에 따른 RN API 추가 고려.
