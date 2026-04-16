package com.napsspplugin

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class InterstitialModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "NapSspInterstitial"

    @ReactMethod
    fun load(adUnitId: String, promise: Promise) {
        // Placeholder: load interstitial ad via native SDK
        promise.resolve(true)
    }

    @ReactMethod
    fun show(promise: Promise) {
        // Placeholder: show interstitial if loaded
        promise.resolve(true)
    }
}
