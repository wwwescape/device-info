package com.wwwescape.deviceinfo.data.memory

data class StorageVolumeInfo(
    val isRemovable: Boolean,
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = totalBytes - freeBytes
}
