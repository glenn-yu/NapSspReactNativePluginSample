import React, { useState } from 'react';
import {
  Alert,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
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
} from 'react-native-nap-ssp';

// ─── Default real IDs for validation ─────────────────────────────────────────
const DEFAULT_CONFIG = Platform.select({
  android: {
    mediaKey: '10771',
    ids: {
      banner: '104701',
      interstitial: '104703',
      rewarded: '103722',
      native: '104588',
      video: '104589',
      interstitialVideo: '104591',
    },
  },
  ios: {
    mediaKey: '10347',
    ids: {
      banner: '103790',
      interstitial: '103868',
      rewarded: '104710',
      native: '101626',
      video: '104709',
      interstitialVideo: '104711',
    },
  },
  default: {
    mediaKey: '',
    ids: {
      banner: '',
      interstitial: '',
      rewarded: '',
      native: '',
      video: '',
      interstitialVideo: '',
    },
  },
})!;

// ─── Log entry ────────────────────────────────────────────────────────────────
interface LogEntry { ts: string; tag: string; msg: string }
type AdStatusKey = 'banner' | 'native' | 'video' | 'interstitial' | 'rewarded' | 'interstitialVideo';
type AdStatus = {
  loaded: boolean;
  impression: boolean;
  opened: boolean;
  completed: boolean;
  rewarded: boolean;
  lastMessage: string;
};
function now() { return new Date().toLocaleTimeString(); }
const createEmptyAdStatus = (): AdStatus => ({
  loaded: false,
  impression: false,
  opened: false,
  completed: false,
  rewarded: false,
  lastMessage: 'idle',
});
const createInitialAdStatuses = (): Record<AdStatusKey, AdStatus> => ({
  banner: createEmptyAdStatus(),
  native: createEmptyAdStatus(),
  video: createEmptyAdStatus(),
  interstitial: createEmptyAdStatus(),
  rewarded: createEmptyAdStatus(),
  interstitialVideo: createEmptyAdStatus(),
});

// ─── ID input row ─────────────────────────────────────────────────────────────
const isNumeric = (v: string) => /^\d+$/.test(v.trim());

const IdRow = ({ label, value, onChangeText }: {
  label: string;
  value: string;
  onChangeText: (v: string) => void;
}) => {
  const invalid = value.trim().length > 0 && !isNumeric(value);
  return (
    <View style={styles.idRow}>
      <Text style={styles.idLabel}>{label}</Text>
      <TextInput
        style={[styles.idInput, invalid && styles.idInputError]}
        value={value}
        onChangeText={onChangeText}
        onBlur={() => {
          if (value.trim().length > 0 && !isNumeric(value)) {
            Alert.alert(
              '입력 오류',
              `${label} ID는 숫자만 입력 가능합니다.\nnap SSP 파트너사이트에서 발급받은 숫자 ID를 입력하세요.`,
              [{ text: '확인' }],
            );
          }
        }}
        placeholder={`${label} ID (숫자)`}
        keyboardType="numeric"
        autoCapitalize="none"
      />
    </View>
  );
};

// ─── Section wrapper ──────────────────────────────────────────────────────────
const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <View style={styles.section}>
    <Text style={styles.sectionTitle}>{title}</Text>
    {children}
  </View>
);

// ─── Log viewer ───────────────────────────────────────────────────────────────
const LogView = ({ logs }: { logs: LogEntry[] }) => (
  <View style={styles.logBox}>
    {logs.slice(-12).reverse().map((l, i) => (
      <Text key={i} style={styles.logLine}>
        <Text style={styles.logTs}>[{l.ts}] </Text>
        <Text style={styles.logTag}>[{l.tag}] </Text>
        {l.msg}
      </Text>
    ))}
  </View>
);

