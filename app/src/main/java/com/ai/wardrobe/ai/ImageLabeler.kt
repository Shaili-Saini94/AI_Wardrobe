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
    val tags: List<String>
)

@Singleton
class ImageLabeler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val fashionAI = FashionAI.Builder(context)
        .setMode(FashionAIMode.ON_DEVICE)
        .setModelFileName("ai.tflite")
        .setConfidenceThreshold(0.05f) // Ultra-sensitive for detection phase
        .build()

    init {
        fashionAI.prepare()
    }

    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            val originalBitmap = loadBitmap(imageUri) ?: return errorResult()
            Log.d("ImageLabeler", "Analyzing image: ${originalBitmap.width}x${originalBitmap.height}")

            // 1. Multi-pass detection (Full image, 80% center, 60% center)
            val fullDetection = fashionAI.detectClothing(originalBitmap)
            val crop80 = smartCrop(originalBitmap, 0.8)
            val detection80 = fashionAI.detectClothing(crop80)
            val crop60 = smartCrop(originalBitmap, 0.6)
            val detection60 = fashionAI.detectClothing(crop60)

            // 2. Pick the best detection signal
            val allDetections = listOf(fullDetection, detection80, detection60)
            
            // Logic: Prefer results that identified a clothing type over UNKNOWN, 
            // then prefer higher confidence.
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

            Log.d("ImageLabeler", "Best detection: ${bestDetection.subTypeDisplay} (Conf: ${bestDetection.confidence})")

            // 3. Robust verification: Is it clothing?
            // We accept if:
            // - ClothingType is known
            // - OR any alternative is a known clothing type
            // - OR it's a generic high-confidence "garment/fabric" signal
            val hasClothingSignal = bestDetection.clothingType != ClothingType.UNKNOWN || 
                                   bestDetection.alternatives.any { it.clothingType != ClothingType.UNKNOWN } ||
                                   bestDetection.confidence > 0.4f

            if (!hasClothingSignal) {
                Log.w("ImageLabeler", "No clothing signal found. Top label: ${bestDetection.subTypeRaw}")
                return errorResult()
            }

            // 4. Categorize using the best source bitmap
            val sourceForCategorization = when (bestDetection) {
                detection80 -> crop80
                detection60 -> crop60
                else -> originalBitmap
            }
            
            val categoryResult = fashionAI.categorize(bestDetection, sourceForCategorization)
            
            // 5. Final Category Name Logic
            val displayCategory = when {
                bestDetection.clothingType != ClothingType.UNKNOWN -> categoryResult.subType
                bestDetection.alternatives.any { it.clothingType != ClothingType.UNKNOWN } -> {
                    bestDetection.alternatives.first { it.clothingType != ClothingType.UNKNOWN }.label
                }
                else -> "Garment"
            }

            ImageAnalysisResult(
                isClothing = true,
                category = displayCategory.replace("_", " ").capitalize(),
                tags = categoryResult.tags
            )
        } catch (e: Exception) {
            Log.e("ImageLabeler", "Error analyzing image", e)
            errorResult()
        }
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
