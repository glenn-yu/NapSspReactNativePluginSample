# Maestro Readiness — 2026-04-30

## Snapshot
- Android Maestro validate: passing on latest SDK bump.
- Android short soak (10m): 4/4 PASS after popup-handling hardening.
- Android prior full soak (30m): 10/10 PASS after validator-popup fix.
- iOS Maestro validate: now passing with deterministic interstitial proof IDs (`summary-inter-opened-yes`, `summary-inter-impression-yes`).
- iOS short soak (10m): 11/11 PASS on the updated selector-driven flow.

## Latest commits
- `b0e6014` — Update Nap SSP SDK versions to latest releases
- `a3bf3e8` — Stabilize Maestro validation flows
- `0d9405a` — Align iOS soak fallback with validate flow

## What is stable now
### Android
- Stable selectors for banner/native/video/interstitial flow.
- Popup guard for `Ad Manager native ad validator` (`Dismiss` / `See issues`).
- Current evidence is strong enough for internal/commercial confidence.

### iOS
- Interstitial proof no longer depends on OCR for the main validate flow.
- App now exposes deterministic proof IDs for interstitial `opened` and `impression` state.
- Short soak passed cleanly on the updated flow.

## Remaining gap
- iOS fallback OCR remains in the helper scripts as a safety net, but current latest validate and short soak evidence no longer needed it.
- A fresh full 30-minute iOS soak on the new selector-only proof path would be the last nice-to-have hardening step.

## Recommended next step
1. Run a fresh full 30-minute iOS soak on the new proof IDs.
2. If it stays clean, treat both Android and iOS Maestro coverage as commercially/internal-ready.
3. Optionally remove or downgrade OCR fallback once enough clean iOS runs accumulate.
