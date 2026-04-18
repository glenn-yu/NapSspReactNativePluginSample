import React, {useEffect, useState} from 'react';
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
  InterstitialVideoAd,
  NapSspAd,
  NativeAd,
  RewardedAd,
  VideoAd,
  isNativeModuleAvailable,
} from '../../src';

import {Platform} from 'react-native';

const TEST_CONFIG_ANDROID = {
  mediaKey: '10771',
  bannerId: '104701', // 320x50
  nativeAdId: '104588',
  videoAdId: '104591', // instream
  interstitialId: '104704',
  interstitialVideoId: '104703',
  rewardedId: '103722',
};

const TEST_CONFIG_IOS = {
  mediaKey: '10347',
  bannerId: '103790', // 320x50
  nativeAdId: '101626',
  videoAdId: '104711',
  interstitialId: '104707',
  interstitialVideoId: '103868',
  rewardedId: '104710',
};

const TEST_CONFIG = Platform.OS === 'ios' ? TEST_CONFIG_IOS : TEST_CONFIG_ANDROID;

function App(): JSX.Element {
  const [statusText, setStatusText] = useState('Waiting for SDK initialization...');
  const appendStatus = (label: string, payload?: unknown) => {
    const line = payload === undefined ? label : `${label}: ${JSON.stringify(payload)}`;
    console.log(`[NapSspExample] ${line}`);
    setStatusText((prev) => `${line}\n\n${prev}`);
  };
  const isNative = isNativeModuleAvailable([
    'NapSspModule',
    'NapSspBannerView',
    'NapSspNativeAdView',
    'NapSspVideoAdView',
    'NapSspInterstitialVideo',
  ]);

  useEffect(() => {
    let isMounted = true;

    const initialize = async () => {
      const config = {
        mediaKey: TEST_CONFIG.mediaKey,
        adUnitIds: [
          TEST_CONFIG.bannerId,
          TEST_CONFIG.nativeAdId,
          TEST_CONFIG.videoAdId,
          TEST_CONFIG.interstitialId,
          TEST_CONFIG.interstitialVideoId,
          TEST_CONFIG.rewardedId,
        ],
        logLevel: 'debug' as const,
      };

      appendStatus('initialize.start', {
        platform: Platform.OS,
        nativeAvailable: isNative,
        config,
      });

      try {
        await NapSspAd.initialize(config);
        appendStatus('initialize.success');

        const status = await NapSspAd.getStatus();
        appendStatus('getStatus.success', status);
      } catch (error) {
        console.warn('NapSsp initialize failed', error);
        appendStatus('initialize.failed', String(error));
        if (isMounted) {
          setStatusText((prev) => `Initialization failed: ${String(error)}\n\n${prev}`);
        }
      }
    };

    initialize();

    return () => {
      isMounted = false;
    };
  }, []);

  const refreshStatus = async () => {
    try {
      const status = await NapSspAd.getStatus();
      appendStatus('refreshStatus.success', status);
    } catch (error) {
      appendStatus('refreshStatus.failed', String(error));
      Alert.alert('Status', `Unable to fetch status: ${String(error)}`);
    }
  };

  const handleShowInterstitial = async () => {
    const interstitial = new InterstitialAd(TEST_CONFIG.interstitialId);
    interstitial.addAdEventListener('loaded', () => appendStatus('interstitial.loaded', TEST_CONFIG.interstitialId));
    interstitial.addAdEventListener('loadFailed', (error) => appendStatus('interstitial.loadFailed', error));
    interstitial.addAdEventListener('opened', () => appendStatus('interstitial.opened', TEST_CONFIG.interstitialId));
    interstitial.addAdEventListener('closed', () => appendStatus('interstitial.closed', TEST_CONFIG.interstitialId));
    interstitial.addAdEventListener('clicked', () => appendStatus('interstitial.clicked', TEST_CONFIG.interstitialId));
    interstitial.addAdEventListener('impression', () => appendStatus('interstitial.impression', TEST_CONFIG.interstitialId));
    try {
      appendStatus('interstitial.load.start', TEST_CONFIG.interstitialId);
      await interstitial.load();
      appendStatus('interstitial.show.start', TEST_CONFIG.interstitialId);
      await interstitial.show();
    } catch (error) {
      appendStatus('interstitial.failed', String(error));
      Alert.alert('알림', `전면 광고를 불러오지 못했습니다.\n${String(error)}`);
    }
  };

  const handleShowInterstitialVideo = async () => {
    const interstitialVideo = new InterstitialVideoAd(TEST_CONFIG.interstitialVideoId);
    interstitialVideo.addAdEventListener('loaded', () => appendStatus('interstitialVideo.loaded', TEST_CONFIG.interstitialVideoId));
    interstitialVideo.addAdEventListener('loadFailed', (error) => appendStatus('interstitialVideo.loadFailed', error));
    interstitialVideo.addAdEventListener('opened', () => appendStatus('interstitialVideo.opened', TEST_CONFIG.interstitialVideoId));
    interstitialVideo.addAdEventListener('closed', () => appendStatus('interstitialVideo.closed', TEST_CONFIG.interstitialVideoId));
    interstitialVideo.addAdEventListener('clicked', () => appendStatus('interstitialVideo.clicked', TEST_CONFIG.interstitialVideoId));
    interstitialVideo.addAdEventListener('impression', () => appendStatus('interstitialVideo.impression', TEST_CONFIG.interstitialVideoId));
    try {
      appendStatus('interstitialVideo.load.start', TEST_CONFIG.interstitialVideoId);
      await interstitialVideo.load();
      appendStatus('interstitialVideo.show.start', TEST_CONFIG.interstitialVideoId);
      await interstitialVideo.show();
      Alert.alert('알림', '전면 동영상 광고 show()가 호출되었습니다.');
    } catch (error) {
      appendStatus('interstitialVideo.failed', String(error));
      Alert.alert('알림', `전면 동영상 광고를 불러오지 못했습니다.\n${String(error)}`);
    }
  };

  const handleShowRewarded = async () => {
    const rewarded = new RewardedAd(TEST_CONFIG.rewardedId);

    rewarded.addAdEventListener('loaded', () => appendStatus('rewarded.loaded', TEST_CONFIG.rewardedId));
    rewarded.addAdEventListener('loadFailed', (error) => appendStatus('rewarded.loadFailed', error));
    rewarded.addAdEventListener('opened', () => appendStatus('rewarded.opened', TEST_CONFIG.rewardedId));
    rewarded.addAdEventListener('closed', () => appendStatus('rewarded.closed', TEST_CONFIG.rewardedId));
    rewarded.addAdEventListener('clicked', () => appendStatus('rewarded.clicked', TEST_CONFIG.rewardedId));
    rewarded.addAdEventListener('impression', () => appendStatus('rewarded.impression', TEST_CONFIG.rewardedId));
    rewarded.addAdEventListener('onRewarded', () => {
      appendStatus('rewarded.rewarded', TEST_CONFIG.rewardedId);
      Alert.alert('보상 획득!', '보상 이벤트가 발생했습니다.');
    });

    try {
      appendStatus('rewarded.load.start', TEST_CONFIG.rewardedId);
      await rewarded.load();
      appendStatus('rewarded.show.start', TEST_CONFIG.rewardedId);
      await rewarded.show();
    } catch (error) {
      appendStatus('rewarded.failed', String(error));
      Alert.alert('알림', `보상형 광고를 불러오지 못했습니다.\n${String(error)}`);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.title}>Nap SSP 광고 테스트</Text>
          <Text style={styles.subtitle}>
            iOS 브리지 예제를 처음 보는 분도 따라할 수 있도록 placeholder/native-safe
            동작과 실제 API 호출 경로를 함께 보여줍니다.
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>0. 초기화 상태</Text>
          <View style={styles.statusCard}>
            <Text style={styles.statusLabel}>Native modules available</Text>
            <Text style={styles.statusValue}>
              {String(
                isNativeModuleAvailable([
                  'NapSspModule',
                  'NapSspBannerView',
                  'NapSspNativeAdView',
                  'NapSspVideoAdView',
                  'NapSspInterstitialVideo',
                ]),
              )}
            </Text>
            <Text style={styles.statusLabel}>NapSspAd.getStatus()</Text>
            <Text style={styles.statusJson}>{statusText}</Text>
            <View style={styles.statusActions}>
              <TouchableOpacity style={styles.smallButton} onPress={refreshStatus}>
                <Text style={styles.buttonText}>새로고침</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>1. 배너 광고 (Banner)</Text>
          <View style={styles.adContainer}>
            {isNative ? (
              <BannerAd
                adUnitId={TEST_CONFIG.bannerId}
                size="BANNER_320x50"
                onAdLoaded={() => appendStatus('banner.loaded', TEST_CONFIG.bannerId)}
                onAdFailedToLoad={(e) => appendStatus('banner.loadFailed', e)}
              />
            ) : (
              <Text style={{color: '#6B7280'}}>Native banner not available (placeholder)</Text>
            )}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>2. 네이티브 광고 (Native Ad)</Text>
          <View style={styles.adContainer}>
            {isNative ? (
              <NativeAd adUnitId={TEST_CONFIG.nativeAdId} />
            ) : (
              <Text style={{color: '#6B7280'}}>Native ad not available (placeholder)</Text>
            )}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>3. 동영상 광고 뷰 (Video Ad)</Text>
          <View style={styles.adContainer}>
            {isNative ? (
              <VideoAd adUnitId={TEST_CONFIG.videoAdId} />
            ) : (
              <Text style={{color: '#6B7280'}}>Video ad not available (placeholder)</Text>
            )}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>4. 전면 광고 (Interstitial)</Text>
          <TouchableOpacity style={styles.button} onPress={handleShowInterstitial}>
            <Text style={styles.buttonText}>전면 광고 보기</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>5. 전면 동영상 광고 (Interstitial Video)</Text>
          <TouchableOpacity
            style={[styles.button, styles.secondaryButton]}
            onPress={handleShowInterstitialVideo}
          >
            <Text style={styles.buttonText}>전면 동영상 광고 보기</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>6. 보상형 광고 (Rewarded)</Text>
          <TouchableOpacity style={styles.button} onPress={handleShowRewarded}>
            <Text style={styles.buttonText}>보상형 광고 보기</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F7',
  },
  scrollContent: {
    padding: 20,
  },
  header: {
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1D1D1F',
  },
  subtitle: {
    marginTop: 8,
    color: '#636366',
    lineHeight: 20,
  },
  section: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 12,
    color: '#3A3A3C',
  },
  statusCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 2,
  },
  statusLabel: {
    color: '#6B7280',
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    marginTop: 8,
  },
  statusValue: {
    color: '#111827',
    fontSize: 14,
    marginTop: 4,
  },
  statusJson: {
    color: '#111827',
    fontSize: 12,
    marginTop: 4,
    fontFamily: 'Menlo',
  },
  statusActions: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 16,
  },
  adContainer: {
    backgroundColor: '#FFFFFF',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 15,
    borderRadius: 10,
    alignItems: 'center',
  },
  smallButton: {
    flex: 1,
    backgroundColor: '#007AFF',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
  },
  secondaryButton: {
    backgroundColor: '#FF9500',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default App;
