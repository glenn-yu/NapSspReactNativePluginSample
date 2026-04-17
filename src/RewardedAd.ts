import { TypedEventEmitter } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { RewardItem, RewardedAdEventMap } from './types';

interface NativeRewardedModule {
  load?: (adUnitId: string) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void;
}

export type RewardedEventName = keyof RewardedAdEventMap | 'onRewarded';

export class RewardedAd {
  public readonly adUnitId: string;

  private _loaded = false;
  private readonly emitter = new TypedEventEmitter<RewardedAdEventMap>();

  constructor(adUnitId: string) {
    if (!adUnitId || adUnitId.trim().length === 0) {
      throw new Error('RewardedAd requires a non-empty adUnitId.');
    }

    this.adUnitId = adUnitId;
  }

  async load(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    if (!nativeModule?.load) {
      throw createNativeModuleMissingError('rewarded ads', NativeModuleNames.rewarded);
    }

    try {
      await nativeModule.load(this.adUnitId);
      this._loaded = true;
      this.emitter.emit('loaded', undefined);
    } catch (error) {
      const adError = normalizeAdError(error, 'rewarded_load_failed');
      this.emitter.emit('loadFailed', adError);
      throw adError;
    }
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
    this.emitter.emit('opened', undefined);
  }

  addAdEventListener(event: 'onRewarded', handler: (reward: RewardItem) => void): () => void;
  addAdEventListener<K extends keyof RewardedAdEventMap>(
    event: K,
    handler: (payload: RewardedAdEventMap[K]) => void,
  ): () => void;
  addAdEventListener(event: RewardedEventName, handler: (payload: unknown) => void): () => void {
    const normalizedEvent = event === 'onRewarded' ? 'rewarded' : event;
    return this.emitter.on(normalizedEvent as keyof RewardedAdEventMap, handler as never);
  }

  isLoaded(): boolean {
    return this._loaded;
  }

  destroy(): void {
    this._loaded = false;
    this.emitter.removeAllListeners();

    const nativeModule = getNativeModuleFromNames<NativeRewardedModule>(NativeModuleNames.rewarded);
    nativeModule?.destroy?.(this.adUnitId);
  }
}
