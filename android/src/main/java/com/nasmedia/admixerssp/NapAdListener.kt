package com.nasmedia.admixerssp

import com.nasmedia.admixerssp.ads.AdListener
import com.nasmedia.admixerssp.ads.RewardInfo
import com.nasmedia.admixerssp.common.core.AdNetworkType

/**
 * Bridges the SDK's [AdListener] onto the plugin.
 *
 * Only the non-deprecated overloads are overridden: the SDK still invokes the legacy
 * `String adapterName` variants, but those are scheduled for removal in SDK 3.0 and their default
 * implementations already delegate to the standard callbacks used here.
 * https://napmx.github.io/#/android/native/api-reference
 *
 * `AMMBannerView` keeps its listener in a `WeakReference`, so callers must retain the instance for
 * as long as the ad view lives.
 */
internal open class NapAdListener(
    private val dispatch: (() -> Unit) -> Unit = { it() },
    private val isActive: () -> Boolean = { true },
    private val onReceived: (network: AdNetworkType, ad: Any) -> Unit = { _, _ -> },
    private val onLoadFailed: (code: Int, message: String?) -> Unit = { _, _ -> },
    private val onShowFailed: (code: Int, message: String?) -> Unit = { _, _ -> },
    private val onDisplayed: () -> Unit = {},
    private val onClicked: () -> Unit = {},
    private val onClosed: () -> Unit = {},
    private val onCompleted: () -> Unit = {},
    private val onSkipped: () -> Unit = {},
    private val onRewarded: (transactionId: String?) -> Unit = {},
) : AdListener() {

    override fun onReceivedAd(networkType: AdNetworkType, adView: Any) {
        dispatch { if (isActive()) onReceived(networkType, adView) }
    }

    override fun onFailedToReceiveAd(errorCode: Int, errorMsg: String?) {
        dispatch { if (isActive()) onLoadFailed(errorCode, errorMsg) }
    }

    override fun onAdShowFailed(adView: Any?, networkType: AdNetworkType, errorCode: Int, errorMsg: String?) {
        dispatch { if (isActive()) onShowFailed(errorCode, errorMsg) }
    }

    override fun onAdDisplayed() { dispatch { if (isActive()) onDisplayed() } }

    override fun onAdClicked() { dispatch { if (isActive()) onClicked() } }

    override fun onAdClosed() { dispatch { if (isActive()) onClosed() } }

    override fun onAdCompleted() { dispatch { if (isActive()) onCompleted() } }

    override fun onAdSkipped() { dispatch { if (isActive()) onSkipped() } }

    // The SDK only calls the RewardInfo overload (its default impl delegates to the no-arg one),
    // so overriding this single method guarantees exactly one reward notification.
    override fun onAdRewarded(info: RewardInfo) {
        dispatch { if (isActive()) onRewarded(info.transactionId) }
    }
}
