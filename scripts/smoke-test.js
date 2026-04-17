const Module = require('module');
const originalLoad = Module._load;

function createMockComponent(name) {
  const MockComponent = function MockComponent() {
    return { __mockComponent: name };
  };
  MockComponent.displayName = name;
  return MockComponent;
}

const mockReactNative = {
  NativeModules: {
    NapSspModule: {
      initialize: async () => undefined,
      setLogLevel: () => undefined,
      setCoppa: () => undefined,
      getStatus: () => ({ initialized: true }),
    },
  },
  NativeEventEmitter: class NativeEventEmitter {
    addListener() {
      return { remove() {} };
    }
  },
  Platform: { OS: 'ios' },
  requireNativeComponent: (name) => createMockComponent(name),
  StyleSheet: { hairlineWidth: 1, create: (styles) => styles },
  Text: 'Text',
  View: 'View',
};

Module._load = function patchedLoad(request, parent, isMain) {
  if (request === 'react-native') {
    return mockReactNative;
  }
  return originalLoad.call(this, request, parent, isMain);
};

async function main() {
  const lib = require('../lib');

  const expectedExports = [
    'NapSspAd',
    'BannerAd',
    'NativeAd',
    'VideoAd',
    'InterstitialAd',
    'RewardedAd',
    'InterstitialVideoAd',
  ];

  for (const key of expectedExports) {
    if (!(key in lib)) {
      throw new Error(`Missing export: ${key}`);
    }
  }

  await lib.NapSspAd.initialize({ mediaKey: 'test-media-key', adUnitIds: ['banner-id'] });

  if (!lib.NapSspAd.isInitialized()) {
    throw new Error('NapSspAd should be initialized after initialize()');
  }

  const status = await lib.NapSspAd.getStatus();
  if (!status || typeof status !== 'object' || status.initialized !== true) {
    throw new Error('Unexpected NapSspAd status');
  }

  const banner = lib.BannerAd({ adUnitId: 'banner-id' });
  if (!banner) {
    throw new Error('BannerAd render returned empty result');
  }

  const interstitial = new lib.InterstitialAd('interstitial-id');
  if (interstitial.isLoaded()) {
    throw new Error('Interstitial should not be loaded before native events');
  }

  const rewarded = new lib.RewardedAd('rewarded-id');
  const unsubscribe = rewarded.addAdEventListener('onRewarded', () => undefined);
  if (typeof unsubscribe !== 'function') {
    throw new Error('Rewarded onRewarded should return an unsubscribe function');
  }
  unsubscribe();

  console.log('smoke-test: ok');
}

main().catch((error) => {
  console.error('smoke-test failed');
  console.error(error);
  process.exit(1);
});
