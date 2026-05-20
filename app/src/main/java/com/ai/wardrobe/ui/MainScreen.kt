package com.ai.wardrobe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.wardrobe.ui.calendar.CalendarScreen
import com.ai.wardrobe.ui.calendar.CalendarViewModel
import com.ai.wardrobe.ui.dashboard.DashboardScreen
import com.ai.wardrobe.ui.dashboard.DashboardViewModel
import com.ai.wardrobe.ui.insights.InsightsScreen
import com.ai.wardrobe.ui.insights.InsightsViewModel
import com.ai.wardrobe.ui.onboarding.OnboardingScreen
import com.ai.wardrobe.ui.profile.ProfileScreen
import com.ai.wardrobe.ui.styling.StylingScreen
import com.ai.wardrobe.ui.styling.StylingViewModel
import com.ai.wardrobe.ui.theme.*
import com.ai.wardrobe.ui.wardrobe.WardrobeGalleryScreen
import com.ai.wardrobe.ui.wardrobe.WardrobeViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKey {
    @Serializable data object Onboarding : NavKey()
    @Serializable data object Dashboard  : NavKey()
    @Serializable data object Closet     : NavKey()
    @Serializable data object Style      : NavKey()
    @Serializable data object Insights   : NavKey()
    @Serializable data object Calendar   : NavKey()
    @Serializable data object Profile    : NavKey()
}

@Composable
fun MainScreen() {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val wardrobeViewModel: WardrobeViewModel   = hiltViewModel()
    val stylingViewModel: StylingViewModel     = hiltViewModel()
    val calendarViewModel: CalendarViewModel   = hiltViewModel()
    val insightsViewModel: InsightsViewModel   = hiltViewModel()

    var currentKey by remember { mutableStateOf<NavKey>(NavKey.Onboarding) }

    Scaffold(
        bottomBar = {
            if (currentKey !is NavKey.Onboarding && currentKey !is NavKey.Profile) {
                NavigationBar(
                    windowInsets   = WindowInsets.navigationBars,
                    containerColor = ClosetNavBg,
                    tonalElevation = 0.dp,
                    modifier       = Modifier.height(80.dp)
                ) {
                    ClosetNavItem(
                        selected = currentKey is NavKey.Dashboard,
                        onClick  = { currentKey = NavKey.Dashboard },
                        icon     = { Icon(Icons.Rounded.Home, "Home", modifier = Modifier.size(22.dp)) },
                        label    = "HOME"
                    )
                    ClosetNavItem(
                        selected = currentKey is NavKey.Closet,
                        onClick  = { currentKey = NavKey.Closet },
                        icon     = { Icon(Icons.Rounded.Checkroom, "Wardrobe", modifier = Modifier.size(20.dp)) },
                        label    = "WARDROBE"
                    )
                    ClosetNavItem(
                        selected = currentKey is NavKey.Style,
                        onClick  = { currentKey = NavKey.Style },
                        icon     = { Icon(Icons.Rounded.AutoAwesome, "Stylist", modifier = Modifier.size(22.dp)) },
                        label    = "STYLIST"
                    )
                    ClosetNavItem(
                        selected = currentKey is NavKey.Insights,
                        onClick  = { currentKey = NavKey.Insights },
                        icon     = { Icon(Icons.Rounded.Lightbulb, "Insights", modifier = Modifier.size(20.dp)) },
                        label    = "INSIGHTS"
                    )
                    ClosetNavItem(
                        selected = currentKey is NavKey.Calendar,
                        onClick  = { currentKey = NavKey.Calendar },
                        icon     = { Icon(Icons.Rounded.CalendarMonth, "Planner", modifier = Modifier.size(20.dp)) },
                        label    = "PLANNER"
                    )
                }
            }
        },
        containerColor      = ClosetBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentKey) {
                is NavKey.Onboarding -> OnboardingScreen(
                    onStartScanning = { currentKey = NavKey.Dashboard },
                    onSkip          = { currentKey = NavKey.Dashboard }
                )
                is NavKey.Dashboard -> DashboardScreen(
                    viewModel    = dashboardViewModel,
                    onScanClick  = { currentKey = NavKey.Closet },
                    onStyleClick = { currentKey = NavKey.Style },
                    onInsightsClick = { currentKey = NavKey.Insights }
                )
                is NavKey.Closet -> WardrobeGalleryScreen(
                    viewModel = wardrobeViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
                is NavKey.Style -> StylingScreen(
                    viewModel = stylingViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
                is NavKey.Insights -> InsightsScreen(
                    viewModel = insightsViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
                is NavKey.Calendar -> CalendarScreen(
                    viewModel = calendarViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
                is NavKey.Profile -> ProfileScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RowScope.ClosetNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick  = onClick,
        icon     = icon,
        label    = {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = ClosetBlack,
            selectedTextColor   = ClosetBlack,
            unselectedIconColor = ClosetInactive,
            unselectedTextColor = ClosetInactive,
            indicatorColor      = Color.Transparent
        )
    )
}
