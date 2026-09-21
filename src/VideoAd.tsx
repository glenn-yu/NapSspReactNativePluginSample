import React from 'react';
import { Platform, View, type StyleProp, type ViewStyle } from 'react-native';
import { NativeModuleNames } from './nativeBridge';
import {
  dispatchReload,
  notLinkedError,
  resolveNativeAdComponent,
  type NativeInlineAdProps,
} from './inlineAdView';
import type { AdViewHandle, VideoAdProps } from './types';

interface NativeVideoProps extends NativeInlineAdProps {
  isRetry?: boolean;
  onAdCompleted?: () => void;
  onAdSkipped?: () => void;
}

const nativeVideo = resolveNativeAdComponent<NativeVideoProps>(NativeModuleNames.videoAd);

/** Inline (in-feed) video ad. */
const VideoAd = React.forwardRef<AdViewHandle, VideoAdProps>(function VideoAd(props, ref) {
  const nativeRef = React.useRef<unknown>(null);
  const containerStyle: StyleProp<ViewStyle> = [{ width: '100%', minHeight: 200 }, props.style];

  React.useImperativeHandle(
    ref,
    () => ({
      reload: () => {
        if (nativeVideo) {
          dispatchReload(nativeRef.current, nativeVideo.name);
        }
      },
    }),
    [],
  );

  const { onAdFailedToLoad, adUnitId } = props;

  React.useEffect(() => {
    if (!nativeVideo) {
      onAdFailedToLoad?.(notLinkedError(adUnitId, 'video', Platform.OS));
    }
  }, [onAdFailedToLoad, adUnitId]);

  if (!nativeVideo) {
    return <View style={containerStyle} testID={props.testID} />;
  }

  const NativeComponent = nativeVideo.component;

  return (
    <NativeComponent
      ref={nativeRef as never}
      adUnitId={props.adUnitId}
      isRetry={props.isRetry ?? false}
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
      onAdCompleted={props.onAdCompleted}
      onAdSkipped={props.onAdSkipped}
    />
  );
});

export default VideoAd;
