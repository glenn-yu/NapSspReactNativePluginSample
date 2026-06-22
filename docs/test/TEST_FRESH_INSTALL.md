# react-native-nap-ssp@0.1.7 — 신규 설치 검증 가이드

> **목적**: npm에서 `react-native-nap-ssp@0.1.7`을 완전히 새로운 React Native 프로젝트에 설치하고 광고가 정상적으로 초기화·표시되는지 단계별로 검증합니다.
> 이전 컨텍스트 없이 처음 이 플러그인을 사용하는 개발자를 기준으로 작성되었습니다.

---

## 사전 체크리스트 (Prerequisites)

아래 항목을 모두 준비한 뒤 다음 단계로 진행하세요.

### 공통

- [ ] **Node.js 18 이상** 설치 확인
  ```bash
  node -v
  # 출력 예: v18.20.3 (18.x 또는 20.x 권장)
  ```
- [ ] **npm 9 이상** 또는 **Yarn 1.22 이상** 설치 확인
  ```bash
  npm -v
  # 출력 예: 9.8.1
  ```
- [ ] **React Native CLI** 설치 확인 (npx 방식으로 사용하므로 전역 설치 불필요)
  ```bash
  npx @react-native-community/cli --version
  # 처음 실행 시 자동 다운로드됩니다.
  ```
- [ ] **나스미디어 미디어 키(Media Key)** 및 **광고 단위 ID(Ad Unit ID)** 발급 완료
  - 미발급 시 광고가 로드되지 않습니다.
  - 문의: nap_adx@nasmedia.co.kr

### Android

- [ ] **Android Studio** (Hedgehog 이상) 설치
- [ ] **JDK 17** 설치 및 `JAVA_HOME` 환경 변수 설정
  ```bash
  java -version
  # 출력 예: openjdk version "17.0.x"
  ```
- [ ] **Android SDK** Platform 34, Build Tools 34 설치 (Android Studio SDK Manager에서 확인)
- [ ] **`ANDROID_HOME`** 환경 변수 설정
  ```bash
  echo $ANDROID_HOME
  # 출력 예: /Users/username/Library/Android/sdk  (macOS)
  #          C:\Users\username\AppData\Local\Android\Sdk  (Windows)
  ```
- [ ] Android 에뮬레이터 또는 실기기 연결 확인 (API 21 이상)
  ```bash
  adb devices
  # 출력 예: emulator-5554   device
  ```

### iOS (macOS 전용)

- [ ] **Xcode 15.3 이상** 설치 및 Command Line Tools 활성화
  ```bash
  xcode-select -p
  # 출력 예: /Applications/Xcode.app/Contents/Developer
  ```
- [ ] **CocoaPods 1.13 이상** 설치
  ```bash
  pod --version
  # 출력 예: 1.15.2
  ```
- [ ] iOS 시뮬레이터 또는 실기기 준비 (iOS 14.0 이상)

---

## Step 1 — 새 React Native 프로젝트 생성

```bash
# 작업 디렉토리로 이동 (예: ~/Projects)
cd ~/Projects

# 새 프로젝트 생성 (TypeScript 템플릿 기본 포함)
npx @react-native-community/cli init NapSspTestApp

# 생성 완료 후 프로젝트 디렉토리로 이동
cd NapSspTestApp
```

- [ ] 프로젝트 생성이 완료되면 `android/`, `ios/`, `App.tsx` 파일이 있는지 확인
  ```bash
  ls
  # android  App.tsx  ios  node_modules  package.json  ...
  ```

---

## Step 2 — 플러그인 설치

```bash
# NapSspTestApp 디렉토리에서 실행
npm install react-native-nap-ssp@0.1.7
```

예상 출력:
```
added 1 package, and audited XXX packages in Xs
```

- [ ] 설치 중 에러가 없어야 합니다 (`npm warn`은 무시 가능).

---

## Step 3 — 패키지 내용물 확인

설치 후 `node_modules/react-native-nap-ssp/` 디렉토리를 직접 확인합니다.

```bash
ls node_modules/react-native-nap-ssp/
```

- [ ] `android/` 폴더 존재 확인
- [ ] `ios/` 폴더 존재 확인
- [ ] `lib/` 폴더 존재 확인 (컴파일된 JS/TS 출력)
- [ ] `NapSspPlugin.podspec` 파일 존재 확인
- [ ] `docs/` 폴더 존재 확인 (가이드 문서 포함)

