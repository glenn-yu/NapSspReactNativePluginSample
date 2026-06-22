package com.nasmedia.admixerssp

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
            if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
                try {
                    val adMixerClass = Class.forName("com.nasmedia.admixerssp.common.AdMixer")
                    val getInstance = adMixerClass.getMethod("getInstance")
                    val adMixer = getInstance.invoke(null)
                    
                    // Try "initialize" first (per docs), then fallback to "init"
                    val initializeMethod = try {
                        adMixerClass.getMethod(
                            "initialize",
                            android.content.Context::class.java,
                            String::class.java,
                            java.util.ArrayList::class.java,
                        )
                    } catch (_: NoSuchMethodException) {
                        adMixerClass.getMethod(
                            "init",
                            android.content.Context::class.java,
                            String::class.java,
                            java.util.ArrayList::class.java,
                        )
                    }
                    
                    initializeMethod.invoke(adMixer, context.applicationContext, config.mediaKey, java.util.ArrayList(config.adUnitIds))
                    android.util.Log.d("NapSspSdkBridge", "AdMixer initialized with ${initializeMethod.name}")

                    // v2.0.0: 미디에이션 어댑터/네트워크 SDK 는 initialize() 시 자동 등록되고 워터폴에서
                    // 지연 초기화됩니다. 네트워크별 키(Pangle app_id, AppLovin sdkKey 등)는 media-conf
                    // 서버 설정으로 전달되므로 앱에서 별도 SDK init 호출이 필요 없습니다.
                    // (v2 auto-registers adapters and lazily inits network SDKs — no manual init needed.)
                } catch (e: Throwable) {
                    android.util.Log.e("NapSspSdkBridge", "Vendor SDK initialization failed: ${e.message}", e)
                }
            }
        } catch (_: Throwable) {
            // reflection guard - silently ignore
        }
    }

    fun setLogLevel(level: String) {
        val normalized = level.trim().ifEmpty { logLevel }
        logLevel = normalized
        if (BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED) {
            try {
                val logClass = Class.forName("com.nasmedia.admixerssp.common.AdMixerLog")
                val logLevelClass = Class.forName("com.nasmedia.admixerssp.common.AdMixerLog\$LogLevel")
                val levelValue = when (normalized.lowercase()) {
                    "verbose", "debug" -> logLevelClass.getField("DEBUG").get(null)
                    "warn" -> logLevelClass.getField("WARN").get(null)
                    "error" -> logLevelClass.getField("ERROR").get(null)
                    "none" -> logLevelClass.getField("NONE").get(null)
                    else -> logLevelClass.getField("INFO").get(null)
                }
                logClass.getMethod("setLogLevel", logLevelClass).invoke(null, levelValue)
            } catch (_: Throwable) {}
        }
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
                "naverAdManagerEnabled" to (config.mediations?.naverAdManagerEnabled == true),
                "teadsEnabled" to (config.mediations?.teadsEnabled == true),
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
