# Expo Compatibility Guide

`react-native-nap-ssp` 플러그인은 네이티브 코드(Kotlin/Swift)를 포함하고 있으므로, **Expo Go** 앱에서는 직접 실행할 수 없습니다. Expo 환경에서 사용하려면 **Development Builds**를 생성해야 합니다.

---

## 1. Expo Prebuild 사용

Expo 프로젝트에서 이 플러그인을 사용하려면 아래 절차를 따르십시오.

### 1.1 의존성 설치
```bash
npx expo install react-native-nap-ssp
```

### 1.2 네이티브 설정 (Config Plugins)
현재 본 플러그인은 자동 Config Plugin을 지원하지 않으므로, `app.json` 또는 `app.config.js`에서 필요한 권한 및 메타데이터를 직접 설정하거나, `expo-build-properties`를 활용해야 합니다.

**app.json 예시:**
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
              "https://devrepo.kakao.com/nexus/content/groups/public/"
            ]
          },
          "ios": {
            "deploymentTarget": "13.0"
          }
        }
      ]
    ]
  }
}
```

---

## 2. Development Builds 생성

설정 후 아래 명령어를 통해 커스텀 클라이언트를 빌드하여 테스트하십시오.

```bash
# Android
npx expo run:android

# iOS
npx expo run:ios
```

---

## 3. 주의 사항
- **Expo Go 미지원**: 네이티브 브릿지가 없는 표준 Expo Go에서는 광고가 작동하지 않습니다. 반드시 `npx expo run:[android|ios]`를 통한 개발 빌드를 사용하세요.
- **네이티브 설정**: 미디에이션(Google, Pangle 등) 사용 시 필요한 `Info.plist` 및 `AndroidManifest.xml` 수정 사항은 [Mediation Setup Guide](./MEDIATION_GUIDE.md)를 참조하여 Expo의 `plugins` 설정을 통해 반영해야 합니다.
