package com.gwangy

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class VideoAdViewManager : SimpleViewManager<NapSspVideoAdView>() {
    override fun getName(): String = NapSspContracts.VIDEO_AD_VIEW_NAME

    override fun createViewInstance(reactContext: ThemedReactContext): NapSspVideoAdView = NapSspVideoAdView(reactContext)

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

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            NapSspContracts.VIEW_EVENT_AD_LOADED to mapOf("registrationName" to "onAdLoaded"),
            NapSspContracts.VIEW_EVENT_AD_FAILED to mapOf("registrationName" to "onAdFailedToLoad"),
            NapSspContracts.VIEW_EVENT_AD_CLICKED to mapOf("registrationName" to "onAdClicked"),
            NapSspContracts.VIEW_EVENT_AD_OPENED to mapOf("registrationName" to "onAdOpened"),
            NapSspContracts.VIEW_EVENT_AD_CLOSED to mapOf("registrationName" to "onAdClosed"),
            "onVideoCompleted" to mapOf("registrationName" to "onAdCompleted"),
            "onVideoSkipped" to mapOf("registrationName" to "onAdSkipped")
        )
    }
}
