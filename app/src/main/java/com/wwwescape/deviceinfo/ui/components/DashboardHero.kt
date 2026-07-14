package com.wwwescape.deviceinfo.ui.components

import com.wwwescape.deviceinfo.data.memory.StorageVolumeInfo
import com.wwwescape.deviceinfo.data.battery.BatteryInfo
import androidx.compose.foundation.layout.BoxWithConstraints
import com.wwwescape.deviceinfo.ui.tv.focusHighlight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.battery.BatteryRepository
import com.wwwescape.deviceinfo.data.deviceos.DeviceOsRepository
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import kotlin.math.roundToInt

/**
 * The Dashboard's hero card: device identity plus live Battery/Storage gauges. Replaces the
 * mockup's decorative phone illustration with real, live data — nothing here is a placeholder.
 */
@Composable
fun DashboardHero(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val deviceOsInfo = remember { DeviceOsRepository.collectStatic(context) }
    val batteryInfo = remember { BatteryRepository.currentBatteryInfo(context) }
    val internalStorage = remember { MemoryRepository.readStorageVolumes(context).firstOrNull() }

    Card(
        modifier = modifier
            .focusHighlight(MaterialTheme.shapes.extraLarge, makeFocusable = true)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        // Wide screens (TV, tablet landscape): identity and gauges side by side, so the hero stays
        // short and the category tiles are visible without scrolling. Phones keep the stacked layout.
        BoxWithConstraints(modifier = Modifier.padding(20.dp)) {
            if (maxWidth >= WideHeroMinWidth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        HeroIdentity(deviceOsInfo.displayName, deviceOsInfo.androidVersion)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        HeroGauges(batteryInfo, internalStorage)
                    }
                }
            } else {
                Column {
                    HeroIdentity(deviceOsInfo.displayName, deviceOsInfo.androidVersion)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row { HeroGauges(batteryInfo, internalStorage) }
                }
            }
        }
    }
}

private val WideHeroMinWidth = 600.dp

@Composable
private fun HeroIdentity(displayName: String, androidVersion: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
    ) {
        Text(
            text = stringResource(R.string.hero_device_overview).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
    Text(
        text = displayName,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(top = 12.dp),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.hero_os_version, androidVersion),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun RowScope.HeroGauges(batteryInfo: BatteryInfo, internalStorage: StorageVolumeInfo?) {
    // TVs and other mains-powered devices have no battery — don't show a fake 100%.
    if (batteryInfo.isPresent) {
        HeroGauge(
            progress = batteryInfo.percent / 100f,
            caption = stringResource(R.string.section_battery),
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
    if (internalStorage != null) {
        val storageFraction =
            (internalStorage.usedBytes.toFloat() / internalStorage.totalBytes.toFloat()).coerceIn(0f, 1f)
        HeroGauge(
            progress = storageFraction,
            caption = stringResource(R.string.widget_storage_label),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RowScope.HeroGauge(
    progress: Float,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(percent = 50)),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = caption.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
