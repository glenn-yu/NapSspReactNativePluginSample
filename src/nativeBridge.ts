import { NativeModules, Platform, UIManager } from 'react-native';

export const NativeModuleNames = {
  napSsp: ['NapSspModule'],
  interstitial: ['NapSspInterstitial'],
  rewarded: ['NapSspRewarded'],
  banner: ['NapSspBannerView'],
  nativeAd: ['NapSspNativeAdView'],
  videoAd: ['NapSspVideoAdView'],
  interstitialVideo: ['NapSspInterstitialVideo'],
} as const;

export function getNativeModule<T extends Record<string, any>>(moduleName: string): T | undefined {
  const module = NativeModules[moduleName] as T | undefined;
  if (!module || Object.keys(module).length === 0) {
    return undefined;
  }
  return module;
}

export function getNativeModuleFromNames<T extends Record<string, any>>(moduleNames: readonly string[]): T | undefined {
  for (const moduleName of moduleNames) {
    const module = getNativeModule<T>(moduleName);
    if (module) {
      return module;
    }
  }
  return undefined;
}

function asModuleNames(moduleNameOrNames: string | readonly string[]): readonly string[] {
  return typeof moduleNameOrNames === 'string' ? [moduleNameOrNames] : moduleNameOrNames;
}

export function isNativeModuleAvailable(moduleNameOrNames: string | readonly string[]): boolean {
  return getNativeModuleFromNames(asModuleNames(moduleNameOrNames)) !== undefined;
}

export function isNativeViewAvailable(componentNameOrNames: string | readonly string[]): boolean {
  return asModuleNames(componentNameOrNames).some((componentName) => {
    try {
      return !!UIManager.getViewManagerConfig?.(componentName);
    } catch {
      return false;
    }
  });
}

export function createNativeModuleMissingError(
  feature: string,
  moduleNameOrNames: string | readonly string[],
): Error {
  const moduleLabel = Array.isArray(moduleNameOrNames)
    ? moduleNameOrNames.join(' / ')
    : moduleNameOrNames;

  return new Error(
    `NapSsp ${feature} is not linked. Expected native module "${moduleLabel}" to be registered on ${Platform.OS}.`,
  );
}

