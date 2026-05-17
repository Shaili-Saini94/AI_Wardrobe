package com.fashionai.sdk.model

import com.fashionai.sdk.color.DetectedColor

/**
 * Represents a single clothing item in the user's wardrobe.
 * Produced by the SDK after detection + categorization.
 */
data class ClothingItem(
    /** Unique identifier — your app can use its own ID scheme */
    val id: String,

    /** URI or path of the original clothing image */
    val imageUri: String,

    /** Primary clothing category */
    val type: ClothingType,

    /** Specific sub-type (e.g. "Polo", "Cargo Pants", "Ankle Boots") */
    val subType: String,

    /** Dominant colors detected in the image */
    val colors: List<DetectedColor> = emptyList(),

    /** Auto-generated descriptive tags (e.g. "smart casual", "breathable") */
    val tags: List<String> = emptyList(),

    /** Occasions this item is suitable for */
    val occasions: List<Occasion> = emptyList(),

    /** Seasons this item is suitable for */
    val seasons: List<Season> = emptyList(),

    /** Detected pattern */
    val pattern: Pattern = Pattern.UNKNOWN,

    /** Style types this item fits into */
    val styleTypes: List<StyleType> = emptyList(),

    /** Gender relevance */
    val genderRelevance: GenderRelevance = GenderRelevance.UNISEX,

    /** Optional brand name */
    val brand: String? = null,

    /** Optional user notes */
    val notes: String? = null,

    /** Timestamp when item was added (ms since epoch) */
    val addedAt: Long = System.currentTimeMillis(),

    /** Timestamp when last worn — null if never worn */
    val lastWornAt: Long? = null,

    /** Total times worn */
    val wornCount: Int = 0,

    /** Whether the item is active in the wardrobe */
    val isActive: Boolean = true
) {
    /** True if this item has never been worn */
    val isNeverWorn: Boolean get() = lastWornAt == null

    /** Returns the primary (most dominant) color, or null */
    val primaryColor: DetectedColor? get() = colors.firstOrNull()
}
