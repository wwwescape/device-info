package com.wwwescape.deviceinfo.ui.screens.sensors

import android.app.Application
import android.hardware.Sensor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.deviceinfo.data.sensors.SensorInfo
import com.wwwescape.deviceinfo.data.sensors.SensorReading
import com.wwwescape.deviceinfo.data.sensors.SensorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

private const val MAX_ACCEL_HISTORY = 30

class SensorsViewModel(application: Application) : AndroidViewModel(application) {
    val sensors: List<SensorInfo> = SensorsRepository.listSensors(application)
    val accelerometer: Sensor? = SensorsRepository.defaultAccelerometer(application)
    val accelerometerInfo: SensorInfo? = sensors.firstOrNull { it.sensor == accelerometer }

    private val _accelHistory = MutableStateFlow<List<Float>>(emptyList())
    val accelHistory: StateFlow<List<Float>> = _accelHistory.asStateFlow()

    // The accelerometer listener is only registered while the screen observes this (it stops 5s
    // after the app is backgrounded or another destination covers it) — a sensor left running at
    // UI rate is one of the most expensive things an app can do to the battery. The sparkline
    // history is kept across pauses since it's fed as a side effect of the same subscription.
    val accelReading: StateFlow<SensorReading?> = (accelerometer?.let { SensorsRepository.readings(application, it) } ?: emptyFlow())
        .onEach { reading ->
            val magnitude = sqrt(reading.values.sumOf { (it * it).toDouble() }).toFloat()
            _accelHistory.update { history -> (history + magnitude).takeLast(MAX_ACCEL_HISTORY) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
