# 로드맵 (Roadmap)

> KO: `react-native-nap-ssp` 프로젝트의 진행 현황 및 향후 계획입니다. (현재 플러그인 0.2.0)
> EN: Progress and future plans for the `react-native-nap-ssp` project. (Current plugin 0.2.0.)

---

## 완료 (Done) — 2026 Q2

> KO:
> - [x] 전 포맷(배너, 전면, 전면 동영상, 리워드, 네이티브, 인라인 비디오) 네이티브 연동
> - [x] **네이티브 SDK v2 마이그레이션** (Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7)
> - [x] **NaverAdManager 어댑터 추가** (Android/iOS)
> - [x] **Teads 어댑터 추가** (Android 전용)
> - [x] 전면 광고 Basic 전용화(popup/countDown 제거) 및 v2 변경 사항 플러그인 내부 흡수
> - [x] Maestro 기반 자동화 검증 시스템 구축
> - [x] 상세 API 및 사용자 가이드(한/영) 정비
>
> EN:
> - [x] Native integration for all formats (banner, interstitial, interstitial video, rewarded, native, inline video)
> - [x] **Native SDK v2 migration** (Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7)
> - [x] **NaverAdManager adapter added** (Android/iOS)
> - [x] **Teads adapter added** (Android-only)
> - [x] Interstitial made Basic-only (popup/countDown removed); v2 changes absorbed inside the plugin
> - [x] Maestro-based automated validation
> - [x] Detailed API & user guides updated (KO/EN)

---

## 향후 계획 (Next Steps)

### New Architecture 지원 (New Architecture — JSI/Fabric)
> KO: 현재 Old Architecture 기반 플러그인을 TurboModules 및 Fabric Renderer를 지원하도록 업그레이드할 예정입니다.
> EN: Upgrade the currently Old-Architecture plugin to support TurboModules and the Fabric renderer.

### iOS Teads 어댑터 (iOS Teads Adapter)
> KO: Teads는 현재 Android 전용입니다. iOS용 Teads 어댑터가 제공되면 플러그인에 반영할 예정입니다.
> EN: Teads is currently Android-only. We will integrate an iOS Teads adapter once it becomes available.

### Expo Config Plugin 제공 (Expo Config Plugin)
> KO: `app.json` 설정만으로 네이티브 권한·저장소를 자동 구성하는 Config Plugin을 개발할 예정입니다.
> EN: Provide a Config Plugin that auto-configures native permissions/repos from `app.json` alone.

### 비즈보드(Bizboard) 전용 컴포넌트 (Bizboard Component)
> KO: 카카오 애드핏 비즈보드 단독 지면에 최적화된 전용 UI 컴포넌트 추가를 검토 중입니다.
> EN: Considering a dedicated UI component optimized for the Kakao AdFit Bizboard slot.

### 추가 미디에이션 어댑터 확장 (More Mediation Adapters)
> KO: 지원 네트워크 외 추가 광고 네트워크 어댑터 지원을 지속 확대할 계획입니다.
> EN: Continue expanding support for additional ad network adapters.
