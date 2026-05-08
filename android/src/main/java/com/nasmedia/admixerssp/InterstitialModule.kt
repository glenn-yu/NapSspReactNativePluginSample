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
            Log.d(tag, "startInterstitial request adUnitId=$normalizedAdUnitId")
            interstitial.javaClass.getMethod("startInterstitial").invoke(interstitial)
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
        try { builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = builderClass.getMethod("build").invoke(builder)
        val interstitial = interstitialClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        interstitialClass.getMethod("setAdInfo", adInfoClass).invoke(interstitial, adInfo)

        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    Log.d(tag, "onReceivedAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    val hasInterstitial = runCatching {
                        interstitial.javaClass.getField("hasInterstitial").getBoolean(interstitial)
                    }.getOrDefault(true)

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
                        return@newProxyInstance null
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
                        startPromise.resolve(null)
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
                "onFailedToReceiveAd" -> {
                    Log.d(tag, "onFailedToReceiveAd adUnitId=$adUnitId args=${args?.contentToString()}")
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
                    if (loadedAdUnitIds[adUnitId] != true) {
                        loadedAdUnitIds.remove(adUnitId)
                        NapSspSdkBridge.clearInterstitial(adUnitId)
                        if (startPromises.containsKey(adUnitId)) {
                            Log.d(tag, "ignoring intermediate load failure for active start flow adUnitId=$adUnitId")
                        } else {
                            loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_LOAD_FAILED", message)
                        }
                    }
                }
                "onEventAd" -> {
                    val rawEvent = args?.getOrNull(1)
                    val eventName = rawEvent?.toString()?.trim()?.uppercase()
                    Log.d(tag, "onEventAd adUnitId=$adUnitId rawEvent=$rawEvent normalized=$eventName args=${args?.contentToString()}")
                    when (eventName) {
                        "DISPLAYED", "OPEN", "OPENED", "SHOW", "SHOWN" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL, "rawEvent" to rawEvent?.toString()))
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL, "rawEvent" to rawEvent?.toString()))
                        }
                        "CLICK", "CLICKED", "LEFT_CLICK", "RIGHT_CLICK" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL, "rawEvent" to rawEvent?.toString()))
                        "CLOSE", "CLOSED", "DISMISS", "DISMISSED" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLOSED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL, "rawEvent" to rawEvent?.toString()))
                            loadedAdUnitIds.remove(adUnitId)
                            NapSspSdkBridge.clearInterstitial(adUnitId)
                        }
                        else -> {
                            Log.d(tag, "onEventAd unhandled adUnitId=$adUnitId rawEvent=$rawEvent normalized=$eventName")
                        }
                    }
                }
            }
            null
        }

        interstitialClass.getMethod("setAdListener", listenerClass).invoke(interstitial, listener)
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
