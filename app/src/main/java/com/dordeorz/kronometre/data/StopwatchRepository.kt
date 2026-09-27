package com.dordeorz.kronometre.data

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.stopwatchDataStore: DataStore<Preferences> by preferencesDataStore(name = "stopwatch")

class StopwatchRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(StopwatchState())
    private var loaded = false

    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    suspend fun load(): StopwatchState = mutex.withLock {
        ensureLoaded()
        _state.value
    }

    suspend fun start(): StopwatchState = update { it.start(clock()) }

    suspend fun pause(): StopwatchState = update { it.pause(clock()) }

    suspend fun lap(): StopwatchState = update { it.lap(clock()) }

    suspend fun reset(): StopwatchState = update { it.reset() }

    private suspend fun update(transform: (StopwatchState) -> StopwatchState): StopwatchState =
        mutex.withLock {
            ensureLoaded()
            val old = _state.value
            val new = transform(old)
            if (new != old) {
                _state.value = new
                write(new)
            }
            new
        }

    private suspend fun ensureLoaded() {
        if (loaded) return
        val prefs = dataStore.data.first()
        val stored = StopwatchState(
            elapsedMs = prefs[ELAPSED_MS] ?: 0L,
            runningSinceElapsedRealtime = prefs[RUNNING_SINCE],
            laps = decodeLaps(prefs[LAPS]),
        )
        val restored = stored.copy(runningSinceElapsedRealtime = null)
        if (restored != stored) write(restored)
        _state.value = restored
        loaded = true
    }

    private suspend fun write(state: StopwatchState) {
        dataStore.edit { prefs ->
            prefs[ELAPSED_MS] = state.elapsedMs
            val since = state.runningSinceElapsedRealtime
            if (since == null) prefs.remove(RUNNING_SINCE) else prefs[RUNNING_SINCE] = since
            if (state.laps.isEmpty()) prefs.remove(LAPS) else prefs[LAPS] = encodeLaps(state.laps)
        }
    }

    companion object {
        private val ELAPSED_MS = longPreferencesKey("elapsed_ms")
        private val RUNNING_SINCE = longPreferencesKey("running_since")
        private val LAPS = stringPreferencesKey("laps")

        @Volatile
        private var instance: StopwatchRepository? = null

        fun get(context: Context): StopwatchRepository =
            instance ?: synchronized(this) {
                instance ?: StopwatchRepository(context.applicationContext.stopwatchDataStore)
                    .also { instance = it }
            }

        internal fun encodeLaps(laps: List<Long>): String = laps.joinToString(",")

        internal fun decodeLaps(raw: String?): List<Long> =
            raw.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }
    }
}
