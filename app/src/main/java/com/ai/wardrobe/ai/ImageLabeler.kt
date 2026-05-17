package com.ai.wardrobe.ai

import android.content.Context
import android.net.Uri
import com.fashionai.sdk.FashionAI
import com.fashionai.sdk.model.ClothingType
import com.fashionai.sdk.model.FashionAIMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ImageAnalysisResult(
    val isClothing: Boolean,
    val category: String,
    val tags: List<String>
)

@Singleton
class ImageLabeler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val fashionAI = FashionAI.Builder(context)
        .setMode(FashionAIMode.ON_DEVICE)
        .setModelFileName("ai.tflite")
        .setConfidenceThreshold(0.3f) // Lower threshold to detect items even on people
        .build()

    init {
        fashionAI.prepare()
    }

    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            val detection = fashionAI.detectClothing(imageUri)
            
            // Check if it's any valid clothing type (Top, Bottom, Footwear, etc.)
            val isClothing = detection.clothingType != ClothingType.UNKNOWN && detection.confidence > 0.3f
            
            if (!isClothing) {
                return ImageAnalysisResult(
                    isClothing = false,
                    category = "Unknown",
                    tags = emptyList()
                )
            }

            val categoryResult = fashionAI.categorize(detection)
            
            ImageAnalysisResult(
                isClothing = true,
                category = categoryResult.subType,
                tags = categoryResult.tags
            )
        } catch (e: Exception) {
            ImageAnalysisResult(
                isClothing = false,
                category = "Error",
                tags = emptyList()
            )
        }
    }
}
