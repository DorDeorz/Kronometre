package com.dordeorz.kronometre.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

object AlarmSoundPlayer {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    fun start(context: Context) {
        val app = context.applicationContext
        handler.post { startInternal(app) }
    }

    fun stop() {
        handler.post { stopInternal() }
    }

    @Synchronized
    private fun startInternal(context: Context) {
        if (player != null) return
        val source = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val created = MediaPlayer()
        try {
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            created.setDataSource(context, source)
            created.isLooping = true
            created.prepare()
            created.start()
            player = created
        } catch (e: Exception) {
            created.release()
        }
    }

    @Synchronized
    private fun stopInternal() {
        val current = player ?: return
        player = null
        try {
            current.stop()
        } catch (e: IllegalStateException) {
            // Already stopped.
        } finally {
            current.release()
        }
    }
}
