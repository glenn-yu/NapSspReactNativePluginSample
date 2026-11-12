/**
 * Contract smoke test for the built package in `lib/`.
 *
 * Runs in plain Node against a hand-written `react-native` mock, so it verifies the public API,
 * the argument validation and the native-event routing without a device.
 */
const assert = require('assert');
const Module = require('module');

const originalLoad = Module._load;

// ── react-native mock ────────────────────────────────────────────────────────

/** NativeEventEmitter listeners share one global RCTDeviceEventEmitter bus. */
const nativeListeners = new Map();
/** Every native method call, so the test can assert on what reached the bridge. */
const calls = [];

function record(moduleName, method, args) {
  calls.push({ moduleName, method, args });
}

function callsTo(moduleName, method) {
  return calls.filter((call) => call.moduleName === moduleName && call.method === method);
}

function emitNative(moduleName, eventName, payload) {
  const handlers = nativeListeners.get(eventName) ?? [];
  handlers.forEach((handler) => handler(payload));
}

function fullScreenModuleMock(name) {
  const loaded = new Set();
  return {
    addListener: () => undefined,
    removeListeners: () => undefined,
    load: async (adUnitId, options) => {
      record(name, 'load', [adUnitId, options]);
      loaded.add(adUnitId);
    },
    start: async (adUnitId, options) => {
      record(name, 'start', [adUnitId, options]);
    },
    show: async (adUnitId) => {
      record(name, 'show', [adUnitId]);
      if (!loaded.has(adUnitId)) {
        throw { code: 'nap_ssp_ad_not_ready', message: 'not ready' };
      }
    },
    isLoaded: async (adUnitId) => loaded.has(adUnitId),
    isLoading: async () => false,
    cancelLoad: async (adUnitId) => {
      record(name, 'cancelLoad', [adUnitId]);
      loaded.delete(adUnitId);
    },
    destroy: (adUnitId) => {
      record(name, 'destroy', [adUnitId]);
      loaded.delete(adUnitId);
    },
  };
}

const registeredViews = new Set(['NapSspBannerView', 'NapSspNativeAdView', 'NapSspVideoAdView']);

const mockReactNative = {
  NativeModules: {
    NapSspModule: {
      addListener: () => undefined,
      removeListeners: () => undefined,
      initialize: async (config) => {
        record('NapSspModule', 'initialize', [config]);
        return { initialized: true, platform: 'android', privacy: config.privacy ?? {} };
      },
      setLogLevel: async (level) => record('NapSspModule', 'setLogLevel', [level]),
      setPrivacyConsent: async (consent) => record('NapSspModule', 'setPrivacyConsent', [consent]),
      setCoppa: async (enabled) => record('NapSspModule', 'setCoppa', [enabled]),
      setTestMode: async (enabled) => {
        record('NapSspModule', 'setTestMode', [enabled]);
        return true;
      },
      setTestDeviceIds: async (ids) => {
        record('NapSspModule', 'setTestDeviceIds', [ids]);
        return true;
      },
      getStatus: async () => ({ initialized: true, platform: 'android', testMode: true }),
      requestTrackingAuthorization: async () => 'authorized',
    },
    NapSspInterstitial: fullScreenModuleMock('NapSspInterstitial'),
    NapSspRewarded: fullScreenModuleMock('NapSspRewarded'),
    NapSspInterstitialVideo: fullScreenModuleMock('NapSspInterstitialVideo'),
  },
  NativeEventEmitter: class NativeEventEmitter {
    constructor(nativeModule) {
      this.moduleName =
        Object.keys(mockReactNative.NativeModules).find(
          (key) => mockReactNative.NativeModules[key] === nativeModule,
        ) ?? 'unknown';
    }

    addListener(eventName, handler) {
      const key = eventName;
      const handlers = nativeListeners.get(key) ?? [];
      handlers.push(handler);
      nativeListeners.set(key, handlers);
      return {
        remove: () => {
          const current = nativeListeners.get(key) ?? [];
          nativeListeners.set(
            key,
            current.filter((entry) => entry !== handler),
          );
        },
      };
    }
  },
  Platform: { OS: 'android' },
  UIManager: {
    getViewManagerConfig: (name) =>
      registeredViews.has(name) ? { Commands: { reload: 1 } } : null,
    dispatchViewManagerCommand: (...args) => record('UIManager', 'dispatchViewManagerCommand', args),
  },
  findNodeHandle: () => 1,
  requireNativeComponent: (name) => name,
  StyleSheet: { hairlineWidth: 1, create: (styles) => styles },
  Text: 'Text',
  View: 'View',
};

