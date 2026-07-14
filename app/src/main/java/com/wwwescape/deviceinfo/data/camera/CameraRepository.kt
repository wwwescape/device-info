package com.wwwescape.deviceinfo.data.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build

/**
 * Reads Camera2 characteristics for every lens. Enumerating cameras and reading their static
 * characteristics needs no permission — only actually opening a camera session would.
 *
 * Many phones (Pixel 8 included) expose their rear array as a single "logical multi-camera" ID in
 * [CameraManager.getCameraIdList] — the wide, ultra-wide, and telephoto sensors only become
 * individually visible via [CameraCharacteristics.getPhysicalCameraIds], a separate public,
 * non-privileged API (since API 28). Without unwrapping it, a triple-lens phone looks like it has
 * one blended "lens".
 */
object CameraRepository {

    fun listCameras(context: Context): List<CameraLensInfo> {
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
        return runCatching {
            cameraManager.cameraIdList.flatMap { id -> readLensGroup(cameraManager, id) }
        }.getOrElse { emptyList() }
    }

    /** Expands a single top-level camera ID into one [CameraLensInfo] per physical sensor, or a
     * single-element list if it isn't a logical multi-camera. */
    private fun readLensGroup(manager: CameraManager, id: String): List<CameraLensInfo> {
        val characteristics = manager.getCameraCharacteristics(id)
        val facing = resolveFacing(characteristics)

        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            characteristics.physicalCameraIds
        } else {
            emptySet()
        }
        if (physicalIds.isEmpty()) {
            return listOf(readCameraInfo(id, facing, characteristics))
        }

        val physicalLenses = physicalIds.sorted().mapNotNull { physicalId ->
            runCatching { readCameraInfo(physicalId, facing, manager.getCameraCharacteristics(physicalId)) }.getOrNull()
        }
        return dedupeByFocalLength(physicalLenses).ifEmpty { listOf(readCameraInfo(id, facing, characteristics)) }
    }

    /** Some logical multi-cameras expose the *same* physical lens under two physical IDs — e.g. a
     * full-resolution mode and a cropped/binned mode — rather than genuinely distinct optical
     * modules (confirmed on this app's test device: two "front" physical IDs report identical
     * focal length and aperture, differing only in pixel array size). Lenses sharing a focal
     * length are the same physical lens; keep only the highest-resolution reading from each
     * group so a single real lens doesn't get listed twice. */
    private fun dedupeByFocalLength(lenses: List<CameraLensInfo>): List<CameraLensInfo> =
        lenses.groupBy { lens -> lens.focalLengthsMm.firstOrNull()?.let { Math.round(it * 10) } ?: lens.id.hashCode() }
            .values
            .map { group -> group.maxBy { it.megapixels ?: 0.0 } }

    private fun resolveFacing(characteristics: CameraCharacteristics): CameraFacing =
        when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
            else -> CameraFacing.UNKNOWN
        }

    private fun readCameraInfo(id: String, facing: CameraFacing, characteristics: CameraCharacteristics): CameraLensInfo {
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val maxJpegSize = streamMap?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }

        // On quad/nona-Bayer sensors, SENSOR_INFO_PIXEL_ARRAY_SIZE reflects the true physical
        // pixel count, but the default JPEG output (maxJpegSize) is pixel-binned to a quarter (or
        // less) of that — e.g. a real 50MP main sensor whose regular photos come out at 12.5MP.
        // SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION (API 33+) is the same idea but for the
        // sensor's un-binned "remosaic" mode when the regular field itself is already binned.
        val maxResPixelArraySize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION)
        } else {
            null
        }
        val pixelArraySize = maxResPixelArraySize ?: characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val resolutionWidth = pixelArraySize?.width ?: maxJpegSize?.width
        val resolutionHeight = pixelArraySize?.height ?: maxJpegSize?.height
        val megapixels = if (resolutionWidth != null && resolutionHeight != null) {
            (resolutionWidth.toDouble() * resolutionHeight) / 1_000_000.0
        } else {
            null
        }

        val sensorPhysicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val sensorSizeMm = sensorPhysicalSize?.let { "%.2f × %.2f mm".format(it.width, it.height) }

        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty()
        val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList().orEmpty()

        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val hasOpticalStabilization = characteristics
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.any { it != CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_OFF } ?: false
        val hasVideoStabilization = characteristics
            .get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            ?.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF } ?: false
        val supports4kVideo = streamMap?.getOutputSizes(MediaRecorder::class.java)
            ?.any { it.width >= 3840 && it.height >= 2160 } ?: false

        val colorFilterArrangement = colorFilterArrangementName(
            characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT),
        )

        val hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> CameraHardwareLevel.LEGACY
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> CameraHardwareLevel.LIMITED
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> CameraHardwareLevel.FULL
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> CameraHardwareLevel.LEVEL_3
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> CameraHardwareLevel.EXTERNAL
            else -> CameraHardwareLevel.UNKNOWN
        }

        return CameraLensInfo(
            id = id,
            facing = facing,
            megapixels = megapixels,
            resolutionWidth = resolutionWidth,
            resolutionHeight = resolutionHeight,
            pixelArrayWidth = pixelArraySize?.width,
            focalLengthsMm = focalLengths,
            aperturesFNumber = apertures,
            sensorSizeMm = sensorSizeMm,
            sensorPhysicalWidthMm = sensorPhysicalSize?.width,
            sensorPhysicalHeightMm = sensorPhysicalSize?.height,
            colorFilterArrangement = colorFilterArrangement,
            hasFlash = hasFlash,
            hasOpticalStabilization = hasOpticalStabilization,
            hasVideoStabilization = hasVideoStabilization,
            supports4kVideo = supports4kVideo,
            hardwareLevel = hardwareLevel,
        )
    }

    /** Bayer/monochrome/NIR filter names are fixed technical terms — never translated. */
    private fun colorFilterArrangementName(arrangement: Int?): String? = when (arrangement) {
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> "RGB"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO -> "Monochrome"
        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR -> "NIR"
        else -> null
    }
}
