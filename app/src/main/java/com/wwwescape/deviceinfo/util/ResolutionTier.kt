package com.wwwescape.deviceinfo.util

/** Maps real pixel dimensions to a standard industry resolution tier name — no fabricated
 * marketing qualifiers, just the widely-used tier convention based on the shorter pixel edge. */
fun resolutionTier(widthPx: Int, heightPx: Int): String {
    val shortEdge = minOf(widthPx, heightPx)
    return when {
        shortEdge >= 2160 -> "4K UHD"
        shortEdge >= 1440 -> "Quad HD+"
        shortEdge >= 1080 -> "Full HD+"
        shortEdge >= 720 -> "HD+"
        else -> "SD"
    }
}
