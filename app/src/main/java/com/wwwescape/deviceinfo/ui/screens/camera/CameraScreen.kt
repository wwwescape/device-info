package com.wwwescape.deviceinfo.ui.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCameraBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.camera.CameraFacing
import com.wwwescape.deviceinfo.data.camera.CameraLensInfo
import com.wwwescape.deviceinfo.ui.components.EmptyStateNotice
import com.wwwescape.deviceinfo.ui.components.SectionHeader
import com.wwwescape.deviceinfo.ui.components.StatTile
import com.wwwescape.deviceinfo.util.opticalZoomRatio

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
) {
    if (viewModel.cameras.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateNotice(
                text = stringResource(R.string.cameras_empty),
                icon = Icons.Default.PhotoCameraBack,
            )
        }
        return
    }

    // Wide-to-narrow, matching how phone camera apps order their lens switcher (0.5x, 1x, 5x…).
    val rearLenses = viewModel.cameras
        .filter { it.facing == CameraFacing.BACK }
        .sortedBy { it.focalLength35mmEquivMm ?: Float.MAX_VALUE }
    val frontLenses = viewModel.cameras.filter { it.facing == CameraFacing.FRONT }
    val otherLenses = viewModel.cameras.filter { it.facing != CameraFacing.BACK && it.facing != CameraFacing.FRONT }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CameraHero(rearLensCount = rearLenses.size, mainLens = viewModel.mainLens)

        if (rearLenses.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    icon = Icons.Rounded.CameraAlt,
                    title = stringResource(R.string.section_rear_cameras, rearLenses.size),
                )
                rearLenses.forEachIndexed { index, lens ->
                    val role = viewModel.lensRoles[lens.id]
                    CameraLensCard(
                        lens = lens,
                        title = role?.label() ?: stringResource(R.string.lens_fallback_format, index + 1),
                        zoomRatio = opticalZoomRatio(lens, viewModel.mainLens),
                    )
                }
            }
        }

        if (frontLenses.size == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(icon = Icons.Rounded.PhotoCamera, title = stringResource(R.string.section_front_camera))
                CameraLensCard(lens = frontLenses.first(), title = null, zoomRatio = null)
            }
        } else if (frontLenses.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    icon = Icons.Rounded.PhotoCamera,
                    title = stringResource(R.string.section_front_cameras, frontLenses.size),
                )
                frontLenses.forEachIndexed { index, lens ->
                    CameraLensCard(
                        lens = lens,
                        title = stringResource(R.string.lens_fallback_format, index + 1),
                        zoomRatio = null,
                    )
                }
            }
        }

        otherLenses.forEach { lens ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(icon = Icons.Rounded.PhotoCamera, title = lens.facing.label())
                CameraLensCard(lens = lens, title = null, zoomRatio = null)
            }
        }
    }
}

@Composable
private fun CameraHero(rearLensCount: Int, mainLens: CameraLensInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = stringResource(R.string.hero_lens_count_format, rearLensCount),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (mainLens?.megapixels != null) {
                Text(
                    text = stringResource(R.string.hero_main_sensor_format, mainLens.megapixels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** A stat-grid lens card: an optional small role overline, a big megapixel headline, a 2-column
 * grid of [StatTile]s for the rest of the specs, and a row of feature pills for the booleans. */
@Composable
private fun CameraLensCard(
    lens: CameraLensInfo,
    title: String?,
    zoomRatio: Float?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = lens.megapixels?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = " MP",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (lens.resolutionWidth != null && lens.resolutionHeight != null) {
                    Text(
                        text = "${lens.resolutionWidth} × ${lens.resolutionHeight}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val tiles = buildList {
                add(
                    stringResource(R.string.stat_aperture) to (
                        lens.aperturesFNumber.takeIf { it.isNotEmpty() }?.joinToString(", ") { "f/$it" }
                            ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(
                    stringResource(R.string.stat_focal_length) to (
                        lens.focalLengthsMm.takeIf { it.isNotEmpty() }?.joinToString(", ") { "${it}mm" }
                            ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(
                    stringResource(R.string.stat_focal_length_35mm) to (
                        lens.focalLength35mmEquivMm?.let { "%.0fmm".format(it) } ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(
                    stringResource(R.string.stat_field_of_view) to (
                        lens.fieldOfViewDegrees?.let { "%.0f°".format(it) } ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                if (zoomRatio != null) {
                    add(stringResource(R.string.stat_optical_zoom) to stringResource(R.string.zoom_ratio_format, zoomRatio))
                }
                add(
                    stringResource(R.string.stat_sensor_size) to (
                        lens.sensorSizeMm ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(
                    stringResource(R.string.stat_pixel_pitch) to (
                        lens.pixelPitchMicrons?.let { "%.2f µm".format(it) } ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(
                    stringResource(R.string.stat_color_filter) to (
                        lens.colorFilterArrangement ?: stringResource(R.string.stat_unavailable)
                        ),
                )
                add(stringResource(R.string.stat_hardware_level) to lens.hardwareLevel.label())
            }
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (label, value) -> StatTile(label, value, modifier = Modifier.weight(1f)) }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeaturePill(stringResource(R.string.stat_flash), lens.hasFlash)
                FeaturePill(stringResource(R.string.stat_optical_stabilization), lens.hasOpticalStabilization)
                FeaturePill(stringResource(R.string.stat_video_stabilization), lens.hasVideoStabilization)
                FeaturePill(stringResource(R.string.stat_4k_video), lens.supports4kVideo)
            }
        }
    }
}

@Composable
private fun FeaturePill(label: String, enabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = if (enabled) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Cancel,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