Module._load = function patchedLoad(request, parent, isMain) {
  if (request === 'react-native') {
    return mockReactNative;
  }
  return originalLoad.call(this, request, parent, isMain);
};

// ── tests ────────────────────────────────────────────────────────────────────

const React = require('react');
const lib = require('../lib');

async function testExports() {
  const expected = [
    'NapSspAd',
    'BannerAd',
    'NativeAd',
    'VideoAd',
    'InterstitialAd',
    'RewardedAd',
    'InterstitialVideoAd',
    'isNativeModuleAvailable',
    'isNativeViewAvailable',
    'NativeModuleNames',
    'normalizeAdError',
  ];
  expected.forEach((key) => assert.ok(key in lib, `missing export: ${key}`));
}

async function testInitializeValidation() {
  await assert.rejects(() => lib.NapSspAd.initialize(), /config object/);
  await assert.rejects(
    () => lib.NapSspAd.initialize({ mediaKey: '  ', adUnitIds: ['1'] }),
    /non-empty mediaKey/,
  );
  await assert.rejects(
    () => lib.NapSspAd.initialize({ mediaKey: '1', adUnitIds: [] }),
    /at least one adUnitId/,
  );
  // Ad unit ids come from the partner site and are always numeric.
  await assert.rejects(
    () => lib.NapSspAd.initialize({ mediaKey: '1', adUnitIds: ['banner-id'] }),
    /numeric adUnitIds/,
  );
}

async function testListenersBeforeSetup() {
  const { globalEvents } = require('../lib/events');
  const removed = globalEvents.addListener('unusedBeforeSetup', () => assert.fail('removed listener fired'));
  removed();
  const ad = new lib.RewardedAd('before-setup');
  let count = 0;
  ad.addAdEventListener('rewarded', () => count++);
  globalEvents.setup('missingModule');
  assert.strictEqual(nativeListeners.get('onRewarded')?.length ?? 0, 0);
  globalEvents.setup('NapSspModule');
  globalEvents.setup('NapSspRewarded');
  assert.strictEqual(nativeListeners.get('unusedBeforeSetup')?.length ?? 0, 0);
  assert.strictEqual(nativeListeners.get('onRewarded').length, 1);
  emitNative('NapSspRewarded', 'onRewarded', { adUnitId: 'before-setup', format: 'rewarded' });
  assert.strictEqual(count, 1);
  ad.destroy();
  assert.strictEqual(nativeListeners.get('onRewarded').length, 0);
}

async function testGlobalEventRouting() {
  const interstitial = new lib.InterstitialAd('shared');
  const rewarded = new lib.RewardedAd('shared');
  const video = new lib.InterstitialVideoAd('shared');
  const counts = { interstitial: 0, rewarded: 0, video: 0, reward: 0 };
  interstitial.addAdEventListener('loaded', () => counts.interstitial++);
  rewarded.addAdEventListener('loaded', () => counts.rewarded++);
  video.addAdEventListener('loaded', () => counts.video++);
  rewarded.addAdEventListener('rewarded', () => counts.reward++);
  assert.strictEqual(nativeListeners.get('onAdLoaded').length, 1);
  for (const format of ['interstitial', 'rewarded', 'interstitial_video']) {
    emitNative('ignored', 'onAdLoaded', { adUnitId: 'shared', format });
  }
  emitNative('ignored', 'onRewarded', { adUnitId: 'shared', format: 'rewarded' });
  assert.deepStrictEqual(counts, { interstitial: 1, rewarded: 1, video: 1, reward: 1 });
  interstitial.destroy();
  rewarded.destroy();
  video.destroy();
  assert.strictEqual(nativeListeners.get('onAdLoaded').length, 0);
  const again = new lib.InterstitialAd('shared');
  let seen = 0;
  again.addAdEventListener('loaded', () => seen++);
  // Older native versions may omit format; retain delivery for those payloads.
  emitNative('ignored', 'onAdLoaded', { adUnitId: 'shared' });
  assert.strictEqual(seen, 1);
  again.destroy();
  assert.strictEqual(nativeListeners.get('onAdLoaded').length, 0);
}

