import React, { useEffect, useMemo, useState } from 'react';
import { Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { BannerAd, InterstitialAd, NapSspAd, NativeModuleNames, isNativeModuleAvailable } from 'react-native-nap-ssp';

export default function App() {
  const [status, setStatus] = useState('Booting sample…');
  const [nativeReady, setNativeReady] = useState(false);

  const interstitial = useMemo(() => new InterstitialAd('TEST_INTERSTITIAL'), []);

  useEffect(() => {
    setNativeReady(
      isNativeModuleAvailable(NativeModuleNames.napSsp) &&
        isNativeModuleAvailable(NativeModuleNames.interstitial) &&
        isNativeModuleAvailable(NativeModuleNames.banner),
    );

    NapSspAd.initialize({
      mediaKey: 'YOUR_MEDIA_KEY',
      adUnitIds: ['TEST_BANNER', 'TEST_INTERSTITIAL'],
      logLevel: 'info',
      coppa: false,
    })
      .then(() => setStatus('NapSspAd.initialize() completed.'))
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : String(error);
        setStatus(`Initialization skipped: ${message}`);
      });
  }, []);

  const loadInterstitial = async () => {
    try {
      await interstitial.load();
      setStatus('Interstitial loaded.');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(`Load failed: ${message}`);
    }
  };

  const showInterstitial = async () => {
    try {
      await interstitial.show();
      setStatus('Interstitial shown.');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(`Show failed: ${message}`);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>NapSsp React Native Plugin</Text>
        <Text style={styles.subtitle}>JS/TS scaffold and example app</Text>
        <Text style={styles.status}>{status}</Text>
        <Text style={styles.note}>
          Native bridge ready: {nativeReady ? 'yes' : 'no'}
        </Text>

        <BannerAd adUnitId="TEST_BANNER" style={styles.banner} />

        <View style={styles.actions}>
          <Pressable style={styles.button} onPress={loadInterstitial}>
            <Text style={styles.buttonText}>Load interstitial</Text>
          </Pressable>
          <Pressable style={styles.button} onPress={showInterstitial}>
            <Text style={styles.buttonText}>Show interstitial</Text>
          </Pressable>
        </View>

        <Text style={styles.footer}>
          This sample demonstrates the public API surface before the native modules are wired.
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    color: '#0F172A',
    fontSize: 24,
    fontWeight: '700',
  },
  subtitle: {
    color: '#334155',
    fontSize: 14,
  },
  status: {
    color: '#1D4ED8',
    fontSize: 13,
    textAlign: 'center',
  },
  note: {
    color: '#64748B',
    fontSize: 12,
    textAlign: 'center',
  },
  banner: {
    marginTop: 8,
  },
  actions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    marginTop: 12,
  },
  button: {
    backgroundColor: '#2563EB',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
  },
  footer: {
    color: '#475569',
    fontSize: 12,
    marginTop: 8,
    textAlign: 'center',
  },
});
