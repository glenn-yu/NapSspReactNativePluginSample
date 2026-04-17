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
import { NativeModuleNames } from './nativeBridge';
import type { AdError, BannerSize } from './types';

export interface BannerAdProps {
  adUnitId: string;
  size?: BannerSize;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: AdError) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}

type NativeBannerProps = Omit<
  BannerAdProps,
  'onAdLoaded' | 'onAdFailedToLoad' | 'onAdClicked' | 'onAdOpened' | 'onAdClosed'
> & {
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: { nativeEvent: AdError }) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  style?: StyleProp<ViewStyle>;
};

const FALLBACK_DIMENSIONS: Record<BannerSize, { width: number; height: number }> = {
  BANNER_320x50: { width: 320, height: 50 },
  BANNER_320x100: { width: 320, height: 100 },
  BANNER_300x250: { width: 300, height: 250 },
  LARGE_BANNER: { width: 320, height: 100 },
  MEDIUM_RECTANGLE: { width: 300, height: 250 },
  SMART_BANNER: { width: 320, height: 50 },
};

function resolveNativeBannerComponent(): React.ComponentType<NativeBannerProps> | null {
  for (const componentName of NativeModuleNames.banner) {
    try {
      return requireNativeComponent<NativeBannerProps>(componentName);
    } catch {
      // Try the next known native component name.
    }
  }

  return null;
}

const NativeBannerComponent = resolveNativeBannerComponent();

export default function BannerAd(props: BannerAdProps) {
  const size = props.size ?? 'BANNER_320x50';
  const dimensions = FALLBACK_DIMENSIONS[size];

  // Merge default dimensions with user-provided styles.
  const containerStyle = [
    { width: dimensions.width, height: dimensions.height },
    props.style,
  ];

  if (NativeBannerComponent) {
    return (
      <NativeBannerComponent
        adUnitId={props.adUnitId}
        size={size}
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
      <Text style={styles.title}>NapSsp Banner</Text>
      <Text style={styles.subtitle}>{props.adUnitId}</Text>
      <Text style={styles.note}>{Platform.OS} native view not linked yet</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: {
    alignItems: 'center',
    backgroundColor: '#EEF2FF',
    borderColor: '#CBD5E1',
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    justifyContent: 'center',
    overflow: 'hidden',
  },
  title: {
    color: '#1E293B',
    fontSize: 14,
    fontWeight: '600',
  },
  subtitle: {
    color: '#475569',
    fontSize: 12,
    marginTop: 2,
  },
  note: {
    color: '#64748B',
    fontSize: 10,
    marginTop: 4,
  },
});
