# Example app

`ExampleHostApp` is a runnable integration reference, not a toy. Each panel walks the full
lifecycle a media app needs for one format:

```
initialize → load → callback → show / render → success or failure → reload → cleanup
```

Every SDK callback is written to an on-screen event log, so you can see exactly which events fire —
and which do not — on each platform.

| File | What to copy from it |
| :--- | :--- |
| [`App.tsx`](./ExampleHostApp/App.tsx) | Initialization order, per-format panels, listener cleanup on unmount. |
| [`adConfig.ts`](./ExampleHostApp/adConfig.ts) | Where the media key and ad unit IDs belong. |
| [`android/gradle.properties`](./ExampleHostApp/android/gradle.properties) | `napSsp.mediations` adapter selection. |
| [`android/settings.gradle`](./ExampleHostApp/android/settings.gradle) | The extra Maven repositories the adapters need. |

---

## What it covers

| Format | Load | Show | Reload | Cleanup |
| :--- | :---: | :---: | :---: | :---: |
| Banner | on mount | automatic | `ref.reload()` | unmount |
| Native | on mount | automatic | `ref.reload()` | unmount |
| Inline video | on mount | automatic | `ref.reload()` | unmount |
| Interstitial | `load()` | `show()` / `start()` | `load()` again | `destroy()` |
| Interstitial video | `load()` | `show()` / `start()` | `load()` again | `destroy()` |
| Rewarded | `load()` | `show()` / `start()` | `load()` again | `destroy()` |

It also exercises `cancelLoad()`, `isReady()` and `getStatus()`.

---

## Prerequisites

* Node 20+
* **Android** — JDK 17, Android SDK with API 36, `adb` on `PATH`, an emulator or device
* **iOS** — macOS, Xcode 16+, CocoaPods

Replace the values in `adConfig.ts` with the media key and ad unit IDs issued for your own app on
the [partner site](https://publisher.admixer.co.kr). The values committed here are nap mx test
inventory and will not serve your ads.

---

## Run it

From the repository root, build the package first — the example consumes `lib/`, not `src/`:

```bash
npm install
npm run build
```

Then:

```bash
cd example/ExampleHostApp
npm install

npm run start        # Metro
npm run android      # or
npm run ios          # macOS only
```

---

## Notes on this app's configuration

### `newArchEnabled=false`

React Native 0.76+ enables the New Architecture by default. This plugin ships legacy
`ReactPackage` modules and `SimpleViewManager` views; New Architecture support has **not** been
verified, so the example pins the legacy architecture rather than claiming something untested.

### Kotlin

The app is generated from the React Native 0.81 template, whose Kotlin default (2.1.20) already
satisfies the plugin's requirement. On React Native 0.76–0.80 you must raise `kotlinVersion` to
`2.1.21` yourself — see [Setup](../docs/SETUP.md#kotlin-요구사항).

### `metro.config.js`

This app links the plugin from the repository root (`file:../..`), which Metro sees as a symlink
pointing outside the project. `metro.config.js` therefore sets `watchFolders`, blocks the
repository's own `node_modules` (otherwise the plugin gets a second copy of React and every hook
throws `Cannot read property 'useRef' of null`) and points `nodeModulesPaths` at this app.

**An app that installs `react-native-nap-ssp` from npm needs none of this** — the default Metro
config is enough.

### Mediation repositories

`android/settings.gradle` declares the Kakao, ByteDance, Teads and Huawei Maven repositories. These
have to live in the **app**, not the library: Gradle resolves a library's transitive dependencies
using the consuming project's repositories.

---

## Verification status

| Check | Result |
| :--- | :--- |
| Android — plugin AAR assembles against nap mx core 2.3.0 with all 7 adapters | ✅ verified |
| Android — host app assembles, merges and dexes the plugin + adapters | ✅ verified (standalone harness, AGP 8.7.2 / Kotlin 2.1.21 / compileSdk 35) |
| Android — this app builds and runs on a device | ✅ verified (Galaxy SM-S947N, Android 16) |
| Real ad delivery on a device | ✅ banner and inline video served and reported impressions; native returned a genuine no-fill (`nap_ssp_no_ads`) |
| iOS — build | ⛔ not measured; no macOS/Xcode available in the development environment |
| New Architecture | ⛔ not measured |

Treat anything marked "not measured" as unverified — do your own smoke test before shipping.
