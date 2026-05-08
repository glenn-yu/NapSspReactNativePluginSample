# Android 패키지명 변경 검증 테스트 가이드

**플러그인:** `react-native-nap-ssp`
**변경 내용:** Android 패키지명 `com.gwangy` → `com.nasmedia.admixerssp`
**검증 버전:** `0.1.7`
**작성일:** 2026-05-08

---

## 개요

이 문서는 `react-native-nap-ssp` 플러그인의 Android 네임스페이스가
`com.gwangy` 에서 `com.nasmedia.admixerssp` 로 정상적으로 변경되었는지
처음부터 끝까지 단계별로 검증하기 위한 체크리스트 가이드입니다.

> **중요:** 이전 버전(`com.gwangy`)을 사용하던 호스트 앱은 ProGuard keep 규칙과
> 클래스 참조를 반드시 새 패키지명으로 업데이트해야 합니다.

---

## 1. 환경 설정 (사전 준비)

### 1.1 필수 도구 확인

- [ ] **Android Studio Hedgehog (2023.1.1)** 이상 설치 확인
  ```
  Android Studio > About Android Studio 에서 버전 확인
  ```

- [ ] **JDK 17 (Amazon Corretto 17 권장)** 설치 및 JAVA_HOME 설정 확인
  ```bash
  java -version
  # 출력 예: openjdk version "17.0.x" ...
  ```
  > 프로젝트 `gradle.properties`에서 `org.gradle.java.home=C:/Users/Administrator/.jdks/corretto-17.0.18` 로 고정되어 있음.
  > 경로가 다를 경우 `integration-test-app/android/gradle.properties` 의 해당 줄을 수정.

- [ ] **Node.js 18+** 및 **npm 9+** 설치 확인
  ```bash
  node -v
  npm -v
  ```

- [ ] **Android 에뮬레이터 또는 실물 기기** 준비
  - 에뮬레이터: API 23 (Android 6.0) 이상, x86_64 이미지 권장
  - 실물 기기: ADB 디버깅 활성화 (`개발자 옵션 > USB 디버깅`)
  - 연결 확인:
    ```bash
    adb devices
    # 기기가 'device' 상태로 표시되어야 함
    ```

- [ ] **테스트에 사용할 광고 유닛 ID** 준비 (Nap SSP 콘솔에서 발급된 것)

---

## 2. 신규 설치 및 패키지명 검증

### 2.1 기존 캐시 제거 (이전 버전 잔재 방지)

- [ ] 기존 node_modules 및 Gradle 캐시 삭제
  ```bash
  # 프로젝트 루트에서 실행
  cd /path/to/your-rn-project

  rm -rf node_modules
  rm -rf android/.gradle
  rm -rf android/app/build
  ```

- [ ] npm 캐시 강제 정리
  ```bash
  npm cache clean --force
  ```

### 2.2 npm 신규 설치

- [ ] `react-native-nap-ssp@0.1.7` 설치
  ```bash
  npm install react-native-nap-ssp@0.1.7
  ```

- [ ] 설치된 버전 확인
  ```bash
  npm list react-native-nap-ssp
  # 출력: react-native-nap-ssp@0.1.7
  ```

### 2.3 node_modules 내 패키지명 검증

- [ ] `package.json` 버전 확인
  ```bash
  cat node_modules/react-native-nap-ssp/package.json | grep '"version"'
  # 출력: "version": "0.1.7"
  ```

- [ ] Android 네임스페이스 확인 (`com.nasmedia.admixerssp` 이어야 함)
  ```bash
  grep -r "namespace" node_modules/react-native-nap-ssp/android/build.gradle
  # 출력: namespace 'com.nasmedia.admixerssp'
  ```

- [ ] AndroidManifest.xml 패키지명 확인
  ```bash
  cat node_modules/react-native-nap-ssp/android/src/main/AndroidManifest.xml
  # package="com.nasmedia.admixerssp" 이어야 함
  ```

- [ ] 소스 파일 경로에 구 패키지명(`com/gwangy`) 이 없는지 확인
  ```bash
  find node_modules/react-native-nap-ssp/android/src -type f -name "*.kt" | head -20
  # 모든 파일 경로가 com/nasmedia/admixerssp/ 하위에 있어야 함
  ```

- [ ] 소스 코드 내 구 패키지명 잔재 검색 (결과가 없어야 함)
  ```bash
  grep -r "com\.gwangy" node_modules/react-native-nap-ssp/android/src/
  # 아무 출력도 없어야 정상
  ```

