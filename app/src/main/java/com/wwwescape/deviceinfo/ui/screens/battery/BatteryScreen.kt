package com.wwwescape.deviceinfo.ui.screens.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.battery.BatteryInfo
import com.wwwescape.deviceinfo.data.battery.ThermalInfo
import com.wwwescape.deviceinfo.data.settings.TemperatureUnit
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.ProgressStatRow
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.StatTile
import com.wwwescape.deviceinfo.ui.screens.settings.SettingsViewModel
import com.wwwescape.deviceinfo.util.formatTemperature
import kotlin.math.abs

/** Fixed visual ceiling for the Thermals progress bars — device/battery temps don't have a
 * natural "max", so this is just a sensible scale for the bar, not a real threshold. */
private const val ThermalScaleCeilingCelsius = 60f

@Composable
fun BatteryScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val thermalInfo by viewModel.thermalInfo.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    BatteryScreenContent(
        battery = batteryInfo,
        thermal = thermalInfo,
        temperatureUnit = settings.temperatureUnit,
        modifier = modifier,
    )
}

@Composable
private fun BatteryScreenContent(
    battery: BatteryInfo,
    thermal: ThermalInfo,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val notAvailable = stringResource(R.string.stat_not_available)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BatteryHero(battery = battery)

        DetailSection(
            icon = Icons.Rounded.MonitorHeart,
            title = stringResource(R.string.section_battery_health),
            rows = listOf(
                { DetailStatRow(stringResource(R.string.stat_health), battery.health.label()) },
                { DetailStatRow(stringResource(R.string.stat_thermal_status), thermal.status.label()) },
                { DetailStatRow(stringResource(R.string.stat_voltage), "%.2f V".format(battery.voltageMillivolts / 1000f)) },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_design_capacity),
                        battery.designCapacityMah?.let { "$it mAh" } ?: notAvailable,
                    )
                },
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(icon = Icons.Rounded.Bolt, title = stringResource(R.string.section_power))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.stat_power_source),
                    battery.plugSource.label(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    stringResource(R.string.stat_power_level),
                    formatWatts(battery.voltageMillivolts, battery.currentMicroAmps) ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.stat_status),
                    battery.status.label(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    stringResource(R.string.stat_current),
                    formatAmps(battery.currentMicroAmps) ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(icon = Icons.Rounded.Thermostat, title = stringResource(R.string.section_thermals))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgressStatRow(
                        label = stringResource(R.string.stat_soc_temp),
                        value = battery.socTemperatureCelsius?.let { formatTemperature(it, temperatureUnit) } ?: notAvailable,
                        progress = thermalFraction(battery.socTemperatureCelsius),
                    )
                    ProgressStatRow(
                        label = stringResource(R.string.stat_temperature),
                        value = formatTemperature(battery.temperatureCelsius, temperatureUnit),
                        progress = thermalFraction(battery.temperatureCelsius),
                    )
                    ProgressStatRow(
                        label = stringResource(R.string.stat_skin_temp),
                        value = battery.skinTemperatureCelsius?.let { formatTemperature(it, temperatureUnit) } ?: notAvailable,
                        progress = thermalFraction(battery.skinTemperatureCelsius),
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryHero(battery: BatteryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { battery.percent / 100f },
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(percent = 50)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${battery.percent}%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = battery.status.label().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private fun thermalFraction(celsius: Float?): Float =
    if (celsius == null) 0f else (celsius / ThermalScaleCeilingCelsius).coerceIn(0f, 1f)

private fun formatAmps(currentMicroAmps: Long?): String? =
    currentMicroAmps?.let { "%.2f A".format(abs(it) / 1_000_000f) }

private fun formatWatts(voltageMillivolts: Int, currentMicroAmps: Long?): String? =
    currentMicroAmps?.let { "%.1f W".format(abs((voltageMillivolts / 1000f) * (it / 1_000_000f))) }
