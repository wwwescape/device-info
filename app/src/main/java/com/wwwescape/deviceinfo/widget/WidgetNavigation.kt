package com.wwwescape.deviceinfo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.wwwescape.deviceinfo.MainActivity

/** Intent extra carrying the [com.wwwescape.deviceinfo.ui.navigation.Destination] route a widget
 * tap should open — read by `MainActivity` and consumed by `DeviceInfoApp` to navigate there once. */
const val EXTRA_DESTINATION_ROUTE = "destination_route"

/** An [Intent] that opens the app directly on [route] — `FLAG_ACTIVITY_NEW_TASK` is required
 * since this launches from a widget's PendingIntent, outside any Activity context. */
fun widgetDestinationIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_DESTINATION_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

/** Reopens [DeviceInfoWidgetConfigActivity] for an already-placed widget instance, so its
 * Light/Dark/System choice can be changed after the fact (the system only shows a configuration
 * activity automatically once, at initial placement). */
fun widgetConfigIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, DeviceInfoWidgetConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
