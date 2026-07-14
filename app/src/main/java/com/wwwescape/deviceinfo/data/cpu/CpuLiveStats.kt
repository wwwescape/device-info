package com.wwwescape.deviceinfo.data.cpu

data class CpuLiveStats(
    /** Overall load 0-100, averaged across all cores. Null if /proc/stat isn't readable (common on API 26+). */
    val loadPercent: Float?,
    /** Current clock speed per core in MHz, in core-index order. Null entries mean that core's file wasn't readable. */
    val coreFrequenciesMhz: List<Int?>,
)
