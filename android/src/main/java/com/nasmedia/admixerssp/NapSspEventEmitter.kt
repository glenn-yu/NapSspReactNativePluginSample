package com.nasmedia.admixerssp

import android.view.View
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.events.RCTEventEmitter

internal object NapSspEventEmitter {

    fun emitModuleEvent(
        reactContext: ReactApplicationContext?,
        eventName: String,
        data: Map<String, Any?> = emptyMap(),
    ) {
        val context = reactContext ?: return
        if (!context.hasActiveReactInstance()) return

        val payload = (data + mapOf("eventName" to eventName, "source" to "module")).toWritableMap()
        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, payload)
    }

    fun emitViewEvent(view: View, eventName: String, data: Map<String, Any?> = emptyMap()) {
        val reactContext = view.context as? ReactContext ?: return
        if (!reactContext.hasActiveReactInstance()) return

        val payload = (data + mapOf("eventName" to eventName, "source" to "view")).toWritableMap()
        reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(view.id, eventName, payload)
    }
}
