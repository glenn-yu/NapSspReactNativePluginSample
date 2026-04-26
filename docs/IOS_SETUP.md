# iOS Setup Guide (Detailed)

iOS 프로젝트에서 `react-native-nap-ssp` 플러그인을 안정적으로 사용하기 위한 상세 설정 가이드입니다.

---

## 1. CocoaPods 설정 (Podfile)

### 1.1 기본 의존성
```ruby
# Podfile 상단
platform :ios, '13.0'

target 'YourAppName' do
  # (필수) Nap SSP SDK 본체
  pod 'AdMixerMediation'
  
  # (선택) 미디에이션 어댑터
  pod 'AdMixerMediationGAM'
  pod 'AdMixerMediationAdFit'
end
```

### 1.2 use_frameworks! 이슈
React Native 프로젝트에서 `use_frameworks! :linkage => :static`을 사용하는 경우, 일부 SDK 링킹 오류가 발생할 수 있습니다. 오류 발생 시 해당 라인을 확인하십시오.

---

## 2. Xcode 프로젝트 설정

### 2.1 Info.plist 권한
광고 추적(ATT)을 위해 아래 키가 포함되어야 합니다.
- `NSUserTrackingUsageDescription`: "사용자 맞춤형 광고 제공을 위해 권한이 필요합니다." (용도에 맞게 수정)

### 2.2 SKAdNetworkIdentifier 등록
미디에이션 광고 수익 극대화를 위해 각 광고 네트워크의 ID를 `Info.plist`에 등록해야 합니다. 상세 목록은 [Mediation Setup Guide](./MEDIATION_GUIDE.md)를 참조하십시오.

---

## 3. Swift 환경 (Bridging Header)

본 플러그인은 Swift로 작성되었습니다. React Native 프로젝트가 기본적으로 Swift를 지원하지 않는 경우(오래된 프로젝트), Xcode에서 빈 Swift 파일을 하나 생성하면 자동으로 **Bridging Header**를 생성할지 묻는 팝업이 뜹니다. 이때 "Create Bridging Header"를 선택하십시오.

---

## 4. 시뮬레이터 제약 사항

일부 벤더 SDK는 **Intel/M1 시뮬레이터**에서 광고 로드를 제한하거나 특정 아키텍처(arm64) 빌드 이슈를 일으킬 수 있습니다. 가급적 **실기기(Physical Device)**를 통해 최종 검증하십시오.
