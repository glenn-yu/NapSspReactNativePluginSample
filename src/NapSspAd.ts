import { globalEvents } from './events';
import { createNativeModuleMissingError, getNativeModuleFromNames, NativeModuleNames } from './nativeBridge';
import type { LogLevel, NapSspConfig, NapSspStatus } from './types';

interface NapSspNativeModule {
  initialize?: (config: NapSspConfig) => Promise<void>;
  setLogLevel?: (level: LogLevel) => void;
  setCoppa?: (enabled: boolean) => void;
  getStatus?: () => Promise<NapSspStatus> | NapSspStatus;
  requestTrackingAuthorization?: () => Promise<string> | string;
}

function cloneConfig(config: NapSspConfig): NapSspConfig {
  return {
    ...config,
    adUnitIds: [...config.adUnitIds],
    mediations: config.mediations ? { ...config.mediations } : undefined,
  };
}

class NapSspAd {
  private static _initialized = false;
  private static _config: NapSspConfig | undefined;

  static async initialize(config: NapSspConfig): Promise<void> {
    this.validateConfig(config);

    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (!nativeModule?.initialize) {
      throw createNativeModuleMissingError('initialization', NativeModuleNames.napSsp);
    }

    // Setup global event bridges before calling native initialize
    globalEvents.setup(NativeModuleNames.napSsp[0]);
    globalEvents.setup(NativeModuleNames.interstitial[0]);
    globalEvents.setup(NativeModuleNames.rewarded[0]);
    globalEvents.setup(NativeModuleNames.interstitialVideo[0]);

    await nativeModule.initialize(config);
    this._initialized = true;
    this._config = cloneConfig(config);

    if (config.logLevel) {
      this.setLogLevel(config.logLevel);
    }

    if (typeof config.coppa === 'boolean') {
      this.setCoppa(config.coppa);
    }
  }

  static setLogLevel(level: LogLevel): void {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (typeof nativeModule?.setLogLevel === 'function') {
      nativeModule.setLogLevel(level);
    }
  }

  static setCoppa(enabled: boolean): void {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (typeof nativeModule?.setCoppa === 'function') {
      nativeModule.setCoppa(enabled);
    }
  }

  static isInitialized(): boolean {
    return this._initialized;
  }

  static getConfig(): NapSspConfig | undefined {
    return this._config ? cloneConfig(this._config) : undefined;
  }

  static async getStatus(): Promise<NapSspStatus> {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (typeof nativeModule?.getStatus === 'function') {
      return await Promise.resolve(nativeModule.getStatus());
    }

    return {
      initialized: this._initialized,
      placeholderMode: true,
      details: this._config ? { configuredAdUnitCount: this._config.adUnitIds.length } : undefined,
    };
  }

  static async requestTrackingAuthorization(): Promise<string> {
    const nativeModule = getNativeModuleFromNames<NapSspNativeModule>(NativeModuleNames.napSsp);
    if (typeof nativeModule?.requestTrackingAuthorization === 'function') {
      return await Promise.resolve(nativeModule.requestTrackingAuthorization());
    }

    return 'unavailable';
  }

  private static validateConfig(config: NapSspConfig): void {
    if (!config || typeof config !== 'object') {
      throw new Error('NapSspAd.initialize requires a config object.');
    }

    if (!config.mediaKey || config.mediaKey.trim().length === 0) {
      throw new Error('NapSspAd.initialize requires a non-empty mediaKey.');
    }

    if (!Array.isArray(config.adUnitIds) || config.adUnitIds.length === 0) {
      throw new Error('NapSspAd.initialize requires at least one adUnitId.');
    }
  }
}

export default NapSspAd;
