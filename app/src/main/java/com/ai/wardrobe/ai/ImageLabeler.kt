package com.ai.wardrobe.ai

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLabeler @Inject constructor(
    private val tflite: TFLiteClothingClassifier,
    private val gemini: GeminiClothingAnalyzer
) {
    /**
     * Detects ALL clothing items in the image using on-device TFLite crop analysis.
     * Falls back to Gemini only if TFLite finds nothing.
     */
    suspend fun analyzeAllItems(imageUri: Uri): List<ImageAnalysisResult> {
        val localResults = tflite.detectAll(imageUri)
        if (localResults.isNotEmpty()) return localResults
        return try {
            gemini.analyzeAllClothing(imageUri)
        } catch (e: Exception) {
            listOf(
                ImageAnalysisResult(
                    isClothing = false,
                    category = "",
                    tags = emptyList(),
                    debugReason = "No clothing detected"
                )
            )
        }
    }

    /** Single-item classify. */
    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        val local = tflite.classify(imageUri)
        if (local.isClothing) return local
        return try { gemini.analyzeClothing(imageUri) } catch (e: Exception) { local }
    }
}
