package com.gwangy

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class NativeAdViewManager : SimpleViewManager<NapSspNativeAdView>() {
    override fun getName(): String = NapSspContracts.NATIVE_AD_VIEW_NAME

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
            NapSspContracts.VIEW_EVENT_AD_LOADED to mutableMapOf("registrationName" to "onAdLoaded"),
            NapSspContracts.VIEW_EVENT_AD_FAILED to mutableMapOf("registrationName" to "onAdFailedToLoad"),
            NapSspContracts.VIEW_EVENT_AD_CLICKED to mutableMapOf("registrationName" to "onAdClicked"),
            NapSspContracts.VIEW_EVENT_AD_OPENED to mutableMapOf("registrationName" to "onAdOpened"),
            NapSspContracts.VIEW_EVENT_AD_CLOSED to mutableMapOf("registrationName" to "onAdClosed"),
            NapSspContracts.VIEW_EVENT_AD_IMPRESSION to mutableMapOf("registrationName" to "onAdImpression")
        )
    }
}
