import { NativeModules, Platform } from 'react-native';

export const NativeModuleNames = {
  napSsp: ['NapSspModule'],
  interstitial: ['InterstitialModule', 'NapSspInterstitial'],
  rewarded: ['RewardedModule', 'NapSspRewardedModule', 'NapSspRewarded'],
  banner: ['BannerView', 'NapSspBannerView'],
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

export async function callNative<T>(
  moduleNameOrNames: string | readonly string[],
  method: string,
  args: readonly unknown[] = [],
  feature?: string,
): Promise<T> {
  const resolvedFeature = feature ?? asModuleNames(moduleNameOrNames).join(' / ');
  const module = getNativeModuleFromNames<Record<string, (...methodArgs: any[]) => Promise<T> | T>>(
    asModuleNames(moduleNameOrNames),
  );

  const fn = module?.[method];

  if (typeof fn !== 'function') {
    throw createNativeModuleMissingError(resolvedFeature, moduleNameOrNames);
  }

  return await Promise.resolve(fn(...args));
}
