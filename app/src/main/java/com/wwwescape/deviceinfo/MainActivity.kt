package com.wwwescape.deviceinfo

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wwwescape.deviceinfo.data.settings.AppSettings
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import com.wwwescape.deviceinfo.data.settings.THEME_MODE_PREF_KEY
import com.wwwescape.deviceinfo.data.settings.THEME_PREFS_NAME
import com.wwwescape.deviceinfo.data.settings.ThemeMode
import com.wwwescape.deviceinfo.ui.DeviceInfoApp
import com.wwwescape.deviceinfo.ui.navigation.Destination
import com.wwwescape.deviceinfo.ui.theme.DeviceInfoTheme
import com.wwwescape.deviceinfo.widget.EXTRA_DESTINATION_ROUTE
import com.wwwescape.deviceinfo.widget.WidgetUpdater
import com.wwwescape.deviceinfo.widget.syncWidgetRefreshWork
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pendingRoute = mutableStateOf<String?>(null)

    /** Runs before any DataStore/Compose code can — reads the synchronous SharedPreferences
     * mirror of [ThemeMode] (see [SettingsRepository.setThemeMode]) and forces the base context's
     * night-mode configuration to match, so the window's pre-Compose theme resolution (and, on
     * API <31 where the splash is drawn in-process, the splash itself) honors the user's explicit
     * Light/Dark choice instead of defaulting to the raw system setting. On API 31+ the native
     * splash is drawn by the system before this process starts, so it can't be reached this way —
     * a brief system-colored flash there is an Android platform constraint, not fixable in-app. */
    override fun attachBaseContext(newBase: Context) {
        val stored = newBase.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(THEME_MODE_PREF_KEY, null)
        val mode = stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
        val forcedNightBit = when (mode) {
            ThemeMode.LIGHT -> Configuration.UI_MODE_NIGHT_NO
            ThemeMode.DARK -> Configuration.UI_MODE_NIGHT_YES
            ThemeMode.SYSTEM -> null
        }
        val context = if (forcedNightBit == null) {
            newBase
        } else {
            val overridden = Configuration(newBase.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or forcedNightBit
            }
            newBase.createConfigurationContext(overridden)
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = destinationRouteFrom(intent)
        syncWidgetRefreshWork(applicationContext)

        setContent {
            val settings by remember { SettingsRepository.settingsFlow(applicationContext) }
                .collectAsState(initial = AppSettings())
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }
            val route by pendingRoute

            DeviceInfoTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor,
                colorTheme = settings.colorTheme,
                themeContrast = settings.themeContrast,
                pureDark = settings.pureDark,
                absoluteDark = settings.absoluteDark,
            ) {
                DeviceInfoApp(
                    startDestinationRoute = route,
                    onStartDestinationConsumed = { pendingRoute.value = null },
                )
            }
        }
    }

    /** A widget tap while the app is already running arrives here instead of a fresh [onCreate]
     * — requires `launchMode="singleTop"` on this activity in the manifest. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = destinationRouteFrom(intent)
    }

    /** This activity is exported (it's the launcher entry point), so any app can start it with
     * arbitrary extras — only honor routes that name one of our own screens. */
    private fun destinationRouteFrom(intent: Intent?): String? {
        val route = intent?.getStringExtra(EXTRA_DESTINATION_ROUTE) ?: return null
        return route.takeIf { candidate -> Destination.entries.any { it.route == candidate } }
    }

    /** Widgets otherwise only refresh every ~15 minutes in the background (Android's WorkManager
     * floor) — this covers the common case of switching back to the app (from recents, or a
     * fresh/singleTop launch) so the widgets are current the next time they're glanced at,
     * without needing a persistent foreground service. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { WidgetUpdater.refreshAll(applicationContext) }
    }
}
