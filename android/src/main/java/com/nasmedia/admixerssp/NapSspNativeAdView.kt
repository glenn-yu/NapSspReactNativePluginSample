package com.nasmedia.admixerssp

import android.content.Context
import android.widget.FrameLayout
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext
import com.nasmedia.admixerssp.ads.AMMNativeAdView
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.common.AdMixer
import com.nasmedia.admixerssp.common.nativeads.NativeAdViewBinder
import com.nasmedia.admixerssp.reactnative.R

/**
 * React Native host for [AMMNativeAdView].
 *
 * The creative is rendered into `res/layout/nap_ssp_native_ad.xml` through a
 * [NativeAdViewBinder]. Host apps can override the whole layout by shipping their own
 * `nap_ssp_native_ad.xml` with the same view IDs.
 * https://napmx.github.io/#/android/native/native-ad
 */
class NapSspNativeAdView(context: Context) : FrameLayout(context), LifecycleEventListener {

    private var adView: AMMNativeAdView? = null
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

    fun reload() {
        release()
        currentState = NapSspLoadState.IDLE
        maybeLoad()
    }

    fun destroyNativeAd() {
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
            emitFailure(unit, NapSspAdErrors.NOT_INITIALIZED, "NapSspAd.initialize() must resolve before rendering a native ad.")
            return
        }

        currentState = NapSspLoadState.LOADING

        // Adfit and several other adapters reject a non-Activity context.
        val activityContext = (context as? ThemedReactContext)?.currentActivity ?: context
        val view = AMMNativeAdView(activityContext)

        val listener = NapAdListener(
            onReceived = { _, _ -> handleReceived(unit) },
            onLoadFailed = { code, message -> handleFailure(unit, code, message) },
            onDisplayed = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_IMPRESSION, unit) },
            onClicked = { emitViewEvent(NapSspContracts.VIEW_EVENT_AD_CLICKED, unit) },
        )
        adListener = listener

        view.setAdViewListener(listener)
        view.setViewBinder(buildViewBinder())
        view.setAdInfo(NapSspSdkBridge.applyAdapterConfig(AdInfo.Builder(unit)).build())

        adView = view
        removeAllViews()
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        view.loadAd()
    }

    private fun buildViewBinder(): NativeAdViewBinder =
        NativeAdViewBinder.Builder(R.layout.nap_ssp_native_ad)
            .setIconImageId(R.id.nap_mx_iv_icon)
            .setTitleId(R.id.nap_mx_tv_title)
            .setAdvertiserId(R.id.nap_mx_tv_adv)
            .setDescriptionId(R.id.nap_mx_tv_desc)
            .setMainViewId(R.id.nap_mx_iv_main)
            .setCtaId(R.id.nap_mx_btn_cta)
            .build()

    private fun handleReceived(unit: String) {
        val view = adView
        if (view != null && !view.hasAd) {
            handleFailure(unit, AdMixer.AX_ERR_NO_ADS, "No fill")
            return
        }
        currentState = NapSspLoadState.LOADED
        post(measureAndLayout)
        emitViewEvent(NapSspContracts.VIEW_EVENT_AD_LOADED, unit)
    }

    private fun handleFailure(unit: String, code: Int, message: String?) {
        currentState = NapSspLoadState.FAILED
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            NapSspAdErrors.payload(unit, NapSspContracts.FORMAT_NATIVE_AD, code, message),
        )
    }

    private fun emitFailure(unit: String, code: String, message: String) {
        currentState = NapSspLoadState.FAILED
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_FAILED,
            mapOf(
                "adUnitId" to unit,
                "format" to NapSspContracts.FORMAT_NATIVE_AD,
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun emitViewEvent(eventName: String, unit: String) {
        NapSspEventEmitter.emitViewEvent(
            this,
            eventName,
            mapOf("adUnitId" to unit, "format" to NapSspContracts.FORMAT_NATIVE_AD),
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
