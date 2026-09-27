package com.oguzh.kronometre.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oguzh.kronometre.MainActivity
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchState

class StopwatchNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = NotificationManagerCompat.from(context)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(state: StopwatchState, interactive: Boolean): Notification {
        val publicVersion = builder(state, interactive).build()
        return builder(state, interactive).setPublicVersion(publicVersion).build()
    }

    private fun builder(state: StopwatchState, interactive: Boolean): NotificationCompat.Builder {
        val elapsed = state.currentElapsedMs()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stopwatch)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setContentIntent(openAppIntent())
        if (!state.isRunning) {
            return builder
                .setContentTitle(context.getString(R.string.notification_title_paused))
                .setContentText(DateUtils.formatElapsedTime(elapsed / 1000))
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
                .addAction(R.drawable.ic_play, context.getString(R.string.action_resume), serviceIntent(StopwatchActions.ACTION_RESUME))
                .addAction(R.drawable.ic_reset, context.getString(R.string.action_reset), serviceIntent(StopwatchActions.ACTION_RESET))
        }
        builder.setContentTitle(context.getString(R.string.notification_title))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), serviceIntent(StopwatchActions.ACTION_PAUSE))
            .addAction(R.drawable.ic_lap, context.getString(R.string.action_lap), serviceIntent(StopwatchActions.ACTION_LAP))
        if (interactive) {
            builder.setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - elapsed)
                .setContentText(lapText(state))
        } else {
            builder.setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(context.getString(R.string.notification_minutes, formatMinutes(elapsed)))
        }
        return builder
    }

    private fun lapText(state: StopwatchState): String? =
        if (state.laps.isEmpty()) null else context.getString(R.string.notification_laps, state.laps.size)

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun serviceIntent(action: String): PendingIntent {
        val requestCode = StopwatchActions.requestCode(action)
        val intent = StopwatchActions.intent(context, action)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    companion object {
        const val CHANNEL_ID = "stopwatch_v2"
        private const val LEGACY_CHANNEL_ID = "stopwatch"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN_APP = 0

        fun formatMinutes(elapsedMs: Long): String {
            val minutes = elapsedMs / 60_000L
            return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
        }
    }
}
