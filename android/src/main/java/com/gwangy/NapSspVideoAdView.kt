package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.react.uimanager.ThemedReactContext

class NapSspVideoAdView(context: Context) : FrameLayout(context) {
    private val placeholderLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#FFEBEE"))
        setPadding(32, 32, 32, 32)
    }

    private val titleTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#C62828"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        text = "NapSsp Video Ad"
    }

    private val adUnitIdTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#D32F2F"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        setPadding(0, 8, 0, 8)
    }

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE
    private var adViewInstance: View? = null

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            adUnitIdTextView.text = field ?: "<unset>"
            maybeAutoLoad()
        }

    var isRetry: Boolean = false

    init {
        placeholderLayout.addView(titleTextView)
        placeholderLayout.addView(adUnitIdTextView)
        addView(placeholderLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        isClickable = true
        isFocusable = true
        setOnClickListener {
            val id = adUnitId
            if (!id.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to id, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }
        }
    }

    fun destroyVideoAd() {
        val id = adUnitId
        if (!id.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_CLOSED,
                mapOf("adUnitId" to id, "format" to NapSspContracts.FORMAT_VIDEO),
            )
        }
        currentState = NapSspLoadState.DESTROYED
        adViewInstance?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        adViewInstance = null
        removeAllViews()
    }

    private fun maybeAutoLoad() {
        val normalizedAdUnitId = adUnitId
        if (normalizedAdUnitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to null,
                    "format" to NapSspContracts.FORMAT_VIDEO,
                    "code" to "NAP_SSP_INVALID_AD_UNIT",
                    "message" to "Video adUnitId is required",
                ),
            )
            return
        }

        if (currentState == NapSspLoadState.LOADED) return

        currentState = NapSspLoadState.LOADING

        var vendorLoaded = false
        try {
            tryAttachVendorVideoView(normalizedAdUnitId)
            vendorLoaded = true
        } catch (_: Throwable) {
            vendorLoaded = false
        }

        if (!vendorLoaded) {
            // fallback placeholder
            postDelayed({
                currentState = NapSspLoadState.LOADED
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_LOADED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }, 300)
        }
    }

    private fun tryAttachVendorVideoView(normalizedAdUnitId: String) {
        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) throw UnsupportedOperationException("vendor SDK disabled")

        val videoAdViewClass = Class.forName("com.nasmedia.admixerssp.ads.VideoAdView")
        val adInfoBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        val builder = adInfoBuilderClass.getConstructor(String::class.java).newInstance(normalizedAdUnitId)
        try {
            adInfoBuilderClass.getMethod("isRetry", Boolean::class.javaPrimitiveType).invoke(builder, isRetry)
        } catch (_: Throwable) {}
        try {
            adInfoBuilderClass.getMethod("setIsUseMediation", Boolean::class.java).invoke(builder, true)
        } catch (_: Throwable) {}
        val adInfo = adInfoBuilderClass.getMethod("build").invoke(builder)

        val videoAdView = videoAdViewClass.getConstructor(android.content.Context::class.java).newInstance(context)
        adViewInstance = videoAdView as View

        videoAdViewClass.getMethod("setAdInfo", adInfoClass).invoke(videoAdView, adInfo)

        val listener = java.lang.reflect.Proxy.newProxyInstance(
            listenerClass.classLoader,
            arrayOf(listenerClass)
        ) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    post {
                        removeAllViews()
                        addView(videoAdView as View, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                    }
                    currentState = NapSspLoadState.LOADED
                    NapSspEventEmitter.emitViewEvent(
                        this@NapSspVideoAdView,
                        NapSspContracts.VIEW_EVENT_AD_LOADED,
                        mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                    )
                }
                "onFailedToReceiveAd" -> {
                    currentState = NapSspLoadState.FAILED
                    NapSspEventEmitter.emitViewEvent(
                        this@NapSspVideoAdView,
                        NapSspContracts.VIEW_EVENT_AD_FAILED,
                        mapOf(
                            "adUnitId" to normalizedAdUnitId,
                            "format" to NapSspContracts.FORMAT_VIDEO,
                            "code" to (args?.get(2) as? Int ?: -1),
                            "message" to (args?.get(3)?.toString() ?: ""),
                        ),
                    )
                }
                "onEventAd" -> {
                    when (args?.get(1)?.toString()) {
                        "COMPLETION" -> NapSspEventEmitter.emitViewEvent(
                            this@NapSspVideoAdView,
                            NapSspContracts.VIEW_EVENT_AD_COMPLETED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                        )
                        "SKIPPED" -> NapSspEventEmitter.emitViewEvent(
                            this@NapSspVideoAdView,
                            NapSspContracts.VIEW_EVENT_AD_SKIPPED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                        )
                        "CLICK" -> NapSspEventEmitter.emitViewEvent(
                            this@NapSspVideoAdView,
                            NapSspContracts.VIEW_EVENT_AD_CLICKED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                        )
                        "DISPLAYED" -> NapSspEventEmitter.emitViewEvent(
                            this@NapSspVideoAdView,
                            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                        )
                    }
                }
            }
            null
        }

        videoAdViewClass.getMethod("setAdViewListener", listenerClass).invoke(videoAdView, listener)
        videoAdViewClass.getMethod("loadAd").invoke(videoAdView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        adViewInstance?.let { runCatching { it.javaClass.getMethod("onResume").invoke(it) } }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adViewInstance?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        adViewInstance = null
    }
}
