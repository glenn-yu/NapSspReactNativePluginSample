# Maestro Ad Validation Debug Notes

## Goal
Tighten validation so a test passes only when ad response is received and the creative is actually exposed.

## Findings so far

### 1. Previous PASS criteria were too weak
Earlier Maestro flows mainly confirmed that the flow advanced, dialogs were dismissed, and the event-log section became reachable. That was not strong enough to prove a real ad response plus creative exposure.

### 2. Android Maestro transport was flaky but recoverable
A full cleanup sequence restored Android Maestro one-shot validation:

```bash
pkill -f maestro || true
adb kill-server
adb start-server
lsof -i :7001
```

This indicates Android Maestro was not permanently broken, but vulnerable to stale host/device session state.

### 3. A real test-app issue existed in the view-ad status path
The integration test app was marking some view ads as effectively visible at `onAdLoaded` time.
That was incorrect.

Specifically:
- `BannerAd` already exposed `onAdImpression`
- `NativeAd` did not pass `onAdImpression` through its JS wrapper
- `VideoAd` did not pass `onAdImpression` through its JS wrapper
- `integration-test-app/App.tsx` was treating `onAdLoaded` as if it also implied impression for banner/native/video

## Root-cause split

### Test-app / JS wrapper issues
Confirmed:
- `NativeAd.tsx` was missing `onAdImpression` prop plumbing
- `VideoAd.tsx` was missing `onAdImpression` prop plumbing
- `App.tsx` status panel logic was too optimistic for banner/native/video

These are test-app / JS-layer issues, not necessarily native plugin failures.

### Native plugin issues
Still under investigation for exact format-by-format confirmation.
Current Android native modules do appear to emit meaningful lifecycle events for fullscreen formats:
- interstitial: `DISPLAYED -> opened + impression`
- rewarded: `DISPLAYED -> opened + impression`, plus `EARNEDREWARD`

So fullscreen Android callback wiring currently looks more plausible than the original test-app status panel.

## Fixes applied
- Added `onAdImpression` support to `NativeAdProps`
- Added `onAdImpression` support to `VideoAdProps`
- Passed `onAdImpression` through `NativeAd.tsx`
- Passed `onAdImpression` through `VideoAd.tsx`
- Updated `integration-test-app/App.tsx` so banner/native/video only set `impression=true` on actual impression callbacks
- Updated Maestro flows to wait for stronger status-panel conditions instead of only generic flow progress

## Current working hypothesis
- A meaningful part of the recent validation failure was caused by the test app overstating ad visibility status.
- After the JS/test-app fixes, any remaining failures are more likely to reflect either:
  1. selector/visibility issues in Maestro, or
  2. real native/plugin callback gaps per ad format.

## Next debugging steps
1. Rebuild Android and iOS apps with the new status wiring.
2. Re-run one-shot Maestro validation on both platforms.
3. Compare:
   - status panel text
   - event log text
   - native logs
4. If status panel still does not advance despite event logs, inspect RN rendering/visibility behavior.
5. If event logs also lack impression/opened/rewarded signals, inspect native plugin callback bridging format by format.
