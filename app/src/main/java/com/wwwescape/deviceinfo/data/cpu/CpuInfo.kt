package com.wwwescape.deviceinfo.data.cpu

data class CpuCoreInfo(
    val index: Int,
    /** Peak clock speed in MHz, from cpufreq sysfs. Null if the file isn't readable on this device. */
    val maxFreqMhz: Int?,
    val minFreqMhz: Int?,
)

data class CpuInfo(
    val socName: String,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val coreCount: Int,
    val cores: List<CpuCoreInfo>,
    val kernelVersion: String,
    /** CPU frequency-scaling governor (e.g. "schedutil"). Null if not readable on this device. */
    val governor: String?,
    val hasVulkanSupport: Boolean,
)