- [ ] consumer-rules.pro 내 keep 규칙 확인
  ```bash
  cat node_modules/react-native-nap-ssp/android/consumer-rules.pro
  # -keep class com.nasmedia.admixerssp.** { *; } 규칙이 있어야 함
  # 주의: com.gwangy keep 규칙은 삭제되거나 별도로 남아있을 수 있음 (7절 참조)
  ```

**합격 기준:** 위 다섯 가지 검사에서 `com.gwangy` 가 소스 경로 또는 코드에 등장하지 않고,
`com.nasmedia.admixerssp` 가 정상적으로 표시되면 합격.

---

## 3. 호스트 앱 ProGuard 규칙 설정

> **릴리즈 빌드 전에 반드시 수행해야 합니다.**

### 3.1 호스트 앱 ProGuard 파일에 keep 규칙 추가

- [ ] 호스트 앱의 `android/app/proguard-rules.pro` 파일을 열고 아래 규칙 추가
  (파일이 없으면 새로 생성)

  ```proguard
  # ============================================================
  # react-native-nap-ssp Android 패키지명 변경 대응 keep 규칙
  # com.gwangy -> com.nasmedia.admixerssp (v0.1.7 이상)
  # ============================================================

  # 플러그인 브리지 모듈 (RN 패키지 등록 및 JS 호출 유지)
  -keep class com.nasmedia.admixerssp.** { *; }
  -dontwarn com.nasmedia.admixerssp.**

  # Nap SSP 벤더 SDK 및 미디에이션 어댑터
  -keep class com.nasmedia.admanager.** { *; }
  -dontwarn com.nasmedia.admanager.**
  -keep class com.nasmedia.adfit.** { *; }
  -dontwarn com.nasmedia.adfit.**
  -keep interface com.nasmedia.** { *; }
  -dontwarn com.nasmedia.**

  # Pangle (바이트댄스) 미디에이션
  -keep class com.pangle.** { *; }
  -dontwarn com.pangle.**

  # AppLovin 미디에이션
  -keep class com.applovin.** { *; }
  -dontwarn com.applovin.**

  # React Native 브리지 (기존 규칙이 없는 경우에만 추가)
  -keep class com.facebook.react.** { *; }
  -dontwarn com.facebook.react.**
  ```

  > **이전 버전에서 마이그레이션하는 경우:**
  > `com.gwangy.**` keep 규칙이 기존 파일에 있다면 위 규칙으로 교체합니다.
  > 구 규칙을 남겨두어도 빌드 오류는 없지만, 불필요한 항목은 제거하는 것을 권장합니다.

- [ ] `android/app/build.gradle` 에서 ProGuard 설정이 활성화되어 있는지 확인
  ```groovy
  buildTypes {
      release {
          minifyEnabled true
          proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
      }
  }
  ```

---

## 4. 디버그 빌드 테스트

### 4.1 Metro 번들러 시작

- [ ] Metro 서버 시작 (새 터미널 창)
  ```bash
  # 프로젝트 루트에서
  npx react-native start --reset-cache
  ```
  > `--reset-cache` 옵션으로 이전 JS 번들 캐시를 초기화합니다.

### 4.2 디버그 APK 빌드 및 기기 설치

- [ ] 디버그 빌드 실행
  ```bash
  # 프로젝트 루트에서
  npx react-native run-android
  ```
  또는 Gradle 직접 실행:
  ```bash
  cd android
  ./gradlew installDebug
  ```

- [ ] 빌드 성공 확인 (에러 없이 `BUILD SUCCESSFUL` 출력)

### 4.3 런타임 동작 검증

- [ ] 앱이 기기에서 정상 실행되는지 확인 (크래시 없음)

- [ ] 로그캣으로 플러그인 초기화 확인
  ```bash
  adb logcat -s "NapSsp" "ReactNativeJS" | grep -i "nap\|admixer\|ssp"
  ```
  - `com.nasmedia.admixerssp` 관련 초기화 로그 확인
  - `ClassNotFoundException: com.gwangy.*` 오류가 없어야 함

- [ ] `ClassNotFoundException` 전체 검색
  ```bash
  adb logcat | grep -i "ClassNotFoundException"
  # 아무 출력도 없어야 정상
  ```

- [ ] 광고 로드 테스트
  - 배너 광고가 화면에 표시되는지 확인
  - 전면 광고(Interstitial)가 정상적으로 노출되는지 확인
  - 보상형 광고(Rewarded) 정상 작동 확인
  - 네이티브 광고 렌더링 정상 여부 확인

