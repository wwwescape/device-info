package com.wwwescape.deviceinfo.data.memory

/** A kernel-level RAM breakdown from `/proc/meminfo`, in bytes. */
data class MemoryBreakdown(
    val activeBytes: Long,
    val cachedBytes: Long,
    val swapUsedBytes: Long,
    val buffersBytes: Long,
)
