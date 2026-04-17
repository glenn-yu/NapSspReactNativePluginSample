package com.napsspplugin

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

internal class NapSspBannerView(context: Context) : FrameLayout(context) {
    private val placeholderTextView: TextView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        gravity = Gravity.CENTER
        text = "NapSsp banner placeholder"
        setPadding(24, 24, 24, 24)
    }

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            updatePlaceholderText()
            maybeAutoLoad()
        }

    var size: String = "BANNER_320x50"
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() } ?: "BANNER_320x50"
            updatePlaceholderText()
            maybeAutoLoad()
        }

    var autoLoad: Boolean = true
        set(value) {
            field = value
            if (value) {
                maybeAutoLoad()
            }
        }

    init {
        setBackgroundColor(Color.parseColor("#1A1A1A"))
        addView(
            placeholderTextView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            },
        )
        isClickable = true
        isFocusable = true
        setOnClickListener {
            val adUnitId = adUnitId
            if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
                NapSspSdkBridge.markBannerState(adUnitId, NapSspLoadState.SHOWN)
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                )
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_OPENED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
                )
            }
        }
        updatePlaceholderText()
    }

    fun reload() {
        maybeAutoLoad(force = true)
    }

    fun destroyBanner() {
        val adUnitId = adUnitId
        if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_CLOSED,
                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_BANNER),
            )
        }
        currentState = NapSspLoadState.DESTROYED
        if (!adUnitId.isNullOrBlank()) {
            NapSspSdkBridge.clearBanner(adUnitId)
        }
        removeAllViews()
    }

    private fun maybeAutoLoad(force: Boolean = false) {
        if (!autoLoad && !force) {
            return
        }

        val normalizedAdUnitId = adUnitId
        if (normalizedAdUnitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to null,
                    "format" to NapSspContracts.FORMAT_BANNER,
                    "code" to "NAP_SSP_INVALID_AD_UNIT",
                    "message" to "Banner adUnitId is required",
                ),
            )
            return
        }

        if (!isSupportedSize(size)) {
            currentState = NapSspLoadState.FAILED
            NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.FAILED)
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to normalizedAdUnitId,
                    "format" to NapSspContracts.FORMAT_BANNER,
                    "code" to "NAP_SSP_INVALID_BANNER_SIZE",
                    "message" to "Unsupported banner size: $size",
                ),
            )
            return
        }

        if (!force && currentState == NapSspLoadState.LOADED) {
            return
        }

        currentState = NapSspLoadState.LOADING
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADING)
        currentState = NapSspLoadState.LOADED
        NapSspSdkBridge.markBannerState(normalizedAdUnitId, NapSspLoadState.LOADED)
        NapSspEventEmitter.emitViewEvent(
            this,
            NapSspContracts.VIEW_EVENT_AD_LOADED,
            mapOf(
                "adUnitId" to normalizedAdUnitId,
                "size" to size,
                "format" to NapSspContracts.FORMAT_BANNER,
            ),
        )
    }

    private fun isSupportedSize(value: String): Boolean {
        return when (value) {
            "BANNER_320x50",
            "BANNER_320x100",
            "BANNER_300x250",
            "LARGE_BANNER",
            "MEDIUM_RECTANGLE",
            "SMART_BANNER" -> true
            else -> false
        }
    }

    private fun updatePlaceholderText() {
        placeholderTextView.text = buildString {
            append("NapSsp banner placeholder\n")
            append("state=")
            append(currentState.name)
            append("\nadUnitId=")
            append(adUnitId ?: "<unset>")
            append("\nsize=")
            append(size)
            append("\nautoLoad=")
            append(autoLoad)
        }
    }
}
