package com.nasmedia.admixerssp

import android.app.Activity
import android.content.Context
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.ads.BaseInterstitialManager
import com.nasmedia.admixerssp.common.AdMixer
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
        var showing: Boolean = false,
    )

    @Volatile private var invalidated = false
    // Only retained during the synchronous SDK show invocation; later failures remain events.
    private val showPromises = ConcurrentHashMap<String, Promise>()
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

            if (ads[adUnitId]?.showing == true) {
                promise.reject(NapSspAdErrors.NOT_READY, "An ad is already being presented for this adUnitId.")
                return@runOnUi
            }

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
                    present(adUnitId, ad, activity, promise)
                    return@runOnUi
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
            if (ad == null || !ad.isReady || ads[adUnitId]?.showing == true) {
                promise.reject(
                    NapSspAdErrors.NOT_READY,
                    "No ${format.replace('_', ' ')} ad is ready for adUnitId \"$adUnitId\". Await load() first.",
                )
                return@runOnUi
            }
            present(adUnitId, ad, activity, promise)
        }
    }

    fun isLoaded(rawAdUnitId: String, promise: Promise) {
        runOnUi(promise) { promise.resolve(ads[rawAdUnitId.trim()]?.ad?.isReady == true) }
    }

    fun isLoading(rawAdUnitId: String, promise: Promise) {
        runOnUi(promise) { promise.resolve(ads[rawAdUnitId.trim()]?.ad?.isLoading == true) }
    }

    /** Cancels an in-flight load. A showing ad is preserved (SDK no-ops when not loading). */
    fun cancelLoad(rawAdUnitId: String, promise: Promise) {
        val adUnitId = rawAdUnitId.trim()
        runOnUi(promise) {
            val ad = ads[adUnitId]?.ad
            if (ad?.isLoading == true) {
                // SDK 2.5.1 synchronously reports AX_ERR_CANCELLED through onLoadFailed.
                // That callback owns the event and the pending load/start rejection.
                ad.cancelLoad()
                // Settle defensively if the SDK completed without delivering its callback.
                if (loadPromises.containsKey(adUnitId) || startPromises.containsKey(adUnitId)) {
                    handleLoadFailed(adUnitId, AdMixer.AX_ERR_CANCELLED, "Load cancelled.")
                }
            }
            promise.resolve(null)
        }
    }

    fun destroy(rawAdUnitId: String, promise: Promise) {
        val adUnitId = rawAdUnitId.trim()
        runOnUi(promise) {
            loadPromises.remove(adUnitId)?.reject(NapSspAdErrors.DESTROYED, "Destroyed before the load completed")
            startPromises.remove(adUnitId)?.reject(NapSspAdErrors.DESTROYED, "Destroyed before the ad was shown")
            release(ads.remove(adUnitId))
            clearState(adUnitId)
            promise.resolve(null)
        }
    }

    fun invalidate() {
        invalidated = true
        UiThreadUtil.runOnUiThread {
            loadPromises.values.toList().forEach { it.reject(NapSspAdErrors.LOAD_CANCELLED, "React context invalidated") }
            startPromises.values.toList().forEach { it.reject(NapSspAdErrors.LOAD_CANCELLED, "React context invalidated") }
            loadPromises.clear()
            startPromises.clear()
            val pending = ads.values.toList()
            ads.keys.toList().forEach(::clearState)
            ads.clear()
            pending.forEach(::release)
        }
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private fun obtain(adUnitId: String, activity: Activity, options: ReadableMap?): BaseInterstitialManager<*> {
        ads[adUnitId]?.let {
            if (it.ad.isReady || it.ad.isLoading || it.showing) return it.ad
            // A fresh request must pick up its own options and current Activity.
            release(ads.remove(adUnitId))
        }

        val ad = createAd(activity)
        val builder = AdInfo.Builder(adUnitId)
        applyOptions(builder, options)
        NapSspSdkBridge.applyAdapterConfig(builder)
        ad.setAdInfo(builder.build())

        val listener = NapAdListener(
            dispatch = { callback -> UiThreadUtil.runOnUiThread { callback() } },
            isActive = { !invalidated && ads[adUnitId]?.ad === ad },
            onReceived = { _, _ -> handleReceived(adUnitId) },
            onLoadFailed = { code, message -> handleLoadFailed(adUnitId, code, message) },
            onShowFailed = { code, message -> handleShowFailed(adUnitId, code, message) },
            onDisplayed = {
                markState(adUnitId, NapSspLoadState.SHOWN)
                emit(NapSspContracts.EVENT_AD_OPENED, adUnitId)
                emit(NapSspContracts.EVENT_AD_IMPRESSION, adUnitId)
            },
            onClicked = { emit(NapSspContracts.EVENT_AD_CLICKED, adUnitId) },
            onClosed = {
                ads[adUnitId]?.showing = false
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

    private fun handleReceived(adUnitId: String) {
        val entry = ads[adUnitId] ?: return
        if (!entry.ad.hasInterstitial) {
            // The waterfall completed but nothing was filled.
            handleLoadFailed(adUnitId, com.nasmedia.admixerssp.common.AdMixer.AX_ERR_NO_ADS, "No fill")
            return
        }

        markState(adUnitId, NapSspLoadState.LOADED)
        emit(NapSspContracts.EVENT_AD_LOADED, adUnitId)

        val startPromise = startPromises.remove(adUnitId)
        if (startPromise != null) {
            val activity = requireActivity(startPromise) ?: return
            present(adUnitId, entry.ad, activity, startPromise)
            return
        }
        loadPromises.remove(adUnitId)?.resolve(null)
    }

    private fun handleLoadFailed(adUnitId: String, code: Int, message: String?) {
        markState(adUnitId, if (code == AdMixer.AX_ERR_CANCELLED) NapSspLoadState.IDLE else NapSspLoadState.FAILED)
        val payload = NapSspAdErrors.payload(adUnitId, format, code, message)
        emitPayload(NapSspContracts.EVENT_AD_FAILED, payload)

        val rejection = payload["code"] as String
        val reason = payload["message"] as String
        startPromises.remove(adUnitId)?.reject(rejection, reason)
        loadPromises.remove(adUnitId)?.reject(rejection, reason)
    }

    private fun handleShowFailed(adUnitId: String, code: Int, message: String?) {
        ads[adUnitId]?.showing = false
        val payload = NapSspAdErrors.payload(adUnitId, format, code, message)
        showPromises.remove(adUnitId)?.reject(payload["code"] as String, payload["message"] as String)
        val isReady = ads[adUnitId]?.ad?.isReady == true
        markState(adUnitId, if (isReady) NapSspLoadState.LOADED else NapSspLoadState.FAILED)
        emitPayload(
            NapSspContracts.EVENT_AD_FAILED,
            payload + mapOf("phase" to "show", "isReady" to isReady),
        )
    }

    private fun present(adUnitId: String, ad: BaseInterstitialManager<*>, activity: Activity, promise: Promise) {
        showPromises[adUnitId] = promise
        ads[adUnitId]?.showing = true
        try {
            ad.show(activity)
            // Preserve the public invocation-complete contract while honoring synchronous failure.
            showPromises.remove(adUnitId)?.resolve(null)
        } catch (error: Throwable) {
            handleShowFailed(adUnitId, AdMixer.AX_ERR_SHOW_FAILED, error.message ?: error.toString())
        }
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
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            promise.reject(
                NapSspAdErrors.ACTIVITY_REQUIRED,
                "Full-screen ads need a foreground Activity. Retry once the app is resumed.",
            )
            return null
        }
        return activity
    }

    private inline fun runOnUi(promise: Promise, crossinline block: () -> Unit) {
        UiThreadUtil.runOnUiThread {
            try {
                if (invalidated) {
                    promise.reject(NapSspAdErrors.DESTROYED, "React context invalidated")
                    return@runOnUiThread
                }
                block()
            } catch (error: Throwable) {
                loadPromises.entries.removeAll { it.value === promise }
                startPromises.entries.removeAll { it.value === promise }
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
            NapSspContracts.FORMAT_INTERSTITIAL_VIDEO -> NapSspSdkBridge.markInterstitialVideoState(adUnitId, state)
            NapSspContracts.FORMAT_REWARDED -> NapSspSdkBridge.markRewardedState(adUnitId, state)
            else -> NapSspSdkBridge.markInterstitialState(adUnitId, state)
        }
    }

    private fun clearState(adUnitId: String) {
        when (format) {
            NapSspContracts.FORMAT_INTERSTITIAL_VIDEO -> NapSspSdkBridge.clearInterstitialVideo(adUnitId)
            NapSspContracts.FORMAT_REWARDED -> NapSspSdkBridge.clearRewarded(adUnitId)
            else -> NapSspSdkBridge.clearInterstitial(adUnitId)
        }
    }
}
