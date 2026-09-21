import { FullScreenAd } from './FullScreenAd';
import { NativeModuleNames } from './nativeBridge';
import type { InterstitialAdEventMap, InterstitialAdOptions } from './types';

/**
 * Full-screen image/HTML interstitial.
 *
 * ```ts
 * const ad = new InterstitialAd('1234567');
 * ad.addAdEventListener('closed', () => ad.destroy());
 * await ad.load();
 * await ad.show();
 * ```
 */
export class InterstitialAd extends FullScreenAd<InterstitialAdEventMap, InterstitialAdOptions> {
  constructor(adUnitId: string, options?: InterstitialAdOptions) {
    super(adUnitId, NativeModuleNames.interstitial, 'interstitial ads', 'interstitial', options);
  }
}
