package dev.wardrobekit.vision.util

import android.graphics.Bitmap
import android.graphics.Color

object ImageUtils {
    /**
     * Heuristic to detect if a bitmap is likely a screenshot.
     * Screenshots often have perfectly uniform areas (status bar, navigation bar, flat UI elements).
     */
    fun isLikelyScreenshot(bitmap: Bitmap): Boolean {
        if (bitmap.width < 100 || bitmap.height < 100) return false
        
        // 1. Check for perfectly uniform horizontal or vertical lines (common in UI)
        val sampleX = bitmap.width / 2
        val sampleY = bitmap.height / 2
        
        var verticalUniformity = 0
        var lastPixel = bitmap.getPixel(sampleX, 0)
        for (y in 1 until bitmap.height step 10) {
            val pixel = bitmap.getPixel(sampleX, y)
            if (pixel == lastPixel) verticalUniformity++
            lastPixel = pixel
        }
        
        var horizontalUniformity = 0
        lastPixel = bitmap.getPixel(0, sampleY)
        for (x in 1 until bitmap.width step 10) {
            val pixel = bitmap.getPixel(x, sampleY)
            if (pixel == lastPixel) horizontalUniformity++
            lastPixel = pixel
        }

        // 2. Screenshots often have a very limited color palette in specific areas or perfectly flat backgrounds
        // High uniformity in a vertical slice of a photo is rare, but common in UI lists or solid backgrounds.
        val verticalRatio = verticalUniformity.toFloat() / (bitmap.height / 10)
        val horizontalRatio = horizontalUniformity.toFloat() / (bitmap.width / 10)
        
        return verticalRatio > 0.4f || horizontalRatio > 0.4f
    }
}