- [ ] 광고 로드 실패 시 로그 패턴 확인
  ```bash
  adb logcat | grep -iE "admixer|napmedia|ssp" | grep -i "error\|fail\|exception"
  ```

**합격 기준:** `BUILD SUCCESSFUL`, 앱 정상 실행, `ClassNotFoundException` 없음,
배너·전면 광고 중 최소 1종 이상 정상 노출.

---

## 5. 릴리즈 빌드 테스트 (ProGuard 활성화)

### 5.1 릴리즈 APK 빌드

- [ ] 서명 키스토어가 설정되어 있는지 확인
  (`android/app/build.gradle` 의 `signingConfigs` 블록 또는 환경 변수)

- [ ] 릴리즈 빌드 실행
  ```bash
  cd android
  ./gradlew assembleRelease
  ```
  > 빌드 시간: 환경에 따라 5~15분 소요될 수 있습니다.

- [ ] 빌드 결과 파일 위치 확인
  ```bash
  ls -lh android/app/build/outputs/apk/release/
  # app-release.apk 또는 app-release-unsigned.apk 가 있어야 함
  ```

### 5.2 ProGuard 난독화 결과 검증

- [ ] mapping.txt 파일 생성 확인
  ```bash
  ls android/app/build/outputs/mapping/release/mapping.txt
  ```

- [ ] `com.nasmedia.admixerssp` 클래스가 keep 처리되어 mapping에 남아있는지 확인
  ```bash
  grep "com.nasmedia.admixerssp" android/app/build/outputs/mapping/release/mapping.txt | head -20
  # NapSspPackage, NapSspModule 등의 클래스명이 그대로 남아있어야 함
  ```

- [ ] 구 패키지명이 mapping에 등장하지 않는지 확인 (결과가 없어야 함)
  ```bash
  grep "com.gwangy" android/app/build/outputs/mapping/release/mapping.txt
  # 아무 출력도 없어야 정상
  ```

### 5.3 릴리즈 APK 설치 및 런타임 검증

- [ ] 릴리즈 APK를 기기에 직접 설치
  ```bash
  adb install android/app/build/outputs/apk/release/app-release.apk
  # 이미 설치된 경우: adb install -r ...
  ```

- [ ] 앱 실행 후 크래시 없음 확인

- [ ] 로그캣에서 `ClassNotFoundException` 및 ProGuard 관련 오류 없음 확인
  ```bash
  adb logcat | grep -iE "ClassNotFoundException|NoClassDefFoundError|ProGuard"
  ```

- [ ] 광고가 릴리즈 빌드에서도 정상 노출되는지 확인
  (디버그와 동일한 광고 유닛 ID 사용)

**합격 기준:** `./gradlew assembleRelease` 에서 `BUILD SUCCESSFUL`,
mapping.txt 에 `com.gwangy` 없음, 릴리즈 APK 설치 후 앱 정상 실행 및 광고 노출.

---

## 6. Gradle 빌드 출력물 심층 검증 (선택)

> 추가 확인이 필요한 경우 수행하는 선택 항목입니다.

- [ ] 빌드된 AAR 내부 패키지 구조 확인
  ```bash
  # 플러그인 라이브러리 AAR 빌드 (integration-test-app 기준)
  cd node_modules/react-native-nap-ssp/android
  # 또는 integration-test-app 프로젝트 내 :react-native-nap-ssp 모듈
  ./gradlew :react-native-nap-ssp:assembleDebug

  # AAR 파일에서 classes.jar 추출하여 패키지 확인
  unzip -p build/outputs/aar/*.aar classes.jar | jar tf /dev/stdin | grep "com/"
  # com/nasmedia/admixerssp/ 경로만 있어야 함 (com/gwangy/ 없어야 함)
  ```

- [ ] Gradle 의존성 트리에서 플러그인 모듈 확인
  ```bash
  cd android
  ./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep "nap-ssp\|admixerssp"
  ```

---

## 7. 합격/불합격 판정 기준 요약

