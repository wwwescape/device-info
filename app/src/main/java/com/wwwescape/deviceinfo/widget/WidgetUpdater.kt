package com.wwwescape.deviceinfo.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes every placed widget with a fresh snapshot; called when the app is opened. */
object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        DeviceInfoWidget().updateAll(context)
    }
}
