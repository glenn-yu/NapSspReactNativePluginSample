package com.napsspplugin

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap

internal data class NapSspConfig(
    val mediaKey: String,
    val adUnitIds: List<String>,
    val mediations: NapSspMediationConfig? = null,
    val logLevel: String? = null,
    val coppa: Boolean = false,
)

internal data class NapSspMediationConfig(
    val adManager: Map<String, Any?>? = null,
    val pangle: Map<String, Any?>? = null,
    val appLovin: Map<String, Any?>? = null,
    val unityAds: Map<String, Any?>? = null,
    val adFitEnabled: Boolean = false,
)

internal enum class NapSspLoadState {
    IDLE,
    LOADING,
    LOADED,
    FAILED,
    SHOWN,
    DESTROYED,
}

internal fun ReadableMap.toNapSspConfig(): NapSspConfig {
    val mediaKey = getString("mediaKey")?.trim().orEmpty()
    require(mediaKey.isNotEmpty()) { "mediaKey is required" }

    val adUnitIds = readStringList("adUnitIds")
    require(adUnitIds.isNotEmpty()) { "adUnitIds must contain at least one item" }
    val mediations = if (hasKey("mediations") && !isNull("mediations")) {
        getMap("mediations")?.toMediationConfig()
    } else {
        null
    }

    val logLevel = if (hasKey("logLevel") && !isNull("logLevel")) getString("logLevel") else null
    val coppa = if (hasKey("coppa") && !isNull("coppa")) getBoolean("coppa") else false

    return NapSspConfig(
        mediaKey = mediaKey,
        adUnitIds = adUnitIds,
        mediations = mediations,
        logLevel = logLevel,
        coppa = coppa,
    )
}

private fun ReadableMap.toMediationConfig(): NapSspMediationConfig {
    return NapSspMediationConfig(
        adManager = readNestedMap("adManager"),
        pangle = readNestedMap("pangle"),
        appLovin = readNestedMap("appLovin"),
        unityAds = readNestedMap("unityAds"),
        adFitEnabled = if (hasKey("adFit") && !isNull("adFit")) getBoolean("adFit") else false,
    )
}

private fun ReadableMap.readNestedMap(key: String): Map<String, Any?>? {
    if (!hasKey(key) || isNull(key)) return null
    val value = getMap(key) ?: return null
    val result = mutableMapOf<String, Any?>()
    value.toHashMap().forEach { (nestedKey, nestedValue) -> result[nestedKey] = nestedValue }
    return result
}

private fun ReadableMap.readStringList(key: String): List<String> {
    if (!hasKey(key) || isNull(key)) return emptyList()
    val array: ReadableArray = getArray(key) ?: return emptyList()
    val result = mutableListOf<String>()
    for (index in 0 until array.size()) {
        val value = array.getString(index)?.trim().orEmpty()
        if (value.isNotEmpty()) {
            result.add(value)
        }
    }
    return result
}
