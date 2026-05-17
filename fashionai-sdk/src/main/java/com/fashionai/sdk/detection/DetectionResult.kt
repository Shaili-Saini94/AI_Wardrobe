package com.fashionai.sdk.detection

import com.fashionai.sdk.model.ClothingType

/**
 * Raw output from the clothing detection model.
 * Pass this to [com.fashionai.sdk.categorization.SmartCategorizer] to enrich it.
 */
data class DetectionResult(
    /** The broad clothing category */
    val clothingType: ClothingType,

    /** Specific sub-type label from the model (e.g. "polo_shirt", "cargo_pants") */
    val subTypeRaw: String,

    /** Human-readable sub-type name (e.g. "Polo", "Cargo Pants") */
    val subTypeDisplay: String,

    /** Model confidence score (0.0–1.0) */
    val confidence: Float,

    /** All top-N candidates the model considered */
    val alternatives: List<DetectionCandidate> = emptyList(),

    /** True if this is a fallback / low-confidence result */
    val isFallback: Boolean = false
) {
    val isHighConfidence: Boolean get() = confidence >= 0.75f
    val isMediumConfidence: Boolean get() = confidence in 0.55f..0.75f
    val isLowConfidence: Boolean get() = confidence < 0.55f

    companion object {
        fun unknown(): DetectionResult = DetectionResult(
            clothingType = ClothingType.UNKNOWN,
            subTypeRaw = "unknown",
            subTypeDisplay = "Unknown",
            confidence = 0f,
            isFallback = true
        )
    }
}

data class DetectionCandidate(
    val label: String,
    val confidence: Float,
    val clothingType: ClothingType
)
