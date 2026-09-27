package com.oguzh.kronometre.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchStateTest {

    @Test
    fun currentElapsedAddsRunningSegment() {
        val state = StopwatchState(elapsedMs = 1_000L, runningSinceElapsedRealtime = 10_000L)
        assertEquals(3_500L, state.currentElapsedMs(now = 12_500L))
    }

    @Test
    fun currentElapsedWhenPausedIgnoresClock() {
        val state = StopwatchState(elapsedMs = 4_200L)
        assertEquals(4_200L, state.currentElapsedMs(now = 999_999L))
    }

    @Test
    fun startIsIdempotent() {
        val running = StopwatchState().start(now = 100L)
        assertTrue(running.isRunning)
        assertSame(running, running.start(now = 500L))
    }

    @Test
    fun pauseAccumulatesAndStops() {
        val paused = StopwatchState(elapsedMs = 2_000L).start(now = 1_000L).pause(now = 4_000L)
        assertFalse(paused.isRunning)
        assertEquals(5_000L, paused.elapsedMs)
    }

    @Test
    fun resumeContinuesFromAccumulated() {
        val state = StopwatchState().start(0L).pause(3_000L).start(10_000L)
        assertEquals(5_000L, state.currentElapsedMs(now = 12_000L))
    }

    @Test
    fun lapOnlyWhileRunning() {
        val paused = StopwatchState(elapsedMs = 1_000L)
        assertSame(paused, paused.lap(now = 50L))
        val lapped = StopwatchState().start(0L).lap(1_500L).lap(4_000L)
        assertEquals(listOf(1_500L, 4_000L), lapped.laps)
    }

    @Test
    fun resetClearsEverything() {
        val state = StopwatchState(elapsedMs = 9L, laps = listOf(1L, 2L)).reset()
        assertEquals(StopwatchState(), state)
    }
}
