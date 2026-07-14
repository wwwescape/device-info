package com.wwwescape.deviceinfo.ui.screens.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.memory.MemoryBreakdown
import com.wwwescape.deviceinfo.data.memory.MemoryLiveStats
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import com.wwwescape.deviceinfo.data.memory.RamInfo
import com.wwwescape.deviceinfo.data.memory.StorageHardwareInfo
import com.wwwescape.deviceinfo.data.memory.StorageVolumeInfo
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.StatTile
import com.wwwescape.deviceinfo.util.formatBytes
import kotlin.math.roundToInt

@Composable
fun MemoryScreen(
    modifier: Modifier = Modifier,
    viewModel: MemoryViewModel = viewModel(),
) {
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
    val breakdown = remember { MemoryRepository.readMemoryBreakdown() }
    val hardwareInfo = remember { MemoryRepository.readStorageHardwareInfo() }

    MemoryScreenContent(
        liveStats = liveStats,
        breakdown = breakdown,
        hardwareInfo = hardwareInfo,
        modifier = modifier,
    )
}

@Composable
private fun MemoryScreenContent(
    liveStats: MemoryLiveStats,
    breakdown: MemoryBreakdown?,
    hardwareInfo: StorageHardwareInfo?,
    modifier: Modifier = Modifier,
) {
    val internalStorage = liveStats.storageVolumes.firstOrNull { !it.isRemovable }
    val removableStorage = liveStats.storageVolumes.firstOrNull { it.isRemovable }
    val notAvailable = stringResource(R.string.stat_not_available)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MemoryHero(ram = liveStats.ram, internalStorage = internalStorage)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(icon = Icons.Rounded.Layers, title = stringResource(R.string.section_memory_breakdown))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.stat_active),
                    breakdown?.activeBytes?.let { formatBytes(it) } ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    stringResource(R.string.stat_cached),
                    breakdown?.cachedBytes?.let { formatBytes(it) } ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    stringResource(R.string.stat_swap),
                    breakdown?.swapUsedBytes?.let { formatBytes(it) } ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    stringResource(R.string.stat_buffers),
                    breakdown?.buffersBytes?.let { formatBytes(it) } ?: notAvailable,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (internalStorage != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeader(icon = Icons.Rounded.Storage, title = stringResource(R.string.section_storage_allocation))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        stringResource(R.string.stat_used),
                        formatBytes(internalStorage.usedBytes),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stat_available),
                        formatBytes(internalStorage.freeBytes),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        DetailSection(
            icon = Icons.Rounded.Dns,
            title = stringResource(R.string.section_hardware_info),
            rows = listOf(
                { DetailStatRow(stringResource(R.string.stat_device_node), hardwareInfo?.deviceNode ?: notAvailable) },
                { DetailStatRow(stringResource(R.string.stat_file_system), hardwareInfo?.fileSystem?.uppercase() ?: notAvailable) },
            ),
        )

        if (removableStorage != null) {
            DetailSection(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.section_removable_storage),
                rows = listOf(
                    { DetailStatRow(stringResource(R.string.stat_used), formatBytes(removableStorage.usedBytes)) },
                    { DetailStatRow(stringResource(R.string.stat_available), formatBytes(removableStorage.freeBytes)) },
                ),
            )
        }
    }
}

@Composable
private fun MemoryHero(ram: RamInfo, internalStorage: StorageVolumeInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val ramFraction = if (ram.totalBytes > 0) (ram.usedBytes.toFloat() / ram.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
            HeroUsageRow(
                label = stringResource(R.string.section_ram),
                value = "${formatBytes(ram.usedBytes)} / ${formatBytes(ram.totalBytes)}",
                fraction = ramFraction,
            )
            if (internalStorage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val storageFraction = if (internalStorage.totalBytes > 0) {
                    (internalStorage.usedBytes.toFloat() / internalStorage.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                HeroUsageRow(
                    label = stringResource(R.string.section_internal_storage),
                    value = "${formatBytes(internalStorage.usedBytes)} / ${formatBytes(internalStorage.totalBytes)}",
                    fraction = storageFraction,
                )
            }
        }
    }
}

@Composable
private fun HeroUsageRow(label: String, value: String, fraction: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
        )
        Text(
            text = stringResource(R.string.stat_utilized_format, (fraction * 100).roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
