package com.ai.wardrobe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.*
import androidx.navigation3.ui.NavDisplay
import com.ai.wardrobe.ui.styling.StylingScreen
import com.ai.wardrobe.ui.styling.StylingViewModel
import com.ai.wardrobe.ui.wardrobe.WardrobeGalleryScreen
import com.ai.wardrobe.ui.wardrobe.WardrobeViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKey {
    @Serializable
    data object Closet : NavKey()
    @Serializable
    data object Style : NavKey()
}

@Composable
fun MainScreen() {
    val wardrobeViewModel: WardrobeViewModel = viewModel()
    val stylingViewModel: StylingViewModel = viewModel()
    
    // Using a simple state for navigation since Nav3 seems to have API mismatches or is unavailable
    var currentKey by remember { mutableStateOf<NavKey>(NavKey.Closet) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentKey is NavKey.Closet,
                    onClick = { currentKey = NavKey.Closet },
                    icon = { Icon(Icons.Rounded.Checkroom, contentDescription = "Closet") },
                    label = { Text("Closet") }
                )
                NavigationBarItem(
                    selected = currentKey is NavKey.Style,
                    onClick = { currentKey = NavKey.Style },
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Style") },
                    label = { Text("Style") }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentKey) {
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
            }
        }
    }
}
