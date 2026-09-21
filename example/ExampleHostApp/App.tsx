/**
 * react-native-nap-ssp integration reference.
 *
 * Each panel walks the full lifecycle a media app needs:
 *   initialize → load → callback → show/render → success or failure → reload → cleanup
 *
 * Every callback is written to the shared event log, so you can see exactly which SDK events fire
 * (and which do not) on each platform.
 */
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
  Platform,
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
  type AdError,
  type AdViewHandle,
  type NapSspStatus,
} from 'react-native-nap-ssp';

import {adConfig, allAdUnitIds} from './adConfig';

type LogFn = (message: string) => void;
type AdViewRef = React.RefObject<AdViewHandle | null>;

function describeError(error: unknown): string {
  const adError = error as AdError | undefined;
  if (adError?.code) {
    const native = adError.nativeCode !== undefined ? ` (native ${adError.nativeCode})` : '';
    return `${adError.code}${native}: ${adError.message}`;
  }
  return error instanceof Error ? error.message : String(error);
}

// ─────────────────────────────────────────────────────────────────────────────
// UI primitives
// ─────────────────────────────────────────────────────────────────────────────

function Button({
  title,
  onPress,
  disabled,
  primary,
}: {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  primary?: boolean;
}) {
  return (
    <TouchableOpacity
      style={[styles.button, primary && styles.buttonPrimary, disabled && styles.buttonDisabled]}
      onPress={onPress}
      disabled={disabled}>
      <Text style={[styles.buttonText, primary && styles.buttonTextInverted]}>{title}</Text>
    </TouchableOpacity>
  );
}

