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
import type { AdError, NativeAdProps } from './types';

type NativeAdComponentProps = Omit<
  NativeAdProps,
  'onAdLoaded' | 'onAdFailedToLoad' | 'onAdClicked' | 'onAdOpened' | 'onAdClosed'
> & {
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: { nativeEvent: AdError }) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  style?: StyleProp<ViewStyle>;
};

function resolveNativeAdComponent(): React.ComponentType<NativeAdComponentProps> | null {
  for (const componentName of NativeModuleNames.nativeAd) {
    if (!isNativeViewAvailable(componentName)) {
      continue;
    }

    return requireNativeComponent<NativeAdComponentProps>(componentName);
  }

  return null;
}

const NativeAdComponent = resolveNativeAdComponent();

export default function NativeAd(props: NativeAdProps) {
  // Default styling for Native Ad container to give it some predictable boundaries
  // Developers can override this via props.style
  const containerStyle: StyleProp<ViewStyle> = [
    { minHeight: 250, minWidth: 300, width: '100%' as const },
    props.style,
  ];

  if (NativeAdComponent) {
    return (
      <NativeAdComponent
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
      <Text style={styles.title}>NapSsp Native Ad</Text>
      <Text style={styles.subtitle}>{props.adUnitId}</Text>
      <Text style={styles.note}>{Platform.OS} native view not linked yet</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: {
    alignItems: 'center',
    backgroundColor: '#F0FDF4', // Light green background to distinguish from Banner
    borderColor: '#86EFAC',
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    justifyContent: 'center',
    overflow: 'hidden',
  },
  title: {
    color: '#166534',
    fontSize: 14,
    fontWeight: '600',
  },
  subtitle: {
    color: '#15803D',
    fontSize: 12,
    marginTop: 2,
  },
  note: {
    color: '#22C55E',
    fontSize: 10,
    marginTop: 4,
  },
});
