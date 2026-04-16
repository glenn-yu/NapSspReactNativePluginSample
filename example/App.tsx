import React from 'react';
import { SafeAreaView, Text } from 'react-native';
import { BannerAd } from '../src/BannerAd';

export default function App() {
  return (
    <SafeAreaView style={{flex:1,alignItems:'center',justifyContent:'center'}}>
      <Text>NapSsp React Native Plugin - Example</Text>
      <BannerAd adUnitId="TEST_BANNER" />
    </SafeAreaView>
  );
}
