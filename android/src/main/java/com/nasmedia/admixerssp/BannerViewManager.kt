package com.nasmedia.admixerssp

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class BannerViewManager : SimpleViewManager<NapSspBannerView>() {
    override fun getName(): String = NapSspContracts.BANNER_VIEW_NAME

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspBannerView =
        NapSspBannerView(reactContext)

    override fun onDropViewInstance(view: NapSspBannerView) {
        view.destroyBanner()
        super.onDropViewInstance(view)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: NapSspBannerView, adUnitId: String?) {
        view.adUnitId = adUnitId
    }

    @ReactProp(name = "size")
    fun setSize(view: NapSspBannerView, size: String?) {
        view.size = size ?: "BANNER_320x50"
    }

    @ReactProp(name = "autoLoad", defaultBoolean = true)
    fun setAutoLoad(view: NapSspBannerView, autoLoad: Boolean) {
        view.autoLoad = autoLoad
    }

    override fun getCommandsMap(): MutableMap<String, Int> = mutableMapOf(COMMAND_RELOAD to 1)

    override fun receiveCommand(view: NapSspBannerView, commandId: String?, args: ReadableArray?) {
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
    )

    companion object {
        const val COMMAND_RELOAD = "reload"
    }
}
