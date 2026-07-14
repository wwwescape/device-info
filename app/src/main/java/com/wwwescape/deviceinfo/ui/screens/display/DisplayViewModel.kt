package com.wwwescape.deviceinfo.ui.screens.display

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wwwescape.deviceinfo.data.display.DisplayInfo
import com.wwwescape.deviceinfo.data.display.DisplayRepository

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    val displayInfo: DisplayInfo = DisplayRepository.collectStatic(application)
}
