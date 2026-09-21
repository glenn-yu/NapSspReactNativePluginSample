package com.nasmedia.admixerssp

import android.app.Activity
import android.content.Context
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.ads.BaseInterstitialManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared implementation for the three full-screen formats (interstitial, rewarded, interstitial
 * video). All of them extend [BaseInterstitialManager], so a single host can drive them.
 *
 * Every SDK call is marshalled onto the UI thread: the managers install a `Handler` on the calling
 * looper and `show()` needs a foreground Activity.
 */
internal class NapSspFullScreenAdHost(
    private val reactContext: ReactApplicationContext,
    private val format: String,
    private val createAd: (Context) -> BaseInterstitialManager<*>,
    private val applyOptions: (AdInfo.Builder, ReadableMap?) -> Unit = { _, _ -> },
) {
    private class Entry(
        val ad: BaseInterstitialManager<*>,
        // AMMBannerView-style weak listener retention does not apply here, but keeping the strong
        // reference makes the teardown path explicit.
        var listener: NapAdListener?,
    )

    private val ads = ConcurrentHashMap<String, Entry>()
    private val loadPromises = ConcurrentHashMap<String, Promise>()
    private val startPromises = ConcurrentHashMap<String, Promise>()

    fun load(rawAdUnitId: String, options: ReadableMap?, promise: Promise) {
        performLoad(rawAdUnitId, options, promise, autoShow = false)
    }

    /** Load, then present as soon as the ad arrives. */
    fun start(rawAdUnitId: String, options: ReadableMap?, promise: Promise) {
        performLoad(rawAdUnitId, options, promise, autoShow = true)
    }

    private fun performLoad(rawAdUnitId: String, options: ReadableMap?, promise: Promise, autoShow: Boolean) {
        val adUnitId = normalize(rawAdUnitId, promise) ?: return
        runOnUi(promise) {
            val activity = requireActivity(promise) ?: return@runOnUi

            val reason = if (autoShow) "Superseded by start()" else "Superseded by load()"
            loadPromises.remove(adUnitId)?.reject(NapSspAdErrors.LOAD_CANCELLED, reason)
            startPromises.remove(adUnitId)?.reject(NapSspAdErrors.LOAD_CANCELLED, reason)

            val pending = if (autoShow) startPromises else loadPromises
            pending[adUnitId] = promise

            val ad = obtain(adUnitId, activity, options)

            // `loadAd()` drops a re-request without a callback when an ad is already READY or a
            // load is still in flight. Settling those cases here is what keeps the promise from
            // hanging forever. https://napmx.github.io/#/android/native/api-reference
            if (ad.isReady) {
                pending.remove(adUnitId)
                markState(adUnitId, NapSspLoadState.LOADED)
                if (autoShow) {
                    markState(adUnitId, NapSspLoadState.SHOWN)
                    ad.show(activity)
                }
                promise.resolve(null)
                return@runOnUi
            }

            if (ad.isLoading) {
                // The in-flight load's callback will settle the promise we just stored.
                return@runOnUi
            }

            markState(adUnitId, NapSspLoadState.LOADING)
            ad.loadAd()
        }
    }

    fun show(rawAdUnitId: String, promise: Promise) {
        val adUnitId = normalize(rawAdUnitId, promise) ?: return
        runOnUi(promise) {
            val activity = requireActivity(promise) ?: return@runOnUi
            val ad = ads[adUnitId]?.ad
            if (ad == null || !ad.isReady) {
                promise.reject(
                    NapSspAdErrors.NOT_READY,
                    "No ${format.replace('_', ' ')} ad is ready for adUnitId \"$adUnitId\". Await load() first.",
                )
                return@runOnUi
            }
            markState(adUnitId, NapSspLoadState.SHOWN)
            ad.show(activity)
            promise.resolve(null)
        }
    }

    fun isLoaded(rawAdUnitId: String, promise: Promise) {
        promise.resolve(ads[rawAdUnitId.trim()]?.ad?.isReady == true)
    }

    fun isLoading(rawAdUnitId: String, promise: Promise) {
        promise.resolve(ads[rawAdUnitId.trim()]?.ad?.isLoading == true)
    }

    /** Cancels an in-flight load. A showing ad is preserved (SDK no-ops when not loading). */
    fun cancelLoad(rawAdUnitId: String, promise: Promise) {
        val adUnitId = rawAdUnitId.trim()
        runOnUi(promise) {
            ads[adUnitId]?.ad?.cancelLoad()
            loadPromises.remove(adUnitId)?.reject(NapSspAdErrors.LOAD_CANCELLED, "Cancelled by cancelLoad()")
            startPromises.remove(adUnitId)?.reject(NapSspAdErrors.LOAD_CANCELLED, "Cancelled by cancelLoad()")
            clearState(adUnitId)
            promise.resolve(null)
        }
    }

    fun destroy(rawAdUnitId: String, promise: Promise) {
        val adUnitId = rawAdUnitId.trim()
        loadPromises.remove(adUnitId)?.reject(NapSspAdErrors.DESTROYED, "Destroyed before the load completed")
        startPromises.remove(adUnitId)?.reject(NapSspAdErrors.DESTROYED, "Destroyed before the ad was shown")
        runOnUi(promise) {
            release(ads.remove(adUnitId))
            clearState(adUnitId)
            promise.resolve(null)
        }
    }

    fun invalidate() {
        loadPromises.values.toList().forEach { it.reject(NapSspAdErrors.LOAD_CANCELLED, "React context invalidated") }
        startPromises.values.toList().forEach { it.reject(NapSspAdErrors.LOAD_CANCELLED, "React context invalidated") }
        loadPromises.clear()
        startPromises.clear()

        val pending = ads.values.toList()
        ads.clear()
        UiThreadUtil.runOnUiThread { pending.forEach(::release) }
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private fun obtain(adUnitId: String, activity: Activity, options: ReadableMap?): BaseInterstitialManager<*> {
        ads[adUnitId]?.let { return it.ad }

        val ad = createAd(activity)
        val builder = AdInfo.Builder(adUnitId)
        applyOptions(builder, options)
        NapSspSdkBridge.applyAdapterConfig(builder)
        ad.setAdInfo(builder.build())

        val listener = NapAdListener(
            onReceived = { _, _ -> handleReceived(adUnitId, activity) },
            onLoadFailed = { code, message -> handleLoadFailed(adUnitId, code, message) },
            onShowFailed = { code, message -> handleShowFailed(adUnitId, code, message) },
            onDisplayed = {
                emit(NapSspContracts.EVENT_AD_OPENED, adUnitId)
                emit(NapSspContracts.EVENT_AD_IMPRESSION, adUnitId)
            },
            onClicked = { emit(NapSspContracts.EVENT_AD_CLICKED, adUnitId) },
            onClosed = {
                markState(adUnitId, NapSspLoadState.IDLE)
                emit(NapSspContracts.EVENT_AD_CLOSED, adUnitId)
            },
            onCompleted = { emit(NapSspContracts.EVENT_VIDEO_COMPLETED, adUnitId) },
            onSkipped = { emit(NapSspContracts.EVENT_VIDEO_SKIPPED, adUnitId) },
            onRewarded = { transactionId ->
                emit(
                    NapSspContracts.EVENT_REWARDED,
                    adUnitId,
                    mapOf(
                        "transactionId" to transactionId,
                        // The SDK deliberately does not carry an amount or a currency: units differ
                        // per network and are not trustworthy through mediation. Kept for 0.4.x
                        // compatibility only — use transactionId to reconcile with the S2S callback.
                        "type" to "reward",
                        "amount" to 1,
                    ),
                )
            },
        )
        ad.setAdListener(listener)

        ads[adUnitId] = Entry(ad, listener)
        return ad
    }

    private fun handleReceived(adUnitId: String, activity: Activity) {
        val entry = ads[adUnitId]
        if (entry != null && !entry.ad.hasInterstitial) {
            // The waterfall completed but nothing was filled.
            handleLoadFailed(adUnitId, com.nasmedia.admixerssp.common.AdMixer.AX_ERR_NO_ADS, "No fill")
            return
        }

        markState(adUnitId, NapSspLoadState.LOADED)
        emit(NapSspContracts.EVENT_AD_LOADED, adUnitId)

        val startPromise = startPromises.remove(adUnitId)
        if (startPromise != null) {
            markState(adUnitId, NapSspLoadState.SHOWN)
            entry?.ad?.show(activity)
            startPromise.resolve(null)
            return
        }
        loadPromises.remove(adUnitId)?.resolve(null)
    }

    private fun handleLoadFailed(adUnitId: String, code: Int, message: String?) {
        markState(adUnitId, NapSspLoadState.FAILED)
        val payload = NapSspAdErrors.payload(adUnitId, format, code, message)
        emitPayload(NapSspContracts.EVENT_AD_FAILED, payload)

        val rejection = payload["code"] as String
        val reason = payload["message"] as String
        startPromises.remove(adUnitId)?.reject(rejection, reason)
        loadPromises.remove(adUnitId)?.reject(rejection, reason)
    }

    private fun handleShowFailed(adUnitId: String, code: Int, message: String?) {
        markState(adUnitId, NapSspLoadState.FAILED)
        emitPayload(
            NapSspContracts.EVENT_AD_FAILED,
            NapSspAdErrors.payload(adUnitId, format, code, message) + ("phase" to "show"),
        )
    }

    private fun release(entry: Entry?) {
        if (entry == null) return
        runCatching { entry.ad.setAdListener(null) }
        runCatching { entry.ad.stop() }
        entry.listener = null
    }

    private fun normalize(rawAdUnitId: String?, promise: Promise): String? {
        val adUnitId = rawAdUnitId?.trim().orEmpty()
        if (adUnitId.isEmpty()) {
            promise.reject(NapSspAdErrors.INVALID_AD_UNIT, "A non-empty adUnitId is required.")
            return null
        }
        if (NapSspSdkBridge.getConfiguration() == null) {
            promise.reject(
                NapSspAdErrors.NOT_INITIALIZED,
                "NapSspAd.initialize() must resolve before requesting a $format ad.",
            )
            return null
        }
        return adUnitId
    }

    private fun requireActivity(promise: Promise): Activity? {
        val activity = reactContext.currentActivity
        if (activity == null) {
            promise.reject(
                NapSspAdErrors.ACTIVITY_REQUIRED,
                "Full-screen ads need a foreground Activity. Retry once the app is resumed.",
            )
        }
        return activity
    }

    private inline fun runOnUi(promise: Promise, crossinline block: () -> Unit) {
        UiThreadUtil.runOnUiThread {
            try {
                block()
            } catch (error: Throwable) {
                promise.reject(NapSspAdErrors.LOAD_FAILED, error.message ?: error.toString(), error)
            }
        }
    }

    private fun emit(eventName: String, adUnitId: String, extra: Map<String, Any?> = emptyMap()) {
        emitPayload(eventName, mapOf("adUnitId" to adUnitId, "format" to format) + extra)
    }

    private fun emitPayload(eventName: String, payload: Map<String, Any?>) {
        NapSspEventEmitter.emitModuleEvent(reactContext, eventName, payload)
    }

    private fun markState(adUnitId: String, state: NapSspLoadState) {
        when (format) {
            NapSspContracts.FORMAT_REWARDED -> NapSspSdkBridge.markRewardedState(adUnitId, state)
            else -> NapSspSdkBridge.markInterstitialState(adUnitId, state)
        }
    }

    private fun clearState(adUnitId: String) {
        when (format) {
            NapSspContracts.FORMAT_REWARDED -> NapSspSdkBridge.clearRewarded(adUnitId)
            else -> NapSspSdkBridge.clearInterstitial(adUnitId)
        }
    }
}
