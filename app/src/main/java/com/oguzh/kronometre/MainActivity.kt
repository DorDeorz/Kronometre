package com.oguzh.kronometre

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oguzh.kronometre.data.SettingsRepository
import com.oguzh.kronometre.data.StopwatchRepository
import com.oguzh.kronometre.data.ThemeMode
import com.oguzh.kronometre.service.StopwatchActions
import com.oguzh.kronometre.ui.ScreenDimController
import com.oguzh.kronometre.ui.SettingsScreen
import com.oguzh.kronometre.ui.StopwatchScreen
import com.oguzh.kronometre.ui.StopwatchViewModel
import com.oguzh.kronometre.ui.theme.KronometreTheme
import com.oguzh.kronometre.widget.StopwatchWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy { StopwatchRepository.get(applicationContext) }
    private val settings by lazy { SettingsRepository.get(applicationContext) }

    private val viewModel: StopwatchViewModel by viewModels {
        viewModelFactory {
            initializer {
                val appContext = applicationContext
                StopwatchViewModel(repository.state) { action -> StopwatchActions.send(appContext, action) }
            }
        }
    }

    private lateinit var dimController: ScreenDimController
    private var swallowTouch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        dimController = ScreenDimController(window)
        setContentView(R.layout.activity_main)
        findViewById<ComposeView>(R.id.compose_host).setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
            var showSettings by rememberSaveable { mutableStateOf(false) }
            val dark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            KronometreTheme(darkTheme = dark) {
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        themeMode = themeMode,
                        version = BuildConfig.VERSION_NAME,
                        onThemeModeChange = { mode -> lifecycleScope.launch { settings.setThemeMode(mode) } },
                        onBack = { showSettings = false },
                    )
                } else {
                    StopwatchScreen(
                        viewModel = viewModel,
                        onRunningChanged = dimController::setRunning,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
        lifecycleScope.launch {
            repository.load()
            if (savedInstanceState == null) StopwatchWidget().updateAll(applicationContext)
        }
    }

    override fun onStart() {
        super.onStart()
        dimController.setRunning(viewModel.state.value.isRunning)
    }

    override fun onResume() {
        super.onResume()
        dimController.onInteraction()
    }

    override fun onStop() {
        dimController.release()
        super.onStop()
    }

    override fun onDestroy() {
        dimController.release()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) swallowTouch = dimController.onInteraction()
        if (swallowTouch) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) swallowTouch = false
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        dimController.onInteraction()
    }
}
