# 미디에이션 가이드 (Mediation Guide)

> KO: `react-native-nap-ssp`(0.2.0+)는 다양한 타사 광고 네트워크(미디에이션) 연동을 지원합니다. 네이티브 SDK는 Android **AdMixer SSP v2.0.0** / iOS **AdMixerMediation 2.3.7** 입니다. 각 네트워크별 필수 네이티브 설정을 확인하세요.
> EN: `react-native-nap-ssp` (0.2.0+) supports several third-party ad-network (mediation) integrations. The native SDKs are Android **AdMixer SSP v2.0.0** / iOS **AdMixerMediation 2.3.7**. Review the required native setup for each network.

---

## 1. 공통 사항 (Overview)

> KO: 미디에이션을 사용하려면 초기화 시 `mediations` 옵션으로 사용할 어댑터를 활성화합니다. **v2.0.0부터 네이티브에서 `registerAdapter()`를 수동 호출할 필요가 없습니다** — Gradle/Pod 의존성에 어댑터가 포함되어 있으면 초기화 시 자동 등록됩니다.
> EN: To use mediation, enable adapters via the `mediations` option at init. **As of v2.0.0 you no longer call `registerAdapter()` natively** — adapters present in your Gradle/Pod dependencies are auto-registered at init.

```tsx
NapSspAd.initialize({
  mediaKey: '...',
  adUnitIds: ['...'],
  mediations: {
    adManager: { googleAppId: 'ca-app-pub-...' },
    adFit: true,
    pangle: { appId: 'PANGLE_APP_ID' },
    appLovin: { sdkKey: 'APPLOVIN_SDK_KEY' },
    unityAds: { appId: 'UNITY_GAME_ID' },
    naverAdManager: true, // v2.0.0+ (신규 / new)
    teads: true,          // v2.0.0+ (Android 전용 / Android only)
  },
});
```

> ℹ️ KO: `naverAdManager`와 `teads`는 v2.0.0에서 추가된 어댑터입니다. **Teads는 Android 전용이며 iOS에는 제공되지 않습니다.**
> ℹ️ EN: `naverAdManager` and `teads` are new in v2.0.0. **Teads is Android-only and is not available on iOS.**

---

## 2. 지원 어댑터 (Supported Adapters)

| 어댑터 (Adapter) | Android | iOS | `mediations` 키 (key) |
| :--- | :---: | :---: | :--- |
| Google AdManager | ✅ | ✅ | `adManager` |
| Kakao AdFit | ✅ | ✅ | `adFit` |
| Pangle | ✅ | ✅ | `pangle` |
| AppLovin | ✅ | ✅ | `appLovin` |
| Unity Ads | ✅ | ✅ | `unityAds` |
| Naver Ad Manager | ✅ | ✅ | `naverAdManager` |
| Teads | ✅ | ❌ | `teads` |

---

## 3. 네트워크별 상세 설정 (Per-Network Native Setup)

### 🤖 Android (AndroidManifest.xml / build.gradle)

> KO: 어댑터 의존성·Maven 저장소·ProGuard 규칙의 전체 목록은 [ANDROID_SETUP.md](./ANDROID_SETUP.md)를 참고하세요. 아래는 매니페스트 등 네트워크별 추가 설정만 정리합니다.
> EN: For the full list of adapter dependencies, Maven repositories, and ProGuard rules, see [ANDROID_SETUP.md](./ANDROID_SETUP.md). Below are only the per-network manifest extras.

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

#### Teads (신규 / new — Android only)
> KO: `settings.gradle`(또는 root `build.gradle`)에 Teads Maven 저장소 2개를 추가해야 합니다. 매니페스트 추가 설정은 없습니다.
> EN: Add the two Teads Maven repositories to `settings.gradle` (or root `build.gradle`). No extra manifest entries are required.

```gradle
maven { url 'https://sdk.teads.tv/android/repo' }
maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }
```

#### Naver Ad Manager (신규 / new)
> KO: 별도 설정이 필요 없습니다. `com.naver.gfpsdk.PUBLISHER_CD`는 SDK가 제공하므로 **매니페스트에 추가하지 마세요.**
> EN: No extra setup. `com.naver.gfpsdk.PUBLISHER_CD` is provided by the SDK, so **do not add it to the manifest.**

---

### 🍎 iOS (Info.plist)

> KO: iOS Pod/SPM 좌표 및 어댑터 추가 방법은 [IOS_SETUP.md](./IOS_SETUP.md)를 참고하세요. **iOS에는 Teads 어댑터가 없습니다.** NaverAdManager(NAM)는 v2.3.7에서 추가되었습니다.
> EN: For iOS Pod/SPM coordinates and adapter setup, see [IOS_SETUP.md](./IOS_SETUP.md). **There is no Teads adapter on iOS.** NaverAdManager (NAM) was added in v2.3.7.

#### Google AdManager / AdMob
```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy</string>
```

