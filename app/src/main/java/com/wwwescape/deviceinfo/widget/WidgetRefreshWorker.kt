package com.wwwescape.deviceinfo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Refreshes placed widgets in the background. This is the widget's only periodic refresh:
 * `updatePeriodMillis` is 0 in the widget XML because those alarms wake a dozing device, whereas
 * WorkManager defers to Doze/App Standby and batches with other jobs. 15 minutes is WorkManager's
 * periodic floor. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.refreshAll(applicationContext)
        return Result.success()
    }
}

private const val WIDGET_REFRESH_WORK_NAME = "widget_periodic_refresh"

/** Idempotent — safe to call on every app launch; [ExistingPeriodicWorkPolicy.KEEP] leaves an
 * already-scheduled job alone rather than restarting its interval. */
fun scheduleWidgetRefreshWork(context: Context) {
    val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WIDGET_REFRESH_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

fun cancelWidgetRefreshWork(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WIDGET_REFRESH_WORK_NAME)
}

/** Schedules the periodic refresh only while at least one widget is placed, and cancels it
 * otherwise — so users who never add the widget never pay for a background job. Covers installs
 * that scheduled it unconditionally before [DeviceInfoWidgetReceiver.onEnabled] took over. */
fun syncWidgetRefreshWork(context: Context) {
    val ids = runCatching {
        AppWidgetManager.getInstance(context)
            ?.getAppWidgetIds(ComponentName(context, DeviceInfoWidgetReceiver::class.java))
    }.getOrNull()
    if (ids != null && ids.isNotEmpty()) scheduleWidgetRefreshWork(context) else cancelWidgetRefreshWork(context)
}
