package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext

class NapSspNativeAdView(context: Context) : FrameLayout(context), LifecycleEventListener {

    private var sdkNativeAdView: Any? = null
    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

    // 플레이스홀더 (SDK 미사용 시)
    private val placeholder = TextView(context).apply {
        setTextColor(Color.parseColor("#2E7D32"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#E8F5E9"))
        setPadding(24, 24, 24, 24)
    }

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            updatePlaceholder()
            maybeAutoLoad()
        }

    init {
        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            addView(placeholder, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        
        // RN 레이아웃 변화 감지
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            post(measureAndLayout)
        }
        
        updatePlaceholder()
    }

    fun destroyNativeAd() {
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        sdkNativeAdView = null
        currentState = NapSspLoadState.DESTROYED
        removeAllViews()
    }

    private fun maybeAutoLoad() {
        val unitId = adUnitId
        if (unitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf("adUnitId" to null, "format" to NapSspContracts.FORMAT_NATIVE_AD,
                    "code" to "NAP_SSP_INVALID_AD_UNIT", "message" to "Native adUnitId is required"),
            )
            return
        }

        if (currentState == NapSspLoadState.LOADED) return
        currentState = NapSspLoadState.LOADING

        if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadWithSdk(unitId)
        } else {
            postDelayed({
                currentState = NapSspLoadState.LOADED
                NapSspEventEmitter.emitViewEvent(
                    this, NapSspContracts.VIEW_EVENT_AD_LOADED,
                    mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                )
                NapSspEventEmitter.emitViewEvent(
                    this, NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                    mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                )
            }, 300)
        }
    }

    private fun loadWithSdk(unitId: String) {
        try {
            // Activity context 필요 (Adfit 등 미디에이션 필수 요구사항)
            val activityContext: Context =
                (context as? ThemedReactContext)?.currentActivity ?: context

            val nativeAdViewClass = Class.forName("com.nasmedia.admixerssp.ads.NativeAdView")
            val adInfoClass       = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
            val builderClass      = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
            val binderClass       = Class.forName("com.nasmedia.admixerssp.ads.NativeAdViewBinder")
            val binderBuilderClass= Class.forName("com.nasmedia.admixerssp.ads.NativeAdViewBinder\$Builder")
            val listenerClass     = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

            // AdInfo
            val adInfoBuilder = builderClass.getConstructor(String::class.java).newInstance(unitId)
            runCatching {
                builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType)
                    .invoke(adInfoBuilder, true)
                
                // Mediation setViewIds (Google, Adfit, Pangle)
                val adViewIds = java.util.HashMap<String, Int>()
                adViewIds.put("iv_icon", R.id.nap_ssp_native_icon)
                adViewIds.put("tv_title", R.id.nap_ssp_native_title)
                adViewIds.put("tv_adv", R.id.nap_ssp_native_adv)
                adViewIds.put("tv_desc", R.id.nap_ssp_native_desc)
                adViewIds.put("iv_main", R.id.nap_ssp_native_main)
                adViewIds.put("btn_cta", R.id.nap_ssp_native_cta)
                
                val setViewIdsMethod = builderClass.getMethod("setViewIds", String::class.java, java.util.Map::class.java)
                setViewIdsMethod.invoke(adInfoBuilder, "ADMANAGER", adViewIds)
                setViewIdsMethod.invoke(adInfoBuilder, "ADFIT", adViewIds)
                setViewIdsMethod.invoke(adInfoBuilder, "PANGLE", adViewIds)
            }
            val adInfo = builderClass.getMethod("build").invoke(adInfoBuilder)

            // NativeAdViewBinder
            val binderBuilder = binderBuilderClass
                .getConstructor(Int::class.javaPrimitiveType)
                .newInstance(R.layout.nap_ssp_native_ad)
            binderBuilderClass.getMethod("setIconImageId",  Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_icon)
            binderBuilderClass.getMethod("setTitleId",      Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_title)
            binderBuilderClass.getMethod("setAdvertiserId", Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_adv)
            binderBuilderClass.getMethod("setDescriptionId",Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_desc)
            binderBuilderClass.getMethod("setMainViewId",   Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_main)
            binderBuilderClass.getMethod("setCtaId",        Int::class.javaPrimitiveType).invoke(binderBuilder, R.id.nap_ssp_native_cta)
            val viewBinder = binderBuilderClass.getMethod("build").invoke(binderBuilder)

            // NativeAdView 생성 (Activity context 사용)
            val nativeAdView = nativeAdViewClass
                .getConstructor(Context::class.java)
                .newInstance(activityContext)
            nativeAdViewClass.getMethod("setAdInfo",     adInfoClass).invoke(nativeAdView, adInfo)
            nativeAdViewClass.getMethod("setViewBinder", binderClass).invoke(nativeAdView, viewBinder)

            // 리스너 설정
            val listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader, arrayOf(listenerClass),
            ) { _, method, args ->
                when (method.name) {
                    "onReceivedAd" -> {
                        val hasAd = runCatching {
                            nativeAdViewClass.getField("hasAd").getBoolean(nativeAdView)
                        }.getOrDefault(true)
                        if (!hasAd) {
                            currentState = NapSspLoadState.FAILED
                            NapSspEventEmitter.emitViewEvent(
                                this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                                mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD, "code" to -1, "message" to "No fill (hasAd is false)")
                            )
                            return@newProxyInstance null
                        }
                        post {
                            removeAllViews()
                            addView(
                                nativeAdView as View,
                                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                            )
                            post(measureAndLayout)
                        }
                        currentState = NapSspLoadState.LOADED
                        NapSspEventEmitter.emitViewEvent(
                            this, NapSspContracts.VIEW_EVENT_AD_LOADED,
                            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                        )
                    }
                    "onFailedToReceiveAd" -> {
                        currentState = NapSspLoadState.FAILED
                        NapSspEventEmitter.emitViewEvent(
                            this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf(
                                "adUnitId" to unitId,
                                "format" to NapSspContracts.FORMAT_NATIVE_AD,
                                "code" to (args?.getOrNull(2) as? Int ?: -1),
                                "message" to (args?.getOrNull(3)?.toString() ?: "unknown"),
                            ),
                        )
                    }
                    "onEventAd" -> {
                        when (args?.getOrNull(1)?.toString()) {
                            "CLICK" -> NapSspEventEmitter.emitViewEvent(
                                this, NapSspContracts.VIEW_EVENT_AD_CLICKED,
                                mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                            )
                            "DISPLAYED" -> NapSspEventEmitter.emitViewEvent(
                                this, NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                                mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                            )
                        }
                    }
                }
                null
            }

            nativeAdViewClass.getMethod("setAdViewListener", listenerClass).invoke(nativeAdView, listener)
            sdkNativeAdView = nativeAdView
            nativeAdViewClass.getMethod("loadNativeAd").invoke(nativeAdView)

        } catch (e: Throwable) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to unitId,
                    "format" to NapSspContracts.FORMAT_NATIVE_AD,
                    "code" to "NAP_SSP_NATIVE_LOAD_FAILED",
                    "message" to (e.message ?: "unknown"),
                ),
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
        sdkNativeAdView?.let { runCatching { it.javaClass.getMethod("onResume").invoke(it) } }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        sdkNativeAdView = null
    }

    override fun onHostResume() {
        sdkNativeAdView?.let { runCatching { it.javaClass.getMethod("onResume").invoke(it) } }
    }

    override fun onHostPause() {
        sdkNativeAdView?.let { runCatching { it.javaClass.getMethod("onPause").invoke(it) } }
    }

    override fun onHostDestroy() {
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        sdkNativeAdView = null
    }

    private fun updatePlaceholder() {
        placeholder.text = "NapSsp Native Ad\nadUnitId=${adUnitId ?: "<unset>"}\nstate=${currentState.name}"
    }
}
