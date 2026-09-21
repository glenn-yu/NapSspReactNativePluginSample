package com.nasmedia.admixerssp

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.nasmedia.admixerssp.ads.AMMInterstitial

class InterstitialModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val host = NapSspFullScreenAdHost(
        reactContext = reactContext,
        format = NapSspContracts.FORMAT_INTERSTITIAL,
        createAd = { context -> AMMInterstitial(context) },
        applyOptions = { builder, options ->
            // Close-button touch area is a percentage (20~100) on Android; the JS API keeps the
            // iOS-style 0.2~1.0 ratio and converts here.
            options?.optDouble("closeButtonTouchAreaRatio")?.let { ratio ->
                builder.setCloseButtonBound((ratio.coerceIn(0.2, 1.0) * 100).toInt())
            }
            options?.optBoolean("disableBackKey")?.let { builder.setDisableBackKey(it) }
        },
    )

    override fun getName(): String = NapSspContracts.INTERSTITIAL_MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.INTERSTITIAL_MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod fun load(adUnitId: String, options: ReadableMap?, promise: Promise) = host.load(adUnitId, options, promise)

    @ReactMethod fun start(adUnitId: String, options: ReadableMap?, promise: Promise) = host.start(adUnitId, options, promise)

    @ReactMethod fun show(adUnitId: String, promise: Promise) = host.show(adUnitId, promise)

    @ReactMethod fun isLoaded(adUnitId: String, promise: Promise) = host.isLoaded(adUnitId, promise)

    @ReactMethod fun isLoading(adUnitId: String, promise: Promise) = host.isLoading(adUnitId, promise)

    @ReactMethod fun cancelLoad(adUnitId: String, promise: Promise) = host.cancelLoad(adUnitId, promise)

    @ReactMethod fun destroy(adUnitId: String, promise: Promise) = host.destroy(adUnitId, promise)

    @ReactMethod fun addListener(eventName: String) = Unit

    @ReactMethod fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        host.invalidate()
        super.invalidate()
    }
}
