import React from 'react';
import { Platform, View, type StyleProp, type ViewStyle } from 'react-native';
import { NativeModuleNames } from './nativeBridge';
import {
  dispatchReload,
  notLinkedError,
  resolveNativeAdComponent,
  type NativeInlineAdProps,
} from './inlineAdView';
import type { AdViewHandle, NativeAdProps } from './types';

const nativeAd = resolveNativeAdComponent<NativeInlineAdProps>(NativeModuleNames.nativeAd);

/**
 * Native (in-feed) ad.
 *
 * The creative is rendered by the SDK into the plugin's layout — Android uses
 * `res/layout/nap_ssp_native_ad.xml`, iOS uses `AMMNativeAdView.xib`. Ship your own file with the
 * same view IDs to restyle it.
 */
const NativeAd = React.forwardRef<AdViewHandle, NativeAdProps>(function NativeAd(props, ref) {
  const nativeRef = React.useRef<unknown>(null);
  const containerStyle: StyleProp<ViewStyle> = [{ width: '100%' }, props.style];

  React.useImperativeHandle(
    ref,
    () => ({
      reload: () => {
        if (nativeAd) {
          dispatchReload(nativeRef.current, nativeAd.name);
        }
      },
    }),
    [],
  );

  const { onAdFailedToLoad, adUnitId } = props;

  React.useEffect(() => {
    if (!nativeAd) {
      onAdFailedToLoad?.(notLinkedError(adUnitId, 'native', Platform.OS));
    }
  }, [onAdFailedToLoad, adUnitId]);

  if (!nativeAd) {
    return <View style={containerStyle} testID={props.testID} />;
  }

  const NativeComponent = nativeAd.component;

  return (
    <NativeComponent
      ref={nativeRef as never}
      adUnitId={props.adUnitId}
      style={containerStyle}
      testID={props.testID}
      onAdLoaded={props.onAdLoaded}
      onAdFailedToLoad={
        props.onAdFailedToLoad ? (event) => props.onAdFailedToLoad?.(event.nativeEvent) : undefined
      }
      onAdClicked={props.onAdClicked}
      onAdOpened={props.onAdOpened}
      onAdClosed={props.onAdClosed}
      onAdImpression={props.onAdImpression}
    />
  );
});

export default NativeAd;
