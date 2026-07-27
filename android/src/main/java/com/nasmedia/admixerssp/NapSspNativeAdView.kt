package com.nasmedia.admixerssp

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.ThemedReactContext

class NapSspNativeAdView(context: Context) : FrameLayout(context), LifecycleEventListener {

    companion object {
        private const val TAG = "NapSspNativeAdView"
    }

    private var sdkNativeAdView: Any? = null
    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    private fun layoutRes(name: String, fallback: Int): Int {
        return resources.getIdentifier(name, "layout", context.packageName).takeIf { it != 0 } ?: fallback
    }

    private fun idRes(name: String, fallback: Int): Int {
        return resources.getIdentifier(name, "id", context.packageName).takeIf { it != 0 } ?: fallback
    }

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

    private fun emitViewEventOnUi(eventName: String, data: Map<String, Any?> = emptyMap()) {
        post {
            NapSspEventEmitter.emitViewEvent(this, eventName, data)
        }
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

        addOnLayoutChangeListener(layoutChangeListener)

        updatePlaceholder()
    }

    fun destroyNativeAd() {
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
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
        Log.d(TAG, "maybeAutoLoad unitId=$unitId sdkEnabled=${BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED}")

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

            val nativeAdViewClass = Class.forName("com.nasmedia.admixerssp.ads.AMMNativeAdView")
            val adInfoClass       = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
            val builderClass      = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
            val binderClass       = Class.forName("com.nasmedia.admixerssp.common.nativeads.NativeAdViewBinder")
            val binderBuilderClass= Class.forName("com.nasmedia.admixerssp.common.nativeads.NativeAdViewBinder\$Builder")

            val nativeLayoutId = layoutRes("nap_ssp_native_ad", R.layout.nap_ssp_native_ad)
            val nativeIconId = idRes("nap_mx_iv_icon", R.id.nap_mx_iv_icon)
            val nativeTitleId = idRes("nap_mx_tv_title", R.id.nap_mx_tv_title)
            val nativeAdvId = idRes("nap_mx_tv_adv", R.id.nap_mx_tv_adv)
            val nativeDescId = idRes("nap_mx_tv_desc", R.id.nap_mx_tv_desc)
            val nativeMainId = idRes("nap_mx_iv_main", R.id.nap_mx_iv_main)
            val nativeCtaId = idRes("nap_mx_btn_cta", R.id.nap_mx_btn_cta)
            Log.d(TAG, "resourceIds layout=$nativeLayoutId icon=$nativeIconId title=$nativeTitleId adv=$nativeAdvId desc=$nativeDescId main=$nativeMainId cta=$nativeCtaId package=${context.packageName}")

            // AdInfo
            val adInfoBuilder = builderClass.getConstructor(String::class.java).newInstance(unitId)
            runCatching {
                builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType)
                    .invoke(adInfoBuilder, true)
            }
            val adInfo = builderClass.getMethod("build").invoke(adInfoBuilder)

            // NativeAdViewBinder
            val binderBuilder = binderBuilderClass
                .getConstructor(Int::class.javaPrimitiveType)
                .newInstance(nativeLayoutId)
            binderBuilderClass.getMethod("setIconImageId",  Int::class.javaPrimitiveType).invoke(binderBuilder, nativeIconId)
            binderBuilderClass.getMethod("setTitleId",      Int::class.javaPrimitiveType).invoke(binderBuilder, nativeTitleId)
            binderBuilderClass.getMethod("setAdvertiserId", Int::class.javaPrimitiveType).invoke(binderBuilder, nativeAdvId)
            binderBuilderClass.getMethod("setDescriptionId",Int::class.javaPrimitiveType).invoke(binderBuilder, nativeDescId)
            binderBuilderClass.getMethod("setMainViewId",   Int::class.javaPrimitiveType).invoke(binderBuilder, nativeMainId)
            binderBuilderClass.getMethod("setCtaId",        Int::class.javaPrimitiveType).invoke(binderBuilder, nativeCtaId)
            val viewBinder = binderBuilderClass.getMethod("build").invoke(binderBuilder)

            // NativeAdView 생성 (Activity context 사용)
            val nativeAdView = nativeAdViewClass
                .getConstructor(Context::class.java)
                .newInstance(activityContext)
            nativeAdViewClass.getMethod("setAdInfo",     adInfoClass).invoke(nativeAdView, adInfo)
            nativeAdViewClass.getMethod("setViewBinder", binderClass).invoke(nativeAdView, viewBinder)

