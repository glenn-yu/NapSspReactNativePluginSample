import { TypedEventEmitter, globalEvents } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { RewardedAdEventMap, RewardedAdOptions, RewardPayload } from './types';

interface NativeRewardedModule {
  load?: (adUnitId: string, options?: RewardedAdOptions) => Promise<void>;
  start?: (adUnitId: string, options?: RewardedAdOptions) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  cancelLoad?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void;
}

export type RewardedEventName = keyof RewardedAdEventMap | 'onRewarded';

export class RewardedAd {
  public readonly adUnitId: string;
  private readonly options?: RewardedAdOptions;

  private _loaded = false;
  private readonly emitter = new TypedEventEmitter<RewardedAdEventMap>();
  private readonly subscriptions: Array<() => void> = [];

  constructor(adUnitId: string, options?: RewardedAdOptions) {
    if (!adUnitId || adUnitId.trim().length === 0) {
      throw new Error('RewardedAd requires a non-empty adUnitId.');
    }

    this.adUnitId = adUnitId;
    this.options = options;
    this.setupEventListeners();
  }

  private setupEventListeners(): void {
    const events = [
      'onAdLoaded',
      'onAdFailedToLoad',
      'onAdOpened',
      'onAdClosed',
      'onAdClicked',
      'onAdImpression',
      'onRewarded',
      'onVideoCompleted',
      'onVideoSkipped',
    ];

    events.forEach((eventName) => {
      const cleanup = globalEvents.addListener(eventName, (payload: any) => {
        if (payload?.adUnitId !== this.adUnitId) {
          return;
        }

        switch (eventName) {
          case 'onAdLoaded':
            this._loaded = true;
            this.emitter.emit('loaded', undefined);
            break;
          case 'onAdFailedToLoad':
            this._loaded = false;
            this.emitter.emit('loadFailed', normalizeAdError(payload));
            break;
          case 'onAdOpened':
            this.emitter.emit('opened', undefined);
            break;
          case 'onAdClosed':
            this._loaded = false;
            this.emitter.emit('closed', undefined);
            break;
          case 'onAdClicked':
            this.emitter.emit('clicked', undefined);
            break;
          case 'onAdImpression':
            this.emitter.emit('impression', undefined);
            break;
          case 'onRewarded':
            this.emitter.emit('rewarded', {
              type: payload?.type ?? 'reward',
              amount: payload?.amount ?? 1,
            });
            break;
          case 'onVideoCompleted':
            this.emitter.emit('completed', undefined);
            break;
          case 'onVideoSkipped':
            this.emitter.emit('skipped', undefined);
            break;
        }
      });
      this.subscriptions.push(cleanup);
    });
  }

  async load(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    if (!nativeModule?.load) {
      throw createNativeModuleMissingError('rewarded ads', NativeModuleNames.rewarded);
    }

    try {
      await nativeModule.load(this.adUnitId, this.options);
      this._loaded = true;
    } catch (error) {
      const adError = normalizeAdError(error, 'rewarded_load_failed');
      this.emitter.emit('loadFailed', adError);
      throw adError;
    }
  }

  async start(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    if (nativeModule?.start) {
      try {
        await nativeModule.start(this.adUnitId, this.options);
        this._loaded = false;
        return;
      } catch (error) {
        const adError = normalizeAdError(error, 'rewarded_start_failed');
        this.emitter.emit('loadFailed', adError);
        throw adError;
      }
    }

    await this.load();
    await this.show();
  }

  async show(): Promise<void> {
    if (!this._loaded) {
      throw new Error(`Rewarded ad "${this.adUnitId}" has not been loaded.`);
    }

    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    if (!nativeModule?.show) {
      throw createNativeModuleMissingError('rewarded ads', NativeModuleNames.rewarded);
    }

    await nativeModule.show(this.adUnitId);
    this._loaded = false;
  }

  addAdEventListener(event: 'onRewarded', handler: (payload: RewardPayload) => void): () => void;
  addAdEventListener<K extends keyof RewardedAdEventMap>(
    event: K,
    handler: (payload: RewardedAdEventMap[K]) => void,
  ): () => void;
  addAdEventListener(event: RewardedEventName, handler: (...args: any[]) => void): () => void {
    const normalizedEvent = event === 'onRewarded' ? 'rewarded' : event;
    return this.emitter.on(normalizedEvent as keyof RewardedAdEventMap, handler as never);
  }

  isLoaded(): boolean {
    return this._loaded;
  }

  async cancelLoad(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    if (nativeModule?.cancelLoad) {
      await nativeModule.cancelLoad(this.adUnitId);
    }
  }

  destroy(): void {
    this._loaded = false;
    this.emitter.removeAllListeners();
    this.subscriptions.forEach((cleanup) => cleanup());
    this.subscriptions.length = 0;

    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    nativeModule?.destroy?.(this.adUnitId);
  }
}
