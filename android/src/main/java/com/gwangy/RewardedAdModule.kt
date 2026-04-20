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
            val rewardedAd = createOrGetRewardedAd(normalizedAdUnitId, activity, options)
            Log.d(tag, "loadRewardVideoAd request adUnitId=$normalizedAdUnitId")
            rewardedAd.javaClass.getMethod("loadRewardVideoAd").invoke(rewardedAd)
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("NAP_SSP_REWARDED_LOAD_FAILED", error)
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
            runCatching { rewardedAd.javaClass.getMethod("onDestroy").invoke(rewardedAd) }
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
                }
                "onFailedToReceiveAd" -> {
                    Log.d(tag, "onFailedToReceiveAd adUnitId=$adUnitId args=${args?.contentToString()}")
                    loadedAdUnitIds.remove(adUnitId)
                    NapSspSdkBridge.clearRewarded(adUnitId)
                    NapSspEventEmitter.emitModuleEvent(
                        reactContext,
                        NapSspContracts.EVENT_AD_FAILED,
                        mapOf(
                            "adUnitId" to adUnitId,
                            "format" to NapSspContracts.FORMAT_REWARDED,
                            "code" to (args?.getOrNull(2) as? Int ?: -1),
                            "message" to (args?.getOrNull(3)?.toString() ?: "unknown"),
                        ),
                    )
                }
                "onEventAd" -> {
                    val eventName = args?.getOrNull(1)?.toString()
                    Log.d(tag, "onEventAd adUnitId=$adUnitId event=$eventName args=${args?.contentToString()}")
                    when (eventName) {
                        "DISPLAYED" -> {
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_OPENED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_IMPRESSION, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        }
                        "CLICK", "LEFT_CLICK", "RIGHT_CLICK" -> NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_AD_CLICKED, mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "COMPLETION" -> NapSspEventEmitter.emitModuleEvent(reactContext, "onVideoCompleted", mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "SKIPPED" -> NapSspEventEmitter.emitModuleEvent(reactContext, "onVideoSkipped", mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED))
                        "EARNEDREWARD" -> NapSspEventEmitter.emitModuleEvent(
                            reactContext,
                            NapSspContracts.EVENT_REWARDED,
                            mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_REWARDED, "type" to "reward", "amount" to 1),
                        )
                        "CLOSE" -> {
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
        loadedAdUnitIds.clear()
        rewardedAds.values.forEach { rewardedAd ->
            runCatching { rewardedAd.javaClass.getMethod("onDestroy").invoke(rewardedAd) }
        }
        rewardedAds.clear()
        super.invalidate()
    }
}
