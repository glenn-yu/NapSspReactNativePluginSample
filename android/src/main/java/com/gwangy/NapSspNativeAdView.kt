package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class NapSspNativeAdView(context: Context) : FrameLayout(context) {
    private val placeholderLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#E8F5E9")) // Light green background
        setPadding(32, 32, 32, 32)
    }

    private val titleTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#2E7D32"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        text = "NapSsp Native Ad"
    }

    private val adUnitIdTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#388E3C"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        setPadding(0, 8, 0, 8)
    }

    private val noteTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#4CAF50"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        gravity = Gravity.CENTER
        text = "Waiting for native SDK integration"
    }

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            adUnitIdTextView.text = field ?: "<unset>"
            maybeAutoLoad()
        }

    init {
        // Build the layout
        placeholderLayout.addView(titleTextView)
        placeholderLayout.addView(adUnitIdTextView)
        placeholderLayout.addView(noteTextView)

        addView(
            placeholderLayout,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        
        isClickable = true
        isFocusable = true
        setOnClickListener {
            val adUnitId = adUnitId
            if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_CLICKED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                )
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_OPENED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
                )
            }
        }
    }

    fun destroyNativeAd() {
        val adUnitId = adUnitId
        if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_CLOSED,
                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_NATIVE_AD),
            )
        }
        currentState = NapSspLoadState.DESTROYED
        removeAllViews()
    }

    private fun maybeAutoLoad() {
        val normalizedAdUnitId = adUnitId
        if (normalizedAdUnitId.isNullOrBlank()) {
            currentState = NapSspLoadState.FAILED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_FAILED,
                mapOf(
                    "adUnitId" to null,
                    "format" to NapSspContracts.FORMAT_NATIVE_AD,
                    "code" to "NAP_SSP_INVALID_AD_UNIT",
                    "message" to "Native adUnitId is required",
                ),
            )
            return
        }

        if (currentState == NapSspLoadState.LOADED) {
            return
        }

        currentState = NapSspLoadState.LOADING

        // Fallback placeholder behavior
        postDelayed({
            currentState = NapSspLoadState.LOADED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_LOADED,
                mapOf(
                    "adUnitId" to normalizedAdUnitId,
                    "format" to NapSspContracts.FORMAT_NATIVE_AD,
                ),
            )
        }, 300)
    }
}