            // 리스너 설정
            val bridge = object : NapListenerBridge {
                override fun onReceivedAd(adapterName: String, ad: Any) {
                    val hasAd = runCatching {
                        nativeAdViewClass.getField("hasAd").get(nativeAdView) as? Boolean
                    }.getOrNull() ?: true
                    if (!hasAd) {
                        if (BuildConfig.DEBUG) {
                            emitDebugPlaceholderLoad(unitId, "debug-no-fill")
                        } else {
                            currentState = NapSspLoadState.FAILED
                            emitViewEventOnUi(
                                NapSspContracts.VIEW_EVENT_AD_FAILED,
                                mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD, "code" to -1, "message" to "No fill (hasAd is false)")
                            )
                        }
                        return
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
                    Log.d(TAG, "onReceivedAd hasAd=$hasAd")
                    emitViewEventOnUi(
                        NapSspContracts.VIEW_EVENT_AD_LOADED,
                        mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                    )
                }

                override fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?) {
                    Log.e(TAG, "onFailedToReceiveAd code=$code message=$msg")
                    if (BuildConfig.DEBUG) {
                        emitDebugPlaceholderLoad(unitId, "debug-sdk-failed:$msg")
                    } else {
                        currentState = NapSspLoadState.FAILED
                        emitViewEventOnUi(
                            NapSspContracts.VIEW_EVENT_AD_FAILED,
                            mapOf(
                                "adUnitId" to unitId,
                                "format" to NapSspContracts.FORMAT_NATIVE_AD,
                                "code" to code,
                                "message" to msg,
                            ),
                        )
                    }
                }

                override fun onAdClicked() {
                    emitViewEventOnUi(
                        NapSspContracts.VIEW_EVENT_AD_CLICKED,
                        mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                    )
                    emitViewEventOnUi(
                        NapSspContracts.VIEW_EVENT_AD_OPENED,
                        mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                    )
                    postDelayed({
                        emitViewEventOnUi(
                            NapSspContracts.VIEW_EVENT_AD_CLOSED,
                            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                        )
                    }, 200)
                }

                override fun onAdDisplayed() {
                    emitViewEventOnUi(
                        NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
                        mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                    )
                }
            }

            val listenerClass = Class.forName("com.nasmedia.admixerssp.NapAdListener")
            val bridgeClass = Class.forName("com.nasmedia.admixerssp.NapListenerBridge")
            val listener = listenerClass.getConstructor(bridgeClass).newInstance(bridge)
            // AMMNativeAdView.setAdViewListener(Object) — v2 인라인 뷰 시그니처는 Object 파라미터.
            nativeAdViewClass.getMethod("setAdViewListener", Any::class.java).invoke(nativeAdView, listener)
            sdkNativeAdView = nativeAdView
            Log.d(TAG, "calling loadNativeAd")
            nativeAdViewClass.getMethod("loadNativeAd").invoke(nativeAdView)
            if (BuildConfig.DEBUG) {
                postDelayed({
                    if (currentState == NapSspLoadState.LOADING) {
                        emitDebugPlaceholderLoad(unitId, "debug-sdk-timeout")
                    }
                }, 12000)
            }

        } catch (e: Throwable) {
            Log.e(TAG, "loadWithSdk failed", e)
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

    private fun emitDebugPlaceholderLoad(unitId: String, source: String) {
        currentState = NapSspLoadState.LOADED
        updatePlaceholder()
        emitViewEventOnUi(
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD, "source" to source),
        )
        emitViewEventOnUi(
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION,
            mapOf("adUnitId" to unitId, "format" to NapSspContracts.FORMAT_NATIVE_AD, "source" to source),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context as? ThemedReactContext)?.addLifecycleEventListener(this)
        sdkNativeAdView?.let { runCatching { it.javaClass.getMethod("onResume").invoke(it) } }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeOnLayoutChangeListener(layoutChangeListener)
        (context as? ThemedReactContext)?.removeLifecycleEventListener(this)
        sdkNativeAdView?.let {
            runCatching { it.javaClass.getMethod("onPause").invoke(it) }
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
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
            runCatching { it.javaClass.getMethod("destroy").invoke(it) }
        }
        sdkNativeAdView = null
    }

    private fun updatePlaceholder() {
        placeholder.text = "NapSsp Native Ad\nadUnitId=${adUnitId ?: "<unset>"}\nstate=${currentState.name}"
    }
}
