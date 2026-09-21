import { FullScreenAd } from './FullScreenAd';
import { NativeModuleNames } from './nativeBridge';
import type { InterstitialVideoAdEventMap, InterstitialVideoAdOptions } from './types';

/** Full-screen video interstitial. */
export class InterstitialVideoAd extends FullScreenAd<
  InterstitialVideoAdEventMap,
  InterstitialVideoAdOptions
> {
  constructor(adUnitId: string, options?: InterstitialVideoAdOptions) {
    super(
      adUnitId,
      NativeModuleNames.interstitialVideo,
      'interstitial video ads',
      'interstitial_video',
      options,
    );
  }

  protected override extraEventNames(): readonly string[] {
    return ['onVideoCompleted', 'onVideoSkipped'];
  }

  protected override handleExtraEvent(eventName: string, _payload: Record<string, unknown>): boolean {
    switch (eventName) {
      case 'onVideoCompleted':
        this.emitTyped('completed', undefined);
        return true;
      case 'onVideoSkipped':
        this.emitTyped('skipped', undefined);
        return true;
      default:
        return false;
    }
  }
}
