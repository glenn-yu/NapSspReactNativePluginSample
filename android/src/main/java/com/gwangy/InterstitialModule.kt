package com.gwangy

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.util.concurrent.ConcurrentHashMap

class InterstitialModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val loadedAdUnitIds = ConcurrentHashMap<String, Boolean>()

    override fun getName(): String = NapSspContracts.INTERSTITIAL_MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.INTERSTITIAL_MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod
    fun load(adUnitId: String, promise: Promise) {
        val normalizedAdUnitId = adUnitId.trim()
        if (normalizedAdUnitId.isEmpty()) {
            promise.reject("NAP_SSP_INVALID_AD_UNIT", "Interstitial adUnitId is required")
            return
        }

        loadedAdUnitIds[normalizedAdUnitId] = true
        NapSspSdkBridge.markInterstitialState(normalizedAdUnitId, NapSspLoadState.LOADED)
        NapSspEventEmitter.emitModuleEvent(
            reactContext,
            NapSspContracts.EVENT_AD_LOADED,
            mapOf("adUnitId" to normalizedAdUnitId, "format" to NapSspContracts.FORMAT_INTERSTITIAL),
        )
        promise.resolve(null)
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
        NapSspSdkBridge.clearInterstitial(normalizedAdUnitId)
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
