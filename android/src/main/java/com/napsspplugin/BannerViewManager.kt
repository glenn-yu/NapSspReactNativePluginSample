package com.napsspplugin

import android.view.View
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class BannerViewManager: SimpleViewManager<View>() {
    override fun getName(): String = "NapSspBannerView"

    override fun createViewInstance(reactContext: ThemedReactContext): View {
        // Placeholder: Return an empty View. Replace with actual AdView from nap ssp SDK.
        return View(reactContext)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: View, adUnitId: String?) {
        // Placeholder: store adUnitId and trigger load when set
    }
}
