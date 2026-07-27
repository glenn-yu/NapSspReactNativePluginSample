import { TypedEventEmitter, globalEvents } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { InterstitialAdEventMap, InterstitialAdOptions } from './types';

interface NativeInterstitialModule {
  load?: (adUnitId: string, options?: InterstitialAdOptions) => Promise<void>;
  start?: (adUnitId: string, options?: InterstitialAdOptions) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  cancelLoad?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void;
}

export class InterstitialAd {
  public readonly adUnitId: string;
  private readonly options?: InterstitialAdOptions;

  private _loaded = false;
  private readonly emitter = new TypedEventEmitter<InterstitialAdEventMap>();
  private readonly subscriptions: Array<() => void> = [];

  constructor(adUnitId: string, options?: InterstitialAdOptions) {
    if (!adUnitId || adUnitId.trim().length === 0) {
      throw new Error('InterstitialAd requires a non-empty adUnitId.');
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
        }
      });
      this.subscriptions.push(cleanup);
    });
  }

  async load(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    if (!nativeModule?.load) {
      throw createNativeModuleMissingError('interstitial ads', NativeModuleNames.interstitial);
    }

    try {
      await nativeModule.load(this.adUnitId, this.options);
      this._loaded = true;
    } catch (error) {
      const adError = normalizeAdError(error, 'interstitial_load_failed');
      this.emitter.emit('loadFailed', adError);
      throw adError;
    }
  }

  async start(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    if (nativeModule?.start) {
      try {
        await nativeModule.start(this.adUnitId, this.options);
        this._loaded = false;
        return;
      } catch (error) {
        const adError = normalizeAdError(error, 'interstitial_start_failed');
        this.emitter.emit('loadFailed', adError);
        throw adError;
      }
    }

    await this.load();
    await this.show();
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
  }

  isLoaded(): boolean {
    return this._loaded;
  }

  async cancelLoad(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    if (nativeModule?.cancelLoad) {
      await nativeModule.cancelLoad(this.adUnitId);
    }
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
    this.subscriptions.forEach((cleanup) => cleanup());
    this.subscriptions.length = 0;

    const nativeModule = getNativeModuleFromNames<NativeInterstitialModule>(NativeModuleNames.interstitial);
    nativeModule?.destroy?.(this.adUnitId);
  }
}
