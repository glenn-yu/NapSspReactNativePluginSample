package com.nasmedia.admixerssp

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType

internal data class NapSspConfig(
    val mediaKey: String,
    val adUnitIds: List<String>,
    val logLevel: String? = null,
    val privacy: NapSspPrivacyConsent = NapSspPrivacyConsent(),
    val testMode: Boolean? = null,
    val testDeviceIds: List<String> = emptyList(),
    /**
     * Network keys the host app supplies itself, keyed by `AdMixer.ADAPTER_*`.
     * Applied through `AdInfo.Builder.setAdapterConfig`; the media-conf server always wins, so
     * these only fill in keys the server did not send (Server-Precedence).
     */
    val adapterConfig: Map<String, Map<String, String>> = emptyMap(),
    /** Informational copy of the `mediations` block, surfaced through `getStatus()`. */
    val mediationFlags: Map<String, Any?> = emptyMap(),
)

/**
 * Tri-state privacy signals. `null` means "unspecified": the SDK leaves the vendor default or the
 * CMP-read value untouched, which is a different state from an explicit `false`.
 * https://napmx.github.io/#/android/native/privacy
 */
internal data class NapSspPrivacyConsent(
    val childDirected: Boolean? = null,
    val gdprConsent: Boolean? = null,
    val ccpaDoNotSell: Boolean? = null,
    val usPrivacy: String? = null,
) {
    fun mergedWith(other: NapSspPrivacyConsent) = NapSspPrivacyConsent(
        childDirected = other.childDirected ?: childDirected,
        gdprConsent = other.gdprConsent ?: gdprConsent,
        ccpaDoNotSell = other.ccpaDoNotSell ?: ccpaDoNotSell,
        usPrivacy = other.usPrivacy ?: usPrivacy,
    )

    fun describe(): Map<String, Any?> = linkedMapOf(
        "childDirected" to childDirected,
        "gdprConsent" to gdprConsent,
        "ccpaDoNotSell" to ccpaDoNotSell,
        "usPrivacy" to usPrivacy,
    )

    val isEmpty: Boolean
        get() = childDirected == null && gdprConsent == null && ccpaDoNotSell == null && usPrivacy == null
}

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

    val mediations = if (hasKey("mediations") && !isNull("mediations")) getMap("mediations") else null

    return NapSspConfig(
        mediaKey = mediaKey,
        adUnitIds = adUnitIds.distinct(),
        logLevel = optString("logLevel")?.takeIf { it.isNotEmpty() },
        privacy = readPrivacyConsent(),
        testMode = optBoolean("testMode"),
        testDeviceIds = readStringList("testDeviceIds"),
        adapterConfig = mediations?.toAdapterConfig() ?: emptyMap(),
        mediationFlags = mediations?.describeMediationFlags() ?: emptyMap(),
    )
}

/**
 * Maps the JS `mediations` block onto the adapter keys the SDK understands.
 * https://napmx.github.io/#/android/native/privacy (네트워크별 키 주입)
 */
private fun ReadableMap.toAdapterConfig(): Map<String, Map<String, String>> {
    val result = LinkedHashMap<String, Map<String, String>>()

    optStringMap("pangle")?.get("appId")?.takeIf { it.isNotBlank() }?.let {
        result[com.nasmedia.admixerssp.common.AdMixer.ADAPTER_PANGLE] = mapOf("app_id" to it)
    }
    optStringMap("appLovin")?.get("sdkKey")?.takeIf { it.isNotBlank() }?.let {
        result[com.nasmedia.admixerssp.common.AdMixer.ADAPTER_APPLOVIN] = mapOf("sdkKey" to it)
    }

    return result
}

private fun ReadableMap.describeMediationFlags(): Map<String, Any?> = linkedMapOf(
    "adFit" to (optBoolean("adFit") == true),
    "naverAdManager" to (optBoolean("naverAdManager") == true),
    "teads" to (optBoolean("teads") == true),
    "adManagerConfigured" to (optStringMap("adManager") != null),
    "pangleConfigured" to (optStringMap("pangle") != null),
    "appLovinConfigured" to (optStringMap("appLovin") != null),
    "unityAdsConfigured" to (optStringMap("unityAds") != null),
)

