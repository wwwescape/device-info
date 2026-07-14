package com.wwwescape.deviceinfo.data.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.wwwescape.deviceinfo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.Inet4Address
import java.net.Inet6Address

/**
 * Reads connectivity, Wi-Fi, cellular, Bluetooth and NFC state. Several fields (Wi-Fi SSID,
 * cellular signal/network type, paired Bluetooth device names) are gated behind runtime
 * permissions the Network screen requests contextually — every read here checks first and
 * fails soft to null rather than crashing on a SecurityException.
 */
object NetworkRepository {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPhoneStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBluetoothConnectPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun snapshot(context: Context): NetworkSnapshot {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        val linkProperties = activeNetwork?.let(connectivityManager::getLinkProperties)
        val connectionType = resolveConnectionType(capabilities)

        return NetworkSnapshot(
            connectionType = connectionType,
            ipAddress = readLocalIpAddress(linkProperties),
            ipv6Address = readLocalIpv6Address(linkProperties),
            dnsServers = linkProperties?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty(),
            wifi = readWifiState(context, isConnectedByCapabilities = connectionType == ConnectionType.WIFI),
            cellularSims = readCellularSims(context),
            bluetooth = readBluetoothState(context),
            hasNfc = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            isNfcEnabled = NfcAdapter.getDefaultAdapter(context)?.isEnabled ?: false,
        )
    }

