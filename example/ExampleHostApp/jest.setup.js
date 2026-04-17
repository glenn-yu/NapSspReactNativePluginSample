jest.mock('../../src', () => {
  const mockReact = require('react');
  const {View} = require('react-native');

  class MockInterstitialAd {
    async load() {}
    async show() {}
    addAdEventListener() {
      return () => {};
    }
  }

  class MockInterstitialVideoAd {
    async load() {}
    async show() {}
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
  };
});
