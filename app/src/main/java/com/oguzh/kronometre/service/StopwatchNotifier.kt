package com.oguzh.kronometre.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oguzh.kronometre.MainActivity
import com.oguzh.kronometre.R
import com.oguzh.kronometre.data.StopwatchState

class StopwatchNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
            setSound(null, null)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun build(state: StopwatchState, interactive: Boolean): Notification {
        val publicVersion = builder(state, interactive).build()
        return builder(state, interactive).setPublicVersion(publicVersion).build()
    }

    private fun builder(state: StopwatchState, interactive: Boolean): NotificationCompat.Builder {
        val elapsed = state.currentElapsedMs()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stopwatch)
            .setContentTitle(context.getString(R.string.notification_title))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setContentIntent(openAppIntent())
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
        if (state.isRunning) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), serviceIntent(StopwatchActions.ACTION_PAUSE))
                .addAction(R.drawable.ic_lap, context.getString(R.string.action_lap), serviceIntent(StopwatchActions.ACTION_LAP))
        } else {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
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

    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            context,
            StopwatchActions.requestCode(action),
            StopwatchActions.intent(context, action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val CHANNEL_ID = "stopwatch"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN_APP = 0

        fun formatMinutes(elapsedMs: Long): String {
            val minutes = elapsedMs / 60_000L
            return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
        }
    }
}
