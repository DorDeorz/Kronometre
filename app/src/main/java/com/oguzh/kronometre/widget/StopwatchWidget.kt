package com.oguzh.kronometre.widget

import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.oguzh.kronometre.MainActivity
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchRepository
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.service.StopwatchActions

class StopwatchWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, MEDIUM, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = StopwatchRepository.get(context)
        repository.load()
        provideContent {
            val state by repository.state.collectAsState()
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }
}

private val COMPACT = DpSize(110.dp, 40.dp)
private val MEDIUM = DpSize(150.dp, 100.dp)
private val WIDE = DpSize(250.dp, 100.dp)

private data class WidgetAction(val icon: Int, val label: Int, val action: String)

private fun primaryAction(state: StopwatchState) = when {
    state.isRunning -> WidgetAction(R.drawable.ic_pause, R.string.action_pause, StopwatchActions.ACTION_PAUSE)
    state.elapsedMs > 0L -> WidgetAction(R.drawable.ic_play, R.string.action_resume, StopwatchActions.ACTION_RESUME)
    else -> WidgetAction(R.drawable.ic_play, R.string.action_start, StopwatchActions.ACTION_RESUME)
}

private fun secondaryAction(state: StopwatchState) = when {
    state.isRunning -> WidgetAction(R.drawable.ic_lap, R.string.action_lap, StopwatchActions.ACTION_LAP)
    state.elapsedMs > 0L -> WidgetAction(R.drawable.ic_reset, R.string.action_reset, StopwatchActions.ACTION_RESET)
    else -> null
}

@Composable
private fun WidgetContent(state: StopwatchState) {
    val size = LocalSize.current
    val base = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(24.dp)
        .clickable(actionStartActivity<MainActivity>())
    when {
        size.height < MEDIUM.height -> CompactContent(state, base)
        size.width < WIDE.width -> MediumContent(state, base)
        else -> WideContent(state, base)
    }
}

@Composable
private fun CompactContent(state: StopwatchState, modifier: GlanceModifier) {
    val primary = primaryAction(state)
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeViews(state, textSp = 24f, modifier = GlanceModifier.defaultWeight())
        RoundButton(primary, primary = true, sizeDp = 36)
    }
}

@Composable
private fun MediumContent(state: StopwatchState, modifier: GlanceModifier) {
    val primary = primaryAction(state)
    val secondary = secondaryAction(state)
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusText(state)
        TimeViews(state, textSp = 34f, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondary != null) {
                RoundButton(secondary, primary = false, sizeDp = 44)
                Spacer(GlanceModifier.width(16.dp))
            }
            RoundButton(primary, primary = true, sizeDp = 44)
        }
    }
}

@Composable
private fun WideContent(state: StopwatchState, modifier: GlanceModifier) {
    val context = LocalContext.current
    val primary = primaryAction(state)
    val secondary = secondaryAction(state)
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusText(state)
        TimeViews(state, textSp = 44f, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (secondary != null) {
                FilledButton(
                    text = context.getString(secondary.label),
                    onClick = serviceAction(context, secondary.action),
                    icon = ImageProvider(secondary.icon),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.secondaryContainer,
                        contentColor = GlanceTheme.colors.onSecondaryContainer,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
            }
            FilledButton(
                text = context.getString(primary.label),
                onClick = serviceAction(context, primary.action),
                icon = ImageProvider(primary.icon),
                colors = primaryColors(state),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun StatusText(state: StopwatchState) {
    val context = LocalContext.current
    Text(
        text = context.getString(
            when {
                state.isRunning -> R.string.status_running
                state.elapsedMs > 0L -> R.string.status_paused
                else -> R.string.status_reset
            },
        ),
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
    )
}

@Composable
private fun TimeViews(state: StopwatchState, textSp: Float, modifier: GlanceModifier) {
    val context = LocalContext.current
    val color = GlanceTheme.colors.onSurface.getColor(context).toArgb()
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AndroidRemoteViews(remoteViews = timeViews(context, state, color, textSp))
    }
}

@Composable
private fun RoundButton(action: WidgetAction, primary: Boolean, sizeDp: Int) {
    val context = LocalContext.current
    CircleIconButton(
        imageProvider = ImageProvider(action.icon),
        contentDescription = context.getString(action.label),
        onClick = serviceAction(context, action.action),
        modifier = GlanceModifier.size(sizeDp.dp),
        backgroundColor = if (primary) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer,
        contentColor = if (primary) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
    )
}

@Composable
private fun primaryColors(state: StopwatchState?) =
    if (state?.isRunning == true) {
        ButtonDefaults.buttonColors(backgroundColor = GlanceTheme.colors.error, contentColor = GlanceTheme.colors.onError)
    } else {
        ButtonDefaults.buttonColors(backgroundColor = GlanceTheme.colors.primary, contentColor = GlanceTheme.colors.onPrimary)
    }

private fun serviceAction(context: Context, action: String): Action =
    actionStartService(StopwatchActions.intent(context, action), isForegroundService = true)

private fun timeViews(context: Context, state: StopwatchState, color: Int, textSp: Float): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_remote_chronometer)
    val elapsed = state.currentElapsedMs()
    val base = SystemClock.elapsedRealtime() - elapsed
    for (id in intArrayOf(R.id.widget_chronometer, R.id.widget_static_time)) {
        views.setTextColor(id, color)
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, textSp)
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
