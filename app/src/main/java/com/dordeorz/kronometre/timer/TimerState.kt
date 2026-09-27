package com.dordeorz.kronometre.timer

import android.os.SystemClock

enum class TimerStatus { Idle, Running, Paused, Ringing }

data class TimerState(
    val status: TimerStatus = TimerStatus.Idle,
    val durationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val endsAtElapsedRealtime: Long = 0L,
) {
    fun remaining(now: Long = SystemClock.elapsedRealtime()): Long = when (status) {
        TimerStatus.Running -> (endsAtElapsedRealtime - now).coerceAtLeast(0L)
        TimerStatus.Paused -> remainingMs
        else -> 0L
    }

    fun start(durationMs: Long, now: Long): TimerState =
        if (durationMs <= 0L) this else TimerState(TimerStatus.Running, durationMs, durationMs, now + durationMs)

    fun pause(now: Long): TimerState =
        if (status != TimerStatus.Running) this else copy(status = TimerStatus.Paused, remainingMs = remaining(now))

    fun resume(now: Long): TimerState =
        if (status != TimerStatus.Paused) this else copy(status = TimerStatus.Running, endsAtElapsedRealtime = now + remainingMs)

    fun addMinute(now: Long): TimerState = when (status) {
        TimerStatus.Running -> copy(durationMs = durationMs + MINUTE, endsAtElapsedRealtime = endsAtElapsedRealtime + MINUTE)
        TimerStatus.Paused -> copy(durationMs = durationMs + MINUTE, remainingMs = remainingMs + MINUTE)
        TimerStatus.Ringing -> TimerState(TimerStatus.Running, MINUTE, MINUTE, now + MINUTE)
        TimerStatus.Idle -> this
    }

    fun ring(): TimerState = if (status == TimerStatus.Running) copy(status = TimerStatus.Ringing, remainingMs = 0L) else this

    fun cancel(): TimerState = TimerState(durationMs = durationMs)

    companion object {
        const val MINUTE = 60_000L
    }
}
