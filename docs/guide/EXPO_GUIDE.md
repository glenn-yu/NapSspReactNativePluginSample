# Expo 호환 가이드 (Expo Compatibility Guide)

> KO: `react-native-nap-ssp` (0.2.0) 플러그인은 네이티브 코드(Kotlin/Swift)를 포함하므로 **Expo Go** 앱에서는 직접 실행할 수 없습니다. Expo 환경에서 사용하려면 **Development Builds**를 생성해야 합니다.
> EN: The `react-native-nap-ssp` (0.2.0) plugin contains native code (Kotlin/Swift), so it cannot run inside the **Expo Go** app. To use it under Expo, you must create a **Development Build**.

---

## 1. Expo Prebuild 사용 (Using Expo Prebuild)

> KO: Expo 프로젝트에서 이 플러그인을 사용하려면 아래 절차를 따르십시오.
> EN: To use this plugin in an Expo project, follow the steps below.

### 1.1 의존성 설치 (Install Dependency)

```bash
npx expo install react-native-nap-ssp
```

### 1.2 네이티브 설정 (Native Setup via Config Plugins)

> KO: 현재 본 플러그인은 자동 Config Plugin을 지원하지 않으므로, `app.json` 또는 `app.config.js`에서 필요한 권한·메타데이터를 직접 설정하거나 `expo-build-properties`를 활용해야 합니다. v2 기준으로 Android `minSdkVersion`은 21이며, 사용하는 어댑터 저장소를 `extraMavenRepos`에 추가하고 iOS 최소 배포 타깃은 **14.0**입니다.
> EN: This plugin does not yet ship an automatic Config Plugin, so set the required permissions/metadata directly in `app.json` / `app.config.js`, or use `expo-build-properties`. For v2, Android `minSdkVersion` is 21; add the Maven repos for the adapters you use via `extraMavenRepos`, and the iOS minimum deployment target is **14.0**.

**app.json 예시 (Example):**
```json
{
  "expo": {
    "plugins": [
      [
        "expo-build-properties",
        {
          "android": {
            "minSdkVersion": 21,
            "extraMavenRepos": [
              "https://devrepo.kakao.com/nexus/content/groups/public/",
              "https://artifact.bytedance.com/repository/pangle/",
              "https://sdk.teads.tv/android/repo",
              "https://teads.jfrog.io/artifactory/SDKAndroid-maven-prod"
            ]
          },
          "ios": {
            "deploymentTarget": "14.0"
          }
        }
      ]
    ]
  }
}
```

> KO: 어댑터별 최소 API 레벨이 다릅니다 — Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24. 더 높은 API를 요구하는 어댑터를 사용하면 `minSdkVersion`을 그에 맞게 올리십시오.
> EN: Minimum API levels differ per adapter — Core/AdFit/Teads=21, AdManager/Pangle/Unity/NaverAdManager=23, AppLovin=24. If you use an adapter that requires a higher API, raise `minSdkVersion` accordingly.

---

## 2. Development Builds 생성 (Create a Development Build)

> KO: 설정 후 아래 명령어로 커스텀 클라이언트를 빌드하여 테스트하십시오.
> EN: After configuring, build a custom client with the commands below and test.

```bash
# Android
npx expo run:android

# iOS
npx expo run:ios
```

---

## 3. 주의 사항 (Notes)

> KO:
> - **Expo Go 미지원**: 네이티브 브릿지가 없는 표준 Expo Go에서는 광고가 작동하지 않습니다. 반드시 `npx expo run:[android|ios]` 개발 빌드를 사용하세요.
> - **네이티브 설정**: 미디에이션(AdManager, AdFit, Pangle, AppLovin, Unity, NaverAdManager, Teads 등) 사용 시 필요한 `Info.plist` 및 `AndroidManifest.xml` 수정 사항은 [Mediation Setup Guide](./MEDIATION_GUIDE.md)를 참조하여 Expo `plugins` 설정으로 반영하세요. Teads는 Android 전용입니다.
> - **AdManager 사용 시**: `play-services-ads`는 25.2.0 상한이며 25.3.0+는 비호환이므로, 필요 시 버전 강제(force)를 적용하세요.
>
> EN:
> - **No Expo Go support**: ads do not work in standard Expo Go (no native bridge). Always use a `npx expo run:[android|ios]` development build.
> - **Native setup**: for mediation networks (AdManager, AdFit, Pangle, AppLovin, Unity, NaverAdManager, Teads, etc.), reflect the required `Info.plist` / `AndroidManifest.xml` changes through Expo `plugins`, per the [Mediation Setup Guide](./MEDIATION_GUIDE.md). Teads is Android-only.
> - **When using AdManager**: `play-services-ads` is capped at 25.2.0 (25.3.0+ is incompatible); force the version if needed.