function Section({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {subtitle ? <Text style={styles.sectionSubtitle}>{subtitle}</Text> : null}
      {children}
    </View>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Full-screen formats: interstitial, interstitial video, rewarded
// ─────────────────────────────────────────────────────────────────────────────

/** The subset of the ad API this panel drives. */
interface FullScreenAdController {
  load(): Promise<void>;
  show(): Promise<void>;
  start(): Promise<void>;
  cancelLoad(): Promise<void>;
  isReady(): Promise<boolean>;
  destroy(): void;
}

interface PanelHandlers {
  onLoaded: () => void;
  onLoadFailed: (error: AdError) => void;
  onOpened: () => void;
  onImpression: () => void;
  onClicked: () => void;
  onClosed: () => void;
}

/**
 * `subscribe` is supplied per call site so the ad keeps its concrete type — the three ad classes
 * expose different event maps, so a shared union would not be callable.
 */
function FullScreenAdPanel<T extends FullScreenAdController>({
  title,
  subtitle,
  adUnitId,
  create,
  subscribe,
  log,
}: {
  title: string;
  subtitle: string;
  adUnitId: string;
  create: () => T;
  subscribe: (ad: T, handlers: PanelHandlers) => Array<() => void>;
  log: LogFn;
}) {
  const adRef = useRef<T | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);

  // One ad instance per panel: created with the component, destroyed with it. destroy() is what
  // releases the native ad and the SDK's server-config listener.
  useEffect(() => {
    const ad = create();
    adRef.current = ad;

    const unsubscribers = subscribe(ad, {
      onLoaded: () => {
        setLoaded(true);
        log(`${title} ▸ loaded`);
      },
      onLoadFailed: (error) => {
        setLoaded(false);
        log(`${title} ▸ loadFailed — ${describeError(error)}`);
      },
      onOpened: () => log(`${title} ▸ opened`),
      onImpression: () => log(`${title} ▸ impression`),
      onClicked: () => log(`${title} ▸ clicked`),
      onClosed: () => {
        setLoaded(false);
        log(`${title} ▸ closed`);
      },
    });

    return () => {
      unsubscribers.forEach((unsubscribe) => unsubscribe());
      ad.destroy();
      adRef.current = null;
    };
    // `create` and `subscribe` are stable for the lifetime of the panel.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const run = useCallback(
    async (label: string, action: (ad: T) => Promise<void>) => {
      const ad = adRef.current;
      if (!ad) {
        return;
      }
      setBusy(true);
      log(`${title} ▸ ${label}…`);
      try {
        await action(ad);
        log(`${title} ▸ ${label} resolved`);
      } catch (error) {
        log(`${title} ▸ ${label} rejected — ${describeError(error)}`);
      } finally {
        setBusy(false);
      }
    },
    [log, title],
  );

  return (
    <Section title={title} subtitle={`${subtitle} · adUnitId ${adUnitId}`}>
      <Text style={styles.state}>
        state: {busy ? 'working…' : loaded ? 'ready to show' : 'idle'}
      </Text>
      <View style={styles.buttonRow}>
        <Button title="Load" onPress={() => run('load()', (ad) => ad.load())} disabled={busy} />
        <Button
          title="Show"
          primary
          onPress={() => run('show()', (ad) => ad.show())}
          disabled={busy || !loaded}
        />
        <Button
          title="Load + Show"
          onPress={() => run('start()', (ad) => ad.start())}
          disabled={busy}
        />
      </View>
      <View style={styles.buttonRow}>
        <Button
          title="Cancel load"
          onPress={() => run('cancelLoad()', (ad) => ad.cancelLoad())}
          disabled={busy}
        />
        <Button
          title="isReady()"
          onPress={() =>
            run('isReady()', async (ad) => {
              const ready = await ad.isReady();
              setLoaded(ready);
              log(`${title} ▸ isReady() = ${ready}`);
            })
          }
          disabled={busy}
        />
      </View>
    </Section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Inline formats: banner, native, inline video
// ─────────────────────────────────────────────────────────────────────────────

function InlineAdPanel({
  title,
  subtitle,
  adUnitId,
  log,
  render,
}: {
  title: string;
  subtitle: string;
  adUnitId: string;
  log: LogFn;
  render: (handlers: {
    ref: AdViewRef;
    onAdLoaded: () => void;
    onAdFailedToLoad: (error: AdError) => void;
    onAdClicked: () => void;
    onAdImpression: () => void;
  }) => React.ReactNode;
}) {
  const ref = useRef<AdViewHandle | null>(null);
  const [state, setState] = useState<'loading' | 'loaded' | 'failed'>('loading');

  const handlers = useMemo(
    () => ({
      ref,
      onAdLoaded: () => {
        setState('loaded');
        log(`${title} ▸ loaded`);
      },
      onAdFailedToLoad: (error: AdError) => {
        setState('failed');
        log(`${title} ▸ failed — ${describeError(error)}`);
      },
      onAdClicked: () => log(`${title} ▸ clicked`),
      onAdImpression: () => log(`${title} ▸ impression`),
    }),
    [log, title],
  );

  return (
    <Section title={title} subtitle={`${subtitle} · adUnitId ${adUnitId}`}>
      <Text style={styles.state}>state: {state}</Text>
      <View style={styles.adSlot}>{render(handlers)}</View>
      <View style={styles.buttonRow}>
        <Button
          title="Reload"
          onPress={() => {
            setState('loading');
            log(`${title} ▸ reload()`);
            ref.current?.reload();
          }}
        />
      </View>
    </Section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// App
// ─────────────────────────────────────────────────────────────────────────────

const MAX_LOG_LINES = 60;

export default function App() {
  const [log, setLog] = useState<string[]>([]);
  const [status, setStatus] = useState<NapSspStatus | null>(null);
  const [initError, setInitError] = useState<string | null>(null);

  const append = useCallback((message: string) => {
    const line = `${new Date().toLocaleTimeString()}  ${message}`;
    console.log(`[NapSspExample] ${line}`);
    setLog((previous) => [line, ...previous].slice(0, MAX_LOG_LINES));
  }, []);

  // Step 1 — initialize once, before any ad is requested.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        // iOS: resolve tracking permission first so the first request can carry the IDFA.
        if (Platform.OS === 'ios') {
          append(`ATT status: ${await NapSspAd.requestTrackingAuthorization()}`);
        }

        const result = await NapSspAd.initialize({
          mediaKey: adConfig.mediaKey,
          adUnitIds: allAdUnitIds,
          logLevel: __DEV__ ? 'verbose' : 'error',
          // Privacy travels with initialize() so it reaches every network before it starts.
          privacy: {childDirected: false},
          // Android only; resolves false on iOS.
          testMode: __DEV__,
        });

        if (!cancelled) {
          setStatus(result);
          append('NapSspAd.initialize() resolved');
        }
      } catch (error) {
        if (!cancelled) {
          setInitError(describeError(error));
          append(`NapSspAd.initialize() failed — ${describeError(error)}`);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [append]);

  const initialized = status?.initialized === true;

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.heading}>react-native-nap-ssp</Text>
        <Text style={styles.subheading}>
          {Platform.OS} · mediaKey {adConfig.mediaKey}
        </Text>

        <Section title="1. SDK status">
          {initError ? (
            <Text style={styles.error}>initialize() failed: {initError}</Text>
          ) : (
            <Text style={styles.state}>
              {initialized
                ? `initialized · testMode ${String(status?.testMode ?? false)}${
                    status?.testModeSupported === false ? ' (not supported on iOS)' : ''
                  }`
                : 'initializing…'}
            </Text>
          )}
          <View style={styles.buttonRow}>
            <Button
              title="Refresh getStatus()"
              onPress={async () => {
                const next = await NapSspAd.getStatus();
                setStatus(next);
                append(`getStatus() → privacy ${JSON.stringify(next.privacy ?? {})}`);
              }}
            />
          </View>
        </Section>

        {initialized ? (
          <>
            <InlineAdPanel
              title="2. Banner"
              subtitle="loads on mount"
              adUnitId={adConfig.banner}
              log={append}
              render={(h) => (
                <BannerAd
                  ref={h.ref}
                  adUnitId={adConfig.banner}
                  size="BANNER_320x50"
                  onAdLoaded={h.onAdLoaded}
                  onAdFailedToLoad={h.onAdFailedToLoad}
                  onAdClicked={h.onAdClicked}
                  onAdImpression={h.onAdImpression}
                />
              )}
            />

            <InlineAdPanel
              title="3. Native"
              subtitle="rendered by the SDK into the plugin layout"
              adUnitId={adConfig.nativeAd}
              log={append}
              render={(h) => (
                <NativeAd
                  ref={h.ref}
                  adUnitId={adConfig.nativeAd}
                  style={styles.nativeAd}
                  onAdLoaded={h.onAdLoaded}
                  onAdFailedToLoad={h.onAdFailedToLoad}
                  onAdClicked={h.onAdClicked}
                  onAdImpression={h.onAdImpression}
                />
              )}
            />

            <InlineAdPanel
              title="4. Inline video"
              subtitle="in-feed video"
              adUnitId={adConfig.video}
              log={append}
              render={(h) => (
                <VideoAd
                  ref={h.ref}
                  adUnitId={adConfig.video}
                  style={styles.videoAd}
                  onAdLoaded={h.onAdLoaded}
                  onAdFailedToLoad={h.onAdFailedToLoad}
                  onAdClicked={h.onAdClicked}
                  onAdImpression={h.onAdImpression}
                  onAdCompleted={() => append('4. Inline video ▸ completed')}
                  onAdSkipped={() => append('4. Inline video ▸ skipped')}
                />
              )}
            />

            <FullScreenAdPanel
              title="5. Interstitial"
              subtitle="full-screen image/HTML"
              adUnitId={adConfig.interstitial}
              log={append}
              create={() =>
                new InterstitialAd(adConfig.interstitial, {closeButtonTouchAreaRatio: 0.6})
              }
              subscribe={(ad, h) => [
                ad.addAdEventListener('loaded', h.onLoaded),
                ad.addAdEventListener('loadFailed', h.onLoadFailed),
                ad.addAdEventListener('opened', h.onOpened),
                ad.addAdEventListener('impression', h.onImpression),
                ad.addAdEventListener('clicked', h.onClicked),
                ad.addAdEventListener('closed', h.onClosed),
              ]}
            />

            <FullScreenAdPanel
              title="6. Interstitial video"
              subtitle="skipped never fires on iOS"
              adUnitId={adConfig.interstitialVideo}
              log={append}
              create={() => new InterstitialVideoAd(adConfig.interstitialVideo, {timeout: 20})}
              subscribe={(ad, h) => [
                ad.addAdEventListener('loaded', h.onLoaded),
                ad.addAdEventListener('loadFailed', h.onLoadFailed),
                ad.addAdEventListener('opened', h.onOpened),
                ad.addAdEventListener('impression', h.onImpression),
                ad.addAdEventListener('clicked', h.onClicked),
                ad.addAdEventListener('closed', h.onClosed),
                ad.addAdEventListener('completed', () => append('6. Interstitial video ▸ completed')),
                ad.addAdEventListener('skipped', () => append('6. Interstitial video ▸ skipped')),
              ]}
            />

            <FullScreenAdPanel
              title="7. Rewarded"
              subtitle="grant the reward from the rewarded event"
              adUnitId={adConfig.rewarded}
              log={append}
              create={() =>
                new RewardedAd(adConfig.rewarded, {customParams: {userId: 'example-user'}})
              }
              subscribe={(ad, h) => [
                ad.addAdEventListener('loaded', h.onLoaded),
                ad.addAdEventListener('loadFailed', h.onLoadFailed),
                ad.addAdEventListener('opened', h.onOpened),
                ad.addAdEventListener('impression', h.onImpression),
                ad.addAdEventListener('clicked', h.onClicked),
                ad.addAdEventListener('closed', h.onClosed),
                ad.addAdEventListener('completed', () => append('7. Rewarded ▸ completed')),
                ad.addAdEventListener('skipped', () => append('7. Rewarded ▸ skipped')),
                // transactionId is what reconciles with the S2S reward callback.
                ad.addAdEventListener('rewarded', (reward) =>
                  append(`7. Rewarded ▸ rewarded — transactionId=${reward.transactionId ?? 'n/a'}`),
                ),
              ]}
            />
          </>
        ) : null}

        <Section title="Event log" subtitle="newest first">
          {log.length === 0 ? (
            <Text style={styles.state}>No events yet.</Text>
          ) : (
            log.map((line, index) => (
              <Text key={`${index}-${line}`} style={styles.logLine}>
                {line}
              </Text>
            ))
          )}
        </Section>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {backgroundColor: '#F5F6F8', flex: 1},
  content: {padding: 16, paddingBottom: 48},
  heading: {color: '#111827', fontSize: 22, fontWeight: '700'},
  subheading: {color: '#6B7280', fontSize: 13, marginBottom: 16, marginTop: 2},
  section: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E5E7EB',
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 12,
    padding: 14,
  },
  sectionTitle: {color: '#111827', fontSize: 16, fontWeight: '600'},
  sectionSubtitle: {color: '#6B7280', fontSize: 12, marginTop: 2},
  state: {color: '#374151', fontSize: 13, marginTop: 8},
  error: {color: '#B91C1C', fontSize: 13, marginTop: 8},
  adSlot: {alignItems: 'center', marginTop: 12},
  nativeAd: {minHeight: 280, width: '100%'},
  videoAd: {height: 200, width: '100%'},
  buttonRow: {flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12},
  button: {
    backgroundColor: '#F3F4F6',
    borderColor: '#D1D5DB',
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  buttonPrimary: {backgroundColor: '#2563EB', borderColor: '#2563EB'},
  buttonDisabled: {opacity: 0.45},
  buttonText: {color: '#111827', fontSize: 13, fontWeight: '600'},
  buttonTextInverted: {color: '#FFFFFF'},
  logLine: {color: '#4B5563', fontSize: 11, marginTop: 4},
});
