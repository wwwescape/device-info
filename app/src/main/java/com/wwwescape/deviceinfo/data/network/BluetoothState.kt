package com.wwwescape.deviceinfo.data.network

data class BluetoothState(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    /** Device names; empty when disabled, none paired, or (Android 12+) without the Connect permission. */
    val pairedDeviceNames: List<String>,
)
