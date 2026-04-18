package com.gwangy

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class NapSspVideoAdView(context: Context) : FrameLayout(context) {
    private val placeholderLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red background
        setPadding(32, 32, 32, 32)
    }

    private val titleTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#C62828"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        text = "NapSsp Video Ad"
    }

    private val adUnitIdTextView: TextView = TextView(context).apply {
        setTextColor(Color.parseColor("#D32F2F"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        setPadding(0, 8, 0, 8)
    }

    private var currentState: NapSspLoadState = NapSspLoadState.IDLE

    var adUnitId: String? = null
        set(value) {
            field = value?.trim()?.takeIf { it.isNotEmpty() }
            adUnitIdTextView.text = field ?: "<unset>"
            maybeAutoLoad()
        }

    var isRetry: Boolean = false

    init {
        placeholderLayout.addView(titleTextView)
        placeholderLayout.addView(adUnitIdTextView)

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
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
                NapSspEventEmitter.emitViewEvent(
                    this,
                    NapSspContracts.VIEW_EVENT_AD_OPENED,
                    mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
                )
            }
        }
    }

    fun destroyVideoAd() {
        val adUnitId = adUnitId
        if (!adUnitId.isNullOrBlank() && currentState == NapSspLoadState.LOADED) {
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_CLOSED,
                mapOf("adUnitId" to adUnitId, "format" to NapSspContracts.FORMAT_VIDEO),
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
                    "format" to NapSspContracts.FORMAT_VIDEO,
                    "code" to "NAP_SSP_INVALID_AD_UNIT",
                    "message" to "Video adUnitId is required",
                ),
            )
            return
        }

        if (currentState == NapSspLoadState.LOADED) {
            return
        }

        currentState = NapSspLoadState.LOADING

        postDelayed({
            currentState = NapSspLoadState.LOADED
            NapSspEventEmitter.emitViewEvent(
                this,
                NapSspContracts.VIEW_EVENT_AD_LOADED,
                mapOf(
                    "adUnitId" to normalizedAdUnitId,
                    "format" to NapSspContracts.FORMAT_VIDEO,
                ),
            )
        }, 300)
    }
}
