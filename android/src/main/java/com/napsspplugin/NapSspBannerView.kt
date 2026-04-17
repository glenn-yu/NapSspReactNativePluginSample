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

    var adUnitId: String? = null
        set(value) {
            field = value
            updatePlaceholderText()
            if (autoLoad) {
                simulateLoad()
            }
        }

    var size: String = "BANNER_320x50"
        set(value) {
            field = value.ifBlank { "BANNER_320x50" }
            updatePlaceholderText()
        }

    var autoLoad: Boolean = true
        set(value) {
            field = value
            if (value && !adUnitId.isNullOrBlank()) {
                simulateLoad()
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
            NapSspEventEmitter.emitViewEvent(
                this,
                "topAdClicked",
                mapOf("adUnitId" to adUnitId, "format" to "banner"),
            )
        }
        updatePlaceholderText()
    }

    fun reload() {
        simulateLoad()
    }

    fun destroyBanner() {
        removeAllViews()
    }

    private fun simulateLoad() {
        if (adUnitId.isNullOrBlank()) {
            return
        }

        NapSspEventEmitter.emitViewEvent(
            this,
            "topAdLoaded",
            mapOf(
                "adUnitId" to adUnitId,
                "size" to size,
                "format" to "banner",
            ),
        )
    }

    private fun updatePlaceholderText() {
        placeholderTextView.text = buildString {
            append("NapSsp banner placeholder\n")
            append("adUnitId=")
            append(adUnitId ?: "<unset>")
            append("\nsize=")
            append(size)
        }
    }
}
