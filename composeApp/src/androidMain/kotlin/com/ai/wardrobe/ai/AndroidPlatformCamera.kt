package com.ai.wardrobe.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.ai.wardrobe.util.FileUtil

class AndroidPlatformCamera : PlatformCamera {
    @Composable
    override fun rememberCameraLauncher(onImageCaptured: (String) -> Unit): () -> Unit {
        val context = LocalContext.current
        var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
        
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                capturedImageUri?.let { onImageCaptured(it.toString()) }
            }
        }
        
        return {
            val file = FileUtil.createImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            capturedImageUri = uri
            launcher.launch(uri)
        }
    }

    @Composable
    override fun rememberGalleryLauncher(onImageSelected: (String) -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { onImageSelected(it.toString()) }
        }
        
        return {
            launcher.launch("image/*")
        }
    }
}
