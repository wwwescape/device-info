package com.wwwescape.deviceinfo.util

import com.wwwescape.deviceinfo.data.camera.CameraFacing
import com.wwwescape.deviceinfo.data.camera.CameraLensInfo

enum class CameraLensRole {
    MAIN,
    ULTRA_WIDE,
    TELEPHOTO,
}

private const val ULTRA_WIDE_MAX_EQUIV_MM = 20f
private const val TELEPHOTO_MIN_EQUIV_MM = 35f

/**
 * Classifies a device's rear-facing lenses by 35mm-equivalent focal length: the shortest is
 * Ultra-Wide (only if meaningfully short), the longest is Telephoto (only if meaningfully long),
 * everything else — including a device's only rear lens — is Main. Front-facing lenses are never
 * classified; role labeling only applies to the rear array.
 */
fun classifyLensRoles(lenses: List<CameraLensInfo>): Map<String, CameraLensRole> {
    val rear = lenses.filter { it.facing == CameraFacing.BACK && it.focalLength35mmEquivMm != null }
    if (rear.isEmpty()) return emptyMap()

    val roles = rear.associate { it.id to CameraLensRole.MAIN }.toMutableMap()
    if (rear.size > 1) {
        val shortest = rear.minBy { it.focalLength35mmEquivMm!! }
        val longest = rear.maxBy { it.focalLength35mmEquivMm!! }
        if (shortest.id != longest.id) {
            if (shortest.focalLength35mmEquivMm!! < ULTRA_WIDE_MAX_EQUIV_MM) {
                roles[shortest.id] = CameraLensRole.ULTRA_WIDE
            }
            if (longest.focalLength35mmEquivMm!! > TELEPHOTO_MIN_EQUIV_MM) {
                roles[longest.id] = CameraLensRole.TELEPHOTO
            }
        }
    }
    return roles
}

fun mainLens(lenses: List<CameraLensInfo>, roles: Map<String, CameraLensRole>): CameraLensInfo? =
    lenses.firstOrNull { roles[it.id] == CameraLensRole.MAIN }

/**
 * Optical zoom ratio of [lens] relative to [mainLens], e.g. 5.0 for a 5x telephoto. Null if
 * either 35mm-equivalent focal length is unavailable, [mainLens] is null, or [lens] is itself the
 * main lens (no zoom ratio relative to itself).
 */
fun opticalZoomRatio(lens: CameraLensInfo, mainLens: CameraLensInfo?): Float? {
    if (mainLens == null || lens.id == mainLens.id) return null
    val lensEquiv = lens.focalLength35mmEquivMm ?: return null
    val mainEquiv = mainLens.focalLength35mmEquivMm ?: return null
    if (mainEquiv <= 0) return null
    return lensEquiv / mainEquiv
}
