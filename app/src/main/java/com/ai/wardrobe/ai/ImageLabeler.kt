package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.35f)
            .build()
    )

    suspend fun analyzeImage(imageUri: Uri): ImageAnalysisResult {
        return try {
            val rawBitmap = loadBitmap(imageUri)
                ?: return errorResult("Image load failed. Please try another image.")

            val originalBitmap = normalizeBitmap(rawBitmap)

            val bitmapsToAnalyze = listOf(
                originalBitmap,
                smartCrop(originalBitmap, 0.85),
                smartCrop(originalBitmap, 0.65)
            )

            val candidates = mutableListOf<LabelCandidate>()

            bitmapsToAnalyze.forEachIndexed { index, bitmap ->
                val labels = analyzeBitmap(bitmap)

                labels.forEach { candidate ->
                    candidates += candidate.copy(passIndex = index)
                }
            }

            if (candidates.isEmpty()) {
                return errorResult("No label detected. Try a clearer clothing photo.")
            }

            val sortedCandidates = candidates.sortedByDescending { it.confidence }

            Log.d(
                "ImageLabeler",
                "ML Kit Labels: ${sortedCandidates.joinToString { "${it.label}:${it.confidence}" }}"
            )

            val allLabelsText = sortedCandidates
                .joinToString(" ") { it.label }
                .lowercase()

            val normalized = ClothingTaxonomy.classify(
                rawLabel = allLabelsText,
                aiTags = sortedCandidates.map { it.label }
            ) ?: fallbackClassification(
                labelText = allLabelsText,
                bitmap = originalBitmap
            )

            if (normalized == null) {
                return errorResult(
                    "No valid clothing detected. Detected labels: ${
                        sortedCandidates.take(5).joinToString { it.label }
                    }"
                )
            }

            val strongestConfidence = sortedCandidates.firstOrNull()?.confidence ?: 0f

            val tags = (
                    normalized.tags +
                            normalized.group +
                            normalized.subcategory +
                            sortedCandidates.take(5).map { it.label }
                    )
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            ImageAnalysisResult(
                isClothing = true,
                category = normalized.subcategory,
                tags = tags,
                occasions = normalized.occasions,
                seasons = inferSeasons(normalized),
                styleTypes = inferStyleTypes(normalized),
                mood = "Stylish",
                weather = inferWeather(normalized),
                debugReason = "Detected ${normalized.subcategory} with confidence $strongestConfidence"
            )
        } catch (e: Exception) {
            Log.e("ImageLabeler", "Image analysis failed", e)
            errorResult("Failed to analyze image: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun analyzeBitmap(bitmap: Bitmap): List<LabelCandidate> {
        return withContext(Dispatchers.Default) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val labels = labeler.process(inputImage).await()

            labels.map {
                LabelCandidate(
                    label = it.text,
                    confidence = it.confidence,
                    passIndex = 0
                )
            }
        }
    }

    private fun fallbackClassification(
        labelText: String,
        bitmap: Bitmap
    ): ClothingClassification? {
        val hasGenericClothingHint = labelText.hasAny(
            "clothing",
            "fashion",
            "apparel",
            "garment",
            "textile",
            "outerwear",
            "footwear",
            "dress",
            "shirt",
            "shoe",
            "jeans",
            "pants"
        )

        val hasStrongNonClothingHint = labelText.hasAny(
            "food",
            "animal",
            "dog",
            "cat",
            "car",
            "vehicle",
            "furniture",
            "chair",
            "table",
            "building",
            "plant",
            "flower",
            "phone",
            "laptop"
        )

        if (!hasGenericClothingHint || hasStrongNonClothingHint) {
            return null
        }

        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        return when {
            labelText.hasAny("footwear", "shoe", "sneaker", "boot", "sandal", "heel") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Shoes",
                    occasions = listOf("Casual outing", "Party", "Office / Formal"),
                    tags = listOf("footwear", "shoes")
                )

            aspectRatio > 1.25f ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Shoes",
                    occasions = listOf("Casual outing", "Party"),
                    tags = listOf("footwear", "shoes")
                )

            aspectRatio < 0.65f ->
                ClothingClassification(
                    group = "One Piece",
                    subcategory = "Dress",
                    occasions = listOf("Party", "Date night", "Casual outing"),
                    tags = listOf("one-piece", "dress")
                )

            else ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Top",
                    occasions = listOf("Casual outing", "Party", "Date night"),
                    tags = listOf("top", "casual")
                )
        }
    }

    private fun inferSeasons(classification: ClothingClassification): List<String> {
        val text = searchableText(classification)

        return when {
            text.hasAny("hoodie", "jacket", "boots", "winter", "mountain") ->
                listOf("WINTER", "AUTUMN")

            text.hasAny("shorts", "sleeveless", "sandals", "beach", "summer") ->
                listOf("SUMMER", "SPRING")

            else ->
                listOf("ALL_SEASON")
        }
    }

    private fun inferStyleTypes(classification: ClothingClassification): List<String> {
        val text = searchableText(classification)

        return when {
            text.hasAny("office", "formal", "shirt", "blazer", "heels") ->
                listOf("FORMAL", "SMART")

            text.hasAny("party", "clubbing", "crop", "dress", "heels") ->
                listOf("PARTY", "STYLISH")

            text.hasAny("sports", "gym", "joggers", "sneakers") ->
                listOf("SPORTY", "CASUAL")

            text.hasAny("beach", "sandals", "shorts") ->
                listOf("BEACH", "CASUAL")

            else ->
                listOf("CASUAL")
        }
    }

    private fun inferWeather(classification: ClothingClassification): String {
        val text = searchableText(classification)

        return when {
            text.hasAny("hoodie", "jacket", "boots", "mountain") -> "Cold"
            text.hasAny("shorts", "sleeveless", "sandals", "beach") -> "Sunny"
            else -> "Any"
        }
    }

    private fun searchableText(classification: ClothingClassification): String {
        return buildString {
            append(classification.group)
            append(" ")
            append(classification.subcategory)
            append(" ")
            append(classification.tags.joinToString(" "))
            append(" ")
            append(classification.occasions.joinToString(" "))
        }.lowercase()
    }

    private fun normalizeBitmap(source: Bitmap): Bitmap {
        val maxDimension = 1024
        val width = source.width
        val height = source.height

        val alreadyValid = width <= maxDimension &&
                height <= maxDimension &&
                source.config == Bitmap.Config.ARGB_8888

        if (alreadyValid) return source

        val scale = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(
            source,
            0,
            0,
            width,
            height,
            matrix,
            true
        ).copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun smartCrop(source: Bitmap, factor: Double): Bitmap {
        val width = source.width
        val height = source.height

        val newWidth = (width * factor).toInt().coerceAtLeast(1)
        val newHeight = (height * factor).toInt().coerceAtLeast(1)

        val startX = ((width - newWidth) / 2).coerceAtLeast(0)
        val startY = ((height - newHeight) / 2).coerceAtLeast(0)

        return Bitmap.createBitmap(
            source,
            startX,
            startY,
            newWidth.coerceAtMost(width - startX),
            newHeight.coerceAtMost(height - startY)
        )
    }

    private suspend fun loadBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                Log.e("ImageLabeler", "Bitmap load failed", e)
                null
            }
        }
    }

    private fun errorResult(reason: String): ImageAnalysisResult {
        return ImageAnalysisResult(
            isClothing = false,
            category = "Unknown",
            tags = emptyList(),
            occasions = emptyList(),
            seasons = emptyList(),
            styleTypes = emptyList(),
            debugReason = reason
        )
    }

    private data class LabelCandidate(
        val label: String,
        val confidence: Float,
        val passIndex: Int
    )

    private fun String.hasAny(vararg keywords: String): Boolean {
        return keywords.any { keyword ->
            contains(keyword.lowercase())
        }
    }
}