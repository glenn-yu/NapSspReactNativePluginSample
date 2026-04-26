import { TypedEventEmitter, globalEvents } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { InterstitialVideoAdEventMap, InterstitialVideoAdOptions } from './types';

interface NativeInterstitialVideoModule {
  load?: (adUnitId: string, options?: InterstitialVideoAdOptions) => Promise<void>;
  start?: (adUnitId: string, options?: InterstitialVideoAdOptions) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void;
}

export class InterstitialVideoAd {
  public readonly adUnitId: string;
  private readonly options?: InterstitialVideoAdOptions;

  private _loaded = false;
  private readonly emitter = new TypedEventEmitter<InterstitialVideoAdEventMap>();
  private readonly subscriptions: Array<() => void> = [];

  constructor(adUnitId: string, options?: InterstitialVideoAdOptions) {
    if (!adUnitId || adUnitId.trim().length === 0) {
      throw new Error('InterstitialVideoAd requires a non-empty adUnitId.');
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
      'onVideoCompleted',
      'onVideoSkipped'
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
    const nativeModule = getNativeModuleFromNames<NativeInterstitialVideoModule>(NativeModuleNames.interstitialVideo);
    if (!nativeModule?.load) {
      throw createNativeModuleMissingError('interstitial video ads', NativeModuleNames.interstitialVideo);
    }

    try {
      await nativeModule.load(this.adUnitId, this.options);
    } catch (error) {
      const adError = normalizeAdError(error, 'interstitial_video_load_failed');
      this.emitter.emit('loadFailed', adError);
      throw adError;
    }
  }

  async start(): Promise<void> {
    const nativeModule = getNativeModuleFromNames<NativeInterstitialVideoModule>(NativeModuleNames.interstitialVideo);
    if (nativeModule?.start) {
      try {
        await nativeModule.start(this.adUnitId, this.options);
        this._loaded = false;
        return;
      } catch (error) {
        const adError = normalizeAdError(error, 'interstitial_video_start_failed');
        this.emitter.emit('loadFailed', adError);
        throw adError;
      }
    }

    // Fallback to load + show if start is not available on native side (e.g. iOS)
    await this.load();
    await this.show();
  }

  async show(): Promise<void> {
    if (!this._loaded) {
      throw new Error(`Interstitial video ad "${this.adUnitId}" has not been loaded.`);
    }

    const nativeModule = getNativeModuleFromNames<NativeInterstitialVideoModule>(NativeModuleNames.interstitialVideo);
    if (!nativeModule?.show) {
      throw createNativeModuleMissingError('interstitial video ads', NativeModuleNames.interstitialVideo);
    }

    await nativeModule.show(this.adUnitId);
    this._loaded = false;
  }

  isLoaded(): boolean {
    return this._loaded;
  }

  addAdEventListener<K extends keyof InterstitialVideoAdEventMap>(
    event: K,
    handler: (payload: InterstitialVideoAdEventMap[K]) => void,
  ): () => void {
    return this.emitter.on(event, handler);
  }

  destroy(): void {
    this._loaded = false;
    this.emitter.removeAllListeners();
    this.subscriptions.forEach((cleanup) => cleanup());
    this.subscriptions.length = 0;

    const nativeModule = getNativeModuleFromNames<NativeInterstitialVideoModule>(NativeModuleNames.interstitialVideo);
    nativeModule?.destroy?.(this.adUnitId);
  }
}
