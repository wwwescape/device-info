package com.wwwescape.deviceinfo.ui.screens.deviceos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.deviceos.DeviceOsInfo
import com.wwwescape.deviceinfo.ui.components.DetailHero
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.theme.JetBrainsMonoFamily
import com.wwwescape.deviceinfo.util.formatSecurityPatchDate

@Composable
fun DeviceOsScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceOsViewModel = viewModel(),
) {
    DeviceOsScreenContent(info = viewModel.deviceOsInfo, modifier = modifier)
}

@Composable
private fun DeviceOsScreenContent(
    info: DeviceOsInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DetailHero(
            icon = Icons.Rounded.PhoneAndroid,
            label = stringResource(R.string.hero_hardware_signature),
            title = info.displayName,
        )

        DetailSection(
            icon = Icons.Rounded.PhoneAndroid,
            title = stringResource(R.string.section_device),
            rows = listOfNotNull(
                info.marketingName?.let {
                    { DetailStatRow(stringResource(R.string.stat_marketing_name), it) }
                },
                { DetailStatRow(stringResource(R.string.stat_model), info.model) },
                { DetailStatRow(stringResource(R.string.stat_manufacturer), info.manufacturer) },
                { DetailStatRow(stringResource(R.string.stat_board), info.board) },
                { DetailStatRow(stringResource(R.string.stat_hardware), info.hardware) },
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_android_id),
                        value = truncatedAndroidId(info.androidId),
                        valueFontFamily = JetBrainsMonoFamily,
                    )
                },
            ),
        )

        DetailSection(
            icon = Icons.Rounded.Android,
            title = stringResource(R.string.section_android_os),
            rows = listOf(
                { VersionStatRow(stringResource(R.string.stat_android_version), info.androidVersion) },
                { DetailStatRow(stringResource(R.string.stat_api_level), info.apiLevel.toString()) },
                { DetailStatRow(stringResource(R.string.stat_security_patch), formatSecurityPatchDate(info.securityPatch)) },
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_build_id),
                        value = info.buildId,
                        valueFontFamily = JetBrainsMonoFamily,
                    )
                },
            ),
        )

        DetailSection(
            icon = Icons.Rounded.DeveloperBoard,
            title = stringResource(R.string.section_firmware),
            rows = listOf(
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_kernel_version),
                        value = info.kernelVersion,
                        valueFontFamily = JetBrainsMonoFamily,
                    )
                },
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_baseband),
                        value = info.baseband ?: stringResource(R.string.stat_not_available),
                        valueFontFamily = if (info.baseband != null) JetBrainsMonoFamily else null,
                    )
                },
                {
                    DetailStatRow(
                        label = stringResource(R.string.stat_bootloader),
                        value = info.bootloader,
                        valueFontFamily = JetBrainsMonoFamily,
                    )
                },
            ),
        )
    }
}

private fun truncatedAndroidId(id: String): String =
    if (id.length > 12) "${id.take(6)}...${id.takeLast(4)}" else id

@Composable
private fun VersionStatRow(label: String, version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Text(
                text = version,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
