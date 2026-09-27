package com.oguzh.kronometre.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object StopwatchActions {
    const val ACTION_RESUME = "com.oguzh.kronometre.action.RESUME"
    const val ACTION_PAUSE = "com.oguzh.kronometre.action.PAUSE"
    const val ACTION_LAP = "com.oguzh.kronometre.action.LAP"
    const val ACTION_RESET = "com.oguzh.kronometre.action.RESET"

    fun intent(context: Context, action: String): Intent =
        Intent(context, StopwatchService::class.java).setAction(action)

    fun requestCode(action: String): Int = action.hashCode()

    fun send(context: Context, action: String): Boolean =
        try {
            ContextCompat.startForegroundService(context, intent(context, action))
            true
        } catch (e: IllegalStateException) {
            false
        } catch (e: SecurityException) {
            false
        }
}
