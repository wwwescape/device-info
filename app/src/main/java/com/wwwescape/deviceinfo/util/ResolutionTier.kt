package com.wwwescape.deviceinfo.util

/** Maps real pixel dimensions to a standard industry resolution tier name — no fabricated
 * marketing qualifiers, just the widely-used tier convention based on the shorter pixel edge.
 * The "+" suffix is the convention for panels taller than 16:9 (e.g. a 2400×1080 phone is
 * "Full HD+"); a standard 16:9 panel such as a 1920×1080 TV is plain "Full HD". */
fun resolutionTier(widthPx: Int, heightPx: Int): String {
    val shortEdge = minOf(widthPx, heightPx)
    val longEdge = maxOf(widthPx, heightPx)
    val plus = if (shortEdge > 0 && longEdge.toFloat() / shortEdge > 16f / 9f + 0.01f) "+" else ""
    return when {
        shortEdge >= 2160 -> "4K UHD"
        shortEdge >= 1440 -> "Quad HD$plus"
        shortEdge >= 1080 -> "Full HD$plus"
        shortEdge >= 720 -> "HD$plus"
        else -> "SD"
    }
}
