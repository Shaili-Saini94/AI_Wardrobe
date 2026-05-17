package com.ai.wardrobe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ai.wardrobe.ui.styling.StylingScreen
import com.ai.wardrobe.ui.styling.StylingViewModel
import com.ai.wardrobe.ui.wardrobe.WardrobeGalleryScreen
import com.ai.wardrobe.ui.wardrobe.WardrobeViewModel
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.ai.ImageLabeler
import com.ai.wardrobe.ai.PlatformCamera
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKey {
    @Serializable
    data object Closet : NavKey()
    @Serializable
    data object Style : NavKey()
}

@Composable
fun MainScreen(
    repository: WardrobeRepository,
    imageLabeler: ImageLabeler,
    platformCamera: PlatformCamera,
) {
    var currentTab by remember { mutableStateOf<NavKey>(NavKey.Closet) }
    
    // In a full CMP app, we'd use a Multiplatform ViewModel solution like Decompose or Koin
    // For this restructuring, we keep it simple with remember and Manual DI
    val wardrobeViewModel = remember { WardrobeViewModel(repository, imageLabeler) }
    val stylingViewModel = remember { StylingViewModel(repository) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab is NavKey.Closet,
                    onClick = { currentTab = NavKey.Closet },
                    icon = { Icon(Icons.Rounded.Checkroom, contentDescription = "Closet") },
                    label = { Text("Closet") }
                )
                NavigationBarItem(
                    selected = currentTab is NavKey.Style,
                    onClick = { currentTab = NavKey.Style },
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Style") },
                    label = { Text("Style") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                is NavKey.Closet -> {
                    WardrobeGalleryScreen(
                        viewModel = wardrobeViewModel,
                        platformCamera = platformCamera,
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
