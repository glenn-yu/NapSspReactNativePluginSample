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

  class MockRewardedAd {
    async load() {}
    async show() {}
    addAdEventListener() {
      return () => {};
    }
  }

  return {
    NapSspAd: {
      initialize: jest.fn().mockResolvedValue(undefined),
    },
    BannerAd: (props) => mockReact.createElement(View, props),
    InterstitialAd: MockInterstitialAd,
    RewardedAd: MockRewardedAd,
    isNativeModuleAvailable: jest.fn().mockReturnValue(false),
  };
});
