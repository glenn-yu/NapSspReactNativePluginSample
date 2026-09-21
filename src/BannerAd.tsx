import React from 'react';
import { Platform, View, type StyleProp, type ViewStyle } from 'react-native';
import { NativeModuleNames } from './nativeBridge';
import {
  dispatchReload,
  notLinkedError,
  resolveNativeAdComponent,
  type NativeInlineAdProps,
} from './inlineAdView';
import type { AdError, AdViewHandle, BannerSize } from './types';

export interface BannerAdProps {
  adUnitId: string;
  /**
   * Layout hint only — the served size comes from the ad unit's server configuration.
   * It seeds the view's default width/height so the row does not collapse before the first fill;
   * `style` always wins.
   */
  size?: BannerSize;
  /** Set to `false` to suppress the automatic load on mount. Defaults to `true`. */
  autoLoad?: boolean;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}

interface NativeBannerProps extends NativeInlineAdProps {
  size?: string;
  autoLoad?: boolean;
}

const KNOWN_DIMENSIONS: Record<string, { width: number; height: number }> = {
  BANNER_320x50: { width: 320, height: 50 },
  BANNER_320x100: { width: 320, height: 100 },
  BANNER_300x250: { width: 300, height: 250 },
  BANNER_320x480: { width: 320, height: 480 },
  LARGE_BANNER: { width: 320, height: 100 },
  MEDIUM_RECTANGLE: { width: 300, height: 250 },
  SMART_BANNER: { width: 320, height: 50 },
};

/** Parses `BANNER_WxH` so a new server-side size needs no code change. */
function resolveBannerDimensions(size: string): { width: number; height: number } {
  const known = KNOWN_DIMENSIONS[size];
  if (known) {
    return known;
  }

  const match = size.match(/(\d+)[xX](\d+)/);
  if (match) {
    const width = Number(match[1]);
    const height = Number(match[2]);
    if (width > 0 && height > 0) {
      return { width, height };
    }
  }
  return KNOWN_DIMENSIONS.BANNER_320x50!;
}

const nativeBanner = resolveNativeAdComponent<NativeBannerProps>(NativeModuleNames.banner);

/**
 * Inline banner. The ad loads when the view is attached and is destroyed when it unmounts.
 *
 * ```tsx
 * const ref = useRef<AdViewHandle>(null);
 * <BannerAd ref={ref} adUnitId="1234567" size="BANNER_320x50" />
 * // later: ref.current?.reload()
 * ```
 */
const BannerAd = React.forwardRef<AdViewHandle, BannerAdProps>(function BannerAd(props, ref) {
  const nativeRef = React.useRef<unknown>(null);
  const size = props.size ?? 'BANNER_320x50';
  const dimensions = resolveBannerDimensions(size);
  const containerStyle: StyleProp<ViewStyle> = [dimensions, props.style];

  React.useImperativeHandle(
    ref,
    () => ({
      reload: () => {
        if (nativeBanner) {
          dispatchReload(nativeRef.current, nativeBanner.name);
        }
      },
    }),
    [],
  );

  const { onAdFailedToLoad, adUnitId } = props;

  // Report the missing native view instead of rendering a stand-in that looks like a real ad.
  React.useEffect(() => {
    if (!nativeBanner) {
      onAdFailedToLoad?.(notLinkedError(adUnitId, 'banner', Platform.OS));
    }
  }, [onAdFailedToLoad, adUnitId]);

  if (!nativeBanner) {
    return <View style={containerStyle} testID={props.testID} />;
  }

  const NativeBanner = nativeBanner.component;

  return (
    <View style={containerStyle}>
      <NativeBanner
        ref={nativeRef as never}
        adUnitId={props.adUnitId}
        size={size}
        autoLoad={props.autoLoad ?? true}
        style={{ width: '100%', height: '100%' }}
        testID={props.testID}
        onAdLoaded={props.onAdLoaded}
        onAdFailedToLoad={
          props.onAdFailedToLoad
            ? (event) => props.onAdFailedToLoad?.(event.nativeEvent)
            : undefined
        }
        onAdClicked={props.onAdClicked}
        onAdOpened={props.onAdOpened}
        onAdClosed={props.onAdClosed}
        onAdImpression={props.onAdImpression}
      />
    </View>
  );
});

export default BannerAd;
