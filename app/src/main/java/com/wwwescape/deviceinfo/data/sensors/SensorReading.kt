package com.wwwescape.deviceinfo.data.sensors

import android.hardware.SensorManager

/** [accuracy] is one of [SensorManager]'s `SENSOR_STATUS_*` constants — defaults to
 * `SENSOR_STATUS_NO_CONTACT` as a real "not yet reported" sentinel until the first
 * `onAccuracyChanged` callback arrives. */
data class SensorReading(
    val values: List<Float>,
    val timestampNanos: Long,
    val accuracy: Int = SensorManager.SENSOR_STATUS_NO_CONTACT,
)
