# SPM (Swift Package Manager) 통합 가이드

## 현재 상태

SPM 지원 코드는 포함되어 있지만, **아직 바로 배포 가능한 완성 상태는 아닙니다.**

이유:
- `ios/Package.swift`의 `adMixerMediationChecksum` 값이 아직 placeholder입니다.
- checksum이 공식 배포값으로 확정되기 전까지는 SPM 연동을 참고용 또는 사내용 검증용으로만 취급하는 것이 안전합니다.
- 미디에이션 어댑터는 현재 CocoaPods 기준 안내가 더 현실적입니다.

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
