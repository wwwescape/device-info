package com.wwwescape.deviceinfo.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wwwescape.deviceinfo.R

@Composable
fun formatUptime(millis: Long): String {
    val totalSeconds = millis / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    val daysStr = stringResource(R.string.uptime_days_format, days)
    val hoursStr = stringResource(R.string.uptime_hours_format, hours)
    val minutesStr = stringResource(R.string.uptime_minutes_format, minutes)
    val secondsStr = stringResource(R.string.uptime_seconds_format, seconds)

    return buildString {
        if (days > 0) append("$daysStr ")
        if (days > 0 || hours > 0) append("$hoursStr ")
        if (days > 0 || hours > 0 || minutes > 0) append("$minutesStr ")
        append(secondsStr)
    }
}