internal fun NapSspConfig.requireValid(): NapSspConfig {
    require(mediaKey.isNotBlank()) { "mediaKey is required" }

    val cleaned = adUnitIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    require(cleaned.isNotEmpty()) { "adUnitIds must contain at least one item" }
    val nonNumeric = cleaned.filter { it.toLongOrNull() == null }
    require(nonNumeric.isEmpty()) {
        "adUnit IDs must be numeric strings (received non-numeric: ${nonNumeric.joinToString()}). " +
            "Get numeric IDs from the nap mx partner site."
    }

    return copy(
        mediaKey = mediaKey.trim(),
        adUnitIds = cleaned,
        logLevel = logLevel?.trim()?.takeIf { it.isNotEmpty() },
        testDeviceIds = testDeviceIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
    )
}

internal fun ReadableMap.readPrivacyConsent(): NapSspPrivacyConsent {
    val source = if (hasKey("privacy") && !isNull("privacy")) getMap("privacy") else null

    // `coppa` on the root config is the 0.4.x spelling of `privacy.childDirected`.
    val legacyCoppa = optBoolean("coppa")

    return NapSspPrivacyConsent(
        childDirected = source?.optBoolean("childDirected") ?: legacyCoppa,
        gdprConsent = source?.optBoolean("gdprConsent"),
        ccpaDoNotSell = source?.optBoolean("ccpaDoNotSell"),
        usPrivacy = source?.optString("usPrivacy")?.takeIf { it.isNotEmpty() },
    )
}

internal fun ReadableMap.optBoolean(key: String): Boolean? =
    if (hasKey(key) && !isNull(key) && getType(key) == ReadableType.Boolean) getBoolean(key) else null

internal fun ReadableMap.optString(key: String): String? =
    if (hasKey(key) && !isNull(key) && getType(key) == ReadableType.String) getString(key)?.trim() else null

internal fun ReadableMap.optInt(key: String): Int? =
    if (hasKey(key) && !isNull(key) && getType(key) == ReadableType.Number) getDouble(key).toInt() else null

internal fun ReadableMap.optDouble(key: String): Double? =
    if (hasKey(key) && !isNull(key) && getType(key) == ReadableType.Number) getDouble(key) else null

internal fun ReadableMap.optStringMap(key: String): Map<String, String>? {
    if (!hasKey(key) || isNull(key) || getType(key) != ReadableType.Map) return null
    val source = getMap(key) ?: return null
    val result = LinkedHashMap<String, String>()
    val iterator = source.keySetIterator()
    while (iterator.hasNextKey()) {
        val entryKey = iterator.nextKey()
        when (source.getType(entryKey)) {
            ReadableType.String -> source.getString(entryKey)?.let { result[entryKey] = it }
            ReadableType.Number -> result[entryKey] = source.getDouble(entryKey).let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            }
            ReadableType.Boolean -> result[entryKey] = source.getBoolean(entryKey).toString()
            else -> Unit
        }
    }
    return result.takeIf { it.isNotEmpty() }
}

internal fun ReadableMap.readStringList(key: String): List<String> {
    if (!hasKey(key) || isNull(key)) return emptyList()
    val array: ReadableArray = getArray(key) ?: return emptyList()
    val result = mutableListOf<String>()
    for (index in 0 until array.size()) {
        if (array.getType(index) != ReadableType.String) continue
        val value = array.getString(index)?.trim().orEmpty()
        if (value.isNotEmpty()) {
            result.add(value)
        }
    }
    return result
}

internal fun ReadableArray.toStringList(): List<String> {
    val result = mutableListOf<String>()
    for (index in 0 until size()) {
        if (getType(index) != ReadableType.String) continue
        val value = getString(index)?.trim().orEmpty()
        if (value.isNotEmpty()) {
            result.add(value)
        }
    }
    return result
}
