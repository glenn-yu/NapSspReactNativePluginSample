# 🤖 Android 설정 가이드 (Android Setup Guide)

![Android](https://img.shields.io/badge/Android-minSdk%2021-brightgreen)
![SDK](https://img.shields.io/badge/admixer--ssp-2.0.0-blue)
![JDK](https://img.shields.io/badge/JDK-17-orange)

> KO: `react-native-nap-ssp` 플러그인(0.2.0+)의 Android 네이티브 설정 상세 가이드입니다. 내장 네이티브 SDK는 **AdMixer SSP v2.0.0**입니다.
> EN: Detailed Android native setup guide for the `react-native-nap-ssp` plugin (0.2.0+). The bundled native SDK is **AdMixer SSP v2.0.0**.

---

## 목차 (Table of Contents)

1. [Maven 리포지토리 설정 (Maven Repositories)](#1-maven-리포지토리-설정-maven-repositories)
2. [의존성 추가 (Dependencies)](#2-의존성-추가-dependencies)
3. [네트워크 버전 범위 / 최소 API (Network Versions & Min API)](#3-네트워크-버전-범위--최소-api-network-versions--min-api)
4. [AndroidManifest.xml 설정 (Manifest)](#4-androidmanifestxml-설정-manifest)
5. [ProGuard / R8 설정 (ProGuard / R8)](#5-proguard--r8-설정-proguard--r8)
6. [SDK 초기화 — 어댑터 자동 등록 (Initialization — Auto Adapter Registration)](#6-sdk-초기화--어댑터-자동-등록-initialization--auto-adapter-registration)
7. [의존성 중복 처리 (Resolving Duplicate SDKs)](#7-의존성-중복-처리-resolving-duplicate-sdks)

---

## 1. Maven 리포지토리 설정 (Maven Repositories)

> KO: 프로젝트 루트 레벨 `android/build.gradle` 또는 `android/settings.gradle`의 저장소 블록에 추가합니다. 사용하는 네트워크에 따라 필요한 저장소만 추가하면 됩니다.
> EN: Add to the repositories block of your root-level `android/build.gradle` or `android/settings.gradle`. Only the repositories required by the networks you use are needed.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // Kakao AdFit 사용 시 / required for Kakao AdFit
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
        // Pangle 사용 시 / required for Pangle
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }
        // Teads 사용 시 (신규) / required for Teads (new in v2.0.0)
        maven { url 'https://sdk.teads.tv/android/repo' }
        maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }
    }
}
```

| 네트워크 (Network) | 필요 저장소 (Required repositories) |
| :--- | :--- |
| Google AdManager, AppLovin, Unity, NaverAdManager | `google()` / `mavenCentral()` 만으로 해결 (resolved by these alone) |
| Kakao AdFit | `devrepo.kakao.com` |
| Pangle | `artifact.bytedance.com` |
| Teads | `sdk.teads.tv`, `teads.jfrog.io` |

---

## 2. 의존성 추가 (Dependencies)

> KO: 앱 레벨 `android/app/build.gradle`의 `dependencies` 블록에 추가합니다. Maven 그룹은 `io.github.nasmedia-tech` 이며, Core와 모든 어댑터의 버전은 **2.0.0**입니다.
> EN: Add to the `dependencies` block of your app-level `android/app/build.gradle`. The Maven group is `io.github.nasmedia-tech`, and Core plus all adapters are version **2.0.0**.

### 필수 의존성 (Required)

```gradle
dependencies {
    // ✅ Core SDK
    implementation 'io.github.nasmedia-tech:admixer-ssp:2.0.0'
    // ✅ Google Advertising ID
    implementation 'com.google.android.gms:play-services-ads-identifier:18.2.0'
}
```

### 미디에이션 어댑터 (Mediation adapters — 사용할 네트워크만 추가 / add only what you use)

```gradle
dependencies {
    implementation 'io.github.nasmedia-tech:admixer-admanager:2.0.0'       // Google AdManager
    implementation 'io.github.nasmedia-tech:admixer-adfit:2.0.0'           // Kakao AdFit
    implementation 'io.github.nasmedia-tech:admixer-pangle:2.0.0'          // Pangle
    implementation 'io.github.nasmedia-tech:admixer-applovin:2.0.0'        // AppLovin
    implementation 'io.github.nasmedia-tech:admixer-unity:2.0.0'           // Unity Ads
    implementation 'io.github.nasmedia-tech:admixer-naveradmanager:2.0.0'  // Naver Ad Manager (신규 / new)
    implementation 'io.github.nasmedia-tech:admixer-teads:2.0.0'           // Teads (신규 / new)
}
```

> KO: 어댑터 aar에는 해당 네트워크 SDK가 전이 의존으로 포함됩니다. 별도 네트워크 SDK(예: `pag-sdk`, `applovin-sdk`)를 직접 선언할 필요가 없습니다.
> EN: Each adapter aar bundles its network SDK transitively. You do not need to declare the underlying network SDK (e.g. `pag-sdk`, `applovin-sdk`) yourself.

### ⚠️ Google AdManager `play-services-ads` 상한 (Version cap)

> KO: AdManager 어댑터가 끌어오는 `play-services-ads`는 **25.2.0 상한**을 지켜야 합니다. 25.3.0+는 비호환이므로, 다른 어댑터가 상위 버전을 끌어오지 못하도록 강제 고정을 권장합니다.
> EN: The `play-services-ads` pulled in by the AdManager adapter must stay at the **25.2.0 cap**. 25.3.0+ is incompatible, so force-pin it to prevent other adapters from upgrading it.

```gradle
configurations.all {
    resolutionStrategy {
        force 'com.google.android.gms:play-services-ads:25.2.0'
    }
}
```

### 빌드 환경 요구사항 (Build environment)

| 항목 (Item) | 최솟값 (Minimum) |
| :--- | :--- |
| minSdk | 21 (Android 5.0) — 단, 어댑터별 최소 API는 아래 3절 참고 / see §3 for per-adapter min API |
| compileSdk / targetSdk | 34 이상 권장 (34+ recommended) |
| JDK | 17 |

> ⚠️ KO: JDK 17이 아니면 `Unsupported class file major version` 오류가 발생합니다.
> ⚠️ EN: A JDK other than 17 causes `Unsupported class file major version` errors.

---

## 3. 네트워크 버전 범위 / 최소 API (Network Versions & Min API)

> KO: 각 어댑터(2.0.0)에 번들된 네트워크 SDK 버전과, 호스트 앱이 충족해야 하는 최소 Android API입니다.
> EN: Bundled network SDK versions per 2.0.0 adapter, and the minimum Android API the host app must satisfy.

| 네트워크 (Network) | 번들 SDK (Bundled) | 최소 Android API (Min API) |
| :--- | :--- | :--- |
| AdMixer (Core) | 2.0.0 | API 21 (Android 5.0) |
| Kakao AdFit | ads-base 3.21.17 | API 21 (Android 5.0) |
| Teads | teads-sdk 6.1.0 | API 21 (Android 5.0) |
| Google AdManager | play-services-ads 25.2.0 (상한 / cap) | API 23 (Android 6.0) |
| Pangle | pag-sdk 8.0.0.5 | API 23 (Android 6.0) |
| Unity Ads | unity-ads 4.18.1 | API 23 (Android 6.0) |
| Naver Ad Manager | nam-bom 8.16.0 | API 23 (Android 6.0) |
| AppLovin | applovin-sdk 13.6.3 | API 24 (Android 7.0) |

> KO: 추가한 어댑터 중 가장 높은 최소 API에 맞춰 호스트 앱 `minSdkVersion`을 올려야 합니다. (예: AppLovin 사용 시 24)
> EN: Raise your host app `minSdkVersion` to the highest min API among the adapters you add (e.g. 24 when using AppLovin).

---

## 4. AndroidManifest.xml 설정 (Manifest)

> KO: `android/app/src/main/AndroidManifest.xml`에 추가합니다.
> EN: Add to `android/app/src/main/AndroidManifest.xml`.

### 필수 권한 (Required permissions)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### Google AdManager 사용 시 (When using Google AdManager — `<application>` 내부)

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="[nap mx로부터 발급받은 Google App ID]" />
```

### Naver Ad Manager 사용 시 (When using Naver Ad Manager)

> KO: 별도 설정이 필요 없습니다. `com.naver.gfpsdk.PUBLISHER_CD`는 nap mx가 `admixer-naveradmanager` aar에서 제공·관리하므로 **호스트 앱 매니페스트에 설정하지 마세요.**
> EN: No manifest setup is needed. `com.naver.gfpsdk.PUBLISHER_CD` is provided and managed by nap mx inside the `admixer-naveradmanager` aar, so **do not set it in the host app manifest.**

> 💡 KO: Google App ID 발급 등 문의는 nap_mx@nasmedia.co.kr.
> 💡 EN: Contact nap_mx@nasmedia.co.kr to obtain a Google App ID.

---

## 5. ProGuard / R8 설정 (ProGuard / R8)

> KO: 릴리즈 빌드에서 난독화로 광고가 표시되지 않는 문제를 방지합니다. `android/app/proguard-rules.pro`에 추가하세요. 사용하는 어댑터 모듈만 남기면 됩니다.
> EN: Prevents obfuscation from breaking ad rendering in release builds. Add to `android/app/proguard-rules.pro`, keeping only the adapter modules you use.

```proguard
# ✅ 필수 — AdMixer Core / Required — AdMixer Core
-keep class com.nasmedia.admixerssp.** { *; }

# 사용하는 어댑터 모듈만 추가 / keep only the adapters you use
-keep class com.nasmedia.admanager.** { *; }        # Google AdManager
-keep class com.nasmedia.adfit.** { *; }             # Kakao AdFit
-keep class com.nasmedia.pangle.** { *; }            # Pangle
-keep class com.nasmedia.applovin.** { *; }          # AppLovin
-keep class com.nasmedia.unity.** { *; }             # Unity Ads
-keep class com.nasmedia.naveradmanager.** { *; }    # Naver Ad Manager (신규 / new)
-keep class com.nasmedia.teads.** { *; }             # Teads (신규 / new)
```

> ℹ️ KO: 각 어댑터 aar는 `consumer-rules.pro`를 포함하여 대부분의 규칙이 자동 적용됩니다. 위 규칙은 추가 안전망입니다.
> ℹ️ EN: Each adapter aar ships a `consumer-rules.pro`, so most rules apply automatically. The rules above are an extra safety net.

> ⚠️ KO: 이 설정 없이 릴리즈 빌드를 배포하면 광고가 노출되지 않을 수 있습니다.
> ⚠️ EN: Without these rules, ads may not show in release builds.

---

## 6. SDK 초기화 — 어댑터 자동 등록 (Initialization — Auto Adapter Registration)

> KO: `react-native-nap-ssp`에서는 JS 레이어의 `NapSspAd.initialize()`가 네이티브 초기화(`AdMixer.getInstance().initialize()`)를 처리합니다.
> EN: With `react-native-nap-ssp`, the JS-layer `NapSspAd.initialize()` performs native initialization (`AdMixer.getInstance().initialize()`).

> 🚨 KO: **v2.0.0부터 `registerAdapter()` 수동 호출이 더 이상 필요하지 않습니다.** `initialize()`가 Gradle 의존성(클래스패스)에 포함된 어댑터를 자동으로 탐지·등록합니다. 기존 `MainApplication`의 `AdMixer.registerAdapter(...)` 호출은 **모두 제거하세요.**
> 🚨 EN: **As of v2.0.0, manual `registerAdapter()` calls are no longer needed.** `initialize()` auto-detects and registers adapters present in your Gradle dependencies (classpath). **Remove all `AdMixer.registerAdapter(...)` calls** from your existing `MainApplication`.

```kotlin
// MainApplication.kt — v2.0.0
class MainApplication : Application(), ReactApplication {
    override fun onCreate() {
        super.onCreate()
        // 어댑터를 위한 별도 코드가 필요 없습니다.
        // No adapter code is required here.
        // build.gradle 에 추가된 어댑터는 NapSspAd.initialize() 시 자동 등록됩니다.
        // Adapters added to build.gradle are auto-registered when NapSspAd.initialize() runs.
    }
}
```

> KO: JS에서의 초기화 예시 (사용할 어댑터를 `mediations`로 활성화):
> EN: JS-side initialization (enable the adapters you use via `mediations`):

```tsx
NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['...'],
  mediations: {
    adManager: { googleAppId: 'ca-app-pub-...' },
    adFit: true,
    pangle: { appId: 'PANGLE_APP_ID' },
    appLovin: { sdkKey: 'APPLOVIN_SDK_KEY' },
    unityAds: { appId: 'UNITY_GAME_ID' },
    naverAdManager: true, // v2.0.0+
    teads: true,          // v2.0.0+
  },
  logLevel: 'verbose', // 운영 시 'error' 권장 / use 'error' in production
});
```

> ℹ️ KO: 네트워크 SDK(Pangle 등)는 워터폴에서 어댑터가 자동(lazy) 초기화하므로 `PAGSdk.init()` / `MobileAds.initialize()` 등 수동 호출이 필요 없습니다.
> ℹ️ EN: Network SDKs (e.g. Pangle) are lazily initialized by the adapters during the waterfall, so manual `PAGSdk.init()` / `MobileAds.initialize()` calls are unnecessary.
>
> ℹ️ KO: **v2.0.0 기준** Pangle `appId` · AppLovin `sdkKey` · Unity `appId` 등 네트워크 자격증명은 파트너 사이트(media-conf 서버) 설정으로 전달됩니다. 위 JS `mediations` 의 해당 값은 선택(상태 표시용)이며, 키 자체로 SDK 를 초기화하지 않습니다. `adFit`/`naverAdManager`/`teads` 처럼 사용 여부만 켜면 됩니다.
> ℹ️ EN: **As of v2.0.0**, network credentials (Pangle `appId`, AppLovin `sdkKey`, Unity `appId`, …) are delivered via partner-site (media-conf) settings. Those values in the JS `mediations` config are optional (status only) and do not themselves initialize the SDK — just toggle usage like `adFit`/`naverAdManager`/`teads`.

---

## 7. 의존성 중복 처리 (Resolving Duplicate SDKs)

> KO: 다른 광고 SDK와 함께 사용해 동일 네트워크 SDK가 중복되면 `exclude`로 해결합니다.
> EN: If another ad SDK in your app duplicates a network SDK, resolve it with `exclude`.

```gradle
dependencies {
    // 이미 Google AdManager SDK를 직접 사용 중 / already using Google AdManager directly
    implementation("io.github.nasmedia-tech:admixer-admanager:2.0.0") {
        exclude group: "com.google.android.gms", module: "play-services-ads"
    }
    // 이미 Kakao AdFit SDK를 직접 사용 중 / already using Kakao AdFit directly
    implementation("io.github.nasmedia-tech:admixer-adfit:2.0.0") {
        exclude group: "com.kakao.adfit", module: "ads-base"
    }
    // 이미 Pangle SDK를 직접 사용 중 / already using Pangle directly
    implementation("io.github.nasmedia-tech:admixer-pangle:2.0.0") {
        exclude group: "com.pangle.global", module: "pag-sdk"
    }
}
```

> ⚠️ KO: exclude 후 의존성 트리에 동일 네트워크 SDK가 1개만 남는지, 빌드와 광고 노출이 정상인지 확인하세요.
> ⚠️ EN: After excluding, verify only one copy of the network SDK remains in the dependency tree, and that the build and ad rendering still work.

---

## 문의 (Contact)

> KO: nap_mx@nasmedia.co.kr — 공식 가이드: https://napmx.github.io/
> EN: nap_mx@nasmedia.co.kr — Official guide: https://napmx.github.io/
