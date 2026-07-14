package com.wwwescape.deviceinfo.data.network

data class NetworkSnapshot(
    val connectionType: ConnectionType,
    val ipAddress: String?,
    val ipv6Address: String?,
    val dnsServers: List<String>,
    val wifi: WifiState,
    /** One entry per active SIM (dual-SIM devices report two), in slot order. */
    val cellularSims: List<CellularState>,
    val bluetooth: BluetoothState,
    val hasNfc: Boolean,
    val isNfcEnabled: Boolean,
)
