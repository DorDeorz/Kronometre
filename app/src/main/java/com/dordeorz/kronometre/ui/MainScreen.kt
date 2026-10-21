package com.dordeorz.kronometre.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.timer.TimerController
import com.dordeorz.kronometre.ui.oem.BatteryOptimizationHelper

@Composable
fun MainScreen(
    stopwatchViewModel: StopwatchViewModel,
    timerViewModel: TimerViewModel,
    showCentis: Boolean,
    oemHelpDismissed: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onRunningChanged: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissOemHelp: () -> Unit,
) {
    val context = LocalContext.current
    val timerState by timerViewModel.state.collectAsStateWithLifecycle()
    var exactAllowed by remember { mutableStateOf(true) }
    var timerPermissionAsked by rememberSaveable { mutableStateOf(false) }
    val timerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LifecycleResumeEffect(Unit) {
        exactAllowed = TimerController.canScheduleExact(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !timerPermissionAsked &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            timerPermissionAsked = true
            timerPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val tabs = listOf(
        R.string.tab_stopwatch to R.drawable.ic_stopwatch,
        R.string.tab_timer to R.drawable.ic_timer,
    )
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(tabs[selectedTab].first),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(stringResource(label), maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedTab == 0) {
                StopwatchScreen(
                    viewModel = stopwatchViewModel,
                    showCentis = showCentis,
                    oemHelpDismissed = oemHelpDismissed,
                    onRunningChanged = onRunningChanged,
                    onDismissOemHelp = onDismissOemHelp,
                )
            } else {
                TimerScreen(
                    state = timerState,
                    exactAlarmsAllowed = exactAllowed,
                    onStart = timerViewModel::start,
                    onToggle = timerViewModel::toggle,
                    onAddMinute = timerViewModel::addMinute,
                    onCancel = timerViewModel::cancel,
                    onOpenExactAlarmSettings = { BatteryOptimizationHelper.openExactAlarmSettings(context) },
                )
            }
        }
    }
}
