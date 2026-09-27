package com.dordeorz.kronometre.ui

import com.dordeorz.kronometre.data.StopwatchState
import com.dordeorz.kronometre.service.StopwatchActions
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchViewModelTest {

    private val state = MutableStateFlow(StopwatchState())
    private val sent = mutableListOf<String>()
    private var now = 0L
    private var dispatchSucceeds = true

    private val viewModel = StopwatchViewModel(state) { action ->
        sent += action
        if (dispatchSucceeds) {
            state.value = when (action) {
                StopwatchActions.ACTION_RESUME -> state.value.start(now)
                StopwatchActions.ACTION_PAUSE -> state.value.pause(now)
                StopwatchActions.ACTION_LAP -> state.value.lap(now)
                StopwatchActions.ACTION_RESET -> state.value.reset()
                else -> state.value
            }
        }
        dispatchSucceeds
    }

    @Test
    fun fullCycle() {
        now = 1_000L
        viewModel.toggle()
        assertTrue(state.value.isRunning)

        now = 2_500L
        viewModel.lap()
        assertEquals(listOf(1_500L), state.value.laps)

        now = 4_000L
        viewModel.toggle()
        assertFalse(state.value.isRunning)
        assertEquals(3_000L, state.value.elapsedMs)

        viewModel.reset()
        assertEquals(StopwatchState(), state.value)
        assertEquals(
            listOf(
                StopwatchActions.ACTION_RESUME,
                StopwatchActions.ACTION_LAP,
                StopwatchActions.ACTION_PAUSE,
                StopwatchActions.ACTION_RESET,
            ),
            sent,
        )
    }

    @Test
    fun lapIgnoredWhenPausedAndResetIgnoredWhenRunning() {
        viewModel.lap()
        assertTrue(sent.isEmpty())
        viewModel.toggle()
        viewModel.reset()
        assertEquals(listOf(StopwatchActions.ACTION_RESUME), sent)
    }

    @Test
    fun resetIgnoredWhenAlreadyZero() {
        viewModel.reset()
        assertTrue(sent.isEmpty())
    }

    @Test
    fun failedStartIsReported() {
        dispatchSucceeds = false
        viewModel.toggle()
        assertTrue(viewModel.startFailed.value)
        viewModel.dismissError()
        assertFalse(viewModel.startFailed.value)
    }
}