async function testInitialize() {
  const status = await lib.NapSspAd.initialize({
    mediaKey: '9001',
    adUnitIds: ['1000001', '1000002', '1000003'],
    logLevel: 'debug',
    privacy: { childDirected: false, gdprConsent: true },
    testMode: true,
  });

  assert.strictEqual(status.initialized, true);
  assert.ok(lib.NapSspAd.isInitialized());

  const [call] = callsTo('NapSspModule', 'initialize');
  assert.ok(call, 'initialize should reach the native module');
  // Privacy and test settings travel with initialize() so they land before the SDK starts.
  assert.deepStrictEqual(call.args[0].privacy, { childDirected: false, gdprConsent: true });
  assert.strictEqual(call.args[0].testMode, true);

  const config = lib.NapSspAd.getConfig();
  assert.strictEqual(config.mediaKey, '9001');
  assert.strictEqual(config.adUnitIds.length, 3);
  config.adUnitIds.push('mutated');
  assert.strictEqual(lib.NapSspAd.getConfig().adUnitIds.length, 3, 'getConfig must return a copy');
}

async function testPrivacyAndTestMode() {
  await lib.NapSspAd.setPrivacyConsent({ childDirected: true, ccpaDoNotSell: true });
  assert.deepStrictEqual(callsTo('NapSspModule', 'setPrivacyConsent').at(-1).args[0], {
    childDirected: true,
    ccpaDoNotSell: true,
  });

  await assert.rejects(() => lib.NapSspAd.setPrivacyConsent(null), /consent object/);

  assert.strictEqual(await lib.NapSspAd.setTestMode(true), true);
  assert.strictEqual(await lib.NapSspAd.setTestDeviceIds(['AAAA-BBBB']), true);
  await assert.rejects(() => lib.NapSspAd.setTestDeviceIds('AAAA'), /array/);

  await lib.NapSspAd.setLogLevel('error');
  assert.strictEqual(callsTo('NapSspModule', 'setLogLevel').at(-1).args[0], 'error');

  assert.strictEqual(await lib.NapSspAd.requestTrackingAuthorization(), 'authorized');
}

async function testInterstitialLifecycle() {
  const ad = new lib.InterstitialAd('1000001', { closeButtonTouchAreaRatio: 0.6 });

  const seen = [];
  ad.addAdEventListener('loaded', () => seen.push('loaded'));
  ad.addAdEventListener('opened', () => seen.push('opened'));
  ad.addAdEventListener('closed', () => seen.push('closed'));

  assert.strictEqual(ad.isLoaded(), false);

  await ad.load();
  assert.deepStrictEqual(callsTo('NapSspInterstitial', 'load').at(-1).args, [
    '1000001',
    { closeButtonTouchAreaRatio: 0.6 },
  ]);

  // Native drives readiness through events.
  emitNative('NapSspInterstitial', 'onAdLoaded', { adUnitId: '1000001', format: 'interstitial' });
  assert.strictEqual(ad.isLoaded(), true);
  assert.strictEqual(await ad.isReady(), true);

  await ad.show();
  emitNative('NapSspInterstitial', 'onAdOpened', { adUnitId: '1000001' });
  assert.strictEqual(ad.isLoaded(), false);
  emitNative('NapSspInterstitial', 'onAdClosed', { adUnitId: '1000001' });
  assert.strictEqual(ad.isLoaded(), false);
  assert.deepStrictEqual(seen, ['loaded', 'opened', 'closed']);

  // Events for another ad unit must not leak across instances.
  const other = new lib.InterstitialAd('1000002');
  let otherSaw = false;
  other.addAdEventListener('loaded', () => {
    otherSaw = true;
  });
  emitNative('NapSspInterstitial', 'onAdLoaded', { adUnitId: '1000001' });
  assert.strictEqual(otherSaw, false, 'events must be filtered by adUnitId');
  other.destroy();

  ad.destroy();
  assert.ok(callsTo('NapSspInterstitial', 'destroy').some((call) => call.args[0] === '1000001'));
  await assert.rejects(() => ad.load(), /destroyed/);
}

