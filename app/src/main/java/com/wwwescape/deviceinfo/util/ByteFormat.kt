package com.wwwescape.deviceinfo.util

/** Formats a byte count using decimal (1000-based) units, matching Android's own storage UI. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"

    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1000 && unitIndex < units.lastIndex) {
        value /= 1000
        unitIndex++
    }
    return if (unitIndex == 0) "${value.toInt()} ${units[unitIndex]}" else "%.2f %s".format(value, units[unitIndex])
}
