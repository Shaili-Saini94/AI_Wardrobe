package com.fashionai.sdk.categorization

import com.fashionai.sdk.model.*

/**
 * Full categorization output after detection + smart tagging.
 * This is the enriched result you save to your wardrobe database.
 */
data class CategoryResult(
    val type: ClothingType,
    val subType: String,
    val occasions: List<Occasion>,
    val seasons: List<Season>,
    val pattern: Pattern,
    val styleTypes: List<StyleType>,
    val genderRelevance: GenderRelevance,
    val tags: List<String>,
    val formalityLevel: FormalityLevel
)
