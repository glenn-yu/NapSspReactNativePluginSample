jest.mock('react-native/Libraries/BatchedBridge/NativeModules', () => ({
  __fbBatchedBridgeConfig: {}, 
  UIManager: { RCTView: {}, setJSResponder: jest.fn() },
  PlatformConstants: { osVersion: '13.0', platform: 'iOS' },
}));

jest.mock('react-native/Libraries/StyleSheet/StyleSheet', () => ({
  create: jest.fn(),
}));

jest.mock('react-native/Libraries/TurboModule/TurboModuleRegistry', () => ({
  get: jest.fn(),
  getEnforcing: jest.fn(),
}));

jest.mock('react-native/Libraries/ReactNative/AppRegistry', () => ({
  registerComponent: jest.fn(),
  getApplication: jest.fn(),
}));

jest.mock('react-native/Libraries/Renderer/shims/ReactNative', () => ({
  render: jest.fn(),
}));

jest.mock('react-native-nap-ssp', () => {
  const React = require('react');
  const { View } = require('react-native');

  const createMockAd = (name) => {
    return (props) => React.createElement(View, { ...props, testID: name });
  };

  return {
    NapSspAd: {
      initialize: jest.fn().mockResolvedValue(undefined),
      isInitialized: jest.fn().mockReturnValue(true),
      getConfig: jest.fn().mockReturnValue({ mediaKey: 'test', adUnitIds: [] }),
      getStatus: jest.fn().mockResolvedValue({ initialized: true, placeholderMode: true }),
    },
    BannerAd: createMockAd('BannerAd'),
    NativeAd: createMockAd('NativeAd'),
    VideoAd: createMockAd('VideoAd'),
    InterstitialAd: jest.fn().mockImplementation(() => ({
      load: jest.fn().mockResolvedValue(undefined),
      show: jest.fn().mockResolvedValue(undefined),
      start: jest.fn().mockResolvedValue(undefined),
      destroy: jest.fn(),
      addAdEventListener: jest.fn().mockReturnValue(jest.fn()),
      isLoaded: jest.fn().mockReturnValue(true),
    })),
    RewardedAd: jest.fn().mockImplementation(() => ({
      load: jest.fn().mockResolvedValue(undefined),
      show: jest.fn().mockResolvedValue(undefined),
      start: jest.fn().mockResolvedValue(undefined),
      destroy: jest.fn(),
      addAdEventListener: jest.fn().mockReturnValue(jest.fn()),
      isLoaded: jest.fn().mockReturnValue(true),
    })),
    InterstitialVideoAd: jest.fn().mockImplementation(() => ({
      load: jest.fn().mockResolvedValue(undefined),
      show: jest.fn().mockResolvedValue(undefined),
      start: jest.fn().mockResolvedValue(undefined),
      destroy: jest.fn(),
      addAdEventListener: jest.fn().mockReturnValue(jest.fn()),
      isLoaded: jest.fn().mockReturnValue(true),
    })),
    isNativeModuleAvailable: jest.fn().mockReturnValue(true),
    isNativeViewAvailable: jest.fn().mockReturnValue(true),
    normalizeAdError: (e) => e,
  };
});