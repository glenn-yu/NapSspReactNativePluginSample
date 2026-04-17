# Changelog

## Unreleased

### Added
- Stronger TypeScript config and compiled output target under `lib/`
- Typed NapSsp config, mediation, ad error, reward, and event definitions
- Bridge wrappers for the current placeholder native module names
- A banner fallback view for when the native component is not linked yet
- Example app updates that demonstrate the public API safely
- Honest CI/build placeholders that run typecheck and build instead of pretending native builds pass

### Notes
- The Android and iOS SDK integrations are still placeholders in this branch.
- Native module names in the JS layer now match the Swift placeholders in `ios/`.
