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

## 3. 의존성 추가 가이드

각 플랫폼별 `build.gradle` 및 `Podfile`에 어댑터 라이브러리를 추가하는 방법은 [README.md](../../README.md)의 **"네이티브 필수 설정"** 섹션을 참조하십시오.
