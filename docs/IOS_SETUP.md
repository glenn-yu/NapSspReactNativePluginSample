# 🍎 iOS 설정 가이드 (iOS Setup Guide)

![iOS](https://img.shields.io/badge/iOS-14.0%2B-lightgrey)
![Xcode](https://img.shields.io/badge/Xcode-15.3%2B-blue)
![Swift](https://img.shields.io/badge/Swift-5.9%2B-orange)

> KO: `react-native-nap-ssp` 플러그인의 iOS 설정 상세 가이드입니다. iOS 코어는 **AdMixerMediation 2.3.7** 기준이며, 최소 **iOS 14.0**, **Xcode 15.3+** 가 필요합니다.
> EN: Detailed iOS setup guide for the `react-native-nap-ssp` plugin. iOS core is based on **AdMixerMediation 2.3.7**, requiring a minimum of **iOS 14.0** and **Xcode 15.3+**.

---

## 목차 (Table of Contents)

1. [SDK 설치 — CocoaPods (Install via CocoaPods)](#1-sdk-설치--cocoapods-install-via-cocoapods)
2. [SDK 설치 — Swift Package Manager (Install via SPM)](#2-sdk-설치--swift-package-manager-install-via-spm)
3. [Xcode 프로젝트 설정 (Xcode Project Setup)](#3-xcode-프로젝트-설정-xcode-project-setup)
4. [Info.plist 설정 (Info.plist Configuration)](#4-infoplist-설정-infoplist-configuration)
5. [Swift Bridging Header (구형 프로젝트) (Legacy Projects)](#5-swift-bridging-header-구형-프로젝트-legacy-projects)
6. [디버그 로그 (Debug Logging)](#6-디버그-로그-debug-logging)

---

## 1. SDK 설치 — CocoaPods (Install via CocoaPods)

> KO: `ios/Podfile`에 다음을 추가하고 `pod install`을 실행합니다. 사용할 미디에이션 어댑터만 골라서 추가하세요. **iOS에는 Teads 어댑터가 없습니다.**
> EN: Add the following to `ios/Podfile` and run `pod install`. Add only the mediation adapters you use. **There is no Teads adapter on iOS.**

```ruby
platform :ios, '14.0'

target 'YourAppName' do
  use_frameworks!

  # 필수 — Nap SSP 코어 SDK (Required — Nap SSP core SDK)
  pod 'AdMixerMediation'

  # 선택 — 사용할 미디에이션 어댑터만 추가 (Optional — add only the adapters you use)
  pod 'AdMixerMediationGAM'       # Google AdManager
  pod 'AdMixerMediationAdFit'     # Kakao AdFit
  pod 'AdMixerMediationPangle'    # Pangle
  pod 'AdMixerMediationAppLovin'  # AppLovin
  pod 'AdMixerMediationUnityAds'  # Unity Ads
  pod 'AdMixerMediationNAM'       # Naver AdManager (신규 / new)
end
```

```bash
cd ios && pod install
```

> KO: CocoaPods는 버전을 고정하지 않으면 범위 내 최신 코어/어댑터를 자동으로 가져옵니다(현재 코어 2.3.7).
> EN: When versions are not pinned, CocoaPods automatically pulls the latest core/adapters within range (current core 2.3.7).

> ⚠️ KO: 반드시 `.xcodeproj`가 아닌 **`.xcworkspace`** 파일로 Xcode를 열어 빌드하세요.
> ⚠️ EN: Always open and build with the **`.xcworkspace`** file, not the `.xcodeproj`.

> 💡 KO: `use_frameworks! :linkage => :static`을 사용하는 경우 일부 SDK 링킹 오류가 발생할 수 있습니다. 오류 발생 시 해당 라인을 제거하거나 동적 링킹(`use_frameworks!`)으로 변경하세요.
> 💡 EN: Using `use_frameworks! :linkage => :static` may cause linking errors with some SDKs. If errors occur, remove that line or switch to dynamic linking (`use_frameworks!`).

---

## 2. SDK 설치 — Swift Package Manager (Install via SPM)

> KO: CocoaPods 대신 SPM을 사용할 수 있습니다. 자세한 내용은 [SPM_GUIDE.md](./SPM_GUIDE.md)를 참고하세요.
> EN: You can use SPM instead of CocoaPods. See [SPM_GUIDE.md](./SPM_GUIDE.md) for full details.

### 옵션 A — npm 패키지 내 로컬 패키지 참조 (권장) (Option A — Local package reference, recommended)

> KO: `react-native-nap-ssp`를 npm으로 설치한 후:
> EN: After installing `react-native-nap-ssp` via npm:

1. Xcode → **File → Add Packages... → Add Local Package**
2. `node_modules/react-native-nap-ssp/ios` 폴더 선택 (select folder)
3. `NapSspPlugin` 타겟이 앱 타겟에 자동으로 링크됩니다. (`NapSspPlugin` target links automatically to your app target.)

### 옵션 B — 공식 SPM 저장소 직접 추가 (Option B — Add official SPM repositories directly)

> KO: Xcode → **File → Add Package Dependencies**에서 아래 URL을 추가합니다. 미디에이션 어댑터도 공식 SPM 저장소로 제공됩니다(Naver AdManager 포함).
> EN: In Xcode → **File → Add Package Dependencies**, add the URLs below. Mediation adapters are also available as official SPM repositories (including Naver AdManager).

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

### 현재 플러그인 내장 버전 (Bundled version in this plugin — Package.swift)

| 항목 (Item) | 값 (Value) |
| :--- | :--- |
| AdMixerMediation 버전 (version) | `2.3.7` |
| URL | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM/releases/download/2.3.7/AdMixerMediation2.3.7.xcframework.zip` |
| SHA256 Checksum | `8f3b00161ff57ad71f583a9f353814112f4c79f6224d3f42824e7df3a555791f` |

> KO: 2.3.5+ 버전은 공식 SPM 릴리스 채널로만 배포됩니다. (레거시 `iOS-AdMixerDownload` 바이너리 채널은 2.3.4까지만 제공.)
> EN: Versions 2.3.5+ are distributed only via the official SPM release channel. (The legacy `iOS-AdMixerDownload` binary channel only goes up to 2.3.4.)

---

## 3. Xcode 프로젝트 설정 (Xcode Project Setup)

### SKAdNetworkIdentifier 등록 (Register SKAdNetworkIdentifier)

> KO: 미디에이션 광고 수익을 극대화하려면 각 광고 네트워크의 SKAdNetwork ID를 `Info.plist`에 등록해야 합니다. 상세 목록은 [Mediation Guide](./MEDIATION_GUIDE.md)를 참조하세요.
> EN: To maximize mediation ad revenue, register each ad network's SKAdNetwork ID in `Info.plist`. See the [Mediation Guide](./MEDIATION_GUIDE.md) for the full list.

```xml
<key>SKAdNetworkItems</key>
<array>
  <dict>
    <key>SKAdNetworkIdentifier</key>
    <string>238da6jt44.skadnetwork</string>
  </dict>
  <!-- 각 네트워크 ID 추가 (add each network ID) -->
</array>
```

---

## 4. Info.plist 설정 (Info.plist Configuration)

### ATT 추적 권한 (필수) (ATT Tracking Permission — Required)

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

> ⚠️ KO: 이 항목이 없으면 ATT 팝업이 표시되지 않고 앱 심사에서 반려될 수 있습니다.
> ⚠️ EN: Without this key, the ATT prompt will not appear and the app may be rejected during review.

### Google AdManager 사용 시 (When using Google AdManager)

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy</string>
```

> KO: Google App ID 발급은 nap mx 운영팀(nap_mx@nasmedia.co.kr)으로 문의하세요.
> EN: To obtain a Google App ID, contact the nap mx team (nap_mx@nasmedia.co.kr).

### App Transport Security (필요 시) (if required)

```xml
<key>NSAppTransportSecurity</key>
<dict>
  <key>NSAllowsArbitraryLoads</key>
  <true/>
</dict>
```

---

## 5. Swift Bridging Header (구형 프로젝트) (Legacy Projects)

> KO: 본 플러그인은 Swift로 작성되어 있습니다. Swift를 사용하지 않는 오래된 React Native 프로젝트의 경우:
> EN: This plugin is written in Swift. For older React Native projects that do not use Swift:

1. Xcode에서 빈 Swift 파일(`Empty.swift`)을 하나 생성합니다. (Create an empty Swift file, e.g. `Empty.swift`.)
2. "Create Bridging Header" 팝업이 뜨면 **Create Bridging Header**를 선택합니다. (When prompted, choose **Create Bridging Header**.)

---

## 6. 디버그 로그 (Debug Logging)

> KO: 개발 중 광고 로드 흐름을 확인하려면 `NapSspAd.initialize()`의 `logLevel` 옵션을 사용하세요. `setDebugEnabled()`는 초기화 내부에서 자동으로 호출되므로 직접 호출할 필요가 없습니다.
> EN: To trace the ad load flow during development, use the `logLevel` option of `NapSspAd.initialize()`. `setDebugEnabled()` is called automatically inside initialization, so you do not need to call it directly.

```tsx
// JS 초기화 시 logLevel 설정 (set logLevel during JS initialization)
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: [...],
  logLevel: 'debug',  // 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none'
});
```

> KO: 런타임 중 로그 레벨을 변경하려면:
> EN: To change the log level at runtime:

```tsx
NapSspAd.setLogLevel('debug');
```

> 💡 KO: 운영 환경 배포 전 `logLevel: 'none'` 또는 `'warn'`으로 변경하세요.
> 💡 EN: Before releasing to production, change `logLevel` to `'none'` or `'warn'`.

---

## 시뮬레이터 제약사항 (Simulator Limitations)

> KO: 일부 미디에이션 SDK(Pangle 등)는 시뮬레이터에서 광고 노출이 제한됩니다. 최종 광고 동작 검증은 반드시 **실기기(Physical Device)**에서 진행하세요.
> EN: Some mediation SDKs (e.g. Pangle) have limited ad rendering on the simulator. Always validate final ad behavior on a **physical device**.

---

## 문의 (Contact)

**nap_mx@nasmedia.co.kr**
