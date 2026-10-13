package com.dordeorz.kronometre.ui

import android.graphics.Typeface
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
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

@Composable
fun TimeDisplay(
    state: StopwatchState,
    now: Long,
    modifier: Modifier = Modifier,
    textDp: Float = TIME_TEXT_DP,
    showCentis: Boolean = false,
) {
    if (showCentis) {
        CentisTimeDisplay(state, now, modifier, textDp)
        return
    }
    val color = MaterialTheme.colorScheme.onBackground.toArgb()
    AndroidView(
        factory = { context ->
            Chronometer(context).apply {
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                maxLines = 1
            }
        },
        update = { chronometer ->
            chronometer.setTextColor(color)
            chronometer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textDp)
            chronometer.base = SystemClock.elapsedRealtime() - state.currentElapsedMs()
            if (state.isRunning) chronometer.start() else chronometer.stop()
        },
        modifier = modifier,
    )
}

@Composable
fun rememberFrameClock(state: StopwatchState, enabled: Boolean): Long {
    val now by produceState(SystemClock.elapsedRealtime(), state, enabled) {
        value = SystemClock.elapsedRealtime()
        if (enabled && state.isRunning) {
            while (isActive) {
                withFrameMillis { }
                value = SystemClock.elapsedRealtime()
            }
        }
    }
    return now
}

@Composable
private fun CentisTimeDisplay(state: StopwatchState, now: Long, modifier: Modifier, textDp: Float) {
    val ms = state.currentElapsedMs(now)
    val size = with(LocalDensity.current) { textDp.dp.toSp() }
    val text = buildAnnotatedString {
        append(DateUtils.formatElapsedTime(ms / 1000))
        withStyle(SpanStyle(fontSize = size * CENTIS_SCALE)) {
            append("," + ((ms % 1000) / 10).toString().padStart(2, '0'))
        }
    }
    Text(
        text = text,
        style = TextStyle(
            fontSize = size,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = MaterialTheme.colorScheme.onBackground,
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun ProgressRing(fraction: Float, stroke: Dp, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val sweep = 360f * fraction.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val width = stroke.toPx()
        val inset = width / 2f
        val arcSize = Size(size.width - width, size.height - width)
        val topLeft = Offset(inset, inset)
        drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
        if (sweep > 0f) {
            drawArc(color, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

fun ringFraction(state: StopwatchState, now: Long, sweeping: Boolean): Float = when {
    state.isRunning && !sweeping -> 1f
    state.isRunning || state.elapsedMs > 0L -> (state.currentElapsedMs(now) % MINUTE_MS) / MINUTE_MS.toFloat()
    else -> 0f
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
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
            containerColor = if (state.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (state.isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun RoundAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
    val context = LocalContext.current
    val splits = laps.mapIndexed { index, total -> total - (laps.getOrNull(index - 1) ?: 0L) }
    val fastest = if (splits.size >= 3) splits.indexOf(splits.min()) else -1
    val slowest = if (splits.size >= 3) splits.indexOf(splits.max()) else -1
    val rows = laps.indices.reversed()
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (header != null) item(key = "header") { header() }
        if (laps.isNotEmpty()) {
            item(key = "summary") { LapSummary(splits, fastest) }
            item(key = "laps") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.laps_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { copyLaps(context, laps) }) {
                                Text(stringResource(R.string.laps_copy))
                            }
                        }
                        LapRow(
                            first = stringResource(R.string.lap_header_number),
                            second = stringResource(R.string.lap_header_split),
                            third = stringResource(R.string.lap_header_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        rows.forEach { index ->
                            val color = when (index) {
                                fastest -> MaterialTheme.colorScheme.primary
                                slowest -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            LapRow(
                                first = stringResource(R.string.lap_label, index + 1),
                                second = formatLap(splits[index]),
                                third = formatLap(laps[index]),
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                                bold = index == fastest,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LapSummary(splits: List<Long>, fastest: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            icon = R.drawable.ic_lap,
            title = stringResource(R.string.laps_count_title),
            value = splits.size.toString(),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            icon = R.drawable.ic_stopwatch,
            title = stringResource(R.string.laps_fastest_title),
            value = formatLap(if (fastest >= 0) splits[fastest] else splits.min()),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(icon: Int, title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun copyLaps(context: Context, laps: List<Long>) {
    val text = laps.mapIndexed { index, total ->
        val split = total - (laps.getOrNull(index - 1) ?: 0L)
        context.getString(R.string.lap_label, index + 1) + "\t" + formatLap(split) + "\t" + formatLap(total)
    }.joinToString("\n")
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.laps_clip_label), text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.laps_copied, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun LapRow(first: String, second: String, third: String, style: TextStyle, color: Color, bold: Boolean = false) {
    val rowStyle = if (bold) style.copy(fontWeight = FontWeight.Bold) else style
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = first, style = rowStyle, color = color, modifier = Modifier.weight(1f))
        Text(text = second, style = rowStyle, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(text = third, style = rowStyle, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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

private const val TIME_TEXT_DP = 56f
private const val MINUTE_MS = 60_000L
private const val CENTIS_SCALE = 0.55f

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
