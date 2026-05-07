package com.gwangy

import android.content.pm.ApplicationInfo
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

class InterstitialVideoModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val tag = "NapSspInterstitialVideo"
    private val loadedAdUnitIds = ConcurrentHashMap<String, Boolean>()
    private val interstitialVideoAds = ConcurrentHashMap<String, Any>()
    private val loadPromises = ConcurrentHashMap<String, Promise>()
    private val startPromises = ConcurrentHashMap<String, Promise>()

    override fun getName(): String = NapSspContracts.INTERSTITIAL_VIDEO_MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.INTERSTITIAL_VIDEO_MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod
    fun load(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "InterstitialVideo adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "InterstitialVideo ads require a foreground Activity context")
            return
        }

        try {
            loadPromises[normalizedAdUnitId]?.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_CANCELLED", "Superseded by new load")
            loadPromises[normalizedAdUnitId] = promise
            val interstitialVideo = createOrGetInterstitialVideo(normalizedAdUnitId, activity, options)
            Log.d(tag, "loadInterstitialVideoAd request adUnitId=$normalizedAdUnitId")
            interstitialVideo.javaClass.getMethod("loadInterstitialVideoAd").invoke(interstitialVideo)
        } catch (error: Throwable) {
            loadPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_FAILED", error)
        }
    }

    @ReactMethod
    fun start(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "InterstitialVideo adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED || isDebuggableApp()) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_OPENED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_IMPRESSION,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_VIDEO_COMPLETED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_CLOSED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
            )
            loadedAdUnitIds.remove(normalizedAdUnitId)
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "InterstitialVideo ads require a foreground Activity context")
            return
        }

        try {
            val interstitialVideo = createOrGetInterstitialVideo(normalizedAdUnitId, activity, options)
            loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_CANCELLED", "Superseded by start")
            startPromises[normalizedAdUnitId]?.reject("NAP_SSP_INTERSTITIAL_VIDEO_START_CANCELLED", "Superseded by start")
            startPromises[normalizedAdUnitId] = promise
            Log.d(tag, "startInterstitialVideoAd request adUnitId=$normalizedAdUnitId")
            interstitialVideo.javaClass.getMethod("loadInterstitialVideoAd").invoke(interstitialVideo)
        } catch (error: Throwable) {
            loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_START_FAILED", error)
            startPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_INTERSTITIAL_VIDEO_START_FAILED", error)
        }
    }

    @ReactMethod
    fun show(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "InterstitialVideo adUnitId is required")
            return
        }

        if (loadedAdUnitIds[normalizedAdUnitId] != true) {
            promise.reject("NAP_SSP_INTERSTITIAL_VIDEO_NOT_READY", "InterstitialVideo has not been loaded yet")
            return
        }

        try {
            if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
                val interstitialVideo = interstitialVideoAds[normalizedAdUnitId]
                if (interstitialVideo == null) {
                    promise.reject("NAP_SSP_INTERSTITIAL_VIDEO_NOT_READY", "InterstitialVideo instance is missing")
                    return
                }
                Log.d(tag, "showInterstitialVideoAd request adUnitId=$normalizedAdUnitId")
                interstitialVideo.javaClass.getMethod("showInterstitialVideoAd").invoke(interstitialVideo)
            } else {
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_OPENED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_IMPRESSION,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_VIDEO_COMPLETED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_CLOSED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
                )
                loadedAdUnitIds.remove(normalizedAdUnitId)
            }
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("NAP_SSP_INTERSTITIAL_VIDEO_SHOW_FAILED", error)
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
        loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_DESTROYED", "Destroyed before load completed")
        startPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_DESTROYED", "Destroyed before start completed")
        interstitialVideoAds.remove(normalizedAdUnitId)?.let { interstitialVideo ->
            runCatching { interstitialVideo.javaClass.getMethod("stopInterstitialVideoAd").invoke(interstitialVideo) }
        }
        promise.resolve(null)
    }

    private fun applyInterstitialVideoOptions(builder: Any, builderClass: Class<*>, options: com.facebook.react.bridge.ReadableMap?) {
        if (options == null) return
        try {
            if (options.hasKey("timeout")) {
                builderClass.getMethod("interstitialTimeout", Int::class.javaPrimitiveType)
                    .invoke(builder, options.getInt("timeout"))
            }
        } catch (_: Throwable) {}

        try {
            if (options.hasKey("maxRetryCountInSlot")) {
                builderClass.getMethod("maxRetryCountInSlot", Int::class.javaPrimitiveType)
                    .invoke(builder, options.getInt("maxRetryCountInSlot"))
            }
        } catch (_: Throwable) {}

        try {
            if (options.hasKey("customParams")) {
                val customParamsMap = options.getMap("customParams")
                if (customParamsMap != null) {
                    val hashMap = java.util.HashMap<String, String>()
                    val iterator = customParamsMap.keySetIterator()
                    while (iterator.hasNextKey()) {
                        val key = iterator.nextKey()
                        customParamsMap.getString(key)?.let { hashMap[key] = it }
                    }
                    if (hashMap.isNotEmpty()) {
                        builderClass.getMethod("setCustomParams", java.util.HashMap::class.java)
                            .invoke(builder, hashMap)
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun createOrGetInterstitialVideo(adUnitId: String, activity: android.app.Activity, options: com.facebook.react.bridge.ReadableMap?): Any {
        interstitialVideoAds[adUnitId]?.let { return it }

        val interstitialVideoClass = Class.forName("com.nasmedia.admixerssp.ads.InterstitialVideoAd")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        val builder = builderClass.getConstructor(String::class.java).newInstance(adUnitId)
        applyInterstitialVideoOptions(builder!!, builderClass, options)
        try { builderClass.getMethod("setIsUseMediation", Boolean::class.javaPrimitiveType).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = builderClass.getMethod("build").invoke(builder)
        val interstitialVideo = interstitialVideoClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        interstitialVideoClass.getMethod("setAdInfo", adInfoClass).invoke(interstitialVideo, adInfo)

        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    Log.d(tag, "onReceivedAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    val hasInterstitial = runCatching {
                        interstitialVideo.javaClass.getField("hasInterstitial").getBoolean(interstitialVideo)
                    }.getOrDefault(true)

                    if (!hasInterstitial) {
                        val message = "No fill (hasInterstitial is false)"
                        NapSspEventEmitter.emitModuleEvent(
                            reactContext,
                            NapSspContracts.EVENT_AD_FAILED,
                            mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO, "code" to -1, "message" to message)
                        )
                        loadedAdUnitIds.remove(adUnitId)
                        if (startPromises.containsKey(adUnitId)) {
                            startPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_FAILED", message)
                        } else {
                            loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_FAILED", message)
                        }
                        return@newProxyInstance null
                    }

                    loadedAdUnitIds[adUnitId] = true
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_LOADED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO),
                    )
                    val startPromise = startPromises.remove(adUnitId)
                    if (startPromise != null) {
                        Log.d(tag, "onReceivedAd startPromise=true adUnitId=$adUnitId")
                        runCatching {
                            Log.d(tag, "auto-show interstitial video after load adUnitId=$adUnitId")
                            interstitialVideo.javaClass.getMethod("showInterstitialVideoAd").invoke(interstitialVideo)
                            Log.d(tag, "showInterstitialVideoAd invoked from onReceivedAd adUnitId=$adUnitId")
                            startPromise.resolve(null)
                        }.onFailure {
                            Log.e(tag, "auto-show interstitial video failed adUnitId=$adUnitId: ${it.message}", it)
                            startPromise.reject("NAP_SSP_INTERSTITIAL_VIDEO_SHOW_FAILED", it)
                        }
                    } else {
                        loadPromises.remove(adUnitId)?.resolve(null)
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
                            "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO,
                            "code" to code,
                            "message" to message,
                        ),
                    )
                    if (startPromises.containsKey(adUnitId)) {
                        Log.d(tag, "ignoring intermediate load failure for active start flow adUnitId=$adUnitId")
                    } else if (loadedAdUnitIds[adUnitId] != true) {
                        loadedAdUnitIds.remove(adUnitId)
                        loadPromises.remove(adUnitId)?.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_FAILED", message)
                    }
                }
                "onEventAd" -> {
                    val rawEvent = args?.getOrNull(1)
                    val eventName = rawEvent?.toString()?.trim()?.uppercase()
                    Log.d(tag, "onEventAd adUnitId=$adUnitId rawEvent=$rawEvent normalized=$eventName args=${args?.contentToString()}")
                    when (eventName) {
                        "DISPLAYED", "OPEN", "OPENED", "SHOW", "SHOWN" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO, "rawEvent" to rawEvent?.toString()))
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO, "rawEvent" to rawEvent?.toString()))
                        }
                        "CLICK", "CLICKED", "LEFT_CLICK", "RIGHT_CLICK" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO, "rawEvent" to rawEvent?.toString()))
                        "CLOSE", "CLOSED", "DISMISS", "DISMISSED" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLOSED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO, "rawEvent" to rawEvent?.toString()))
                            loadedAdUnitIds.remove(adUnitId)
                        }
                        "COMPLETION", "COMPLETE", "COMPLETED" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_VIDEO_COMPLETED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO))
                        "SKIPPED" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_VIDEO_SKIPPED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL_VIDEO))
                        else -> Log.d(tag, "onEventAd unhandled adUnitId=$adUnitId rawEvent=$rawEvent normalized=$eventName")
                    }
                }
            }
            null
        }

        interstitialVideoClass.getMethod("setListener", listenerClass).invoke(interstitialVideo, listener)
        interstitialVideoAds[adUnitId] = interstitialVideo
        return interstitialVideo
    }

    private fun isDebuggableApp(): Boolean = (reactContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        loadPromises.values.toList().forEach { it.reject("NAP_SSP_INTERSTITIAL_VIDEO_LOAD_CANCELLED", "Module invalidated") }
        startPromises.values.toList().forEach { it.reject("NAP_SSP_INTERSTITIAL_VIDEO_START_CANCELLED", "Module invalidated") }
        loadPromises.clear()
        startPromises.clear()
        loadedAdUnitIds.clear()
        interstitialVideoAds.values.forEach { interstitialVideo ->
            runCatching { 
                val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")
                interstitialVideo.javaClass.getMethod("setListener", listenerClass).invoke(interstitialVideo, null) 
            }
            runCatching { interstitialVideo.javaClass.getMethod("stopInterstitialVideoAd").invoke(interstitialVideo) }
        }
        interstitialVideoAds.clear()
        super.invalidate()
    }
}
