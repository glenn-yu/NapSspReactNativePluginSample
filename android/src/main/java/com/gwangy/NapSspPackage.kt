package com.gwangy

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class NapSspPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(
            NapSspModule(reactContext),
            InterstitialModule(reactContext),
            RewardedAdModule(reactContext),
            InterstitialVideoModule(reactContext),
        )
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return listOf(
            BannerViewManager(),
            NativeAdViewManager(),
            VideoAdViewManager(),
        )
    }
}