```bash
# 핵심 폴더 존재 여부 일괄 확인
ls node_modules/react-native-nap-ssp/android/src/
ls node_modules/react-native-nap-ssp/ios/
ls node_modules/react-native-nap-ssp/lib/

# 패키지 버전 재확인
cat node_modules/react-native-nap-ssp/package.json | grep '"version"'
# 출력: "version": "0.1.7"
```

---

## Step 4 — Android 설정

### 4-1. 프로젝트 루트 `android/build.gradle` — Maven 리포지토리 추가

`android/build.gradle` 파일의 `allprojects > repositories` 블록에 아래 두 Maven 저장소를 추가합니다.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // 나스미디어 AdMixer SDK 의존 저장소
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }
    }
}
```

> React Native 0.73+ 프로젝트는 `allprojects` 블록이 없는 경우가 있습니다.
> 그럴 경우 `android/settings.gradle`의 `dependencyResolutionManagement > repositories` 블록에도 동일하게 추가하세요.

- [ ] 저장소 추가 완료

### 4-2. 앱 모듈 `android/app/build.gradle` — 의존성 추가

```gradle
dependencies {
    // ... 기존 의존성 유지 ...
    implementation 'io.github.nasmedia-tech:admixer-ssp:2.0.0'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.2.0'
}
```

- [ ] 의존성 추가 완료

### 4-3. `android/app/src/main/AndroidManifest.xml` — 필수 권한 추가

```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    ...
</manifest>
```

- [ ] 권한 추가 완료

### 4-4. 플러그인 namespace 확인

플러그인의 Android 빌드 설정에 올바른 namespace가 있는지 확인합니다.

```bash
grep -n "namespace" node_modules/react-native-nap-ssp/android/build.gradle
# 출력: namespace 'com.nasmedia.admixerssp'
```

- [ ] `com.nasmedia.admixerssp` namespace 확인 완료

### 4-5. Android Studio Gradle Sync

Android Studio에서 프로젝트를 열고 **File > Sync Project with Gradle Files** 를 실행하거나 터미널에서 아래를 실행합니다.

```bash
cd android && ./gradlew dependencies --configuration debugRuntimeClasspath 2>&1 | head -40
cd ..
```

- [ ] Gradle Sync 중 에러 없음 확인

---

## Step 5 — iOS 설정

> macOS 환경에서만 해당됩니다. Windows/Linux 사용자는 이 단계를 건너뜁니다.

### 5-1. `ios/Podfile` 설정 확인 및 수정

`ios/Podfile`을 열고 아래 내용을 확인합니다.

```ruby
platform :ios, '14.0'   # 14.0 이상이어야 합니다 (13.0 이상도 지원)

target 'NapSspTestApp' do
  config = use_native_modules!
  use_react_native!(
    :path => config[:reactNativePath],
  )
  # NapSspPlugin은 use_native_modules! 가 자동으로 포함시킵니다.
end
```

> `platform :ios` 버전이 13.0 미만이면 `14.0` 이상으로 변경하세요.

- [ ] `platform :ios, '14.0'` (또는 그 이상) 설정 확인

### 5-2. Pod 설치

```bash
cd ios
pod install
cd ..
```

예상 출력 (일부):
```
Analyzing dependencies
Downloading dependencies
Installing NapSspPlugin (0.1.7)
Installing AdMixerMediation (x.x.x)
...
Pod installation complete! There are XX dependencies from the Podfile
```

- [ ] `Installing NapSspPlugin (0.1.7)` 라인이 출력에 포함되는지 확인
- [ ] `Pod installation complete!` 메시지 확인
- [ ] `ios/NapSspTestApp.xcworkspace` 파일 생성 확인

```bash
ls ios/*.xcworkspace
# 출력: ios/NapSspTestApp.xcworkspace
```

### 5-3. NapSspPlugin Pod 해석 확인

```bash
cat ios/Podfile.lock | grep NapSspPlugin
# 출력 예:
#   - NapSspPlugin (0.1.7)
#   NapSspPlugin: ...
```

- [ ] `NapSspPlugin (0.1.7)` 버전이 Podfile.lock에 기록되어 있는지 확인

---

## Step 6 — 최소 통합 코드 작성

`App.tsx`를 아래 내용으로 교체합니다. 미디어 키와 광고 단위 ID는 나스미디어 운영팀에서 발급받은 실제 값으로 변경하세요.

```tsx
/**
 * NapSspTestApp — 최소 통합 테스트
 * react-native-nap-ssp@0.1.7
 */
import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { NapSspAd, BannerAd } from 'react-native-nap-ssp';

