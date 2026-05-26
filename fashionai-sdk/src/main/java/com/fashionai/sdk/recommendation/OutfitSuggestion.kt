package com.fashionai.sdk.recommendation

import com.fashionai.sdk.model.ClothingItem

/**
 * A complete outfit suggestion produced by the recommendation engine.
 */
data class OutfitSuggestion(
    /** The clothing items that make up this outfit */
    val items: List<ClothingItem>,

    /** Overall compatibility score (0.0–1.0) */
    val compatibilityScore: Float,

    /** Human-readable style note (e.g. "Clean minimal look with neutral tones") */
    val styleNote: String,

    /** How well this outfit fits the requested occasion (0.0–1.0) */
    val occasionFit: Float,

    /** How well this outfit suits the current weather (0.0–1.0, null if no weather context) */
    val weatherFit: Float? = null,

    /** Color harmony score (0.0–1.0) */
    val colorHarmonyScore: Float,

    /** Optional detailed reasoning (populated when LLM is used) */
    val reasoning: String = "",

    /** Any weather-specific advice (e.g. "Avoid suede in rain") */
    val weatherNote: String? = null
) {
    /** Percentage label for display (e.g. "88%") */
    val scorePercent: String get() = "${(compatibilityScore * 100).toInt()}%"

    /** Quick summary combining key scores */
    val summary: String get() = buildString {
        append(styleNote)
        if (!weatherNote.isNullOrBlank()) {
            append(" · ")
            append(weatherNote)
        }
    }
}
