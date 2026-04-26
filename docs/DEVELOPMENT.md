# Development Guide & Architecture

이 문서는 `react-native-nap-ssp` 플러그인의 내부 구조와 기여(Contribution)를 위한 가이드를 제공합니다.

---

## 1. 아키텍처 원칙: "Build-Safe, Runtime-Safe"

본 플러그인은 **매체사 앱의 빌드 복잡성을 최소화**하기 위해 다음과 같은 원칙으로 설계되었습니다.

### 1.1 Reflection 기반의 벤더 SDK 로딩 (Android)
Android 네이티브 모듈은 Nap SSP SDK 및 미디에이션 SDK를 직접 참조하지 않고 **Java Reflection**을 통해 런타임에 로드합니다.
- **장점**: 매체사 앱에서 특정 SDK를 포함하지 않더라도 컴파일 에러가 발생하지 않으며, 앱 크기를 최적화할 수 있습니다.
- **구현**: `android/src/main/java/com/gwangy/NapSspSdkBridge.kt` 참조.

### 1.2 조건부 컴파일 및 Placeholder (iOS)
iOS는 `#if canImport(AdMixerMediation)`와 같은 조건부 컴파일을 사용하여 SDK 존재 여부를 판단합니다.
- **장점**: CocoaPods로 SDK를 설치하지 않은 상태에서도 `pod install` 및 빌드가 가능합니다.

---

## 2. 프로젝트 구조

- `src/`: React Native 공개 API (TypeScript).
- `android/`: Kotlin 기반 네이티브 모듈 및 뷰 매니저.
- `ios/`: Swift 기반 네이티브 모듈 및 뷰 매니저.
- `example/`: 기본적인 API 동작 확인용 샘플 앱.
- `integration-test-app/`: 실제 SDK 연동 및 Maestro 자동화 검증용 앱.

---

## 3. 기여 방법 (Contribution)

1. **이슈 확인**: 수정하거나 추가하고 싶은 기능이 있다면 이슈를 먼저 생성해 주세요.
2. **로컬 개발 환경**: 
   ```bash
   npm install
   npm run build
   ```
3. **검증**: 변경 사항을 적용한 후 `npm run verify` 및 `integration-test-app`에서 정상 동작을 확인해야 합니다.
4. **PR 생성**: `main` 브랜치를 대상으로 Pull Request를 생성해 주세요.

---

## 4. 자동 검증 파이프라인 (CI)

본 프로젝트는 GitHub Actions를 통해 모든 Pull Request와 `main` 브랜치 푸시 시점에 아래 검증을 자동으로 수행합니다.

### 4.1 JS/TS 검증 (`ci.yml`)
- **Lint & Type Check**: 코드 스타일 및 TypeScript 타입 무결성 검사.
- **Build Test**: `npm run build`를 통한 라이브러리 번들링 성공 여부 확인.
- **Smoke Test**: `scripts/smoke-test.js`를 통한 공개 API 노출 및 기본 초기화 로직 검증.

### 4.2 예제 앱 빌드 검증
- **Example Host App**: `example/ExampleHostApp` 프로젝트가 각 플랫폼(Android/iOS) 환경에서 정상적으로 스캐폴딩되고 빌드 준비가 되는지 확인합니다.

---

## 5. 코딩 컨벤션
- **TypeScript**: 엄격한 타입 체크(`strict: true`)를 준수합니다.
- **Native**: 네이티브 로그는 JS의 `logLevel` 설정을 따르도록 `NapSspEventEmitter`를 통해 중계합니다.
