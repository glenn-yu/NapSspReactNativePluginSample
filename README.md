# react-native-nap-ssp

TypeScript-first sample scaffold for the KT Nasmedia nap ssp React Native plugin.

> Status: the JS/TS surface is improved, and the bridge wrappers now line up with the placeholder native names. Android now exposes a structured placeholder bridge with status metadata and event plumbing; the real vendor SDK calls are still opt-in.

## What is in this repo
- Typed public API for initialization, banner ads, interstitial ads, and rewarded ads
- A bridge layer that validates inputs, reports missing native modules clearly, and exposes native status metadata
- A banner fallback component for environments where the native view is not linked yet
- An example app that demonstrates the intended usage pattern
- Docs for current status, TODOs, Android integration assumptions, and release notes

## Install

```bash
npm install
```

## Build the JS/TS package

```bash
npm run build
```

The build emits compiled output into `lib/` and generates declarations for TypeScript consumers.

## Example app

The example package consumes the root library via a local file dependency.

```bash
npm run build
npm --prefix example start
```

See [`example/README.md`](./example/README.md) for more context.

## Public API snapshot

### Initialization

```ts
import { NapSspAd } from 'react-native-nap-ssp';

await NapSspAd.initialize({
  mediaKey: 'YOUR_MEDIA_KEY',
  adUnitIds: ['BANNER_HOME', 'INTERSTITIAL_HOME'],
  logLevel: 'info',
  coppa: false,
});

const status = await NapSspAd.getStatus();
console.log(status.placeholderMode ? 'placeholder runtime' : 'vendor runtime');
```

### Banner ads

```tsx
import { BannerAd } from 'react-native-nap-ssp';

<BannerAd
  adUnitId="BANNER_HOME"
  size="BANNER_320x50"
  onAdLoaded={() => console.log('banner loaded')}
  onAdFailedToLoad={(error) => console.log(error.message)}
/>
```

### Interstitial ads

```ts
import { InterstitialAd } from 'react-native-nap-ssp';

const interstitial = new InterstitialAd('INTERSTITIAL_HOME');
await interstitial.load();
await interstitial.show();
```

### Rewarded ads

```ts
import { RewardedAd } from 'react-native-nap-ssp';

const rewarded = new RewardedAd('REWARDED_HOME');
rewarded.addAdEventListener('onRewarded', (reward) => {
  console.log(reward.type, reward.amount);
});
```

## Event shape

The JS layer now exposes a consistent event model:
- `loaded`
- `loadFailed`
- `opened`
- `closed`
- `clicked`
- `impression`
- `rewarded` / `onRewarded` for rewarded ads

These events are currently driven by the JS wrapper layer and will be connected to the real native callbacks later.

## Current limitations

- Android now has the package/module/view scaffolding in place, but the official nap ssp SDK entry points still need to be wired to the vendor APIs
- Vendor Android dependencies are gated behind the Gradle property `napSsp.enableVendorSdk=true` so the placeholder bridge can build without the private SDKs present
- iOS native SDK integration is still placeholder-only
- The example app is intentionally defensive and will show placeholder/failure text until the native modules are linked in an app
- `NapSspAd.getStatus()` exposes the current placeholder/native readiness snapshot when the host bridge provides it
- CI validates TypeScript and the JS build only; it does not build the native projects yet

## Roadmap / status docs
- [`docs/nap_ssp_plan.txt`](./docs/nap_ssp_plan.txt)
- [`CHANGELOG.md`](./CHANGELOG.md)
- [`RELEASE_NOTES.md`](./RELEASE_NOTES.md)
- [`TODO.md`](./TODO.md)
