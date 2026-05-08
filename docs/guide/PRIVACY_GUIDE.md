# Privacy & Compliance Guide

이 문서는 관련 법규(GDPR, CCPA, COPPA 등)를 준수하기 위해 `react-native-nap-ssp` 플러그인에서 제공하는 개인정보 보호 설정 방법을 안내합니다.

---

## 1. 아동 개인정보 보호 (COPPA)

미국 아동 온라인 개인정보 보호법(COPPA)을 준수해야 하는 경우, 초기화 시 `coppa` 옵션을 설정할 수 있습니다.

```tsx
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: ['...'],
  coppa: true, // 아동 대상 앱인 경우 true 설정
});
```

---

## 2. 사용자 추적 권한 (iOS ATT)

iOS 14.5 이상에서는 사용자의 광고 식별자(IDFA)에 접근하기 위해 앱 추적 투명성(ATT) 권한이 필요합니다.

### 2.1 Info.plist 설정
`NSUserTrackingUsageDescription` 키를 추가하고 사용자에게 보여줄 메시지를 입력하십시오.

### 2.2 권한 요청 실행
```tsx
import { NapSspAd } from 'react-native-nap-ssp';

const status = await NapSspAd.requestTrackingAuthorization();
console.log('ATT Status:', status);
```

---

## 3. 광고 식별자 권한 (Android API 33+)

안드로이드 13(API 33) 이상을 타겟팅하는 경우, 광고 ID에 접근하기 위해 `AndroidManifest.xml`에 아래 권한을 추가해야 합니다.

```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
```

---

## 4. 기타 규정 (GDPR/CCPA)

나스미디어 Nap SSP SDK는 내부적으로 규정을 준수하며, 특정 국가의 규제 대응을 위한 추가 파라미터가 필요한 경우 [Mediation Setup Guide](./MEDIATION_GUIDE.md)를 참조하여 각 네트워크별 설정을 확인하십시오.
