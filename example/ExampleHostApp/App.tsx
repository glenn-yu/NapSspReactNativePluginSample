/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import type {PropsWithChildren} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  Button,
  Alert,
} from 'react-native';
import { NapSspAd, BannerAd, InterstitialAd, RewardedAd, isNativeModuleAvailable } from '../../src';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

type SectionProps = PropsWithChildren<{
  title: string;
}>;

function Section({children, title}: SectionProps): JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
}

function App(): JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  const handleInitialize = async () => {
    try {
      await NapSspAd.initialize({
        mediaKey: 'TEST_MEDIA_KEY',
        adUnitIds: ['TEST_BANNER', 'TEST_INTERSTITIAL', 'TEST_REWARDED'],
        logLevel: 'debug',
      });
      Alert.alert('Initialized');
    } catch (e:any) {
      Alert.alert('Initialize failed', e?.message || String(e));
    }
  };

  const showInterstitial = async () => {
    try {
      const inter = new InterstitialAd('TEST_INTERSTITIAL');
      await inter.load();
      await inter.show();
      Alert.alert('Interstitial shown');
    } catch (e:any) {
      Alert.alert('Interstitial error', e?.message || String(e));
    }
  };

  const showRewarded = async () => {
    try {
      const r = new RewardedAd('TEST_REWARDED');
      r.addAdEventListener('onRewarded', (reward) => {
        Alert.alert('Rewarded', JSON.stringify(reward));
      });
      await r.load();
      await r.show();
    } catch (e:any) {
      Alert.alert('Rewarded error', e?.message || String(e));
    }
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
            padding: 20,
          }}>
          <Section title="Test Controls">
            <View style={{ marginBottom: 8 }}>
              <Button title="Initialize SDK" onPress={handleInitialize} />
            </View>
            <View style={{ marginBottom: 8 }}>
              <Button title="Show Interstitial" onPress={showInterstitial} />
            </View>
            <View style={{ marginBottom: 8 }}>
              <Button title="Show Rewarded" onPress={showRewarded} />
            </View>
          </Section>

          <Section title="Banner (placeholder/native)">
            <BannerAd adUnitId="TEST_BANNER" size="BANNER_320x50" />
          </Section>

          <Section title="Debug">
            <Text>Native modules available: {JSON.stringify(isNativeModuleAvailable(['NapSspModule','NapSspInterstitial','NapSspRewarded','NapSspBannerView']))}</Text>
          </Section>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
