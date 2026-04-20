package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

class NapSspNativeAdView(context: Context) : RelativeLayout(context) {

    private var sdkNativeAdView: Any? = null
    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    // Programmatic asset views — IDs are assigned after View.generateViewId()
    private val iconImageView = ImageView(context).apply { id = View.generateViewId(); scaleType = ImageView.ScaleType.CENTER_CROP }
    private val headlineTextView = TextView(context).apply { id = View.generateViewId(); setTextColor(Color.parseColor("#212121")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTypeface(null, android.graphics.Typeface.BOLD) }
    private val advertiserTextView = TextView(context).apply { id = View.generateViewId(); setTextColor(Color.parseColor("#757575")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) }
    private val descriptionTextView = TextView(context).apply { id = View.generateViewId(); setTextColor(Color.parseColor("#424242")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) }
    private val mediaContainerView = FrameLayout(context).apply { id = View.generateViewId() }
    private val ctaTextView = TextView(context).apply {
        id = View.generateViewId(); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1565C0")); gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setPadding(24, 12, 24, 12)
    }

    // Placeholder elements shown when SDK is not available
    private val placeholderContainer = RelativeLayout(context).apply {
        setBackgroundColor(Color.parseColor("#E8F5E9"))
    }
    private val placeholderTitle = TextView(context).apply {
        setTextColor(Color.parseColor("#2E7D32")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
        text = "NapSsp Native Ad"
    }
    private val placeholderAdUnitId = TextView(context).apply {
        setTextColor(Color.parseColor("#388E3C")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); gravity = Gravity.CENTER
    }

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            placeholderAdUnitId.text = "adUnitId: ${field ?: "<unset>"}"
            maybeAutoLoad()
        }

    init {
        if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            buildSdkLayout()
        } else {
            buildPlaceholderLayout()
        }

        isClickable = true
        isFocusable = true
        setOnClickListener {
            val id = adUnitId
            if (!id.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
                NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to id, "format" to NapSspContracts.FORMAT_NATIVE_AD))
            }
        }
    }

    private fun buildSdkLayout() {
        val iconParams = LayoutParams(120, 120).apply { addRule(ALIGN_PARENT_START); addRule(ALIGN_PARENT_TOP); setMargins(16, 16, 8, 8) }
        addView(iconImageView, iconParams)

        val headlineParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(END_OF, iconImageView.id); addRule(ALIGN_PARENT_TOP); setMargins(0, 16, 16, 4)
        }
        addView(headlineTextView, headlineParams)

        val advertiserParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(END_OF, iconImageView.id); addRule(BELOW, headlineTextView.id); setMargins(0, 4, 16, 4)
        }
        addView(advertiserTextView, advertiserParams)

        val descParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(BELOW, iconImageView.id); addRule(ALIGN_PARENT_START); setMargins(16, 8, 16, 8)
        }
        addView(descriptionTextView, descParams)

        val mediaParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
            addRule(BELOW, descriptionTextView.id); setMargins(16, 8, 16, 8)
        }
        mediaContainerView.minimumHeight = 200
        addView(mediaContainerView, mediaParams)

        val ctaParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(BELOW, mediaContainerView.id); addRule(ALIGN_PARENT_END); setMargins(0, 8, 16, 16)
        }
        addView(ctaTextView, ctaParams)
    }

    private fun buildPlaceholderLayout() {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(placeholderContainer, params)

        val titleParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(CENTER_IN_PARENT); setMargins(16, 16, 16, 8)
        }
        placeholderContainer.addView(placeholderTitle, titleParams)

        val adUnitParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(BELOW, placeholderTitle.id); addRule(CENTER_HORIZONTAL); setMargins(16, 0, 16, 16)
        }
        placeholderContainer.addView(placeholderAdUnitId, adUnitParams)
    }

    fun destroyNativeAd() {
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onDestroy").invoke(it) }
        }
        sdkNativeAdView = null
        currentState = NapSspLoadState.DESTROYED
        removeAllViews()
    }

    private fun maybeAutoLoad() {
        val normalizedAdUnitId = adUnitId
        if (normalizedAdUnitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf("adUnitId" to null, "format" to NapSspContracts.FORMAT_NATIVE_AD,
                    "code" to "NAP_SSP_INVALID_AD_UNIT", "message" to "Native adUnitId is required"))
            return
        }

        if (currentState == NapSspLoadState.LOADED) return
        currentState = NapSspLoadState.LOADING

        if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadWithSdk(normalizedAdUnitId)
        } else {
            postDelayed({
                currentState = NapSspLoadState.LOADED
                NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_LOADED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD))
            }, 300)
        }
    }

    private fun loadWithSdk(normalizedAdUnitId: String) {
        try {
            val nativeAdViewClass = Class.forName("com.nasmedia.admixerssp.ads.NativeAdView")
            val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
            val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
            val binderClass = Class.forName("com.nasmedia.admixerssp.ads.NativeAdViewBinder")
            val binderBuilderClass = Class.forName("com.nasmedia.admixerssp.ads.NativeAdViewBinder\$Builder")
            val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

            val adInfoBuilder = builderClass.getConstructor(String::class.java).newInstance(normalizedAdUnitId)
            builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(adInfoBuilder, true)
            val adInfo = builderClass.getMethod("build").invoke(adInfoBuilder)

            // NativeAdViewBinder maps asset views by programmatic IDs
            val binderBuilder = binderBuilderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(0)
            binderBuilderClass.getMethod("setIconImageId", Int::class.javaPrimitiveType).invoke(binderBuilder, iconImageView.id)
            binderBuilderClass.getMethod("setTitleId", Int::class.javaPrimitiveType).invoke(binderBuilder, headlineTextView.id)
            binderBuilderClass.getMethod("setAdvertiserId", Int::class.javaPrimitiveType).invoke(binderBuilder, advertiserTextView.id)
            binderBuilderClass.getMethod("setDescriptionId", Int::class.javaPrimitiveType).invoke(binderBuilder, descriptionTextView.id)
            binderBuilderClass.getMethod("setMainViewId", Int::class.javaPrimitiveType).invoke(binderBuilder, mediaContainerView.id)
            binderBuilderClass.getMethod("setCtaId", Int::class.javaPrimitiveType).invoke(binderBuilder, ctaTextView.id)
            val viewBinder = binderBuilderClass.getMethod("build").invoke(binderBuilder)

            val nativeAdView = nativeAdViewClass.getConstructor(Context::class.java).newInstance(context)
            nativeAdViewClass.getMethod("setAdInfo", adInfoClass).invoke(nativeAdView, adInfo)
            nativeAdViewClass.getMethod("setViewBinder", binderClass).invoke(nativeAdView, viewBinder)

            val listener = java.lang.reflect.Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
                when (method.name) {
                    "onReceivedAd" -> {
                        val hasAd = runCatching { nativeAdViewClass.getField("hasAd").getBoolean(nativeAdView) }.getOrDefault(true)
                        if (hasAd) {
                            post {
                                removeAllViews()
                                addView(nativeAdView as View, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                            }
                        }
                        currentState = NapSspLoadState.LOADED
                        NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_LOADED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD))
                    }
                    "onFailedToReceiveAd" -> {
                        currentState = NapSspLoadState.FAILED
                        NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD,
                                "code" to (args?.getOrNull(2) as? Int ?: -1),
                                "message" to (args?.getOrNull(3)?.toString() ?: "unknown")))
                    }
                    "onEventAd" -> {
                        when (args?.getOrNull(1)?.toString()) {
                            "CLICK" -> NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_CLICKED,
                                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD))
                            "DISPLAYED" -> NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD))
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
            NapSspEventEmitter.emitViewEvent(this, NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD,
                    "code" to "NAP_SSP_NATIVE_LOAD_FAILED", "message" to (e.message ?: "unknown")))
        }
    }
}
