package com.oguzh.kronometre.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.OemProfile
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.ui.oem.BatteryOptimizationCard
import com.oguzh.kronometre.ui.oem.BatteryOptimizationHelper
import com.oguzh.kronometre.ui.theme.KronometreTheme

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel, onRunningChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val startFailed by viewModel.startFailed.collectAsStateWithLifecycle()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var batteryExempt by remember { mutableStateOf(true) }
    var oemDismissed by rememberSaveable { mutableStateOf(false) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    val profile = remember { OemProfile.current() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        batteryExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionAsked &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            permissionAsked = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.isRunning) { onRunningChanged(state.isRunning) }

    StopwatchContent(
        state = state,
        startFailed = startFailed,
        notificationsEnabled = notificationsEnabled,
        onToggle = viewModel::toggle,
        onLap = viewModel::lap,
        onReset = viewModel::reset,
        onDismissError = viewModel::dismissError,
        onOpenNotificationSettings = { BatteryOptimizationHelper.openNotificationSettings(context) },
        oemCard = {
            if (!batteryExempt && !oemDismissed) {
                BatteryOptimizationCard(profile = profile, onDismiss = { oemDismissed = true })
            }
        },
    )
}

@Composable
fun StopwatchContent(
    state: StopwatchState,
    startFailed: Boolean,
    notificationsEnabled: Boolean,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    onDismissError: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    oemCard: @Composable () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!notificationsEnabled) {
                MessageBanner(
                    message = stringResource(R.string.notifications_disabled),
                    actionLabel = stringResource(R.string.notifications_allow),
                    onAction = onOpenNotificationSettings,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (startFailed) {
                MessageBanner(
                    message = stringResource(R.string.start_failed),
                    actionLabel = stringResource(R.string.ok),
                    onAction = onDismissError,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            oemCard()
            TimeDisplay(state = state, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
            StatusText(state = state)
            ControlRow(
                state = state,
                onToggle = onToggle,
                onLap = onLap,
                onReset = onReset,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            LapList(laps = state.laps, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StopwatchContentPreview() {
    KronometreTheme {
        StopwatchContent(
            state = StopwatchState(elapsedMs = 83_450L, laps = listOf(30_120L, 61_900L)),
            startFailed = false,
            notificationsEnabled = false,
            onToggle = {},
            onLap = {},
            onReset = {},
            onDismissError = {},
            onOpenNotificationSettings = {},
            oemCard = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StopwatchContentDarkPreview() {
    KronometreTheme(darkTheme = true, dynamicColor = false) {
        StopwatchContent(
            state = StopwatchState(),
            startFailed = true,
            notificationsEnabled = true,
            onToggle = {},
            onLap = {},
            onReset = {},
            onDismissError = {},
            onOpenNotificationSettings = {},
            oemCard = {},
        )
    }
}
