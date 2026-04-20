/**
 * TurboModule spec for NapSspModule.
 *
 * This file is the JS-side type contract for the New Architecture (TurboModules/JSI).
 * Native implementations (Android: NativeNapSspModuleSpec, iOS: NativeNapSspModuleSpec.h)
 * must satisfy this interface.
 *
 * STATUS: Spec defined. Native implementation pending (Phase 4 / separate sprint).
 *
 * To enable New Architecture support:
 *   1. Android: extend TurboReactPackage, implement this spec in Kotlin with @ReactModule.
 *   2. iOS: generate ObjC header via `yarn codegen` and implement the Swift class.
 *   3. Set isNewArchEnabled = true in integration-test-app.
 */

import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // ── Core ──────────────────────────────────────────────────────────────────
  initialize(config: {
    mediaKey: string;
    adUnitIds: string[];
    mediations?: Object;
    logLevel?: string;
    coppa?: boolean;
  }): Promise<Object>;

  setLogLevel(level: string): void;
  setCoppa(enabled: boolean): void;
  getStatus(): Promise<Object>;
  requestTrackingAuthorization(): Promise<string>;

  // ── Required for RCTEventEmitter compatibility ────────────────────────────
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NapSspModule');
