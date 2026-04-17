const Module = require('module');
const originalLoad = Module._load;

function createMockComponent(name) {
  const MockComponent = function MockComponent() {
    return {__mockComponent: name};
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
    },
  },
  NativeEventEmitter: class NativeEventEmitter {
    addListener() {
      return {remove() {}};
    }
  },
  Platform: {OS: 'ios'},
  requireNativeComponent: (name) => createMockComponent(name),
  StyleSheet: {hairlineWidth: 1, create: (styles) => styles},
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
    'isNativeModuleAvailable',
    'normalizeAdError',
  ];

  for (const key of expectedExports) {
    if (!(key in lib)) {
      throw new Error(`Missing export: ${key}`);
    }
  }

  await lib.NapSspAd.initialize({
    mediaKey: 'test-media-key',
    adUnitIds: ['banner-id', 'native-id', 'video-id', 'interstitial-id', 'interstitial-video-id', 'rewarded-id'],
    logLevel: 'debug',
    coppa: true,
  });

  if (!lib.NapSspAd.isInitialized()) {
    throw new Error('NapSspAd should be initialized after initialize()');
  }

  const config = lib.NapSspAd.getConfig();
  if (!config || config.mediaKey !== 'test-media-key' || config.adUnitIds.length !== 6) {
    throw new Error('NapSspAd should return a cloned config after initialize()');
  }

  const status = await lib.NapSspAd.getStatus();
  if (!status || typeof status !== 'object' || status.initialized !== true || status.placeholderMode !== true) {
    throw new Error('Unexpected NapSspAd status');
  }

  const banner = lib.BannerAd({adUnitId: 'banner-id'});
  const nativeAd = lib.NativeAd({adUnitId: 'native-id'});
  const videoAd = lib.VideoAd({adUnitId: 'video-id'});

  if (!banner || !nativeAd || !videoAd) {
    throw new Error('Inline ad renders should return a result');
  }

  const interstitial = new lib.InterstitialAd('interstitial-id');
  if (interstitial.isLoaded()) {
    throw new Error('Interstitial should not be loaded before native events');
  }

  const interstitialVideo = new lib.InterstitialVideoAd('interstitial-video-id');
  if (interstitialVideo.isLoaded()) {
    throw new Error('Interstitial video should not be loaded before native events');
  }

  const rewarded = new lib.RewardedAd('rewarded-id');
  const unsubscribe = rewarded.addAdEventListener('onRewarded', () => undefined);
  if (typeof unsubscribe !== 'function') {
    throw new Error('Rewarded onRewarded should return an unsubscribe function');
  }
  unsubscribe();

  if (!lib.isNativeModuleAvailable('NapSspModule')) {
    throw new Error('isNativeModuleAvailable should detect the mocked native module');
  }

  console.log('smoke-test: ok');
}

main().catch((error) => {
  console.error('smoke-test failed');
  console.error(error);
  process.exit(1);
});
