# 🔄 Migration Guide & Version Matrix

Upgrade steps for `react-native-nap-ssp`, plus the verified compatibility matrix between the plugin, the native nap mx SDKs, and third-party mediation networks.

---

## 📋 Table of Contents
1. [v0.4.0 — Native SDK Refresh](#1-v040--native-sdk-refresh)
2. [v0.3.0 Migration Guide](#2-v030-migration-guide)
3. [Verified Version Matrix](#3-verified-version-matrix)
4. [Native SDK Breaking Changes to Be Aware Of](#4-native-sdk-breaking-changes-to-be-aware-of)
5. [Roadmap](#5-roadmap)

---

## 1. v0.4.0 — Native SDK Refresh

No plugin API changed in this release. It advances both native SDKs to the versions in the current official guides.

| | v0.3.0 | **v0.4.0** |
| :--- | :--- | :--- |
| Android BOM | `2026.07.03` | **`2026.07.06`** |
| Android core (`admixer-ssp`) | `2.1.1` | **`2.1.3`** |
| `admixer-admanager` | `2.0.2` | **`2.0.4`** |
| iOS `AdMixerMediation` | `2.3.7` | **`2.4.2`** |

**Also in this release**

* **iOS Teads subspec** — `AdMixerMediationTeads` is now selectable as the `Teads` subspec. Teads was previously Android-only in this plugin.
* **Huawei Maven repository** added for Android, which the official Teads installation guide requires for Huawei device compatibility.
* **`MediationConfig.mobwith` deprecated** — Mobwith is no longer listed as a supported network in the official guide. The field still compiles but is ignored; remove it from your config.

### Action required

| Platform | Steps |
| :--- | :--- |
| **Android** | Re-sync Gradle. If another dependency pulls `play-services-ads` `25.3.0+`, force `25.2.0` — see [Setup §3②](./SETUP.md#-bundled-artifact-versions). |
| **iOS** | `pod install --repo-update`, or resolve SPM packages again. Add the `Teads` subspec if you serve Teads. |

> ⚠️ **iOS 2.4.x is not yet build-verified in this repository.** The bump follows the official release notes (2.4.0 "loadAd API 개선", 2.4.1 simulator fix, 2.4.2 stability + adapter updates), but it was applied on a machine without Xcode. Build and smoke-test your iOS target before shipping to production.

---

## 2. v0.3.0 Migration Guide

### BOM adoption & SDK version bump
v0.3.0 moved the Android build from hardcoded per-artifact coordinates to the official Bill of Materials, taking the core SDK from `2.0.0` to `2.1.1`. This resolved the runtime `NullPointerException` during view destruction tracked as issue **#100**.

### Standardized view ID prefixes (`nap_mx_*`)
All native ad layout IDs moved to the official `nap_mx_*` namespace to avoid collisions with host apps and other libraries:

| Old ID (v0.2.x) | New ID (v0.3.0+) | Purpose |
| :--- | :--- | :--- |
| `nap_ssp_tv_title` | **`nap_mx_tv_title`** | Headline |
| `nap_ssp_iv_icon` | **`nap_mx_iv_icon`** | App icon |
| `nap_ssp_tv_adv` | **`nap_mx_tv_adv`** | Advertiser name |
| `nap_ssp_tv_desc` | **`nap_mx_tv_desc`** | Body text |
| `nap_ssp_iv_main` | **`nap_mx_iv_main`** | Main media view |
| `nap_ssp_btn_cta` | **`nap_mx_btn_cta`** | Call-to-action |

If you override `nap_ssp_native_ad.xml` in your app, rename your view IDs to match.

### New `cancelLoad()` method
`InterstitialAd`, `RewardedAd` and `InterstitialVideoAd` can abort an in-flight load without disturbing a displayed ad:

```typescript
useEffect(() => {
  const interstitial = new InterstitialAd(UNIT_ID);
  interstitial.load();
  return () => {
    interstitial.cancelLoad();
    interstitial.destroy();
  };
}, []);
```

`cancelLoad()` is a no-op when nothing is in flight, so it is safe in any cleanup path.

### Standardized 2-argument error callbacks
Android core v2.1.1 simplified the failure callback to `onFailedToReceiveAd(code: Int, msg: String?)`. The plugin's `NapListenerBridge` wraps both the legacy 4-argument overloads and the 2-argument standard, so **no JS listener change is required** — `loadFailed` continues to deliver an `AdError`.

---

## 3. Verified Version Matrix

Bundled versions for `react-native-nap-ssp` **v0.4.0**:

| Network | Android artifact | Android version | iOS subspec / pod | iOS adapter |
| :--- | :--- | :--- | :--- | :--- |
| **Core SDK** | `admixer-ssp` | **`2.1.3`** | `AdMixerMediation` | **`2.4.2`** |
| **Google Ad Manager** | `admixer-admanager` | `2.0.4` | `AdMixerMediationGAM` (`GAM`) | `1.2.1` |
| **Kakao AdFit** | `admixer-adfit` | `2.0.3` | `AdMixerMediationAdFit` (`AdFit`) | `1.1.1` |
| **Pangle** | `admixer-pangle` | `2.0.2` | `AdMixerMediationPangle` (`Pangle`) | `1.2.1` |
| **AppLovin** | `admixer-applovin` | `2.0.2` | `AdMixerMediationAppLovin` (`AppLovin`) | `1.1.1` |
| **Unity Ads** | `admixer-unity` | `2.0.2` | `AdMixerMediationUnityAds` (`UnityAds`) | `1.1.1` |
| **Naver Ad Manager** | `admixer-naveradmanager` | `2.0.2` | `AdMixerMediationNAM` (`NAM`) | `1.3.1` |
| **Teads** | `admixer-teads` | `2.1.0` | `AdMixerMediationTeads` (`Teads`) | `1.1.0` |
| **BOM** | `admixer-bom` | `2026.07.06` | — | — |

### Not bundled

| Network | Status |
| :--- | :--- |
| **🧪 GMA NextGen** (`admixer-gma-nextgen`) | Beta, Android only. **Mutually exclusive with `admixer-admanager` *and* `admixer-naveradmanager`** — it requires a global exclude of classic `play-services-ads`, and Naver Ad Manager uses GAM mediation internally. Requires `minSdk 24`. Not wired into this plugin; contact [nap_mx@nasmedia.co.kr](mailto:nap_mx@nasmedia.co.kr) before adopting. |
| **Mobwith** | No longer a supported nap mx network. `MediationConfig.mobwith` is deprecated and ignored. |

### Underlying network SDK versions (Android)

| Network | Library | Bundled | Max compatible |
| :--- | :--- | :--- | :--- |
| Google Ad Manager | `play-services-ads` | `25.2.0` | `25.2.0` — ⚠️ `25.3.0+` incompatible |
| Kakao AdFit | `ads-base` | `3.21.17` | `3.22.2` |
| Pangle | `pag-sdk` | `8.0.0.5` | `8.1.0.3` |
| AppLovin | `applovin-sdk` | `13.6.3` | `13.6.3` |
| Unity Ads | `unity-ads` | `4.18.1` | `4.18.1` |
| Naver Ad Manager | `nam-bom` | `8.16.0` | `8.17.0` |
| Teads | `tv.teads.sdk.android:sdk` | `6.2.0` | `6.2.0` |

---

## 4. Native SDK Breaking Changes to Be Aware Of

These originate in the native SDKs. Most are absorbed by the plugin, but they affect you if you also touch the native layer directly or override the bundled layouts.

### Android core v2.1.3
* **New**: `isReady()` / `isLoading()` on all six ad classes. Not yet surfaced through the JS bridge — the plugin exposes `isLoaded()` on fullscreen ad classes instead.
* **Deprecated** (still functional, removed in the next major): `AdInfo.Builder.interstitialTimeout(int)`, `AdInfo.Builder.setUseBackgroundAlpha(boolean)`, and `AdMixer.AX_ERR_NO_FILL`. Branch on `AX_ERR_NO_ADS` for no-fill.

### Android core v2.1.1
* **Reward channels are now mutually exclusive.** Registering a dedicated reward listener suppresses `AdListener.onAdRewarded()`, so a grant is notified exactly once. Apps that previously handled both channels no longer double-grant.
* **House interstitial impressions are now counted.** Expect reported house interstitial impressions to rise — actual delivery is unchanged, only the tally was fixed.
* **Reward SSV postbacks** now include a unique `transaction_id` per grant and an `ifa_use` flag; `ifa` is omitted when child-directed treatment is on.

### Android core v2.1.0
* **`NativeAdViewBinder.Builder.setPrivacyViewId(int)` was removed** — use `setAdChoicesPosition(AdChoicesPosition)` (default top-right). A `nap_mx_privacy_container` slot in a custom layout can be deleted; the SDK overlays the icon itself.
* **`AdMixer.ADAPTER_*` string values changed** (`"AdManager"` → `"GoogleAdManager"`, `"KaKao Adfit"` → `"AdFit"`, `"houseAd"` → `"HouseAd"`). Constant *names* are unchanged, so this compiles silently — **recompile** any native code that compares these strings.
* **Native main media slot honours its declared size**, rather than shrinking to the creative's aspect ratio. Match your slot to the usual 1.91:1 creative to avoid letterboxing.
* **Pangle's ad-choices logo moved to the top-right** to match the SDK default.

### iOS 2.4.x
* `loadAd` API improvements (2.4.0) and a simulator launch fix (2.4.1).
* Teads adapter now requires **TeadsSDK 6.2+**.

---

## 5. Roadmap
* Surface the native `isReady()` / `isLoading()` state through the JS bridge.
* Build-verify the iOS 2.4.x upgrade in CI.
* Fabric / TurboModule native layer optimizations.
