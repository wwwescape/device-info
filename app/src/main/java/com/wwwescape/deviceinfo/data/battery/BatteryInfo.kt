package com.wwwescape.deviceinfo.data.battery

import androidx.annotation.StringRes
import com.wwwescape.deviceinfo.R

enum class BatteryChargeStatus { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

enum class BatteryPlugSource { AC, USB, WIRELESS, DOCK, NONE }

enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD, UNKNOWN }

/** Plain (non-@Composable) label lookup, for callers like widgets that must resolve strings via
 * `context.getString()` instead of `stringResource()`. */
@get:StringRes
val BatteryHealth.labelRes: Int get() = when (this) {
    BatteryHealth.GOOD -> R.string.battery_health_good
    BatteryHealth.OVERHEAT -> R.string.battery_health_overheat
    BatteryHealth.DEAD -> R.string.battery_health_dead
    BatteryHealth.OVER_VOLTAGE -> R.string.battery_health_over_voltage
    BatteryHealth.UNSPECIFIED_FAILURE -> R.string.battery_health_unspecified_failure
    BatteryHealth.COLD -> R.string.battery_health_cold
    BatteryHealth.UNKNOWN -> R.string.battery_health_unknown
}

data class BatteryInfo(
    val percent: Int,
    val status: BatteryChargeStatus,
    val plugSource: BatteryPlugSource,
    val health: BatteryHealth,
    val voltageMillivolts: Int,
    val temperatureCelsius: Float,
    val technology: String,
    val isPresent: Boolean,
    /** Instantaneous current draw in µA (positive = charging on most OEMs, but sign isn't
     * guaranteed consistent across devices). Null if the property isn't supported. */
    val currentMicroAmps: Long?,
    /** Rated (not remaining) capacity in mAh, from sysfs. Null if unreadable. */
    val designCapacityMah: Int?,
    /** Device-wide thermal-zone readings via [android.os.HardwarePropertiesManager]. Null if the
     * device doesn't expose that zone to a non-privileged app. */
    val socTemperatureCelsius: Float?,
    val skinTemperatureCelsius: Float?,
)
