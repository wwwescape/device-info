package com.wwwescape.deviceinfo.data.display

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityBucket: String,
    val refreshRateHz: Float,
    val supportedRefreshRatesHz: List<Float>,
    val screenSizeInches: Double,
    val hdrTypes: List<String>,
    val isWideColorGamut: Boolean,
) {
    /** Diagonal pixel density — derived from real pixel counts and screen size, not fabricated. */
    val ppi: Int get() {
        val diagonalPx = kotlin.math.sqrt((widthPx * widthPx + heightPx * heightPx).toDouble())
        return if (screenSizeInches > 0) (diagonalPx / screenSizeInches).toInt() else 0
    }
}
