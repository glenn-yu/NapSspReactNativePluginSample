# Version Matrix

이 문서는 `react-native-nap-ssp` 플러그인 버전과 대응하는 Android/iOS 네이티브 SDK 버전 정보를 관리합니다.

---

## 1. SDK 버전 호환성

| Plugin 버전 | Android Core SDK | iOS Core SDK | 주요 변경 사항 |
| :--- | :--- | :--- | :--- |
| **0.1.6** | `1.0.23` | `2.3.3` | 버전 통일, 공개 가이드 문서 npm 배포 포함 |
| **0.1.3** | `1.0.23` | `2.3.3` | iOS SPM 2.3.3 업데이트, 스레드 안전성 수정, 이벤트 소급 구독 |
| **0.1.2** | `1.0.23` | `2.3.2` | 전면형 API 통일, Maestro 검증 강화 |
| **0.1.1** | `1.0.21` | `2.2.0` | 네이티브 전 포맷 브릿지 연결 |
| **0.1.0** | `1.0.18` | `2.1.0` | 초기 스캐폴딩 및 배너/전면 지원 |

---

## 2. 미디에이션 어댑터 버전 (0.1.6 기준)

### Android (Maven Central: `io.github.nasmedia-tech`)

| 어댑터 | artifact ID | 버전 |
| :--- | :--- | :--- |
| **Core** | `admixer-ssp` | `1.0.23` |
| **Google AdManager** | `admixer-admanager` | `1.0.15_delta` |
| **Kakao AdFit** | `admixer-adfit` | `1.0.12_beta` |
| **Pangle** | `admixer-pangle` | `1.0.12_beta` |
| **AppLovin** | `admixer-applovin` | `1.0.10_beta` |
| **UnityAds** | `admixer-unity` | `1.0.7_beta` |
| **Ads Identifier** | `play-services-ads-identifier` | `18.3.0` |

> Android 미디에이션 어댑터는 추가 리포지토리가 필요합니다:
> - `https://devrepo.kakao.com/nexus/content/groups/public/` (AdFit)
> - `https://artifact.bytedance.com/repository/pangle/` (Pangle)

### iOS (CocoaPods / SPM)

| 어댑터 | Pod 이름 | 버전 |
| :--- | :--- | :--- |
| **Core** | `AdMixerMediation` | `2.3.3` |
| **Google AdManager** | `AdMixerMediationGAM` | `1.0.8` |
| **Kakao AdFit** | `AdMixerMediationAdFit` | `1.0.7` |
| **Pangle** | `AdMixerMediationPangle` | `1.0.6` |
| **AppLovin** | `AdMixerMediationAppLovin` | `1.0.5` |
| **UnityAds** | `AdMixerMediationUnityAds` | `1.0.6` |

### iOS SPM Binary Target

| 항목 | 값 |
| :--- | :--- |
| **버전** | `2.3.3` |
| **URL** | `https://github.com/Nasmedia-Tech/iOS-AdMixerDownload/raw/main/AdMixerMediation2.3.3.xcframework.zip` |
| **SHA256 Checksum** | `b40eb8ae2eff354e56de68ad11de0030002d17ba66a48b2df2bad461c1a6049f` |

---

## 3. 업데이트 가이드

플러그인 버전을 올릴 때에는 `build.gradle` 및 `Podfile`에서 해당 네이티브 SDK 버전도 함께 업데이트해야 할 수 있습니다. 상세 내역은 [CHANGELOG.md](../../CHANGELOG.md)를 참조하십시오.
