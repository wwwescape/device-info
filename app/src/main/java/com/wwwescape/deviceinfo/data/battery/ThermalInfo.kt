package com.wwwescape.deviceinfo.data.battery

import androidx.annotation.StringRes
import com.wwwescape.deviceinfo.R

/** Mirrors [android.os.PowerManager] thermal status. UNAVAILABLE covers pre-Android 10 devices. */
enum class ThermalStatus { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNAVAILABLE }

data class ThermalInfo(val status: ThermalStatus)

/** Plain (non-@Composable) label lookup, for callers like widgets that can't use
 * `stringResource()` and must resolve strings via `context.getString()` instead. */
@get:StringRes
val ThermalStatus.labelRes: Int get() = when (this) {
    ThermalStatus.NONE -> R.string.thermal_status_none
    ThermalStatus.LIGHT -> R.string.thermal_status_light
    ThermalStatus.MODERATE -> R.string.thermal_status_moderate
    ThermalStatus.SEVERE -> R.string.thermal_status_severe
    ThermalStatus.CRITICAL -> R.string.thermal_status_critical
    ThermalStatus.EMERGENCY -> R.string.thermal_status_emergency
    ThermalStatus.SHUTDOWN -> R.string.thermal_status_shutdown
    ThermalStatus.UNAVAILABLE -> R.string.thermal_status_unavailable
}
