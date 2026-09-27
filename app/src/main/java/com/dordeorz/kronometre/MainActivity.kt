package com.dordeorz.kronometre

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dordeorz.kronometre.data.AppSettings
import com.dordeorz.kronometre.data.SettingsRepository
import com.dordeorz.kronometre.data.StopwatchRepository
import com.dordeorz.kronometre.data.ThemeMode
import com.dordeorz.kronometre.data.UpdateChecker
import com.dordeorz.kronometre.data.UpdateStatus
import com.dordeorz.kronometre.service.StopwatchActions
import com.dordeorz.kronometre.ui.ScreenDimController
import com.dordeorz.kronometre.ui.SettingsScreen
import com.dordeorz.kronometre.ui.MainScreen
import com.dordeorz.kronometre.ui.StopwatchViewModel
import com.dordeorz.kronometre.ui.TimerViewModel
import com.dordeorz.kronometre.timer.TimerController
import com.dordeorz.kronometre.ui.theme.KronometreTheme
import com.dordeorz.kronometre.widget.StopwatchWidget
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

    private val timerViewModel: TimerViewModel by viewModels()
    private val openTimerRequest = mutableStateOf(false)

    private lateinit var dimController: ScreenDimController
    private var swallowTouch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        dimController = ScreenDimController(window)
        openTimerRequest.value = intent.getBooleanExtra(TimerController.EXTRA_OPEN_TIMER, false)
        setContentView(R.layout.activity_main)
        findViewById<ComposeView>(R.id.compose_host).setContent {
            val appSettings by settings.settings.collectAsStateWithLifecycle(AppSettings())
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Unknown) }
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            val openTimer by openTimerRequest
            LaunchedEffect(openTimer) {
                if (openTimer) {
                    selectedTab = 1
                    showSettings = false
                    openTimerRequest.value = false
                }
            }
            val dark = when (appSettings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(appSettings.dimScreen) { dimController.enabled = appSettings.dimScreen }
            KronometreTheme(darkTheme = dark) {
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        settings = appSettings,
                        version = BuildConfig.VERSION_NAME,
                        onThemeModeChange = { mode -> lifecycleScope.launch { settings.setThemeMode(mode) } },
                        onShowCentisChange = { value -> lifecycleScope.launch { settings.setShowCentis(value) } },
                        onDimScreenChange = { value -> lifecycleScope.launch { settings.setDimScreen(value) } },
                        updateStatus = updateStatus,
                        onCheckUpdates = {
                            updateStatus = UpdateStatus.Checking
                            lifecycleScope.launch { updateStatus = UpdateChecker.check(BuildConfig.VERSION_NAME) }
                        },
                        onOpenUrl = ::openUrl,
                        onBack = { showSettings = false },
                    )
                } else {
                    MainScreen(
                        stopwatchViewModel = viewModel,
                        timerViewModel = timerViewModel,
                        showCentis = appSettings.showCentis,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
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

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            return
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(TimerController.EXTRA_OPEN_TIMER, false)) openTimerRequest.value = true
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
