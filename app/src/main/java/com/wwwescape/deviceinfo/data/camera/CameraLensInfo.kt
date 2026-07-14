package com.wwwescape.deviceinfo.data.camera

import kotlin.math.atan
import kotlin.math.hypot

/** 35mm-film diagonal, in mm — the industry-standard reference for "equivalent focal length". */
private const val FULL_FRAME_DIAGONAL_MM = 43.27

data class CameraLensInfo(
    val id: String,
    val facing: CameraFacing,
    val megapixels: Double?,
    val resolutionWidth: Int?,
    val resolutionHeight: Int?,
    val pixelArrayWidth: Int?,
    val focalLengthsMm: List<Float>,
    val aperturesFNumber: List<Float>,
    val sensorSizeMm: String?,
    val sensorPhysicalWidthMm: Float?,
    val sensorPhysicalHeightMm: Float?,
    val colorFilterArrangement: String?,
    val hasFlash: Boolean,
    val hasOpticalStabilization: Boolean,
    val hasVideoStabilization: Boolean,
    val supports4kVideo: Boolean,
    val hardwareLevel: CameraHardwareLevel,
) {
    val sensorDiagonalMm: Double? get() =
        if (sensorPhysicalWidthMm != null && sensorPhysicalHeightMm != null) {
            hypot(sensorPhysicalWidthMm.toDouble(), sensorPhysicalHeightMm.toDouble())
        } else {
            null
        }

    val pixelPitchMicrons: Double? get() {
        val width = sensorPhysicalWidthMm ?: return null
        val pixels = pixelArrayWidth ?: return null
        if (pixels <= 0) return null
        return (width / pixels) * 1000.0
    }

    /** 35mm-equivalent focal length, using the primary reported focal length. */
    val focalLength35mmEquivMm: Float? get() {
        val focalLength = focalLengthsMm.firstOrNull() ?: return null
        val diagonal = sensorDiagonalMm ?: return null
        if (diagonal <= 0) return null
        return (focalLength * (FULL_FRAME_DIAGONAL_MM / diagonal)).toFloat()
    }

    /** Diagonal field of view, in degrees, from the primary reported focal length. */
    val fieldOfViewDegrees: Float? get() {
        val focalLength = focalLengthsMm.firstOrNull() ?: return null
        val diagonal = sensorDiagonalMm ?: return null
        if (focalLength <= 0) return null
        return Math.toDegrees(2 * atan(diagonal / (2.0 * focalLength))).toFloat()
    }
}
