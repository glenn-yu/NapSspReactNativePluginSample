# TODO - Phase 0 / scaffold hardening

## Done in this pass
- [x] Added stricter TypeScript config and a build target under `lib/`
- [x] Replaced `any`-heavy config/event types with explicit JS/TS contracts
- [x] Added native bridge wrappers that match the current Swift placeholder module names
- [x] Added a placeholder banner fallback view with clear messaging
- [x] Updated the example app to demonstrate initialization and safe interstitial calls
- [x] Added changelog and release notes scaffolding
- [x] Switched CI/build placeholders to typecheck + build instead of echo-only steps
- [x] Added a structured Android bridge with explicit constants, state snapshots, and placeholder event plumbing
- [x] Documented the Android build/runtime assumptions in `docs/android_integration.md`

## Done in this pass
- [x] Wired Android `NapSspModule` / `InterstitialModule` / `RewardedAdModule` / `BannerViewManager` to the real nap ssp SDK APIs with reflection-based vendor loading and placeholder fallback
- [x] Wired iOS `NapSspModule` / `InterstitialModule` / `RewardedModule` / `BannerView` to the real nap ssp SDK APIs with placeholder fallback when vendor frameworks are unavailable
- [x] Added unit/smoke verification for the JS/TS bridge layer (`npm run verify`)

## Still blocked
- [ ] Confirm the exact native callback names for all mediation/event edges against the final vendor SDK versions before removing the remaining placeholder event assumptions
- [x] Confirm the example app on physical device/simulator after native SDK wiring and CocoaPods dependency resolution
- [ ] Add end-to-end native tests once the bridge is exercised against real SDK responses
- [ ] Add publishing automation after package metadata is finalized

## Validation notes
- [x] `integration-test-app/ios`: `pod install --repo-update` completed successfully
- [x] Android `integration-test-app` built, installed, and passed Maestro flow using Metro + `adb reverse tcp:8081 tcp:8081`
- [x] iOS `integration-test-app` built for simulator, launched on `iPhone 17 Pro`, and passed Maestro flow when targeting the simulator UDID explicitly
- [x] Hardened Maestro flows to wait on stable controls (`SDK 초기화`) and tolerate optional alert/dismiss paths during ad validation
