package com.wwwescape.deviceinfo.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DeviceInfoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceInfoWidget()

    /** First widget instance placed. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetRefreshWork(context)
    }

    /** Last widget instance removed — nothing left to keep fresh. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetRefreshWork(context)
    }
}
