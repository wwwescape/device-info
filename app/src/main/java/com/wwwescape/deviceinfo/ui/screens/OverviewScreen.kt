package com.wwwescape.deviceinfo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wwwescape.deviceinfo.ui.components.CategoryGrid
import com.wwwescape.deviceinfo.ui.components.DashboardHero
import com.wwwescape.deviceinfo.ui.components.LiveMetricsSection
import com.wwwescape.deviceinfo.ui.navigation.Destination

/** The tabless Dashboard: a hero device summary, the full category grid, and Live Metrics. */
@Composable
fun OverviewScreen(
    onCategoryClick: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    CategoryGrid(
        categories = Destination.detailScreens,
        onCategoryClick = onCategoryClick,
        modifier = modifier,
        header = { DashboardHero() },
        footer = { LiveMetricsSection() },
    )
}
