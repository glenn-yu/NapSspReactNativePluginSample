# Troubleshooting

`react-native-nap-ssp` 사용 중 발생할 수 있는 주요 문제와 해결 방법입니다.

---

## 1. 설치 및 빌드 이슈

### `NapSspXXX is not linked` 에러
**원인**: 네이티브 모듈이 현재 앱 바이너리에 포함되어 있지 않습니다.
**해결**:
- **Android**: 안드로이드 스튜디오에서 `Sync Project with Gradle Files`를 실행하거나 `npx react-native run-android`를 다시 실행하세요.
- **iOS**: `cd ios && pod install` 명령을 실행하여 CocoaPods 의존성을 갱신하고 다시 빌드하세요.

### `Unsupported class file major version 69` (Android)
**원인**: JDK 버전(예: JDK 25)이 현재 Gradle 버전과 호환되지 않습니다.
**해결**: **JDK 17**을 사용하도록 프로젝트 설정을 변경하세요.

---

## 2. 광고 로드 실패 (Failed to Load)

### `mediaKey` 또는 `adUnitId` 오류
**원인**: 유효하지 않거나 등록되지 않은 키를 사용 중입니다.
**해결**: 나스미디어 파트너 사이트에서 발급받은 실제 미디어 키와 광고 단위 ID를 다시 확인하세요.

### 네트워크 연결 문제
**원인**: 광고 요청은 실제 서버와 통신해야 합니다.
**해결**: 기기 또는 에뮬레이터의 인터넷 연결 상태를 확인하세요. 비행기 모드나 오프라인 환경에서는 광고가 로드되지 않습니다.

### 실기기 vs 시뮬레이터
**원인**: 일부 미디에이션 SDK(Pangle 등)는 가상 환경(시뮬레이터)에서 광고 노출이 제한될 수 있습니다.
**해결**: 정확한 검증을 위해서는 반드시 **실제 기기(Physical Device)**에서 테스트할 것을 권장합니다.

---

## 3. iOS 특이사항

### ATT 권한 팝업 미노출
**원인**: `Info.plist`에 `NSUserTrackingUsageDescription` 항목이 누락되었거나 앱 추적 권한 설정이 비활성화되어 있습니다.
**해결**: `Info.plist` 설정을 다시 확인하고, 시뮬레이터의 경우 `설정 > 개인정보 보호 > 추적`에서 '앱이 추적을 요청하도록 허용'이 켜져 있는지 확인하세요.

---

## 4. Android 특이사항

### 릴리즈 빌드에서 광고 미노출
**원인**: ProGuard 또는 R8에 의해 SDK 클래스가 제거(Obfuscation)되었습니다.
**해결**: `android/app/proguard-rules.pro` 또는 패키지의 `consumer-rules.pro`에 필수 `-keep` 규칙이 포함되어 있는지 확인하세요.
```
-keep class com.nasmedia.admixerssp.** { *; }
-keep interface com.nasmedia.** { *; }
```
