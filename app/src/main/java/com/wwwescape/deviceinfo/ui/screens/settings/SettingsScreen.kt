package com.wwwescape.deviceinfo.ui.screens.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.settings.AppSettings
import com.wwwescape.deviceinfo.data.settings.ColorTheme
import com.wwwescape.deviceinfo.data.settings.RefreshInterval
import com.wwwescape.deviceinfo.data.settings.TemperatureUnit
import com.wwwescape.deviceinfo.data.settings.ThemeContrast
import com.wwwescape.deviceinfo.data.settings.ThemeMode
import com.wwwescape.deviceinfo.util.openUrl

/** Fixed row height shared by every individual settings row (toggle, navigation, and picker
 * rows alike), so rows with a subtitle and rows without one line up identically — a plain
 * `heightIn(min = ...)` lets two-line rows grow taller than one-line rows. */
private val SettingsRowHeight = 72.dp

private const val PRIVACY_POLICY_URL = "https://www.ericppereira.co.in/apps/device-info/privacy-policy.html"

@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreenContent(
        settings = settings,
        onThemeModeSelected = viewModel::setThemeMode,
        onDynamicColorChanged = viewModel::setDynamicColor,
        onColorThemeSelected = viewModel::setColorTheme,
        onThemeContrastSelected = viewModel::setThemeContrast,
        onPureDarkChanged = viewModel::setPureDark,
        onAbsoluteDarkChanged = viewModel::setAbsoluteDark,
        onTemperatureUnitSelected = viewModel::setTemperatureUnit,
        onRefreshIntervalSelected = viewModel::setRefreshInterval,
        onNavigateToLicenses = onNavigateToLicenses,
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    settings: AppSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onColorThemeSelected: (ColorTheme) -> Unit,
    onThemeContrastSelected: (ThemeContrast) -> Unit,
    onPureDarkChanged: (Boolean) -> Unit,
    onAbsoluteDarkChanged: (Boolean) -> Unit,
    onTemperatureUnitSelected: (TemperatureUnit) -> Unit,
    onRefreshIntervalSelected: (RefreshInterval) -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showContrastDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel(stringResource(R.string.section_appearance))
        SettingsGroupCard {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.setting_dynamic_color),
                    subtitle = stringResource(R.string.setting_dynamic_color_subtitle),
                    checked = settings.useDynamicColor,
                    onCheckedChange = onDynamicColorChanged,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
            NavigationRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.setting_color_theme),
                subtitle = settings.colorTheme.label(),
                onClick = { showColorThemeDialog = true },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NavigationRow(
                icon = Icons.Rounded.DarkMode,
                title = stringResource(R.string.setting_theme),
                subtitle = settings.themeMode.label(),
                onClick = { showThemeDialog = true },
            )
            // Contrast/pure dark/absolute dark all only make sense for dark theme — hidden
            // entirely rather than shown-but-inert when the effective theme (accounting for
            // "System default" actually resolving to light) is light.
            if (isDarkTheme) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                NavigationRow(
                    icon = Icons.Rounded.Contrast,
                    title = stringResource(R.string.setting_theme_contrast),
                    subtitle = settings.themeContrast.label(),
                    onClick = { showContrastDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow(
                    icon = Icons.Rounded.Brightness6,
                    title = stringResource(R.string.setting_pure_dark),
                    subtitle = stringResource(R.string.setting_pure_dark_subtitle),
                    checked = settings.pureDark,
                    onCheckedChange = onPureDarkChanged,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow(
                    icon = Icons.Rounded.NightsStay,
                    title = stringResource(R.string.setting_absolute_dark),
                    subtitle = stringResource(R.string.setting_absolute_dark_subtitle),
                    checked = settings.absoluteDark,
                    onCheckedChange = onAbsoluteDarkChanged,
                )
            }
        }

        SectionLabel(stringResource(R.string.section_general))
        SettingsGroupCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SettingsRowHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(Icons.Rounded.Thermostat)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.setting_temperature_unit), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = settings.temperatureUnit.label(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SegmentedOptionRow(
                    options = TemperatureUnit.entries,
                    selected = settings.temperatureUnit,
                    label = { it.shortLabel() },
                    onSelect = onTemperatureUnitSelected,
                    modifier = Modifier.width(112.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NavigationRow(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.setting_refresh_interval),
                subtitle = settings.refreshInterval.label(),
                onClick = { showRefreshDialog = true },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NavigationRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.setting_language),
                subtitle = appLanguageLabel(currentLanguageTag),
                onClick = { showLanguageDialog = true },
            )
        }

        SectionLabel(stringResource(R.string.section_about))
        SettingsGroupCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Text(
                text = stringResource(R.string.about_app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NavigationRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.section_privacy_policy),
                subtitle = null,
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = { openUrl(context, PRIVACY_POLICY_URL) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NavigationRow(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.section_open_source_licenses),
                subtitle = stringResource(R.string.settings_row_licenses_subtitle),
                onClick = onNavigateToLicenses,
            )
        }
    }

    if (showThemeDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label() },
            onSelect = onThemeModeSelected,
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showColorThemeDialog) {
        ThemePickerDialog(
            selected = settings.colorTheme,
            onSelect = onColorThemeSelected,
            onDismiss = { showColorThemeDialog = false },
        )
    }
    if (showContrastDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_theme_contrast),
            options = ThemeContrast.entries,
            selected = settings.themeContrast,
            label = { it.label() },
            onSelect = onThemeContrastSelected,
            onDismiss = { showContrastDialog = false },
        )
    }
    if (showRefreshDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_refresh_interval),
            options = RefreshInterval.entries,
            selected = settings.refreshInterval,
            label = { it.label() },
            onSelect = onRefreshIntervalSelected,
            onDismiss = { showRefreshDialog = false },
        )
    }
    if (showLanguageDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_language),
            options = SUPPORTED_APP_LANGUAGES,
            selected = currentLanguageTag,
            label = { appLanguageLabel(it) },
            onSelect = { tag ->
                AppCompatDelegate.setApplicationLocales(
                    if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                )
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

/** A grouped-settings card matching [com.wwwescape.deviceinfo.ui.components.StatCard]'s visual
 * treatment, but without a built-in title — this screen's section labels render outside the
 * card via [SectionLabel] instead. */
@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Rounded.ChevronRight,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(SettingsRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            RowIcon(icon)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A compact single-choice row for a short, mutually-exclusive enum of options. */
@Composable
private fun <T> SegmentedOptionRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label(option)) },
            )
        }
    }
}
