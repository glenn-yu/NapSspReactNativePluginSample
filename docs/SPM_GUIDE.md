# SPM (Swift Package Manager) 통합 가이드

## 현재 상태

SPM 지원이 **정식으로 사용 가능한 상태**입니다.

- `ios/Package.swift`에 AdMixerMediation **2.3.3** XCFramework 공식 URL과 sha256 checksum이 반영되어 있습니다.
- CocoaPods와 SPM 두 가지 방식 모두 지원합니다.
- 미디에이션 어댑터(GAM, AdFit, Pangle 등)는 현재 CocoaPods로만 제공됩니다.

---

## 현재 버전 정보

| 항목 | 값 |
| :--- | :--- |
| **AdMixerMediation 버전** | `2.3.3` |
| **URL** | `https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.3.xcframework.zip` |
| **SHA256 Checksum** | `b40eb8ae2eff354e56de68ad11de0030002d17ba66a48b2df2bad461c1a6049f` |

---

## 사용 방법

### 옵션 A: npm 패키지 내 로컬 패키지 참조

React Native 프로젝트에 `react-native-nap-ssp`를 npm으로 설치한 후:

1. Xcode → `File → Add Packages...` → `Add Local Package`
2. `node_modules/react-native-nap-ssp/ios` 폴더 선택
3. `NapSspPlugin` 타겟이 앱 타겟에 자동 링크됩니다.

### 옵션 B: 직접 Package.swift 참조 (향후 원격 배포 시)

```swift
dependencies: [
    .package(url: "https://github.com/glenn-yu/react-native-nap-ssp", from: "0.1.3"),
],
```

---

## checksum 재계산 방법 (SDK 업데이트 시)

새 버전의 XCFramework ZIP을 다운로드한 후 아래 명령으로 checksum을 구합니다:

```bash
swift package compute-checksum AdMixerMediation2.x.x.xcframework.zip
```

구한 값을 `ios/Package.swift` 상단의 두 상수에 반영합니다:

```swift
let adMixerMediationURL = "https://...새_URL.../AdMixerMediation2.x.x.xcframework.zip"
let adMixerMediationChecksum = "새로운_sha256_체크섬"
```

---

## 미디에이션 어댑터

SPM 방식에서 미디에이션 어댑터(GAM, AdFit, Pangle 등)는 현재 CocoaPods로만 제공됩니다.  
각 어댑터의 SPM 지원 여부는 나스미디어 SDK 운영팀에 확인하세요.

> **문의**: nap_adx@nasmedia.co.kr
