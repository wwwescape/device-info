package com.wwwescape.deviceinfo.data.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Reads RAM usage via [ActivityManager] and storage capacity via [StatFs]. Both are cheap,
 * unrestricted system calls, so they're polled together on a single ticker.
 */
object MemoryRepository {

    private val MEMINFO_SEPARATOR = Regex(":\\s+")

    fun liveStats(context: Context, intervalMillis: Long = 2_000L): Flow<MemoryLiveStats> = flow {
        val appContext = context.applicationContext
        while (true) {
            emit(MemoryLiveStats(ram = readRamInfo(appContext), storageVolumes = readStorageVolumes(appContext)))
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun readRamInfo(context: Context): RamInfo {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return RamInfo(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            lowMemoryThresholdBytes = info.threshold,
            isLow = info.lowMemory,
        )
    }

    /** Internal storage first, followed by any detected removable volumes. */
    fun readStorageVolumes(context: Context): List<StorageVolumeInfo> {
        val volumes = mutableListOf<StorageVolumeInfo>()

        runCatching {
            val statFs = StatFs(Environment.getDataDirectory().path)
            volumes += StorageVolumeInfo(
                isRemovable = false,
                totalBytes = statFs.totalBytes,
                freeBytes = statFs.availableBytes,
            )
        }

        val externalDirs = context.getExternalFilesDirs(null)
        // getExternalFilesDirs already creates each app-specific dir; null means the volume
        // isn't mounted right now.
        externalDirs.drop(1).forEach { dir ->
            if (dir == null) return@forEach
            runCatching {
                val statFs = StatFs(dir.path)
                volumes += StorageVolumeInfo(
                    isRemovable = true,
                    totalBytes = statFs.totalBytes,
                    freeBytes = statFs.availableBytes,
                )
            }
        }

        return volumes
    }

    /** Parses `/proc/meminfo` for a kernel-level breakdown beyond what [ActivityManager] exposes.
     * Fails soft to null — this file is normally readable, but not guaranteed on every OEM. */
    fun readMemoryBreakdown(): MemoryBreakdown? = runCatching {
        val fields = File("/proc/meminfo").readLines()
            .mapNotNull { line ->
                val parts = line.split(MEMINFO_SEPARATOR, limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val kb = parts[1].trim().removeSuffix(" kB").toLongOrNull() ?: return@mapNotNull null
                parts[0] to kb * 1024
            }
            .toMap()

        val swapTotal = fields["SwapTotal"] ?: return@runCatching null
        val swapFree = fields["SwapFree"] ?: return@runCatching null
        MemoryBreakdown(
            activeBytes = fields["Active"] ?: return@runCatching null,
            cachedBytes = fields["Cached"] ?: return@runCatching null,
            swapUsedBytes = swapTotal - swapFree,
            buffersBytes = fields["Buffers"] ?: return@runCatching null,
        )
    }.getOrNull()

    /** Parses `/proc/mounts` for the real device node and filesystem backing `/data` (internal
     * storage). Fails soft to null if the mount entry isn't found or isn't readable. */
    fun readStorageHardwareInfo(): StorageHardwareInfo? = runCatching {
        File("/proc/mounts").readLines()
            .map { it.split(" ") }
            .firstOrNull { fields -> fields.size >= 3 && fields[1] == "/data" }
            ?.let { fields -> StorageHardwareInfo(deviceNode = fields[0], fileSystem = fields[2]) }
    }.getOrNull()
}
