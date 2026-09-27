package com.oguzh.kronometre.ui

import android.graphics.Typeface
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.Chronometer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.ui.theme.KronometreTheme
import com.oguzh.kronometre.ui.theme.actionColors

@Composable
fun TimeDisplay(state: StopwatchState, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground.toArgb()
    AndroidView(
        factory = { context ->
            Chronometer(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, TIME_TEXT_SP)
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
            }
        },
        update = { chronometer ->
            chronometer.setTextColor(color)
            chronometer.base = SystemClock.elapsedRealtime() - state.currentElapsedMs()
            if (state.isRunning) chronometer.start() else chronometer.stop()
        },
        modifier = modifier,
    )
}

@Composable
fun StatusText(state: StopwatchState, modifier: Modifier = Modifier) {
    val text = when {
        state.isRunning -> R.string.status_running
        state.elapsedMs > 0L -> R.string.status_paused
        else -> R.string.status_reset
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun ControlRow(
    state: StopwatchState,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = actionColors()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(containerColor = if (state.isRunning) colors.pause else colors.start),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(
                painterResource(if (state.isRunning) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    when {
                        state.isRunning -> R.string.action_pause
                        state.elapsedMs > 0L -> R.string.action_resume
                        else -> R.string.action_start
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onLap, enabled = state.isRunning, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_lap), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_lap))
            }
            FilledTonalButton(
                onClick = onReset,
                enabled = !state.isRunning && state.elapsedMs > 0L,
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_reset), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_reset))
            }
        }
    }
}

@Composable
fun LapList(laps: List<Long>, modifier: Modifier = Modifier) {
    val rows = laps.mapIndexed { index, total -> Triple(index + 1, total - (laps.getOrNull(index - 1) ?: 0L), total) }.asReversed()
    LazyColumn(modifier = modifier) {
        items(rows, key = { it.first }) { (number, split, total) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lap_label, number),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatLap(split),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(text = formatLap(total), style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun MessageBanner(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

fun formatLap(ms: Long): String =
    DateUtils.formatElapsedTime(ms / 1000) + "," + ((ms % 1000) / 10).toString().padStart(2, '0')

private const val TIME_TEXT_SP = 64f

@Preview(showBackground = true)
@Composable
private fun ControlRowPreview() {
    KronometreTheme {
        ControlRow(state = StopwatchState(elapsedMs = 65_000L), onToggle = {}, onLap = {}, onReset = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LapListPreview() {
    KronometreTheme {
        LapList(laps = listOf(12_340L, 25_010L, 40_990L))
    }
}
