package com.oguzh.kronometre.service

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.oguzh.kronometre.data.StopwatchRepository
import com.oguzh.kronometre.data.StopwatchState
import com.oguzh.kronometre.widget.StopwatchWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StopwatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: StopwatchRepository
    private lateinit var notifier: StopwatchNotifier
    private var interactive = true

    private val minuteTick = Runnable {
        val state = repository.state.value
        if (state.isRunning && !interactive) {
            post(notifier.build(state, interactive = false))
            scheduleMinuteTick(state)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val nowInteractive = when (intent.action) {
                Intent.ACTION_SCREEN_ON -> true
                Intent.ACTION_SCREEN_OFF -> false
                else -> return
            }
            if (nowInteractive == interactive) return
            interactive = nowInteractive
            val state = repository.state.value
            if (state.isRunning) render(state, null)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = StopwatchRepository.get(this)
        notifier = StopwatchNotifier(this)
        notifier.ensureChannel()
        interactive = getSystemService(PowerManager::class.java).isInteractive
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        scope.launch {
            val state = when (action) {
                StopwatchActions.ACTION_RESUME -> repository.start()
                StopwatchActions.ACTION_PAUSE -> repository.pause()
                StopwatchActions.ACTION_LAP -> repository.lap()
                StopwatchActions.ACTION_RESET -> repository.reset()
                else -> repository.load()
            }
            refreshWidget()
            render(state, startId)
        }
        return START_STICKY
    }

    private fun render(state: StopwatchState, startId: Int?) {
        handler.removeCallbacks(minuteTick)
        val notification = notifier.build(state, interactive)
        if (!post(notification)) {
            if (!state.isRunning && state.elapsedMs == 0L) {
                NotificationManagerCompat.from(this).cancel(StopwatchNotifier.NOTIFICATION_ID)
            }
            stop(startId)
            return
        }
        if (state.isRunning) {
            if (!interactive) scheduleMinuteTick(state)
        } else {
            val keepNotification = state.elapsedMs > 0L
            ServiceCompat.stopForeground(
                this,
                if (keepNotification) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE,
            )
            stop(startId)
        }
    }

    private fun stop(startId: Int?) {
        if (startId == null) stopSelf() else stopSelf(startId)
    }

    private fun post(notification: Notification): Boolean =
        try {
            ServiceCompat.startForeground(
                this,
                StopwatchNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            true
        } catch (e: IllegalStateException) {
            false
        } catch (e: SecurityException) {
            false
        }

    private fun scheduleMinuteTick(state: StopwatchState) {
        handler.removeCallbacks(minuteTick)
        val delay = MINUTE_MS - (state.currentElapsedMs() % MINUTE_MS) + TICK_SLACK_MS
        handler.postDelayed(minuteTick, delay)
    }

    private fun refreshWidget() {
        val context = applicationContext
        CoroutineScope(Dispatchers.Default).launch { StopwatchWidget().updateAll(context) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(minuteTick)
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val MINUTE_MS = 60_000L
        const val TICK_SLACK_MS = 50L
    }
}
