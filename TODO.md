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

## Still blocked
- [ ] Wire Android `NapSspModule` / `InterstitialModule` / `RewardedAdModule` / `BannerViewManager` to the real nap ssp SDK APIs
- [ ] Wire iOS `NapSspModule` / `InterstitialModule` / `BannerView` to the real nap ssp SDK APIs
- [ ] Confirm the exact native callback names for all mediation/event edges before replacing the placeholder state machine
- [ ] Confirm the example app on device/simulator after native SDK wiring
- [ ] Add unit tests once the bridge starts returning real values
- [ ] Add publishing automation after package metadata is finalized
