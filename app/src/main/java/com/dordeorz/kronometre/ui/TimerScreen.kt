package com.dordeorz.kronometre.ui

import android.os.SystemClock
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.timer.TimerState
import com.dordeorz.kronometre.timer.TimerStatus
import com.dordeorz.kronometre.ui.theme.KronometreTheme
import com.dordeorz.kronometre.ui.theme.actionColors
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TimerScreen(
    state: TimerState,
    exactAlarmsAllowed: Boolean,
    onStart: (Long) -> Unit,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (!exactAlarmsAllowed) {
            Text(
                text = stringResource(R.string.timer_exact_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        if (state.status == TimerStatus.Idle) {
            TimerInput(initialMs = state.durationMs, onStart = onStart, modifier = Modifier.weight(1f))
        } else {
            TimerRunning(state, onToggle, onAddMinute, onCancel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimerInput(initialMs: Long, onStart: (Long) -> Unit, modifier: Modifier) {
    var digits by rememberSaveable { mutableStateOf(msToDigits(initialMs)) }
    val duration = digitsToMs(digits)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    DigitsDisplay(digits)
                    StartButton(enabled = duration > 0L, onClick = { onStart(duration) }, modifier = Modifier.padding(top = 16.dp))
                }
                Keypad(onDigit = { digits = push(digits, it) }, onDelete = { digits = pop(digits) }, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                DigitsDisplay(digits)
                Keypad(
                    onDigit = { digits = push(digits, it) },
                    onDelete = { digits = pop(digits) },
                    modifier = Modifier.widthIn(max = 360.dp),
                )
                StartButton(enabled = duration > 0L, onClick = { onStart(duration) }, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun DigitsDisplay(digits: String) {
    val padded = digits.padStart(6, '0')
    val active = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val units = listOf(R.string.unit_hours, R.string.unit_minutes, R.string.unit_seconds).map { stringResource(it) }
    val text = buildAnnotatedString {
        for (i in 0 until 3) {
            val start = i * 2
            val filled = 6 - digits.length <= start + 1
            withStyle(SpanStyle(color = if (filled) active else muted, fontSize = 52.sp, fontWeight = FontWeight.Light)) {
                append(padded.substring(start, start + 2))
            }
            withStyle(SpanStyle(color = muted, fontSize = 20.sp)) { append(units[i]) }
            if (i < 2) append(" ")
        }
    }
    Text(text = text, maxLines = 1, style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"))
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, modifier: Modifier) {
    val rows = listOf("123", "456", "789")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> KeyButton(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton("00", Modifier.weight(1f)) { onDigit('0'); onDigit('0') }
            KeyButton("0", Modifier.weight(1f)) { onDigit('0') }
            KeyButton("⌫", Modifier.weight(1f), onClick = onDelete)
        }
    }
}

@Composable
private fun KeyButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.aspectRatio(1.6f)) {
        Text(label, fontSize = 24.sp)
    }
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    RoundAction(
        icon = R.drawable.ic_play,
        label = stringResource(R.string.action_start),
        containerColor = actionColors().start,
        contentColor = MaterialTheme.colorScheme.surface,
        size = 80.dp,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun TimerRunning(
    state: TimerState,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    val remaining by produceState(state.remaining(), state) {
        value = state.remaining()
        while (isActive && state.status == TimerStatus.Running) {
            delay(value % 1000L + 1L)
            value = state.remaining()
        }
    }
    val ringing = state.status == TimerStatus.Ringing
    val progress = if (state.durationMs > 0L) remaining.toFloat() / state.durationMs else 0f
    val colors = actionColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                color = if (ringing) colors.pause else colors.start,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (ringing) stringResource(R.string.timer_done) else DateUtils.formatElapsedTime((remaining + 999L) / 1000L),
                    fontSize = if (ringing) 32.sp else 56.sp,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                )
                TextButton(onClick = onAddMinute) { Text(stringResource(R.string.timer_add_minute)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction(
                icon = R.drawable.ic_reset,
                label = stringResource(if (ringing) R.string.timer_stop else R.string.timer_cancel),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 64.dp,
                enabled = true,
                onClick = onCancel,
            )
            if (!ringing) {
                val running = state.status == TimerStatus.Running
                RoundAction(
                    icon = if (running) R.drawable.ic_pause else R.drawable.ic_play,
                    label = stringResource(if (running) R.string.action_pause else R.string.action_resume),
                    containerColor = if (running) colors.pause else colors.start,
                    contentColor = MaterialTheme.colorScheme.surface,
                    size = 88.dp,
                    enabled = true,
                    onClick = onToggle,
                )
            }
        }
    }
}

private fun push(digits: String, digit: Char): String =
    if (digits.length >= 6 || (digits.isEmpty() && digit == '0')) digits else digits + digit

private fun pop(digits: String): String = digits.dropLast(1)

private fun digitsToMs(digits: String): Long {
    val padded = digits.padStart(6, '0')
    val h = padded.substring(0, 2).toLong()
    val m = padded.substring(2, 4).toLong()
    val s = padded.substring(4, 6).toLong()
    return ((h * 3600L) + (m * 60L) + s) * 1000L
}

private fun msToDigits(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000L
    val text = String.format(Locale.ROOT, "%02d%02d%02d", (total / 3600L).coerceAtMost(99L), (total % 3600L) / 60L, total % 60L)
    return text.trimStart('0')
}

@Preview(showBackground = true)
@Composable
private fun TimerInputPreview() {
    KronometreTheme {
        TimerScreen(TimerState(), exactAlarmsAllowed = true, onStart = {}, onToggle = {}, onAddMinute = {}, onCancel = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerRunningPreview() {
    KronometreTheme {
        TimerScreen(
            TimerState().start(300_000L, SystemClock.elapsedRealtime()),
            exactAlarmsAllowed = true,
            onStart = {},
            onToggle = {},
            onAddMinute = {},
            onCancel = {},
        )
    }
}
