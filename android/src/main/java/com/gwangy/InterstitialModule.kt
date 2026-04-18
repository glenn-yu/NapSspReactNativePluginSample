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
            val interstitial = createOrGetInterstitial(normalizedAdUnitId, activity)
            Log.d(tag, "loadInterstitial request adUnitId=$normalizedAdUnitId")
            interstitial.javaClass.getMethod("loadInterstitial").invoke(interstitial)
            promise.resolve(null)
        } catch (error: Throwable) {
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
            runCatching { interstitial.javaClass.getMethod("onDestroy").invoke(interstitial) }
        }
        NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
        promise.resolve(null)
    }

    private fun createOrGetInterstitial(adUnitId: String, activity: android.app.Activity): Any {
        interstitialAds[adUnitId]?.let { return it }

        val interstitialClass = Class.forName("com.nasmedia.admixerssp.ads.InterstitialAd")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        val builder = builderClass.getConstructor(String::class.java).newInstance(adUnitId)
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
                }
                "onFailedToReceiveAd" -> {
                    Log.d(tag, "onFailedToReceiveAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    loadedAdUnitIds.remove(adUnitId)
                    NapSspSdkBridge.clearInterstitial(adUnitId)
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_FAILED,
                        mapOf(
                            "adUnitId" to adUnitId,
                            "format" to NapSspContracts.FORMAT_INTERSTITIAL,
                            "code" to (args?.getOrNull(2) as? Int ?: -1),
                            "message" to (args?.getOrNull(3)?.toString() ?: "unknown"),
                        ),
                    )
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
        loadedAdUnitIds.clear()
        interstitialAds.values.forEach { interstitial ->
            runCatching { interstitial.javaClass.getMethod("onDestroy").invoke(interstitial) }
        }
        interstitialAds.clear()
        super.invalidate()
    }
}
