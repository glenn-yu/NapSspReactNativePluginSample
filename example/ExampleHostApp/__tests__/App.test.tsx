/**
 * @format
 */

import 'react-native';
import React from 'react';
import {Text} from 'react-native';
import renderer, {act} from 'react-test-renderer';
import App from '../App';

jest.mock('react-native/Libraries/BatchedBridge/NativeModules', () => ({
  UIManager: { RCTView: () => ({}) },
  PlatformConstants: { osVersion: '13.0', platform: 'iOS' },
}));

jest.mock('../../../src', () => ({
  ...jest.requireActual('../../../src'),
  TEST_CONFIG: {
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
  },
}));

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
});