package com.wwwescape.deviceinfo.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.deviceinfo.data.settings.AppSettings
import com.wwwescape.deviceinfo.data.settings.ColorTheme
import com.wwwescape.deviceinfo.data.settings.RefreshInterval
import com.wwwescape.deviceinfo.data.settings.SettingsRepository
import com.wwwescape.deviceinfo.data.settings.TemperatureUnit
import com.wwwescape.deviceinfo.data.settings.ThemeContrast
import com.wwwescape.deviceinfo.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { SettingsRepository.setThemeMode(getApplication(), mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { SettingsRepository.setDynamicColor(getApplication(), enabled) }
    }

    fun setColorTheme(theme: ColorTheme) {
        viewModelScope.launch { SettingsRepository.setColorTheme(getApplication(), theme) }
    }

    fun setThemeContrast(contrast: ThemeContrast) {
        viewModelScope.launch { SettingsRepository.setThemeContrast(getApplication(), contrast) }
    }

    fun setPureDark(enabled: Boolean) {
        viewModelScope.launch { SettingsRepository.setPureDark(getApplication(), enabled) }
    }

    fun setAbsoluteDark(enabled: Boolean) {
        viewModelScope.launch { SettingsRepository.setAbsoluteDark(getApplication(), enabled) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { SettingsRepository.setTemperatureUnit(getApplication(), unit) }
    }

    fun setRefreshInterval(interval: RefreshInterval) {
        viewModelScope.launch { SettingsRepository.setRefreshInterval(getApplication(), interval) }
    }
}
