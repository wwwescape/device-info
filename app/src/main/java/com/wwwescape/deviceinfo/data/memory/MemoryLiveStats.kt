package com.wwwescape.deviceinfo.data.memory

data class MemoryLiveStats(
    val ram: RamInfo,
    /** Internal storage first, followed by any detected removable volumes. */
    val storageVolumes: List<StorageVolumeInfo>,
)
