package com.oguzh.kronometre.widget

import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartService
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
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.oguzh.kronometre.MainActivity
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchRepository
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.service.StopwatchActions

class StopwatchWidget : GlanceAppWidget() {

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

@Composable
private fun WidgetContent(state: StopwatchState) {
    val context = LocalContext.current
    val timeColor = GlanceTheme.colors.onSurface.getColor(context).toArgb()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(statusRes(state)),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        AndroidRemoteViews(remoteViews = timeViews(context, state, timeColor))
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.isRunning) {
                Button(
                    text = context.getString(R.string.action_pause),
                    onClick = serviceAction(context, StopwatchActions.ACTION_PAUSE),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.error,
                        contentColor = GlanceTheme.colors.onError,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = context.getString(R.string.action_lap),
                    onClick = serviceAction(context, StopwatchActions.ACTION_LAP),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.secondaryContainer,
                        contentColor = GlanceTheme.colors.onSecondaryContainer,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            } else {
                Button(
                    text = context.getString(if (state.elapsedMs == 0L) R.string.action_start else R.string.action_resume),
                    onClick = serviceAction(context, StopwatchActions.ACTION_RESUME),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (state.elapsedMs > 0L) {
                    Spacer(GlanceModifier.width(8.dp))
                    Button(
                        text = context.getString(R.string.action_reset),
                        onClick = serviceAction(context, StopwatchActions.ACTION_RESET),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GlanceTheme.colors.secondaryContainer,
                            contentColor = GlanceTheme.colors.onSecondaryContainer,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

private fun serviceAction(context: Context, action: String) =
    actionStartService(StopwatchActions.intent(context, action), isForegroundService = true)

private fun statusRes(state: StopwatchState): Int = when {
    state.isRunning -> R.string.status_running
    state.elapsedMs > 0L -> R.string.status_paused
    else -> R.string.status_reset
}

private fun timeViews(context: Context, state: StopwatchState, color: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_remote_chronometer)
    val elapsed = state.currentElapsedMs()
    val base = SystemClock.elapsedRealtime() - elapsed
    views.setTextColor(R.id.widget_chronometer, color)
    views.setTextColor(R.id.widget_static_time, color)
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
