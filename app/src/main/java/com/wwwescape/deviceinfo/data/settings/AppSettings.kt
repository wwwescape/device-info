package com.wwwescape.deviceinfo.data.settings

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Curated seed hues for the manual "Color Theme" picker — null (DEFAULT) keeps the app's
 * hand-authored static palette; every other entry generates a full scheme from that hue via
 * [com.wwwescape.deviceinfo.ui.theme.generateColorScheme]. Ignored when dynamic color is on. */
enum class ColorTheme(val seedHue: Float?) {
    DEFAULT(null),
    OCEAN(205f),
    FOREST(140f),
    SUNSET(25f),
    GRAPE(280f),
    ROSE(340f),
    SLATE(220f),
    GOLD(45f),
    TEAL(175f),
    PLUM(300f),
    MOSS(95f),
    CORAL(12f),
    INDIGO(245f),
    MUSTARD(55f),
    CRIMSON(355f),
    MINT(160f),
    PERIWINKLE(230f),
}

/** STANDARD leaves the resolved color scheme untouched; MEDIUM/HIGH push text/outline colors
 * further from their surface for stronger contrast. Applies in both light and dark theme, but
 * is only surfaced in Settings while dark theme is active — see SettingsScreen. */
enum class ThemeContrast { STANDARD, MEDIUM, HIGH }

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/** Polling cadence for live-updating screens (CPU load/frequency, RAM/storage). */
enum class RefreshInterval(val cpuMillis: Long, val memoryMillis: Long) {
    BATTERY_SAVER(3_000L, 5_000L),
    NORMAL(1_500L, 2_000L),
    FAST(500L, 1_000L),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val pureDark: Boolean = false,
    val absoluteDark: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val refreshInterval: RefreshInterval = RefreshInterval.NORMAL,
)
