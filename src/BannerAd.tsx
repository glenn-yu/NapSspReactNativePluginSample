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
import type { AdError, BannerSize } from './types';

export interface BannerAdProps {
  adUnitId: string;
  size?: BannerSize;
  /** Android only: set to false to suppress automatic ad loading on mount. Defaults to true. */
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

type NativeBannerProps = Omit<
  BannerAdProps,
  'onAdLoaded' | 'onAdFailedToLoad' | 'onAdClicked' | 'onAdOpened' | 'onAdClosed' | 'onAdImpression' | 'autoLoad'
> & {
  autoLoad?: boolean;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (event: { nativeEvent: AdError }) => void;
  onAdClicked?: () => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdImpression?: () => void;
  style?: StyleProp<ViewStyle>;
};

const FALLBACK_DIMENSIONS: Record<BannerSize, { width: number; height: number }> = {
  BANNER_320x50: { width: 320, height: 50 },
  BANNER_320x100: { width: 320, height: 100 },
  BANNER_300x250: { width: 300, height: 250 },
  BANNER_320x480: { width: 320, height: 480 },
  LARGE_BANNER: { width: 320, height: 100 },
  MEDIUM_RECTANGLE: { width: 300, height: 250 },
  SMART_BANNER: { width: 320, height: 50 },
};

function resolveNativeBannerComponent(): React.ComponentType<NativeBannerProps> | null {
  for (const componentName of NativeModuleNames.banner) {
    if (!isNativeViewAvailable(componentName)) {
      continue;
    }

    return requireNativeComponent<NativeBannerProps>(componentName);
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
      <View style={containerStyle}>
        <NativeBannerComponent
          adUnitId={props.adUnitId}
          size={size}
          autoLoad={props.autoLoad ?? true}
          style={{ width: '100%', height: '100%' }}
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
      </View>
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
