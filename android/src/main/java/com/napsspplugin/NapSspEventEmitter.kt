package com.napsspplugin

import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.RCTEventEmitter

internal object NapSspEventEmitter {
    fun emitModuleEvent(
        reactContext: ReactApplicationContext?,
        eventName: String,
        data: Map<String, Any?> = emptyMap(),
    ) {
        val context = reactContext ?: return
        if (!context.hasActiveReactInstance()) return
        val payload = toWritableMap(data + ("eventName" to eventName) + ("source" to "module"))
        context
            .getJSModule(com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, payload)
    }

    fun emitViewEvent(view: View, eventName: String, data: Map<String, Any?> = emptyMap()) {
        val reactContext = view.context as? ReactContext ?: return
        if (!reactContext.hasActiveReactInstance()) return
        val payload = toWritableMap(data + ("eventName" to eventName) + ("source" to "view"))
        reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(view.id, eventName, payload)
    }

    private fun toWritableMap(data: Map<String, Any?>): WritableMap {
        return Arguments.createMap().apply {
            data.forEach { (key, value) ->
                when (value) {
                    null -> putNull(key)
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    is Long -> putDouble(key, value.toDouble())
                    is Map<*, *> -> putMap(key, toWritableMap(value.entries.associate { it.key.toString() to it.value }))
                    is List<*> -> putArray(key, toWritableArray(value))
                    else -> putString(key, value.toString())
                }
            }
        }
    }

    private fun toWritableArray(values: List<*>): com.facebook.react.bridge.WritableArray {
        return Arguments.createArray().apply {
            values.forEach { value ->
                when (value) {
                    null -> pushNull()
                    is String -> pushString(value)
                    is Boolean -> pushBoolean(value)
                    is Int -> pushInt(value)
                    is Double -> pushDouble(value)
                    is Float -> pushDouble(value.toDouble())
                    is Long -> pushDouble(value.toDouble())
                    is Map<*, *> -> pushMap(toWritableMap(value.entries.associate { it.key.toString() to it.value }))
                    is List<*> -> pushArray(toWritableArray(value))
                    else -> pushString(value.toString())
                }
            }
        }
    }
}
