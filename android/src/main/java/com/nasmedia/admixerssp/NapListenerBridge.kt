package com.nasmedia.admixerssp

interface NapListenerBridge {
    fun onReceivedAd(adapterName: String, ad: Any)
    fun onFailedToReceiveAd(ad: Any?, name: String, code: Int, msg: String?)
    fun onAdShowFailed(ad: Any?, name: String, code: Int, msg: String?) {}
    fun onAdClicked() {}
    fun onAdClosed() {}
    fun onAdDisplayed() {}
    fun onAdCompleted() {}
    fun onAdSkipped() {}
    fun onAdRewarded() {}
}
