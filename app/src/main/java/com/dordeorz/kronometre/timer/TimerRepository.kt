package com.dordeorz.kronometre.timer

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

private val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer")

class TimerRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(TimerState())
    private var loaded = false

    val state: StateFlow<TimerState> = _state.asStateFlow()

    suspend fun load(): TimerState = mutex.withLock {
        ensureLoaded()
        _state.value
    }

    suspend fun update(transform: (TimerState, Long) -> TimerState): Pair<TimerState, TimerState> =
        mutex.withLock {
            ensureLoaded()
            val old = _state.value
            val new = transform(old, clock())
            if (new != old) {
                _state.value = new
                write(new)
            }
            old to new
        }

    private suspend fun ensureLoaded() {
        if (loaded) return
        val prefs = dataStore.data.first()
        val stored = TimerState(
            status = TimerStatus.entries.firstOrNull { it.name == prefs[STATUS] } ?: TimerStatus.Idle,
            durationMs = prefs[DURATION] ?: 0L,
            remainingMs = prefs[REMAINING] ?: 0L,
            endsAtElapsedRealtime = prefs[ENDS_AT] ?: 0L,
        )
        val rebooted = stored.status == TimerStatus.Running && stored.endsAtElapsedRealtime - clock() > stored.durationMs
        val restored = if (rebooted) stored.cancel() else stored
        if (restored != stored) write(restored)
        _state.value = restored
        loaded = true
    }

    private suspend fun write(state: TimerState) {
        dataStore.edit { prefs ->
            prefs[STATUS] = state.status.name
            prefs[DURATION] = state.durationMs
            prefs[REMAINING] = state.remainingMs
            prefs[ENDS_AT] = state.endsAtElapsedRealtime
        }
    }

    companion object {
        private val STATUS = stringPreferencesKey("status")
        private val DURATION = longPreferencesKey("duration_ms")
        private val REMAINING = longPreferencesKey("remaining_ms")
        private val ENDS_AT = longPreferencesKey("ends_at")

        @Volatile
        private var instance: TimerRepository? = null

        fun get(context: Context): TimerRepository =
            instance ?: synchronized(this) {
                instance ?: TimerRepository(context.applicationContext.timerDataStore).also { instance = it }
            }
    }
}
