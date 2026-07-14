package com.wwwescape.deviceinfo.ui.screens.network

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.network.BluetoothState
import com.wwwescape.deviceinfo.data.network.CellularState
import com.wwwescape.deviceinfo.data.network.ConnectionType
import com.wwwescape.deviceinfo.data.network.NetworkRepository
import com.wwwescape.deviceinfo.data.network.NetworkSnapshot
import com.wwwescape.deviceinfo.data.network.NetworkThroughput
import com.wwwescape.deviceinfo.data.network.WifiState
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.SparklineChart

private val runtimePermissions: List<String> = buildList {
    // Requested together so the system can offer its precise/approximate location dialog;
    // NetworkRepository.hasLocationPermission() only checks FINE, since that's what SSID needs.
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
}

@Composable
fun NetworkScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val throughput by viewModel.throughput.collectAsStateWithLifecycle()
    val throughputHistory by viewModel.throughputHistory.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    val hasAllPermissions = remember(snapshot) {
        NetworkRepository.hasLocationPermission(context) &&
            NetworkRepository.hasPhoneStatePermission(context) &&
            NetworkRepository.hasBluetoothConnectPermission(context)
    }

    NetworkScreenContent(
        snapshot = snapshot,
        throughput = throughput,
        throughputHistory = throughputHistory,
        hasAllPermissions = hasAllPermissions,
        onRequestPermissions = { permissionLauncher.launch(runtimePermissions.toTypedArray()) },
        modifier = modifier,
    )
}

@Composable
private fun NetworkScreenContent(
    snapshot: NetworkSnapshot,
    throughput: NetworkThroughput,
    throughputHistory: List<NetworkThroughput>,
    hasAllPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!hasAllPermissions) {
            PermissionRationaleCard(onRequestPermissions = onRequestPermissions)
        }

        NetworkHero(snapshot = snapshot)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(icon = Icons.Rounded.BarChart, title = stringResource(R.string.section_network_traffic))
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.stat_download, formatMbps(throughput.downMbps)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.stat_upload, formatMbps(throughput.upMbps)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val combined = throughputHistory.map { it.downMbps + it.upMbps }
                    if (combined.size >= 2) {
                        val max = combined.max().coerceAtLeast(0.1f)
                        SparklineChart(
                            values = combined.map { (it / max).coerceIn(0f, 1f) },
                            contentDescription = stringResource(R.string.network_traffic_content_description),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        WifiSection(snapshot.wifi)
        CellularSection(snapshot.cellularSims)

        DetailSection(
            icon = Icons.Rounded.Router,
            title = stringResource(R.string.section_technical_details),
            rows = listOf(
                { DetailStatRow(stringResource(R.string.stat_ip_address), snapshot.ipAddress ?: stringResource(R.string.stat_unavailable)) },
                { DetailStatRow(stringResource(R.string.stat_ipv6_address), snapshot.ipv6Address ?: stringResource(R.string.stat_unavailable)) },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_dns_servers),
                        snapshot.dnsServers.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: stringResource(R.string.stat_unavailable),
                    )
                },
            ),
        )

        BluetoothSection(snapshot.bluetooth)

        DetailSection(
            icon = Icons.Rounded.Nfc,
            title = stringResource(R.string.section_other_radios),
            rows = listOf(
                {
                    DetailStatRow(
                        stringResource(R.string.stat_nfc),
                        when {
                            !snapshot.hasNfc -> stringResource(R.string.stat_not_supported)
                            snapshot.isNfcEnabled -> stringResource(R.string.stat_enabled)
                            else -> stringResource(R.string.stat_disabled)
                        },
                    )
                },
            ),
        )
    }
}

