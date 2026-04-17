package com.napsspplugin

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class NapSspModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "NapSspModule"

    @ReactMethod
    fun initialize(config: ReadableMap, promise: Promise) {
        try {
            val parsedConfig = config.toNapSspConfig()
            NapSspSdkBridge.initialize(parsedConfig)
            NapSspEventEmitter.emitModuleEvent(
                reactContext,
                "napSsp_initialized",
                NapSspSdkBridge.describeStatus(),
            )
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("NAP_SSP_INITIALIZE_FAILED", error.message, error)
        }
    }

    @ReactMethod
    fun setLogLevel(level: String, promise: Promise) {
        NapSspSdkBridge.setLogLevel(level)
        promise.resolve(null)
    }

    @ReactMethod
    fun setCoppa(enabled: Boolean, promise: Promise) {
        NapSspSdkBridge.setCoppa(enabled)
        promise.resolve(null)
    }

    @ReactMethod
    fun getStatus(promise: Promise) {
        promise.resolve(NapSspSdkBridge.describeStatus().toReadableMap())
    }

    override fun invalidate() {
        NapSspSdkBridge.reset()
        super.invalidate()
    }
}

private fun Map<String, Any?>.toReadableMap() = com.facebook.react.bridge.Arguments.createMap().apply {
    forEach { (key, value) ->
        when (value) {
            null -> putNull(key)
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Double -> putDouble(key, value)
            is Float -> putDouble(key, value.toDouble())
            is Long -> putDouble(key, value.toDouble())
            is List<*> -> {
                val array = com.facebook.react.bridge.Arguments.createArray()
                value.forEach { item ->
                    when (item) {
                        null -> array.pushNull()
                        is String -> array.pushString(item)
                        is Boolean -> array.pushBoolean(item)
                        is Int -> array.pushInt(item)
                        is Double -> array.pushDouble(item)
                        is Float -> array.pushDouble(item.toDouble())
                        is Long -> array.pushDouble(item.toDouble())
                        else -> array.pushString(item.toString())
                    }
                }
                putArray(key, array)
            }
            is Map<*, *> -> putMap(key, itemToReadableMap(value))
            else -> putString(key, value.toString())
        }
    }
}

private fun itemToReadableMap(value: Map<*, *>): com.facebook.react.bridge.WritableMap =
    com.facebook.react.bridge.Arguments.createMap().apply {
        value.forEach { (nestedKey, nestedValue) ->
            val key = nestedKey.toString()
            when (nestedValue) {
                null -> putNull(key)
                is String -> putString(key, nestedValue)
                is Boolean -> putBoolean(key, nestedValue)
                is Int -> putInt(key, nestedValue)
                is Double -> putDouble(key, nestedValue)
                is Float -> putDouble(key, nestedValue.toDouble())
                is Long -> putDouble(key, nestedValue.toDouble())
                else -> putString(key, nestedValue.toString())
            }
        }
    }
