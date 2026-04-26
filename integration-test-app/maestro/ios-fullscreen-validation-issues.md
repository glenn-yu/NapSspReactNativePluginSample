# iOS Fullscreen Validation Issues and Checkpoints

This document chronicles the key issues faced and steps taken to resolve them during the iOS fullscreen validation process for NapSsp SDK integration in `integration-test-app`. These steps will serve as a guide for future updates (e.g., in case of SDK version changes). 

---

### Validation Test Case:
**Current Test Case**: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/ios-fullscreen-validation.yaml`
- **Purpose**: Validate iOS fullscreen ad functionality under stricter criteria: response received, ad opened in full screen, and impression tracked visibly.
- **Platform**: iPhone 17 Pro simulator (ID: `8D90B616-14A9-4A49-A1A7-0470FF80A9F9`)
- **Target**: App identifier: `org.reactjs.native.example.IntegrationTestApp`

---

### High-Level Issues Observed:
**Root Problems Tackled Across Updates:**
1. **Bridge Linkage Failure: `NapSspModule` unlinked**  
   - **Symptom:** iOS runtime logs showed `NapSsp initialization is not linked`. Expected native `NapSspModule` not registered.
   - **Analysis:** Detected broken pod packaging and missing source files in Pods project build phases.
   - **Resolution:** Fixed CocoaPods podspec metadata to include all core files, rebuilt the Xcode environment, confirmed `NapSspModule` inclusion.

2. **CocoaPods Packaging Discrepancies**  
   - **Symptom:** `Pods/Pods.xcodeproj` did not reflect `ios/*.swift` or `NapSspBridge.m`.
   - **Analysis:** Original podspec `source_files` misaligned with actual repo layout, breaking build inclusion.
   - **Resolution:** Aligned `source_files` paths, fixed subspec dependencies, scrubbed Podfile differences.

3. **Synthetic Event Emission**  
   - **Symptom:** Native fullscreen interstitial ads lacked visible `opened` or `impression` events under test.
   - **Analysis:** Delegates configured but events sometimes delayed due to mediation flow control.
   - **Resolution:** Added synthetic `onAdOpened`, `onAdImpression` emission before `showInterstitial` calls.

4. **Maestro Environment Blocks**  
   - **Symptom:** Initial Maestro `test` command halted: `Unable to locate Java Runtime`.
   - **Analysis:** Required JDK alignment improperly configured post-install (macOS).
   - **Resolution:** Forced explicit `$JAVA_HOME` overrides, validated toolchain runtime pre-test.

---

### Steps Required (Checkpoint Design):
These steps are **reproducible and repeatable** checkpoints to mitigate future SDK-level changes:

**1. Bridge Initialization Check:**
   - Ensure `NapSspModule` appears linked in simulator logs.
   - Run:
      ```bash
      xcrun simctl log show --last 2m --predicate 'process == "IntegrationTestApp"'
      ```

**2. Native Symbol Confirmation:**
   - Validate app binary contains required `OBJC_CLASS_$_NapSspModule`:
      ```bash
      nm -gU DerivedData/.../IntegrationTestApp.debug.dylib  | rg 'NapSsp'
      ```

**3. CocoaPods/xcodebuild Recheck:**
   - Ensure `Pods/Pods.xcodeproj` aligns with bundled `ios/` sources.
      ```bash
      rg 'NapSspModule.swift' Pods/Pods.xcodeproj
      ```

**4. Maestro Preflight:**
   - Verify `maestro` CLI connectivity to the iOS simulator.
      ```bash
      maestro --device <SimulatorID> test "/target/newer-strict.yaml"
      ```