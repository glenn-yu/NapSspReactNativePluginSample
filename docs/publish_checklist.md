# Publish Checklist

## Package
- Name: `react-native-nap-ssp`
- Current version: `0.1.2`
- Repo: `https://github.com/glenn-yu/react-native-nap-ssp`

## Version-aligned files
- `package.json`
- `ios/NapSspPlugin.podspec`
- `README.md`
- `CHANGELOG.md`
- `RELEASE_NOTES.md`

## Verified before publish
Run from repo root:

```bash
npm run verify
npm pack --dry-run
```

Run example smoke test:

```bash
cd example/ExampleHostApp
npm test -- --runInBand
```

## Checked results
- `npm run verify` passed
- `npm pack --dry-run` passed
- ExampleHostApp Jest smoke test passed
- Dry-run package result:
  - tarball: `react-native-nap-ssp-0.1.2.tgz`
  - package size: about `37.6 kB`
  - unpacked size: about `181.0 kB`

## Environment requirements
### Android
- Java/JDK installed
- Android SDK installed
- `adb` available on PATH

### iOS
- macOS
- Xcode / command line tools
- CocoaPods installed

## npm publish steps
From repo root:

```bash
npm whoami
```

If not logged in:

```bash
npm login
# or
npm adduser
```

Then publish:

```bash
npm run verify
npm pack --dry-run
npm publish --access public
```

## Current blocker
- This machine is not logged into npm yet.
- `npm whoami` returned `ENEEDAUTH`.

## Recommended GitHub Release
### Title
`v0.1.2`

### Body
```md
## react-native-nap-ssp v0.1.2

This release improves release consistency and makes the repository easier to validate locally.

### Highlights
- Added the full `example/ExampleHostApp` React Native host app to the repository
- Restored `InterstitialAdOptions` in the public TypeScript API
- Aligned rewarded example usage with the current event contract
- Improved Android and iOS example helper scripts with prerequisite checks
- Synced package metadata, README, changelog, release notes, and iOS podspec to `0.1.2`

### Verified
- `npm run verify`
- `cd example/ExampleHostApp && npm test -- --runInBand`

### Notes
- Android local execution requires Java/JDK, Android SDK, and `adb`
- iOS local execution requires macOS, Xcode, and CocoaPods
- Real ad loading still depends on valid media keys, ad unit IDs, and host app SDK setup
```
