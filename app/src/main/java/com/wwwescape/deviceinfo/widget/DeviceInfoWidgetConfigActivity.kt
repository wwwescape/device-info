package com.wwwescape.deviceinfo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.settings.AppSettings
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import com.wwwescape.deviceinfo.data.settings.ThemeMode
import com.wwwescape.deviceinfo.ui.screens.settings.label
import com.wwwescape.deviceinfo.ui.theme.DeviceInfoTheme
import kotlinx.coroutines.launch

/**
 * Launched by the system right after the user drops the widget on their home screen (declared
 * via `android:configure` in `device_info_widget_info.xml`), and reopened later by the widget's own
 * gear icon (see [widgetConfigIntent]). Lets the widget's theme be pinned to Light/Dark/System
 * independent of the app's own setting — e.g. a widget on an always-dark home screen next to an
 * app the user runs in Light mode. Must be `exported="true"`: the widget host (the launcher, a
 * separate app) is what starts this activity, not the system on our behalf.
 *
 * If the user leaves a previous instance of this activity in the background without finishing it
 * (e.g. presses Home instead of Save), the system redelivers a later launch to that same instance
 * via [onNewIntent] rather than creating a fresh one — [appWidgetIdState] must be Compose state
 * (not a plain field) so the UI and [saveAndFinish] pick up the newly-requested widget instead of
 * silently reusing the first one this activity instance ever saw.
 */
class DeviceInfoWidgetConfigActivity : ComponentActivity() {

    private val appWidgetIdState = mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        applyIntent(intent)
        if (appWidgetIdState.intValue == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val settings by remember { SettingsRepository.settingsFlow(applicationContext) }
                .collectAsState(initial = AppSettings())
            var selected by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var isEditingExisting by remember { mutableStateOf(false) }
            val appWidgetId = appWidgetIdState.intValue

            LaunchedEffect(appWidgetId) {
                val glanceId = GlanceAppWidgetManager(this@DeviceInfoWidgetConfigActivity).getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState(this@DeviceInfoWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                val stored = prefs[WIDGET_THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                selected = stored ?: ThemeMode.SYSTEM
                isEditingExisting = stored != null
            }

            val darkTheme = when (selected) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DeviceInfoTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor,
                colorTheme = settings.colorTheme,
                themeContrast = settings.themeContrast,
                pureDark = settings.pureDark,
                absoluteDark = settings.absoluteDark,
            ) {
                DeviceInfoWidgetConfigScreen(
                    selected = selected,
                    confirmLabel = stringResource(
                        if (isEditingExisting) R.string.widget_config_save else R.string.widget_config_add,
                    ),
                    onSelect = { selected = it },
                    onConfirm = { lifecycleScope.launch { saveAndFinish(appWidgetId, selected) } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setResult(RESULT_CANCELED)
        applyIntent(intent)
        if (appWidgetIdState.intValue == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
    }

    /** This activity is exported, so any app can launch it with any widget ID — only accept IDs
     * that belong to one of our own placed widgets; anything else is treated as invalid. */
    private fun applyIntent(intent: Intent?) {
        val requestedId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val ownIds = runCatching {
            AppWidgetManager.getInstance(this)
                ?.getAppWidgetIds(ComponentName(this, DeviceInfoWidgetReceiver::class.java))
        }.getOrNull() ?: IntArray(0)
        appWidgetIdState.intValue = if (requestedId in ownIds) requestedId else AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private suspend fun saveAndFinish(appWidgetId: Int, mode: ThemeMode) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(this, glanceId) { prefs -> prefs[WIDGET_THEME_MODE_KEY] = mode.name }
        DeviceInfoWidget().update(this, glanceId)

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@Composable
private fun DeviceInfoWidgetConfigScreen(
    selected: ThemeMode,
    confirmLabel: String,
    onSelect: (ThemeMode) -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.systemBarsPadding().padding(24.dp).fillMaxSize()) {
            Text(text = stringResource(R.string.widget_config_title), style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.widget_config_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = mode == selected, onClick = { onSelect(mode) })
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == selected, onClick = null)
                    Text(
                        text = mode.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text(confirmLabel)
            }
        }
    }
}
