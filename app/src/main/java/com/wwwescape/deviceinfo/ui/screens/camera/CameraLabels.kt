package com.wwwescape.deviceinfo.ui.screens.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.data.camera.CameraFacing
import com.wwwescape.deviceinfo.data.camera.CameraHardwareLevel
import com.wwwescape.deviceinfo.util.CameraLensRole

@Composable
fun CameraFacing.label(): String = stringResource(
    when (this) {
        CameraFacing.FRONT -> R.string.camera_facing_front
        CameraFacing.BACK -> R.string.camera_facing_back
        CameraFacing.EXTERNAL -> R.string.camera_facing_external
        CameraFacing.UNKNOWN -> R.string.value_unknown
    },
)

@Composable
fun CameraHardwareLevel.label(): String = stringResource(
    when (this) {
        CameraHardwareLevel.LEGACY -> R.string.camera_hardware_legacy
        CameraHardwareLevel.LIMITED -> R.string.camera_hardware_limited
        CameraHardwareLevel.FULL -> R.string.camera_hardware_full
        CameraHardwareLevel.LEVEL_3 -> R.string.camera_hardware_level_3
        CameraHardwareLevel.EXTERNAL -> R.string.camera_facing_external
        CameraHardwareLevel.UNKNOWN -> R.string.value_unknown
    },
)

@Composable
fun CameraLensRole.label(): String = stringResource(
    when (this) {
        CameraLensRole.MAIN -> R.string.lens_role_main
        CameraLensRole.ULTRA_WIDE -> R.string.lens_role_ultrawide
        CameraLensRole.TELEPHOTO -> R.string.lens_role_telephoto
    },
)
