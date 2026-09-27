package com.oguzh.kronometre.widget

import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.oguzh.kronometre.MainActivity
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchRepository
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.service.StopwatchActions
import com.oguzh.kronometre.ui.formatLap

class StopwatchWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = StopwatchRepository.get(context)
        repository.load()
        provideContent {
            val state by repository.state.collectAsState()
            WidgetContent(state)
        }
    }
}

private val ROW_LAYOUT_MAX_HEIGHT = 100.dp
private val HEADER_MIN_HEIGHT = 220.dp
private val TWO_ROW_MAX_WIDTH = 240.dp
private val PADDING = 12.dp
private val GAP = 8.dp
private val HEADER_HEIGHT = 20.dp
private val LAP_LINE_HEIGHT = 20.dp
private const val SECONDARY_HEIGHT_RATIO = 0.85f

private val TextSecondary = ColorProvider(day = Color(0xFF5A5D57), night = Color(0xFFB5B8B0))
private val OnSecondary = ColorProvider(day = Color(0xFF1B1C1A), night = Color(0xFFF2F2EE))
private val OnAccent = ColorProvider(day = Color.White, night = Color.White)
private val Running = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF388E3C))

private enum class ButtonStyle(@DrawableRes val background: Int) {
    Start(R.drawable.widget_button_start),
    Stop(R.drawable.widget_button_stop),
    Secondary(R.drawable.widget_button_secondary),
}

private data class WidgetButton(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val action: String,
    val style: ButtonStyle,
)

private fun buttons(state: StopwatchState): List<WidgetButton> {
    val reset = WidgetButton(R.drawable.ic_reset, R.string.action_reset, StopwatchActions.ACTION_RESET, ButtonStyle.Secondary)
    return when {
        state.isRunning -> listOf(
            reset,
            WidgetButton(R.drawable.ic_pause, R.string.action_pause, StopwatchActions.ACTION_PAUSE, ButtonStyle.Stop),
            WidgetButton(R.drawable.ic_lap, R.string.action_lap, StopwatchActions.ACTION_LAP, ButtonStyle.Secondary),
        )
        state.elapsedMs > 0L -> listOf(
            reset,
            WidgetButton(R.drawable.ic_play, R.string.action_resume, StopwatchActions.ACTION_RESUME, ButtonStyle.Start),
        )
        else -> listOf(
            WidgetButton(R.drawable.ic_play, R.string.action_start, StopwatchActions.ACTION_RESUME, ButtonStyle.Start),
        )
    }
}

@Composable
private fun WidgetContent(state: StopwatchState) {
    val size = LocalSize.current
    val modifier = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(R.drawable.widget_background))
        .clickable(actionStartActivity<MainActivity>())
        .padding(PADDING)
    if (size.height < ROW_LAYOUT_MAX_HEIGHT) {
        RowLayout(state, size.width - PADDING * 2, size.height - PADDING * 2, modifier)
    } else {
        ColumnLayout(state, size.width - PADDING * 2, size.height - PADDING * 2, modifier)
    }
}

@Composable
private fun RowLayout(state: StopwatchState, width: Dp, height: Dp, modifier: GlanceModifier) {
    val all = buttons(state)
    val shown = if (width < 200.dp) listOf(all.first { it.style != ButtonStyle.Secondary }) else all
    val buttonHeight = height.coerceIn(32.dp, 64.dp)
    val buttonWidth = minOf(buttonHeight * 1.3f, (width * 0.5f - GAP * shown.size) / shown.size)
    val buttonsWidth = (buttonWidth + GAP) * shown.size
    val showLap = state.laps.isNotEmpty() && height >= 56.dp
    val timeHeight = if (showLap) height - LAP_LINE_HEIGHT else height
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            TimeViews(state, timeTextSize(state, width - buttonsWidth, timeHeight), GlanceModifier.fillMaxWidth())
            if (showLap) LapLine(state)
        }
        shown.forEach {
            Spacer(GlanceModifier.width(GAP))
            PillButton(it, GlanceModifier.width(buttonWidth).height(buttonHeight), buttonHeight)
        }
    }
}

