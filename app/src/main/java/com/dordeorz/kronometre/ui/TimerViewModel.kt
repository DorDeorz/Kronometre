package com.dordeorz.kronometre.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dordeorz.kronometre.timer.TimerController
import com.dordeorz.kronometre.timer.TimerRepository
import com.dordeorz.kronometre.timer.TimerState
import com.dordeorz.kronometre.timer.TimerStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    val state: StateFlow<TimerState> = TimerRepository.get(application).state

    init {
        viewModelScope.launch { TimerController.refresh(application) }
    }

    fun start(durationMs: Long) = launch { TimerController.start(it, durationMs) }

    fun toggle() = launch {
        val action = if (state.value.status == TimerStatus.Running) TimerController.ACTION_PAUSE else TimerController.ACTION_RESUME
        TimerController.handle(it, action)
    }

    fun addMinute() = launch { TimerController.handle(it, TimerController.ACTION_ADD_MINUTE) }

    fun cancel() = launch { TimerController.handle(it, TimerController.ACTION_CANCEL) }

    private fun launch(block: suspend (Application) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch { block(app) }
    }
}
