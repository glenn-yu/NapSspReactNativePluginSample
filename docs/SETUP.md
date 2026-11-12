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
| React Native | 0.76 or newer (package peer dependency) |
| React | 18.2 or newer |
| Android `minSdk` | 21 (higher for some adapters — see below) |
| Android `compileSdk` | 35 or newer |
| Android Kotlin | Plugin defaults to 2.1.21; check host compatibility (see [Kotlin compatibility](#3-3-kotlin-compatibility)) |
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

### 3-3. Kotlin compatibility

The plugin's Gradle configuration defaults to Kotlin `2.1.21`; `napSsp.kotlinVersion` can
override that version. Host projects can use different Gradle and Kotlin configurations, so inspect
your actual host settings and resolved dependencies and build the complete app after upgrading. An
incompatible Kotlin metadata error may require updating the host Kotlin plugin to a compatible version.
For reference, the official React Native community templates specify Kotlin [2.0.21 for 0.79](https://raw.githubusercontent.com/react-native-community/template/0.79-stable/template/android/build.gradle) and [2.1.20 for 0.80](https://raw.githubusercontent.com/react-native-community/template/0.80-stable/template/android/build.gradle). Your project may differ.

The previously documented React Native template version ranges and a universal minimum Kotlin version
have not been verified for this SDK update; do not rely on them as a compatibility matrix.

The [official SDK guide](https://napmx.github.io/#/android/native/getting-started) reports testing
Kotlin 2.1.0 and 2.2.10 with a Flutter host (AGP 8.10.1, Java 17, debug and release with R8) and
recommends 2.2.10. This is not React Native validation. For a host using the Groovy `buildscript`
pattern, update its existing `kotlinVersion` if compatibility requires it, for example:

```groovy
// android/build.gradle (top level); retain the rest of your existing configuration
buildscript {
    ext {
        kotlinVersion = "2.2.10"
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
```

Adapt this to your project's Gradle plugin configuration rather than adding a duplicate declaration.
A previous 0.5.0 build reported `Class 'kotlin.Unit' was compiled with an incompatible version of
Kotlin` using Kotlin 2.0.21 against metadata 2.2.0. That report is specific to that configuration;
the 0.6.0 native build was not completed, so it does not establish a universal minimum for this release.

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

The base pod has no adapters by default. For adapters, declare the local plugin once before
`use_native_modules!` in your existing target so autolinking sees the same pod source. Choose only
the subspecs you use. CocoaPods AdFit uses the 1.1.x adapter; its 2.x releases are SPM-only and
require iOS 15 or later.

```ruby
# ios/Podfile, inside your existing target, before use_native_modules!
pod 'NapSspPlugin', :path => '../node_modules/react-native-nap-ssp',
    :subspecs => ['GAM', 'AdFit']
config = use_native_modules!
# Keep the rest of your React Native Podfile configuration.
```

Other adapter subspecs are `Pangle`, `AppLovin`, `UnityAds`, `NAM` and `Teads` (Teads requires
Xcode 26+). Adjust the path if your dependency is hoisted. This Podfile pattern has not been
validated with an iOS build for 0.6.0; verify `pod install` and the host build.

```bash
cd ios && pod install --repo-update
```

### 4-2. Swift Package Manager

The React Native plugin is installed through CocoaPods. Standalone Swift Package Manager
integration of this React Native bridge is not currently supported. The native SDK itself offers
SPM packages; see the [official guide](https://napmx.github.io/#/ios/native/getting-started).

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
