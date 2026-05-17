package com.fashionai.sdk.color

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Maps raw RGB values to human-friendly fashion color names.
 */
internal object FashionColorDictionary {

    private val COLOR_MAP: List<Pair<IntArray, String>> = listOf(
        intArrayOf(0, 0, 0) to "Black",
        intArrayOf(255, 255, 255) to "White",
        intArrayOf(128, 128, 128) to "Grey",
        intArrayOf(192, 192, 192) to "Silver",
        intArrayOf(64, 64, 64) to "Charcoal",
        intArrayOf(255, 0, 0) to "Red",
        intArrayOf(180, 0, 0) to "Dark Red",
        intArrayOf(255, 80, 80) to "Coral Red",
        intArrayOf(255, 160, 122) to "Salmon",
        intArrayOf(255, 99, 71) to "Tomato",
        intArrayOf(220, 20, 60) to "Crimson",
        intArrayOf(139, 0, 0) to "Maroon",
        intArrayOf(0, 0, 255) to "Blue",
        intArrayOf(0, 0, 139) to "Dark Blue",
        intArrayOf(0, 0, 80) to "Navy Blue",
        intArrayOf(70, 130, 180) to "Steel Blue",
        intArrayOf(135, 206, 235) to "Sky Blue",
        intArrayOf(173, 216, 230) to "Light Blue",
        intArrayOf(0, 128, 128) to "Teal",
        intArrayOf(64, 224, 208) to "Turquoise",
        intArrayOf(0, 255, 0) to "Green",
        intArrayOf(0, 128, 0) to "Dark Green",
        intArrayOf(34, 139, 34) to "Forest Green",
        intArrayOf(144, 238, 144) to "Light Green",
        intArrayOf(128, 128, 0) to "Olive Green",
        intArrayOf(255, 255, 0) to "Yellow",
        intArrayOf(255, 215, 0) to "Gold",
        intArrayOf(255, 165, 0) to "Orange",
        intArrayOf(255, 140, 0) to "Dark Orange",
        intArrayOf(255, 127, 80) to "Coral",
        intArrayOf(128, 0, 128) to "Purple",
        intArrayOf(75, 0, 130) to "Indigo",
        intArrayOf(238, 130, 238) to "Violet",
        intArrayOf(221, 160, 221) to "Plum",
        intArrayOf(255, 192, 203) to "Pink",
        intArrayOf(255, 105, 180) to "Hot Pink",
        intArrayOf(255, 20, 147) to "Deep Pink",
        intArrayOf(210, 105, 30) to "Chocolate Brown",
        intArrayOf(139, 69, 19) to "Saddle Brown",
        intArrayOf(160, 82, 45) to "Sienna",
        intArrayOf(205, 133, 63) to "Peru",
        intArrayOf(222, 184, 135) to "Burlywood",
        intArrayOf(245, 245, 220) to "Beige",
        intArrayOf(255, 228, 196) to "Bisque",
        intArrayOf(250, 240, 230) to "Linen",
        intArrayOf(255, 248, 220) to "Cream",
        intArrayOf(240, 230, 140) to "Khaki",
        intArrayOf(189, 183, 107) to "Dark Khaki",
        intArrayOf(210, 180, 140) to "Tan",
        intArrayOf(188, 143, 143) to "Rosy Brown",
        intArrayOf(47, 79, 79) to "Dark Slate Grey",
        intArrayOf(0, 100, 0) to "Bottle Green",
        intArrayOf(85, 107, 47) to "Olive Drab",
        intArrayOf(107, 142, 35) to "Yellow Green",
        intArrayOf(32, 178, 170) to "Light Sea Green",
        intArrayOf(95, 158, 160) to "Cadet Blue",
        intArrayOf(100, 149, 237) to "Cornflower Blue",
        intArrayOf(123, 104, 238) to "Medium Slate Blue",
        intArrayOf(147, 112, 219) to "Medium Purple",
        intArrayOf(199, 21, 133) to "Medium Violet Red",
        intArrayOf(255, 228, 225) to "Misty Rose",
        intArrayOf(250, 235, 215) to "Antique White",
        intArrayOf(255, 250, 240) to "Floral White",
        intArrayOf(245, 255, 250) to "Mint Cream",
        intArrayOf(240, 248, 255) to "Alice Blue",
        intArrayOf(230, 230, 250) to "Lavender",
        intArrayOf(216, 191, 216) to "Thistle",
        intArrayOf(176, 196, 222) to "Light Steel Blue",
        intArrayOf(255, 250, 205) to "Lemon Chiffon",
        intArrayOf(255, 239, 213) to "Papaya Whip",
        intArrayOf(255, 218, 185) to "Peach",
        intArrayOf(255, 160, 122) to "Light Salmon",
        intArrayOf(250, 128, 114) to "Salmon Pink",
        intArrayOf(240, 128, 128) to "Light Coral",
        intArrayOf(205, 92, 92) to "Indian Red",
        intArrayOf(178, 34, 34) to "Firebrick",
        intArrayOf(165, 42, 42) to "Brown",
        intArrayOf(128, 0, 0) to "Dark Maroon",
        intArrayOf(255, 215, 0) to "Mustard Yellow",
        intArrayOf(218, 165, 32) to "Goldenrod",
        intArrayOf(184, 134, 11) to "Dark Goldenrod",
        intArrayOf(153, 101, 21) to "Dark Yellow",
        intArrayOf(0, 128, 0) to "Army Green",
        intArrayOf(34, 34, 34) to "Off Black",
        intArrayOf(245, 245, 245) to "Off White"
    )

    /** Returns the nearest fashion color name for the given RGB values. */
    fun nearest(r: Int, g: Int, b: Int): String {
        return COLOR_MAP.minByOrNull { (rgb, _) ->
            colorDistance(r, g, b, rgb[0], rgb[1], rgb[2])
        }?.second ?: "Unknown"
    }

    /** Returns the nearest fashion color name for an Android color int. */
    fun nearest(colorInt: Int): String {
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        return nearest(r, g, b)
    }

    private fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        // Weighted Euclidean — human eye is more sensitive to green
        return sqrt(0.299 * dr * dr + 0.587 * dg * dg + 0.114 * db * db)
    }

    /** True if the color is considered "neutral" in fashion context */
    fun isNeutral(hex: String): Boolean {
        val neutral = nearest(android.graphics.Color.parseColor(hex))
        return neutral in setOf(
            "Black", "White", "Grey", "Silver", "Charcoal", "Off Black", "Off White",
            "Beige", "Cream", "Linen", "Khaki", "Tan", "Camel"
        )
    }
}
