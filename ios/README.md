# NapSspPlugin iOS

This folder contains the React Native iOS bridge for `react-native-nap-ssp`.

## Exported native entry points

- `NapSspModule`
- `NapSspInterstitial`
- `NapSspRewarded`
- `NapSspBannerView`

## Notes

- The current implementation is a placeholder runtime that preserves the expected JS bridge shape.
- `show(adUnitId)` is intentionally part of the interstitial and rewarded native methods so the bridge lines up with the JS wrapper classes.
- ATT is treated as iOS 14.5+ and should still be requested from app code at an appropriate consent moment.

See [`../docs/ios_integration.md`](../docs/ios_integration.md) for setup and runtime details.
