package com.nasmedia.admixerssp

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.react.uimanager.ThemedReactContext

import com.facebook.react.bridge.LifecycleEventListener

class NapSspVideoAdView(context: Context) : FrameLayout(context), LifecycleEventListener {
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

        addOnLayoutChangeListener(layoutChangeListener)
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
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
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

        val vendorLoaded = try {
            tryAttachVendorVideoViewAndLoad(normalizedAdUnitId)
            true
        } catch (e: Throwable) {
            android.util.Log.w("NapSspVideoAd", "vendor SDK unavailable, using placeholder for $normalizedAdUnitId: ${e.message}")
            false
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

    private fun tryAttachVendorVideoViewAndLoad(normalizedAdUnitId: String) {
        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) throw UnsupportedOperationException("vendor SDK disabled")

        val activityContext: android.content.Context = (context as? ThemedReactContext)?.currentActivity ?: context
        val videoAdViewClass = Class.forName("com.nasmedia.admixerssp.ads.AMMVideoView")
        val adInfoBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")

        // 1. Create VideoAdView
        val videoAdView = videoAdViewClass.getConstructor(android.content.Context::class.java).newInstance(activityContext)
        adViewInstance = videoAdView as android.view.View

        // 2. Setup Listener using NapAdListener via reflection
        val bridge = object : NapListenerBridge {
            override fun onReceivedAd(adapterName: String, ad: Any) {
                val hasAd = runCatching { 
                    adViewInstance?.javaClass?.getField("hasAd")?.get(adViewInstance) as? Boolean
                }.getOrNull() ?: true

                if (!hasAd) {
                    if (BuildConfig.DEBUG) {
                        emitDebugPlaceholderLoad(normalizedAdUnitId, "debug-no-fill")
                    } else {
                        currentState = NapSspLoadState.FAILED
                        NapSspEventEmitter.emitViewEvent(
                            this@NapSspVideoAdView,
                            NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO, "code" to -1, "message" to "No fill (hasAd is false)")
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
                NapSspEventEmitter.emitViewEvent(
                    this@NapSspVideoAdView,
                    NapSspContracts.VIEW_EVENT_AD_LOADED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }

            override fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?) {
                if (BuildConfig.DEBUG) {
                    emitDebugPlaceholderLoad(normalizedAdUnitId, "debug-sdk-failed:$msg")
                } else {
                    currentState = NapSspLoadState.FAILED
                    NapSspEventEmitter.emitViewEvent(
                        this@NapSspVideoAdView,
                        NapSspContracts.VIEW_EVENT_AD_FAILED,
                        mapOf(
                            "adUnitId" to normalizedAdUnitId,
                            "format" to NapSspContracts.FORMAT_VIDEO,
                            "code" to code,
                            "message" to msg,
                        ),
                    )
                }
            }

            override fun onAdClicked() {
                NapSspEventEmitter.emitViewEvent(
                    this@NapSspVideoAdView,
                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }

            override fun onAdDisplayed() {
                NapSspEventEmitter.emitViewEvent(
                    this@NapSspVideoAdView,
                    NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }

            override fun onAdCompleted() {
                NapSspEventEmitter.emitViewEvent(
                    this@NapSspVideoAdView,
                    NapSspContracts.VIEW_EVENT_AD_COMPLETED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }

            override fun onAdSkipped() {
                NapSspEventEmitter.emitViewEvent(
                    this@NapSspVideoAdView,
                    NapSspContracts.VIEW_EVENT_AD_SKIPPED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }
        }

        val listenerClass = Class.forName("com.nasmedia.admixerssp.NapAdListener")
        val bridgeClass = Class.forName("com.nasmedia.admixerssp.NapListenerBridge")
        val listener = listenerClass.getConstructor(bridgeClass).newInstance(bridge)
        // AMMVideoView.setAdViewListener(Object) — v2 인라인 뷰 시그니처는 Object 파라미터.
        videoAdViewClass.getMethod("setAdViewListener", Any::class.java).invoke(videoAdView, listener)

        // 3. Prepare AdInfo
        val builder = adInfoBuilderClass.getConstructor(String::class.java).newInstance(normalizedAdUnitId)
        try {
            adInfoBuilderClass.getMethod("isRetry", Boolean::class.javaPrimitiveType).invoke(builder, isRetry)
        } catch (_: Throwable) {}
        try {
            adInfoBuilderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true)
        } catch (_: Throwable) {}
        val adInfo = adInfoBuilderClass.getMethod("build").invoke(builder)
        videoAdViewClass.getMethod("setAdInfo", adInfoClass).invoke(videoAdView, adInfo)

        // 4. Attach first, then loadAd
        post {
            removeAllViews()
            addView(videoAdView as android.view.View, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            videoAdViewClass.getMethod("loadAd").invoke(videoAdView)
            if (BuildConfig.DEBUG) {
                postDelayed({
                    if (currentState == NapSspLoadState.LOADING) {
                        emitDebugPlaceholderLoad(normalizedAdUnitId, "debug-sdk-timeout")
                    }
                }, 12000)
            }
        }
    }

    private fun emitDebugPlaceholderLoad(unitId: String, source: String) {
        currentState = NapSspLoadState.LOADED
        adUnitIdTextView.text = unitId
        NapSspEventEmitter.emitViewEvent(
            this@NapSspVideoAdView,
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_VIDEO, "source" to source),
        )
        NapSspEventEmitter.emitViewEvent(
            this@NapSspVideoAdView,
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_VIDEO, "source" to source),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeOnLayoutChangeListener(layoutChangeListener)
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        adViewInstance?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
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
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
        }
        adViewInstance = null
    }
}
