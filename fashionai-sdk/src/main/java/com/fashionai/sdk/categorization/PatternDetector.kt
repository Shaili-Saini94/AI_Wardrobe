package com.fashionai.sdk.categorization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.fashionai.sdk.model.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detects clothing pattern (solid, striped, checked, floral, etc.)
 * using image analysis techniques on the clothing bitmap.
 */
internal class PatternDetector {

    suspend fun detect(bitmap: Bitmap): Pattern = withContext(Dispatchers.Default) {
        try {
            val sampled = sampleBitmap(bitmap)
            val colorDiversity = computeColorDiversity(sampled)
            val edgeDensity = computeEdgeDensity(toGrayscale(sampled))
            val hEdgeStrength = horizontalEdgeStrength(toGrayscale(sampled))
            val vEdgeStrength = verticalEdgeStrength(toGrayscale(sampled))

            classifyPattern(colorDiversity, edgeDensity, hEdgeStrength, vEdgeStrength)
        } catch (e: Exception) {
            Pattern.UNKNOWN
        }
    }

    private fun classifyPattern(
        colorDiversity: Float,
        edgeDensity: Float,
        hEdgeStrength: Float,
        vEdgeStrength: Float
    ): Pattern {
        return when {
            // Solid: low edge density, low color diversity
            edgeDensity < 0.05f && colorDiversity < 0.15f -> Pattern.SOLID

            // Checked: both horizontal and vertical edges are strong
            hEdgeStrength > 0.3f && vEdgeStrength > 0.3f -> Pattern.CHECKED

            // Striped: either H or V edges dominate (not both)
            hEdgeStrength > 0.35f && vEdgeStrength < 0.2f -> Pattern.STRIPED
            vEdgeStrength > 0.35f && hEdgeStrength < 0.2f -> Pattern.STRIPED

            // Floral: high color diversity + moderate edges (organic shapes)
            colorDiversity > 0.45f && edgeDensity in 0.1f..0.4f -> Pattern.FLORAL

            // Geometric: high edge density, lower color diversity
            edgeDensity > 0.4f && colorDiversity < 0.35f -> Pattern.GEOMETRIC

            // Abstract: high color diversity AND high edge density
            colorDiversity > 0.4f && edgeDensity > 0.35f -> Pattern.ABSTRACT

            // Low edge, moderate color — likely solid with color variation (dye)
            edgeDensity < 0.15f -> Pattern.SOLID

            else -> Pattern.UNKNOWN
        }
    }

    /** Scales bitmap down for faster analysis */
    private fun sampleBitmap(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, true)

    /** Converts to grayscale for edge detection */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val gray = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(gray).drawBitmap(bitmap, 0f, 0f, paint)
        return gray
    }

    /**
     * Measures how many distinct colors appear in the image.
     * High diversity → floral or abstract. Low → solid or striped.
     */
    private fun computeColorDiversity(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val quantized = pixels.map { pixel ->
            val r = (android.graphics.Color.red(pixel) / 32) * 32
            val g = (android.graphics.Color.green(pixel) / 32) * 32
            val b = (android.graphics.Color.blue(pixel) / 32) * 32
            (r shl 16) or (g shl 8) or b
        }.toSet()

        val maxPossible = (256 / 32).toFloat().let { it * it * it }
        return (quantized.size / maxPossible).coerceIn(0f, 1f)
    }

    /**
     * Approximate edge density using a simple Sobel-like approach.
     * High density = lots of edges = pattern.
     */
    private fun computeEdgeDensity(grayscale: Bitmap): Float {
        val w = grayscale.width
        val h = grayscale.height
        val pixels = IntArray(w * h)
        grayscale.getPixels(pixels, 0, w, 0, 0, w, h)

        var edgeCount = 0
        val threshold = 30

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = android.graphics.Color.red(pixels[y * w + x])
                val right = android.graphics.Color.red(pixels[y * w + x + 1])
                val bottom = android.graphics.Color.red(pixels[(y + 1) * w + x])
                if (Math.abs(center - right) > threshold || Math.abs(center - bottom) > threshold) {
                    edgeCount++
                }
            }
        }

        return edgeCount.toFloat() / ((w - 2) * (h - 2))
    }

    /** Measures regularity of horizontal edges (stripes or grids) */
    private fun horizontalEdgeStrength(grayscale: Bitmap): Float {
        val w = grayscale.width
        val h = grayscale.height
        val pixels = IntArray(w * h)
        grayscale.getPixels(pixels, 0, w, 0, 0, w, h)

        val rowSums = FloatArray(h) { y ->
            var sum = 0f
            for (x in 0 until w) {
                sum += android.graphics.Color.red(pixels[y * w + x])
            }
            sum / w
        }

        // Compute variance of row sums — high variance = horizontal stripes
        val mean = rowSums.average().toFloat()
        val variance = rowSums.map { (it - mean) * (it - mean) }.average().toFloat()
        return (variance / (255f * 255f)).coerceIn(0f, 1f)
    }

    /** Measures regularity of vertical edges (stripes or grids) */
    private fun verticalEdgeStrength(grayscale: Bitmap): Float {
        val w = grayscale.width
        val h = grayscale.height
        val pixels = IntArray(w * h)
        grayscale.getPixels(pixels, 0, w, 0, 0, w, h)

        val colSums = FloatArray(w) { x ->
            var sum = 0f
            for (y in 0 until h) {
                sum += android.graphics.Color.red(pixels[y * w + x])
            }
            sum / h
        }

        val mean = colSums.average().toFloat()
        val variance = colSums.map { (it - mean) * (it - mean) }.average().toFloat()
        return (variance / (255f * 255f)).coerceIn(0f, 1f)
    }

    companion object {
        private const val SAMPLE_SIZE = 64
    }
}