    /** Emits real device-wide download/upload throughput in Mb/s, sampled from [TrafficStats]'s
     * cumulative byte counters roughly every [intervalMillis]. */
    fun trafficUpdates(intervalMillis: Long = 1_000L): Flow<NetworkThroughput> = flow {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(intervalMillis)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val seconds = intervalMillis / 1000f
            val downMbps = bytesDeltaToMbps(rx, previousRx, seconds)
            val upMbps = bytesDeltaToMbps(tx, previousTx, seconds)
            previousRx = rx
            previousTx = tx
            emit(NetworkThroughput(downMbps = downMbps, upMbps = upMbps))
        }
    }.flowOn(Dispatchers.IO)

    private fun bytesDeltaToMbps(current: Long, previous: Long, seconds: Float): Float {
        if (current == TrafficStats.UNSUPPORTED.toLong() || previous == TrafficStats.UNSUPPORTED.toLong()) return 0f
        val deltaBytes = (current - previous).coerceAtLeast(0)
        return (deltaBytes * 8f / 1_000_000f) / seconds
    }

    /** Emits whenever the active network changes, so the ViewModel can re-snapshot. */
    fun networkChangeEvents(context: Context): Flow<Unit> = callbackFlow {
        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(Unit)
            }

            override fun onLost(network: Network) {
                trySend(Unit)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun resolveConnectionType(capabilities: NetworkCapabilities?): ConnectionType = when {
        capabilities == null -> ConnectionType.NONE
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
        else -> ConnectionType.OTHER
    }

    private fun readLocalIpAddress(linkProperties: LinkProperties?): String? =
        linkProperties?.linkAddresses
            ?.mapNotNull { it.address as? Inet4Address }
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress

    private fun readLocalIpv6Address(linkProperties: LinkProperties?): String? =
        linkProperties?.linkAddresses
            ?.mapNotNull { it.address as? Inet6Address }
            ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?.hostAddress

    @Suppress("DEPRECATION")
    private fun readWifiState(context: Context, isConnectedByCapabilities: Boolean): WifiState {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
            ?: return WifiState(
                isConnected = false, ssid = null, linkSpeedMbps = null, frequencyMhz = null,
                signalLevel = null, rssiDbm = null, standard = null,
            )

        // Without Location permission, WifiManager obfuscates connectionInfo (networkId == -1,
        // ssid == "<unknown ssid>") even while genuinely connected, so trust the transport
        // capability check for isConnected rather than connectionInfo alone.
        val info = wifiManager.connectionInfo
        val isConnected = isConnectedByCapabilities || (info != null && info.networkId != -1)

        val ssid = if (isConnected && hasLocationPermission(context)) {
            info.ssid?.removeSurrounding("\"")?.takeUnless { it == "<unknown ssid>" }
        } else {
            null
        }

        return WifiState(
            isConnected = isConnected,
            ssid = ssid,
            linkSpeedMbps = if (isConnected) info.linkSpeed else null,
            frequencyMhz = if (isConnected) info.frequency else null,
            signalLevel = if (isConnected) WifiManager.calculateSignalLevel(info.rssi, 5) else null,
            rssiDbm = if (isConnected) info.rssi else null,
            standard = if (isConnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiStandardName(context, info.wifiStandard)
            } else {
                null
            },
        )
    }

    /** Wi-Fi generation names are fixed technical standard names — never translated. */
    private fun wifiStandardName(context: Context, standard: Int): String = when (standard) {
        ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7 (802.11be)"
        ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
        ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
        ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
        ScanResult.WIFI_STANDARD_11AD -> "802.11ad"
        ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
        else -> context.getString(R.string.value_unknown)
    }

    /** One entry per active SIM. Falls back to a single slot-agnostic reading when
     * per-subscription info isn't available (no Phone State permission, or a single-SIM
     * device that reports no active subscriptions). */
    @SuppressLint("MissingPermission")
    private fun readCellularSims(context: Context): List<CellularState> {
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val unavailable = CellularState(
            isAvailable = false, carrierName = null, networkTypeName = null, signalLevel = null,
            signalDbm = null, isRoaming = null, simState = null,
        )
        if (!hasTelephony) return listOf(unavailable)

        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            ?: return listOf(unavailable)

        val hasPhonePermission = hasPhoneStatePermission(context)
        val activeSubscriptions = if (hasPhonePermission) {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            runCatching { subscriptionManager?.activeSubscriptionInfoList }.getOrNull().orEmpty()
        } else {
            emptyList()
        }

        if (activeSubscriptions.isEmpty()) {
            return listOf(readCellularState(context, telephonyManager, hasPhonePermission, slotIndex = -1))
        }

        return activeSubscriptions
            .sortedBy { it.simSlotIndex }
            .map { subscriptionInfo ->
                val perSimTelephonyManager = telephonyManager.createForSubscriptionId(subscriptionInfo.subscriptionId)
                readCellularState(
                    context,
                    perSimTelephonyManager,
                    hasPhonePermission,
                    slotIndex = subscriptionInfo.simSlotIndex,
                    carrierNameOverride = subscriptionInfo.carrierName?.toString()?.takeUnless { it.isBlank() },
                )
            }
    }

    @SuppressLint("MissingPermission")
    private fun readCellularState(
        context: Context,
        telephonyManager: TelephonyManager,
        hasPhonePermission: Boolean,
        slotIndex: Int,
        carrierNameOverride: String? = null,
    ): CellularState {
        val carrierName = carrierNameOverride ?: telephonyManager.networkOperatorName?.takeUnless { it.isBlank() }
        val networkTypeName = if (hasPhonePermission) networkTypeName(context, telephonyManager.networkType) else null
        val signalStrength = if (hasPhonePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { telephonyManager.signalStrength }.getOrNull()
        } else {
            null
        }

        return CellularState(
            isAvailable = true,
            slotIndex = slotIndex,
            carrierName = carrierName,
            networkTypeName = networkTypeName,
            signalLevel = signalStrength?.level,
            signalDbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm
            } else {
                null
            },
            isRoaming = telephonyManager.isNetworkRoaming,
            simState = simStateName(context, telephonyManager.simState),
        )
    }

    /** SIM card state as a real, user-facing label. */
    private fun simStateName(context: Context, state: Int): String = when (state) {
        TelephonyManager.SIM_STATE_READY -> context.getString(R.string.sim_state_ready)
        TelephonyManager.SIM_STATE_ABSENT -> context.getString(R.string.sim_state_absent)
        TelephonyManager.SIM_STATE_PIN_REQUIRED,
        TelephonyManager.SIM_STATE_PUK_REQUIRED,
        TelephonyManager.SIM_STATE_NETWORK_LOCKED,
        -> context.getString(R.string.sim_state_locked)
        else -> context.getString(R.string.value_unknown)
    }

    @SuppressLint("MissingPermission")
    private fun readBluetoothState(context: Context): BluetoothState {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return BluetoothState(isSupported = false, isEnabled = false, pairedDeviceNames = emptyList())

        val isEnabled = adapter.isEnabled
        val canReadPairedDevices = isEnabled && hasBluetoothConnectPermission(context)
        val pairedDeviceNames = if (canReadPairedDevices) {
            runCatching { adapter.bondedDevices.map { it.name ?: it.address } }.getOrElse { emptyList() }
        } else {
            emptyList()
        }

        return BluetoothState(isSupported = true, isEnabled = isEnabled, pairedDeviceNames = pairedDeviceNames)
    }

    /** 5G/LTE/3G/2G are fixed technical generation names — never translated. */
    private fun networkTypeName(context: Context, type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        else -> context.getString(R.string.value_unknown)
    }
}