#### Pangle / AppLovin / Unity (SKAdNetwork)
> KO: 각 네트워크의 `SKAdNetworkIdentifier`를 `Info.plist`에 추가해야 합니다. 최신 ID 목록은 각 네트워크 공식 가이드를 참고하세요.
> EN: Add each network's `SKAdNetworkIdentifier` to `Info.plist`. Refer to each network's official guide for the latest IDs.

```xml
<key>SKAdNetworkItems</key>
<array>
  <dict>
    <key>SKAdNetworkIdentifier</key>
    <string>238da6jt44.skadnetwork</string> <!-- Pangle 예시 / example -->
  </dict>
  <!-- ... 기타 ID 추가 / add other IDs -->
</array>
```

---

## 4. 어댑터별 지원 배너 사이즈 (Adapter-Specific Banner Sizes)

> KO: 일부 어댑터는 표준 사이즈 외에 전용 배너 사이즈를 지원합니다.
> EN: Some adapters support dedicated banner sizes beyond the standard ones.

| 어댑터 (Adapter) | 전용 배너 사이즈 (Custom size) | `size` prop |
| :--- | :--- | :--- |
| NaverAdManager (AdManager) | 360 × 230 | `'BANNER_360x230'` |
| AdFit (Kakao) | 360 × 210 | `'BANNER_360x210'` |

```tsx
// NaverAdManager 전용 배너 / NaverAdManager custom banner
<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size="BANNER_360x230"
  style={{ width: 360, height: 230 }}
/>

// AdFit(Kakao) 전용 배너 / AdFit custom banner
<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size="BANNER_360x210"
  style={{ width: 360, height: 210 }}
/>
```

> 💡 KO: Android는 `style`의 `width`/`height`로 뷰 크기를 제어하고, iOS는 `size` prop 값으로 SDK 프레임이 자동 설정됩니다. 해당 어댑터가 등록되지 않으면 광고가 노출되지 않습니다.
> 💡 EN: On Android the view size is controlled by `style` `width`/`height`; on iOS the SDK frame is set from the `size` prop. Ads won't show if the matching adapter isn't registered.

### BANNER_WxH 동적 사이즈 (Dynamic size, v0.1.8+)

> KO: `size` prop은 `'BANNER_너비x높이'` 형식 문자열을 그대로 받아 동적으로 처리합니다. 서버에서 사이즈를 내려주는 구조라면 플러그인 업데이트 없이 바로 사용 가능합니다.
> EN: The `size` prop accepts any `'BANNER_WIDTHxHEIGHT'` string and handles it dynamically. If your server returns sizes, you can use them directly without a plugin update.

```tsx
const bannerSize = serverResponse.size; // e.g. "BANNER_360x230"
const [width, height] = bannerSize.replace('BANNER_', '').split('x').map(Number);

<BannerAd
  adUnitId="YOUR_AD_UNIT_ID"
  size={bannerSize}
  style={{ width, height }}
/>
```

---

## 5. v2.0.0 미디에이션 변경 요약 (What Changed in v2.0.0)

> KO: 미디에이션 관점에서의 핵심 변경입니다. 대부분 네이티브 내부 동작이며 JS 사용자 코드는 영향이 거의 없습니다.
> EN: Key changes from a mediation standpoint. Most are native-internal; JS user code is largely unaffected.

- **어댑터 자동 등록 (Auto adapter registration):** KO: `registerAdapter()` 수동 호출 제거 — 의존성에 포함된 어댑터를 `initialize()`가 자동 등록. / EN: No manual `registerAdapter()` — `initialize()` auto-registers adapters found in dependencies.
- **신규 어댑터 (New adapters):** KO: NaverAdManager(Android·iOS), Teads(Android 전용). / EN: NaverAdManager (Android & iOS), Teads (Android only).
- **AdManager 버전 상한 (Version cap):** KO: `play-services-ads` ≤ 25.2.0 강제 권장. / EN: pin `play-services-ads` ≤ 25.2.0.
- **전면 Basic 전용 (Interstitial Basic-only):** KO: popup/countDown 타입 제거 — JS의 전면 popup/countdown 옵션도 제거됨. / EN: popup/countDown types removed — the JS interstitial popup/countdown options are gone too.

> ℹ️ KO: 상세 마이그레이션 절차는 [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)를 참고하세요.
> ℹ️ EN: For the full migration steps, see [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md).

---

## 6. 의존성 추가 가이드 (Adding Dependencies)

> KO: 플랫폼별 `build.gradle`·`Podfile`/SPM 어댑터 추가 방법은 [ANDROID_SETUP.md](./ANDROID_SETUP.md)·[IOS_SETUP.md](./IOS_SETUP.md) 및 [README.md](../../README.md)의 "네이티브 필수 설정" 섹션을 참고하세요.
> EN: For per-platform adapter setup in `build.gradle`, `Podfile`/SPM, see [ANDROID_SETUP.md](./ANDROID_SETUP.md), [IOS_SETUP.md](./IOS_SETUP.md), and the "Native setup" section of [README.md](../../README.md).

---

## 문의 (Contact)

> KO: nap_mx@nasmedia.co.kr — 공식 가이드: https://napmx.github.io/
> EN: nap_mx@nasmedia.co.kr — Official guide: https://napmx.github.io/