async function testShowPhaseFailureKeepsLoadedState() {
  const ad = new lib.InterstitialAd('1000003');
  const errors = [];
  ad.addAdEventListener('loadFailed', (error) => errors.push(error));

  emitNative('NapSspInterstitial', 'onAdLoaded', { adUnitId: '1000003' });
  assert.strictEqual(ad.isLoaded(), true);

  emitNative('NapSspInterstitial', 'onAdFailedToLoad', {
    adUnitId: '1000003',
    phase: 'show',
    code: 'nap_ssp_show_failed',
    message: 'show failed',
    nativeCode: -5,
  });

  assert.strictEqual(errors.length, 1);
  assert.strictEqual(errors[0].code, 'nap_ssp_show_failed');
  assert.strictEqual(errors[0].nativeCode, -5);

  // If native discarded the ad on show failure, JS readiness must follow native state.
  emitNative('NapSspInterstitial', 'onAdFailedToLoad', {
    adUnitId: '1000003', format: 'interstitial', phase: 'show', isReady: false,
    code: 'nap_ssp_show_failed', message: 'ad discarded',
  });
  assert.strictEqual(ad.isLoaded(), false);
  assert.strictEqual(errors.at(-1).phase, 'show');
  emitNative('NapSspInterstitial', 'onAdLoaded', { adUnitId: '1000003', format: 'interstitial' });
  emitNative('NapSspInterstitial', 'onAdFailedToLoad', {
    adUnitId: '1000003', format: 'interstitial', phase: 'show', isReady: true,
    code: 'nap_ssp_show_failed', message: 'ad still ready',
  });
  assert.strictEqual(ad.isLoaded(), true);

  // A load failure does clear it.
  emitNative('NapSspInterstitial', 'onAdFailedToLoad', {
    adUnitId: '1000003',
    code: 'nap_ssp_no_ads',
    message: 'no fill',
  });
  assert.strictEqual(ad.isLoaded(), false);
  ad.destroy();
}

async function testRewardedPayload() {
  const ad = new lib.RewardedAd('1000001', { customParams: { userId: 'u-1' } });

  const rewards = [];
  // Both the short name and the legacy 'onRewarded' spelling must work.
  ad.addAdEventListener('rewarded', (payload) => rewards.push(payload));
  const unsubscribe = ad.addAdEventListener('onRewarded', (payload) => rewards.push(payload));
  assert.strictEqual(typeof unsubscribe, 'function');

  emitNative('NapSspRewarded', 'onRewarded', {
    adUnitId: '1000001',
    transactionId: 'tx-123',
    type: 'reward',
    amount: 1,
  });

  assert.strictEqual(rewards.length, 2);
  assert.strictEqual(rewards[0].transactionId, 'tx-123');

  const completed = [];
  ad.addAdEventListener('completed', () => completed.push(true));
  emitNative('NapSspRewarded', 'onVideoCompleted', { adUnitId: '1000001' });
  assert.strictEqual(completed.length, 1);

  unsubscribe();
  ad.destroy();
}

async function testCancelLoad() {
  const ad = new lib.InterstitialVideoAd('1000002', { timeout: 15 });
  await ad.load();
  await ad.cancelLoad();
  assert.ok(callsTo('NapSspInterstitialVideo', 'cancelLoad').some((c) => c.args[0] === '1000002'));
  assert.strictEqual(ad.isLoaded(), false);
  ad.destroy();
}

async function testCancelKeepsReadyAd() {
  const ad = new lib.InterstitialAd('1000010');
  await ad.load();
  emitNative('NapSspInterstitial', 'onAdLoaded', { adUnitId: '1000010' });
  const native = mockReactNative.NativeModules.NapSspInterstitial;
  const originalCancel = native.cancelLoad;
  native.cancelLoad = async () => {};
  try {
    await ad.cancelLoad();
    assert.strictEqual(ad.isLoaded(), true);
  } finally {
    native.cancelLoad = originalCancel;
    ad.destroy();
  }
}

