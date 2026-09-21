package com.nasmedia.admixerssp

import com.nasmedia.admixerssp.reactnative.BuildConfig

internal object NapSspContracts {
    const val MODULE_NAME = "NapSspModule"
    const val INTERSTITIAL_MODULE_NAME = "NapSspInterstitial"
    const val REWARDED_MODULE_NAME = "NapSspRewarded"
    const val INTERSTITIAL_VIDEO_MODULE_NAME = "NapSspInterstitialVideo"
    const val BANNER_VIEW_NAME = "NapSspBannerView"
    const val NATIVE_AD_VIEW_NAME = "NapSspNativeAdView"
    const val VIDEO_AD_VIEW_NAME = "NapSspVideoAdView"

    const val EVENT_AD_LOADED = "onAdLoaded"
    const val EVENT_AD_FAILED = "onAdFailedToLoad"
    const val EVENT_AD_CLICKED = "onAdClicked"
    const val EVENT_AD_OPENED = "onAdOpened"
    const val EVENT_AD_CLOSED = "onAdClosed"
    const val EVENT_AD_IMPRESSION = "onAdImpression"
    const val EVENT_REWARDED = "onRewarded"
    const val EVENT_VIDEO_COMPLETED = "onVideoCompleted"
    const val EVENT_VIDEO_SKIPPED = "onVideoSkipped"
    const val EVENT_STATUS = "napSsp_status"

    const val VIEW_EVENT_AD_LOADED = "topAdLoaded"
    const val VIEW_EVENT_AD_FAILED = "topAdFailed"
    const val VIEW_EVENT_AD_CLICKED = "topAdClicked"
    const val VIEW_EVENT_AD_OPENED = "topAdOpened"
    const val VIEW_EVENT_AD_CLOSED = "topAdClosed"
    const val VIEW_EVENT_AD_IMPRESSION = "topAdImpression"
    const val VIEW_EVENT_AD_COMPLETED = "onVideoCompleted"
    const val VIEW_EVENT_AD_SKIPPED = "onVideoSkipped"

    const val FORMAT_BANNER = "banner"
    const val FORMAT_INTERSTITIAL = "interstitial"
    const val FORMAT_REWARDED = "rewarded"
    const val FORMAT_NATIVE_AD = "native"
    const val FORMAT_VIDEO = "video"
    const val FORMAT_INTERSTITIAL_VIDEO = "interstitial_video"

    val SUPPORTED_FORMATS = listOf(
        FORMAT_BANNER,
        FORMAT_INTERSTITIAL,
        FORMAT_REWARDED,
        FORMAT_NATIVE_AD,
        FORMAT_VIDEO,
        FORMAT_INTERSTITIAL_VIDEO,
    )

    val SUPPORTED_EVENTS = listOf(
        EVENT_AD_LOADED,
        EVENT_AD_FAILED,
        EVENT_AD_CLICKED,
        EVENT_AD_OPENED,
        EVENT_AD_CLOSED,
        EVENT_AD_IMPRESSION,
        EVENT_REWARDED,
        EVENT_VIDEO_COMPLETED,
        EVENT_VIDEO_SKIPPED,
    )

    private val enabledMediations: List<String>
        get() = BuildConfig.NAP_SSP_MEDIATIONS
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun sdkCoordinates(): Map<String, Any?> = linkedMapOf(
        "bom" to BuildConfig.NAP_SSP_BOM_COORDINATE,
        "core" to BuildConfig.NAP_SSP_CORE_COORDINATE,
        "mediations" to enabledMediations,
    )

    fun moduleConstants(moduleName: String): Map<String, Any?> = linkedMapOf(
        "moduleName" to moduleName,
        "supportedFormats" to SUPPORTED_FORMATS,
        "supportedEvents" to SUPPORTED_EVENTS,
        "sdkCoordinates" to sdkCoordinates(),
    )

    fun statusSnapshot(
        initialized: Boolean,
        logLevel: String,
        privacy: Map<String, Any?>,
        testMode: Boolean,
        testDeviceIdCount: Int,
        configuredAdUnitIds: Collection<String>,
        runtimeState: Map<String, Any?>,
    ): Map<String, Any?> = linkedMapOf(
        "initialized" to initialized,
        "platform" to "android",
        "logLevel" to logLevel,
        "privacy" to privacy,
        // Mirrors privacy.childDirected; kept for backwards compatibility with 0.4.x.
        "coppa" to (privacy["childDirected"] == true),
        "testMode" to testMode,
        "testDeviceIdCount" to testDeviceIdCount,
        "configuredAdUnitIds" to configuredAdUnitIds.toList(),
        "supportedFormats" to SUPPORTED_FORMATS,
        "supportedEvents" to SUPPORTED_EVENTS,
        "sdkCoordinates" to sdkCoordinates(),
        "runtime" to runtimeState,
    )
}
