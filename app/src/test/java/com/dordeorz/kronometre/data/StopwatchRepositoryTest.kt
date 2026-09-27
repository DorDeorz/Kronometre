package com.dordeorz.kronometre.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class StopwatchRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 0L

    private fun TestScope.dataStore(name: String = "test") =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("$name.preferences_pb") }

    @Test
    fun commandsUpdateStateAndPersist() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        val repository = StopwatchRepository(store) { now }

        now = 1_000L
        repository.start()
        now = 3_000L
        repository.lap()
        now = 6_000L
        val paused = repository.pause()

        assertFalse(paused.isRunning)
        assertEquals(5_000L, paused.elapsedMs)
        assertEquals(listOf(2_000L), paused.laps)

        val reloaded = StopwatchRepository(store) { now }.load()
        assertEquals(paused, reloaded)
    }

    @Test
    fun restoreAfterProcessDeathIsPaused() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        store.edit {
            it[longPreferencesKey("elapsed_ms")] = 7_000L
            it[longPreferencesKey("running_since")] = 500L
        }
        now = 90_000L
        val restored = StopwatchRepository(store) { now }.load()
        assertFalse(restored.isRunning)
        assertEquals(7_000L, restored.elapsedMs)
    }

    @Test
    fun resetClearsPersistedState() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        val repository = StopwatchRepository(store) { now }
        repository.start()
        now = 2_000L
        repository.lap()
        repository.pause()
        repository.reset()
        val reloaded = StopwatchRepository(store) { now }.load()
        assertEquals(StopwatchState(), reloaded)
    }

    @Test
    fun startWritesRunningSince() = runTest(UnconfinedTestDispatcher()) {
        val store = dataStore()
        now = 42L
        val state = StopwatchRepository(store) { now }.start()
        assertTrue(state.isRunning)
        assertEquals(42L, state.runningSinceElapsedRealtime)
    }

    @Test
    fun lapsRoundTripThroughCsv() {
        val laps = listOf(1L, 22L, 333L)
        assertEquals(laps, StopwatchRepository.decodeLaps(StopwatchRepository.encodeLaps(laps)))
        assertEquals(emptyList<Long>(), StopwatchRepository.decodeLaps(null))
        assertEquals(emptyList<Long>(), StopwatchRepository.decodeLaps(""))
    }
}