async function testDestroyedAdDoesNotRegainReadiness() {
  const ad = new lib.InterstitialAd('1000011');
  const native = mockReactNative.NativeModules.NapSspInterstitial;
  const original = native.isLoaded;
  let resolveReady;
  native.isLoaded = () => new Promise((resolve) => { resolveReady = resolve; });
  try {
    const pending = ad.isReady();
    ad.destroy();
    resolveReady(true);
    assert.strictEqual(await pending, false);
    assert.strictEqual(ad.isLoaded(), false);
  } finally {
    native.isLoaded = original;
    ad.destroy();
  }
}

async function testCancelReadinessDoesNotResurrectDestroyedAd() {
  const ad = new lib.InterstitialAd('1000012');
  const native = mockReactNative.NativeModules.NapSspInterstitial;
  const original = native.isLoaded;
  let resolveReady;
  native.isLoaded = () => new Promise((resolve) => { resolveReady = resolve; });
  try {
    const pending = ad.cancelLoad();
    await Promise.resolve(); // Let cancellation reach the deferred readiness query.
    ad.destroy();
    resolveReady(true);
    await pending;
    assert.strictEqual(ad.isLoaded(), false);
  } finally {
    native.isLoaded = original;
    ad.destroy();
  }
}

async function testAdUnitIdValidation() {
  assert.throws(() => new lib.InterstitialAd(''), /non-empty adUnitId/);
  assert.throws(() => new lib.RewardedAd('   '), /non-empty adUnitId/);
  assert.throws(() => new lib.InterstitialVideoAd(undefined), /non-empty adUnitId/);
}

async function testInlineViews() {
  // Views resolve the registered native component; creating the element must not throw.
  [lib.BannerAd, lib.NativeAd, lib.VideoAd].forEach((Component) => {
    const element = React.createElement(Component, { adUnitId: '1000001' });
    assert.ok(element && element.type, 'inline ad component should produce an element');
  });

  assert.ok(lib.isNativeViewAvailable('NapSspBannerView'));
  assert.ok(!lib.isNativeViewAvailable('NapSspMissingView'));
  assert.ok(lib.isNativeModuleAvailable(lib.NativeModuleNames.napSsp));
}

async function testNormalizeAdError() {
  assert.deepStrictEqual(lib.normalizeAdError('boom'), {
    code: 'nap_ssp_error',
    message: 'boom',
  });

  const nativeError = lib.normalizeAdError({
    code: 'napssp_show_failed', message: 'show failed', phase: 'show',
    details: { underlyingCode: 17, underlyingDomain: 'network' },
  });
  assert.strictEqual(nativeError.phase, 'show');
  assert.strictEqual(nativeError.details.underlyingCode, 17);
  assert.strictEqual(nativeError.details.underlyingDomain, 'network');

  const normalized = lib.normalizeAdError({ code: 'x', message: 'm', nativeCode: 7 });
  assert.strictEqual(normalized.code, 'x');
  assert.strictEqual(normalized.nativeCode, 7);
}

async function main() {
  const tests = [
    testExports,
    testInitializeValidation,
    testListenersBeforeSetup,
    testInitialize,
    testGlobalEventRouting,
    testPrivacyAndTestMode,
    testInterstitialLifecycle,
    testShowPhaseFailureKeepsLoadedState,
    testRewardedPayload,
    testCancelLoad,
    testCancelKeepsReadyAd,
    testDestroyedAdDoesNotRegainReadiness,
    testCancelReadinessDoesNotResurrectDestroyedAd,
    testAdUnitIdValidation,
    testInlineViews,
    testNormalizeAdError,
  ];

  for (const test of tests) {
    await test();
    console.log(`  ✓ ${test.name}`);
  }

  console.log(`smoke-test: ok (${tests.length} checks)`);
}

main().catch((error) => {
  console.error('smoke-test failed');
  console.error(error);
  process.exit(1);
});
