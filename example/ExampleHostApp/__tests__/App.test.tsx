/**
 * @format
 */

import 'react-native';
import React from 'react';
import renderer from 'react-test-renderer';
import App from '../App';

jest.mock('../../../src', () => {
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
    BannerAd: (props: any) => mockReact.createElement(View, props),
    InterstitialAd: MockInterstitialAd,
    RewardedAd: MockRewardedAd,
    isNativeModuleAvailable: jest.fn().mockReturnValue(false),
  };
});

it('renders the beginner-friendly host screen', () => {
  const tree = renderer.create(<App />).toJSON();
  expect(tree).toBeTruthy();
});
