package com.dordeorz.kronometre

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.dordeorz.kronometre.data.StopwatchRepository
import com.dordeorz.kronometre.timer.TimerController
import com.dordeorz.kronometre.timer.TimerRepository
import com.dordeorz.kronometre.widget.StopwatchWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        val app = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                TimerRepository.get(app).onBoot()
                TimerController.refresh(app)
                StopwatchRepository.get(app).onBoot()
                StopwatchWidget().updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
