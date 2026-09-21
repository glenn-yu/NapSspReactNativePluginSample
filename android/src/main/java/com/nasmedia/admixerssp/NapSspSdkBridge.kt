package com.nasmedia.admixerssp

import android.content.Context
import android.util.Log
import com.nasmedia.admixerssp.ads.AdInfo
import com.nasmedia.admixerssp.common.AdMixer
import com.nasmedia.admixerssp.common.AdMixerLog
import java.util.concurrent.ConcurrentHashMap

/**
 * Single owner of the global nap mx SDK state.
 *
 * Every call below targets the public SDK API directly (no reflection) so that a signature change
 * in a future SDK release fails the build instead of silently degrading at runtime.
 */
internal object NapSspSdkBridge {
    private const val TAG = "NapSspSdkBridge"

    @Volatile
    private var latestConfig: NapSspConfig? = null

    @Volatile
    private var logLevel: String = "info"

    @Volatile
    private var privacy: NapSspPrivacyConsent = NapSspPrivacyConsent()

    @Volatile
    private var testMode: Boolean = false

    @Volatile
    private var testDeviceIds: List<String> = emptyList()

    private val bannerStates = ConcurrentHashMap<String, NapSspLoadState>()
    private val interstitialStates = ConcurrentHashMap<String, NapSspLoadState>()
    private val rewardedStates = ConcurrentHashMap<String, NapSspLoadState>()

    fun initialize(context: Context, rawConfig: NapSspConfig) {
        val config = rawConfig.requireValid()

        // Log level first so the initialize() path itself is traceable.
        config.logLevel?.let { setLogLevel(it) }

        // Privacy and test signals must be applied BEFORE initialize(): adapters read them when the
        // waterfall lazily initialises each network SDK.
        // https://napmx.github.io/#/android/native/privacy
        applyPrivacy(privacy.mergedWith(config.privacy))
        config.testMode?.let { setTestMode(it) }
        if (config.testDeviceIds.isNotEmpty()) {
            setTestDeviceIds(config.testDeviceIds)
        }

        AdMixer.getInstance().initialize(
            context.applicationContext,
            config.mediaKey,
            ArrayList(config.adUnitIds),
        )
        latestConfig = config
        Log.d(TAG, "AdMixer initialized adUnits=${config.adUnitIds.size}")
    }

    fun setLogLevel(level: String) {
        val normalized = level.trim().lowercase().ifEmpty { logLevel }
        logLevel = normalized
        AdMixerLog.setLogLevel(
            when (normalized) {
                "verbose" -> AdMixerLog.LogLevel.VERBOSE
                "debug" -> AdMixerLog.LogLevel.DEBUG
                "warn" -> AdMixerLog.LogLevel.WARN
                "error" -> AdMixerLog.LogLevel.ERROR
                "none" -> AdMixerLog.LogLevel.NONE
                else -> AdMixerLog.LogLevel.INFO
            },
        )
    }

    fun applyPrivacy(consent: NapSspPrivacyConsent) {
        privacy = privacy.mergedWith(consent)

        privacy.childDirected?.let {
            AdMixer.setTagForChildDirectedTreatment(
                if (it) {
                    AdMixer.AX_TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                } else {
                    AdMixer.AX_TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                },
            )
        }
        privacy.gdprConsent?.let { AdMixer.setGdprConsent(it) }
        privacy.ccpaDoNotSell?.let { AdMixer.setCcpaDoNotSell(it) }
        privacy.usPrivacy?.takeIf { it.isNotBlank() }?.let { AdMixer.setUsPrivacy(it) }
    }

    fun setTestMode(enabled: Boolean) {
        testMode = enabled
        AdMixer.setTestMode(enabled)
    }

    fun setTestDeviceIds(ids: List<String>) {
        testDeviceIds = ids
        AdMixer.setTestDeviceIds(ids)
    }

    fun getConfiguration(): NapSspConfig? = latestConfig

    /** Applies the host-supplied network keys. The media-conf server value always wins. */
    fun applyAdapterConfig(builder: AdInfo.Builder): AdInfo.Builder {
        latestConfig?.adapterConfig?.forEach { (adapterName, values) ->
            builder.setAdapterConfig(adapterName, values)
        }
        return builder
    }

    fun markBannerState(adUnitId: String, state: NapSspLoadState) {
        bannerStates[adUnitId] = state
    }

    fun clearBanner(adUnitId: String) {
        bannerStates.remove(adUnitId)
    }

    fun markInterstitialState(adUnitId: String, state: NapSspLoadState) {
        interstitialStates[adUnitId] = state
    }

    fun clearInterstitial(adUnitId: String) {
        interstitialStates.remove(adUnitId)
    }

    fun markRewardedState(adUnitId: String, state: NapSspLoadState) {
        rewardedStates[adUnitId] = state
    }

    fun clearRewarded(adUnitId: String) {
        rewardedStates.remove(adUnitId)
    }

    fun describeStatus(): Map<String, Any?> {
        val config = latestConfig
        val runtime = linkedMapOf<String, Any?>(
            "bannerStates" to bannerStates.mapValues { it.value.name },
            "interstitialStates" to interstitialStates.mapValues { it.value.name },
            "rewardedStates" to rewardedStates.mapValues { it.value.name },
        )

        if (config != null) {
            runtime["mediaKeyHash"] = config.mediaKey.hashCode()
            runtime["mediationFlags"] = config.mediationFlags
            runtime["adapterConfigKeys"] = config.adapterConfig.keys.toList()
        }

        return NapSspContracts.statusSnapshot(
            initialized = config != null,
            logLevel = logLevel,
            privacy = privacy.describe(),
            testMode = testMode,
            testDeviceIdCount = testDeviceIds.size,
            configuredAdUnitIds = config?.adUnitIds ?: emptyList(),
            runtimeState = runtime,
        )
    }

    fun reset() {
        latestConfig = null
        logLevel = "info"
        privacy = NapSspPrivacyConsent()
        testMode = false
        testDeviceIds = emptyList()
        bannerStates.clear()
        interstitialStates.clear()
        rewardedStates.clear()
    }
}
