package com.napsspplugin

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class NapSspModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "NapSspModule"

    @ReactMethod
    fun initialize(config: String, promise: Promise) {
        // Placeholder: parse config JSON and call native SDK initialization
        promise.resolve(null)
    }

    @ReactMethod
    fun setLogLevel(level: String) {
        // Placeholder
    }
}
