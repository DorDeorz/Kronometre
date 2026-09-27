package com.dordeorz.kronometre.ui

import android.graphics.Typeface
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.Chronometer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.data.StopwatchState
import com.dordeorz.kronometre.ui.theme.KronometreTheme
import com.dordeorz.kronometre.ui.theme.actionColors

@Composable
fun TimeDisplay(state: StopwatchState, modifier: Modifier = Modifier, textSp: Float = TIME_TEXT_SP) {
    val color = MaterialTheme.colorScheme.onBackground.toArgb()
    AndroidView(
        factory = { context ->
            Chronometer(context).apply {
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
            }
        },
        update = { chronometer ->
            chronometer.setTextColor(color)
            chronometer.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
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
    val canReset = !state.isRunning && state.elapsedMs > 0L
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundAction(
            icon = if (canReset) R.drawable.ic_reset else R.drawable.ic_lap,
            label = stringResource(if (canReset) R.string.action_reset else R.string.action_lap),
            onClick = if (canReset) onReset else onLap,
            enabled = state.isRunning || canReset,
            size = 64.dp,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        RoundAction(
            icon = if (state.isRunning) R.drawable.ic_pause else R.drawable.ic_play,
            label = stringResource(
                when {
                    state.isRunning -> R.string.action_pause
                    state.elapsedMs > 0L -> R.string.action_resume
                    else -> R.string.action_start
                },
            ),
            onClick = onToggle,
            enabled = true,
            size = 88.dp,
            containerColor = if (state.isRunning) colors.pause else colors.start,
            contentColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun RoundAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            modifier = Modifier.size(size),
        ) {
            Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(size / 2.4f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LapList(laps: List<Long>, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    val rows = laps.mapIndexed { index, total -> Triple(index + 1, total - (laps.getOrNull(index - 1) ?: 0L), total) }.asReversed()
    LazyColumn(modifier = modifier) {
        if (header != null) item(key = "header") { header() }
        if (rows.isNotEmpty()) {
            item(key = "columns") {
                LapRow(
                    first = stringResource(R.string.lap_header_number),
                    second = stringResource(R.string.lap_header_split),
                    third = stringResource(R.string.lap_header_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
            }
        }
        items(rows, key = { it.first }) { (number, split, total) ->
            LapRow(
                first = stringResource(R.string.lap_label, number),
                second = formatLap(split),
                third = formatLap(total),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LapRow(first: String, second: String, third: String, style: TextStyle, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = first, style = style, color = color, modifier = Modifier.weight(1f))
        Text(text = second, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(text = third, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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

private const val TIME_TEXT_SP = 72f

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
