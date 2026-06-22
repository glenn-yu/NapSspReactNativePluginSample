package com.nasmedia.admixerssp

import com.nasmedia.admixerssp.ads.AdListener

class NapAdListener(private val bridge: NapListenerBridge) : AdListener() {
    override fun onReceivedAd(adapterName: String, ad: Any) {
        bridge.onReceivedAd(adapterName, ad)
    }

    override fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?) {
        bridge.onFailedToReceiveAd(ad, name, code, msg)
    }

    override fun onAdShowFailed(ad: Any?, name: String, code: Int, msg: String?) {
        bridge.onAdShowFailed(ad, name, code, msg)
    }

    override fun onAdClicked() {
        bridge.onAdClicked()
    }

    override fun onAdClosed() {
        bridge.onAdClosed()
    }

    override fun onAdDisplayed() {
        bridge.onAdDisplayed()
    }

    override fun onAdCompleted() {
        bridge.onAdCompleted()
    }

    override fun onAdSkipped() {
        bridge.onAdSkipped()
    }

    override fun onAdRewarded() {
        bridge.onAdRewarded()
    }
}
