package com.wwwescape.deviceinfo.ui.screens.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.ScatterPlot
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.sensors.SensorInfo
import com.wwwescape.deviceinfo.data.sensors.SensorReading
import com.wwwescape.deviceinfo.data.sensors.SensorsRepository
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.EmptyStateNotice
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.SparklineChart
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun SensorsScreen(
    modifier: Modifier = Modifier,
    viewModel: SensorsViewModel = viewModel(),
) {
    var expandedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    if (viewModel.sensors.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateNotice(
                text = stringResource(R.string.sensors_empty),
                icon = Icons.Default.SensorsOff,
            )
        }
        return
    }

    val accelReading by viewModel.accelReading.collectAsStateWithLifecycle()
    val accelHistory by viewModel.accelHistory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SensorsHero(
                accelerometerInfo = viewModel.accelerometerInfo,
                reading = accelReading,
                history = accelHistory,
            )
        }
        item {
            SectionHeader(
                icon = Icons.Rounded.Sensors,
                title = stringResource(R.string.sensors_count_format, viewModel.sensors.size),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        itemsIndexed(viewModel.sensors, key = { index, info -> "${info.name}_${info.vendor}_$index" }) { index, info ->
            SensorCard(
                info = info,
                isExpanded = expandedIndex == index,
                onClick = { expandedIndex = if (expandedIndex == index) null else index },
            )
        }
    }
}

@Composable
private fun SensorsHero(
    accelerometerInfo: SensorInfo?,
    reading: SensorReading?,
    history: List<Float>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hero_primary_sensor).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                if (accelerometerInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.label_live).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = accelerometerInfo?.name ?: stringResource(R.string.stat_not_available),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (accelerometerInfo != null) {
                if (history.size >= 2) {
                    val max = history.max().coerceAtLeast(0.1f)
                    SparklineChart(
                        values = history.map { (it / max).coerceIn(0f, 1f) },
                        contentDescription = stringResource(R.string.accel_history_content_description),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AxisValue(stringResource(R.string.sensor_axis_x), reading?.values?.getOrNull(0))
                    AxisValue(stringResource(R.string.sensor_axis_y), reading?.values?.getOrNull(1))
                    AxisValue(stringResource(R.string.sensor_axis_z), reading?.values?.getOrNull(2))
                }
            }
        }
    }
}

@Composable
private fun AxisValue(label: String, value: Float?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value?.let { "%.3f".format(it) } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun SensorCard(
    info: SensorInfo,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "sensorChevronRotation",
    )

    val readingsFlow = remember(info.sensor, isExpanded) {
        if (isExpanded) SensorsRepository.readings(context, info.sensor) else emptyFlow()
    }
    val reading by readingsFlow.collectAsStateWithLifecycle(initialValue = null)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = sensorIcon(info.sensor.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = info.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = info.unit?.let { "${info.typeName} • $it" } ?: info.typeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                    if (isExpanded) {
                        reading?.let { r ->
                            accuracyLabelRes(r.accuracy)?.let { labelRes ->
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                ) {
                                    Text(
                                        text = stringResource(labelRes).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = info.maxRateHz?.let { "$it Hz" } ?: stringResource(R.string.rate_on_change),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.action_collapse else R.string.action_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val unitSuffix = info.unit?.let { " $it" }.orEmpty()
                    DetailStatRow(stringResource(R.string.stat_vendor), info.vendor)
                    DetailStatRow(stringResource(R.string.stat_range), "±${info.maximumRange}$unitSuffix")
                    DetailStatRow(stringResource(R.string.stat_resolution), "${info.resolution}$unitSuffix")
                    DetailStatRow(stringResource(R.string.stat_power), "${info.power} mA")
                    DetailStatRow(stringResource(R.string.stat_min_delay), "${info.minDelayMicros} µs")

                    Text(
                        text = stringResource(R.string.section_live_values),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    LiveValues(reading, info.unit)
                }
            }
        }
    }
}

@Composable
private fun LiveValues(reading: SensorReading?, unit: String?) {
    if (reading == null) {
        Text(
            text = stringResource(R.string.sensor_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val unitSuffix = unit?.let { " $it" }.orEmpty()
    val labels = when (reading.values.size) {
        1 -> listOf(stringResource(R.string.sensor_value_single))
        3 -> listOf(
            stringResource(R.string.sensor_axis_x),
            stringResource(R.string.sensor_axis_y),
            stringResource(R.string.sensor_axis_z),
        )
        else -> reading.values.indices.map { stringResource(R.string.sensor_value_n, it + 1) }
    }
    labels.forEachIndexed { index, label ->
        DetailStatRow(label, "%.4f%s".format(reading.values.getOrElse(index) { 0f }, unitSuffix))
    }
}

private fun accuracyLabelRes(accuracy: Int): Int? = when (accuracy) {
    SensorManager.SENSOR_STATUS_UNRELIABLE -> R.string.accuracy_unreliable
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.accuracy_low
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.accuracy_medium
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.accuracy_high
    else -> null
}

private fun sensorIcon(type: Int): ImageVector = when (type) {
    Sensor.TYPE_ACCELEROMETER -> Icons.Rounded.Speed
    Sensor.TYPE_LINEAR_ACCELERATION -> Icons.Rounded.Vibration
    Sensor.TYPE_GRAVITY -> Icons.Rounded.Height
    Sensor.TYPE_GYROSCOPE -> Icons.AutoMirrored.Rounded.RotateRight
    Sensor.TYPE_MAGNETIC_FIELD -> Icons.Rounded.Explore
    Sensor.TYPE_LIGHT -> Icons.Rounded.WbSunny
    Sensor.TYPE_PRESSURE -> Icons.Rounded.Compress
    Sensor.TYPE_PROXIMITY -> Icons.Rounded.Waves
    Sensor.TYPE_RELATIVE_HUMIDITY -> Icons.Rounded.WaterDrop
    Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_TEMPERATURE -> Icons.Rounded.Thermostat
    Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR -> Icons.AutoMirrored.Rounded.DirectionsWalk
    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> Icons.Rounded.ScatterPlot
    else -> Icons.Rounded.Sensors
}
