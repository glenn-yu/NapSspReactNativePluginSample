# 개인정보 보호 & 규정 준수 가이드 (Privacy & Compliance Guide)

> KO: 이 문서는 관련 법규(GDPR, CCPA/US Privacy, COPPA 등)를 준수하기 위해 `react-native-nap-ssp` 플러그인(Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7) 에서 제공하는 개인정보 보호 설정 방법을 안내합니다.
> EN: This document explains the privacy settings provided by the `react-native-nap-ssp` plugin (Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7) for complying with regulations such as GDPR, CCPA/US Privacy, and COPPA.

---

## 1. 아동 개인정보 보호 (Child Privacy / COPPA)

> KO: 미국 아동 온라인 개인정보 보호법(COPPA)을 준수해야 하는 경우, 초기화 시 `coppa` 옵션을 설정하십시오. 이 옵션은 내부적으로 AdMixer v2의 `setTagForChildDirectedTreatment`로 매핑되어, 아동 대상 처리(child-directed treatment) 플래그를 전 어댑터에 전파합니다.
> EN: If you must comply with the US Children's Online Privacy Protection Act (COPPA), set the `coppa` option at initialization. Internally this maps to AdMixer v2's `setTagForChildDirectedTreatment`, propagating the child-directed treatment flag to all adapters.

```tsx
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: ['...'],
  coppa: true, // 아동 대상 앱인 경우 true / set true for child-directed apps
});
```

---

## 2. v2 전역 개인정보 API (v2 Global Privacy APIs — Android)

> KO: Android v2.0.0의 AdMixer는 아래 전역 동의/개인정보 API를 제공합니다. 이들은 네이티브 레벨에서 `initialize()` 전/직후에 적용되며, 미디에이션 어댑터 전반에 전달됩니다. (iOS는 네이티브 API 명세가 다르므로, ATT 흐름과 SDK 내부 처리에 따릅니다.)
> EN: AdMixer on Android v2.0.0 exposes the global consent/privacy APIs below. They are applied at the native layer around `initialize()` and propagated across mediation adapters. (iOS uses a different native API surface and relies on the ATT flow plus the SDK's internal handling.)

| AdMixer 네이티브 API (Android) | 목적 (Purpose) |
| :--- | :--- |
| `AdMixer.setGdprConsent(...)` | GDPR 동의 상태 설정 / Set GDPR consent state |
| `AdMixer.setUsPrivacy(...)` | US Privacy(IAB) 문자열 설정 / Set US Privacy (IAB) string |
| `AdMixer.setCcpaDoNotSell(...)` | CCPA "Do Not Sell" 설정 / Set CCPA Do-Not-Sell |
| `AdMixer.setTagForChildDirectedTreatment(...)` | COPPA 아동 대상 처리 플래그 / COPPA child-directed flag |
| `AdMixer.setTestMode(...)` | 테스트 모드 활성화 / Enable test mode |
| `AdMixer.setTestDeviceIds(...)` | 테스트 디바이스 ID 등록 / Register test device IDs |

> KO: 플러그인 옵션 매핑 — 현재 JS 표면에서는 `coppa` 옵션이 `setTagForChildDirectedTreatment`로 직접 매핑됩니다. GDPR/CCPA/US Privacy/테스트 모드가 필요하면 위 네이티브 API를 앱의 네이티브 모듈(또는 추후 제공될 JS 브리지)에서 호출하십시오.
> EN: Plugin option mapping — on the current JS surface, the `coppa` option maps directly to `setTagForChildDirectedTreatment`. For GDPR/CCPA/US Privacy/test mode, call the native APIs above from your app's native module (or a future JS bridge).

> KO: 참고(내부 동작) — v2부터 `registerAdapter()` 호출은 불필요하며, `initialize()`가 Gradle 의존성의 어댑터를 자동 등록합니다.
> EN: Note (internal) — from v2 `registerAdapter()` is no longer needed; `initialize()` auto-registers the adapters present in your Gradle dependencies.

---

## 3. 사용자 추적 권한 (iOS ATT)

> KO: iOS 14.5 이상에서는 사용자의 광고 식별자(IDFA)에 접근하기 위해 앱 추적 투명성(ATT) 권한이 필요합니다. (최소 배포 타깃 iOS 14.0)
> EN: On iOS 14.5+, App Tracking Transparency (ATT) authorization is required to access the advertising identifier (IDFA). (Minimum deployment target iOS 14.0.)

### 3.1 Info.plist 설정 (Info.plist Setup)

> KO: `NSUserTrackingUsageDescription` 키를 추가하고 사용자에게 보여줄 메시지를 입력하십시오.
> EN: Add the `NSUserTrackingUsageDescription` key with a message to show the user.

### 3.2 권한 요청 실행 (Request Authorization)

```tsx
import { NapSspAd } from 'react-native-nap-ssp';

const status = await NapSspAd.requestTrackingAuthorization();
console.log('ATT Status:', status);
```

---

## 4. 광고 식별자 권한 (Android Advertising ID / API 33+)

> KO: 안드로이드 13(API 33) 이상을 타겟팅하는 경우, 광고 ID에 접근하기 위해 `AndroidManifest.xml`에 아래 권한을 추가해야 합니다. v2는 `com.google.android.gms:play-services-ads-identifier:18.2.0`를 사용합니다.
> EN: When targeting Android 13 (API 33)+, add the permission below to `AndroidManifest.xml` to access the advertising ID. v2 uses `com.google.android.gms:play-services-ads-identifier:18.2.0`.

```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
```

---

## 5. 네트워크별 규정 설정 (Per-Network Compliance)

> KO: 특정 광고 네트워크별 추가 규제 파라미터가 필요한 경우 [Mediation Setup Guide](./MEDIATION_GUIDE.md)를 참조하십시오. 추가 문의는 nap_mx@nasmedia.co.kr 또는 공식 가이드 https://napmx.github.io/ 를 확인하세요.
> EN: For per-network compliance parameters, see the [Mediation Setup Guide](./MEDIATION_GUIDE.md). For further questions contact nap_mx@nasmedia.co.kr or see the official guide at https://napmx.github.io/.
