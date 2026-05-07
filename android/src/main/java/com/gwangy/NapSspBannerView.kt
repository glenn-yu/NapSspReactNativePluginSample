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

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

    private val layoutChangeListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        post(measureAndLayout)
    }

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
                postDelayed({
                    NapSspEventEmitter.emitViewEvent(
                        this,
                        NapSspContracts.VIEW_EVENT_AD_CLOSED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                    )
                }, 200)
            }
        }

        addOnLayoutChangeListener(layoutChangeListener)

        updatePlaceholderText()
    }

    fun reload() {
        adViewInstance?.let { av ->
            runCatching { av.javaClass.getMethod("onPause").invoke(av) }
            runCatching { av.javaClass.getMethod("onDestroy").invoke(av) }
        }
        adViewInstance = null
        removeAllViews()
        addView(
            placeholderTextView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            },
        )
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
        if (!autoLoad && !force) return

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

        if (!force && currentState == NapSspLoadState.LOADED) return

        currentState = NapSspLoadState.LOADING
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADING)

        val vendorLoaded = try {
            tryAttachVendorAdViewAndLoad(normalizedAdUnitId)
            true
        } catch (e: Throwable) {
            android.util.Log.w("NapSspBanner", "vendor SDK unavailable, using placeholder for $normalizedAdUnitId: ${e.message}")
            false
        }

        if (vendorLoaded) return

        // fallback placeholder behavior (Mock mode)
        currentState = NapSspLoadState.LOADED
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADED)
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf("adUnitId" to normalizedAdUnitId, "size" to size, "format" to NapSspContracts.FORMAT_BANNER, "source" to "placeholder"),
        )
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_BANNER, "source" to "placeholder"),
        )
    }

    private fun isSupportedSize(value: String): Boolean {
        return when (value) {
            "BANNER_320x50", "BANNER_320x100", "BANNER_300x250", "BANNER_320x480", "LARGE_BANNER", "MEDIUM_RECTANGLE", "SMART_BANNER" -> true
            else -> false
        }
    }

    private fun tryAttachVendorAdViewAndLoad(unit: String) {
        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) throw UnsupportedOperationException("vendor SDK disabled")

        val activityContext: android.content.Context = (context as? com.facebook.react.uimanager.ThemedReactContext)?.currentActivity ?: context
        val adViewClass = Class.forName("com.nasmedia.admixerssp.ads.AdView")
        val adInfoBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val adListenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        // 1. Create AdView
        val adView = adViewClass.getConstructor(android.content.Context::class.java).newInstance(activityContext) as android.view.View
        adViewInstance = adView

        // 2. Setup Proxy Listener
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            adListenerClass.classLoader,
            arrayOf(adListenerClass)
        ) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    val hasAd = runCatching { 
                        adViewInstance?.javaClass?.getField("hasAd")?.getBoolean(adViewInstance) 
                    }.getOrDefault(true) ?: true

                    if (!hasAd) {
                        currentState = NapSspLoadState.FAILED
                        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.FAILED)
                        NapSspEventEmitter.emitViewEvent(
                            this@NapSspBannerView,
                            NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER, "code" to -1, "message" to "No fill (hasAd is false)")
                        )
                        return@newProxyInstance null
                    }

                    post {
                        val av = adViewInstance ?: return@post
                        removeAllViews()
                        addView(av, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                        // Critical: SDK guide says showAd() must be called after addView
                        runCatching { av.javaClass.getMethod("showAd").invoke(av) }

                        // Force RN layout sync
                        post(measureAndLayout)
                    }
                    currentState = NapSspLoadState.LOADED
                    NapSspSdkBridge.markBannerState(unit, NapSspLoadState.LOADED)
                    NapSspEventEmitter.emitViewEvent(
                        this@NapSspBannerView,
                        NapSspContracts.VIEW_EVENT_AD_LOADED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                    )
                }
                "onFailedToReceiveAd" -> {
                    currentState = NapSspLoadState.FAILED
                    NapSspSdkBridge.markBannerState(unit, NapSspLoadState.FAILED)
                    NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_FAILED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER, "code" to (args?.get(2) as? Int ?: -1), "message" to (args?.get(3)?.toString() ?: "")))
                }
                "onEventAd" -> {
                    val normalizedEvent = args?.get(1)?.toString()?.trim()?.uppercase()
                    when (normalizedEvent) {
                        "CLICK", "CLICKED" -> NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER))
                        "DISPLAYED" -> NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER))
                    }
                }
            }
            null
        }
        adViewClass.getMethod("setAdViewListener", adListenerClass).invoke(adView, proxy)

        // 3. Prepare AdInfo
        val builder = adInfoBuilderClass.getConstructor(String::class.java).newInstance(unit)
        try { adInfoBuilderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = adInfoBuilderClass.getMethod("build").invoke(builder)
        adViewClass.getMethod("setAdInfo", adInfo.javaClass).invoke(adView, adInfo)

        // 4. Attach first, then loadAd (Critical Order for Method 2)
        post {
            removeAllViews()
            addView(adView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            adView.javaClass.getMethod("loadAd").invoke(adView)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeOnLayoutChangeListener(layoutChangeListener)
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        adViewInstance?.let { av ->
            runCatching { av.javaClass.getMethod("onPause").invoke(av) }
            runCatching { av.javaClass.getMethod("onDestroy").invoke(av) }
        }
        adViewInstance = null
    }

    override fun onHostResume() {
        adViewInstance?.let { av -> runCatching { av.javaClass.getMethod("onResume").invoke(av) } }
    }

    override fun onHostPause() {
        adViewInstance?.let { av -> runCatching { av.javaClass.getMethod("onPause").invoke(av) } }
    }

    override fun onHostDestroy() {
        adViewInstance?.let { av ->
            runCatching { av.javaClass.getMethod("onPause").invoke(av) }
            runCatching { av.javaClass.getMethod("onDestroy").invoke(av) }
        }
        adViewInstance = null
    }

    private fun updatePlaceholderText() {
        placeholderTextView.text = "NapSsp banner placeholder\nstate=${currentState.name}\nadUnitId=${adUnitId ?: "<unset>"}\nsize=${size}\nautoLoad=${autoLoad}"
    }
}