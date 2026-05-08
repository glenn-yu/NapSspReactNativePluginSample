# 🍎 iOS 설정 가이드

![iOS](https://img.shields.io/badge/iOS-14.0%2B-lightgrey)
![Xcode](https://img.shields.io/badge/Xcode-15.3%2B-blue)
![Swift](https://img.shields.io/badge/Swift-5.9%2B-orange)

`react-native-nap-ssp` 플러그인의 iOS 설정 상세 가이드입니다.

---

## 목차

1. [SDK 설치 — CocoaPods](#1-sdk-설치--cocoapods)
2. [SDK 설치 — Swift Package Manager](#2-sdk-설치--swift-package-manager-spm)
3. [Xcode 프로젝트 설정](#3-xcode-프로젝트-설정)
4. [Info.plist 설정](#4-infoplist-설정)
5. [Swift Bridging Header](#5-swift-bridging-header-구형-프로젝트)
6. [디버그 로그](#6-디버그-로그)

---

## 1. SDK 설치 — CocoaPods

`ios/Podfile`에 다음을 추가하고 `pod install`을 실행합니다.

```ruby
platform :ios, '14.0'

target 'YourAppName' do
  use_frameworks!

  # 필수 — Nap SSP 코어 SDK
  pod 'AdMixerMediation'

  # 선택 — 사용할 미디에이션 어댑터만 추가
  pod 'AdMixerMediationGAM'       # Google AdManager
  pod 'AdMixerMediationAdFit'     # Kakao AdFit
  pod 'AdMixerMediationPangle'    # Pangle
  pod 'AdMixerMediationAppLovin'  # AppLovin
  pod 'AdMixerMediationUnityAds'  # Unity Ads
end
```

```bash
cd ios && pod install
```

> ⚠️ 반드시 `.xcodeproj`가 아닌 **`.xcworkspace`** 파일로 Xcode를 열어 빌드하세요.

> 💡 `use_frameworks! :linkage => :static`을 사용하는 경우 일부 SDK 링킹 오류가 발생할 수 있습니다. 오류 발생 시 해당 라인을 제거하거나 동적 링킹(`use_frameworks!`)으로 변경하세요.

---

## 2. SDK 설치 — Swift Package Manager (SPM)

CocoaPods 대신 SPM을 사용할 수 있습니다.

### 옵션 A — npm 패키지 내 로컬 패키지 참조 (권장)

`react-native-nap-ssp`를 npm으로 설치한 후:

1. Xcode → **File → Add Packages... → Add Local Package**
2. `node_modules/react-native-nap-ssp/ios` 폴더 선택
3. `NapSspPlugin` 타겟이 앱 타겟에 자동으로 링크됩니다.

### 옵션 B — 공식 SPM 저장소 직접 추가

Xcode → **File → Add Package Dependencies**에서 아래 URL을 추가합니다.

| 패키지 | URL |
| :--- | :--- |
| 코어 SDK | `https://github.com/Nasmedia-Tech/iOS-SSP-SPM.git` |
| 미디에이션 | `https://github.com/Nasmedia-Tech/iOS-SSP-Mediation-SPM.git` |

> 💡 미디에이션 어댑터(GAM, AdFit, Pangle 등)의 SPM 지원 여부는 나스미디어 담당자에게 확인하세요.

### 현재 플러그인 내장 버전 (Package.swift)

| 항목 | 값 |
| :--- | :--- |
| AdMixerMediation 버전 | `2.3.3` |
| SHA256 Checksum | `b40eb8ae2eff354e56de68ad11de0030002d17ba66a48b2df2bad461c1a6049f` |

---

## 3. Xcode 프로젝트 설정

### SKAdNetworkIdentifier 등록

미디에이션 광고 수익을 극대화하려면 각 광고 네트워크의 SKAdNetwork ID를 `Info.plist`에 등록해야 합니다.  
상세 목록은 [Mediation Guide](./MEDIATION_GUIDE.md)를 참조하세요.

```xml
<key>SKAdNetworkItems</key>
<array>
  <dict>
    <key>SKAdNetworkIdentifier</key>
    <string>238da6jt44.skadnetwork</string>
  </dict>
  <!-- 각 네트워크 ID 추가 -->
</array>
```

---

## 4. Info.plist 설정

### ATT 추적 권한 (필수)

```xml
<key>NSUserTrackingUsageDescription</key>
<string>사용자 맞춤형 광고 제공을 위해 추적 권한이 필요합니다.</string>
```

> ⚠️ 이 항목이 없으면 ATT 팝업이 표시되지 않고 앱 심사에서 반려될 수 있습니다.

### Google AdManager 사용 시

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy</string>
```

### App Transport Security (필요 시)

```xml
<key>NSAppTransportSecurity</key>
<dict>
  <key>NSAllowsArbitraryLoads</key>
  <true/>
</dict>
```

---

## 5. Swift Bridging Header (구형 프로젝트)

본 플러그인은 Swift로 작성되어 있습니다. Swift를 사용하지 않는 오래된 React Native 프로젝트의 경우:

1. Xcode에서 빈 Swift 파일(`Empty.swift`)을 하나 생성합니다.
2. "Create Bridging Header" 팝업이 뜨면 **Create Bridging Header**를 선택합니다.

---

## 6. 디버그 로그

개발 중 광고 로드 흐름을 확인하려면 `NapSspAd.initialize()` 의 `logLevel` 옵션을 사용하세요.  
`setDebugEnabled()`는 초기화 내부에서 자동으로 호출되므로 직접 호출할 필요가 없습니다.

```tsx
// JS 초기화 시 logLevel 설정
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: [...],
  logLevel: 'debug',  // 'verbose' | 'debug' | 'info' | 'warn' | 'error' | 'none'
});
```

런타임 중 로그 레벨을 변경하려면:

```tsx
NapSspAd.setLogLevel('debug');
```

> 💡 운영 환경 배포 전 `logLevel: 'none'` 또는 `'warn'`으로 변경하세요.

---

## 시뮬레이터 제약사항

일부 미디에이션 SDK(Pangle 등)는 시뮬레이터에서 광고 노출이 제한됩니다.  
최종 광고 동작 검증은 반드시 **실기기(Physical Device)**에서 진행하세요.

---

## 문의

**nap_adx@nasmedia.co.kr**