// ── 여기에 실제 발급받은 값을 입력하세요 ───────────────────────
const MEDIA_KEY = 'YOUR_MEDIA_KEY';       // 나스미디어 발급 미디어 키
const BANNER_AD_UNIT_ID = 'YOUR_BANNER_AD_UNIT_ID';  // 배너 광고 단위 ID
// ──────────────────────────────────────────────────────────────

export default function App(): React.JSX.Element {
  const [initStatus, setInitStatus] = useState<string>('초기화 중...');
  const [bannerStatus, setBannerStatus] = useState<string>('배너 광고 대기');

  useEffect(() => {
    // SDK는 앱 시작 시 한 번만 초기화합니다.
    try {
      NapSspAd.initialize({
        mediaKey: MEDIA_KEY,
        adUnitIds: [BANNER_AD_UNIT_ID],
        logLevel: 'debug',
      });
      setInitStatus('SDK 초기화 완료');
      console.log('[NapSsp] SDK initialized successfully');
    } catch (e) {
      const msg = `SDK 초기화 실패: ${e}`;
      setInitStatus(msg);
      console.error('[NapSsp]', msg);
    }
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentInsetAdjustmentBehavior="automatic">
        <View style={styles.header}>
          <Text style={styles.title}>NapSsp 테스트 앱</Text>
          <Text style={styles.version}>react-native-nap-ssp@0.1.7</Text>
          <Text style={styles.status}>{initStatus}</Text>
          <Text style={styles.status}>{bannerStatus}</Text>
        </View>

        {/* 배너 광고 — 320x50 기본 사이즈 */}
        <View style={styles.bannerContainer}>
          <Text style={styles.label}>배너 광고 (320x50)</Text>
          <BannerAd
            adUnitId={BANNER_AD_UNIT_ID}
            size="BANNER_320x50"
            style={styles.banner}
            onAdLoaded={() => {
              const msg = '배너 광고 로드 성공';
              setBannerStatus(msg);
              console.log('[NapSsp]', msg);
            }}
            onAdFailedToLoad={(error: any) => {
              const msg = `배너 광고 로드 실패: ${JSON.stringify(error)}`;
              setBannerStatus(msg);
              console.warn('[NapSsp]', msg);
            }}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F5F5' },
  header: { padding: 20, backgroundColor: '#fff', marginBottom: 16 },
  title: { fontSize: 20, fontWeight: 'bold', marginBottom: 4 },
  version: { fontSize: 12, color: '#888', marginBottom: 8 },
  status: { fontSize: 14, color: '#333', marginTop: 4 },
  bannerContainer: { alignItems: 'center', padding: 16, backgroundColor: '#fff' },
  label: { fontSize: 13, color: '#555', marginBottom: 8 },
  banner: { width: 320, height: 50 },
});
```

- [ ] `MEDIA_KEY`와 `BANNER_AD_UNIT_ID`에 실제 발급 값 입력
- [ ] 파일 저장 완료

---

## Step 7 — Android 빌드 및 실행

```bash
# NapSspTestApp 루트 디렉토리에서 실행
npx react-native run-android
```

### 예상 정상 출력 (Metro 번들러 + Gradle)

```
info Starting JS server...
info Building and installing the app on the device...

BUILD SUCCESSFUL in Xs

info Connecting to the development server...
info Starting the app on "emulator-5554"...
```

- [ ] Metro 번들러가 http://localhost:8081 에서 기동되는 것 확인
- [ ] `BUILD SUCCESSFUL` 메시지 확인
- [ ] 에뮬레이터/기기 화면에 앱이 실행되고 "NapSsp 테스트 앱" 화면 표시 확인
- [ ] 화면 상단 상태 텍스트에 "SDK 초기화 완료" 표시 확인

### Android Metro 로그 확인

Metro 창 또는 `adb logcat`에서 아래 태그를 확인합니다.

```bash
# 별도 터미널에서 실행
adb logcat -s NapSsp ReactNativeJS 2>/dev/null
```

- [ ] `[NapSsp] SDK initialized successfully` 로그 확인
- [ ] 배너 광고 로드 시 `[NapSsp] 배너 광고 로드 성공` 로그 확인 (또는 실패 메시지로 원인 파악)

---

## Step 8 — iOS 빌드 및 실행

> macOS 환경에서만 해당됩니다.

```bash
# NapSspTestApp 루트 디렉토리에서 실행
npx react-native run-ios
```

또는 특정 시뮬레이터를 지정하려면:

```bash
npx react-native run-ios --simulator "iPhone 15"
```

### 예상 정상 출력

```
info Found Xcode workspace "NapSspTestApp.xcworkspace"
info Building (using "xcodebuild")...
BUILD SUCCEEDED

info Launching "NapSspTestApp" on iPhone 15
```

- [ ] `Found Xcode workspace "NapSspTestApp.xcworkspace"` 메시지 확인 (`.xcodeproj`가 아닌 `.xcworkspace` 사용 확인)
- [ ] `BUILD SUCCEEDED` 확인
- [ ] 시뮬레이터에 앱이 실행되고 "NapSsp 테스트 앱" 화면 표시 확인
- [ ] 화면 상단 상태 텍스트에 "SDK 초기화 완료" 표시 확인

### iOS 네이티브 로그 확인

Xcode의 Console 또는 아래 명령으로 확인합니다.

```bash
# xcrun simctl을 통해 시뮬레이터 로그 스트림
xcrun simctl spawn booted log stream --predicate 'process == "NapSspTestApp"' 2>/dev/null
```

- [ ] `[NapSsp] SDK initialized successfully` 로그 확인
- [ ] 배너 광고 로드 성공 로그 확인

---

## Step 9 — Metro 번들러 및 네이티브 로그 확인 항목

### Metro 번들러에서 정상 신호

| 확인 항목 | 예상 출력 |
| :--- | :--- |
| 번들러 기동 | `Metro waiting on http://localhost:8081` |
| 번들 빌드 성공 | `BUNDLE ./index.js` 완료 |
| JS 모듈 임포트 에러 없음 | 빨간 에러 화면(Red Box)이 나타나지 않아야 함 |

### 네이티브 로그에서 정상 신호

| 플랫폼 | 확인 로그 |
| :--- | :--- |
| Android (adb logcat) | `[NapSsp] SDK initialized successfully` |
| Android | `AdMixer` 태그로 SDK 초기화 로그 |
| iOS (Xcode Console) | `[NapSsp] SDK initialized successfully` |
| iOS | `NapSspPlugin` 관련 초기화 로그 |

### 광고 로드 성공 확인

- [ ] `onAdLoaded` 콜백이 호출되어 화면에 "배너 광고 로드 성공" 텍스트 표시
- [ ] 배너 광고 영역(320x50)에 광고 소재 또는 테스트 플레이스홀더 표시

> **참고**: DEBUG 빌드에서는 실제 광고 대신 플레이스홀더가 표시될 수 있습니다.
> 실제 광고 소재 노출은 RELEASE 빌드 + 실기기에서 검증하세요.

---

## Step 10 — 주요 장애 원인 및 해결 방법

### Android

---

#### [오류] Gradle Sync 실패: Could not resolve `io.github.nasmedia-tech:admixer-ssp`

**원인**: Maven 리포지토리가 누락되었습니다.

**해결**:
1. `android/build.gradle`의 `allprojects > repositories`에 아래 두 줄 추가 확인
   ```gradle
   maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
   maven { url 'https://artifact.bytedance.com/repository/pangle/' }
   ```
2. `android/settings.gradle`의 `dependencyResolutionManagement` 블록에도 동일하게 추가 (RN 0.73+)
3. Android Studio에서 **Sync Project with Gradle Files** 재실행

---

#### [오류] `Unsupported class file major version`

**원인**: JDK 버전이 17이 아닙니다.

**해결**:
```bash
java -version
# JDK 17이 아니면 JDK 17을 설치하고 JAVA_HOME을 업데이트합니다.
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
# Windows: 시스템 환경 변수 > JAVA_HOME = C:\Program Files\Java\jdk-17
```

Android Studio 설정: **File > Project Structure > SDK Location > JDK Location**을 JDK 17 경로로 변경

---

#### [오류] `NapSspXXX is not linked` 또는 네이티브 모듈을 찾을 수 없음

**원인**: Gradle 캐시 또는 Metro 캐시 문제입니다.

**해결**:
```bash
# Metro 캐시 초기화 후 재실행
npx react-native start --reset-cache

# 별도 터미널에서
npx react-native run-android
```

또는 Android Gradle 클린 빌드:
```bash
cd android && ./gradlew clean && cd ..
npx react-native run-android
```

---

#### [오류] 릴리즈 빌드에서 광고가 나오지 않음

**원인**: ProGuard가 SDK 클래스를 난독화했습니다.

**해결**: `android/app/proguard-rules.pro`에 아래 규칙 추가
```proguard
-keep class com.nasmedia.admixerssp.** { *; }
-keep class com.nasmedia.admanager.**  { *; }
-keep class com.nasmedia.adfit.**      { *; }
-keep class com.nasmedia.pangle.**     { *; }
-keep class com.google.android.gms.ads.** { *; }
```

---

### iOS

---

#### [오류] `pod install` 실패: Unable to find a specification for `AdMixerMediation`

**원인**: CocoaPods 스펙 저장소가 오래되었거나 플러그인의 podspec 경로를 찾지 못합니다.

**해결**:
```bash
# CocoaPods 저장소 업데이트
pod repo update

# 캐시 초기화 후 재설치
pod cache clean --all
cd ios && pod install
```

---

#### [오류] `pod install` 후 `NapSspPlugin` pod가 Podfile.lock에 없음

**원인**: `use_native_modules!`가 플러그인을 자동으로 연결하지 못했습니다.

**해결**:
```bash
# node_modules를 재설치 후 pod install 재실행
rm -rf node_modules
npm install
cd ios && pod install
```

---

#### [오류] Xcode 빌드 에러: `Multiple commands produce ...` 또는 `Duplicate symbols`

**원인**: `.xcodeproj`로 빌드하고 있습니다.

**해결**: 반드시 `.xcworkspace`로 Xcode를 열거나 `run-ios` 명령어를 사용하세요.
```bash
# 직접 Xcode를 열 경우
open ios/NapSspTestApp.xcworkspace
```

---

#### [오류] iOS 시뮬레이터에서 광고가 로드되지 않음

**원인**: 일부 미디에이션 어댑터는 시뮬레이터를 지원하지 않습니다. 또한 DEBUG 빌드에서는 플레이스홀더가 표시됩니다.

**해결**:
- 실기기에서 테스트합니다.
- `onAdFailedToLoad` 콜백의 에러 메시지를 확인하여 원인을 파악합니다.
- `Info.plist`에 `NSUserTrackingUsageDescription` 키가 있는지 확인합니다.
  ```xml
  <key>NSUserTrackingUsageDescription</key>
  <string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
  ```

---

#### [오류] ATT(AppTrackingTransparency) 팝업이 나타나지 않음

**원인**: `Info.plist`에 `NSUserTrackingUsageDescription`이 없습니다.

**해결**: `ios/NapSspTestApp/Info.plist`에 아래 항목 추가
```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

---

### 공통

---

#### [오류] `Cannot find module 'react-native-nap-ssp'`

**원인**: 패키지가 정상적으로 설치되지 않았습니다.

**해결**:
```bash
# 설치 상태 확인
ls node_modules/react-native-nap-ssp

# 재설치
npm install react-native-nap-ssp@0.1.7

# Metro 캐시 초기화
npx react-native start --reset-cache
```

---

#### [오류] 광고 키(mediaKey / adUnitId)가 잘못되었을 때

**증상**: `onAdFailedToLoad`가 호출되거나 로그에 인증 에러가 표시됩니다.

**해결**: 나스미디어 운영팀(nap_adx@nasmedia.co.kr)에 발급받은 키를 정확히 입력했는지 확인합니다. 키에 공백이나 오탈자가 없는지 점검합니다.

---

## 최종 검증 체크리스트

모든 단계를 완료하면 아래 항목을 최종 확인합니다.

- [ ] `npm install react-native-nap-ssp@0.1.7` 성공
- [ ] `node_modules/react-native-nap-ssp/`에 `android/`, `ios/`, `lib/` 폴더 존재
- [ ] Android: `BUILD SUCCESSFUL` 및 앱 기동 확인
- [ ] Android: Metro 로그에서 `[NapSsp] SDK initialized successfully` 확인
- [ ] Android: 배너 광고 `onAdLoaded` 콜백 호출 확인
- [ ] iOS: `pod install` 후 `NapSspPlugin (0.1.7)` Podfile.lock 기록 확인
- [ ] iOS: `BUILD SUCCEEDED` 및 앱 기동 확인
- [ ] iOS: Metro/Xcode 로그에서 `[NapSsp] SDK initialized successfully` 확인
- [ ] iOS: 배너 광고 `onAdLoaded` 콜백 호출 확인

---

## 참고 문서

| 문서 | 경로 |
| :--- | :--- |
| 시작 가이드 | `docs/GETTING_STARTED.md` |
| Android 상세 설정 | `docs/ANDROID_SETUP.md` |
| iOS 상세 설정 | `docs/IOS_SETUP.md` |
| API 레퍼런스 | `docs/API_REFERENCE.md` |
| 미디에이션 가이드 | `docs/MEDIATION_GUIDE.md` |
| 문제 해결 | `docs/TROUBLESHOOTING.md` |
| FAQ | `docs/FAQ.md` |
| 고급 사용법 | `docs/ADVANCED_USAGE.md` |

**기술 문의**: nap_adx@nasmedia.co.kr
**플러그인 버전**: 0.1.7
