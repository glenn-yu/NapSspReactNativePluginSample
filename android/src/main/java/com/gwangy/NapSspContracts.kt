package com.gwangy

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
    const val EVENT_STATUS = "napSsp_status"

    const val VIEW_EVENT_AD_LOADED = "topAdLoaded"
    const val VIEW_EVENT_AD_FAILED = "topAdFailed"
    const val VIEW_EVENT_AD_CLICKED = "topAdClicked"
    const val VIEW_EVENT_AD_OPENED = "topAdOpened"
    const val VIEW_EVENT_AD_CLOSED = "topAdClosed"

    const val FORMAT_BANNER = "banner"
    const val FORMAT_INTERSTITIAL = "interstitial"
    const val FORMAT_REWARDED = "rewarded"
    const val FORMAT_NATIVE_AD = "native"
    const val FORMAT_VIDEO = "video"
    const val FORMAT_INTERSTITIAL_VIDEO = "interstitial_video"

    val SUPPORTED_FORMATS = listOf(FORMAT_BANNER, FORMAT_INTERSTITIAL, FORMAT_REWARDED, FORMAT_NATIVE_AD, FORMAT_VIDEO, FORMAT_INTERSTITIAL_VIDEO)
    val SUPPORTED_EVENTS = listOf(
        EVENT_AD_LOADED,
        EVENT_AD_FAILED,
        EVENT_AD_CLICKED,
        EVENT_AD_OPENED,
        EVENT_AD_CLOSED,
        EVENT_AD_IMPRESSION,
        EVENT_REWARDED,
        "onVideoCompleted",
        "onVideoSkipped"
    )

    fun moduleConstants(moduleName: String): Map<String, Any?> = linkedMapOf(
        "moduleName" to moduleName,
        "placeholderMode" to !BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED,
        "vendorSdkEnabled" to BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED,
        "supportedFormats" to SUPPORTED_FORMATS,
        "supportedEvents" to SUPPORTED_EVENTS,
        "sdkCoordinates" to linkedMapOf(
            "core" to BuildConfig.NAP_SSP_CORE_COORDINATE,
            "adManager" to BuildConfig.NAP_SSP_AD_MANAGER_COORDINATE,
            "adFit" to BuildConfig.NAP_SSP_ADFIT_COORDINATE,
            "pangle" to BuildConfig.NAP_SSP_PANGLE_COORDINATE,
            "appLovin" to BuildConfig.NAP_SSP_APP_LOVIN_COORDINATE,
            "unity" to BuildConfig.NAP_SSP_UNITY_COORDINATE,
            "adsIdentifier" to BuildConfig.NAP_SSP_ADS_IDENTIFIER_COORDINATE,
        ),
    )

    fun statusSnapshot(
        initialized: Boolean,
        logLevel: String,
        coppaEnabled: Boolean,
        configuredAdUnitIds: Collection<String>,
        runtimeState: Map<String, Any?>,
    ): Map<String, Any?> = linkedMapOf(
        "initialized" to initialized,
        "placeholderMode" to !BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED,
        "vendorSdkEnabled" to BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED,
        "logLevel" to logLevel,
        "coppa" to coppaEnabled,
        "configuredAdUnitIds" to configuredAdUnitIds.toList(),
        "supportedFormats" to SUPPORTED_FORMATS,
        "supportedEvents" to SUPPORTED_EVENTS,
        "sdkCoordinates" to linkedMapOf(
            "core" to BuildConfig.NAP_SSP_CORE_COORDINATE,
            "adManager" to BuildConfig.NAP_SSP_AD_MANAGER_COORDINATE,
            "adFit" to BuildConfig.NAP_SSP_ADFIT_COORDINATE,
            "pangle" to BuildConfig.NAP_SSP_PANGLE_COORDINATE,
            "appLovin" to BuildConfig.NAP_SSP_APP_LOVIN_COORDINATE,
            "unity" to BuildConfig.NAP_SSP_UNITY_COORDINATE,
            "adsIdentifier" to BuildConfig.NAP_SSP_ADS_IDENTIFIER_COORDINATE,
        ),
        "runtime" to runtimeState,
    )
}
