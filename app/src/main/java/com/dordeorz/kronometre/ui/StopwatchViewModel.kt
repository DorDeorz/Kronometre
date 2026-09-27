package com.dordeorz.kronometre.ui

import androidx.lifecycle.ViewModel
import com.dordeorz.kronometre.data.StopwatchState
import com.dordeorz.kronometre.service.StopwatchActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StopwatchViewModel(
    val state: StateFlow<StopwatchState>,
    private val dispatch: (String) -> Boolean,
) : ViewModel() {

    private val _startFailed = MutableStateFlow(false)
    val startFailed: StateFlow<Boolean> = _startFailed.asStateFlow()

    fun toggle() {
        if (state.value.isRunning) send(StopwatchActions.ACTION_PAUSE) else send(StopwatchActions.ACTION_RESUME)
    }

    fun lap() {
        if (state.value.isRunning) send(StopwatchActions.ACTION_LAP)
    }

    fun reset() {
        val current = state.value
        if (!current.isRunning && (current.elapsedMs > 0L || current.laps.isNotEmpty())) send(StopwatchActions.ACTION_RESET)
    }

    fun dismissError() {
        _startFailed.value = false
    }

    private fun send(action: String) {
        _startFailed.value = !dispatch(action)
    }
}
