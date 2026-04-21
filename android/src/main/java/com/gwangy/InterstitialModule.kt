package com.gwangy

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

class InterstitialModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val tag = "NapSspInterstitial"
    private val loadedAdUnitIds = ConcurrentHashMap<String, Boolean>()
    private val interstitialAds = ConcurrentHashMap<String, Any>()
    private val loadPromises = ConcurrentHashMap<String, Promise>()

    override fun getName(): String = NapSspContracts.INTERSTITIAL_MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.INTERSTITIAL_MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod
    fun load(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Interstitial adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspSdkBridge.markInterstitialState(normalizedAdUnitId, NapSspLoadState.LOADED)
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
            )
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "Interstitial ads require a foreground Activity context")
            return
        }

        try {
            loadPromises[normalizedAdUnitId]?.reject("NAP_SSP_INTERSTITIAL_LOAD_CANCELLED", "Superseded by new load")
            loadPromises[normalizedAdUnitId] = promise
            val interstitial = createOrGetInterstitial(normalizedAdUnitId, activity, options)
            Log.d(tag, "loadInterstitial request adUnitId=$normalizedAdUnitId")
            interstitial.javaClass.getMethod("loadInterstitial").invoke(interstitial)
        } catch (error: Throwable) {
            loadPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", error)
        }
    }

    @ReactMethod
    fun show(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Interstitial adUnitId is required")
            return
        }

        if (loadedAdUnitIds[normalizedAdUnitId] != true) {
            promise.reject("NAP_SSP_INTERSTITIAL_NOT_READY", "Interstitial has not been loaded yet")
            return
        }

        try {
            if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
                val interstitial = interstitialAds[normalizedAdUnitId]
                if (interstitial == null) {
                    promise.reject("NAP_SSP_INTERSTITIAL_NOT_READY", "Interstitial instance is missing")
                    return
                }
                NapSspSdkBridge.markInterstitialState(normalizedAdUnitId, NapSspLoadState.SHOWN)
                Log.d(tag, "showInterstitial request adUnitId=$normalizedAdUnitId")
                interstitial.javaClass.getMethod("showInterstitial").invoke(interstitial)
            } else {
                NapSspSdkBridge.markInterstitialState(normalizedAdUnitId, NapSspLoadState.SHOWN)
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_OPENED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_CLOSED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
                )
                loadedAdUnitIds.remove(normalizedAdUnitId)
                NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
            }
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("NAP_SSP_INTERSTITIAL_SHOW_FAILED", error)
        }
    }

    @ReactMethod
    fun isLoaded(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        promise.resolve(loadedAdUnitIds[normalizedAdUnitId] == true)
    }

    @ReactMethod
    fun destroy(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        loadedAdUnitIds.remove(normalizedAdUnitId)
        interstitialAds.remove(normalizedAdUnitId)?.let { interstitial ->
            runCatching { interstitial.javaClass.getMethod("stopInterstitial").invoke(interstitial) }
        }
        NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
        promise.resolve(null)
    }

    private fun applyInterstitialOptions(builder: Any, builderClass: Class<*>, options: com.facebook.react.bridge.ReadableMap?) {
        if (options == null) return
        try {
            val adType = options.getString("type") ?: "default"
            val interstitialAdTypeClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$InterstitialAdType")
            val typeValue = when (adType) {
                "popup", "countdown" -> interstitialAdTypeClass.getField("Popup").get(null)
                else -> interstitialAdTypeClass.getField("Basic").get(null)
            }
            builderClass.getMethod("interstitialAdType", interstitialAdTypeClass).invoke(builder, typeValue)
        } catch (_: Throwable) {}

        if (options.hasKey("buttonLeftText") || options.hasKey("buttonRightText") || options.getString("type") == "countdown") {
            try {
                val popupOptionClass = Class.forName("com.nasmedia.admixerssp.ads.PopupInterstitialAdOption")
                val popupConfig = popupOptionClass.getConstructor().newInstance()

                val buttonLeft = options.getString("buttonLeftText") ?: "닫기"
                popupOptionClass.getMethod("setButtonLeft", String::class.java, String::class.java)
                    .invoke(popupConfig, buttonLeft, null)

                val buttonRight = if (options.hasKey("buttonRightText")) options.getString("buttonRightText") else null
                if (buttonRight != null) {
                    popupOptionClass.getMethod("setButtonRight", String::class.java, String::class.java)
                        .invoke(popupConfig, buttonRight, null)
                }

                if (options.getString("type") == "countdown") {
                    val countDownTime = if (options.hasKey("countDownTime")) options.getInt("countDownTime") else 5
                    popupOptionClass.getMethod("setCountDown", Int::class.java, Int::class.java)
                        .invoke(popupConfig, 0, countDownTime.coerceIn(2, 5))
                }

                builderClass.getMethod("popupAdOption", popupOptionClass).invoke(builder, popupConfig)
            } catch (_: Throwable) {}
        }
    }

    private fun createOrGetInterstitial(adUnitId: String, activity: android.app.Activity, options: com.facebook.react.bridge.ReadableMap?): Any {
        interstitialAds[adUnitId]?.let { return it }

        val interstitialClass = Class.forName("com.nasmedia.admixerssp.ads.InterstitialAd")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        val builder = builderClass.getConstructor(String::class.java).newInstance(adUnitId)
        applyInterstitialOptions(builder!!, builderClass, options)
        try { builderClass.getMethod("setIsUseMediation", Boolean::class.java).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = builderClass.getMethod("build").invoke(builder)
        val interstitial = interstitialClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        interstitialClass.getMethod("setAdInfo", adInfoClass).invoke(interstitial, adInfo)

        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    Log.d(tag, "onReceivedAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    loadedAdUnitIds[adUnitId] = true
                    NapSspSdkBridge.markInterstitialState(adUnitId, NapSspLoadState.LOADED)
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_LOADED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
                    )
                    loadPromises.remove(adUnitId)?.resolve(null)
                }
                "onFailedToReceiveAd" -> {
                    Log.d(tag, "onFailedToReceiveAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    loadedAdUnitIds.remove(adUnitId)
                    NapSspSdkBridge.clearInterstitial(adUnitId)
                    val code = args?.getOrNull(2) as? Int ?: -1
                    val message = args?.getOrNull(3)?.toString() ?: "unknown"
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_FAILED,
                        mapOf(
                            "adUnitId" to adUnitId,
                            "format" to NapSspContracts.FORMAT_INTERSTITIAL,
                            "code" to code,
                            "message" to message,
                        ),
                    )
                    loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", message)
                }
                "onEventAd" -> {
                    val eventName = args?.getOrNull(1)?.toString()
                    Log.d(tag, "onEventAd adUnitId=$adUnitId event=$eventName args=${args?.contentToString()}")
                    when (eventName) {
                        "DISPLAYED" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                        "CLICK" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                        "CLOSE" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLOSED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                            loadedAdUnitIds.remove(adUnitId)
                            NapSspSdkBridge.clearInterstitial(adUnitId)
                        }
                    }
                    if (eventName == "DISPLAYED") {
                        NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                    }
                }
            }
            null
        }

        interstitialClass.getMethod("setAdListener", listenerClass).invoke(interstitial, listener)
        interstitialAds[adUnitId] = interstitial
        return interstitial
    }

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        loadPromises.values.forEach { it.reject("NAP_SSP_INTERSTITIAL_LOAD_CANCELLED", "Module invalidated") }
        loadPromises.clear()
        loadedAdUnitIds.clear()
        interstitialAds.values.forEach { interstitial ->
            runCatching { interstitial.javaClass.getMethod("stopInterstitial").invoke(interstitial) }
        }
        interstitialAds.clear()
        super.invalidate()
    }
}
