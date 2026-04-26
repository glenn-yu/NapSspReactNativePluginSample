import React from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View,
  requireNativeComponent,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import { NativeModuleNames, isNativeViewAvailable } from './nativeBridge';
import type { AdError, VideoAdProps } from './types';

type NativeVideoAdComponentProps = Omit<
  VideoAdProps,
  'onAdLoaded' | 'onAdFailedToLoad' | 'onAdClicked' | 'onAdOpened' | 'onAdClosed' | 'onAdImpression' | 'onAdCompleted' | 'onAdSkipped'
> & {
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: { nativeEvent: AdError }) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
  onAdCompleted?: () => void;
  onAdSkipped?: () => void;
  style?: StyleProp<ViewStyle>;
};

function resolveNativeVideoAdComponent(): React.ComponentType<NativeVideoAdComponentProps> | null {
  for (const componentName of NativeModuleNames.videoAd) {
    if (!isNativeViewAvailable(componentName)) {
      continue;
    }

    return requireNativeComponent<NativeVideoAdComponentProps>(componentName);
  }

  return null;
}

const NativeVideoAdComponent = resolveNativeVideoAdComponent();

export default function VideoAd(props: VideoAdProps) {
  const containerStyle: StyleProp<ViewStyle> = [
    { minHeight: 200, minWidth: 300, width: '100%' as const },
    props.style,
  ];

  if (NativeVideoAdComponent) {
    return (
      <NativeVideoAdComponent
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
  }

  return (
    <View
      accessibilityRole="image"
      style={[
        styles.placeholder,
        containerStyle,
      ]}
    >
      <Text style={styles.title}>NapSsp Video Ad</Text>
      <Text style={styles.subtitle}>{props.adUnitId}</Text>
      <Text style={styles.note}>{Platform.OS} native view not linked yet</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: {
    alignItems: 'center',
    backgroundColor: '#FFF1F2', // Light red background to distinguish
    borderColor: '#FDA4AF',
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    justifyContent: 'center',
    overflow: 'hidden',
  },
  title: {
    color: '#9F1239',
    fontSize: 14,
    fontWeight: '600',
  },
  subtitle: {
    color: '#BE123C',
    fontSize: 12,
    marginTop: 2,
  },
  note: {
    color: '#E11D48',
    fontSize: 10,
    marginTop: 4,
  },
});
