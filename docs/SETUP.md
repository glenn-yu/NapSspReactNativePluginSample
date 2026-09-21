# Setup

Install and configure `react-native-nap-ssp` on both platforms.

Everything here mirrors the official nap mx guides —
[Android](https://napmx.github.io/#/android/native/getting-started) ·
[iOS](https://napmx.github.io/#/ios/native/getting-started). Where this plugin adds a requirement of
its own it is called out explicitly.

---

## 1. Prerequisites

Sign up on the [partner site](https://publisher.admixer.co.kr), register your media and create ad
units. You will get a numeric **media key** (one per app) and a numeric **ad unit ID** per format.

Some networks need a key issued by the nap mx team — contact
[nap_mx@nasmedia.co.kr](mailto:nap_mx@nasmedia.co.kr) for Google App ID, Pangle App ID and
Unity Ads App ID.

---

## 2. Install the package

```bash
npm install react-native-nap-ssp
# or
yarn add react-native-nap-ssp
```

Autolinking picks the plugin up on both platforms. No manual registration is needed.

| Requirement | Version |
| :--- | :--- |
| React Native | 0.72 or newer |
| React | 18.2 or newer |
| Android `minSdk` | 21 (higher for some adapters — see below) |
| Android Kotlin | **2.1 or newer** (see [Kotlin 요구사항](#kotlin-요구사항)) |
| iOS deployment target | 14.0 |
| Xcode | 16+ (26+ when using the Teads adapter) |

---

## 3. Android

### 3-1. Choose mediation adapters

The nap mx core SDK is always linked. Adapters are opt-in, because each one pulls in a third-party
network SDK (and sometimes an extra Maven repository).

```properties
# android/gradle.properties

# Comma separated. Supported: admanager, adfit, pangle, applovin, unity, naveradmanager, teads
napSsp.mediations=admanager,adfit

# Or link every adapter:
# napSsp.mediations=all
```

Leaving the property unset links **the core SDK only** — your waterfall will then serve AdMixer and
house ads but no third-party demand.

> **Upgrading from 0.4.x:** `napSsp.enableVendorSdk` was removed. If your build previously set it to
> `true` without `napSsp.mediations`, every adapter was linked; set `napSsp.mediations=all` to keep
> that behaviour, or list only the networks you actually sell.

### 3-2. Declare the adapter Maven repositories — required

AdFit, Pangle and Teads pull network SDKs that are not on Maven Central. **These repositories have
to be declared in your app**, not in the plugin: Gradle resolves a library's transitive dependencies
using the consuming project's repositories.

```groovy
// android/settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }   // AdFit
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }         // Pangle
        maven { url 'https://sdk.teads.tv/android/repo' }                         // Teads
        maven { url 'https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod' }  // Teads
        maven { url 'https://developer.huawei.com/repo/' }                        // Teads: Huawei devices
    }
}
```

Skip this and the build fails with `Could not find tv.teads.sdk.android:sdk` (or the Kakao/Pangle
equivalent). The Gradle build prints the exact list for the adapters you enabled.

Google Ad Manager, AppLovin, Unity Ads and Naver Ad Manager resolve from `google()` and
`mavenCentral()` alone.

### 3-3. Kotlin 요구사항

**The host app must build with Kotlin 2.1 or newer.**

nap mx core `admixer-ssp:2.3.0` ships `kotlin-stdlib:2.2.10`, whose metadata version is 2.2.0. A
Kotlin 2.0 or older compiler cannot read it and the build fails with:

```
Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.2.0, but the compiler version 2.0.21 can read versions up to 2.1.0.
```

| React Native | Default Kotlin | Action |
| :--- | :--- | :--- |
| 0.79 and newer | 2.1.x | nothing to do |
| 0.72 – 0.78 | 1.8 – 2.0 | pin Kotlin 2.1+ as below |

```groovy
// android/build.gradle (top level)
buildscript {
    ext {
        kotlinVersion = "2.1.21"
        // ...existing ext values
    }
    dependencies {
        classpath("com.android.tools.build:gradle")
        classpath("com.facebook.react:react-native-gradle-plugin")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
```

The plugin's own Kotlin version can be overridden with `napSsp.kotlinVersion` if you need to.

The plugin compiles Java and Kotlin at **17** by default, matching React Native's JDK requirement.
If your app compiles its modules at a different level, align the plugin with
`napSsp.javaVersion=11` — a mismatch fails the build with
`Inconsistent JVM-target compatibility detected`.

### 3-4. minSdk per network

The core SDK targets API 21, but some adapters require more. Your app's `minSdkVersion` must be at
least the highest value among the adapters you enable.

| Network | Minimum Android API |
| :--- | :--- |
| AdMixer core, Kakao AdFit, Pangle, Unity Ads, Teads | 21 (Android 5.0) |
| Google Ad Manager, Naver Ad Manager | 23 (Android 6.0) |
| AppLovin | 24 (Android 7.0) |

### 3-5. Manifest

Add the Google App ID when you enable the `admanager` adapter:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="YOUR_GOOGLE_APP_ID" />
```

Naver Ad Manager needs nothing — its `PUBLISHER_CD` ships inside the adapter AAR. Do **not** declare
it yourself.

If your app declares its own `android:networkSecurityConfig`, add `tools:replace` so yours wins over
the one the core SDK merges in:

```xml
<application
    android:networkSecurityConfig="@xml/your_network_security_config"
    tools:replace="android:networkSecurityConfig">
```

### 3-6. Google Ad Manager version ceiling

`play-services-ads` must stay at **25.2.0 or below** — 25.3.0+ is incompatible. If another
dependency pulls a newer version:

```groovy
configurations.all {
    resolutionStrategy {
        force 'com.google.android.gms:play-services-ads:25.2.0'
    }
}
```

### 3-7. ProGuard / R8

Nothing to do. The core SDK, each adapter and this plugin all ship `consumer-rules.pro`, which
Gradle merges into your release build automatically.

### 3-8. Customising the native ad layout

`NativeAd` renders the creative into `res/layout/nap_ssp_native_ad.xml`. To restyle it, ship a layout
with the same file name and the same view IDs (`nap_mx_iv_icon`, `nap_mx_tv_title`,
`nap_mx_tv_adv`, `nap_mx_tv_desc`, `nap_mx_iv_main`, `nap_mx_btn_cta`) in your app module — it wins
over the plugin's copy at merge time.

---

## 4. iOS

### 4-1. CocoaPods

Autolinking adds the base pod. Add a subspec per adapter you use:

```ruby
# ios/Podfile
target 'YourApp' do
  use_frameworks!

  pod 'NapSspPlugin/GAM'        # Google Ad Manager
  pod 'NapSspPlugin/AdFit'      # Kakao AdFit
  pod 'NapSspPlugin/Pangle'
  pod 'NapSspPlugin/AppLovin'
  pod 'NapSspPlugin/UnityAds'
  pod 'NapSspPlugin/NAM'        # Naver Ad Manager
  pod 'NapSspPlugin/Teads'      # requires Xcode 26+
end
```

```bash
cd ios && pod install --repo-update
```

### 4-2. Swift Package Manager

`ios/Package.swift` pins the `AdMixerMediation` xcframework (2.5.0) with its checksum. Add the
adapter packages you need from
[the official list](https://napmx.github.io/#/ios/native/getting-started).

### 4-3. App Tracking Transparency

Add a purpose string to `Info.plist`:

```xml
<key>NSUserTrackingUsageDescription</key>
<string>Used to deliver more relevant ads.</string>
```

The SDK does not prompt — your app does, and it should resolve before the first ad request:

```tsx
if (Platform.OS === 'ios') {
  const status = await NapSspAd.requestTrackingAuthorization();
  // 'authorized' | 'denied' | 'restricted' | 'notDetermined' | 'unavailable'
}
```

### 4-4. Google Ad Manager

```xml
<key>GADApplicationIdentifier</key>
<string>YOUR_GAD_APP_ID</string>
```

The plugin calls `MobileAds.shared.start()` during `initialize()` only when this key is present.

### 4-5. Customising the native ad layout

`NativeAd` looks for an `AMMNativeAdView.xib` in your app bundle first and falls back to a
programmatic layout. Ship your own xib whose File's Owner is `AMMNativeAdView` and whose outlets
(`iv_icon`, `l_headline`, `l_advertiser`, `l_description`, `media`, `b_cta`) are connected.

---

## 5. Initialize

```tsx
await NapSspAd.initialize({
  mediaKey: '10771',
  adUnitIds: ['104701', '104588', '104591', '104703', '103722'],
  logLevel: __DEV__ ? 'verbose' : 'error',
  privacy: {childDirected: false},
  testMode: __DEV__,
  mediations: {
    // Only needed when the media-conf server does not deliver the key itself.
    pangle: {appId: 'YOUR_PANGLE_APP_ID'},
    appLovin: {sdkKey: 'YOUR_APPLOVIN_SDK_KEY'},
    unityAds: {appId: 'YOUR_UNITY_APP_ID'}, // iOS
    naverAdManager: {publisherCd: 'YOUR_PUBLISHER_CD'}, // iOS
  },
});
```

`initialize()` must resolve before any ad is requested. It rejects on an invalid config — the
message names the offending field.

> **Order matters for privacy.** AppLovin, Unity Ads and Pangle read consent when they start up, so
> pass `privacy` to `initialize()` rather than calling `setPrivacyConsent()` afterwards. See
> [the official mapping table](https://napmx.github.io/#/android/native/privacy).

---

## 6. Verify the integration

```tsx
const status = await NapSspAd.getStatus();
console.log(status.initialized, status.platform, status.privacy, status.testMode);
```

Android logs are tagged `AdMixerSDK`:

```bash
adb logcat | grep AdMixerSDK
```

Set `logLevel: 'verbose'` during development and `'error'` in production.

If a specific network never serves, look for `[SKIP] configuration is invalid (Missing Keys).` or
`[SKIP] Adapter instantiation failed for:` in the log — the first means a required key is missing,
the second means the adapter module is not in the build (check `napSsp.mediations`).
