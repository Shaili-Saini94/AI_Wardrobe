package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object DominantColorExtractor {

    private data class NamedColor(val name: String, val r: Int, val g: Int, val b: Int)

    private val palette = listOf(
        NamedColor("White",     255, 255, 255),
        NamedColor("Black",     20,  20,  20),
        NamedColor("Grey",      128, 128, 128),
        NamedColor("Beige",     245, 225, 193),
        NamedColor("Tan",       210, 180, 140),
        NamedColor("Brown",     139, 90,  43),
        NamedColor("Camel",     193, 154, 107),
        NamedColor("Red",       220, 30,  30),
        NamedColor("Burgundy",  128, 0,   32),
        NamedColor("Pink",      255, 182, 193),
        NamedColor("Orange",    255, 140, 0),
        NamedColor("Coral",     255, 127, 80),
        NamedColor("Yellow",    255, 220, 50),
        NamedColor("Mustard",   204, 164, 30),
        NamedColor("Green",     60,  140, 60),
        NamedColor("Olive",     107, 124, 67),
        NamedColor("Sage",      145, 163, 122),
        NamedColor("Navy",      30,  50,  100),
        NamedColor("Blue",      50,  100, 200),
        NamedColor("Denim",     93,  119, 157),
        NamedColor("Sky Blue",  135, 206, 235),
        NamedColor("Teal",      0,   128, 128),
        NamedColor("Purple",    128, 0,   128),
        NamedColor("Lavender",  180, 160, 220),
        NamedColor("Cream",     255, 253, 208),
    )

    fun extract(context: Context, uri: Uri, sampleSize: Int = 8): String {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return "Unknown"
            extractFromBitmap(raw, sampleSize)
        } catch (e: Exception) { "Unknown" }
    }

    fun extractFromBitmap(bitmap: Bitmap, sampleSize: Int = 8): String {
        val small = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
        val rSum = LongArray(palette.size)
        val gSum = LongArray(palette.size)
        val bSum = LongArray(palette.size)
        val counts = IntArray(palette.size)

        for (x in 0 until small.width) {
            for (y in 0 until small.height) {
                val px = small.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                if (r < 10 && g < 10 && b < 10) continue  // skip pure black (background artifacts)

                // Assign to nearest palette color
                val nearest = palette.indices.minByOrNull { i ->
                    val dr = r - palette[i].r
                    val dg = g - palette[i].g
                    val db = b - palette[i].b
                    dr * dr + dg * dg + db * db
                } ?: 0
                rSum[nearest] += r; gSum[nearest] += g; bSum[nearest] += b
                counts[nearest]++
            }
        }

        val dominant = counts.indices.maxByOrNull { counts[it] } ?: 0
        return if (counts[dominant] > 0) palette[dominant].name else "Unknown"
    }
}
