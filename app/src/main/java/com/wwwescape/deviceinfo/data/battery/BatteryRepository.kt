package com.wwwescape.deviceinfo.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.wwwescape.deviceinfo.R
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

/**
 * Battery state is event-driven (the system broadcasts [Intent.ACTION_BATTERY_CHANGED]
 * whenever it changes), so this polls nothing — it just relays broadcasts and the
 * Android 10+ thermal listener as flows.
 */
object BatteryRepository {

    fun currentBatteryInfo(context: Context): BatteryInfo {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return sticky?.let { parseBatteryInfo(context, it) } ?: BatteryInfo(
            percent = 0,
            status = BatteryChargeStatus.UNKNOWN,
            plugSource = BatteryPlugSource.NONE,
            health = BatteryHealth.UNKNOWN,
            voltageMillivolts = 0,
            temperatureCelsius = 0f,
            technology = context.getString(R.string.value_unknown),
            isPresent = false,
            currentMicroAmps = null,
            designCapacityMah = null,
            socTemperatureCelsius = null,
            skinTemperatureCelsius = null,
        )
    }

    fun batteryUpdates(context: Context): Flow<BatteryInfo> = callbackFlow {
        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivedContext: Context, intent: Intent) {
                trySend(parseBatteryInfo(appContext, intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        sticky?.let { trySend(parseBatteryInfo(appContext, it)) }
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    /** One-shot counterpart to [thermalUpdates], for callers (like widgets) that just need the
     * current value rather than a live listener. */
    fun currentThermalStatus(context: Context): ThermalStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalStatus.UNAVAILABLE
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return ThermalStatus.UNAVAILABLE
        return mapThermalStatus(powerManager.currentThermalStatus)
    }

    fun thermalUpdates(context: Context): Flow<ThermalInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return flow { emit(ThermalInfo(ThermalStatus.UNAVAILABLE)) }
        }
        return callbackFlow {
            val appContext = context.applicationContext
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                trySend(ThermalInfo(mapThermalStatus(status)))
            }
            trySend(ThermalInfo(mapThermalStatus(powerManager.currentThermalStatus)))
            powerManager.addThermalStatusListener(listener)
            awaitClose { powerManager.removeThermalStatusListener(listener) }
        }
    }

    private fun parseBatteryInfo(context: Context, intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryChargeStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
            else -> BatteryChargeStatus.UNKNOWN
        }

        val plugSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> BatteryPlugSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> BatteryPlugSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPlugSource.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> BatteryPlugSource.DOCK
            else -> BatteryPlugSource.NONE
        }

        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }

        return BatteryInfo(
            percent = percent,
            status = status,
            plugSource = plugSource,
            health = health,
            voltageMillivolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            temperatureCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: context.getString(R.string.value_unknown),
            isPresent = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
            currentMicroAmps = readCurrentMicroAmps(context),
            designCapacityMah = designCapacityMah,
            socTemperatureCelsius = readDeviceTemperature(context, HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU),
            skinTemperatureCelsius = readDeviceTemperature(context, HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN),
        )
    }

    private fun readCurrentMicroAmps(context: Context): Long? {
        val batteryManager = context.getSystemService(BatteryManager::class.java) ?: return null
        val current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return current.takeUnless { it == Long.MIN_VALUE }
    }

    /** A fixed hardware property — read once per process instead of on every battery broadcast. */
    private val designCapacityMah: Int? by lazy {
        runCatching {
            File("/sys/class/power_supply/battery/charge_full_design")
                .readText()
                .trim()
                .toLong()
                .let { microAmpHours -> (microAmpHours / 1000).toInt() }
        }.getOrNull()
    }

    /** [HardwarePropertiesManager] is restricted to device owners / DEVICE_POWER holders, so on
     * almost every device it throws a [SecurityException]. Remember that, rather than paying for a
     * failing binder call (and exception) twice on every battery broadcast. */
    @Volatile
    private var deviceTemperaturesDenied = false

    private fun readDeviceTemperature(context: Context, deviceTemperatureType: Int): Float? {
        if (deviceTemperaturesDenied) return null
        return try {
            val manager = context.getSystemService(HardwarePropertiesManager::class.java) ?: return null
            manager.getDeviceTemperatures(deviceTemperatureType, HardwarePropertiesManager.TEMPERATURE_CURRENT)
                .filterNot { it.isNaN() }
                .firstOrNull()
        } catch (e: SecurityException) {
            deviceTemperaturesDenied = true
            null
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun mapThermalStatus(status: Int): ThermalStatus = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> ThermalStatus.UNAVAILABLE
    }
}
