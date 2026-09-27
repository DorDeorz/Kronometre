package com.dordeorz.kronometre.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.timer.TimerController

@Composable
fun MainScreen(
    stopwatchViewModel: StopwatchViewModel,
    timerViewModel: TimerViewModel,
    showCentis: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onRunningChanged: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val timerState by timerViewModel.state.collectAsStateWithLifecycle()
    var exactAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        exactAllowed = TimerController.canScheduleExact(context)
        onPauseOrDispose { }
    }
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.weight(1f)) {
                    Tab(selected = selectedTab == 0, onClick = { onSelectTab(0) }, text = { Text(stringResource(R.string.tab_stopwatch)) })
                    Tab(selected = selectedTab == 1, onClick = { onSelectTab(1) }, text = { Text(stringResource(R.string.tab_timer)) })
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    StopwatchScreen(viewModel = stopwatchViewModel, showCentis = showCentis, onRunningChanged = onRunningChanged)
                } else {
                    TimerScreen(
                        state = timerState,
                        exactAlarmsAllowed = exactAllowed,
                        onStart = timerViewModel::start,
                        onToggle = timerViewModel::toggle,
                        onAddMinute = timerViewModel::addMinute,
                        onCancel = timerViewModel::cancel,
                    )
                }
            }
        }
    }
}