@Composable
private fun ColumnLayout(state: StopwatchState, width: Dp, height: Dp, modifier: GlanceModifier) {
    val shown = buttons(state)
    val primary = shown.first { it.style != ButtonStyle.Secondary }
    val secondaries = shown.filter { it.style == ButtonStyle.Secondary }
    val twoRows = secondaries.size > 1 && width < TWO_ROW_MAX_WIDTH
    val showHeader = height >= HEADER_MIN_HEIGHT - PADDING * 2
    val showLap = state.laps.isNotEmpty()
    val buttonHeight = (height * if (twoRows) 0.22f else 0.26f).coerceIn(40.dp, 64.dp)
    val secondaryHeight = if (twoRows) buttonHeight * SECONDARY_HEIGHT_RATIO else buttonHeight
    val buttonsHeight = if (twoRows) buttonHeight + GAP + secondaryHeight else buttonHeight
    val timeHeight = height -
        (if (showHeader) HEADER_HEIGHT else 0.dp) -
        (if (showLap) LAP_LINE_HEIGHT else 0.dp) -
        buttonsHeight - GAP
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showHeader) Header(state)
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeViews(state, timeTextSize(state, width, timeHeight), GlanceModifier.fillMaxWidth())
            if (showLap) LapLine(state)
        }
        Spacer(GlanceModifier.height(GAP))
        if (twoRows) {
            PillButton(primary, GlanceModifier.fillMaxWidth().height(buttonHeight), buttonHeight)
            Spacer(GlanceModifier.height(GAP))
            ButtonRow(secondaries, secondaryHeight)
        } else {
            ButtonRow(shown, buttonHeight)
        }
    }
}

@Composable
private fun ButtonRow(buttons: List<WidgetButton>, height: Dp) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        buttons.forEachIndexed { index, button ->
            if (index > 0) Spacer(GlanceModifier.width(GAP))
            PillButton(button, GlanceModifier.defaultWeight().height(height), height)
        }
    }
}

@Composable
private fun LapLine(state: StopwatchState) {
    val context = LocalContext.current
    val count = state.laps.size
    val split = state.laps.last() - (state.laps.getOrNull(count - 2) ?: 0L)
    Text(
        text = context.getString(R.string.widget_last_lap, count, formatLap(split)),
        maxLines = 1,
        modifier = GlanceModifier.height(LAP_LINE_HEIGHT),
        style = TextStyle(color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun Header(state: StopwatchState) {
    val context = LocalContext.current
    val status = when {
        state.isRunning -> R.string.status_running
        state.elapsedMs > 0L -> R.string.status_paused
        else -> R.string.status_reset
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_stopwatch),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(if (state.isRunning) Running else TextSecondary),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = context.getString(status),
            style = TextStyle(
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun PillButton(button: WidgetButton, modifier: GlanceModifier, height: Dp) {
    val context = LocalContext.current
    val tint = if (button.style == ButtonStyle.Secondary) OnSecondary else OnAccent
    Box(
        modifier = modifier
            .background(ImageProvider(button.style.background))
            .clickable(serviceAction(context, button.action)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(button.icon),
            contentDescription = context.getString(button.label),
            modifier = GlanceModifier.size(height * 0.5f),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
private fun TimeViews(state: StopwatchState, textDp: Float, modifier: GlanceModifier) {
    val context = LocalContext.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidRemoteViews(remoteViews = timeViews(context, state, textDp))
    }
}

private fun timeTextSize(state: StopwatchState, width: Dp, height: Dp): Float {
    val ems = if (state.isRunning || state.currentElapsedMs() >= DateUtils.HOUR_IN_MILLIS) 4.4f else 3.4f
    return minOf(width.value / ems, height.value * 0.8f).coerceIn(14f, 120f)
}

private fun serviceAction(context: Context, action: String): Action =
    actionStartService(StopwatchActions.intent(context, action), isForegroundService = true)

private fun timeViews(context: Context, state: StopwatchState, textDp: Float): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_remote_chronometer)
    val color = ContextCompat.getColor(context, R.color.widget_text)
    val elapsed = state.currentElapsedMs()
    val base = SystemClock.elapsedRealtime() - elapsed
    for (id in intArrayOf(R.id.widget_chronometer, R.id.widget_static_time)) {
        views.setTextColor(id, color)
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, textDp)
    }
    if (state.isRunning) {
        views.setChronometer(R.id.widget_chronometer, base, null, true)
        views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
        views.setViewVisibility(R.id.widget_static_time, View.GONE)
    } else {
        views.setChronometer(R.id.widget_chronometer, base, null, false)
        views.setTextViewText(R.id.widget_static_time, DateUtils.formatElapsedTime(elapsed / 1000))
        views.setViewVisibility(R.id.widget_chronometer, View.GONE)
        views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
    }
    return views
}
