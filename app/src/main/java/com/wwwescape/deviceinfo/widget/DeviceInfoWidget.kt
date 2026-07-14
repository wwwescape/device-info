package com.wwwescape.deviceinfo.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.battery.BatteryRepository
import com.wwwescape.deviceinfo.data.deviceos.DeviceOsRepository
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import com.wwwescape.deviceinfo.data.settings.ThemeMode
import com.wwwescape.deviceinfo.ui.navigation.Destination
import com.wwwescape.deviceinfo.ui.theme.resolveDeviceInfoColorScheme
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

private const val RING_SIZE_DP = 64

/** Fixed 4x3, non-resizable — a re-creation of the Dashboard's [DashboardHero] card at the exact
 * same colors/type scale/shapes, sized down only by the fixed widget footprint. Tapping anywhere
 * opens the app's Dashboard. Its theme (Light/Dark/System) is set per-instance via
 * [DeviceInfoWidgetConfigActivity], independent of the app's own theme setting; dynamic color still
 * follows the app's global setting. */
class DeviceInfoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deviceInfo = DeviceOsRepository.collectStatic(context)
        val battery = BatteryRepository.currentBatteryInfo(context)
        val storage = MemoryRepository.readStorageVolumes(context).firstOrNull()
        val batteryFraction = battery.percent / 100f
        val overviewLabel = context.getString(R.string.hero_device_overview)
        val androidVersionText = context.getString(R.string.hero_os_version, deviceInfo.androidVersion)
        val batteryLabel = context.getString(R.string.section_battery)
        val storageLabel = context.getString(R.string.widget_storage_label)

        // The widget's own theme choice (set via DeviceInfoWidgetConfigActivity) takes the place
        // of MainActivity's app-wide themeMode, but dynamic color still follows the app setting.
        val widgetPrefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val widgetThemeMode = widgetPrefs[WIDGET_THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val appSettings = SettingsRepository.settingsFlow(context).first()
        val systemInDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val darkTheme = when (widgetThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
        val colorScheme = resolveDeviceInfoColorScheme(
            context = context,
            darkTheme = darkTheme,
            dynamicColor = appSettings.useDynamicColor,
            colorTheme = appSettings.colorTheme,
            themeContrast = appSettings.themeContrast,
            pureDark = appSettings.pureDark,
            absoluteDark = appSettings.absoluteDark,
        )
        // DashboardHero's pill badge is a low-contrast overlay (onPrimaryContainer @ 12% alpha),
        // not a solid color — baked from the resolved scheme since Glance's theme-linked
        // ColorProviders don't support alpha compositing.
        val pillContainerColor = ColorProvider(colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val density = context.resources.displayMetrics.density
        val batteryRing = ringBitmap(colorScheme, batteryFraction, RING_SIZE_DP, density)
        val storageGauge = storage?.let { fractionOf(it.usedBytes, it.totalBytes) }?.let { fraction ->
            fraction to ringBitmap(colorScheme, fraction, RING_SIZE_DP, density)
        }

        provideContent {
            GlanceTheme(colors = ColorProviders(colorScheme)) {
                // Matches DashboardHero's Card(shape = extraLarge, padding = 20.dp).
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(28.dp)
                        .clickable(actionStartActivity(widgetDestinationIntent(context, Destination.Overview.route)))
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetPillBadge(
                            text = overviewLabel,
                            contentColor = GlanceTheme.colors.onPrimaryContainer,
                            containerColor = pillContainerColor,
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        // A 20dp glyph inside a 40dp tap target — RemoteViews click regions on a
                        // small icon are easy to miss/fall through to the card's own click handler
                        // otherwise (Android's own minimum recommended touch target is 48dp).
                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .clickable(actionStartActivity(widgetConfigIntent(context, appWidgetId))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_settings),
                                contentDescription = null,
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Text(
                        // headlineLarge: 28sp, Normal weight (DeviceInfoTypography).
                        text = deviceInfo.displayName,
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, color = GlanceTheme.colors.onPrimaryContainer),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_android),
                            contentDescription = null,
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            // bodyLarge: 16sp, Normal weight.
                            text = androidVersionText,
                            style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onPrimaryContainer),
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(20.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeroGauge(
                            ringImage = batteryRing,
                            ringSizeDp = RING_SIZE_DP,
                            percentText = "${battery.percent}%",
                            caption = batteryLabel,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        if (storageGauge != null) {
                            val (fraction, ring) = storageGauge
                            WidgetHeroGauge(
                                ringImage = ring,
                                ringSizeDp = RING_SIZE_DP,
                                percentText = "${(fraction * 100).roundToInt()}%",
                                caption = storageLabel,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}
