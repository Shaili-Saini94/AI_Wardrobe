package com.ai.wardrobe.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLabeler @Inject constructor() {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    suspend fun labelImage(context: Context, imageUri: Uri): List<String> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val labels = labeler.process(image).await()
            labels.map { it.text }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
