# SPM (Swift Package Manager) 통합 가이드

## 개요

`ios/Package.swift`는 `AdMixerMediation.xcframework`를 `.binaryTarget`으로 선언합니다.  
공식 XCFramework ZIP URL과 checksum이 확정되면 `Package.swift` 상단의 두 상수를 업데이트하세요.

```swift
let adMixerMediationURL = "https://...공식_URL.../AdMixerMediation.xcframework.zip"
let adMixerMediationChecksum = "sha256_체크섬"
```

## 사용 방법

### 1) Xcode에서 로컬 패키지 추가

1. `File → Add Packages... → Add Local Package`
2. `node_modules/react-native-nap-ssp/ios` 폴더 선택
3. `NapSspPlugin` 타겟이 앱 타겟에 자동 링크됩니다.

### 2) checksum 확인 방법

공식 XCFramework ZIP을 다운로드 후 아래 명령으로 checksum을 구합니다:

```bash
swift package compute-checksum AdMixerMediation.xcframework.zip
```

### 3) 미디에이션 어댑터 (CocoaPods 전용)

SPM 방식에서 미디에이션 어댑터(GAM, AdFit, Pangle 등)는 현재 CocoaPods로만 제공됩니다.  
각 어댑터의 SPM 지원 여부는 나스미디어 SDK 운영팀에 확인하세요.

> **문의**: nap_adx@nasmedia.co.kr
