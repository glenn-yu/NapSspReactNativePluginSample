import { TypedEventEmitter, globalEvents } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames } from './nativeBridge';
import { normalizeAdError } from './errors';
import type { InterstitialAdEventMap, RewardPayload } from './types';

export interface NativeFullScreenAdModule<TOptions> {
  load?: (adUnitId: string, options?: TOptions) => Promise<void>;
  start?: (adUnitId: string, options?: TOptions) => Promise<void>;
  show?: (adUnitId: string) => Promise<void>;
  isLoaded?: (adUnitId: string) => Promise<boolean>;
  isLoading?: (adUnitId: string) => Promise<boolean>;
  cancelLoad?: (adUnitId: string) => Promise<void>;
  destroy?: (adUnitId: string) => void | Promise<void>;
}

/**
 * Shared behaviour for the three full-screen formats.
 *
 * Native events are broadcast per module, so every instance filters on its own `adUnitId`.
 */
export abstract class FullScreenAd<
  TEventMap extends InterstitialAdEventMap,
  TOptions,
> {
  public readonly adUnitId: string;
  protected readonly options?: TOptions;

  private _loaded = false;
  private _destroyed = false;
  private readonly emitter = new TypedEventEmitter<TEventMap>();
  private readonly subscriptions: Array<() => void> = [];

  protected constructor(
    adUnitId: string,
    private readonly moduleNames: readonly string[],
    private readonly featureLabel: string,
    private readonly errorPrefix: string,
    options?: TOptions,
  ) {
    if (typeof adUnitId !== 'string' || adUnitId.trim().length === 0) {
      throw new Error(`${featureLabel} requires a non-empty adUnitId.`);
    }

    this.adUnitId = adUnitId.trim();
    this.options = options;
    this.subscribe();
  }

  /** Native events this format listens to. Subclasses add their own. */
  protected extraEventNames(): readonly string[] {
    return [];
  }

  /** Handles a format-specific native event. Return `true` when consumed. */
  protected handleExtraEvent(_eventName: string, _payload: Record<string, unknown>): boolean {
    return false;
  }

  protected emitTyped<K extends keyof TEventMap>(event: K, payload: TEventMap[K]): void {
    this.emitter.emit(event, payload);
  }

  protected toRewardPayload(payload: Record<string, unknown>): RewardPayload {
    return {
      transactionId: typeof payload.transactionId === 'string' ? payload.transactionId : undefined,
      type: typeof payload.type === 'string' ? payload.type : 'reward',
      amount: typeof payload.amount === 'number' ? payload.amount : 1,
    };
  }

  private subscribe(): void {
    const baseEvents = [
      'onAdLoaded',
      'onAdFailedToLoad',
      'onAdOpened',
      'onAdClosed',
      'onAdClicked',
      'onAdImpression',
    ] as const;

    const eventNames = [...baseEvents, ...this.extraEventNames()];

    eventNames.forEach((eventName) => {
      const cleanup = globalEvents.addListener(eventName, (raw: unknown) => {
        const payload = (raw ?? {}) as Record<string, unknown>;
        if (payload.adUnitId !== this.adUnitId) {
          return;
        }

        switch (eventName) {
          case 'onAdLoaded':
            this._loaded = true;
            this.emitter.emit('loaded' as keyof TEventMap, undefined as never);
            return;
          case 'onAdFailedToLoad':
            // A show-phase failure does not invalidate a still-loaded ad.
            if (payload.phase !== 'show') {
              this._loaded = false;
            }
            this.emitter.emit('loadFailed' as keyof TEventMap, normalizeAdError(payload) as never);
            return;
          case 'onAdOpened':
            this.emitter.emit('opened' as keyof TEventMap, undefined as never);
            return;
          case 'onAdClosed':
            this._loaded = false;
            this.emitter.emit('closed' as keyof TEventMap, undefined as never);
            return;
          case 'onAdClicked':
            this.emitter.emit('clicked' as keyof TEventMap, undefined as never);
            return;
          case 'onAdImpression':
            this.emitter.emit('impression' as keyof TEventMap, undefined as never);
            return;
          default:
            this.handleExtraEvent(eventName, payload);
        }
      });
      this.subscriptions.push(cleanup);
    });
  }

  private module(): NativeFullScreenAdModule<TOptions> {
    const nativeModule = getNativeModuleFromNames<NativeFullScreenAdModule<TOptions>>(this.moduleNames);
    if (!nativeModule) {
      throw createNativeModuleMissingError(this.featureLabel, this.moduleNames);
    }
    return nativeModule;
  }

  private assertUsable(): void {
    if (this._destroyed) {
      throw new Error(`${this.featureLabel} "${this.adUnitId}" has been destroyed.`);
    }
  }

  /** Requests an ad. Resolves once the SDK reports a fill, rejects on the final waterfall failure. */
  async load(): Promise<void> {
    this.assertUsable();
    const nativeModule = this.module();
    if (!nativeModule.load) {
      throw createNativeModuleMissingError(this.featureLabel, this.moduleNames);
    }

    try {
      await nativeModule.load(this.adUnitId, this.options);
    } catch (error) {
      throw normalizeAdError(error, `${this.errorPrefix}_load_failed`);
    }
  }

  /** Loads and presents in one call. */
  async start(): Promise<void> {
    this.assertUsable();
    const nativeModule = this.module();
    if (nativeModule.start) {
      try {
        await nativeModule.start(this.adUnitId, this.options);
        return;
      } catch (error) {
        throw normalizeAdError(error, `${this.errorPrefix}_start_failed`);
      }
    }

    await this.load();
    await this.show();
  }

  /** Presents a loaded ad. Rejects when nothing is ready. */
  async show(): Promise<void> {
    this.assertUsable();
    const nativeModule = this.module();
    if (!nativeModule.show) {
      throw createNativeModuleMissingError(this.featureLabel, this.moduleNames);
    }

    try {
      await nativeModule.show(this.adUnitId);
    } catch (error) {
      throw normalizeAdError(error, `${this.errorPrefix}_show_failed`);
    }
  }

  /**
   * Last known readiness, updated from the native events. Synchronous and therefore safe to call
   * during render; use {@link isReady} when the SDK's own state matters.
   */
  isLoaded(): boolean {
    return this._loaded;
  }

  /** Asks the SDK whether an ad is ready to present right now. */
  async isReady(): Promise<boolean> {
    if (this._destroyed) {
      return false;
    }
    const ready = await this.module().isLoaded?.(this.adUnitId);
    if (typeof ready === 'boolean') {
      this._loaded = ready;
      return ready;
    }
    return this._loaded;
  }

  /** Asks the SDK whether a load is still in flight. */
  async isLoading(): Promise<boolean> {
    if (this._destroyed) {
      return false;
    }
    return (await this.module().isLoading?.(this.adUnitId)) ?? false;
  }

  /** Cancels an in-flight load. A showing ad is left alone. */
  async cancelLoad(): Promise<void> {
    if (this._destroyed) {
      return;
    }
    const nativeModule = getNativeModuleFromNames<NativeFullScreenAdModule<TOptions>>(this.moduleNames);
    try {
      await nativeModule?.cancelLoad?.(this.adUnitId);
    } catch {
      // A cancel that races the load completing is not an error worth surfacing.
    }
    this._loaded = false;
  }

  addAdEventListener<K extends keyof TEventMap>(
    event: K,
    handler: (payload: TEventMap[K]) => void,
  ): () => void {
    return this.emitter.on(event, handler);
  }

  /** Releases the native ad and every listener. The instance cannot be reused afterwards. */
  destroy(): void {
    if (this._destroyed) {
      return;
    }
    this._destroyed = true;
    this._loaded = false;
    this.emitter.removeAllListeners();
    this.subscriptions.forEach((cleanup) => cleanup());
    this.subscriptions.length = 0;

    const nativeModule = getNativeModuleFromNames<NativeFullScreenAdModule<TOptions>>(this.moduleNames);
    void Promise.resolve(nativeModule?.destroy?.(this.adUnitId)).catch(() => {
      // Teardown is best-effort — the JS side is already detached.
    });
  }
}
