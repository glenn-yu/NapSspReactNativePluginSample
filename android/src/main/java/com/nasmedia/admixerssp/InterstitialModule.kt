package com.nasmedia.admixerssp

import android.content.pm.ApplicationInfo
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
    private val startPromises = ConcurrentHashMap<String, Promise>()

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
            startPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_START_CANCELLED", "Superseded by load")
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
    fun start(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Interstitial adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED || isDebuggableApp()) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspSdkBridge.markInterstitialState(normalizedAdUnitId, NapSspLoadState.SHOWN)
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_OPENED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_IMPRESSION,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_CLOSED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
            )
            loadedAdUnitIds.remove(normalizedAdUnitId)
            NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "Interstitial ads require a foreground Activity context")
            return
        }

        try {
            val interstitial = createOrGetInterstitial(normalizedAdUnitId, activity, options)
            loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_CANCELLED", "Superseded by start")
            startPromises[normalizedAdUnitId]?.reject("NAP_SSP_INTERSTITIAL_START_CANCELLED", "Superseded by start")
            startPromises[normalizedAdUnitId] = promise
            // v2: 즉시 노출 startInterstitial()이 제거됨 → 로드만 시작하고
            // 수신(onReceivedAd) 시점에 showInterstitial()로 자동 노출한다.
            Log.d(tag, "start interstitial (load → auto-show) adUnitId=$normalizedAdUnitId")
            interstitial.javaClass.getMethod("loadInterstitial").invoke(interstitial)
        } catch (error: Throwable) {
            loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_START_FAILED", error)
            startPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_INTERSTITIAL_START_FAILED", error)
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
            if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED && !BuildConfig.DEBUG) {
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
                    NapSspContracts.EVENT_AD_IMPRESSION,
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
        loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_DESTROYED", "Destroyed before load completed")
        startPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_DESTROYED", "Destroyed before start completed")
        interstitialAds.remove(normalizedAdUnitId)?.let { interstitial ->
            val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")
            runCatching { interstitial.javaClass.getMethod("setAdListener", listenerClass).invoke(interstitial, null) }
            runCatching { interstitial.javaClass.getMethod("stopInterstitial").invoke(interstitial) }
        }
        NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
        promise.resolve(null)
    }

    private fun applyInterstitialOptions(builder: Any, builderClass: Class<*>, options: com.facebook.react.bridge.ReadableMap?) {
        // In v2.0.0, interstitial options like popup type and countdown are deprecated and removed.
    }

    private fun createOrGetInterstitial(adUnitId: String, activity: android.app.Activity, options: com.facebook.react.bridge.ReadableMap?): Any {
        interstitialAds[adUnitId]?.let { return it }

        val interstitialClass = Class.forName("com.nasmedia.admixerssp.ads.AMMInterstitial")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")

        val builder = builderClass.getConstructor(String::class.java).newInstance(adUnitId)
        applyInterstitialOptions(builder!!, builderClass, options)
        try { builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = builderClass.getMethod("build").invoke(builder)
        val interstitial = interstitialClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        interstitialClass.getMethod("setAdInfo", adInfoClass).invoke(interstitial, adInfo)

        val bridge = object : NapListenerBridge {
            override fun onReceivedAd(adapterName: String, ad: Any) {
                Log.d(tag, "onReceivedAd adUnitId=$adUnitId")
                val hasInterstitial = runCatching {
                    interstitial.javaClass.getField("hasInterstitial").get(interstitial) as? Boolean
                }.getOrNull() ?: true

                if (!hasInterstitial) {
                    val message = "No fill (hasInterstitial is false)"
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_FAILED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL, "code" to -1, "message" to message)
                    )
                    loadedAdUnitIds.remove(adUnitId)
                    NapSspSdkBridge.clearInterstitial(adUnitId)
                    if (startPromises.containsKey(adUnitId)) {
                        startPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", message)
                    } else {
                        loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", message)
                    }
                    return
                }

                loadedAdUnitIds[adUnitId] = true
                NapSspSdkBridge.markInterstitialState(adUnitId, NapSspLoadState.LOADED)
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_LOADED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
                )
                val startPromise = startPromises.remove(adUnitId)
                if (startPromise != null) {
                    Log.d(tag, "onReceivedAd startPromise=true adUnitId=$adUnitId")
                    // start() = 로드 후 자동 노출. v2에서 startInterstitial()이 제거되어
                    // 수신 콜백 시점에 showInterstitial()로 노출한다.
                    runCatching {
                        NapSspSdkBridge.markInterstitialState(adUnitId, NapSspLoadState.SHOWN)
                        interstitial.javaClass.getMethod("showInterstitial").invoke(interstitial)
                        startPromise.resolve(null)
                    }.onFailure {
                        Log.e(tag, "auto-show interstitial failed adUnitId=$adUnitId: ${it.message}", it)
                        startPromise.reject("NAP_SSP_INTERSTITIAL_SHOW_FAILED", it)
                    }
                } else {
                    val loadPromise = loadPromises.remove(adUnitId)
                    Log.d(tag, "onReceivedAd loadPromise=${loadPromise != null} adUnitId=$adUnitId")
                    if (loadPromise != null) {
                        loadPromise.resolve(null)
                    } else {
                        Log.w(tag, "onReceivedAd without pending promise adUnitId=$adUnitId loaded=${loadedAdUnitIds[adUnitId]}")
                    }
                }
            }

            override fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?) {
                Log.d(tag, "onFailedToReceiveAd adUnitId=$adUnitId code=$code msg=$msg")
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_FAILED,
                    mapOf(
                        "adUnitId" to adUnitId,
                        "format" to NapSspContracts.FORMAT_INTERSTITIAL,
                        "code" to code,
                        "message" to msg,
                    ),
                )
                if (loadedAdUnitIds[adUnitId] != true) {
                    loadedAdUnitIds.remove(adUnitId)
                    NapSspSdkBridge.clearInterstitial(adUnitId)
                    if (startPromises.containsKey(adUnitId)) {
                        Log.d(tag, "ignoring intermediate load failure for active start flow adUnitId=$adUnitId")
                    } else {
                        loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", msg)
                    }
                }
            }

            override fun onAdClicked() {
                NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
            }

            override fun onAdDisplayed() {
                NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
            }

            override fun onAdClosed() {
                NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLOSED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL))
                loadedAdUnitIds.remove(adUnitId)
                NapSspSdkBridge.clearInterstitial(adUnitId)
            }
        }

        val listenerClass = Class.forName("com.nasmedia.admixerssp.NapAdListener")
        val bridgeClass = Class.forName("com.nasmedia.admixerssp.NapListenerBridge")
        val listener = listenerClass.getConstructor(bridgeClass).newInstance(bridge)
        interstitialClass.getMethod("setAdListener", Class.forName("com.nasmedia.admixerssp.ads.AdListener")).invoke(interstitial, listener)
        interstitialAds[adUnitId] = interstitial
        return interstitial
    }

    private fun isDebuggableApp(): Boolean = (reactContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        loadPromises.values.toList().forEach { it.reject("NAP_SSP_INTERSTITIAL_LOAD_CANCELLED", "Module invalidated") }
        startPromises.values.toList().forEach { it.reject("NAP_SSP_INTERSTITIAL_START_CANCELLED", "Module invalidated") }
        loadPromises.clear()
        startPromises.clear()
        loadedAdUnitIds.clear()
        interstitialAds.values.forEach { interstitial ->
            runCatching {
                val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")
                interstitial.javaClass.getMethod("setAdListener", listenerClass).invoke(interstitial, null)
            }
            runCatching { interstitial.javaClass.getMethod("stopInterstitial").invoke(interstitial) }
        }
        interstitialAds.clear()
        super.invalidate()
    }
}
