package com.dordeorz.kronometre.timer

import com.dordeorz.kronometre.data.FakePreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerRepositoryTest {

    private var now = 0L
    private var boot = 1

    private fun dataStore() = FakePreferencesDataStore()

    private fun repository(store: FakePreferencesDataStore) =
        TimerRepository(store, bootCount = { boot }, clock = { now })

    @Test
    fun runningTimerSurvivesProcessDeathOnSameBoot() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        now = 1_000L
        repository(store).start(60_000L)

        now = 11_000L
        val restored = repository(store).load()
        assertEquals(TimerStatus.Running, restored.status)
        assertEquals(50_000L, restored.remaining(now))
    }

    @Test
    fun runningTimerIsCancelledAfterReboot() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        now = 1_000L
        repository(store).start(60_000L)

        boot = 2
        now = 500L
        val restored = repository(store).load()
        assertEquals(TimerStatus.Idle, restored.status)
    }

    @Test
    fun onBootCancelsRunningButKeepsPausedTimer() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        now = 1_000L
        repository(store).start(60_000L)
        now = 21_000L
        val cancelled = repository(store).onBoot()
        assertEquals(TimerStatus.Idle, cancelled.status)

        now = 1_000L
        repository(store).start(60_000L)
        now = 21_000L
        repository(store).update { state, time -> state.pause(time) }
        val paused = repository(store).onBoot()
        assertEquals(TimerStatus.Paused, paused.status)
        assertEquals(40_000L, paused.remainingMs)
    }

    @Test
    fun alarmTimeoutOnlyStopsRingingTimer() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        now = 0L
        repository(store).start(1_000L)
        repository(store).update { state, time -> state.ring() }
        val stopped = repository(store).update { state, _ ->
            if (state.status == TimerStatus.Ringing) state.cancel() else state
        }.second
        assertEquals(TimerStatus.Idle, stopped.status)
    }
}
