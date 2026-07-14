package com.wwwescape.deviceinfo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.wwwescape.deviceinfo.R

/** Every screen in the app. */
enum class Destination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    Overview("overview", R.string.title_overview, Icons.Rounded.Dashboard),
    DeviceOs("device_os", R.string.title_device_os, Icons.Rounded.PhoneAndroid),
    Cpu("cpu", R.string.title_cpu, Icons.Rounded.Memory),
    Memory("memory", R.string.title_memory, Icons.Rounded.Storage),
    Battery("battery", R.string.title_battery, Icons.Rounded.BatteryFull),
    Display("display", R.string.title_display, Icons.Rounded.AspectRatio),
    Network("network", R.string.title_network, Icons.Rounded.Wifi),
    Sensors("sensors", R.string.title_sensors, Icons.Rounded.Sensors),
    Camera("camera", R.string.title_camera, Icons.Rounded.PhotoCamera),
    Settings("settings", R.string.title_settings, Icons.Rounded.Settings);

    companion object {
        /** The category cards shown on the Dashboard's full grid — excludes Overview itself and Settings, which is reached via the top-bar gear icon. */
        val detailScreens = entries.filter { it != Overview && it != Settings }

        fun fromRoute(route: String?): Destination =
            entries.find { it.route == route } ?: Overview
    }
}
