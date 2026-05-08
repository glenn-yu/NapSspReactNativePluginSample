# Mediation Setup Guide

`react-native-nap-ssp`는 다양한 타사 광고 네트워크(Mediation) 연동을 지원합니다. 각 네트워크별 필수 네이티브 설정을 확인하십시오.

---

## 1. 공통 사항

미디에이션을 사용하려면 초기화 시 `mediations` 옵션을 설정해야 합니다.

```tsx
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: ['...'],
  mediations: {
    adFit: true,
    pangle: { appId: 'PANGLE_APP_ID' },
    appLovin: { sdkKey: 'APPLOVIN_SDK_KEY' },
    // ...
  }
});
```

---

## 2. 네트워크별 상세 설정

### 🤖 Android (AndroidManifest.xml)

#### Google AdManager / AdMob
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
```

#### Pangle
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

---

### 🍎 iOS (Info.plist)

#### Google AdManager / AdMob
```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy</string>
```

#### Pangle / AppLovin / Unity (SKAdNetwork)
`docs/native_guides/` 내의 가이드라인을 참조하여 각 네트워크의 `SKAdNetworkIdentifier`를 `Info.plist`에 추가해야 합니다.

```xml
<key>SKAdNetworkItems</key>
<array>
  <dict>
    <key>SKAdNetworkIdentifier</key>
    <string>238da6jt44.skadnetwork</string> <!-- Pangle example -->
  </dict>
  <!-- ... 기타 ID 추가 -->
</array>
```

---

## 3. 어댑터별 지원 배너 사이즈

일부 어댑터는 표준 사이즈 외에 전용 배너 사이즈를 지원합니다.

| 어댑터 | 전용 배너 사이즈 | `size` prop 값 |
| :--- | :--- | :--- |
| NaverAdManager (AdManager) | 360 × 230 | `'BANNER_360x230'` |
| AdFit (Kakao) | 360 × 210 | `'BANNER_360x210'` |

```tsx
// NaverAdManager 전용 배너
<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size="BANNER_360x230"
  style={{ width: 360, height: 230 }}
/>

// AdFit(Kakao) 전용 배너
<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size="BANNER_360x210"
  style={{ width: 360, height: 210 }}
/>
```

> 💡 Android는 `style`의 `width`/`height`로 뷰 크기를 제어합니다.  
> iOS는 `size` prop 값으로 SDK 프레임이 자동 설정됩니다.  
> 해당 어댑터가 등록되지 않은 경우 광고가 노출되지 않습니다.

### BANNER_WxH 동적 사이즈 (v0.1.8+)

`size` prop은 `'BANNER_너비x높이'` 형식 문자열을 그대로 받아 동적으로 처리합니다.  
서버에서 사이즈를 내려주는 구조라면 플러그인 업데이트 없이 바로 사용 가능합니다.

```tsx
// 서버에서 받은 사이즈를 그대로 전달
const bannerSize = serverResponse.size; // e.g. "BANNER_360x230"
const [width, height] = bannerSize.replace('BANNER_', '').split('x').map(Number);

<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size={bannerSize}
  style={{ width, height }}
/>
```

---

## 4. 의존성 추가 가이드

각 플랫폼별 `build.gradle` 및 `Podfile`에 어댑터 라이브러리를 추가하는 방법은 [README.md](../../README.md)의 **"네이티브 필수 설정"** 섹션을 참조하십시오.
