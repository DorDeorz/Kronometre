package com.dordeorz.kronometre.service

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object NotificationHealth {

    fun canPost(context: Context, channelId: String): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() && isChannelEnabled(context, channelId)

    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = NotificationManagerCompat.from(context).getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun isAlarmAudible(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = NotificationManagerCompat.from(context).getNotificationChannel(channelId) ?: return true
        return channel.importance >= NotificationManager.IMPORTANCE_HIGH && channel.sound != null
    }
}
