package com.wwwescape.deviceinfo.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

fun fractionOf(used: Long, total: Long): Float? =
    if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null

/** The small uppercase label chip above the device name, mirroring [DashboardHero]'s
 * "DEVICE OVERVIEW" badge — same padding, and labelSmall's 11sp/Medium (DeviceInfoTypography). */
@Composable
fun WidgetPillBadge(text: String, contentColor: ColorProvider, containerColor: ColorProvider) {
    Box(
        modifier = GlanceModifier
            .background(containerColor)
            .cornerRadius(50.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor),
        )
    }
}

/** A circular-gauge stat card — a compact re-creation of [DashboardHero]'s `HeroGauge`: a ring
 * (rendered via [ringBitmap], since Glance has no determinate circular indicator) with the
 * percentage centered inside, and a caption below. */
@Composable
fun WidgetHeroGauge(
    ringImage: android.graphics.Bitmap,
    ringSizeDp: Int,
    percentText: String,
    caption: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    // Matches HeroGauge's Card(shape = large, padding = 16.dp).
    Box(
        modifier = modifier
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(ringSizeDp.dp)) {
                Image(
                    provider = ImageProvider(ringImage),
                    contentDescription = null,
                    modifier = GlanceModifier.size(ringSizeDp.dp),
                )
                Text(
                    // labelLarge: 14sp, Medium weight (M3 baseline, unmodified by DeviceInfoTypography).
                    text = percentText,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                // labelSmall: 11sp, Medium weight.
                text = caption.uppercase(),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}
