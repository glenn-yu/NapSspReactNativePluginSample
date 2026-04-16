import React from 'react';
import { View } from 'react-native';

interface Props {
  adUnitId: string;
  size?: string;
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (err: any) => void;
  style?: any;
}

export default function BannerAd(_props: Props) {
  // Placeholder native component wrapper
  return <View style={{ width: 320, height: 50 }} />;
}
