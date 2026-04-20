import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
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

// ─── Default test IDs (replace with real AdUnit IDs) ──────────────────────────
const DEFAULT_MEDIA_KEY = 'TEST_MEDIA_KEY';
const DEFAULT_IDS = {
  banner: 'BANNER_ID',
  interstitial: 'INTER_ID',
  rewarded: 'REWARD_ID',
  native: 'NATIVE_ID',
  video: 'VIDEO_ID',
  interstitialVideo: 'INTER_VIDEO_ID',
};

// ─── Log entry ────────────────────────────────────────────────────────────────
interface LogEntry { ts: string; tag: string; msg: string }
function now() { return new Date().toLocaleTimeString(); }

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
  const [mediaKey, setMediaKey] = useState(DEFAULT_MEDIA_KEY);
  const [ids, setIds] = useState(DEFAULT_IDS);
  const [initStatus, setInitStatus] = useState<'idle' | 'success' | 'failed'>('idle');
  const [logs, setLogs] = useState<LogEntry[]>([]);

  const addLog = (tag: string, msg: string) =>
    setLogs(prev => [...prev, { ts: now(), tag, msg }]);

  // ── Initialize ──────────────────────────────────────────────────────────────
  useEffect(() => {
    NapSspAd.initialize({
      mediaKey,
      adUnitIds: Object.values(ids),
      logLevel: 'debug',
    })
      .then(() => { setInitStatus('success'); addLog('INIT', '✅ SDK 초기화 성공'); })
      .catch((e: any) => { setInitStatus('failed'); addLog('INIT', `❌ ${e?.message ?? e}`); });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Interstitial ────────────────────────────────────────────────────────────
  const showInterstitial = async () => {
    const ad = new InterstitialAd(ids.interstitial, { type: 'popup', buttonLeftText: '닫기' });
    ad.addAdEventListener('loaded', () => addLog('INTER', 'loaded'));
    ad.addAdEventListener('loadFailed', e => addLog('INTER', `loadFailed: ${e.message} (code:${e.nativeCode ?? '-'})`));
    ad.addAdEventListener('opened', () => addLog('INTER', 'opened'));
    ad.addAdEventListener('impression', () => addLog('INTER', 'impression'));
    ad.addAdEventListener('clicked', () => addLog('INTER', 'clicked'));
    ad.addAdEventListener('closed', () => { addLog('INTER', 'closed'); ad.destroy(); });
    try {
      await ad.load();
      await ad.show();
    } catch (e: any) {
      Alert.alert('전면 광고 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  // ── Rewarded ─────────────────────────────────────────────────────────────
  const showRewarded = async () => {
    const ad = new RewardedAd(ids.rewarded, {
      customParams: { userId: 'user_001', session: 'game_stage_1' },
      mute: false,
    });
    ad.addAdEventListener('loaded', () => addLog('REWARD', 'loaded'));
    ad.addAdEventListener('loadFailed', e => addLog('REWARD', `loadFailed: ${e.message} (code:${e.nativeCode ?? '-'})`));
    ad.addAdEventListener('opened', () => addLog('REWARD', 'opened'));
    ad.addAdEventListener('impression', () => addLog('REWARD', 'impression'));
    (ad as any).addAdEventListener('onRewarded', (item: any) => addLog('REWARD', `🎉 rewarded! type=${item?.type} amount=${item?.amount}`));
    ad.addAdEventListener('closed', () => { addLog('REWARD', 'closed'); ad.destroy(); });
    try {
      await ad.load();
      await ad.show();
    } catch (e: any) {
      Alert.alert('리워드 광고 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  // ── InterstitialVideo ───────────────────────────────────────────────────────
  const showInterstitialVideo = async () => {
    const ad = new InterstitialVideoAd(ids.interstitialVideo, { timeout: 20, maxRetryCountInSlot: 0 });
    ad.addAdEventListener('loaded', () => addLog('IV', 'loaded'));
    ad.addAdEventListener('loadFailed', e => addLog('IV', `loadFailed: ${e.message}`));
    ad.addAdEventListener('opened', () => addLog('IV', 'opened'));
    ad.addAdEventListener('completed', () => addLog('IV', '✅ completed'));
    ad.addAdEventListener('skipped', () => addLog('IV', 'skipped'));
    ad.addAdEventListener('closed', () => { addLog('IV', 'closed'); ad.destroy(); });
    try {
      await ad.load();
      await ad.show();
    } catch (e: any) {
      Alert.alert('전면 동영상 오류', e?.message ?? String(e));
      ad.destroy();
    }
  };

  // ─── AdUnit ID editor row ──────────────────────────────────────────────────
  const IdRow = ({ label, field }: { label: string; field: keyof typeof DEFAULT_IDS }) => (
    <View style={styles.idRow}>
      <Text style={styles.idLabel}>{label}</Text>
      <TextInput
        style={styles.idInput}
        value={ids[field]}
        onChangeText={v => setIds(prev => ({ ...prev, [field]: v }))}
        placeholder={`${label} ID`}
        autoCapitalize="none"
      />
    </View>
  );

  const statusColor = initStatus === 'success' ? '#4CAF50' : initStatus === 'failed' ? '#F44336' : '#FF9800';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.title}>Nap SSP 통합 테스트 앱</Text>
        <Text style={[styles.status, { color: statusColor }]}>SDK: {initStatus}</Text>

        {/* ── AdUnit ID 입력 ─────────────────────── */}
        <Section title="AdUnit ID 설정">
          <IdRow label="배너" field="banner" />
          <IdRow label="전면" field="interstitial" />
          <IdRow label="리워드" field="rewarded" />
          <IdRow label="네이티브" field="native" />
          <IdRow label="인라인 동영상" field="video" />
          <IdRow label="전면 동영상" field="interstitialVideo" />
        </Section>

        {/* ── 1. 배너 ────────────────────────────── */}
        <Section title="1. 배너 광고 (320×50)">
          <BannerAd
            adUnitId={ids.banner}
            size="BANNER_320x50"
            onAdLoaded={() => addLog('BANNER', 'loaded')}
            onAdFailedToLoad={e => addLog('BANNER', `failed: ${e.message}`)}
            onAdClicked={() => addLog('BANNER', 'clicked')}
          />
        </Section>

        {/* ── 2. 네이티브 ────────────────────────── */}
        <Section title="2. 네이티브 광고">
          <NativeAd
            adUnitId={ids.native}
            style={styles.nativeAd}
            onAdLoaded={() => addLog('NATIVE', 'loaded')}
            onAdFailedToLoad={e => addLog('NATIVE', `failed: ${e.message}`)}
            onAdClicked={() => addLog('NATIVE', 'clicked')}
          />
        </Section>

        {/* ── 3. 인라인 동영상 ─────────────────────── */}
        <Section title="3. 인라인 동영상 광고">
          <VideoAd
            adUnitId={ids.video}
            style={styles.videoAd}
            onAdLoaded={() => addLog('VIDEO', 'loaded')}
            onAdFailedToLoad={e => addLog('VIDEO', `failed: ${e.message}`)}
            onAdCompleted={() => addLog('VIDEO', '✅ completed')}
            onAdSkipped={() => addLog('VIDEO', 'skipped')}
            onAdClicked={() => addLog('VIDEO', 'clicked')}
          />
        </Section>

        {/* ── 4. Fullscreen 버튼 ─────────────────── */}
        <Section title="4. 전면/리워드/전면동영상 광고">
          <TouchableOpacity style={[styles.button, { backgroundColor: '#1565C0' }]} onPress={showInterstitial}>
            <Text style={styles.buttonText}>전면 광고 (popup)</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, { backgroundColor: '#6A1B9A' }]} onPress={showRewarded}>
            <Text style={styles.buttonText}>리워드 동영상</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, { backgroundColor: '#BF360C' }]} onPress={showInterstitialVideo}>
            <Text style={styles.buttonText}>전면 동영상</Text>
          </TouchableOpacity>
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
  status: { textAlign: 'center', fontSize: 13, marginBottom: 16 },
  section: { marginBottom: 20, backgroundColor: '#fff', borderRadius: 8, padding: 12, elevation: 1, shadowOpacity: 0.06, shadowRadius: 4 },
  sectionTitle: { fontSize: 15, fontWeight: '700', marginBottom: 10, color: '#333' },
  nativeAd: { width: '100%', height: 250 },
  videoAd: { width: '100%', height: 200 },
  button: { borderRadius: 8, padding: 14, alignItems: 'center', marginBottom: 8 },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  idRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 6 },
  idLabel: { width: 80, fontSize: 12, color: '#555' },
  idInput: { flex: 1, borderWidth: 1, borderColor: '#DDD', borderRadius: 6, paddingHorizontal: 8, paddingVertical: 4, fontSize: 12 },
  logBox: { backgroundColor: '#1A1A1A', borderRadius: 6, padding: 8, minHeight: 80 },
  logLine: { color: '#E0E0E0', fontSize: 11, marginBottom: 2 },
  logTs: { color: '#888' },
  logTag: { color: '#4FC3F7', fontWeight: 'bold' },
  clearButton: { alignSelf: 'flex-end', paddingHorizontal: 10, paddingVertical: 4, backgroundColor: '#EEE', borderRadius: 6, marginBottom: 6 },
  clearButtonText: { fontSize: 12, color: '#555' },
});

export default App;
