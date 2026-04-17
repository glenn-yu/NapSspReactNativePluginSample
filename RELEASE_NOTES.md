# Release notes

## Current release status
This repository is still a scaffold/sample project, not a publish-ready SDK wrapper.

### Shipped in this update
- A typed JS/TS API surface for init, banner, interstitial, and rewarded ads
- Runtime checks and clearer errors when native modules are missing
- Example app scaffolding that explains the current placeholder state
- CI/build commands that validate the TypeScript surface

### Remaining blockers
- Android native bridge methods still need real SDK integration
- iOS native bridge methods still need real SDK integration
- Integration tests cannot be meaningful until the native bridge exists

### Publishing checklist
- Verify the Android and iOS module names match the JS wrappers
- Run `npm run verify`
- Build and run the example on both platforms
- Update the version number and package metadata before publishing
