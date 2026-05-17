package com.fashionai.sdk.color

import com.fashionai.sdk.model.ColorHarmony
import kotlin.math.abs

/**
 * Evaluates color compatibility between detected colors using HSV color theory.
 */
internal class ColorCompatibilityEngine {

    private val colorAnalyzer = ColorAnalyzer()

    fun evaluate(colors: List<DetectedColor>): CompatibilityResult {
        if (colors.isEmpty()) {
            return CompatibilityResult(0.8f, ColorHarmony.NEUTRAL, listOf("No colors to evaluate"))
        }
        if (colors.size == 1) {
            return CompatibilityResult(1.0f, ColorHarmony.MONOCHROMATIC, listOf("Single color — always works"))
        }

        val hsvValues = colors.map { colorAnalyzer.hexToHsv(it.hex) }

        // Check if all colors are neutral (black, white, grey, beige etc.)
        val allNeutral = colors.all { FashionColorDictionary.isNeutral(it.hex) }
        if (allNeutral) {
            return CompatibilityResult(
                score = 0.92f,
                harmonyType = ColorHarmony.NEUTRAL,
                notes = listOf("Timeless neutral palette — always stylish", "Easy to accessorize")
            )
        }

        // Filter to chromatic colors for hue analysis (skip near-neutrals)
        val chromatic = hsvValues.filter { hsv -> hsv[1] > 0.15f } // saturation > 15%
        if (chromatic.isEmpty()) {
            return CompatibilityResult(
                score = 0.88f,
                harmonyType = ColorHarmony.NEUTRAL,
                notes = listOf("Muted neutral palette", "Sophisticated and versatile")
            )
        }

        val hues = chromatic.map { it[0] } // 0–360 degrees

        return when {
            isMonochromatic(hues) -> CompatibilityResult(
                score = 0.95f,
                harmonyType = ColorHarmony.MONOCHROMATIC,
                notes = listOf(
                    "Elegant tonal look",
                    "Varying shades of the same hue — very refined",
                    "Great for a polished, put-together appearance"
                )
            )

            isComplementary(hues) -> CompatibilityResult(
                score = 0.88f,
                harmonyType = ColorHarmony.COMPLEMENTARY,
                notes = listOf(
                    "High contrast complementary colors",
                    "Bold and eye-catching combination",
                    "Ensure one color dominates to avoid visual tension"
                )
            )

            isAnalogous(hues) -> CompatibilityResult(
                score = 0.85f,
                harmonyType = ColorHarmony.ANALOGOUS,
                notes = listOf(
                    "Harmonious analogous colors",
                    "Natural and cohesive look",
                    "Easy on the eye — safe and elegant"
                )
            )

            isTriadic(hues) -> CompatibilityResult(
                score = 0.75f,
                harmonyType = ColorHarmony.TRIADIC,
                notes = listOf(
                    "Triadic color scheme",
                    "Vibrant and dynamic — use one color as dominant",
                    "Let one color lead, others as accent"
                )
            )

            isSplitComplementary(hues) -> CompatibilityResult(
                score = 0.80f,
                harmonyType = ColorHarmony.SPLIT_COMPLEMENTARY,
                notes = listOf(
                    "Split complementary — softer contrast",
                    "More nuanced than direct complementary"
                )
            )

            else -> CompatibilityResult(
                score = 0.55f,
                harmonyType = ColorHarmony.CLASH,
                notes = listOf(
                    "Strong color contrast",
                    "Can work as a bold fashion statement",
                    "Ensure your styling is intentional"
                )
            )
        }
    }

    /** True when all hues are within 30° of each other (same color family) */
    private fun isMonochromatic(hues: List<Float>): Boolean {
        val min = hues.min()
        val max = hues.max()
        return hueDiff(min, max) < 30f
    }

    /** True when a pair of hues are roughly opposite on the wheel (150–210° apart) */
    private fun isComplementary(hues: List<Float>): Boolean {
        if (hues.size < 2) return false
        return hues.any { h1 ->
            hues.any { h2 ->
                val diff = hueDiff(h1, h2)
                diff in 150f..210f
            }
        }
    }

    /** True when all hues are within 60° of each other */
    private fun isAnalogous(hues: List<Float>): Boolean {
        val min = hues.min()
        val max = hues.max()
        return hueDiff(min, max) in 1f..60f
    }

    /** True when hues are roughly 120° apart */
    private fun isTriadic(hues: List<Float>): Boolean {
        if (hues.size < 3) return false
        val sorted = hues.sorted()
        val d1 = hueDiff(sorted[0], sorted[1])
        val d2 = hueDiff(sorted[1], sorted[2])
        return d1 in 100f..140f && d2 in 100f..140f
    }

    /** True when one hue has two complements that are 30° on either side */
    private fun isSplitComplementary(hues: List<Float>): Boolean {
        if (hues.size < 3) return false
        return hues.any { base ->
            val complement = (base + 180f) % 360f
            hues.count { h ->
                hueDiff(h, complement) in 20f..50f
            } >= 2
        }
    }

    /** Smallest angular distance between two hues (circular) */
    private fun hueDiff(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2)
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Suggests accent colors that would complement the given base colors.
     */
    fun suggestAccents(baseColors: List<DetectedColor>): List<DetectedColor> {
        if (baseColors.isEmpty()) return emptyList()

        val primaryHsv = colorAnalyzer.hexToHsv(baseColors.first().hex)
        val primaryHue = primaryHsv[0]

        // Complementary hue
        val complementHue = (primaryHue + 180f) % 360f
        // Analogous hues
        val analogous1 = (primaryHue + 30f) % 360f
        val analogous2 = (primaryHue - 30f + 360f) % 360f

        return listOf(
            hsvToDetectedColor(complementHue, primaryHsv[1], primaryHsv[2], "Complementary Accent"),
            hsvToDetectedColor(analogous1, primaryHsv[1], primaryHsv[2], "Analogous Accent"),
            hsvToDetectedColor(analogous2, primaryHsv[1], primaryHsv[2], "Analogous Accent")
        )
    }

    private fun hsvToDetectedColor(h: Float, s: Float, v: Float, label: String): DetectedColor {
        val hsv = floatArrayOf(h, s.coerceIn(0.3f, 0.8f), v.coerceIn(0.4f, 0.9f))
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        val hex = String.format("#%06X", 0xFFFFFF and colorInt)
        return DetectedColor(
            hex = hex,
            name = FashionColorDictionary.nearest(colorInt),
            percentage = 0f
        )
    }
}
