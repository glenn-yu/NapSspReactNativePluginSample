import React, {useEffect} from 'react';
import {
  Alert,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  BannerAd,
  InterstitialAd,
  NapSspAd,
  RewardedAd,
  isNativeModuleAvailable,
} from '../../src';

const TEST_CONFIG = {
  mediaKey: 'TEST_MEDIA_KEY',
  bannerId: 'TEST_BANNER',
  interstitialId: 'TEST_INTERSTITIAL',
  rewardedId: 'TEST_REWARDED',
};

function App(): JSX.Element {
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey: TEST_CONFIG.mediaKey,
      adUnitIds: [TEST_CONFIG.bannerId, TEST_CONFIG.interstitialId, TEST_CONFIG.rewardedId],
      logLevel: 'debug',
    }).catch((error: unknown) => {
      console.warn('NapSsp initialize failed', error);
    });
  }, []);

  const showInterstitial = async () => {
    try {
      const interstitial = new InterstitialAd(TEST_CONFIG.interstitialId);
      await interstitial.load();
      await interstitial.show();
    } catch (error: any) {
      Alert.alert('Interstitial', error?.message ?? 'Failed to show interstitial');
    }
  };

  const showRewarded = async () => {
    try {
      const rewarded = new RewardedAd(TEST_CONFIG.rewardedId);
      rewarded.addAdEventListener('onRewarded', () => {
        Alert.alert('Rewarded', 'Reward event received');
      });
      await rewarded.load();
      await rewarded.show();
    } catch (error: any) {
      Alert.alert('Rewarded', error?.message ?? 'Failed to show rewarded ad');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>NapSsp Example Host App</Text>
        <Text style={styles.subtitle}>
          Beginner-friendly check for initialize, banner, interstitial, and rewarded flows.
        </Text>

        <View style={styles.card}>
          <Text style={styles.label}>Status</Text>
          <Text style={styles.value}>Initializing / placeholder mode</Text>
          <Text style={styles.note}>
            Native modules available: {String(isNativeModuleAvailable(['NapSspModule', 'NapSspBannerView']))}
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.label}>Banner</Text>
          <BannerAd adUnitId={TEST_CONFIG.bannerId} size="BANNER_320x50" />
        </View>

        <View style={styles.actions}>
          <TouchableOpacity style={styles.button} onPress={showInterstitial}>
            <Text style={styles.buttonText}>Show Interstitial</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, styles.secondaryButton]} onPress={showRewarded}>
            <Text style={styles.buttonText}>Show Rewarded</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F7F7F8',
  },
  content: {
    padding: 20,
    gap: 16,
  },
  title: {
    fontSize: 26,
    fontWeight: '700',
    color: '#111827',
  },
  subtitle: {
    color: '#4B5563',
    fontSize: 14,
    lineHeight: 20,
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    gap: 8,
  },
  label: {
    color: '#6B7280',
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  value: {
    color: '#111827',
    fontSize: 16,
    fontWeight: '600',
  },
  note: {
    color: '#6B7280',
    fontSize: 12,
  },
  actions: {
    gap: 12,
  },
  button: {
    alignItems: 'center',
    backgroundColor: '#2563EB',
    borderRadius: 10,
    paddingVertical: 14,
  },
  secondaryButton: {
    backgroundColor: '#F97316',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});

export default App;
