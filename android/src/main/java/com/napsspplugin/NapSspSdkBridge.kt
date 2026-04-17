package com.napsspplugin

import java.util.concurrent.ConcurrentHashMap

internal object NapSspSdkBridge {
    @Volatile
    private var latestConfig: NapSspConfig? = null

    @Volatile
    private var logLevel: String = "info"

    @Volatile
    private var coppaEnabled: Boolean = false

    private val interstitialStates = ConcurrentHashMap<String, NapSspLoadState>()
    private val rewardedStates = ConcurrentHashMap<String, NapSspLoadState>()

    fun initialize(config: NapSspConfig) {
        latestConfig = config
        logLevel = config.logLevel ?: logLevel
        coppaEnabled = config.coppa
    }

    fun setLogLevel(level: String) {
        logLevel = level
    }

    fun setCoppa(enabled: Boolean) {
        coppaEnabled = enabled
    }

    fun getConfiguration(): NapSspConfig? = latestConfig

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

    fun describeStatus(): Map<String, Any?> = mapOf(
        "initialized" to (latestConfig != null),
        "coppa" to coppaEnabled,
        "logLevel" to logLevel,
        "configuredAdUnitIds" to (latestConfig?.adUnitIds ?: emptyList<String>()),
    )

    fun reset() {
        latestConfig = null
        logLevel = "info"
        coppaEnabled = false
        interstitialStates.clear()
        rewardedStates.clear()
    }
}
