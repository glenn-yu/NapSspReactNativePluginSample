package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext

class NapSspBannerView(context: Context) : FrameLayout(context), LifecycleEventListener {
    private val placeholderTextView: TextView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        gravity = Gravity.CENTER
        text = "NapSsp banner placeholder"
        setPadding(24, 24, 24, 24)
    }

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE
    private var adViewInstance: android.view.View? = null

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            updatePlaceholderText()
            maybeAutoLoad()
        }

    var size: String = "BANNER_320x50"
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() } ?: "BANNER_320x50"
            updatePlaceholderText()
            if (adUnitId != null) maybeAutoLoad()
        }

    var autoLoad: Boolean = true
        set(value) {
            field = value
            if (value && adUnitId != null) maybeAutoLoad()
        }

    init {
        setBackgroundColor(Color.parseColor("#1A1A1A"))
        addView(
            placeholderTextView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            },
        )
        isClickable = true
        isFocusable = true
        setOnClickListener {
            val adUnitId = adUnitId
            if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
                NapSspSdkBridge.markBannerState(adUnitId, NapSspLoadState.SHOWN)
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                )
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_OPENED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                )
            }
        }
        updatePlaceholderText()
    }

    fun reload() {
        adViewInstance?.let { av ->
            runCatching { av.javaClass.getMethod("onPause").invoke(av) }
            runCatching { av.javaClass.getMethod("onDestroy").invoke(av) }
        }
        adViewInstance = null
        removeAllViews()
        currentState = NapSspLoadState.IDLE
        maybeAutoLoad(force = true)
    }

    fun destroyBanner() {
        val adUnitId = adUnitId
        if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_CLOSED,
                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
            )
        }
        currentState = NapSspLoadState.DESTROYED
        adViewInstance?.let { av ->
            runCatching { av.javaClass.getMethod("onPause").invoke(av) }
            runCatching { av.javaClass.getMethod("onDestroy").invoke(av) }
        }
        adViewInstance = null
        if (!adUnitId.isNullOrBlank()) {
            NapSspSdkBridge.clearBanner(adUnitId)
        }
        removeAllViews()
    }

    private fun maybeAutoLoad(force: Boolean = false) {
        if (!autoLoad && !force) {
            return
        }

        val normalizedAdUnitId = adUnitId
        if (normalizedAdUnitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to null,
                    "format" to NapSspContracts.FORMAT_BANNER,
                    "code" to "NAP_SSP_INVALID_AD_UNIT",
                    "message" to "Banner adUnitId is required",
                ),
            )
            return
        }

        if (!isSupportedSize(size)) {
            currentState = NapSspLoadState.FAILED
            NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.FAILED)
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to normalizedAdUnitId,
                    "format" to NapSspContracts.FORMAT_BANNER,
                    "code" to "NAP_SSP_INVALID_BANNER_SIZE",
                    "message" to "Unsupported banner size: $size",
                ),
            )
            return
        }

        if (!force && currentState == NapSspLoadState.LOADED) {
            return
        }

        currentState = NapSspLoadState.LOADING
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADING)

        // Try vendor SDK attach and load first. If vendor SDK is not present or fails, fall back to placeholder behavior.
        var vendorLoaded = false
        try {
            tryAttachVendorAdView()
            applyAdUnitToVendor(normalizedAdUnitId)
            vendorLoaded = true
        } catch (_: Throwable) {
            vendorLoaded = false
        }

        if (vendorLoaded) {
            // Success: vendor SDK is now loading the ad. 
            // We do NOT emit AD_LOADED here because the actual ad hasn't arrived yet.
            // The listener (proxy) above will emit AD_LOADED when onReceivedAd is called.
            return
        }

        // fallback placeholder behavior (Mock mode)
        currentState = NapSspLoadState.LOADED
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADED)
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf(
                "adUnitId" to normalizedAdUnitId,
                "size" to size,
                "format" to NapSspContracts.FORMAT_BANNER,
                "source" to "placeholder",
            ),
        )
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
            mapOf(
                "adUnitId" to normalizedAdUnitId,
                "format" to NapSspContracts.FORMAT_BANNER,
                "source" to "placeholder",
            ),
        )
    }

    private fun isSupportedSize(value: String): Boolean {
        return when (value) {
            "BANNER_320x50",
            "BANNER_320x100",
            "BANNER_300x250",
            "BANNER_320x480",
            "LARGE_BANNER",
            "MEDIUM_RECTANGLE",
            "SMART_BANNER" -> true
            else -> false
        }
    }

    private fun tryAttachVendorAdView() {
        if (adViewInstance != null) return
        try {
            if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) return

            val adViewClass = Class.forName("com.nasmedia.admixerssp.ads.AdView")
            val ctor = adViewClass.getConstructor(android.content.Context::class.java)
            val adView = ctor.newInstance(context)
            adViewInstance = adView as android.view.View

            // AdView를 window에 attach해야 onAttachedToWindow → ad request 시작 (공식 방법 2 step 1)
            post {
                removeAllViews()
                addView(adView as android.view.View, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }

            // wire listener
            try {
                val listenerInterface = Class.forName("com.nasmedia.admixerssp.ads.AdListener")
                val proxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerInterface.classLoader,
                    arrayOf(listenerInterface)
                ) { _, method, args ->
                    when (method.name) {
                        "onReceivedAd" -> {
                            // 방법 2: onReceivedAd 에서 removeView → addView → showAd() (공식 가이드 준수)
                            post {
                                val av = adViewInstance ?: return@post
                                removeAllViews()
                                addView(av, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                                runCatching { av.javaClass.getMethod("showAd").invoke(av) }
                                requestLayout()
                            }
                            NapSspEventEmitter.emitViewEvent(
                                this@NapSspBannerView,
                                NapSspContracts.VIEW_EVENT_AD_LOADED,
                                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                            )
                            NapSspEventEmitter.emitViewEvent(
                                this@NapSspBannerView,
                                NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                            )
                        }
                        "onFailedToReceiveAd" -> NapSspEventEmitter.emitViewEvent(
                            this@NapSspBannerView,
                            NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf(
                                "adUnitId" to adUnitId,
                                "format" to NapSspContracts.FORMAT_BANNER,
                                "code" to (args?.get(2) as? Int ?: -1),
                                "message" to (args?.get(3)?.toString() ?: ""),
                            ),
                        )
                        "onEventAd" -> {
                            when (args?.get(1)?.toString()) {
                                "CLICK" -> NapSspEventEmitter.emitViewEvent(
                                    this@NapSspBannerView,
                                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                                )
                                "DISPLAYED" -> NapSspEventEmitter.emitViewEvent(
                                    this@NapSspBannerView,
                                    NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                                )
                            }
                        }
                    }
                    null
                }
                val setListener = adViewClass.getMethod("setAdViewListener", listenerInterface)
                setListener.invoke(adView, proxy)
            } catch (_: Throwable) {
            }
        } catch (_: Throwable) {
        }
    }

    private fun applyAdUnitToVendor(unit: String) {
        val adView = adViewInstance ?: return
        try {
            val adInfoBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
            val adInfoBuilderCtor = adInfoBuilderClass.getConstructor(String::class.java)
            val builder = adInfoBuilderCtor.newInstance(unit)
            try {
                val setIsUseMediation = adInfoBuilderClass.getMethod("setIsUseMediation", Boolean::class.java)
                setIsUseMediation.invoke(builder, true)
            } catch (_: Throwable) {}
            val build = adInfoBuilderClass.getMethod("build")
            val adInfo = build.invoke(builder)
            val setAdInfo = adView.javaClass.getMethod("setAdInfo", adInfo.javaClass)
            setAdInfo.invoke(adView, adInfo)
            try { adView.javaClass.getMethod("loadAd").invoke(adView) } catch (_: Throwable) {}
        } catch (_: Throwable) {
            throw RuntimeException("vendor attach failed")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        adViewInstance?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        adViewInstance = null
    }

    override fun onHostResume() {
        adViewInstance?.let { runCatching { it.javaClass.getMethod("onResume").invoke(it) } }
    }

    override fun onHostPause() {
        adViewInstance?.let { runCatching { it.javaClass.getMethod("onPause").invoke(it) } }
    }

    override fun onHostDestroy() {
        adViewInstance?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        adViewInstance = null
    }

    private fun updatePlaceholderText() {
        placeholderTextView.text = buildString {
            append("NapSsp banner placeholder\n")
            append("state=")
            append(currentState.name)
            append("\nadUnitId=")
            append(adUnitId ?: "<unset>")
            append("\nsize=")
            append(size)
            append("\nautoLoad=")
            append(autoLoad)
        }
    }
}
