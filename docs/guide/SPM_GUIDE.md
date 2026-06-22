# SPM (Swift Package Manager) 통합 가이드 (SPM Integration Guide)

## 현재 상태 (Current Status)

> KO: SPM 지원이 **정식으로 사용 가능한 상태**입니다.
> EN: SPM support is **officially available**.

- KO: `ios/Package.swift`에 AdMixerMediation **2.3.7** XCFramework 공식 URL과 sha256 checksum이 반영되어 있습니다.
  EN: `ios/Package.swift` references the official AdMixerMediation **2.3.7** XCFramework URL and its sha256 checksum.
- KO: CocoaPods와 SPM 두 가지 방식 모두 지원합니다.
  EN: Both CocoaPods and SPM are supported.
- KO: 미디에이션 어댑터(GAM, AdFit, Pangle, AppLovin, UnityAds, Naver AdManager)는 공식 SPM 저장소로도 제공됩니다.
  EN: Mediation adapters (GAM, AdFit, Pangle, AppLovin, UnityAds, Naver AdManager) are also available as official SPM repositories.

---

## 현재 버전 정보 (Bundled Version Info)

| 항목 (Item) | 값 (Value) |
| :--- | :--- |
| **AdMixerMediation 버전 (version)** | `2.3.7` |
| **URL** | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip` |
| **SHA256 Checksum** | `8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f` |

> KO: 2.3.5+ 버전은 공식 SPM 릴리스 채널(`iOS-SSP-Mediation-SPM` releases)로만 배포됩니다. 레거시 `iOS-AdMixerDownload` 바이너리 채널은 2.3.4까지만 제공됩니다.
> EN: Versions 2.3.5+ are distributed only via the official SPM release channel (`iOS-SSP-Mediation-SPM` releases). The legacy `iOS-AdMixerDownload` binary channel only goes up to 2.3.4.

> KO: CocoaPods로 설치하는 경우 버전을 고정하지 않으면 범위 내 최신 코어를 자동으로 가져옵니다.
> EN: When installing via CocoaPods, the latest core within range is pulled automatically unless you pin a version.

---

## 사용 방법 (Usage)

### 옵션 A: npm 패키지 내 로컬 패키지 참조 (Option A: Local package reference)

> KO: React Native 프로젝트에 `react-native-nap-ssp`를 npm으로 설치한 후:
> EN: After installing `react-native-nap-ssp` via npm in your React Native project:

1. Xcode → `File → Add Packages...` → `Add Local Package`
2. `node_modules/react-native-nap-ssp/ios` 폴더 선택 (select folder)
3. `NapSspPlugin` 타겟이 앱 타겟에 자동 링크됩니다. (`NapSspPlugin` target links automatically to your app target.)

### 옵션 B: 공식 SPM 저장소 직접 추가 (Option B: Add official SPM repositories directly)

> KO: Xcode → `File → Add Package Dependencies`에서 아래 저장소를 추가합니다. Dependency Rule은 `Up to Next Major Version`(권장)으로 설정하세요.
> EN: In Xcode → `File → Add Package Dependencies`, add the repositories below. Set the Dependency Rule to `Up to Next Major Version` (recommended).

| 패키지 (Package) | Repository URL |
| :--- | :--- |
| 미디에이션 (Mediation, 필수/required) | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM.git` |
| 코어 (Core, 필수/required) | `https://github.com/Nasmedia-Tech/iOS-SSP-SPM.git` |
| Google AdManager (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-GAM-SPM.git` |
| Kakao AdFit (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-AdFit-SPM.git` |
| Pangle (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-Pangle-SPM.git` |
| Unity Ads (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-UnityAds-SPM.git` |
| AppLovin (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-AppLovin-SPM.git` |
| Naver AdManager (선택/optional) | `https://github.com/Nasmedia-Tech/iOS-SSP-NAM-SPM` |

---

## checksum 재계산 방법 (Recomputing the Checksum — on SDK update)

> KO: 새 버전의 XCFramework ZIP을 다운로드한 후 아래 명령으로 checksum을 구합니다:
> EN: After downloading the new XCFramework ZIP, compute the checksum with:

```bash
swift package compute-checksum AdMixerMediation2.x.x.xcframework.zip
```

> KO: 구한 값을 `ios/Package.swift` 상단의 두 상수에 반영합니다:
> EN: Apply the result to the two constants at the top of `ios/Package.swift`:

```swift
let adMixerMediationURL = "https://...새_URL.../AdMixerMediation2.x.x.xcframework.zip"
let adMixerMediationChecksum = "새로운_sha256_체크섬"
```

---

## 미디에이션 어댑터 (Mediation Adapters)

> KO: 미디에이션 어댑터는 공식 SPM 저장소(위 표) 또는 CocoaPods로 추가할 수 있습니다. iOS에는 Teads 어댑터가 없습니다.
> EN: Mediation adapters can be added via the official SPM repositories (table above) or via CocoaPods. There is no Teads adapter on iOS.

> **문의 (Contact)**: nap_mx@nasmedia.co.kr
