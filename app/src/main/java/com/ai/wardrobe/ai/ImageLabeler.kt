package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.fashionai.sdk.FashionAI
import com.fashionai.sdk.model.ClothingType
import com.fashionai.sdk.model.FashionAIMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ImageAnalysisResult(
    val isClothing: Boolean,
    val category: String,
    val tags: List<String>,
    val occasions: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val styleTypes: List<String> = emptyList(),
    val mood: String? = null,
    val weather: String? = null,
    val debugReason: String? = null
)

@Singleton
class ImageLabeler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val fashionAI = FashionAI.Builder(context)
        .setMode(FashionAIMode.ON_DEVICE)
        .setModelFileName("ai.tflite")
        .setConfidenceThreshold(0.01f) // Ultra-low to catch any possible garment
        .build()

    init {
        fashionAI.prepare()
    }

    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            val originalBitmap = loadBitmap(imageUri) ?: return errorResult("Load failed")

            // Multi-pass detection (Full image, 80% center, 60% center)
            val fullDetection = fashionAI.detectClothing(originalBitmap)
            val crop80 = smartCrop(originalBitmap, 0.8)
            val detection80 = fashionAI.detectClothing(crop80)
            val crop60 = smartCrop(originalBitmap, 0.6)
            val detection60 = fashionAI.detectClothing(crop60)

            // Pick the best signal
            val allDetections = listOf(fullDetection, detection80, detection60)
            var bestDetection = fullDetection
            for (det in allDetections) {
                val currentIsKnown = det.clothingType != ClothingType.UNKNOWN
                val bestIsKnown = bestDetection.clothingType != ClothingType.UNKNOWN
                
                if (currentIsKnown && !bestIsKnown) {
                    bestDetection = det
                } else if (currentIsKnown == bestIsKnown && det.confidence > bestDetection.confidence) {
                    bestDetection = det
                }
            }

            // Logic to accept
            val isKnownType = bestDetection.clothingType != ClothingType.UNKNOWN
            val hasKnownAlt = bestDetection.alternatives.any { it.clothingType != ClothingType.UNKNOWN }
            val isClothing = isKnownType || hasKnownAlt || bestDetection.confidence > 0.4f

            if (!isClothing) {
                return errorResult("No clothing signal")
            }

            // Categorize using the best source bitmap
            val sourceForCategorization = when (bestDetection) {
                detection80 -> crop80
                detection60 -> crop60
                else -> originalBitmap
            }
            
            val categoryResult = fashionAI.categorize(bestDetection, sourceForCategorization)
            
            val displayCategory = when {
                bestDetection.clothingType != ClothingType.UNKNOWN -> categoryResult.subType
                hasKnownAlt -> bestDetection.alternatives.first { it.clothingType != ClothingType.UNKNOWN }.label
                else -> bestDetection.subTypeRaw
            }

            ImageAnalysisResult(
                isClothing = true,
                category = formatCategoryName(displayCategory),
                tags = categoryResult.tags,
                occasions = categoryResult.occasions.map { it.name },
                seasons = categoryResult.seasons.map { it.name },
                styleTypes = categoryResult.styleTypes.map { it.name },
                mood = "Relaxed", // Default mood inference
                weather = categoryResult.seasons.firstOrNull()?.name ?: "Sunny"
            )
        } catch (e: Exception) {
            Log.e("ImageLabeler", "Error", e)
            errorResult("System error")
        }
    }

    private fun formatCategoryName(name: String): String {
        return name.lowercase()
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun smartCrop(source: Bitmap, factor: Double): Bitmap {
        val width = source.width
        val height = source.height
        val newWidth = (width * factor).toInt()
        val newHeight = (height * factor).toInt()
        val startX = (width - newWidth) / 2
        val startY = (height - newHeight) / 2
        return Bitmap.createBitmap(source, startX, startY, newWidth, newHeight)
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) { null }
    }

    private fun errorResult(reason: String) = ImageAnalysisResult(
        isClothing = false,
        category = "Unknown",
        tags = emptyList(),
        debugReason = reason
    )
    
    private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
