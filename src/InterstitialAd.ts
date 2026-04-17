import { TypedEventEmitter } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { InterstitialAdEventMap } from './types';

interface NativeInterstitialModule {
  load?: (adUnitId: string) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void;
}

export class InterstitialAd {
  public readonly adUnitId: string;

  private _loaded = false;
  private readonly emitter = new TypedEventEmitter<InterstitialAdEventMap>();

  constructor(adUnitId: string) {
    if (!adUnitId || adUnitId.trim().length === 0) {
      throw new Error('InterstitialAd requires a non-empty adUnitId.');
    }

    this.adUnitId = adUnitId;
  }

  async load(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    if (!nativeModule?.load) {
      throw createNativeModuleMissingError('interstitial ads', NativeModuleNames.interstitial);
    }

    try {
      await nativeModule.load(this.adUnitId);
      this._loaded = true;
      this.emitter.emit('loaded', undefined);
    } catch (error) {
      const adError = normalizeAdError(error, 'interstitial_load_failed');
      this.emitter.emit('loadFailed', adError);
      throw adError;
    }
  }

  async show(): Promise<void> {
    if (!this._loaded) {
      throw new Error(`Interstitial ad "${this.adUnitId}" has not been loaded.`);
    }

    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    if (!nativeModule?.show) {
      throw createNativeModuleMissingError('interstitial ads', NativeModuleNames.interstitial);
    }

    await nativeModule.show(this.adUnitId);
    this._loaded = false;
    this.emitter.emit('opened', undefined);
    this.emitter.emit('closed', undefined);
  }

  isLoaded(): boolean {
    return this._loaded;
  }

  addAdEventListener<K extends keyof InterstitialAdEventMap>(
    event: K,
    handler: (payload: InterstitialAdEventMap[K]) => void,
  ): () => void {
    return this.emitter.on(event, handler);
  }

  destroy(): void {
    this._loaded = false;
    this.emitter.removeAllListeners();

    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    nativeModule?.destroy?.(this.adUnitId);
  }
}
