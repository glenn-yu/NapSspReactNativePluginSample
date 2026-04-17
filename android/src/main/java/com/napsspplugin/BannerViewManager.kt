package com.napsspplugin

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class BannerViewManager : SimpleViewManager<NapSspBannerView>() {
    override fun getName(): String = NapSspContracts.BANNER_VIEW_NAME

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspBannerView = NapSspBannerView(reactContext)

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
        view.size = size
    }

    @ReactProp(name = "autoLoad", defaultBoolean = true)
    fun setAutoLoad(view: NapSspBannerView, autoLoad: Boolean) {
        view.autoLoad = autoLoad
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            NapSspContracts.VIEW_EVENT_AD_LOADED to mapOf("registrationName" to "onAdLoaded"),
            NapSspContracts.VIEW_EVENT_AD_FAILED to mapOf("registrationName" to "onAdFailedToLoad"),
            NapSspContracts.VIEW_EVENT_AD_CLICKED to mapOf("registrationName" to "onAdClicked"),
            NapSspContracts.VIEW_EVENT_AD_OPENED to mapOf("registrationName" to "onAdOpened"),
            NapSspContracts.VIEW_EVENT_AD_CLOSED to mapOf("registrationName" to "onAdClosed"),
        )
    }
}
