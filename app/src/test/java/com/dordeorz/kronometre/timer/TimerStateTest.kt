package com.dordeorz.kronometre.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    @Test
    fun countsDownFromStart() {
        val state = TimerState().start(60_000L, now = 1_000L)
        assertEquals(TimerStatus.Running, state.status)
        assertEquals(45_000L, state.remaining(now = 16_000L))
        assertEquals(0L, state.remaining(now = 100_000L))
    }

    @Test
    fun pauseAndResumeKeepRemaining() {
        val paused = TimerState().start(60_000L, now = 0L).pause(now = 20_000L)
        assertEquals(40_000L, paused.remaining(now = 999_000L))
        val resumed = paused.resume(now = 100_000L)
        assertEquals(30_000L, resumed.remaining(now = 110_000L))
    }

    @Test
    fun addMinuteExtendsRunningTimer() {
        val state = TimerState().start(60_000L, now = 0L).addMinute(now = 10_000L)
        assertEquals(110_000L, state.remaining(now = 10_000L))
    }

    @Test
    fun cancelKeepsLastDuration() {
        val state = TimerState().start(90_000L, now = 0L).ring().cancel()
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(90_000L, state.durationMs)
    }
}
