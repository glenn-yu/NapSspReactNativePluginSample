import {
  UIManager,
  findNodeHandle,
  requireNativeComponent,
  type HostComponent,
} from 'react-native';
import { isNativeViewAvailable } from './nativeBridge';
import type { AdError } from './types';

/** Shape the native inline views expose to JS. */
export interface NativeInlineAdProps {
  adUnitId: string;
  style?: unknown;
  testID?: string;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: { nativeEvent: AdError }) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
}

/**
 * Resolves the registered native component, or `null` when the plugin is not linked.
 *
 * `requireNativeComponent` is called at most once per name: calling it for an unregistered view
 * logs a hard error in development.
 */
export function resolveNativeAdComponent<TProps>(
  componentNames: readonly string[],
): { component: HostComponent<TProps>; name: string } | null {
  for (const name of componentNames) {
    if (isNativeViewAvailable(name)) {
      return { component: requireNativeComponent<TProps>(name), name };
    }
  }
  return null;
}

/**
 * Sends the `reload` command to a mounted native ad view.
 * Android registers it through `getCommandsMap`, iOS through the view manager method.
 */
export function dispatchReload(ref: unknown, componentName: string): void {
  const handle = findNodeHandle(ref as never);
  if (handle == null) {
    return;
  }

  const commands = UIManager.getViewManagerConfig?.(componentName)?.Commands as
    | Record<string, number | string>
    | undefined;

  UIManager.dispatchViewManagerCommand(handle, commands?.reload ?? 'reload', []);
}

/** Error reported when the native view is missing, so a failure is never silently swallowed. */
export function notLinkedError(adUnitId: string, format: string, platform: string): AdError {
  return {
    code: 'nap_ssp_view_not_linked',
    message:
      `The NapSsp ${format} view is not registered on ${platform}. ` +
      'Rebuild the app after installing react-native-nap-ssp (Android: sync Gradle, iOS: pod install).',
    details: { adUnitId, format },
  };
}
