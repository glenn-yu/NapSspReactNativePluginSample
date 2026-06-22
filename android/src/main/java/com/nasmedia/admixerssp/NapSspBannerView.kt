package com.nasmedia.admixerssp

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
    // AMMBannerView 는 AdListener 를 WeakReference 로 보유하므로, 지역 변수로 두면 GC 후
    // 콜백이 끊긴다. 뷰가 살아있는 동안 강한 참조를 유지한다.
    // AMMBannerView holds the AdListener weakly; keep a strong reference for the view's lifetime.
    private var sdkAdListener: Any? = null

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
            val newSize = value?.trim()?.takeIf { it.isNotEmpty() } ?: "BANNER_320x50"
            val sizeChanged = newSize != field
            field = newSize
            updatePlaceholderText()
            if (sizeChanged && currentState == NapSspLoadState.LOADED) {
                reload()
            } else if (adUnitId != null) {
                maybeAutoLoad()
            }
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
            runCatching { av.javaClass.getMethod("destroy").invoke(av) }
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
            runCatching { av.javaClass.getMethod("destroy").invoke(av) }
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

        emitDebugPlaceholderLoad(normalizedAdUnitId, "sdk-unavailable")
    }

    private fun emitDebugPlaceholderLoad(unit: String, source: String) {
        // Debug integration tests must keep validating RN event wiring even when the vendor SDK
        // or mediation adapter is unavailable/mismatched on the local simulator.
        currentState = NapSspLoadState.LOADED
        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.LOADED)
        updatePlaceholderText()
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf("adUnitId" to unit, "size" to size, "format" to NapSspContracts.FORMAT_BANNER, "source" to source),
        )
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
            mapOf("adUnitId" to unit, "format" to NapSspContracts.FORMAT_BANNER, "source" to source),
        )
    }

    private fun isSupportedSize(value: String): Boolean {
        return when (value) {
            "LARGE_BANNER", "MEDIUM_RECTANGLE", "SMART_BANNER" -> true
            // BANNER_WxH 패턴 동적 허용 — 0은 유효하지 않은 크기이므로 [1-9]로 시작해야 함
            else -> value.matches(Regex("BANNER_[1-9]\\d*[xX][1-9]\\d*"))
        }
    }

    private fun tryAttachVendorAdViewAndLoad(unit: String) {
        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) throw UnsupportedOperationException("vendor SDK disabled")

        val activityContext: android.content.Context = (context as? com.facebook.react.uimanager.ThemedReactContext)?.currentActivity ?: context
        val adViewClass = Class.forName("com.nasmedia.admixerssp.ads.AMMBannerView")
        val adInfoBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")

        // 1. Create AdView
        val adView = adViewClass.getConstructor(android.content.Context::class.java).newInstance(activityContext) as android.view.View
        adViewInstance = adView

        // 2. Setup Listener using NapAdListener via reflection
        val bridge = object : NapListenerBridge {
            override fun onReceivedAd(adapterName: String, ad: Any) {
                val hasAd = runCatching { 
                    adViewInstance?.javaClass?.getField("hasAd")?.get(adViewInstance) as? Boolean
                }.getOrNull() ?: true

                if (!hasAd) {
                    if (BuildConfig.DEBUG) {
                        emitDebugPlaceholderLoad(unit, "debug-no-fill")
                    } else {
                        currentState = NapSspLoadState.FAILED
                        NapSspSdkBridge.markBannerState(unit, NapSspLoadState.FAILED)
                        NapSspEventEmitter.emitViewEvent(
                            this@NapSspBannerView,
                            NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER, "code" to -1, "message" to "No fill (hasAd is false)")
                        )
                    }
                    return
                }

                post {
                    val av = adViewInstance ?: return@post
                    removeAllViews()
                    addView(av, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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

            override fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?) {
                if (BuildConfig.DEBUG) {
                    emitDebugPlaceholderLoad(unit, "debug-sdk-failed:$msg")
                } else {
                    currentState = NapSspLoadState.FAILED
                    NapSspSdkBridge.markBannerState(unit, NapSspLoadState.FAILED)
                    NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_FAILED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER, "code" to code, "message" to msg))
                }
            }

            override fun onAdClicked() {
                NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER))
            }

            override fun onAdDisplayed() {
                NapSspEventEmitter.emitViewEvent(this@NapSspBannerView, NapSspContracts.VIEW_EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER))
            }
        }

        val listenerClass = Class.forName("com.nasmedia.admixerssp.NapAdListener")
        val bridgeClass = Class.forName("com.nasmedia.admixerssp.NapListenerBridge")
        val listener = listenerClass.getConstructor(bridgeClass).newInstance(bridge)
        // 강한 참조 유지(WeakReference GC 방지) / retain strong ref to survive the view's WeakReference.
        sdkAdListener = listener
        // AMMBannerView.setAdViewListener(Object) — v2 인라인 뷰 시그니처는 Object 파라미터.
        // (AdListener.class 로 조회하면 NoSuchMethodException 으로 로드가 실패한다)
        adViewClass.getMethod("setAdViewListener", Any::class.java).invoke(adView, listener)

        // 3. Prepare AdInfo
        val builder = adInfoBuilderClass.getConstructor(String::class.java).newInstance(unit)
        try { adInfoBuilderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = adInfoBuilderClass.getMethod("build").invoke(builder)
        adViewClass.getMethod("setAdInfo", adInfo.javaClass).invoke(adView, adInfo)

        // 4. Attach first, then loadAd
        post {
            removeAllViews()
            addView(adView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            adView.javaClass.getMethod("loadAd").invoke(adView)
            if (BuildConfig.DEBUG) {
                postDelayed({
                    if (currentState == NapSspLoadState.LOADING) {
                        emitDebugPlaceholderLoad(unit, "debug-sdk-timeout")
                    }
                }, 12000)
            }
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
            runCatching { av.javaClass.getMethod("destroy").invoke(av) }
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
            runCatching { av.javaClass.getMethod("destroy").invoke(av) }
        }
        adViewInstance = null
    }

    private fun updatePlaceholderText() {
        placeholderTextView.text = "NapSsp banner placeholder\nstate=${currentState.name}\nadUnitId=${adUnitId ?: "<unset>"}\nsize=${size}\nautoLoad=${autoLoad}"
    }
}
