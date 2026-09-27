package com.dordeorz.kronometre.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Window
import android.view.WindowManager

class ScreenDimController(private val window: Window) {

    private val handler = Handler(Looper.getMainLooper())
    private val dimRunnable = Runnable { dim() }
    private var running = false
    private var dimmed = false
    private var animator: ValueAnimator? = null

    val isDimmed: Boolean get() = dimmed || animator != null

    fun setRunning(value: Boolean) {
        running = value
        if (value) onInteraction() else release()
    }

    fun onInteraction(): Boolean {
        if (!running) return false
        val wasDimmed = isDimmed
        brighten()
        window.decorView.keepScreenOn = true
        handler.removeCallbacks(dimRunnable)
        handler.postDelayed(dimRunnable, DIM_DELAY_MS)
        return wasDimmed
    }

    fun release() {
        handler.removeCallbacks(dimRunnable)
        brighten()
        window.decorView.keepScreenOn = false
    }

    private fun brighten() {
        val current = animator
        animator = null
        current?.cancel()
        dimmed = false
        setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    private fun dim() {
        if (!running) return
        val anim = ValueAnimator.ofFloat(currentBrightness(), DIM_LEVEL).setDuration(DIM_ANIMATION_MS)
        anim.addUpdateListener { setBrightness(it.animatedValue as Float) }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animator !== anim) return
                animator = null
                dimmed = true
                window.decorView.keepScreenOn = false
            }
        })
        animator = anim
        anim.start()
    }

    private fun currentBrightness(): Float {
        val override = window.attributes.screenBrightness
        if (override >= 0f) return override
        val system = try {
            Settings.System.getInt(window.context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Settings.SettingNotFoundException) {
            DEFAULT_START
        }
        return system.coerceIn(DIM_LEVEL, 1f)
    }

    private fun setBrightness(value: Float) {
        val params = window.attributes
        if (params.screenBrightness == value) return
        params.screenBrightness = value
        window.attributes = params
    }

    private companion object {
        const val DIM_DELAY_MS = 10_000L
        const val DIM_ANIMATION_MS = 1_000L
        const val DIM_LEVEL = 0.01f
        const val DEFAULT_START = 0.5f
    }
}
