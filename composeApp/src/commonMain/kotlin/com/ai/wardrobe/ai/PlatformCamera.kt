package com.ai.wardrobe.ai

import androidx.compose.runtime.Composable

interface PlatformCamera {
    @Composable
    fun rememberCameraLauncher(onImageCaptured: (String) -> Unit): () -> Unit
    
    @Composable
    fun rememberGalleryLauncher(onImageSelected: (String) -> Unit): () -> Unit
}
