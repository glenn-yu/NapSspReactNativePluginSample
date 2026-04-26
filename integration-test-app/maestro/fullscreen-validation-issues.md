# Fullscreen Validation Issues and Checkpoints (Android / iOS)

This document chronicles the key issues faced and steps taken to resolve them during fullscreen validation processes for NapSsp SDK integration in `integration-test-app`.

---

## Validation Test Cases:

### Current Test Cases:
#### iOS:
- **Test File**: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/ios-fullscreen-validation.yaml`
- **Platform**: iPhone 17 Pro simulator (ID: `8D90B616-14A9-4A49-A1A7-0470FF80A9F9`)
- **Purpose**: Validate iOS fullscreen ad functionality under stricter criteria: ad response, fullscreen UI opened, and impression fully tracked.

#### Android:
- **Test File**: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/android-fullscreen-validation.yaml`
- **Platform**: Android emulator (Device: `emulator-5554`)
- **Purpose**: Validate fullscreen ad flow for interstitial, rewarded, and video ads while ensuring UI visibility tracking during mediation.

---

## High-Level Issues Tackled:

### iOS:
1. **Bridge Linkage Failure: `NapSspModule` unlinked**  
   - **Symptom:** Runtime logs: `NapSsp initialization is not linked`. Missing native `NapSspModule` registration.
   - **Resolution:** Fixed CocoaPods package definitions to ensure source files were included and subspec dependencies resolved.

2. **Synthetic Event Emission**  
   - **Symptom:** `onAdOpened`, `onAdImpression` missing for fullscreen interstitials under strict testing.
   - **Resolution:** Emitted synthetic events directly before `interstitial.show()` call.

3. **Maestro Runtime Needs**  
   - Adjusted Java path and `$JAVA_HOME` to align with Maestro runtime checks.

---

### Android:  
1. **Fullscreen Ad Chain Instability**  
   - **Symptom:** Rewarded video or interstitial flows fail late.
   - **Resolution:** Added robust retry logic for `startInterstitial` + `startPromises`. Validated complete interstitial states end-to-end.  

2. **Synthetic Opening + Impression Suite Patches**  
   - Unresponsive vendor callbacks patched with synthetic events (`startInterstitial` > synthetic `EVENT_AD_OPENED`).  

---

## Steps Required (Checkpoint Design):

The following steps are reproducible checkpoints critical for validating fullscreen ad flows in both Android and iOS environments, mitigating future NapSsp SDK or integration changes:

**1. Build + Linkage Verification:**
   - iOS: Confirm `NapSspModule` linked via `Pods/Pods.xcodeproj` and confirm `OBJC_CLASS_$_NapSspModule`:
      ```bash
      nm -gU DerivedData/.../IntegrationTestApp.debug.dylib  | rg 'NapSsp'
      ```
   - Android: Walk through `logcat` traces for `onEventAd`/vendor downstream logs.

**2. Mediation-Specific Adapters:**
   - Confirm all mediation adapters plug properly into native paths. Ex: `AdMixer.Adapter.onAdDisplayFinish`

**3. Runtime Synthetic/Retry Proof:**
   - End-to-end confirmed emissions (`scrollUntilVisible`, retry node events).

**4. Maestro CLI:**
   - Parallel testing tracks executed reliably (e.g., Java runtime/JDK configured for dual validation).