@Composable
private fun NetworkHero(snapshot: NetworkSnapshot) {
    val activeCellular = snapshot.cellularSims.firstOrNull { it.isAvailable }
    val level = when (snapshot.connectionType) {
        ConnectionType.WIFI -> snapshot.wifi.signalLevel
        ConnectionType.CELLULAR -> activeCellular?.signalLevel
        else -> null
    }
    val dbm = when (snapshot.connectionType) {
        ConnectionType.WIFI -> snapshot.wifi.rssiDbm
        ConnectionType.CELLULAR -> activeCellular?.signalDbm
        else -> null
    }

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
                    progress = { (level?.let { it / 4f } ?: 0f) },
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(percent = 50)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dbm?.let { "$it" } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.stat_dbm_unit).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Text(
                text = snapshot.connectionType.label(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = connectionQualityLabel(level),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun connectionQualityLabel(level: Int?): String = when (level) {
    null -> stringResource(R.string.stat_not_connected)
    4 -> stringResource(R.string.connection_quality_excellent)
    3 -> stringResource(R.string.connection_quality_good)
    2 -> stringResource(R.string.connection_quality_fair)
    else -> stringResource(R.string.connection_quality_poor)
}

@Composable
private fun WifiSection(wifi: WifiState) {
    if (!wifi.isConnected) {
        DetailSection(
            icon = Icons.Rounded.Wifi,
            title = stringResource(R.string.section_wifi),
            rows = listOf({ DetailStatRow(stringResource(R.string.stat_status), stringResource(R.string.stat_not_connected)) }),
        )
        return
    }
    DetailSection(
        icon = Icons.Rounded.Wifi,
        title = stringResource(R.string.section_wifi),
        rows = listOf(
            { DetailStatRow(stringResource(R.string.stat_ssid), wifi.ssid ?: stringResource(R.string.stat_permission_required)) },
            { DetailStatRow(stringResource(R.string.stat_link_speed), "${wifi.linkSpeedMbps ?: 0} Mbps") },
            { DetailStatRow(stringResource(R.string.stat_frequency), "${wifi.frequencyMhz ?: 0} MHz") },
            { DetailStatRow(stringResource(R.string.stat_wifi_protocol), wifi.standard ?: stringResource(R.string.stat_unavailable)) },
            {
                DetailStatRow(
                    stringResource(R.string.stat_signal_dbm),
                    wifi.rssiDbm?.let { stringResource(R.string.stat_dbm_format, it) } ?: stringResource(R.string.stat_unavailable),
                )
            },
        ),
    )
}

@Composable
private fun CellularSection(cellularSims: List<CellularState>) {
    if (cellularSims.none { it.isAvailable }) {
        DetailSection(
            icon = Icons.Rounded.SignalCellularAlt,
            title = stringResource(R.string.section_cellular),
            rows = listOf({ DetailStatRow(stringResource(R.string.stat_status), stringResource(R.string.stat_not_supported)) }),
        )
        return
    }
    val showSlotLabels = cellularSims.size > 1
    cellularSims.forEachIndexed { index, cellular ->
        val title = if (showSlotLabels) {
            stringResource(R.string.section_cellular_sim_slot, if (cellular.slotIndex >= 0) cellular.slotIndex + 1 else index + 1)
        } else {
            stringResource(R.string.section_cellular)
        }
        DetailSection(
            icon = Icons.Rounded.SignalCellularAlt,
            title = title,
            rows = listOf(
                { DetailStatRow(stringResource(R.string.stat_carrier), cellular.carrierName ?: stringResource(R.string.stat_unavailable)) },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_network_type),
                        cellular.networkTypeName ?: stringResource(R.string.stat_permission_required),
                    )
                },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_sim_status),
                        cellular.simState ?: stringResource(R.string.stat_unavailable),
                        icon = Icons.Rounded.SimCard,
                    )
                },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_roaming),
                        if (cellular.isRoaming == true) stringResource(R.string.stat_yes) else stringResource(R.string.stat_no),
                    )
                },
            ),
        )
    }
}

@Composable
private fun BluetoothSection(bluetooth: BluetoothState) {
    if (!bluetooth.isSupported) {
        DetailSection(
            icon = Icons.Rounded.Bluetooth,
            title = stringResource(R.string.section_bluetooth),
            rows = listOf({ DetailStatRow(stringResource(R.string.stat_status), stringResource(R.string.stat_not_supported)) }),
        )
        return
    }
    val pairedValue = when {
        !bluetooth.isEnabled -> stringResource(R.string.stat_not_available)
        bluetooth.pairedDeviceNames.isEmpty() -> stringResource(R.string.stat_none)
        else -> bluetooth.pairedDeviceNames.joinToString(", ")
    }
    DetailSection(
        icon = Icons.Rounded.Bluetooth,
        title = stringResource(R.string.section_bluetooth),
        rows = listOf(
            {
                DetailStatRow(
                    stringResource(R.string.stat_adapter_state),
                    if (bluetooth.isEnabled) stringResource(R.string.stat_enabled) else stringResource(R.string.stat_disabled),
                )
            },
            { DetailStatRow(stringResource(R.string.stat_paired_devices), pairedValue) },
        ),
    )
}

@Composable
private fun PermissionRationaleCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permission_rationale_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.permission_rationale_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(onClick = onRequestPermissions) {
                Text(stringResource(R.string.permission_rationale_action))
            }
        }
    }
}

private fun formatMbps(mbps: Float): String = "%.1f".format(mbps)
