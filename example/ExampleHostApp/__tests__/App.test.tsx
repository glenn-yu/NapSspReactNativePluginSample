/**
 * @format
 */

import 'react-native';
import React from 'react';
import {Text} from 'react-native';
import renderer, {act} from 'react-test-renderer';
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

  class MockInterstitialVideoAd {
    async load() {}
    async show() {}
    addAdEventListener() {
      return () => {};
    }
  }

  const mockView = (props: any) => mockReact.createElement(View, props);

  return {
    NapSspAd: {
      initialize: jest.fn().mockResolvedValue(undefined),
      getStatus: jest.fn().mockResolvedValue({
        initialized: true,
        placeholderMode: true,
      }),
    },
    BannerAd: mockView,
    NativeAd: mockView,
    VideoAd: mockView,
    InterstitialAd: MockInterstitialAd,
    RewardedAd: MockRewardedAd,
    InterstitialVideoAd: MockInterstitialVideoAd,
    isNativeModuleAvailable: jest.fn().mockReturnValue(false),
  };
});

const mockedSrc = jest.requireMock('../../../src');

it('renders the beginner-friendly host screen and initializes every major ad flow', async () => {
  let tree: ReturnType<typeof renderer.create>;

  await act(async () => {
    tree = renderer.create(<App />);
    await Promise.resolve();
  });

  const textNodes = tree!.root.findAllByType(Text).map((node) => {
    const children = node.props.children;
    return Array.isArray(children) ? children.join('') : String(children);
  });

  expect(textNodes).toEqual(
    expect.arrayContaining([
      'Nap SSP 광고 테스트',
      '0. 초기화 상태',
      '1. 배너 광고 (Banner)',
      '2. 네이티브 광고 (Native Ad)',
      '3. 동영상 광고 뷰 (Video Ad)',
      '4. 전면 광고 (Interstitial)',
      '5. 전면 동영상 광고 (Interstitial Video)',
      '6. 보상형 광고 (Rewarded)',
    ]),
  );

  expect(mockedSrc.NapSspAd.initialize).toHaveBeenCalledWith(
    expect.objectContaining({
      mediaKey: 'TEST_MEDIA_KEY',
      adUnitIds: [
        'TEST_BANNER',
        'TEST_NATIVE_AD',
        'TEST_VIDEO_AD',
        'TEST_INTERSTITIAL',
        'TEST_INTERSTITIAL_VIDEO',
        'TEST_REWARDED',
      ],
      logLevel: 'debug',
    }),
  );

  expect(mockedSrc.NapSspAd.getStatus).toHaveBeenCalledTimes(1);
  expect(mockedSrc.isNativeModuleAvailable).toHaveBeenCalledWith([
    'NapSspModule',
    'NapSspBannerView',
    'NapSspNativeAdView',
    'NapSspVideoAdView',
    'NapSspInterstitialVideo',
  ]);
});
