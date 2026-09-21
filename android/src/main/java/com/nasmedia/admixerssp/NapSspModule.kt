package com.nasmedia.admixerssp

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

class NapSspModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = NapSspContracts.MODULE_NAME

    override fun getConstants(): MutableMap<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return NapSspContracts.moduleConstants(NapSspContracts.MODULE_NAME).toMutableMap() as MutableMap<String, Any>
    }

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    @ReactMethod
    fun initialize(config: ReadableMap, promise: Promise) {
        try {
            NapSspSdkBridge.initialize(reactApplicationContext, config.toNapSspConfig())
            val status = NapSspSdkBridge.describeStatus()
            NapSspEventEmitter.emitModuleEvent(reactContext, NapSspContracts.EVENT_STATUS, status)
            promise.resolve(status.toWritableMap())
        } catch (error: Throwable) {
            promise.reject("nap_ssp_initialize_failed", error.message ?: error.toString(), error)
        }
    }

    @ReactMethod
    fun setLogLevel(level: String, promise: Promise) {
        try {
            NapSspSdkBridge.setLogLevel(level)
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("nap_ssp_set_log_level_failed", error.message ?: error.toString(), error)
        }
    }

    /**
     * Applies GDPR / CCPA / COPPA signals. Call before [initialize] — adapters read these when the
     * waterfall lazily initialises each network SDK.
     */
    @ReactMethod
    fun setPrivacyConsent(consent: ReadableMap, promise: Promise) {
        try {
            NapSspSdkBridge.applyPrivacy(
                NapSspPrivacyConsent(
                    childDirected = consent.optBoolean("childDirected"),
                    gdprConsent = consent.optBoolean("gdprConsent"),
                    ccpaDoNotSell = consent.optBoolean("ccpaDoNotSell"),
                    usPrivacy = consent.optString("usPrivacy")?.takeIf { it.isNotEmpty() },
                ),
            )
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("nap_ssp_set_privacy_failed", error.message ?: error.toString(), error)
        }
    }

    /** Backwards-compatible alias for `setPrivacyConsent({ childDirected })`. */
    @ReactMethod
    fun setCoppa(enabled: Boolean, promise: Promise) {
        try {
            NapSspSdkBridge.applyPrivacy(NapSspPrivacyConsent(childDirected = enabled))
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("nap_ssp_set_coppa_failed", error.message ?: error.toString(), error)
        }
    }

    @ReactMethod
    fun setTestMode(enabled: Boolean, promise: Promise) {
        try {
            NapSspSdkBridge.setTestMode(enabled)
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("nap_ssp_set_test_mode_failed", error.message ?: error.toString(), error)
        }
    }

    @ReactMethod
    fun setTestDeviceIds(ids: ReadableArray, promise: Promise) {
        try {
            NapSspSdkBridge.setTestDeviceIds(ids.toStringList())
            promise.resolve(null)
        } catch (error: Throwable) {
            promise.reject("nap_ssp_set_test_devices_failed", error.message ?: error.toString(), error)
        }
    }

    @ReactMethod
    fun getStatus(promise: Promise) {
        promise.resolve(NapSspSdkBridge.describeStatus().toWritableMap())
    }

    override fun invalidate() {
        NapSspSdkBridge.reset()
        super.invalidate()
    }
}

internal fun Map<String, Any?>.toWritableMap(): WritableMap = Arguments.createMap().apply {
    forEach { (key, value) -> putAny(key, value) }
}

private fun WritableMap.putAny(key: String, value: Any?) {
    when (value) {
        null -> putNull(key)
        is String -> putString(key, value)
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        is Long -> putDouble(key, value.toDouble())
        is Float -> putDouble(key, value.toDouble())
        is Double -> putDouble(key, value)
        is Map<*, *> -> putMap(key, value.toWritableMapAny())
        is Collection<*> -> putArray(key, value.toWritableArray())
        else -> putString(key, value.toString())
    }
}

private fun Map<*, *>.toWritableMapAny(): WritableMap = Arguments.createMap().apply {
    forEach { (key, value) -> putAny(key.toString(), value) }
}

private fun Collection<*>.toWritableArray(): WritableArray = Arguments.createArray().apply {
    forEach { value ->
        when (value) {
            null -> pushNull()
            is String -> pushString(value)
            is Boolean -> pushBoolean(value)
            is Int -> pushInt(value)
            is Long -> pushDouble(value.toDouble())
            is Float -> pushDouble(value.toDouble())
            is Double -> pushDouble(value)
            is Map<*, *> -> pushMap(value.toWritableMapAny())
            is Collection<*> -> pushArray(value.toWritableArray())
            else -> pushString(value.toString())
        }
    }
}
