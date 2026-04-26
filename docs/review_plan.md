# Nap SSP React Native Plugin Review & Verification Plan

이 문서는 개발된 `react-native-nap-ssp` 플러그인과 테스트 앱이 초기 설계 및 최신 네이티브 SDK 가이드(Android v1.0.21 / iOS v2.2.1)를 준수하고 있는지 전수 조사하고, 실제 광고 노출까지 검증하기 위한 가이드라인입니다.

---

## 1. 검증 개요
- **검증 대상**: `react-native-nap-ssp` 패키지 소스, `example` 앱, `integration-test-app`, 네이티브 브릿지 코드
- **기준 문서**:
  - `nap-ssp-android-sdk-native.md` (Android 최신 가이드)
  - `nap-ssp-ios-sdk-native.md` (iOS 최신 가이드)
  - `nap_ssp_react_native_plan.docx` (초기 설계서)

---

## 2. 전수 조사 핵심 체크리스트

### 2.1 SDK 초기화 및 공통 설정 (Core)
- [ ] mediaKey 및 adUnitIds 목록의 정상 전달 여부
- [ ] Log Level 설정 연동 (`AdMixerLog`, `setDebugEnabled`)
- [ ] 미디에이션 어댑터 등록 로직 및 Pangle/AppLovin Key 반영

### 2.2 광고 포맷별 구현 (Ad Formats)
- [ ] **배너**: 생명주기 연동, 사이즈 상수 매핑, 이벤트 콜백
- [ ] **전면**: 옵션(popup, countdown 등) 전달, load/show 비동기 분리
- [ ] **네이티브**: 에셋(icon, headline 등) 바인딩, iOS v2.2.1 removeView 대응
- [ ] **리워드**: 보상 콜백 통합(`onRewarded`), Custom Params 전달

### 2.3 플랫폼 특이사항 (Platform Specifics)
- [ ] Android Gradle 의존성 및 ProGuard 규칙 반영
- [ ] iOS Info.plist 설정 및 ATT 권한 요청 처리

---

## 3. 실검증 작업 단계 (Phases)
1.  **Phase 1. 기능 매핑**: 네이티브 가이드 대비 지원 범위 분류.
2.  **Phase 2. 테스트 앱 빌드**: Android(JDK 17) 및 iOS 빌드 성공 확인.
3.  **Phase 3. 실제 노출 검증**: 실제 Key를 사용한 포맷별 로드 및 이벤트 수집 최종 확인.
4.  **Phase 4. 결과 문서화**: README 및 지원 범위 문서 최종 업데이트.
