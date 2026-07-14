package com.wwwescape.deviceinfo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// DESIGN.md's rounded scale (sm/DEFAULT/md/lg) maps directly to Compose's 5 shape slots;
// extraLarge uses the prose's explicit "28px" main-container value rather than the scale's
// xl=24px, since that's specifically what cards/hero surfaces (the most visible shape in the
// app) are specified to use.
val DeviceInfoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** DESIGN.md's pill/"full" shape for buttons, chips, and nav-item indicators. */
val PillShape = RoundedCornerShape(percent = 50)
