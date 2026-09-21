import { FullScreenAd } from './FullScreenAd';
import { NativeModuleNames } from './nativeBridge';
import type { RewardedAdEventMap, RewardedAdOptions, RewardPayload } from './types';

/** Accepts the short event names plus the legacy `'onRewarded'` spelling. */
export type RewardedEventName = keyof RewardedAdEventMap | 'onRewarded';

/**
 * Rewarded video.
 *
 * Grant the reward from the `rewarded` event and reconcile it with your server using
 * {@link RewardPayload.transactionId}, which matches the S2S callback's `transaction_id`.
 */
export class RewardedAd extends FullScreenAd<RewardedAdEventMap, RewardedAdOptions> {
  constructor(adUnitId: string, options?: RewardedAdOptions) {
    super(adUnitId, NativeModuleNames.rewarded, 'rewarded ads', 'rewarded', options);
  }

  protected override extraEventNames(): readonly string[] {
    return ['onRewarded', 'onVideoCompleted', 'onVideoSkipped'];
  }

  protected override handleExtraEvent(eventName: string, payload: Record<string, unknown>): boolean {
    switch (eventName) {
      case 'onRewarded':
        this.emitTyped('rewarded', this.toRewardPayload(payload));
        return true;
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

  override addAdEventListener(
    event: 'onRewarded',
    handler: (payload: RewardPayload) => void,
  ): () => void;
  override addAdEventListener<K extends keyof RewardedAdEventMap>(
    event: K,
    handler: (payload: RewardedAdEventMap[K]) => void,
  ): () => void;
  override addAdEventListener(
    event: RewardedEventName,
    handler: (...args: never[]) => void,
  ): () => void {
    const normalized = event === 'onRewarded' ? 'rewarded' : event;
    return super.addAdEventListener(normalized as keyof RewardedAdEventMap, handler as never);
  }
}
