package com.ai.wardrobe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.wardrobe.ui.dashboard.DashboardScreen
import com.ai.wardrobe.ui.dashboard.DashboardViewModel
import com.ai.wardrobe.ui.onboarding.OnboardingScreen
import com.ai.wardrobe.ui.styling.StylingScreen
import com.ai.wardrobe.ui.styling.StylingViewModel
import com.ai.wardrobe.ui.wardrobe.WardrobeGalleryScreen
import com.ai.wardrobe.ui.wardrobe.WardrobeViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKey {
    @Serializable
    data object Onboarding : NavKey()
    @Serializable
    data object Dashboard : NavKey()
    @Serializable
    data object Closet : NavKey()
    @Serializable
    data object Style : NavKey()
    @Serializable
    data object Profile : NavKey()
}

@Composable
fun MainScreen() {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val wardrobeViewModel: WardrobeViewModel = hiltViewModel()
    val stylingViewModel: StylingViewModel = hiltViewModel()
    
    // Set Dashboard as the initial landing screen after onboarding
    var currentKey by remember { mutableStateOf<NavKey>(NavKey.Onboarding) }

    Scaffold(
        bottomBar = {
            if (currentKey !is NavKey.Onboarding) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = currentKey is NavKey.Dashboard,
                        onClick = { currentKey = NavKey.Dashboard },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("HOME") }
                    )
                    NavigationBarItem(
                        selected = currentKey is NavKey.Closet,
                        onClick = { currentKey = NavKey.Closet },
                        icon = { Icon(Icons.Rounded.Checkroom, contentDescription = "Wardrobe") },
                        label = { Text("WARDROBE") }
                    )
                    NavigationBarItem(
                        selected = currentKey is NavKey.Style,
                        onClick = { currentKey = NavKey.Style },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Stylist") },
                        label = { Text("STYLIST") }
                    )
                    NavigationBarItem(
                        selected = currentKey is NavKey.Profile,
                        onClick = { currentKey = NavKey.Profile },
                        icon = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                        label = { Text("PROFILE") }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentKey) {
                is NavKey.Onboarding -> {
                    OnboardingScreen(
                        onStartScanning = { currentKey = NavKey.Dashboard },
                        onSkip = { currentKey = NavKey.Dashboard }
                    )
                }
                is NavKey.Dashboard -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onScanClick = { currentKey = NavKey.Closet },
                        onStyleClick = { currentKey = NavKey.Style }
                    )
                }
                is NavKey.Closet -> {
                    WardrobeGalleryScreen(
                        viewModel = wardrobeViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.Style -> {
                    StylingScreen(
                        viewModel = stylingViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.Profile -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Profile Screen")
                    }
                }
            }
        }
    }
}
