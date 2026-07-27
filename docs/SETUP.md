# 🚀 Setup & Installation Guide

Welcome to **`react-native-nap-ssp`**, the official React Native bridge for KT Nasmedia's NapSSP Native & Hybrid Ad SDK. This guide walks you through setting up Android, iOS, CocoaPods/SPM, and Expo environments.

---

## 📋 Table of Contents
1. [Prerequisites](#1-prerequisites)
2. [Package Installation](#2-package-installation)
3. [Android Configuration](#3-android-configuration)
4. [iOS Configuration (CocoaPods & SPM)](#4-ios-configuration-cocoapods--spm)
5. [Expo Integration](#5-expo-integration)
6. [Verification & Testing](#6-verification--testing)

---

## 1. Prerequisites
* **React Native**: `0.60.0` or higher (Supports New Architecture & TurboModules)
* **Android**: API Level 24+ (Android 7.0+), Gradle 7.0+
* **iOS**: iOS 14.0+, Xcode 14+
* **TypeScript**: `4.5.0` or higher recommended

---

## 2. Package Installation

Install the package via npm or yarn:

```bash
npm install react-native-nap-ssp
# or
yarn add react-native-nap-ssp
```

---

## 3. Android Configuration

### ① Bill of Materials (BOM) & Repositories
The plugin automatically applies the latest Nasmedia AdMixer BOM (`io.github.nasmedia-tech:admixer-bom:2026.07.03`) and configures required external repositories (Teads, Kakao, Pangle).

Ensure your root `android/build.gradle` includes standard `mavenCentral()` and `google()` repositories:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // Plugin automatically appends required third-party ad network repositories
    }
}
```

### ② AndroidManifest.xml
Ensure the required permissions are declared in `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- Required for targeted advertising on Android 13+ (API 33+) -->
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    
    <application ...>
        ...
    </application>
</manifest>
```

### ③ ProGuard / R8 Rules
The plugin ships with pre-configured consumer rules (`consumer-rules.pro`), so no extra ProGuard configuration is needed for core SDK classes. If you are integrating mediation adapters manually in your host app, add:

```proguard
-keep class com.nasmedia.admixerssp.** { *; }
-keep interface com.nasmedia.admixerssp.** { *; }
-dontwarn com.nasmedia.admixerssp.**
```

---

## 4. iOS Configuration (CocoaPods & SPM)

### ① CocoaPods Setup
In your iOS folder, run `pod install`:

```bash
cd ios
pod install
```

#### Mediation Subspecs
If you use mediation ad networks (Google Ad Manager, Kakao AdFit, Pangle, AppLovin, UnityAds, Naver Ad Manager), declare the relevant subspecs in your `Podfile`:

```ruby
target 'YourApp' do
  # Include core SDK + specific mediation adapters
  pod 'NapSspPlugin', :path => '../node_modules/react-native-nap-ssp', :subspecs => [
    'GAM',
    'AdFit',
    'Pangle',
    'AppLovin',
    'UnityAds',
    'NAM' # Naver Ad Manager (added in v2.3.7 guidelines)
  ]
end
```

### ② Swift Package Manager (SPM)
If your project uses SPM instead of CocoaPods, link the built-in iOS static targets directly or include `AdMixerMediation` dependencies via Xcode -> Package Dependencies.

### ③ SKAdNetwork & App Tracking Transparency (ATT)
To maximize CPM and fill rate on iOS 14.5+, add `NSUserTrackingUsageDescription` and `SKAdNetworkItems` to your `Info.plist`:

```xml
<key>NSUserTrackingUsageDescription</key>
<string>This identifier will be used to deliver personalized ads to you.</string>
```

---

## 5. Expo Integration

### Bare Workflow
If you use Expo Bare Workflow, run:
```bash
npx expo install react-native-nap-ssp
npx pod-install ios
```

### Managed Workflow (Config Plugins)
The SDK includes native code that requires custom native binaries. Use `npx expo run:android` or `npx expo run:ios` (or EAS Build) to compile your native development client.

---

## 6. Verification & Testing

To test whether the native SDK bridge is properly linked and communicating:

```bash
# Run local smoke tests
npm run smoke:test
```

If you encounter any compilation or runtime issues during setup, check out [FAQ & Troubleshooting Guide](./FAQ.md).
