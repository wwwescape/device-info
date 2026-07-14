package com.wwwescape.deviceinfo.data.cpu

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads SoC/CPU info from [android.os.Build] and the cpufreq/proc-stat sysfs files.
 * Per-core frequency and system-wide load are best-effort: many OEMs and Android 8+
 * SELinux policies block these files for regular apps, so every read fails soft to null
 * instead of crashing or showing a stale value.
 */
object CpuRepository {

    private val WHITESPACE = Regex("\\s+")

    fun collectStatic(context: Context): CpuInfo {
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        // SUPPORTED_ABIS is guaranteed non-empty by the platform; the fallback never
        // surfaces to the UI in practice, so an empty string is fine here.
        val primaryAbi = supportedAbis.firstOrNull() ?: ""
        val coreCount = Runtime.getRuntime().availableProcessors()
        return CpuInfo(
            socName = resolveSocName(),
            primaryAbi = primaryAbi,
            supportedAbis = supportedAbis,
            coreCount = coreCount,
            cores = (0 until coreCount).map { index ->
                CpuCoreInfo(
                    index = index,
                    maxFreqMhz = readFreqMhz(index, "cpuinfo_max_freq"),
                    minFreqMhz = readFreqMhz(index, "cpuinfo_min_freq"),
                )
            },
            kernelVersion = System.getProperty("os.version") ?: "Unknown",
            governor = readGovernor(0),
            hasVulkanSupport = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL),
        )
    }

    /** Emits an updated load percentage + per-core frequency snapshot roughly every [intervalMillis]. */
    fun liveStats(coreCount: Int, intervalMillis: Long = 1_500L): Flow<CpuLiveStats> = flow {
        var previousSnapshot: ProcStatSnapshot? = null
        while (true) {
            val snapshot = readProcStatSnapshot()
            val load = if (previousSnapshot != null && snapshot != null) {
                loadPercentBetween(previousSnapshot, snapshot)
            } else {
                null
            }
            if (snapshot != null) previousSnapshot = snapshot

            val frequencies = (0 until coreCount).map { index -> readCurrentFreqMhz(index) }
            emit(CpuLiveStats(loadPercent = load, coreFrequenciesMhz = frequencies))
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    /** One-shot load percentage for callers (like widgets) that can't stay subscribed to
     * [liveStats] — takes two `/proc/stat` samples [sampleGapMillis] apart and diffs them. */
    suspend fun currentLoadPercent(sampleGapMillis: Long = 700L): Float? = withContext(Dispatchers.IO) {
        val first = readProcStatSnapshot() ?: return@withContext null
        delay(sampleGapMillis)
        val second = readProcStatSnapshot() ?: return@withContext null
        loadPercentBetween(first, second)
    }

    private fun resolveSocName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manufacturer = Build.SOC_MANUFACTURER.takeUnless { it.isNullOrBlank() || it == Build.UNKNOWN }
            val model = Build.SOC_MODEL.takeUnless { it.isNullOrBlank() || it == Build.UNKNOWN }
            val combined = listOfNotNull(manufacturer, model).joinToString(" ")
            if (combined.isNotBlank()) return combined
        }
        return Build.HARDWARE
    }

    private fun readFreqMhz(coreIndex: Int, fileName: String): Int? = runCatching {
        File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/$fileName")
            .readText()
            .trim()
            .toInt() / 1000
    }.getOrNull()

    private fun readCurrentFreqMhz(coreIndex: Int): Int? =
        readFreqMhz(coreIndex, "scaling_cur_freq")

    private fun readGovernor(coreIndex: Int): String? = runCatching {
        File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_governor")
            .readText()
            .trim()
            .takeUnless { it.isEmpty() }
    }.getOrNull()

    private data class ProcStatSnapshot(val idle: Long, val total: Long)

    private fun readProcStatSnapshot(): ProcStatSnapshot? = runCatching {
        val firstLine = File("/proc/stat").bufferedReader().use { it.readLine() }
        val fields = firstLine
            .trim()
            .split(WHITESPACE)
            .drop(1) // drop the leading "cpu" label
            .map { it.toLong() }
        // user, nice, system, idle, iowait, irq, softirq, steal, ...
        val idle = fields[3] + fields.getOrElse(4) { 0L }
        val total = fields.sum()
        ProcStatSnapshot(idle = idle, total = total)
    }.getOrNull()

    private fun loadPercentBetween(previous: ProcStatSnapshot, current: ProcStatSnapshot): Float? {
        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        if (totalDelta <= 0) return null
        return (1f - idleDelta.toFloat() / totalDelta.toFloat()).coerceIn(0f, 1f) * 100f
    }
}
