package com.dordeorz.kronometre.data

import android.os.SystemClock

data class StopwatchState(
    val elapsedMs: Long = 0L,
    val runningSinceElapsedRealtime: Long? = null,
    val laps: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = runningSinceElapsedRealtime != null

    fun currentElapsedMs(now: Long = SystemClock.elapsedRealtime()): Long =
        elapsedMs + (runningSinceElapsedRealtime?.let { (now - it).coerceAtLeast(0L) } ?: 0L)

    fun start(now: Long): StopwatchState =
        if (isRunning) this else copy(runningSinceElapsedRealtime = now)

    fun pause(now: Long): StopwatchState =
        if (!isRunning) this else copy(elapsedMs = currentElapsedMs(now), runningSinceElapsedRealtime = null)

    fun lap(now: Long): StopwatchState =
        if (!isRunning) this else copy(laps = laps + currentElapsedMs(now))

    fun reset(): StopwatchState = StopwatchState()
}
