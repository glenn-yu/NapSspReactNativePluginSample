# React Native Plugin 검증 작업 지시서 (CLI 실행용)

## 1. Objective
React Native Plugin 프로젝트가 초기 개발 계획서 및 Native SDK 가이드 기준으로  
모든 기능이 정상 구현되었는지 전수 검증한다.

---

## 2. Input Documents

다음 문서를 반드시 기준으로 사용한다:

- nap_ssp_react_native_plan.docx
- docs/nap-ssp-android-sdk-native.md
- docs/nap-ssp-ios-sdk-native.md

---

## 3. Target Scope

분석 대상 디렉토리:

/
├── react-native-plugin
├── android
├── ios
├── integration-test-app

---

## 4. Tasks

### 4.1 개발 계획서 기준 기능 매핑

1. nap_ssp_react_native_plan.docx에서 기능 목록 추출
2. 각 기능을 아래 형태로 정리

{
  "feature_name": "",
  "description": "",
  "expected_behavior": ""
}

3. 실제 코드에서 해당 기능 구현 여부 확인
4. 결과 상태:
- IMPLEMENTED
- PARTIAL
- MISSING

---

### 4.2 React Native Bridge 검증

검증 항목:

- JS → Native 호출
- Native → JS 이벤트

이벤트:
- impression
- click
- close
- error

출력:

{
  "bridge": {
    "js_to_native": true,
    "native_to_js": true,
    "events": {
      "impression": true,
      "click": true,
      "close": true,
      "error": true
    }
  }
}

---

### 4.3 Android SDK 검증

- SDK initialization
- Activity lifecycle
- load/show/destroy 흐름
- 에러 처리
- Proguard

상태:
- OK
- WARNING
- FAIL

---

### 4.4 iOS SDK 검증

- SDK initialization
- UIViewController lifecycle
- 광고 흐름
- delegate/callback
- memory leak

---

### 4.5 Integration Test App

경로:
integration-test-app/android
integration-test-app/ios

검증:
- 빌드 여부
- 광고 호출 코드
- 테스트 UI

---

### 4.6 예외 처리

- 광고 로드 실패
- 네트워크 실패
- 중복 호출
- lifecycle 변경

---

## 5. Output Format

{
  "summary": {
    "total_features": 0,
    "implemented": 0,
    "partial": 0,
    "missing": 0
  },
  "issues": [],
  "sdk_compliance": {
    "android": "",
    "ios": ""
  },
  "integration_test": {
    "android": true,
    "ios": true
  }
}

---

## 6. Rules

- 추측 금지
- 불명확 시 PARTIAL
- 문서 기준 필수 검증

---

## 7. Execution

- 전체 스캔
- JSON 출력
- 설명 금지
