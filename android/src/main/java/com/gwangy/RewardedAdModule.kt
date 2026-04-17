package com.gwangy

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.util.concurrent.ConcurrentHashMap

class RewardedAdModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val loadedAdUnitIds = ConcurrentHashMap<String, Boolean>()

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

        loadedAdUnitIds[normalizedAdUnitId] = true
        NapSspSdkBridge.markRewardedState(normalizedAdUnitId, NapSspLoadState.LOADED)
        NapSspEventEmitter.emitModuleEvent(
            reactContext,
            NapSspContracts.EVENT_AD_LOADED,
            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_REWARDED),
        )
        promise.resolve(null)
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
        promise.resolve(null)
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
        NapSspSdkBridge.clearRewarded(normalizedAdUnitId)
        promise.resolve(null)
    }

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        loadedAdUnitIds.clear()
        super.invalidate()
    }
}
