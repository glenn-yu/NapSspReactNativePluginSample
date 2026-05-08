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

                    val registerAdapter = try {
                        adMixerClass.getMethod("registerAdapter", String::class.java)
                    } catch (_: Throwable) { null }
                    val adapters = listOf(
                        "ADAPTER_ADMANAGER",
                        "ADAPTER_ADFIT",
                        "ADAPTER_MOBWITH",
                        "ADAPTER_PANGLE",
                        "ADAPTER_APPLOVIN",
                        "ADAPTER_UNITY"
                    )
                    if (registerAdapter != null) {
                        // Determine if static or instance by checking modifiers
                        val isStatic = java.lang.reflect.Modifier.isStatic(registerAdapter.modifiers)
                        val receiver = if (isStatic) null else adMixer
                        for (adapterConst in adapters) {
                            try {
                                val field = adMixerClass.getField(adapterConst)
                                val adapterName = field.get(null) as? String
                                if (adapterName != null) {
                                    registerAdapter.invoke(receiver, adapterName)
                                    android.util.Log.d("NapSspSdkBridge", "Registered adapter: $adapterName")
                                }
                            } catch (_: Throwable) {
                                // ignore missing adapter constants
                            }
                        }
                    }
                    // Initialize mediation-specific SDKs after AdMixer adapters are registered
                    initializeMediationSdks(context, config)
                } catch (e: Throwable) {
                    android.util.Log.e("NapSspSdkBridge", "Vendor SDK initialization failed: ${e.message}", e)
                }
            }
        } catch (_: Throwable) {
            // reflection guard - silently ignore
        }
    }

    private fun initializeMediationSdks(context: android.content.Context, config: NapSspConfig) {
        // Pangle — requires appId
        val pangleAppId = config.mediations?.pangle?.get("appId") as? String
        if (!pangleAppId.isNullOrBlank()) {
            try {
                val pagConfigBuilderClass = Class.forName("com.bytedance.sdk.openadsdk.api.init.PAGConfig\$Builder")
                val pagConfigBuilder = pagConfigBuilderClass.getConstructor().newInstance()
                pagConfigBuilderClass.getMethod("appId", String::class.java).invoke(pagConfigBuilder, pangleAppId)
                val pagConfig = pagConfigBuilderClass.getMethod("build").invoke(pagConfigBuilder)
                val pagSdkClass = Class.forName("com.bytedance.sdk.openadsdk.api.init.PAGSdk")
                val callbackClass = Class.forName("com.bytedance.sdk.openadsdk.api.init.PAGSdk\$PAGInitCallback")
                val callbackProxy = java.lang.reflect.Proxy.newProxyInstance(
                    callbackClass.classLoader, arrayOf(callbackClass)
                ) { _, _, _ -> null }
                pagSdkClass.getMethod("init", android.content.Context::class.java, pagConfig.javaClass, callbackClass)
                    .invoke(null, context.applicationContext, pagConfig, callbackProxy)
            } catch (_: Throwable) {}
        }

        // AppLovin — requires sdkKey
        val appLovinSdkKey = config.mediations?.appLovin?.get("sdkKey") as? String
        if (!appLovinSdkKey.isNullOrBlank()) {
            try {
                val alSdkClass = Class.forName("com.applovin.sdk.AppLovinSdk")
                val alSdkSettingsClass = Class.forName("com.applovin.sdk.AppLovinSdkSettings")
                val alSdkSettings = alSdkSettingsClass.getConstructor().newInstance()
                val getInstance = alSdkClass.getMethod("getInstance", String::class.java, alSdkSettingsClass, android.content.Context::class.java)
                val alSdk = getInstance.invoke(null, appLovinSdkKey, alSdkSettings, context.applicationContext)
                alSdkClass.getMethod("initializeSdk", android.content.Context::class.java).invoke(alSdk, context.applicationContext)
            } catch (_: Throwable) {}
        }

        // UnityAds — requires appId
        val unityAppId = config.mediations?.unityAds?.get("appId") as? String
        if (!unityAppId.isNullOrBlank()) {
            try {
                val unityAdsClass = Class.forName("com.unity3d.ads.UnityAds")
                val listenerClass = Class.forName("com.unity3d.ads.IUnityAdsInitializationListener")
                val listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.classLoader, arrayOf(listenerClass)
                ) { _, _, _ -> null }
                unityAdsClass.getMethod("initialize", android.content.Context::class.java, String::class.java, Boolean::class.javaPrimitiveType, listenerClass)
                    .invoke(null, context.applicationContext, unityAppId, false, listenerProxy)
            } catch (_: Throwable) {}
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
