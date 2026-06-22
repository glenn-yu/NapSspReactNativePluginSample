# 용어 사전 (Glossary)

> KO: `react-native-nap-ssp` (0.2.0) 및 나스미디어 광고 SDK(Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7) 사용 시 등장하는 주요 용어 설명입니다.
> EN: Key terms used with `react-native-nap-ssp` (0.2.0) and the Nasmedia ad SDK (Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7).

---

## 1. 식별자 (Identifiers)

### MediaKey
> KO: 나스미디어에서 매체사(앱) 단위로 발급하는 고유 키. SDK 초기화(`initialize`) 시 필수입니다.
> EN: A unique key issued by Nasmedia per publisher (app). Required at SDK initialization (`initialize`).

### AdUnitId
> KO: 앱 내 개별 광고 지면(배너, 전면 등)을 식별하는 고유 ID. 한 앱에 여러 지면이 있을 수 있으며 각 포맷에 맞는 ID를 사용합니다.
> EN: A unique ID identifying an individual ad slot (banner, interstitial, etc.). An app may have many slots; use the ID matching each format.

---

## 2. 광고 포맷 (Ad Formats)

### Banner (배너)
> KO: 화면의 일정 영역을 차지하는 띠 형태의 광고입니다.
> EN: A strip-shaped ad occupying a fixed region of the screen.

### Interstitial (전면)
> KO: 화면 전체를 덮는 광고로, 페이지 전환이나 게임 스테이지 종료 시 노출됩니다. **v2부터 Basic 전용**이며 popup/countDown 타입은 제거되었습니다.
> EN: A full-screen ad shown at page transitions or game stage ends. **From v2 it is Basic-only**; the popup/countDown types were removed.

### Interstitial Video (전면 동영상)
> KO: 전면 형태로 노출되는 풀스크린 동영상 광고입니다.
> EN: A full-screen video ad displayed in interstitial form.

### Rewarded (리워드)
> KO: 동영상 광고 시청 완료 시 사용자에게 아이템·포인트 등 보상을 지급하는 풀스크린 포맷입니다.
> EN: A full-screen format that grants the user a reward (item/points) upon completing a video ad.

### Native (네이티브)
> KO: 앱 디자인과 이질감 없이 어우러지도록 구성 요소(제목, 이미지 등)를 직접 배치하는 광고입니다. ([Native Assets Guide](./NATIVE_ASSETS_GUIDE.md) 참조)
> EN: An ad whose elements (title, image, etc.) you place yourself so it blends seamlessly with the app's design. (See the [Native Assets Guide](./NATIVE_ASSETS_GUIDE.md).)

### Inline Video (인라인 비디오, 뷰형)
> KO: 화면 안에 뷰 형태로 삽입되는 동영상 광고입니다(풀스크린 아님). v2에서 뷰형 load API가 추가되었습니다.
> EN: A video ad embedded inline as a view (not full-screen). v2 added the view-type load API.

---

## 3. 기술 용어 (Technical Terms)

### Mediation (미디에이션)
> KO: 여러 광고 네트워크(나스미디어, AdManager, AdFit, Pangle, AppLovin, Unity, NaverAdManager, Teads 등)를 하나의 SDK로 관리하고 수익이 가장 높은 광고를 우선 노출하는 기술입니다. v2부터 `initialize()`가 Gradle 의존성의 어댑터를 자동 등록하므로 수동 `registerAdapter()`가 불필요합니다.
> EN: A technique to manage multiple ad networks (Nasmedia, AdManager, AdFit, Pangle, AppLovin, Unity, NaverAdManager, Teads, etc.) through one SDK and prioritize the highest-yielding ad. From v2, `initialize()` auto-registers adapters from Gradle dependencies, so manual `registerAdapter()` is no longer needed.

### Adapter (어댑터)
> KO: 특정 광고 네트워크를 미디에이션에 연결하는 모듈입니다. `MediationConfig`로 활성화하며, v2에서 `naverAdManager`와 `teads`(Android 전용)가 추가되었습니다.
> EN: A module connecting a specific ad network to mediation. Enabled via `MediationConfig`; v2 added `naverAdManager` and `teads` (Android-only).

### Impression (임프레션 / 노출)
> KO: 광고가 실제 사용자 화면에 노출된 횟수. 수익 집계의 핵심 기준입니다.
> EN: The number of times an ad is actually shown on a user's screen. The core basis for revenue measurement.

### S2S (Server-to-Server) Callback
> KO: 리워드 광고 시청 완료 시 나스미디어 서버가 매체사 서버로 직접 보상 정보를 전송하는 방식. 보안상 가장 권장되는 보상 검증 방법입니다.
> EN: On rewarded completion, the Nasmedia server sends reward info directly to the publisher's server. The most secure, recommended reward-verification method.

### Fill Rate (필 레이트)
> KO: 광고 요청 대비 실제 광고가 전달·노출된 비율입니다.
> EN: The ratio of delivered/shown ads to ad requests.

### COPPA / GDPR / CCPA (개인정보 규정)
> KO: 아동·EU·캘리포니아 등 개인정보 규정. 플러그인의 `coppa` 옵션 및 v2 전역 개인정보 API는 [Privacy Guide](./PRIVACY_GUIDE.md)를 참조하세요.
> EN: Privacy regulations (children / EU / California). See the [Privacy Guide](./PRIVACY_GUIDE.md) for the plugin's `coppa` option and v2 global privacy APIs.
