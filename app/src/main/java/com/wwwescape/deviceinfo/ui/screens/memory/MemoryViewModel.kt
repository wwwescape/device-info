package com.wwwescape.deviceinfo.ui.screens.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.deviceinfo.data.memory.MemoryLiveStats
import com.wwwescape.deviceinfo.data.memory.MemoryRepository
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val refreshIntervalMillis = SettingsRepository.settingsFlow(application)
        .map { it.refreshInterval.memoryMillis }
        .distinctUntilChanged()

    val liveStats: StateFlow<MemoryLiveStats> = refreshIntervalMillis
        .flatMapLatest { intervalMillis -> MemoryRepository.liveStats(application, intervalMillis) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MemoryLiveStats(
                ram = MemoryRepository.readRamInfo(application),
                storageVolumes = emptyList(),
            ),
        )
}
