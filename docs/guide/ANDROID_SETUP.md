# 🤖 Android 설정 가이드

![Android](https://img.shields.io/badge/Android-minSdk%2021-brightgreen)
![SDK](https://img.shields.io/badge/admixer--ssp-1.0.23-blue)
![JDK](https://img.shields.io/badge/JDK-17-orange)

`react-native-nap-ssp` 플러그인의 Android 설정 상세 가이드입니다.

---

## 목차

1. [Maven 리포지토리 설정](#1-maven-리포지토리-설정)
2. [의존성 추가](#2-의존성-추가)
3. [AndroidManifest.xml 설정](#3-androidmanifestxml-설정)
4. [ProGuard / R8 설정](#4-proguard--r8-설정)
5. [SDK 초기화 및 어댑터 등록](#5-sdk-초기화-및-어댑터-등록)
6. [의존성 중복 처리](#6-의존성-중복-처리)

---

## 1. Maven 리포지토리 설정

`android/build.gradle` (프로젝트 루트 레벨)에 추가합니다.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' } // Kakao AdFit
        maven { url "https://artifact.bytedance.com/repository/pangle/" }      // Pangle
    }
}
```

---

## 2. 의존성 추가

`android/app/build.gradle` (앱 레벨)의 `dependencies` 블록에 추가합니다.

### 필수 의존성

```gradle
dependencies {
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.23'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.9.0'
}
```

### 미디에이션 어댑터 (선택 — 사용할 네트워크만 추가)

```gradle
dependencies {
    implementation 'io.github.nasmedia-tech:admixer-admanager:1.0.15_delta'  // Google AdManager
    implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.12_beta'        // Kakao AdFit
    implementation 'io.github.nasmedia-tech:admixer-pangle:1.0.12_beta'       // Pangle
    implementation 'com.pangle.global:pag-sdk:7.7.0.2'                        // Pangle 사용 시 필수
    implementation 'io.github.nasmedia-tech:admixer-applovin:1.0.10_beta'     // AppLovin
    implementation 'io.github.nasmedia-tech:admixer-unity:1.0.7_beta'         // Unity Ads
    implementation 'io.github.nasmedia-tech:admixer-mobwith:1.0.0'            // Mobwith (선택)
}
```

### 빌드 환경 요구사항

| 항목 | 최솟값 |
| :--- | :--- |
| minSdk | 21 (Android 5.0) |
| compileSdk / targetSdk | 34 이상 권장 |
| JDK | 17 |

> ⚠️ JDK 버전이 17이 아닌 경우 `Unsupported class file major version` 오류가 발생합니다.

---

## 3. AndroidManifest.xml 설정

`android/app/src/main/AndroidManifest.xml`에 추가합니다.

### 필수 권한

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### Google AdManager 사용 시 (Application 태그 내부)

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="[nap ssp로부터 발급받은 Google App ID]" />
```

> 💡 Google App ID는 나스미디어 담당자(nap_adx@nasmedia.co.kr)에게 문의하세요.

---

## 4. ProGuard / R8 설정

릴리즈 빌드 시 클래스가 난독화되어 광고가 표시되지 않는 문제를 방지합니다.  
`android/app/proguard-rules.pro`에 추가하세요.

```proguard
# Nap SSP Core
-keep class com.nasmedia.admixerssp.** { *; }

# Mediation Adapters
-keep class com.nasmedia.admanager.**  { *; }
-keep class com.nasmedia.adfit.**      { *; }

# Pangle (실제 SDK 패키지)
-keep class com.pangle.**              { *; }
-dontwarn com.pangle.**

# AppLovin (실제 SDK 패키지)
-keep class com.applovin.**            { *; }
-dontwarn com.applovin.**

# Google Ads
-keep class com.google.android.gms.ads.** { *; }

# RN Plugin Bridge
-keep class com.gwangy.NapSsp** { *; }
```

> ⚠️ 이 설정 없이 릴리즈 빌드를 배포하면 광고가 노출되지 않습니다.

---

## 5. SDK 초기화 및 어댑터 등록

`react-native-nap-ssp`를 사용할 때는 JS 레이어의 `NapSspAd.initialize()`가 네이티브 초기화를 처리합니다.  
단, **미디에이션 어댑터는 네이티브에서 등록**해야 합니다.

`android/app/src/main/java/com/yourapp/MainApplication.kt` (또는 `.java`)에 추가하세요.

```kotlin
import com.nasmedia.admixerssp.common.AdMixer
import com.nasmedia.admixerssp.common.AdMixerLog

class MainApplication : Application(), ReactApplication {
    override fun onCreate() {
        super.onCreate()

        // 개발 중 로그 확인 (운영 환경에서는 제거 또는 NONE으로 변경)
        AdMixerLog.setLogLevel(AdMixerLog.LogLevel.VERBOSE)

        // 사용할 미디에이션 어댑터 등록 (사용하는 네트워크만 포함)
        AdMixer.registerAdapter(AdMixer.ADAPTER_ADMANAGER)  // Google AdManager
        AdMixer.registerAdapter(AdMixer.ADAPTER_ADFIT)      // Kakao AdFit
        AdMixer.registerAdapter(AdMixer.ADAPTER_PANGLE)     // Pangle
        AdMixer.registerAdapter(AdMixer.ADAPTER_APPLOVIN)   // AppLovin
        AdMixer.registerAdapter(AdMixer.ADAPTER_UNITY)      // Unity Ads
        AdMixer.registerAdapter(AdMixer.ADAPTER_MOBWITH)    // Mobwith (선택)
    }
}
```

> 💡 `NapSspAd.initialize()`는 앱 실행 시 `AdMixer.getInstance().initialize()`를 내부적으로 호출합니다.  
> `registerAdapter()`는 그 이전에 호출되어야 합니다.

---

## 6. 의존성 중복 처리

다른 광고 SDK와 함께 사용 중이어서 라이브러리 버전 충돌이 발생하는 경우, `exclude`로 해결합니다.

```gradle
dependencies {
    implementation("io.github.nasmedia-tech:admixer-admanager:1.0.14") {
        exclude group: "com.google.android.gms", module: "play-services-ads"
    }
    implementation("io.github.nasmedia-tech:admixer-adfit:1.0.10") {
        exclude group: "com.kakao.adfit", module: "ads-base"
    }
}
```

---

## 문의

**nap_adx@nasmedia.co.kr**