// ─── Main App ─────────────────────────────────────────────────────────────────
const App = () => {
  const [mediaKey, setMediaKey] = useState(DEFAULT_CONFIG.mediaKey);
  const [ids, setIds] = useState(DEFAULT_CONFIG.ids);
  const [initStatus, setInitStatus] = useState<'idle' | 'success' | 'failed'>('idle');
  const [initError, setInitError] = useState<string | null>(null);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [bannerKey, setBannerKey] = useState(0);
  const [bannerVisible, setBannerVisible] = useState(false);
  const [nativeKey, setNativeKey] = useState(0);
  const [nativeVisible, setNativeVisible] = useState(false);
  const [videoKey, setVideoKey] = useState(0);
  const [videoVisible, setVideoVisible] = useState(false);
  const [adStatuses, setAdStatuses] = useState<Record<AdStatusKey, AdStatus>>(createInitialAdStatuses());

  const updateAdStatus = (key: AdStatusKey, patch: Partial<AdStatus>) => {
    setAdStatuses(prev => ({
      ...prev,
      [key]: {
        ...prev[key],
        ...patch,
      },
    }));
  };

  const resetAdStatus = (key: AdStatusKey, message = 'idle') => {
    setAdStatuses(prev => ({
      ...prev,
      [key]: {
        ...createEmptyAdStatus(),
        lastMessage: message,
      },
    }));
  };

  const addLog = (tag: string, msg: string) =>
    setLogs(prev => [...prev, { ts: now(), tag, msg }]);

  // ── Initialize ──────────────────────────────────────────────────────────────
  const handleInitialize = () => {
    setInitStatus('idle');
    setInitError(null);
    setAdStatuses(createInitialAdStatuses());
    NapSspAd.initialize({
      mediaKey,
      adUnitIds: Object.values(ids),
      logLevel: 'debug',
    })
      .then(() => { setInitStatus('success'); setInitError(null); addLog('INIT', '✅ SDK 초기화 성공'); })
      .catch((e: any) => {
        const message = e?.message ?? String(e);
        setInitStatus('failed');
        setInitError(message);
        addLog('INIT', `❌ ${message}`);
      });
  };

  // ── Interstitial ────────────────────────────────────────────────────────────
  const showInterstitial = async () => {
    resetAdStatus('interstitial', 'requesting');
    const ad = new InterstitialAd(ids.interstitial, { type: 'popup', buttonLeftText: '닫기' });
    ad.addAdEventListener('loaded', () => { addLog('INTER', 'loaded'); updateAdStatus('interstitial', { loaded: true, lastMessage: 'loaded' }); });
    ad.addAdEventListener('loadFailed', e => { addLog('INTER', `loadFailed: ${e.message} (code:${e.nativeCode ?? '-'})`); updateAdStatus('interstitial', { lastMessage: `loadFailed:${e.message}` }); });
    ad.addAdEventListener('opened', () => { addLog('INTER', 'opened'); updateAdStatus('interstitial', { opened: true, lastMessage: 'opened' }); });
    ad.addAdEventListener('impression', () => { addLog('INTER', 'impression'); updateAdStatus('interstitial', { impression: true, lastMessage: 'impression' }); });
    ad.addAdEventListener('clicked', () => addLog('INTER', 'clicked'));
    ad.addAdEventListener('closed', () => { addLog('INTER', 'closed'); ad.destroy(); });
    try {
      addLog('INTER', 'start() start');
      await ad.start();
      addLog('INTER', 'start() resolved');
      updateAdStatus('interstitial', { lastMessage: 'startResolved' });
    } catch (e: any) {
      addLog('INTER', `exception:${e?.message ?? String(e)}`);
      Alert.alert('전면 광고 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  // ── Rewarded ─────────────────────────────────────────────────────────────
  const showRewarded = async () => {
    resetAdStatus('rewarded', 'requesting');
    const ad = new RewardedAd(ids.rewarded, {
      customParams: { userId: 'user_001', session: 'game_stage_1' },
      mute: false,
    });
    ad.addAdEventListener('loaded', () => { addLog('REWARD', 'loaded'); updateAdStatus('rewarded', { loaded: true, lastMessage: 'loaded' }); });
    ad.addAdEventListener('loadFailed', e => { addLog('REWARD', `loadFailed: ${e.message} (code:${e.nativeCode ?? '-'})`); updateAdStatus('rewarded', { lastMessage: `loadFailed:${e.message}` }); });
    ad.addAdEventListener('opened', () => { addLog('REWARD', 'opened'); updateAdStatus('rewarded', { opened: true, lastMessage: 'opened' }); });
    ad.addAdEventListener('impression', () => { addLog('REWARD', 'impression'); updateAdStatus('rewarded', { impression: true, lastMessage: 'impression' }); });
    (ad as any).addAdEventListener('onRewarded', (item: any) => { addLog('REWARD', `🎉 rewarded! type=${item?.type} amount=${item?.amount}`); updateAdStatus('rewarded', { rewarded: true, lastMessage: `rewarded:${item?.amount ?? 1}` }); });
    ad.addAdEventListener('closed', () => { addLog('REWARD', 'closed'); ad.destroy(); });
    try {
      addLog('REWARD', 'start() start');
      await ad.start();
      addLog('REWARD', 'start() resolved');
      updateAdStatus('rewarded', { lastMessage: 'startResolved' });
    } catch (e: any) {
      Alert.alert('리워드 광고 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  // ── InterstitialVideo ───────────────────────────────────────────────────────
  const showInterstitialVideo = async () => {
    resetAdStatus('interstitialVideo', 'requesting');
    const ad = new InterstitialVideoAd(ids.interstitialVideo, { timeout: 20, maxRetryCountInSlot: 0 });
    ad.addAdEventListener('loaded', () => { addLog('IV', 'loaded'); updateAdStatus('interstitialVideo', { loaded: true, lastMessage: 'loaded' }); });
    ad.addAdEventListener('loadFailed', e => { addLog('IV', `loadFailed: ${e.message}`); updateAdStatus('interstitialVideo', { lastMessage: `loadFailed:${e.message}` }); });
    ad.addAdEventListener('opened', () => { addLog('IV', 'opened'); updateAdStatus('interstitialVideo', { opened: true, lastMessage: 'opened' }); });
    ad.addAdEventListener('completed', () => { addLog('IV', '✅ completed'); updateAdStatus('interstitialVideo', { completed: true, lastMessage: 'completed' }); });
    ad.addAdEventListener('skipped', () => addLog('IV', 'skipped'));
    ad.addAdEventListener('closed', () => { addLog('IV', 'closed'); ad.destroy(); });
    try {
      addLog('IV', 'start() start');
      await ad.start();
      addLog('IV', 'start() resolved');
      updateAdStatus('interstitialVideo', { lastMessage: 'startResolved' });
    } catch (e: any) {
      Alert.alert('전면 동영상 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  const statusColor = initStatus === 'success' ? '#4CAF50' : initStatus === 'failed' ? '#F44336' : '#FF9800';
  const initialized = initStatus === 'success';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Nap SSP 통합 테스트 앱</Text>
        <Text style={[styles.status, { color: statusColor }]}>SDK: {initStatus}</Text>
        {initError ? <Text style={styles.errorText}>오류: {initError}</Text> : null}

        <Section title="상단 상태 요약">
          <Text style={styles.statusLine}>INTER_STATUS:{adStatuses.interstitial.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.interstitial.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.interstitial.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>REWARD_STATUS:{adStatuses.rewarded.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.rewarded.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.rewarded.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}:{adStatuses.rewarded.rewarded ? 'REWARDED' : 'NOT_REWARDED'}</Text>
          <Text style={styles.statusLine}>IV_STATUS:{adStatuses.interstitialVideo.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.interstitialVideo.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.interstitialVideo.completed ? 'COMPLETED' : 'NOT_COMPLETED'}</Text>
          <Text style={styles.statusLine}>BANNER_STATUS:{adStatuses.banner.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.banner.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>NATIVE_STATUS:{adStatuses.native.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.native.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>VIDEO_STATUS:{adStatuses.video.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.video.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}:{adStatuses.video.completed ? 'COMPLETED' : 'NOT_COMPLETED'}</Text>
        </Section>

        {/* ── SDK 초기화 설정 ────────────────────── */}
        <Section title="SDK 초기화 설정">
          <View style={styles.idRow}>
            <Text style={styles.idLabel}>Media Key</Text>
            <TextInput
              style={[styles.idInput, mediaKey.trim().length > 0 && !isNumeric(mediaKey) && styles.idInputError]}
              value={mediaKey}
              onChangeText={setMediaKey}
              onBlur={() => {
                if (mediaKey.trim().length > 0 && !isNumeric(mediaKey)) {
                  Alert.alert('입력 오류', 'Media Key는 숫자만 입력 가능합니다.\nnap SSP 파트너사이트에서 발급받은 숫자 키를 입력하세요.', [{ text: '확인' }]);
                }
              }}
              placeholder="Media Key (숫자)"
              keyboardType="numeric"
              autoCapitalize="none"
            />
          </View>
          <IdRow label="배너" value={ids.banner} onChangeText={v => setIds(p => ({ ...p, banner: v }))} />
          <IdRow label="전면" value={ids.interstitial} onChangeText={v => setIds(p => ({ ...p, interstitial: v }))} />
          <IdRow label="리워드" value={ids.rewarded} onChangeText={v => setIds(p => ({ ...p, rewarded: v }))} />
          <IdRow label="네이티브" value={ids.native} onChangeText={v => setIds(p => ({ ...p, native: v }))} />
          <IdRow label="인라인 동영상" value={ids.video} onChangeText={v => setIds(p => ({ ...p, video: v }))} />
          <IdRow label="전면 동영상" value={ids.interstitialVideo} onChangeText={v => setIds(p => ({ ...p, interstitialVideo: v }))} />
          <TouchableOpacity style={[styles.button, { backgroundColor: '#00796B', marginTop: 8 }]} onPress={handleInitialize}>
            <Text style={styles.buttonText}>SDK 초기화</Text>
          </TouchableOpacity>
        </Section>

        {/* ── 1. 배너 ────────────────────────────── */}
        <Section title="1. 배너 광고 (320×50)">
          <View style={styles.adButtonRow}>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#1565C0' }, !initialized && styles.buttonDisabled]} onPress={() => { setBannerVisible(true); setBannerKey(k => k + 1); }}>
              <Text style={styles.buttonText}>{bannerVisible ? '재로드' : '로드'}</Text>
            </TouchableOpacity>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#546E7A' }, !initialized && styles.buttonDisabled]} onPress={() => setBannerVisible(false)}>
              <Text style={styles.buttonText}>언로드</Text>
            </TouchableOpacity>
          </View>
          {bannerVisible && (
            <BannerAd
              key={bannerKey}
              adUnitId={ids.banner}
              size="BANNER_320x50"
              onAdLoaded={() => { addLog('BANNER', 'loaded'); updateAdStatus('banner', { loaded: true, lastMessage: 'loaded' }); }}
              onAdImpression={() => { addLog('BANNER', 'impression'); updateAdStatus('banner', { impression: true, lastMessage: 'impression' }); }}
              onAdFailedToLoad={e => { addLog('BANNER', `failed: ${e.message}`); updateAdStatus('banner', { lastMessage: `failed:${e.message}` }); }}
              onAdClicked={() => addLog('BANNER', 'clicked')}
            />
          )}
        </Section>

        {/* ── 2. 네이티브 ────────────────────────── */}
        <Section title="2. 네이티브 광고">
          <View style={styles.adButtonRow}>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#1565C0' }, !initialized && styles.buttonDisabled]} onPress={() => { setNativeVisible(true); setNativeKey(k => k + 1); }}>
              <Text style={styles.buttonText}>{nativeVisible ? '재로드' : '로드'}</Text>
            </TouchableOpacity>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#546E7A' }, !initialized && styles.buttonDisabled]} onPress={() => setNativeVisible(false)}>
              <Text style={styles.buttonText}>언로드</Text>
            </TouchableOpacity>
          </View>
          {nativeVisible && (
            <NativeAd
              key={nativeKey}
              adUnitId={ids.native}
              style={styles.nativeAd}
              onAdLoaded={() => { addLog('NATIVE', 'loaded'); updateAdStatus('native', { loaded: true, lastMessage: 'loaded' }); }}
              onAdImpression={() => { addLog('NATIVE', 'impression'); updateAdStatus('native', { impression: true, lastMessage: 'impression' }); }}
              onAdFailedToLoad={e => { addLog('NATIVE', `failed: ${e.message}`); updateAdStatus('native', { lastMessage: `failed:${e.message}` }); }}
              onAdClicked={() => addLog('NATIVE', 'clicked')}
            />
          )}
        </Section>

        {/* ── 3. 인라인 동영상 ─────────────────────── */}
        <Section title="3. 인라인 동영상 광고">
          <View style={styles.adButtonRow}>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#1565C0' }, !initialized && styles.buttonDisabled]} onPress={() => { setVideoVisible(true); setVideoKey(k => k + 1); }}>
              <Text style={styles.buttonText}>{videoVisible ? '재로드' : '로드'}</Text>
            </TouchableOpacity>
            <TouchableOpacity disabled={!initialized} style={[styles.adButton, { backgroundColor: '#546E7A' }, !initialized && styles.buttonDisabled]} onPress={() => setVideoVisible(false)}>
              <Text style={styles.buttonText}>언로드</Text>
            </TouchableOpacity>
          </View>
          {videoVisible && (
            <VideoAd
              key={videoKey}
              adUnitId={ids.video}
              style={styles.videoAd}
              onAdLoaded={() => { addLog('VIDEO', 'loaded'); updateAdStatus('video', { loaded: true, lastMessage: 'loaded' }); }}
              onAdImpression={() => { addLog('VIDEO', 'impression'); updateAdStatus('video', { impression: true, lastMessage: 'impression' }); }}
              onAdFailedToLoad={e => { addLog('VIDEO', `failed: ${e.message}`); updateAdStatus('video', { lastMessage: `failed:${e.message}` }); }}
              onAdCompleted={() => { addLog('VIDEO', '✅ completed'); updateAdStatus('video', { completed: true, lastMessage: 'completed' }); }}
              onAdSkipped={() => addLog('VIDEO', 'skipped')}
              onAdClicked={() => addLog('VIDEO', 'clicked')}
            />
          )}
        </Section>

        {/* ── 4. Fullscreen 버튼 ─────────────────── */}
        <Section title="4. 전면/리워드/전면동영상 광고">
          <TouchableOpacity disabled={!initialized} style={[styles.button, { backgroundColor: '#1565C0' }, !initialized && styles.buttonDisabled]} onPress={showInterstitial}>
            <Text style={styles.buttonText}>전면 광고 (popup)</Text>
          </TouchableOpacity>
          <TouchableOpacity disabled={!initialized} style={[styles.button, { backgroundColor: '#6A1B9A' }, !initialized && styles.buttonDisabled]} onPress={showRewarded}>
            <Text style={styles.buttonText}>리워드 동영상</Text>
          </TouchableOpacity>
          <TouchableOpacity disabled={!initialized} style={[styles.button, { backgroundColor: '#BF360C' }, !initialized && styles.buttonDisabled]} onPress={showInterstitialVideo}>
            <Text style={styles.buttonText}>전면 동영상</Text>
          </TouchableOpacity>
        </Section>

        <Section title="광고 응답 상태">
          <Text style={styles.statusLine}>BANNER loaded={String(adStatuses.banner.loaded)} impression={String(adStatuses.banner.impression)} msg={adStatuses.banner.lastMessage}</Text>
          <Text style={styles.statusLine}>BANNER_STATUS:{adStatuses.banner.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.banner.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>NATIVE loaded={String(adStatuses.native.loaded)} impression={String(adStatuses.native.impression)} msg={adStatuses.native.lastMessage}</Text>
          <Text style={styles.statusLine}>NATIVE_STATUS:{adStatuses.native.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.native.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>VIDEO loaded={String(adStatuses.video.loaded)} impression={String(adStatuses.video.impression)} completed={String(adStatuses.video.completed)} msg={adStatuses.video.lastMessage}</Text>
          <Text style={styles.statusLine}>VIDEO_STATUS:{adStatuses.video.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.video.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}:{adStatuses.video.completed ? 'COMPLETED' : 'NOT_COMPLETED'}</Text>
          <Text style={styles.statusLine}>INTER loaded={String(adStatuses.interstitial.loaded)} opened={String(adStatuses.interstitial.opened)} impression={String(adStatuses.interstitial.impression)} msg={adStatuses.interstitial.lastMessage}</Text>
          <Text style={styles.statusLine}>INTER_STATUS:{adStatuses.interstitial.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.interstitial.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.interstitial.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}</Text>
          <Text style={styles.statusLine}>REWARD loaded={String(adStatuses.rewarded.loaded)} opened={String(adStatuses.rewarded.opened)} impression={String(adStatuses.rewarded.impression)} rewarded={String(adStatuses.rewarded.rewarded)} msg={adStatuses.rewarded.lastMessage}</Text>
          <Text style={styles.statusLine}>REWARD_STATUS:{adStatuses.rewarded.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.rewarded.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.rewarded.impression ? 'IMPRESSION' : 'NO_IMPRESSION'}:{adStatuses.rewarded.rewarded ? 'REWARDED' : 'NOT_REWARDED'}</Text>
          <Text style={styles.statusLine}>IV loaded={String(adStatuses.interstitialVideo.loaded)} opened={String(adStatuses.interstitialVideo.opened)} completed={String(adStatuses.interstitialVideo.completed)} msg={adStatuses.interstitialVideo.lastMessage}</Text>
          <Text style={styles.statusLine}>IV_STATUS:{adStatuses.interstitialVideo.loaded ? 'LOADED' : 'NOT_LOADED'}:{adStatuses.interstitialVideo.opened ? 'OPENED' : 'NOT_OPENED'}:{adStatuses.interstitialVideo.completed ? 'COMPLETED' : 'NOT_COMPLETED'}</Text>
        </Section>

        {/* ── 이벤트 로그 ────────────────────────── */}
        <Section title="이벤트 로그">
          <TouchableOpacity style={styles.clearButton} onPress={() => setLogs([])}>
            <Text style={styles.clearButtonText}>로그 초기화</Text>
          </TouchableOpacity>
          <LogView logs={logs} />
        </Section>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FAFAFA' },
  scroll: { padding: 16, paddingBottom: 40 },
  title: { fontSize: 20, fontWeight: 'bold', textAlign: 'center', marginBottom: 4 },
  status: { textAlign: 'center', fontSize: 13, marginBottom: 6 },
  errorText: { textAlign: 'center', fontSize: 11, color: '#D32F2F', marginBottom: 16, paddingHorizontal: 8 },
  section: { marginBottom: 20, backgroundColor: '#fff', borderRadius: 8, padding: 12, elevation: 1, shadowOpacity: 0.06, shadowRadius: 4 },
  sectionTitle: { fontSize: 15, fontWeight: '700', marginBottom: 10, color: '#333' },
  nativeAd: { width: '100%', height: 250 },
  videoAd: { width: '100%', height: 200 },
  button: { borderRadius: 8, padding: 14, alignItems: 'center', marginBottom: 8 },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  adButtonRow: { flexDirection: 'row', gap: 8, marginBottom: 10 },
  adButton: { flex: 1, borderRadius: 8, padding: 10, alignItems: 'center' },
  idRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 6 },
  idLabel: { width: 80, fontSize: 12, color: '#555' },
  idInput: { flex: 1, borderWidth: 1, borderColor: '#DDD', borderRadius: 6, paddingHorizontal: 8, paddingVertical: 4, fontSize: 12 },
  idInputError: { borderColor: '#F44336', backgroundColor: '#FFF3F3' },
  logBox: { backgroundColor: '#1A1A1A', borderRadius: 6, padding: 8, minHeight: 80 },
  logLine: { color: '#E0E0E0', fontSize: 11, marginBottom: 2 },
  logTs: { color: '#888' },
  logTag: { color: '#4FC3F7', fontWeight: 'bold' },
  statusLine: { fontSize: 11, color: '#333', marginBottom: 4 },
  clearButton: { alignSelf: 'flex-end', paddingHorizontal: 10, paddingVertical: 4, backgroundColor: '#EEE', borderRadius: 6, marginBottom: 6 },
  clearButtonText: { fontSize: 12, color: '#555' },
  buttonDisabled: { opacity: 0.35 },
});

export default App;
