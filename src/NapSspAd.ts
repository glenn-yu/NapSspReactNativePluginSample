import { globalEvents } from './events';
import {
  createNativeModuleMissingError,
  getNativeModuleFromNames,
  NativeModuleNames,
} from './nativeBridge';
import type { LogLevel, NapSspConfig, NapSspStatus, PrivacyConsent } from './types';

interface NapSspNativeModule {
  initialize?: (config: NapSspConfig) => Promise<NapSspStatus>;
  setLogLevel?: (level: LogLevel) => Promise<void>;
  setPrivacyConsent?: (consent: PrivacyConsent) => Promise<void>;
  setCoppa?: (enabled: boolean) => Promise<void>;
  setTestMode?: (enabled: boolean) => Promise<boolean | void>;
  setTestDeviceIds?: (ids: string[]) => Promise<boolean | void>;
  getStatus?: () => Promise<NapSspStatus>;
  requestTrackingAuthorization?: () => Promise<string>;
}

function cloneConfig(config: NapSspConfig): NapSspConfig {
  return {
    ...config,
    adUnitIds: [...config.adUnitIds],
    mediations: config.mediations ? { ...config.mediations } : undefined,
    privacy: config.privacy ? { ...config.privacy } : undefined,
    testDeviceIds: config.testDeviceIds ? [...config.testDeviceIds] : undefined,
  };
}

function requireModule(): NapSspNativeModule {
  const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
  if (!nativeModule) {
    throw createNativeModuleMissingError('initialization', NativeModuleNames.napSsp);
  }
  return nativeModule;
}

/**
 * Entry point for the nap mx SDK.
 *
 * `initialize()` must resolve before any ad is requested; privacy and test settings declared on the
 * config are applied inside that call, before the SDK starts, which is what the network adapters
 * require.
 */
class NapSspAd {
  private static _initialized = false;
  private static _config: NapSspConfig | undefined;

  /** Initializes the SDK. Call once, before requesting any ad. */
  static async initialize(config: NapSspConfig): Promise<NapSspStatus> {
    this.validateConfig(config);

    const nativeModule = requireModule();
    if (!nativeModule.initialize) {
      throw createNativeModuleMissingError('initialization', NativeModuleNames.napSsp);
    }

    // Bridge the native event emitters before the SDK can emit anything.
    globalEvents.setup(NativeModuleNames.napSsp[0]);
    globalEvents.setup(NativeModuleNames.interstitial[0]);
    globalEvents.setup(NativeModuleNames.rewarded[0]);
    globalEvents.setup(NativeModuleNames.interstitialVideo[0]);

    const status = await nativeModule.initialize(cloneConfig(config));
    this._initialized = true;
    this._config = cloneConfig(config);
    return status;
  }

  static async setLogLevel(level: LogLevel): Promise<void> {
    await requireModule().setLogLevel?.(level);
  }

  /**
   * Applies GDPR / CCPA / COPPA signals.
   *
   * Prefer passing `privacy` to {@link initialize} — networks that read consent only at start-up
   * (AppLovin, Unity Ads, Pangle) may not pick up a later change until the next app launch.
   */
  static async setPrivacyConsent(consent: PrivacyConsent): Promise<void> {
    if (!consent || typeof consent !== 'object') {
      throw new Error('NapSspAd.setPrivacyConsent requires a consent object.');
    }
    await requireModule().setPrivacyConsent?.(consent);
  }

  /** @deprecated Use {@link setPrivacyConsent} with `{ childDirected }`. */
  static async setCoppa(enabled: boolean): Promise<void> {
    await requireModule().setCoppa?.(enabled);
  }

  /**
   * Enables global test mode. **Android only** — resolves `false` on iOS, where test devices are
   * registered in each network's own dashboard.
   */
  static async setTestMode(enabled: boolean): Promise<boolean> {
    const result = await requireModule().setTestMode?.(enabled);
    return result !== false;
  }

  /**
   * Registers test device advertising IDs (GAID). **Android only** — resolves `false` on iOS.
   * These are sensitive identifiers: keep them out of logs and issue trackers.
   */
  static async setTestDeviceIds(ids: readonly string[]): Promise<boolean> {
    if (!Array.isArray(ids)) {
      throw new Error('NapSspAd.setTestDeviceIds requires an array of advertising IDs.');
    }
    const result = await requireModule().setTestDeviceIds?.([...ids]);
    return result !== false;
  }

  static isInitialized(): boolean {
    return this._initialized;
  }

  static getConfig(): NapSspConfig | undefined {
    return this._config ? cloneConfig(this._config) : undefined;
  }

  static async getStatus(): Promise<NapSspStatus> {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (nativeModule?.getStatus) {
      return await nativeModule.getStatus();
    }
    return { initialized: this._initialized };
  }

  /**
   * Presents the iOS App Tracking Transparency prompt and resolves the resulting status.
   * Resolves `'unavailable'` on Android and on iOS below 14.5.
   */
  static async requestTrackingAuthorization(): Promise<string> {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (nativeModule?.requestTrackingAuthorization) {
      return await nativeModule.requestTrackingAuthorization();
    }
    return 'unavailable';
  }

  private static validateConfig(config: NapSspConfig): void {
    if (!config || typeof config !== 'object') {
      throw new Error('NapSspAd.initialize requires a config object.');
    }

    if (typeof config.mediaKey !== 'string' || config.mediaKey.trim().length === 0) {
      throw new Error('NapSspAd.initialize requires a non-empty mediaKey.');
    }

    if (!Array.isArray(config.adUnitIds) || config.adUnitIds.length === 0) {
      throw new Error('NapSspAd.initialize requires at least one adUnitId.');
    }

    const invalid = config.adUnitIds.filter(
      (id) => typeof id !== 'string' || !/^\d+$/.test(id.trim()),
    );
    if (invalid.length > 0) {
      throw new Error(
        `NapSspAd.initialize requires numeric adUnitIds from the nap mx partner site. Received: ${invalid.join(', ')}`,
      );
    }
  }
}

export default NapSspAd;
