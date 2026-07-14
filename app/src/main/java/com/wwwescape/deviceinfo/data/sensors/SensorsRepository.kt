package com.wwwescape.deviceinfo.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Enumerating sensors and reading their static metadata needs no permission. Live values are
 * only subscribed to for whichever sensor the user has expanded, not all of them at once.
 */
object SensorsRepository {

    fun listSensors(context: Context): List<SensorInfo> {
        val sensorManager = context.getSystemService(SensorManager::class.java) ?: return emptyList()
        return sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.toSensorInfo() }
    }

    fun readings(context: Context, sensor: Sensor): Flow<SensorReading> = callbackFlow {
        val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
        var latestAccuracy = SensorManager.SENSOR_STATUS_NO_CONTACT
        var latestValues: List<Float>? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                latestValues = event.values.toList()
                trySend(SensorReading(latestValues!!, event.timestamp, latestAccuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                latestAccuracy = accuracy
                latestValues?.let { trySend(SensorReading(it, System.nanoTime(), accuracy)) }
            }
        }
        val registered = sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) ?: false
        awaitClose { if (registered) sensorManager?.unregisterListener(listener) }
    }

    /** The device's primary accelerometer, if present — used to drive the Sensors screen hero. */
    fun defaultAccelerometer(context: Context): Sensor? {
        val sensorManager = context.getSystemService(SensorManager::class.java) ?: return null
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun Sensor.toSensorInfo() = SensorInfo(
        sensor = this,
        name = name,
        vendor = vendor,
        typeName = friendlyTypeName(this),
        maximumRange = maximumRange,
        resolution = resolution,
        power = power,
        minDelayMicros = minDelay,
        unit = unitForSensorType(type),
    )

    /** Derives a display name from the reverse-DNS string type, e.g. "android.sensor.linear_acceleration" -> "Linear Acceleration". */
    private fun friendlyTypeName(sensor: Sensor): String =
        sensor.stringType
            .substringAfterLast('.')
            .split('_')
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

    private fun unitForSensorType(type: Int): String? = when (type) {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GRAVITY -> "m/s²"
        Sensor.TYPE_GYROSCOPE -> "rad/s"
        Sensor.TYPE_MAGNETIC_FIELD -> "µT"
        Sensor.TYPE_LIGHT -> "lux"
        Sensor.TYPE_PRESSURE -> "hPa"
        Sensor.TYPE_PROXIMITY -> "cm"
        Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_TEMPERATURE -> "°C"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
        else -> null
    }
}
