package com.fashionai.sdk.color

import com.fashionai.sdk.model.ColorHarmony

/**
 * Result of evaluating color compatibility across clothing items.
 */
data class CompatibilityResult(
    /** Overall harmony score (0.0 = clash, 1.0 = perfect harmony) */
    val score: Float,

    /** Type of color harmony detected */
    val harmonyType: ColorHarmony,

    /** Human-readable notes about the color combination */
    val notes: List<String>,

    /** Suggested accent colors that would complement this palette */
    val suggestedAccents: List<DetectedColor> = emptyList()
) {
    val isCompatible: Boolean get() = score >= 0.60f
    val isHighlyCompatible: Boolean get() = score >= 0.80f

    val summaryText: String get() = when {
        score >= 0.85f -> "Excellent — ${harmonyType.displayName} harmony"
        score >= 0.70f -> "Good — ${harmonyType.displayName} combination"
        score >= 0.55f -> "Acceptable — use with confidence"
        else -> "Bold contrast — wear intentionally"
    }
}
