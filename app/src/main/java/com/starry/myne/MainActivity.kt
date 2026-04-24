package com.starry.myne

import android.content.pm.ShortcutManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.starry.myne.helpers.NetworkObserver
import com.starry.myne.ui.screens.main.MainScreen
import com.starry.myne.ui.screens.settings.viewmodels.SettingsViewModel
import com.starry.myne.ui.screens.settings.viewmodels.ThemeMode
import com.starry.myne.ui.theme.AdjustEdgeToEdge
import custom.AppTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var networkObserver: NetworkObserver
    lateinit var settingsViewModel: SettingsViewModel
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkObserver = NetworkObserver(applicationContext)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Install splash screen before setting content.
        installSplashScreen().setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        enableEdgeToEdge() // enable edge to edge for the activity.

        setContent {
            val themeState = settingsViewModel.theme.observeAsState(initial = ThemeMode.Auto)
            var darktheme = when {
                themeState.value == ThemeMode.Dark -> true
                themeState.value == ThemeMode.Light -> false
                else -> isSystemInDarkTheme()

            }
            AppTheme(isDarkTheme = darktheme) {
                AdjustEdgeToEdge(
                    activity = this,
                    themeState = settingsViewModel.getCurrentTheme()
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colors.background
                ) {
                    val startDestination by mainViewModel.startDestination
                    val status by networkObserver.observe().collectAsState(
                        initial = NetworkObserver.Status.Unavailable
                    )

                    MainScreen(
                        intent = intent,
                        startDestination = startDestination,
                        networkStatus = status
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        updateShortcuts()
    }

    private fun updateShortcuts() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        mainViewModel.buildDynamicShortcuts(
            context = this,
            limit = shortcutManager.maxShortcutCountPerActivity,
            onComplete = { shortcuts ->
                try {
                    shortcutManager.dynamicShortcuts = shortcuts
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity", "Error setting dynamic shortcuts", e)
                }
            }
        )
    }
}