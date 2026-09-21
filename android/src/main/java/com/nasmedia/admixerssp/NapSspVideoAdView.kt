package com.nasmedia.admixerssp

import android.content.Context
import android.widget.FrameLayout
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext
import com.nasmedia.admixerssp.ads.AMMVideoView
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.common.AdMixer

/**
 * React Native host for [AMMVideoView] (inline video).
 * https://napmx.github.io/#/android/native/video
 */
class NapSspVideoAdView(context: Context) : FrameLayout(context), LifecycleEventListener {

    private var adView: AMMVideoView? = null
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

    /** Retry the waterfall once when the first pass returns no fill. */
    var isRetry: Boolean = false

    fun reload() {
        release()
        currentState = NapSspLoadState.IDLE
        maybeLoad()
    }

    fun destroyVideoAd() {
        release()
        currentState = NapSspLoadState.DESTROYED
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
        release()
        super.onDetachedFromWindow()
    }

    override fun onHostResume() {
        adView?.onResume()
    }

    override fun onHostPause() {
        adView?.onPause()
    }

    override fun onHostDestroy() {
        release()
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private fun maybeLoad() {
        if (!attachedToWindow) return
        if (currentState == NapSspLoadState.LOADING || currentState == NapSspLoadState.LOADED) return

        val unit = adUnitId ?: return

        if (NapSspSdkBridge.getConfiguration() == null) {
            emitFailure(unit, NapSspAdErrors.NOT_INITIALIZED, "NapSspAd.initialize() must resolve before rendering a video ad.")
            return
        }

        currentState = NapSspLoadState.LOADING

        val activityContext = (context as? ThemedReactContext)?.currentActivity ?: context
        val view = AMMVideoView(activityContext)

        val listener = NapAdListener(
            onReceived = { _, _ -> handleReceived(unit) },
            onLoadFailed = { code, message -> handleFailure(unit, code, message) },
            onDisplayed = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_IMPRESSION, unit) },
            onClicked = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_CLICKED, unit) },
            onCompleted = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_COMPLETED, unit) },
            onSkipped = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_SKIPPED, unit) },
        )
        adListener = listener

        view.setAdViewListener(listener)
        view.setAdInfo(NapSspSdkBridge.applyAdapterConfig(AdInfo.Builder(unit)).build())

        adView = view
        removeAllViews()
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        view.loadAd()
    }

    private fun handleReceived(unit: String) {
        currentState = NapSspLoadState.LOADED
        post(measureAndLayout)
        emitViewEvent(NapSspContracts.VIEW_EVENT_AD_LOADED, unit)
    }

    private fun handleFailure(unit: String, code: Int, message: String?) {
        if (isRetry && currentState == NapSspLoadState.LOADING && code == AdMixer.AX_ERR_NO_ADS) {
            currentState = NapSspLoadState.IDLE
            isRetry = false
            post { reload() }
            return
        }

        currentState = NapSspLoadState.FAILED
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            NapSspAdErrors.payload(unit, NapSspContracts.FORMAT_VIDEO, code, message),
        )
    }

    private fun emitFailure(unit: String, code: String, message: String) {
        currentState = NapSspLoadState.FAILED
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            mapOf(
                "adUnitId" to unit,
                "format" to NapSspContracts.FORMAT_VIDEO,
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun emitViewEvent(eventName: String, unit: String) {
        NapSspEventEmitter.emitViewEvent(
            this,
            eventName,
            mapOf("adUnitId" to unit, "format" to NapSspContracts.FORMAT_VIDEO),
        )
    }

    private fun release() {
        adView?.let { view ->
            runCatching { view.setAdViewListener(null) }
            runCatching { view.onPause() }
            runCatching { view.stop() }
        }
        adView = null
        adListener = null
        removeAllViews()
    }
}
