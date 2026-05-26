package com.ai.wardrobe.ai

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLabeler @Inject constructor(
    private val gemini: GeminiClothingAnalyzer,
    private val preValidator: ImagePreValidator
) {
    /**
     * Detects ALL clothing items using Gemini Vision AI.
     * TFLite has been removed — Gemini is the sole classifier.
     *
     * Pre-validation rejects images that contain multiple people or screenshots
     * before the API call is made.
     */
    suspend fun analyzeAllItems(imageUri: Uri): List<ImageAnalysisResult> {

        // ── 1. Pixel-based pre-validation (screenshot + multi-person) ─────────
        val validation = preValidator.validate(imageUri)
        if (validation is ImagePreValidator.Result.Rejected) {
            return listOf(rejected(validation.reason))
        }

        // ── 2. Gemini Vision AI — sole classifier ──────────────────────────────
        return try {
            gemini.analyzeAllClothing(imageUri)
        } catch (e: Exception) {
            listOf(rejected("AI analysis unavailable right now — please check your internet and try again in a moment"))
        }
    }

    /** Single-item classify via Gemini. */
    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            gemini.analyzeClothing(imageUri)
        } catch (e: Exception) {
            ImageAnalysisResult(
                isClothing  = false,
                category    = "",
                tags        = emptyList(),
                debugReason = "AI analysis unavailable right now — please check your internet and try again"
            )
        }
    }

    private fun rejected(reason: String) = ImageAnalysisResult(
        isClothing  = false,
        category    = "",
        tags        = emptyList(),
        debugReason = reason
    )
}
