package com.gwangy

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

class RewardedAdModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val tag = "NapSspRewarded"
    private val loadedAdUnitIds = ConcurrentHashMap<String, Boolean>()
    private val rewardedAds = ConcurrentHashMap<String, Any>()
    private val loadPromises = ConcurrentHashMap<String, Promise>()
    private val startPromises = ConcurrentHashMap<String, Promise>()

    override fun getName(): String = NapSspContracts.REWARDED_MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.REWARDED_MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod
    fun load(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Rewarded adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspSdkBridge.markRewardedState(normalizedAdUnitId, NapSspLoadState.LOADED)
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
            )
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "Rewarded ads require a foreground Activity context")
            return
        }

        try {
            loadPromises[normalizedAdUnitId]?.reject("NAP_SSP_REWARDED_LOAD_CANCELLED", "Superseded by new load")
            loadPromises[normalizedAdUnitId] = promise
            val rewardedAd = createOrGetRewardedAd(normalizedAdUnitId, activity, options)
            Log.d(tag, "loadRewardVideoAd request adUnitId=$normalizedAdUnitId")
            rewardedAd.javaClass.getMethod("loadRewardVideoAd").invoke(rewardedAd)
        } catch (error: Throwable) {
            loadPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_REWARDED_LOAD_FAILED", error)
        }
    }

    @ReactMethod
    fun start(adUnitId: String, options: com.facebook.react.bridge.ReadableMap?, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Rewarded adUnitId is required")
            return
        }

        if (!BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            loadedAdUnitIds[normalizedAdUnitId] = true
            NapSspSdkBridge.markRewardedState(normalizedAdUnitId, NapSspLoadState.SHOWN)
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_LOADED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_OPENED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_IMPRESSION,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_REWARDED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED, "type" to "reward", "amount" to 1),
            )
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                NapSspContracts.EVENT_AD_CLOSED,
                mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
            )
            loadedAdUnitIds.remove(normalizedAdUnitId)
            NapSspSdkBridge.clearRewarded(normalizedAdUnitId)
            promise.resolve(null)
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("NAP_SSP_ACTIVITY_REQUIRED", "Rewarded ads require a foreground Activity context")
            return
        }

        try {
            loadPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_REWARDED_LOAD_CANCELLED", "Superseded by start")
            startPromises.remove(normalizedAdUnitId)?.reject("NAP_SSP_REWARDED_START_CANCELLED", "Superseded by new start")
            startPromises[normalizedAdUnitId] = promise
            val rewardedAd = createOrGetRewardedAd(normalizedAdUnitId, activity, options)
            Log.d(tag, "startRewardVideoAd request adUnitId=$normalizedAdUnitId")
            rewardedAd.javaClass.getMethod("loadRewardVideoAd").invoke(rewardedAd)
        } catch (error: Throwable) {
            startPromises.remove(normalizedAdUnitId)
            promise.reject("NAP_SSP_REWARDED_START_FAILED", error)
        }
    }

    @ReactMethod
    fun show(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Rewarded adUnitId is required")
            return
        }

        if (loadedAdUnitIds[normalizedAdUnitId] != true) {
            promise.reject("NAP_SSP_REWARDED_NOT_READY", "Rewarded ad has not been loaded yet")
            return
        }

        try {
            if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
                val rewardedAd = rewardedAds[normalizedAdUnitId]
                if (rewardedAd == null) {
                    promise.reject("NAP_SSP_REWARDED_NOT_READY", "Rewarded instance is missing")
                    return
                }
                NapSspSdkBridge.markRewardedState(normalizedAdUnitId, NapSspLoadState.SHOWN)
                Log.d(tag, "showRewardVideoAd request adUnitId=$normalizedAdUnitId")
                rewardedAd.javaClass.getMethod("showRewardVideoAd").invoke(rewardedAd)
            } else {
                NapSspSdkBridge.markRewardedState(normalizedAdUnitId, NapSspLoadState.SHOWN)
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_OPENED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_IMPRESSION,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_REWARDED,
                    mapOf(
                        "adUnitId" to normalizedAdUnitId,
                        "format" to NapSspContracts.FORMAT_REWARDED,
                        "type" to "reward",
                        "amount" to 1,
                    ),
                )
                NapSspEventEmitter.emitModuleEvent(
                    reactContext,
                    NapSspContracts.EVENT_AD_CLOSED,
                    mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
                )
                loadedAdUnitIds.remove(normalizedAdUnitId)
                NapSspSdkBridge.clearRewarded(normalizedAdUnitId)
            }
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("NAP_SSP_REWARDED_SHOW_FAILED", error)
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
        rewardedAds.remove(normalizedAdUnitId)?.let { rewardedAd ->
            runCatching { rewardedAd.javaClass.getMethod("stopRewardVideoAd").invoke(rewardedAd) }
        }
        NapSspSdkBridge.clearRewarded(normalizedAdUnitId)
        promise.resolve(null)
    }

    private fun applyRewardedOptions(builder: Any, builderClass: Class<*>, options: com.facebook.react.bridge.ReadableMap?) {
        if (options == null) return
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

        try {
            if (options.hasKey("mute") && options.getBoolean("mute")) {
                builderClass.getMethod("setMute", Boolean::class.javaPrimitiveType).invoke(builder, true)
            }
        } catch (_: Throwable) {}
    }

    private fun createOrGetRewardedAd(adUnitId: String, activity: android.app.Activity, options: com.facebook.react.bridge.ReadableMap?): Any {
        rewardedAds[adUnitId]?.let { return it }

        val rewardedClass = Class.forName("com.nasmedia.admixerssp.ads.RewardInterstitialVideoAd")
        val adInfoClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo")
        val builderClass = Class.forName("com.nasmedia.admixerssp.ads.AdInfo\$Builder")
        val listenerClass = Class.forName("com.nasmedia.admixerssp.ads.AdListener")

        val builder = builderClass.getConstructor(String::class.java).newInstance(adUnitId)
        applyRewardedOptions(builder!!, builderClass, options)
        try { builderClass.getMethod("setIsUseMediation", Boolean::class.java).invoke(builder, true) } catch (_: Throwable) {}
        val adInfo = builderClass.getMethod("build").invoke(builder)
        val rewardedAd = rewardedClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        rewardedClass.getMethod("setAdInfo", adInfoClass).invoke(rewardedAd, adInfo)

        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
            when (method.name) {
                "onReceivedAd" -> {
                    Log.d(tag, "onReceivedAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    loadedAdUnitIds[adUnitId] = true
                    NapSspSdkBridge.markRewardedState(adUnitId, NapSspLoadState.LOADED)
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_LOADED,
                        mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
                    )
                    val startPromise = startPromises.remove(adUnitId)
                    if (startPromise != null) {
                        Log.d(tag, "onReceivedAd startPromise=true adUnitId=$adUnitId")
                        runCatching {
                            NapSspSdkBridge.markRewardedState(adUnitId, NapSspLoadState.SHOWN)
                            NapSspEventEmitter.emitModuleEvent(
                                reactContext,
                                NapSspContracts.EVENT_AD_OPENED,
                                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED, "synthetic" to true, "stage" to "onReceivedAd_beforeShow"),
                            )
                            NapSspEventEmitter.emitModuleEvent(
                                reactContext,
                                NapSspContracts.EVENT_AD_IMPRESSION,
                                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED, "synthetic" to true, "stage" to "onReceivedAd_beforeShow"),
                            )
                            Log.d(tag, "auto-show reward after load adUnitId=$adUnitId")
                            rewardedAd.javaClass.getMethod("showRewardVideoAd").invoke(rewardedAd)
                            Log.d(tag, "showRewardVideoAd invoked from onReceivedAd adUnitId=$adUnitId")
                            startPromise.resolve(null)
                        }.onFailure {
                            Log.e(tag, "auto-show reward failed adUnitId=$adUnitId: ${it.message}", it)
                            startPromise.reject("NAP_SSP_REWARDED_SHOW_FAILED", it)
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
                            "format" to NapSspContracts.FORMAT_REWARDED,
                            "code" to code,
                            "message" to message,
                        ),
                    )
                    if (startPromises.containsKey(adUnitId)) {
                        Log.d(tag, "ignoring intermediate load failure for active start flow adUnitId=$adUnitId")
                    } else if (loadedAdUnitIds[adUnitId] != true) {
                        loadedAdUnitIds.remove(adUnitId)
                        NapSspSdkBridge.clearRewarded(adUnitId)
                        loadPromises.remove(adUnitId)?.reject("NAP_SSP_REWARDED_LOAD_FAILED", message)
                    }
                }
                "onEventAd" -> {
                    val eventName = args?.getOrNull(1)?.toString()
                    Log.d(tag, "onEventAd adUnitId=$adUnitId event=$eventName args=${args?.contentToString()}")
                    val normalizedEvent = eventName?.trim()?.uppercase()
                    Log.d(tag, "onEventAd normalized=$normalizedEvent adUnitId=$adUnitId")
                    when (normalizedEvent) {
                        "DISPLAYED", "OPEN", "OPENED", "SHOW", "SHOWN" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        }
                        "CLICK", "CLICKED", "LEFT_CLICK", "RIGHT_CLICK" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "COMPLETION", "COMPLETE", "COMPLETED" -> NapSspEventEmitter.emitModuleEvent(reactContext, "onVideoCompleted", mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "SKIPPED" -> NapSspEventEmitter.emitModuleEvent(reactContext, "onVideoSkipped", mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "EARNEDREWARD", "REWARDED", "REWARD" -> NapSspEventEmitter.emitModuleEvent(
                            reactContext,
                            NapSspContracts.EVENT_REWARDED,
                            mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED, "type" to "reward", "amount" to 1),
                        )
                        "CLOSE", "CLOSED", "DISMISS", "DISMISSED" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLOSED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                            loadedAdUnitIds.remove(adUnitId)
                            NapSspSdkBridge.clearRewarded(adUnitId)
                        }
                    }
                }
            }
            null
        }

        rewardedClass.getMethod("setListener", listenerClass).invoke(rewardedAd, listener)
        rewardedAds[adUnitId] = rewardedAd
        return rewardedAd
    }

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        loadPromises.values.forEach { it.reject("NAP_SSP_REWARDED_LOAD_CANCELLED", "Module invalidated") }
        loadPromises.clear()
        startPromises.values.forEach { it.reject("NAP_SSP_REWARDED_START_CANCELLED", "Module invalidated") }
        startPromises.clear()
        loadedAdUnitIds.clear()
        rewardedAds.values.forEach { rewardedAd ->
            runCatching { rewardedAd.javaClass.getMethod("stopRewardVideoAd").invoke(rewardedAd) }
        }
        rewardedAds.clear()
        super.invalidate()
    }
}
