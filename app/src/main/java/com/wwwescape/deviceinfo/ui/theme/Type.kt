package com.wwwescape.deviceinfo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wwwescape.deviceinfo.R

/** Roboto Flex ships as two static instances (wght 400/500, every other axis at its default)
 * generated from the variable font — the only weights the app uses. The full variable font is
 * ~1.8 MB versus ~96 KB per instance, and static files also render the true Medium weight on
 * API 24-25, where variation settings are ignored. */
val RobotoFlexFamily = FontFamily(
    Font(R.font.roboto_flex_regular, weight = FontWeight.Normal),
    Font(R.font.roboto_flex_medium, weight = FontWeight.Medium),
)

/** Reserved for technical/numeric values (hardware IDs, sensor readings, byte counts) per
 * DESIGN.md's "label-mono" style — monospaced digits avoid 0/O and 1/l ambiguity. */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, weight = FontWeight.Medium),
)

/** [DESIGN.md]'s label-mono style: 12/16, w500, +0.5sp — applied directly at call sites that
 * render technical values, not through a named Typography role (Compose has no "mono" slot). */
val LabelMono = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp,
)

private val baseline = Typography()

val DeviceInfoTypography = baseline.copy(
    displayLarge = baseline.displayLarge.copy(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    // DESIGN.md's headline-lg-mobile: the app is mobile-only, so this *is* headlineLarge.
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = baseline.titleLarge.copy(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = baseline.titleMedium.copy(fontFamily = RobotoFlexFamily, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = RobotoFlexFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = RobotoFlexFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = RobotoFlexFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = RobotoFlexFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = RobotoFlexFamily),
)
