package com.nasmedia.admixerssp

import com.nasmedia.admixerssp.common.AdMixer

/**
 * Maps the SDK's `AdMixer.AX_ERR_*` integers onto stable string codes for JavaScript.
 * The numeric value is always forwarded as `nativeCode` so callers can still branch on it.
 * https://napmx.github.io/#/android/native/error-codes
 */
internal object NapSspAdErrors {
    const val INVALID_AD_UNIT = "nap_ssp_invalid_ad_unit"
    const val NOT_INITIALIZED = "nap_ssp_not_initialized"
    const val ACTIVITY_REQUIRED = "nap_ssp_activity_required"
    const val NOT_READY = "nap_ssp_ad_not_ready"
    const val LOAD_FAILED = "nap_ssp_load_failed"
    const val SHOW_FAILED = "nap_ssp_show_failed"
    const val LOAD_CANCELLED = "nap_ssp_load_cancelled"
    const val DESTROYED = "nap_ssp_destroyed"

    fun stringCode(nativeCode: Int): String = when (nativeCode) {
        AdMixer.AX_ERR_INIT -> "nap_ssp_sdk_not_initialized"
        AdMixer.AX_ERR_ADUNIT -> "nap_ssp_invalid_ad_unit"
        AdMixer.AX_ERR_TIMEOUT -> "nap_ssp_timeout"
        AdMixer.AX_ERR_NO_ADAPTER -> "nap_ssp_no_adapter"
        AdMixer.AX_ERR_ADAPTER -> "nap_ssp_adapter_error"
        AdMixer.AX_ERR_CONFIG_FAIL -> "nap_ssp_config_failed"
        // Every no-fill in the waterfall is reported as AX_ERR_NO_ADS; AX_ERR_NO_FILL is never sent.
        AdMixer.AX_ERR_NO_ADS -> "nap_ssp_no_ads"
        AdMixer.AX_ERR_INVALID_REQUEST -> "nap_ssp_invalid_request"
        AdMixer.AX_ERR_NETWORK -> "nap_ssp_network_error"
        else -> "nap_ssp_error"
    }

    fun payload(adUnitId: String, format: String, nativeCode: Int, message: String?): Map<String, Any?> =
        linkedMapOf(
            "adUnitId" to adUnitId,
            "format" to format,
            "code" to stringCode(nativeCode),
            "nativeCode" to nativeCode,
            "message" to (message ?: stringCode(nativeCode)),
        )
}
