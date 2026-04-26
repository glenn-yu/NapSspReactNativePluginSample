# Android Setup Guide (Detailed)

안드로이드 프로젝트에서 `react-native-nap-ssp` 플러그인을 안정적으로 사용하기 위한 상세 설정 가이드입니다.

---

## 1. 리포지토리 설정 (Project-level build.gradle)

나스미디어 SDK 및 일부 미디에이션 SDK는 특정 Maven 리포지토리가 필요합니다.

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // (필수) 나스미디어 및 미디에이션 리포지토리
        maven { url "https://devrepo.kakao.com/nexus/content/groups/public/" }
        maven { url "https://artifact.bytedance.com/repository/pangle" }
    }
}
```

---

## 2. 앱 수준 설정 (App-level build.gradle)

### 2.1 의존성 추가
```gradle
dependencies {
    implementation 'io.github.nasmedia-tech:admixer-ssp:1.0.21'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    
    // 미디에이션 사용 시 해당 어댑터 추가
    implementation 'io.github.nasmedia-tech:admixer-adfit:1.0.10'
    implementation 'io.github.nasmedia-tech:admixer-pangle:1.0.10'
}
```

### 2.2 Java 17 및 SDK 버전
- **compileSdk / targetSdk**: 34 이상 권장.
- **minSdk**: 21 이상 필수.
- **Java**: JDK 17 환경에서 빌드하는 것을 권장합니다.

---

## 3. ProGuard / R8 설정

릴리즈 빌드 시 클래스가 제거되거나 이름이 변경되어 광고가 로드되지 않는 문제를 방지하기 위해 `proguard-rules.pro`에 아래 내용을 추가하십시오.

```proguard
# Nap SSP Core
-keep class com.nasmedia.admixerssp.** { *; }
-keep interface com.nasmedia.admixerssp.** { *; }

# Kakao AdFit
-keep class com.kakao.adfit.** { *; }

# Google AdManager
-keep class com.google.android.gms.ads.** { *; }

# Reflection을 사용하는 브릿지 보호
-keep class com.gwangy.NapSsp** { *; }
```

---

## 4. 권한 설정 (AndroidManifest.xml)

광고 식별자 접근 및 네트워크 통신을 위해 아래 권한들이 포함되어야 합니다.
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```
