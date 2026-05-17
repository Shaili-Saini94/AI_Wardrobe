package com.fashionai.sdk.color

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import com.fashionai.sdk.internal.FashionAILogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts dominant colors from a clothing image using the Palette API
 * and maps them to human-friendly fashion color names.
 */
internal class ColorAnalyzer {

    /**
     * Extracts up to [maxColors] dominant colors from [bitmap].
     * Returns an empty list if extraction fails.
     */
    suspend fun extractColors(
        bitmap: Bitmap,
        maxColors: Int = 5
    ): List<DetectedColor> = withContext(Dispatchers.Default) {
        try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()

            val swatches = buildList {
                palette.dominantSwatch?.let { add(it) }
                palette.vibrantSwatch?.let { add(it) }
                palette.mutedSwatch?.let { add(it) }
                palette.darkVibrantSwatch?.let { add(it) }
                palette.lightVibrantSwatch?.let { add(it) }
                palette.darkMutedSwatch?.let { add(it) }
                palette.lightMutedSwatch?.let { add(it) }
            }

            val totalPopulation = swatches.sumOf { it.population }.toFloat()
                .coerceAtLeast(1f)

            swatches
                .distinctBy { it.rgb }
                .sortedByDescending { it.population }
                .take(maxColors)
                .map { swatch ->
                    val hex = String.format("#%06X", 0xFFFFFF and swatch.rgb)
                    val name = FashionColorDictionary.nearest(swatch.rgb)
                    val percentage = swatch.population / totalPopulation
                    DetectedColor(hex = hex, name = name, percentage = percentage)
                }
        } catch (e: Exception) {
            FashionAILogger.e("Color extraction failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Converts bitmap pixel data to HSV for analysis.
     * Used by the compatibility engine.
     */
    internal fun hexToHsv(hex: String): FloatArray {
        return try {
            val color = android.graphics.Color.parseColor(hex)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            hsv
        } catch (e: Exception) {
            floatArrayOf(0f, 0f, 0f)
        }
    }
}