| 항목 | 합격 조건 | 불합격 조건 |
|------|-----------|-------------|
| npm 설치 버전 | `0.1.7` 정확히 설치 | 다른 버전이 설치됨 |
| namespace 확인 | `com.nasmedia.admixerssp` | `com.gwangy` 가 남아있음 |
| 소스 경로 | 모두 `com/nasmedia/admixerssp/` | `com/gwangy/` 경로 파일 존재 |
| 디버그 빌드 | `BUILD SUCCESSFUL` | 빌드 오류 |
| 런타임 (디버그) | 앱 정상 실행, ClassNotFoundException 없음 | 크래시 또는 ClassNotFoundException |
| 광고 노출 (디버그) | 최소 1종 이상 정상 노출 | 광고 로드 실패 (네트워크 외 원인) |
| 릴리즈 빌드 | `BUILD SUCCESSFUL` | ProGuard 오류로 빌드 실패 |
| mapping.txt | `com.gwangy` 없음 | `com.gwangy` 가 mapping에 등장 |
| 런타임 (릴리즈) | 앱 정상 실행, 광고 노출 | 크래시, NoClassDefFoundError |

---

## 8. 자주 발생하는 오류 패턴과 진단 방법

### 8.1 `ClassNotFoundException: com.gwangy.NapSspPackage`

**원인:** 이전 버전의 패키지 등록 코드가 `MainApplication.kt` 또는 `MainApplication.java` 에 남아있는 경우.

**진단:**
```bash
grep -r "com.gwangy" android/app/src/
```

**해결:**
```kotlin
// Before (잘못된 코드)
packages.add(com.gwangy.NapSspPackage())

// After (올바른 코드 — 자동링킹 사용 시 이 줄 자체가 불필요)
// react-native-nap-ssp는 자동링킹을 지원하므로 수동 등록 불필요
```

---

### 8.2 릴리즈 빌드 시 `java.lang.NoClassDefFoundError`

**원인:** ProGuard keep 규칙에서 신규 패키지명 `com.nasmedia.admixerssp` 를 누락한 경우.

**진단:**
```bash
grep "com.nasmedia.admixerssp" android/app/proguard-rules.pro
```

**해결:** 3절의 ProGuard 규칙을 `android/app/proguard-rules.pro` 에 추가.

---

### 8.3 `consumer-rules.pro` 에 구 패키지명 keep 규칙이 있는 경우

**증상:** `com.gwangy.**` keep 규칙이 npm 패키지 내 `consumer-rules.pro` 에 남아있음.

**진단:**
```bash
cat node_modules/react-native-nap-ssp/android/consumer-rules.pro | grep "gwangy"
```

**처리 방법:**
- 규칙이 있더라도 해당 패키지가 존재하지 않으므로 빌드 오류는 발생하지 않음
- 그러나 `-keep class com.gwangy.**` 가 남아있으면 경고(`W/` 로그)가 발생할 수 있음
- 해당 줄이 npm 패키지 내부에 있으므로 직접 수정하지 말고,
  호스트 앱 `proguard-rules.pro` 에 `-dontwarn com.gwangy.**` 를 추가하여 경고 억제 가능

---

### 8.4 `Duplicate class` 빌드 오류

**원인:** 이전 버전(`0.1.6` 이하)과 `0.1.7` 이 동시에 의존성 트리에 포함된 경우.

**진단:**
```bash
cd android
./gradlew :app:dependencies | grep "react-native-nap-ssp"
```

**해결:**
```bash
npm ls react-native-nap-ssp
# 중복 버전이 없는지 확인
npm dedupe
```

---

### 8.5 `Manifest merger failed` 오류

**원인:** 호스트 앱 `AndroidManifest.xml` 에 `package="com.gwangy"` 를 참조하는 항목이 있는 경우 (드문 경우).

**진단:**
```bash
grep -r "com.gwangy" android/app/src/main/AndroidManifest.xml
```

**해결:** 해당 참조를 `com.nasmedia.admixerssp` 로 교체.

---

### 8.6 Gradle 캐시 문제로 구 클래스가 남아있는 경우

**증상:** npm install 후에도 이전 패키지명 클래스가 빌드에 포함됨.

**해결:**
```bash
cd android
./gradlew clean
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.gwangy
```
이후 `./gradlew assembleDebug` 재실행.

---

## 참고 정보

- **플러그인 Android namespace:** `com.nasmedia.admixerssp`
- **플러그인 Android manifest package:** `com.nasmedia.admixerssp`
- **핵심 소스 파일:** `NapSspPackage.kt`, `NapSspModule.kt`, `BannerViewManager.kt` 등 (모두 `com.nasmedia.admixerssp` 패키지)
- **consumer-rules.pro 위치:** `node_modules/react-native-nap-ssp/android/consumer-rules.pro`
- **지원 최소 Android SDK:** API 21 (라이브러리), API 23 (integration-test-app)
- **컴파일 SDK:** 34
- **Kotlin 버전:** 1.9.22 (integration-test-app 기준)
- **JDK 요구 버전:** 17
