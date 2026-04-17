package com.napsspplugin

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class BannerViewManager : SimpleViewManager<NapSspBannerView>() {
    override fun getName(): String = "NapSspBannerView"

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspBannerView = NapSspBannerView(reactContext)

    override fun onDropViewInstance(view: NapSspBannerView) {
        view.destroyBanner()
        super.onDropViewInstance(view)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: NapSspBannerView, adUnitId: String?) {
        view.adUnitId = adUnitId?.trim().orEmpty()
    }

    @ReactProp(name = "size")
    fun setSize(view: NapSspBannerView, size: String?) {
        view.size = size?.trim().orEmpty()
    }

    @ReactProp(name = "autoLoad", defaultBoolean = true)
    fun setAutoLoad(view: NapSspBannerView, autoLoad: Boolean) {
        view.autoLoad = autoLoad
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "topAdLoaded" to mapOf("registrationName" to "onAdLoaded"),
            "topAdFailed" to mapOf("registrationName" to "onAdFailedToLoad"),
            "topAdClicked" to mapOf("registrationName" to "onAdClicked"),
            "topAdOpened" to mapOf("registrationName" to "onAdOpened"),
            "topAdClosed" to mapOf("registrationName" to "onAdClosed"),
        )
    }
}
