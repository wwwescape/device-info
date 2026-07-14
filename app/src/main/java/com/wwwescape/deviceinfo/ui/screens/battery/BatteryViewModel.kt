package com.wwwescape.deviceinfo.ui.screens.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.deviceinfo.data.battery.BatteryInfo
import com.wwwescape.deviceinfo.data.battery.BatteryRepository
import com.wwwescape.deviceinfo.data.battery.ThermalInfo
import com.wwwescape.deviceinfo.data.battery.ThermalStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    // Event-driven (broadcast/listener), but ACTION_BATTERY_CHANGED still fires often while
    // charging — unregister once the screen stops being observed (backgrounded, or covered by
    // another destination) so this ViewModel never wakes the app up while it isn't visible.
    val batteryInfo: StateFlow<BatteryInfo> = BatteryRepository.batteryUpdates(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatteryRepository.currentBatteryInfo(application))

    val thermalInfo: StateFlow<ThermalInfo> = BatteryRepository.thermalUpdates(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThermalInfo(ThermalStatus.UNAVAILABLE))
}
