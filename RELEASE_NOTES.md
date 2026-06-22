# Release Notes

> 한국어 + English. 전체 변경 이력은 [CHANGELOG.md](./CHANGELOG.md) 참고. / Full history in [CHANGELOG.md](./CHANGELOG.md).

## v0.2.0 (2026-06-22)

### 한국어

**메이저 네이티브 SDK 업그레이드 릴리스입니다.**

- **Android**: 벤더 SDK 를 **v1.x → v2.0.0** 으로 마이그레이션했습니다. 모든 `admixer-*` 어댑터가 `2.0.0` 으로 올라갔고, `registerAdapter()` 수동 호출이 사라졌으며(자동 등록), 전면 광고는 **Basic 전용**(popup/countDown 제거)이 되었습니다. `AdListener` 가 이름 있는 콜백으로 분리되어 플러그인 브리지를 재구성했습니다.
- **iOS**: 벤더 SDK 를 **2.3.3 → 2.3.7** 로 올리고 최소 배포 타겟을 **iOS 14.0** 으로 정렬했습니다. 전면 popup/countDown 옵션이 제거되었습니다.
- **신규 미디에이션**: **NaverAdManager**(Android·iOS) 와 **Teads**(Android 전용) 를 추가했습니다. JS 에서 `mediations.naverAdManager` / `mediations.teads` 로 활성화합니다.
- **버그 수정**: 인라인 뷰의 `setAdViewListener` reflection 타입 오류로 배너/네이티브/비디오 로드가 실패하던 문제를 수정했습니다.
- **문서**: 모든 가이드를 한국어+영어 2개 언어로 갱신했습니다.

> ⚠️ 대부분의 네이티브 변경은 플러그인이 내부에서 흡수하므로 **앱 코드 변경은 거의 필요하지 않습니다.** 단, 전면 광고의 popup/countDown JS 옵션은 제거되었습니다. 자세한 내용은 [Migration Guide](./docs/MIGRATION_GUIDE.md) 를 참고하세요.

### English

**This is a major native SDK upgrade release.**

- **Android**: migrated the vendor SDK from **v1.x to v2.0.0**. All `admixer-*` adapters are now `2.0.0`, `registerAdapter()` is no longer needed (auto-registration), and interstitials are **Basic-only** (popup/countDown removed). `AdListener` was split into named callbacks; the plugin bridge was reworked accordingly.
- **iOS**: bumped the vendor SDK from **2.3.3 to 2.3.7** and aligned the minimum deployment target to **iOS 14.0**. Interstitial popup/countDown options were removed.
- **New mediation**: added **NaverAdManager** (Android & iOS) and **Teads** (Android only). Enable them from JS via `mediations.naverAdManager` / `mediations.teads`.
- **Bug fix**: fixed an inline-view `setAdViewListener` reflection type error that broke banner/native/video loading.
- **Docs**: all guides updated and made bilingual (Korean + English).

> ⚠️ Most native changes are absorbed by the plugin, so **app code rarely needs changes** — except the removed interstitial popup/countDown JS options. See the [Migration Guide](./docs/MIGRATION_GUIDE.md).
