package com.ai.wardrobe

import androidx.compose.runtime.*
import com.ai.wardrobe.ui.MainScreen
import com.ai.wardrobe.ui.theme.AIWardrobeTheme
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.ai.ImageLabeler
import com.ai.wardrobe.ai.PlatformCamera

@Composable
fun App(
    repository: WardrobeRepository,
    imageLabeler: ImageLabeler,
    platformCamera: PlatformCamera
) {
    AIWardrobeTheme {
        MainScreen(
            repository = repository,
            imageLabeler = imageLabeler,
            platformCamera = platformCamera
        )
    }
}
