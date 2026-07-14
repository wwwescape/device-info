package com.wwwescape.deviceinfo.ui.screens.cpu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.deviceinfo.data.cpu.CpuInfo
import com.wwwescape.deviceinfo.data.cpu.CpuLiveStats
import com.wwwescape.deviceinfo.data.cpu.CpuRepository
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CpuViewModel(application: Application) : AndroidViewModel(application) {

    val cpuInfo: CpuInfo = CpuRepository.collectStatic(application)

    private val refreshIntervalMillis = SettingsRepository.settingsFlow(application)
        .map { it.refreshInterval.cpuMillis }
        .distinctUntilChanged()

    val liveStats: StateFlow<CpuLiveStats> = refreshIntervalMillis
        .flatMapLatest { intervalMillis -> CpuRepository.liveStats(cpuInfo.coreCount, intervalMillis) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CpuLiveStats(loadPercent = null, coreFrequenciesMhz = List(cpuInfo.coreCount) { null }),
        )
}
