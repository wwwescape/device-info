package com.wwwescape.deviceinfo.data.network

data class WifiState(
    val isConnected: Boolean,
    /** Null when not connected, or when Location permission hasn't been granted. */
    val ssid: String?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    /** 0-4, from [android.net.wifi.WifiManager.calculateSignalLevel]. */
    val signalLevel: Int?,
    /** Raw RSSI in dBm, from [android.net.wifi.WifiInfo.getRssi]. */
    val rssiDbm: Int?,
    /** e.g. "Wi-Fi 6 (802.11ax)", from [android.net.wifi.WifiInfo.getWifiStandard]. */
    val standard: String?,
)
