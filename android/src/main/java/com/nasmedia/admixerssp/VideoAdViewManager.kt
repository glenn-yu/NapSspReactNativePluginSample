package com.nasmedia.admixerssp

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class VideoAdViewManager : SimpleViewManager<NapSspVideoAdView>() {
    override fun getName(): String = NapSspContracts.VIDEO_AD_VIEW_NAME

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspVideoAdView =
        NapSspVideoAdView(reactContext)

    override fun onDropViewInstance(view: NapSspVideoAdView) {
        view.destroyVideoAd()
        super.onDropViewInstance(view)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: NapSspVideoAdView, adUnitId: String?) {
        view.adUnitId = adUnitId
    }

    @ReactProp(name = "isRetry")
    fun setIsRetry(view: NapSspVideoAdView, isRetry: Boolean) {
        view.isRetry = isRetry
    }

    override fun getCommandsMap(): MutableMap<String, Int> = mutableMapOf(COMMAND_RELOAD to 1)

    override fun receiveCommand(view: NapSspVideoAdView, commandId: String?, args: ReadableArray?) {
        if (commandId == COMMAND_RELOAD || commandId == "1") {
            view.reload()
        }
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> = mutableMapOf(
        NapSspContracts.VIEW_EVENT_AD_LOADED to mutableMapOf("registrationName" to "onAdLoaded"),
        NapSspContracts.VIEW_EVENT_AD_FAILED to mutableMapOf("registrationName" to "onAdFailedToLoad"),
        NapSspContracts.VIEW_EVENT_AD_CLICKED to mutableMapOf("registrationName" to "onAdClicked"),
        NapSspContracts.VIEW_EVENT_AD_OPENED to mutableMapOf("registrationName" to "onAdOpened"),
        NapSspContracts.VIEW_EVENT_AD_CLOSED to mutableMapOf("registrationName" to "onAdClosed"),
        NapSspContracts.VIEW_EVENT_AD_IMPRESSION to mutableMapOf("registrationName" to "onAdImpression"),
        NapSspContracts.VIEW_EVENT_AD_COMPLETED to mutableMapOf("registrationName" to "onAdCompleted"),
        NapSspContracts.VIEW_EVENT_AD_SKIPPED to mutableMapOf("registrationName" to "onAdSkipped"),
    )

    companion object {
        const val COMMAND_RELOAD = "reload"
    }
}
