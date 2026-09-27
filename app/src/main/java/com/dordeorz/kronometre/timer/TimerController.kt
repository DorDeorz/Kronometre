package com.dordeorz.kronometre.timer

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.dordeorz.kronometre.MainActivity
import com.dordeorz.kronometre.R

object TimerController {
    const val ACTION_PAUSE = "com.dordeorz.kronometre.timer.PAUSE"
    const val ACTION_RESUME = "com.dordeorz.kronometre.timer.RESUME"
    const val ACTION_CANCEL = "com.dordeorz.kronometre.timer.CANCEL"
    const val ACTION_ADD_MINUTE = "com.dordeorz.kronometre.timer.ADD_MINUTE"
    const val ACTION_ALARM = "com.dordeorz.kronometre.timer.ALARM"
    const val ACTION_STOP_ALARM = "com.dordeorz.kronometre.timer.STOP_ALARM"
    const val EXTRA_OPEN_TIMER = "com.dordeorz.kronometre.extra.OPEN_TIMER"

    private const val CHANNEL_ID = "timer"
    private const val ALARM_CHANNEL_ID = "timer_alarm"
    private const val NOTIFICATION_ID = 2
    private const val REQUEST_ALARM = 100
    private const val REQUEST_OPEN = 101

    suspend fun start(context: Context, durationMs: Long) = apply(context) { state, now -> state.start(durationMs, now) }

    suspend fun handle(context: Context, action: String) = apply(context) { state, now ->
        when (action) {
            ACTION_PAUSE -> state.pause(now)
            ACTION_RESUME -> state.resume(now)
            ACTION_ADD_MINUTE -> state.addMinute(now)
            ACTION_ALARM -> if (state.status == TimerStatus.Running && state.remaining(now) <= ALARM_TOLERANCE_MS) state.ring() else state
            ACTION_CANCEL, ACTION_STOP_ALARM -> state.cancel()
            else -> state
        }
    }

    suspend fun refresh(context: Context) {
        val state = TimerRepository.get(context).load()
        render(context.applicationContext, state)
    }

    private suspend fun apply(context: Context, transform: (TimerState, Long) -> TimerState) {
        val app = context.applicationContext
        val (_, new) = TimerRepository.get(app).update(transform)
        render(app, new)
    }

    private fun render(context: Context, state: TimerState) {
        ensureChannels(context)
        if (state.status == TimerStatus.Running) scheduleAlarm(context, state) else cancelAlarm(context)
        val manager = NotificationManagerCompat.from(context)
        if (state.status == TimerStatus.Idle) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            manager.notify(NOTIFICATION_ID, build(context, state))
        } catch (e: SecurityException) {
            return
        }
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    }

    private fun scheduleAlarm(context: Context, state: TimerState) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pending = alarmIntent(context)
        val remaining = state.remaining()
        try {
            if (canScheduleExact(context)) {
                val info = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + remaining, openIntent(context))
                alarmManager.setAlarmClock(info, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + remaining,
                    pending,
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + remaining, pending)
        }
    }

    private fun cancelAlarm(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(alarmIntent(context))
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(ALARM_CHANNEL_ID, context.getString(R.string.timer_alarm_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = VIBRATION
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
    }

    private fun build(context: Context, state: TimerState): Notification {
        if (state.status == TimerStatus.Ringing) {
            val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(context.getString(R.string.timer_done))
                .setContentText(DateUtils.formatElapsedTime(state.durationMs / 1000))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioManager.STREAM_ALARM)
                .setVibrate(VIBRATION)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(openIntent(context))
                .setDeleteIntent(actionIntent(context, ACTION_STOP_ALARM))
                .addAction(R.drawable.ic_reset, context.getString(R.string.timer_stop), actionIntent(context, ACTION_STOP_ALARM))
                .addAction(R.drawable.ic_timer, context.getString(R.string.timer_add_minute), actionIntent(context, ACTION_ADD_MINUTE))
                .build()
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
            return notification
        }
        val builder = baseBuilder(context, state)
        return builder.setPublicVersion(baseBuilder(context, state).build()).build()
    }

    private fun baseBuilder(context: Context, state: TimerState): NotificationCompat.Builder {
        val remaining = state.remaining()
        val running = state.status == TimerStatus.Running
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openIntent(context))
            .setContentTitle(DateUtils.formatElapsedTime(remaining / 1000))
            .setContentText(context.getString(if (running) R.string.timer_running else R.string.timer_paused))
        val views = timeViews(context, state, remaining)
        builder.setCustomContentView(views).setCustomBigContentView(views)
        if (running) {
            builder.addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), actionIntent(context, ACTION_PAUSE))
        } else {
            builder.addAction(R.drawable.ic_play, context.getString(R.string.action_resume), actionIntent(context, ACTION_RESUME))
        }
        return builder
            .addAction(R.drawable.ic_timer, context.getString(R.string.timer_add_minute), actionIntent(context, ACTION_ADD_MINUTE))
            .addAction(R.drawable.ic_reset, context.getString(R.string.timer_cancel), actionIntent(context, ACTION_CANCEL))
    }

    private fun timeViews(context: Context, state: TimerState, remaining: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_time)
        var live = false
        if (state.status == TimerStatus.Running && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            views.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() + remaining, null, true)
            views.setChronometerCountDown(R.id.notification_chronometer, true)
            live = true
        }
        views.setViewVisibility(R.id.notification_chronometer, if (live) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.notification_static_time, if (live) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.notification_static_time, DateUtils.formatElapsedTime((remaining + 999) / 1000))
        val subtitle = context.getString(if (state.status == TimerStatus.Running) R.string.timer_running else R.string.timer_paused)
        views.setTextViewText(R.id.notification_subtitle, subtitle)
        views.setViewVisibility(R.id.notification_subtitle, View.VISIBLE)
        return views
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            Intent(context, TimerReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun actionIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, TimerReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TIMER, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private const val ALARM_TOLERANCE_MS = 2_000L
    private val VIBRATION = longArrayOf(0L, 600L, 400L, 600L, 400L, 600L)
}
