package com.fashionai.sdk.color

/**
 * A color detected in a clothing image.
 *
 * @param hex Color in #RRGGBB format (e.g. "#1A237E")
 * @param name Human-readable fashion color name (e.g. "Navy Blue")
 * @param percentage How much of the image this color occupies (0.0–1.0)
 */
data class DetectedColor(
    val hex: String,
    val name: String,
    val percentage: Float
) {
    /** Red component (0–255) */
    val r: Int get() = hex.substring(1, 3).toInt(16)

    /** Green component (0–255) */
    val g: Int get() = hex.substring(3, 5).toInt(16)

    /** Blue component (0–255) */
    val b: Int get() = hex.substring(5, 7).toInt(16)

    /** Android color int */
    val colorInt: Int get() = android.graphics.Color.parseColor(hex)

    override fun toString(): String = "$name ($hex)"
}
