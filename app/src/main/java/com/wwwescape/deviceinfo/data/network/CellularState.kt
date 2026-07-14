package com.wwwescape.deviceinfo.data.network

data class CellularState(
    /** False on devices with no telephony radio (Wi-Fi-only tablets, etc.). */
    val isAvailable: Boolean,
    /** SIM slot this state was read from (0-based). Only meaningful when the device exposes
     * per-subscription info; -1 if there's just one, slot-agnostic reading. */
    val slotIndex: Int = -1,
    val carrierName: String?,
    /** Null unless the Phone State permission has been granted. */
    val networkTypeName: String?,
    /** 0-4. Null below Android 9 or without the Phone State permission. */
    val signalLevel: Int?,
    /** Raw signal strength in dBm, from [android.telephony.CellSignalStrength.getDbm]. */
    val signalDbm: Int?,
    val isRoaming: Boolean?,
    /** e.g. "Ready", "Absent", from [android.telephony.TelephonyManager.getSimState]. */
    val simState: String?,
)
