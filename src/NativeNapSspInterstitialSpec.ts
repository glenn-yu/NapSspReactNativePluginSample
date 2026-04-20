/**
 * TurboModule spec for NapSspInterstitial / NapSspRewarded / NapSspInterstitialVideo.
 *
 * STATUS: Spec defined. Native implementation pending (Phase 4 / separate sprint).
 */

import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface FullscreenAdSpec extends TurboModule {
  load(adUnitId: string, options: Object | null): Promise<void>;
  show(adUnitId: string): Promise<void>;
  isLoaded(adUnitId: string): Promise<boolean>;
  destroy(adUnitId: string): void;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export const NapSspInterstitialSpec =
  TurboModuleRegistry.getEnforcing<FullscreenAdSpec>('NapSspInterstitialModule');

export const NapSspRewardedSpec =
  TurboModuleRegistry.getEnforcing<FullscreenAdSpec>('NapSspRewardedModule');

export const NapSspInterstitialVideoSpec =
  TurboModuleRegistry.getEnforcing<FullscreenAdSpec>('NapSspInterstitialVideoModule');
