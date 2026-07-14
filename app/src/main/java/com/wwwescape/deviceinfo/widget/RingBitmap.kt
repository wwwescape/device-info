package com.wwwescape.deviceinfo.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

/**
 * Glance's [androidx.glance.appwidget.CircularProgressIndicator] is indeterminate-only (no
 * `progress` param) — there is no determinate ring in the Glance API surface. Rendering one as
 * a plain [Bitmap] via [android.graphics.Canvas] is the standard workaround. Takes the widget's
 * already-resolved [ColorScheme] (see [com.wwwescape.deviceinfo.ui.theme.resolveDeviceInfoColorScheme])
 * rather than deriving its own, so the ring always matches the rest of the card exactly.
 */
fun ringBitmap(colorScheme: ColorScheme, fraction: Float, sizeDp: Int, density: Float): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val ringColor = colorScheme.primary.toArgb()
    val trackColor = colorScheme.primary.copy(alpha = 0.15f).toArgb()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val strokePx = sizePx * 0.125f
    val rect = RectF(strokePx / 2, strokePx / 2, sizePx - strokePx / 2, sizePx - strokePx / 2)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = trackColor
    }
    canvas.drawArc(rect, -90f, 360f, false, trackPaint)

    val progressPaint = Paint(trackPaint).apply { color = ringColor }
    canvas.drawArc(rect, -90f, 360f * fraction.coerceIn(0f, 1f), false, progressPaint)

    return bitmap
}
