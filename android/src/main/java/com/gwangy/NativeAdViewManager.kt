package com.gwangy

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class NativeAdViewManager : SimpleViewManager<NapSspNativeAdView>() {
    override fun getName(): String = "NapSspNativeAdView"

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspNativeAdView = NapSspNativeAdView(reactContext)

    override fun onDropViewInstance(view: NapSspNativeAdView) {
        view.destroyNativeAd()
        super.onDropViewInstance(view)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: NapSspNativeAdView, adUnitId: String?) {
        view.adUnitId = adUnitId
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
