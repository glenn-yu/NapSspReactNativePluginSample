package com.gwangy

import java.util.concurrent.ConcurrentHashMap

internal object NapSspSdkBridge {
    @Volatile
    private var latestConfig: NapSspConfig? = null

    @Volatile
    private var logLevel: String = "info"

    @Volatile
    private var coppaEnabled: Boolean = false

    private val bannerStates = ConcurrentHashMap<String, NapSspLoadState>()
    private val interstitialStates = ConcurrentHashMap<String, NapSspLoadState>()
    private val rewardedStates = ConcurrentHashMap<String, NapSspLoadState>()

    fun initialize(context: android.content.Context, config: NapSspConfig) {
        latestConfig = config.requireValid()
        logLevel = config.logLevel ?: logLevel
        coppaEnabled = config.coppa

        // If the vendor SDK is enabled at build time, try to initialize it.
        // Use reflection so this module can compile without the vendor SDK present (compileOnly).
        try {
            val vendorEnabled = try {
                Class.forName("com.napsspplugin.BuildConfig").getField("NAP_SSP_VENDOR_SDK_ENABLED").getBoolean(null)
            } catch (_: Throwable) {
                false
            }

            if (vendorEnabled) {
                try {
                    val adMixerClass = Class.forName("com.nasmedia.admixerssp.common.AdMixer")
                    val getInstance = adMixerClass.getMethod("getInstance")
                    val adMixer = getInstance.invoke(null)
                    val initialize = adMixerClass.getMethod("initialize", android.content.Context::class.java, String::class.java, List::class.java)
                    initialize.invoke(adMixer, context.applicationContext, config.mediaKey, config.adUnitIds)
                    
                    val registerAdapter = adMixerClass.getMethod("registerAdapter", String::class.java)
                    val adapters = listOf(
                        "ADAPTER_ADMANAGER",
                        "ADAPTER_ADFIT",
                        "ADAPTER_MOBWITH",
                        "ADAPTER_PANGLE",
                        "ADAPTER_APPLOVIN",
                        "ADAPTER_UNITY"
                    )
                    for (adapterConst in adapters) {
                        try {
                            val field = adMixerClass.getField(adapterConst)
                            val adapterName = field.get(null) as? String
                            if (adapterName != null) {
                                registerAdapter.invoke(adMixer, adapterName)
                            }
                        } catch (_: Throwable) {
                            // ignore missing adapter constants
                        }
                    }
                } catch (e: Throwable) {
                    // vendor SDK present but reflection failed; leave placeholder behavior
                }
            }
        } catch (_: Throwable) {
            // reflection guard - silently ignore
        }
    }

    fun setLogLevel(level: String) {
        logLevel = level.trim().ifEmpty { logLevel }
    }

    fun setCoppa(enabled: Boolean) {
        coppaEnabled = enabled
    }

    fun getConfiguration(): NapSspConfig? = latestConfig

    fun markBannerState(adUnitId: String, state: NapSspLoadState) {
        bannerStates[adUnitId] = state
    }

    fun getBannerState(adUnitId: String): NapSspLoadState =
        bannerStates[adUnitId] ?: NapSspLoadState.IDLE

    fun clearBanner(adUnitId: String) {
        bannerStates.remove(adUnitId)
    }

    fun markInterstitialState(adUnitId: String, state: NapSspLoadState) {
        interstitialStates[adUnitId] = state
    }

    fun getInterstitialState(adUnitId: String): NapSspLoadState =
        interstitialStates[adUnitId] ?: NapSspLoadState.IDLE

    fun clearInterstitial(adUnitId: String) {
        interstitialStates.remove(adUnitId)
    }

    fun markRewardedState(adUnitId: String, state: NapSspLoadState) {
        rewardedStates[adUnitId] = state
    }

    fun getRewardedState(adUnitId: String): NapSspLoadState =
        rewardedStates[adUnitId] ?: NapSspLoadState.IDLE

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
            runtime["mediationConfigured"] = config.mediations != null
            runtime["mediationFlags"] = mapOf(
                "adFitEnabled" to (config.mediations?.adFitEnabled == true),
                "mobwithEnabled" to (config.mediations?.mobwithEnabled == true),
                "pangleConfigured" to (config.mediations?.pangle != null),
                "appLovinConfigured" to (config.mediations?.appLovin != null),
                "unityConfigured" to (config.mediations?.unityAds != null),
                "adManagerConfigured" to (config.mediations?.adManager != null),
            )
        }

        return NapSspContracts.statusSnapshot(
            initialized = config != null,
            logLevel = logLevel,
            coppaEnabled = coppaEnabled,
            configuredAdUnitIds = config?.adUnitIds ?: emptyList(),
            runtimeState = runtime,
        )
    }

    fun reset() {
        latestConfig = null
        logLevel = "info"
        coppaEnabled = false
        bannerStates.clear()
        interstitialStates.clear()
        rewardedStates.clear()
    }
}
