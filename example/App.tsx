import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Alert,
} from 'react-native';

// 플러그인에서 필요한 기능들을 불러옵니다.
import {
  NapSspAd,
  BannerAd,
  InterstitialAd,
  RewardedAd,
} from 'react-native-nap-ssp';

// 테스트를 위한 설정값들입니다. (실제 서비스 시에는 발급받은 키를 사용하세요)
const TEST_CONFIG = {
  mediaKey: 'TEST_MEDIA_KEY',
  bannerId: 'BANNER_TEST_ID',
  interstitialId: 'INTERSTITIAL_TEST_ID',
  rewardedId: 'REWARDED_TEST_ID',
};

const App = () => {
  const [isInitialized, setIsInitialized] = useState(false);

  // 1. 앱이 켜질 때 광고 SDK를 초기화합니다.
  useEffect(() => {
    const initSDK = async () => {
      try {
        console.log('SDK 초기화 시작...');
        await NapSspAd.initialize({
          mediaKey: TEST_CONFIG.mediaKey,
          adUnitIds: [TEST_CONFIG.bannerId, TEST_CONFIG.interstitialId, TEST_CONFIG.rewardedId],
          logLevel: 'debug',
        });
        setIsInitialized(true);
        console.log('SDK 초기화 완료!');
      } catch (error) {
        console.error('초기화 중 오류 발생:', error);
      }
    };

    initSDK();
  }, []);

  // 2. 전면 광고를 보여주는 함수입니다.
  const handleShowInterstitial = async () => {
    const interstitial = new InterstitialAd(TEST_CONFIG.interstitialId);
    try {
      console.log('전면 광고 로드 중...');
      await interstitial.load();
      console.log('전면 광고 표시!');
      await interstitial.show();
    } catch (error) {
      Alert.alert('알림', '전면 광고를 불러오지 못했습니다.');
    }
  };

  // 3. 보상형 광고를 보여주는 함수입니다.
  const handleShowRewarded = async () => {
    const rewarded = new RewardedAd(TEST_CONFIG.rewardedId);
    
    // 사용자가 광고 시청을 완료했을 때 이벤트를 등록합니다.
    rewarded.addAdEventListener('onRewarded', (reward) => {
      Alert.alert('보상 획득!', `${reward.type}을(를) ${reward.amount}개 받았습니다.`);
    });

    try {
      console.log('보상형 광고 로드 중...');
      await rewarded.load();
      console.log('보상형 광고 표시!');
      await rewarded.show();
    } catch (error) {
      Alert.alert('알림', '보상형 광고를 불러오지 못했습니다.');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scrollContent}>
        
        <View style={styles.header}>
          <Text style={styles.title}>Nap SSP 광고 테스트</Text>
          <Text style={styles.status}>
            상태: {isInitialized ? '✅ 초기화 완료' : '⏳ 초기화 중...'}
          </Text>
        </View>

        {/* --- 배너 광고 섹션 --- */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>1. 배너 광고 (Banner)</Text>
          <View style={styles.adContainer}>
            <BannerAd
              adUnitId={TEST_CONFIG.bannerId}
              size="BANNER_320x50"
              onAdLoaded={() => console.log('배너 로드 성공')}
              onAdFailedToLoad={(e) => console.log('배너 실패:', e.message)}
            />
          </View>
        </View>

        {/* --- 전면 광고 섹션 --- */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>2. 전면 광고 (Interstitial)</Text>
          <TouchableOpacity 
            style={styles.button} 
            onPress={handleShowInterstitial}
          >
            <Text style={styles.buttonText}>전면 광고 보기</Text>
          </TouchableOpacity>
        </View>

        {/* --- 보상형 광고 섹션 --- */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>3. 보상형 광고 (Rewarded)</Text>
          <TouchableOpacity 
            style={[styles.button, { backgroundColor: '#FF9500' }]} 
            onPress={handleShowRewarded}
          >
            <Text style={styles.buttonText}>보상형 광고 보기</Text>
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F7',
  },
  scrollContent: {
    padding: 20,
  },
  header: {
    marginBottom: 30,
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1D1D1F',
  },
  status: {
    marginTop: 8,
    color: '#86868B',
  },
  section: {
    marginBottom: 40,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 15,
    color: '#3A3A3C',
  },
  adContainer: {
    backgroundColor: '#FFFFFF',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    // 그림자 효과
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
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
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default App;
