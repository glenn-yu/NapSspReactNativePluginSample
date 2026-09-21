package com.nasmedia.admixerssp

import android.content.Context
import android.util.TypedValue
import android.widget.FrameLayout
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext
import com.nasmedia.admixerssp.ads.AMMBannerView
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.common.AdMixer

/**
 * React Native host for [AMMBannerView].
 *
 * The creative size is decided by the ad unit's server configuration, not by the client; the `size`
 * prop only seeds a minimum height so the row does not collapse before the first fill.
 * https://napmx.github.io/#/android/native/banner
 */
class NapSspBannerView(context: Context) : FrameLayout(context), LifecycleEventListener {

    private var adView: AMMBannerView? = null

    // AMMBannerView keeps the listener in a WeakReference — a local would be collected and the
    // callbacks would silently stop arriving.
    private var adListener: NapAdListener? = null

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE
    private var attachedToWindow = false

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        layout(left, top, right, bottom)
    }

    var adUnitId: String? = null
        set(value) {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
            if (normalized == field) return
            field = normalized
            reload()
        }

    /** Layout hint only — the served size comes from the ad unit configuration. */
    var size: String = DEFAULT_SIZE
        set(value) {
            val normalized = value.trim().takeIf { it.isNotEmpty() } ?: DEFAULT_SIZE
            if (normalized == field) return
            field = normalized
            applyMinimumHeight()
        }

    var autoLoad: Boolean = true
        set(value) {
            val wasDisabled = !field
            field = value
            if (value && wasDisabled) reload()
        }

    init {
        applyMinimumHeight()
    }

    /** Destroys the current ad and requests a fresh one. */
    fun reload() {
        releaseAdView()
        currentState = NapSspLoadState.IDLE
        maybeLoad()
    }

    fun destroyBanner() {
        releaseAdView()
        currentState = NapSspLoadState.DESTROYED
        adUnitId?.let { NapSspSdkBridge.clearBanner(it) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
        maybeLoad()
    }

    override fun onDetachedFromWindow() {
        attachedToWindow = false
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        releaseAdView()
        super.onDetachedFromWindow()
    }

    override fun onHostResume() {
        adView?.onResume()
    }

    override fun onHostPause() {
        adView?.onPause()
    }

    override fun onHostDestroy() {
        releaseAdView()
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private fun maybeLoad() {
        if (!autoLoad || !attachedToWindow) return
        if (currentState == NapSspLoadState.LOADING || currentState == NapSspLoadState.LOADED) return

        val unit = adUnitId
        if (unit.isNullOrBlank()) {
            // Nothing to do yet — the prop arrives on the next UI batch.
            return
        }

        if (NapSspSdkBridge.getConfiguration() == null) {
            emitFailure(unit, NapSspAdErrors.NOT_INITIALIZED, "NapSspAd.initialize() must resolve before rendering a banner.")
            return
        }

        currentState = NapSspLoadState.LOADING
        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.LOADING)

        val activityContext = (context as? ThemedReactContext)?.currentActivity ?: context
        val view = AMMBannerView(activityContext)
        val listener = NapAdListener(
            onReceived = { _, _ -> handleReceived(unit) },
            onLoadFailed = { code, message -> handleFailure(unit, code, message) },
            onDisplayed = {
                emitViewEvent(NapSspContracts.VIEW_EVENT_AD_IMPRESSION, unit)
            },
            onClicked = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_CLICKED, unit) },
        )
        adListener = listener

        // The inline views take Object here, not AdListener.
        view.setAdViewListener(listener)
        view.setAdInfo(NapSspSdkBridge.applyAdapterConfig(AdInfo.Builder(unit)).build())

        adView = view
        removeAllViews()
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        view.loadAd()
    }

    private fun handleReceived(unit: String) {
        val view = adView
        if (view != null && !view.hasAd) {
            handleFailure(unit, AdMixer.AX_ERR_NO_ADS, "No fill")
            return
        }

        currentState = NapSspLoadState.LOADED
        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.LOADED)
        post(measureAndLayout)
        emitViewEvent(NapSspContracts.VIEW_EVENT_AD_LOADED, unit, mapOf("size" to size))
    }

    private fun handleFailure(unit: String, code: Int, message: String?) {
        currentState = NapSspLoadState.FAILED
        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.FAILED)
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            NapSspAdErrors.payload(unit, NapSspContracts.FORMAT_BANNER, code, message),
        )
    }

    private fun emitFailure(unit: String, code: String, message: String) {
        currentState = NapSspLoadState.FAILED
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            mapOf(
                "adUnitId" to unit,
                "format" to NapSspContracts.FORMAT_BANNER,
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun emitViewEvent(eventName: String, unit: String, extra: Map<String, Any?> = emptyMap()) {
        NapSspEventEmitter.emitViewEvent(
            this,
            eventName,
            mapOf("adUnitId" to unit, "format" to NapSspContracts.FORMAT_BANNER) + extra,
        )
    }

    private fun releaseAdView() {
        adView?.let { view ->
            runCatching { view.setAdViewListener(null) }
            runCatching { view.onPause() }
            runCatching { view.stop() }
        }
        adView = null
        adListener = null
        removeAllViews()
    }

    private fun applyMinimumHeight() {
        val heightDp = parseHeightDp(size) ?: return
        minimumHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightDp.toFloat(),
            resources.displayMetrics,
        ).toInt()
    }

    companion object {
        private const val DEFAULT_SIZE = "BANNER_320x50"

        private val NAMED_HEIGHTS = mapOf(
            "LARGE_BANNER" to 100,
            "MEDIUM_RECTANGLE" to 250,
            "SMART_BANNER" to 50,
        )

        private val SIZE_PATTERN = Regex("BANNER_[1-9]\\d*[xX]([1-9]\\d*)")

        internal fun parseHeightDp(value: String): Int? {
            NAMED_HEIGHTS[value.uppercase()]?.let { return it }
            return SIZE_PATTERN.matchEntire(value.uppercase())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
    }
}
