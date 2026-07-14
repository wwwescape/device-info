package com.wwwescape.deviceinfo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A minimal single-series sparkline: line + soft fill + a highlighted end dot, no axes or
 * legend. Callers gate on `values.size >= 2` themselves so each domain can show its own
 * "collecting…" placeholder text instead of an empty chart.
 */
@Composable
fun SparklineChart(
    values: List<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = lineColor.copy(alpha = 0.1f)
    val ringColor = MaterialTheme.colorScheme.surfaceContainer

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 8.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val stepX = size.width / (values.size - 1)
        fun yFor(fraction: Float): Float = size.height - fraction.coerceIn(0f, 1f) * size.height

        val linePath = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = yFor(value)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(areaPath, color = areaColor)
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val endCenter = Offset(x = (values.size - 1) * stepX, y = yFor(values.last()))
        drawCircle(color = ringColor, radius = 6.dp.toPx(), center = endCenter)
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = endCenter)
    }
}
