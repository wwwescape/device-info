package com.wwwescape.deviceinfo.data.memory

/** The real device node and filesystem backing internal storage, parsed from `/proc/mounts`. */
data class StorageHardwareInfo(
    val deviceNode: String,
    val fileSystem: String,
)
