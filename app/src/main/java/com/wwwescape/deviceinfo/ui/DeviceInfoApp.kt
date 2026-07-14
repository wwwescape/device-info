package com.wwwescape.deviceinfo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wwwescape.deviceinfo.R
import com.wwwescape.deviceinfo.ui.components.CenteredCollapsingTopBar
import com.wwwescape.deviceinfo.ui.navigation.Destination
import com.wwwescape.deviceinfo.ui.screens.OverviewScreen
import com.wwwescape.deviceinfo.ui.screens.battery.BatteryScreen
import com.wwwescape.deviceinfo.ui.screens.camera.CameraScreen
import com.wwwescape.deviceinfo.ui.screens.cpu.CpuScreen
import com.wwwescape.deviceinfo.ui.screens.deviceos.DeviceOsScreen
import com.wwwescape.deviceinfo.ui.screens.display.DisplayScreen
import com.wwwescape.deviceinfo.ui.screens.memory.MemoryScreen
import com.wwwescape.deviceinfo.ui.screens.network.NetworkScreen
import com.wwwescape.deviceinfo.ui.screens.sensors.SensorsScreen
import com.wwwescape.deviceinfo.ui.screens.settings.LicensesScreen
import com.wwwescape.deviceinfo.ui.screens.settings.SettingsScreen

private const val LICENSES_ROUTE = "licenses"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoApp(
    navController: NavHostController = rememberNavController(),
    startDestinationRoute: String? = null,
    onStartDestinationConsumed: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = Destination.fromRoute(currentRoute)
    val isOverview = currentRoute == Destination.Overview.route
    val isSettingsFamily = currentRoute == Destination.Settings.route ||
        currentRoute == LICENSES_ROUTE

    // A widget tap arrives with a target route — navigate there once, then clear it so backing
    // out doesn't re-trigger the jump (and so a later widget tap on the same route still fires).
    LaunchedEffect(startDestinationRoute) {
        if (startDestinationRoute != null) {
            navController.navigate(startDestinationRoute) { launchSingleTop = true }
            onStartDestinationConsumed()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val titleText = when {
        isOverview -> stringResource(R.string.title_overview)
        currentRoute == LICENSES_ROUTE -> stringResource(R.string.section_open_source_licenses)
        else -> stringResource(currentDestination.titleRes)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenteredCollapsingTopBar(
                title = titleText,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (isOverview) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo_mark),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(28.dp),
                        )
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (!isSettingsFamily) {
                        IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.title_settings),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Overview.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Overview.route) {
                OverviewScreen(onCategoryClick = { destination -> navController.navigate(destination.route) })
            }
            composable(Destination.DeviceOs.route) {
                DeviceOsScreen()
            }
            composable(Destination.Cpu.route) {
                CpuScreen()
            }
            composable(Destination.Memory.route) {
                MemoryScreen()
            }
            composable(Destination.Battery.route) {
                BatteryScreen()
            }
            composable(Destination.Display.route) {
                DisplayScreen()
            }
            composable(Destination.Network.route) {
                NetworkScreen()
            }
            composable(Destination.Sensors.route) {
                SensorsScreen()
            }
            composable(Destination.Camera.route) {
                CameraScreen()
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onNavigateToLicenses = { navController.navigate(LICENSES_ROUTE) },
                )
            }
            composable(LICENSES_ROUTE) {
                LicensesScreen()
            }
        }
    }
}
