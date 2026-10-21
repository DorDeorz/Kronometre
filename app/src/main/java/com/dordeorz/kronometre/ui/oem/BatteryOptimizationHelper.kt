package com.dordeorz.kronometre.ui.oem

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.data.OemProfile
import com.dordeorz.kronometre.data.OemStep
import com.dordeorz.kronometre.data.OemTarget

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun open(context: Context, profile: OemProfile, target: OemTarget): Boolean = when (target) {
        OemTarget.Autostart -> profile.autostartComponents.any { tryStart(context, Intent().setComponent(it)) } ||
            openAppDetails(context) || openBatteryList(context)
        OemTarget.BatteryExemption -> requestExemption(context) || openAppDetails(context) || openBatteryList(context)
        OemTarget.AppDetails -> openAppDetails(context) || openBatteryList(context)
        OemTarget.NotificationSettings -> openNotificationSettings(context)
        OemTarget.Manual -> false
    }

    fun openExactAlarmSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return openAppDetails(context)
        return tryStart(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))) ||
            openAppDetails(context)
    }

    fun openNotificationSettings(context: Context): Boolean {
        val opened = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && tryStart(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
        return opened || openAppDetails(context)
    }

    @SuppressLint("BatteryLife")
    private fun requestExemption(context: Context): Boolean =
        tryStart(context, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context)))

    private fun openAppDetails(context: Context): Boolean =
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)))

    private fun openBatteryList(context: Context): Boolean =
        tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

    private fun tryStart(context: Context, intent: Intent): Boolean =
        try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
}

@Composable
fun BatteryOptimizationCard(profile: OemProfile, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var done by rememberSaveable { mutableStateOf(IntArray(0)) }
    var failed by rememberSaveable { mutableStateOf(IntArray(0)) }
    val containerColor = if (profile.aggressive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(if (profile.aggressive) R.string.oem_title else R.string.oem_title_stock),
                style = MaterialTheme.typography.titleMedium,
            )
            if (profile.aggressive) {
                Text(text = stringResource(R.string.oem_intro), style = MaterialTheme.typography.bodyMedium)
            }
            profile.steps.forEachIndexed { index, step ->
                StepRow(
                    step = step,
                    checked = done.contains(index),
                    failed = failed.contains(index),
                    onCheckedChange = { checked ->
                        done = if (checked) done + index else done.filterNot { it == index }.toIntArray()
                    },
                    onOpen = {
                        if (BatteryOptimizationHelper.open(context, profile, step.target)) {
                            failed = failed.filterNot { it == index }.toIntArray()
                        } else {
                            failed = if (failed.contains(index)) failed else failed + index
                        }
                    },
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.oem_dismiss))
            }
        }
    }
}

@Composable
private fun StepRow(
    step: OemStep,
    checked: Boolean,
    failed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(step.text), style = MaterialTheme.typography.bodyMedium)
            if (failed) {
                Text(
                    text = stringResource(R.string.oem_open_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (step.target != OemTarget.Manual) {
            TextButton(onClick = onOpen) { Text(stringResource(R.string.oem_open)) }
        }
    }
}
