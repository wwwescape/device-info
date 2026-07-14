package com.wwwescape.deviceinfo.ui.screens.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.display.CutoutInfo
import com.wwwescape.deviceinfo.data.display.DisplayInfo
import com.wwwescape.deviceinfo.data.display.DisplayRepository
import com.wwwescape.deviceinfo.ui.components.DetailSection
import com.wwwescape.deviceinfo.ui.components.DetailStatRow
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.util.resolutionTier

@Composable
fun DisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: DisplayViewModel = viewModel(),
) {
    val view = LocalView.current
    val cutoutInfo = remember(view) {
        DisplayRepository.cutoutInfo(ViewCompat.getRootWindowInsets(view))
    }

    DisplayScreenContent(
        info = viewModel.displayInfo,
        cutout = cutoutInfo,
        modifier = modifier,
    )
}

@Composable
private fun DisplayScreenContent(
    info: DisplayInfo,
    cutout: CutoutInfo,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tier = resolutionTier(info.widthPx, info.heightPx)
    val primaryHdrFormat = info.hdrTypes.firstOrNull() ?: stringResource(R.string.value_sdr)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DisplayHero(refreshRateHz = info.refreshRateHz, tier = tier, hdrFormat = primaryHdrFormat)

        DetailSection(
            icon = Icons.Rounded.AspectRatio,
            title = stringResource(R.string.section_resolution),
            rows = listOf(
                { DetailStatRow(stringResource(R.string.stat_resolution), "${info.widthPx} × ${info.heightPx}") },
                { DetailStatRow(stringResource(R.string.stat_ppi), "${info.ppi} PPI") },
                { DetailStatRow(stringResource(R.string.stat_screen_size), "%.1f\"".format(info.screenSizeInches)) },
                { DetailStatRow(stringResource(R.string.stat_resolution_tier), tier) },
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(icon = Icons.Rounded.Speed, title = stringResource(R.string.section_adaptive_refresh))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    info.supportedRefreshRatesHz.forEach { rate ->
                        val isCurrent = rate.toInt() == info.refreshRateHz.toInt()
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = "${rate.toInt()} Hz",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        DetailSection(
            icon = Icons.Rounded.Hd,
            title = stringResource(R.string.section_hdr),
            rows = listOf(
                {
                    DetailStatRow(
                        stringResource(R.string.stat_hdr_types),
                        info.hdrTypes.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: stringResource(R.string.stat_not_supported),
                    )
                },
                {
                    DetailStatRow(
                        stringResource(R.string.stat_wide_color_gamut),
                        if (info.isWideColorGamut) stringResource(R.string.stat_yes) else stringResource(R.string.stat_no),
                    )
                },
            ),
        )

        val cutoutRows = buildList<@Composable () -> Unit> {
            add {
                DetailStatRow(
                    stringResource(R.string.stat_cutout_present),
                    if (cutout.isPresent) stringResource(R.string.stat_yes) else stringResource(R.string.stat_no),
                )
            }
            if (cutout.isPresent) {
                add {
                    val topDp = with(density) { cutout.safeInsetTopPx.toDp() }
                    DetailStatRow(stringResource(R.string.stat_safe_inset_top), "${topDp.value.toInt()} dp")
                }
            }
        }
        DetailSection(
            icon = Icons.Rounded.CropFree,
            title = stringResource(R.string.section_cutout),
            rows = cutoutRows,
        )
    }
}

@Composable
private fun DisplayHero(refreshRateHz: Float, tier: String, hdrFormat: String) {
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${refreshRateHz.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Hz",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.hero_refresh_rate).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                HeroPill(tier)
                HeroPill(hdrFormat)
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
