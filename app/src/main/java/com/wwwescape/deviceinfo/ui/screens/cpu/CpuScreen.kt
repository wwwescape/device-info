package com.wwwescape.deviceinfo.ui.screens.cpu

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.cpu.CpuCoreInfo
import com.wwwescape.deviceinfo.data.cpu.CpuInfo
import com.wwwescape.deviceinfo.data.cpu.CpuLiveStats
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.ProgressStatRow
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.StatTile

@Composable
fun CpuScreen(
    modifier: Modifier = Modifier,
    viewModel: CpuViewModel = viewModel(),
) {
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()

    CpuScreenContent(
        info = viewModel.cpuInfo,
        liveStats = liveStats,
        modifier = modifier,
    )
}

@Composable
private fun CpuScreenContent(
    info: CpuInfo,
    liveStats: CpuLiveStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CpuHero(socName = info.socName, cores = info.cores)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(icon = Icons.Rounded.Architecture, title = stringResource(R.string.section_architecture))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stat_soc), info.socName, modifier = Modifier.weight(1f))
                StatTile(stringResource(R.string.stat_kernel), info.kernelVersion, modifier = Modifier.weight(1f))
            }
            StatTile(
                label = stringResource(R.string.stat_governor),
                value = info.governor ?: stringResource(R.string.stat_not_available),
            )
            StatTile(
                label = stringResource(R.string.stat_instruction_sets),
                value = info.supportedAbis.joinToString(", "),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(icon = Icons.Rounded.Speed, title = stringResource(R.string.section_cores_frequencies))
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_live).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    info.cores.forEach { core ->
                        val currentMhz = liveStats.coreFrequenciesMhz.getOrNull(core.index)
                        val maxMhz = core.maxFreqMhz
                        val fraction = if (currentMhz != null && maxMhz != null && maxMhz > 0) {
                            (currentMhz.toFloat() / maxMhz.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        ProgressStatRow(
                            label = stringResource(R.string.stat_core_n, core.index),
                            value = formatCoreFrequency(currentMhz, maxMhz),
                            progress = fraction,
                        )
                    }
                }
            }
        }

        DetailSection(
            icon = Icons.Rounded.Terminal,
            title = stringResource(R.string.section_os_internals),
            rows = listOf(
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_abi),
                        value = info.primaryAbi,
                        icon = Icons.Rounded.Code,
                    )
                },
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_vulkan_support),
                        value = "",
                        icon = Icons.Rounded.Memory,
                        trailingIcon = if (info.hasVulkanSupport) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Cancel,
                    )
                },
            ),
        )
    }
}

@Composable
private fun CpuHero(socName: String, cores: List<CpuCoreInfo>) {
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
            Text(
                text = stringResource(R.string.hero_main_processor).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = socName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            Box(contentAlignment = Alignment.Center) {
                CoreWedgeRing(cores = cores, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${cores.size}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.stat_cores).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** Alpha applied to each core's wedge, ordered from the fastest cluster (index 0) to the slowest. */
private val ClusterAlphaSteps = listOf(1f, 0.7f, 0.4f)

@Composable
private fun CoreWedgeRing(cores: List<CpuCoreInfo>, color: Color) {
    val alphas = coreWedgeAlphas(cores)
    Canvas(modifier = Modifier.size(140.dp)) {
        val strokeWidthPx = 10.dp.toPx()
        val gapDegrees = if (cores.size > 1) 3f else 0f
        val sweepDegrees = 360f / cores.size - gapDegrees
        val diameter = size.minDimension - strokeWidthPx
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f
        cores.indices.forEach { index ->
            drawArc(
                color = color.copy(alpha = alphas[index]),
                startAngle = startAngle,
                sweepAngle = sweepDegrees,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
            )
            startAngle += sweepDegrees + gapDegrees
        }
    }
}

/** Clusters cores by matching maxFreqMhz (Android exposes no per-core microarch name) and shades each by cluster speed. */
private fun coreWedgeAlphas(cores: List<CpuCoreInfo>): List<Float> {
    val clusterFreqsDescending = cores.map { it.maxFreqMhz }.distinct().sortedByDescending { it ?: -1 }
    if (clusterFreqsDescending.size <= 1) return cores.map { 1f }
    return cores.map { core ->
        val rank = clusterFreqsDescending.indexOf(core.maxFreqMhz).coerceAtMost(ClusterAlphaSteps.lastIndex)
        ClusterAlphaSteps[rank]
    }
}

private fun formatGhz(mhz: Int?): String =
    if (mhz == null) "—" else "%.2f GHz".format(mhz / 1000f)

private fun formatCoreFrequency(currentMhz: Int?, maxMhz: Int?): String =
    "${formatGhz(currentMhz)} / ${formatGhz(maxMhz)}"
