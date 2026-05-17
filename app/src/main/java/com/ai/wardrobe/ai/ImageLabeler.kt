package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        .setConfidenceThreshold(0.15f) // Very low threshold to capture any signal
        .build()

    init {
        fashionAI.prepare()
    }

    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            val bitmap = loadBitmap(imageUri) ?: return errorResult()

            // Detect clothing
            val detection = fashionAI.detectClothing(bitmap)
            
            // Smart detection: Check if the top result OR any of the alternatives are clothing
            val isClothing = detection.clothingType != ClothingType.UNKNOWN || 
                           detection.alternatives.any { it.clothingType != ClothingType.UNKNOWN } ||
                           detection.confidence > 0.5f

            if (!isClothing) {
                return errorResult()
            }

            // Categorize
            val categoryResult = fashionAI.categorize(detection, bitmap)
            
            // If the primary detection was UNKNOWN but we found an alternative, use the best alternative
            val finalCategory = if (detection.clothingType == ClothingType.UNKNOWN) {
                detection.alternatives.firstOrNull { it.clothingType != ClothingType.UNKNOWN }?.label ?: categoryResult.subType
            } else {
                categoryResult.subType
            }

            ImageAnalysisResult(
                isClothing = true,
                category = finalCategory.replace("_", " ").capitalize(),
                tags = categoryResult.tags
            )
        } catch (e: Exception) {
            errorResult()
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun errorResult() = ImageAnalysisResult(
        isClothing = false,
        category = "Unknown",
        tags = emptyList()
    )
    
    private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
