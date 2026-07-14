package com.wwwescape.deviceinfo.data.sensors

import android.hardware.Sensor

data class SensorInfo(
    /** Kept to register/unregister a live listener; carries no context reference. */
    val sensor: Sensor,
    val name: String,
    val vendor: String,
    val typeName: String,
    val maximumRange: Float,
    val resolution: Float,
    /** Current draw in mA while active. */
    val power: Float,
    val minDelayMicros: Int,
    /** Physical unit for range/resolution/live values, e.g. "m/s²". Null if unknown. */
    val unit: String?,
) {
    /** Null for on-change sensors (minDelay == 0, e.g. proximity/light), which report on events
     * rather than a fixed rate. */
    val maxRateHz: Int? get() = if (minDelayMicros > 0) 1_000_000 / minDelayMicros else null
}
