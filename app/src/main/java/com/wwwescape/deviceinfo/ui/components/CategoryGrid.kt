package com.wwwescape.deviceinfo.ui.components

import com.wwwescape.deviceinfo.ui.tv.dpadScrollable
import com.wwwescape.deviceinfo.ui.tv.LocalIsTv
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.battery.BatteryRepository
import com.wwwescape.deviceinfo.data.camera.CameraRepository
import com.wwwescape.deviceinfo.data.cpu.CpuRepository
import com.wwwescape.deviceinfo.data.deviceos.DeviceOsRepository
import com.wwwescape.deviceinfo.data.display.DisplayRepository
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import com.wwwescape.deviceinfo.data.network.NetworkRepository
import com.wwwescape.deviceinfo.data.sensors.SensorsRepository
import com.wwwescape.deviceinfo.ui.navigation.Destination
import com.wwwescape.deviceinfo.ui.screens.battery.label
import com.wwwescape.deviceinfo.ui.screens.network.label
import com.wwwescape.deviceinfo.util.formatBytes

/**
 * The Dashboard's category-tile grid. [header] renders above the tiles as a full-width item
 * (the hero card); [footer] renders below the tiles as a full-width item (Live Metrics).
 */
@Composable
fun CategoryGrid(
    categories: List<Destination>,
    onCategoryClick: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val deviceOsInfo = remember { DeviceOsRepository.collectStatic(context) }
    val cpuInfo = remember { CpuRepository.collectStatic(context) }
    val ramInfo = remember { MemoryRepository.readRamInfo(context) }
    val batteryInfo = remember { BatteryRepository.currentBatteryInfo(context) }
    val displayInfo = remember { DisplayRepository.collectStatic(context) }
    val networkSnapshot = remember { NetworkRepository.snapshot(context) }
    val sensorCount = remember { SensorsRepository.listSensors(context).size }
    val cameraCount = remember { CameraRepository.listCameras(context).size }

    // TV: remember which tile opened a detail screen, so pressing Back returns focus to that same
    // tile instead of dropping the user back at the top of the dashboard.
    val gridState = rememberLazyGridState()
    var lastOpenedRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val tileFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    if (LocalIsTv.current && lastOpenedRoute != null) {
        LaunchedEffect(Unit) {
            runCatching { tileFocusRequesters[lastOpenedRoute]?.requestFocus() }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        // Adaptive rather than a fixed count: a fixed 2 columns leaves tiles enormous (mostly
        // empty space) on tablet-width screens. 140.dp keeps 2 columns on phones as narrow as
        // ~344dp while letting wider/tablet screens fill in with more, appropriately-sized columns.
        // TV: wider tiles — titles like "Memory & Storage" otherwise wrap and push the subtitle out.
        columns = GridCells.Adaptive(minSize = if (LocalIsTv.current) 180.dp else 140.dp),
        modifier = modifier.dpadScrollable(gridState, focusOnEnter = lastOpenedRoute == null),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        items(categories, key = { it.route }) { destination ->
            val tileFocusRequester = remember(destination.route) {
                tileFocusRequesters.getOrPut(destination.route) { FocusRequester() }
            }
            val subtitle = when (destination) {
                Destination.DeviceOs ->
                    stringResource(R.string.headline_device_os, deviceOsInfo.displayName, deviceOsInfo.androidVersion)
                Destination.Cpu ->
                    stringResource(R.string.headline_cpu, cpuInfo.socName, cpuInfo.coreCount)
                Destination.Memory ->
                    stringResource(R.string.headline_memory, formatBytes(ramInfo.totalBytes))
                Destination.Battery -> if (batteryInfo.isPresent) {
                    stringResource(R.string.headline_battery, batteryInfo.percent, batteryInfo.status.label())
                } else {
                    stringResource(R.string.battery_not_present)
                }
                Destination.Display ->
                    stringResource(R.string.headline_display, displayInfo.widthPx, displayInfo.heightPx, displayInfo.refreshRateHz.toInt())
                Destination.Network ->
                    networkSnapshot.connectionType.label()
                Destination.Sensors ->
                    stringResource(R.string.headline_sensors, sensorCount)
                Destination.Camera ->
                    stringResource(R.string.headline_camera, cameraCount)
                else -> stringResource(R.string.subtitle_coming_soon)
            }
            InfoCard(
                title = stringResource(destination.titleRes),
                subtitle = subtitle,
                icon = destination.icon,
                onClick = {
                    lastOpenedRoute = destination.route
                    onCategoryClick(destination)
                },
                modifier = Modifier.focusRequester(tileFocusRequester),
            )
        }
        if (footer != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { footer() }
        }
    }
}
