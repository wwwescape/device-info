package com.wwwescape.deviceinfo.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/** True on Android TV / Google TV devices (leanback UI mode or the leanback system feature). */
fun Context.isTelevision(): Boolean {
    val uiModeManager = getSystemService(UiModeManager::class.java)
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
