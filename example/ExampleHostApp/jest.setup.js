jest.mock('react-native/Libraries/BatchedBridge/NativeModules', () => ({
  __fbBatchedBridgeConfig: {},
  UIManager: {RCTView: {}, setJSResponder: jest.fn()},
  PlatformConstants: {osVersion: '13.0', platform: 'iOS'},
  StatusBarManager: {
    setStyle: jest.fn(),
    setHidden: jest.fn(),
    setNetworkActivityIndicatorVisible: jest.fn(),
    addListener: jest.fn(),
    removeListeners: jest.fn(),
  },
}));

jest.mock('react-native/Libraries/TurboModule/TurboModuleRegistry', () => {
  const platformConstants = {
    getConstants: jest.fn(() => ({
      forceTouchAvailable: false,
      interfaceIdiom: 'phone',
      isTesting: true,
      osVersion: '13.0',
      reactNativeVersion: {major: 0, minor: 72, patch: 0, prerelease: null},
      systemName: 'iOS',
    })),
  };
  const statusBarManager = {
    getConstants: jest.fn(() => ({HEIGHT: 47, DEFAULT_BACKGROUND_COLOR: 0})),
    getHeight: jest.fn((callback) => callback({height: 47})),
    setStyle: jest.fn(),
    setHidden: jest.fn(),
    setNetworkActivityIndicatorVisible: jest.fn(),
    addListener: jest.fn(),
    removeListeners: jest.fn(),
  };

  const getModule = (name) => {
    switch (name) {
      case 'PlatformConstants':
        return platformConstants;
      case 'StatusBarManager':
        return statusBarManager;
      default:
        return undefined;
    }
  };

  return {
    get: jest.fn((name) => getModule(name)),
    getEnforcing: jest.fn((name) => getModule(name)),
  };
});

jest.mock('react-native/Libraries/Utilities/Dimensions', () => ({
  get: jest.fn(() => ({width: 390, height: 844, scale: 3, fontScale: 1})),
  addEventListener: jest.fn(() => ({remove: jest.fn()})),
  removeEventListener: jest.fn(),
}));

jest.mock('react-native/Libraries/Utilities/PixelRatio', () => ({
  __esModule: true,
  default: {
    get: jest.fn(() => 3),
    getFontScale: jest.fn(() => 1),
    getPixelSizeForLayoutSize: jest.fn((size) => size * 3),
    roundToNearestPixel: jest.fn((size) => size),
  },
  get: jest.fn(() => 3),
  getFontScale: jest.fn(() => 1),
  getPixelSizeForLayoutSize: jest.fn((size) => size * 3),
  roundToNearestPixel: jest.fn((size) => size),
}));

jest.mock('react-native/Libraries/EventEmitter/NativeEventEmitter');

jest.mock('../../src', () => {
  const mockReact = require('react');
  const {View} = require('react-native');

  class MockInterstitialAd {
    async load() {}
    async show() {}
    isLoaded() {
      return true;
    }
    addAdEventListener() {
      return () => {};
    }
  }

  class MockInterstitialVideoAd {
    async load() {}
    async show() {}
    isLoaded() {
      return true;
    }
    addAdEventListener() {
      return () => {};
    }
  }

  class MockRewardedAd {
    async load() {}
    async show() {}
    addAdEventListener() {
      return () => {};
    }
  }

  const mockInlineAd = (props) => mockReact.createElement(View, props);

  return {
    NapSspAd: {
      initialize: jest.fn().mockResolvedValue(undefined),
      getStatus: jest.fn().mockResolvedValue({initialized: true, placeholderMode: true}),
    },
    BannerAd: mockInlineAd,
    NativeAd: mockInlineAd,
    VideoAd: mockInlineAd,
    InterstitialAd: MockInterstitialAd,
    InterstitialVideoAd: MockInterstitialVideoAd,
    RewardedAd: MockRewardedAd,
    isNativeModuleAvailable: jest.fn().mockReturnValue(false),
    isNativeViewAvailable: jest.fn().mockReturnValue(false),
  };
});
