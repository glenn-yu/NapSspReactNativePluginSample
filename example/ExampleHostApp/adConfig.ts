import { Platform } from 'react-native';

/**
 * nap mx test inventory.
 *
 * Replace every value with the numeric media key and ad unit IDs issued for your own app on the
 * partner site (https://publisher.admixer.co.kr). One media key per app.
 *
 * These are ad unit identifiers, not credentials — but keep your production values in your own
 * config (or an env-driven file) rather than in a shared sample.
 */
export interface AdConfig {
  mediaKey: string;
  banner: string;
  nativeAd: string;
  video: string;
  interstitial: string;
  interstitialVideo: string;
  rewarded: string;
}

const ANDROID: AdConfig = {
  mediaKey: '10771',
  banner: '104701',
  nativeAd: '104588',
  video: '104591',
  interstitial: '104703',
  interstitialVideo: '104703',
  rewarded: '103722',
};

const IOS: AdConfig = {
  mediaKey: '10347',
  banner: '103790',
  nativeAd: '101626',
  video: '104711',
  interstitial: '104707',
  interstitialVideo: '103868',
  rewarded: '104710',
};

export const adConfig: AdConfig = Platform.OS === 'ios' ? IOS : ANDROID;

/** Every ad unit the app will request, which is what `initialize()` needs up front. */
export const allAdUnitIds: string[] = Array.from(
  new Set([
    adConfig.banner,
    adConfig.nativeAd,
    adConfig.video,
    adConfig.interstitial,
    adConfig.interstitialVideo,
    adConfig.rewarded,
  ]),
);
