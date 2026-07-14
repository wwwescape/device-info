package com.wwwescape.deviceinfo.ui.components

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.deviceos.DeviceOsRepository
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import com.wwwescape.deviceinfo.util.formatBytes
import com.wwwescape.deviceinfo.util.formatUptime

/** The Dashboard's Live Metrics section: real-time Memory Usage and System Uptime rows. */
@Composable
fun LiveMetricsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ramInfo = remember { MemoryRepository.readRamInfo(context) }
    val uptimeMillis by remember { DeviceOsRepository.uptimeMillis() }
        .collectAsStateWithLifecycle(initialValue = SystemClock.elapsedRealtime())

    Column(modifier = modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.section_live_metrics),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        MetricRow(
            title = stringResource(R.string.metric_memory_usage),
            value = "${formatBytes(ramInfo.usedBytes)} / ${formatBytes(ramInfo.totalBytes)}",
            progress = (ramInfo.usedBytes.toFloat() / ramInfo.totalBytes.toFloat()).coerceIn(0f, 1f),
        )
        MetricRow(
            title = stringResource(R.string.metric_system_uptime),
            value = formatUptime(uptimeMillis),
            progress = 1f,
        )
    }
}

@Composable
private fun MetricRow(title: String, value: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}
