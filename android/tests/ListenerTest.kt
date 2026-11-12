package com.nasmedia.admixerssp
import com.nasmedia.admixerssp.common.core.AdNetworkType
fun main() {
    var active = true
    val events = mutableListOf<String>()
    val listener = NapAdListener(
        isActive = { active },
        onReceived = { _, _ -> events += "received" },
        onLoadFailed = { _, _ -> events += "load" },
        onShowFailed = { _, _ -> events += "show" },
        onDisplayed = { events += "display" },
        onClicked = { events += "click" },
        onClosed = { events += "close" },
        onCompleted = { events += "complete" },
        onSkipped = { events += "skip" },
    )
    fun callbacks() {
        listener.onReceivedAd(AdNetworkType.UNKNOWN, Any())
        listener.onFailedToReceiveAd(-1, "failure")
        listener.onAdShowFailed(null, AdNetworkType.UNKNOWN, -1, "failure")
        listener.onAdDisplayed()
        listener.onAdClicked()
        listener.onAdClosed()
        listener.onAdCompleted()
        listener.onAdSkipped()
    }
    callbacks()
    check(events == listOf("received", "load", "show", "display", "click", "close", "complete", "skip"))
    active = false
    callbacks()
    check(events.size == 8) { "Stale callbacks were forwarded" }
    val queue = mutableListOf<() -> Unit>()
    active = true
    val queued = NapAdListener(dispatch = { queue += it }, isActive = { active }, onDisplayed = { events += "queued" })
    queued.onAdDisplayed()
    check(events.size == 8)
    active = false
    queue.removeAt(0)()
    check(events.size == 8) { "Callback queued before teardown escaped guard" }
    println("PASS: 8 active callbacks forwarded once; 8 stale callbacks and a callback queued before teardown suppressed")
}
