package com.fashionai.sdk.categorization

import android.graphics.Bitmap
import com.fashionai.sdk.color.DetectedColor
import com.fashionai.sdk.detection.DetectionResult
import com.fashionai.sdk.detection.FashionLabelDictionary
import com.fashionai.sdk.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Converts raw [DetectionResult] into a full [CategoryResult]
 * by running pattern detection, tag generation, and inference lookups.
 */
internal class SmartCategorizer {

    private val patternDetector = PatternDetector()
    private val tagEngine = TagEngine()

    /**
     * Categorizes a clothing item from its detection result and extracted colors.
     * Pattern detection requires the original bitmap.
     */
    suspend fun categorize(
        detection: DetectionResult,
        bitmap: Bitmap?,
        colors: List<DetectedColor> = emptyList()
    ): CategoryResult = coroutineScope {

        val patternDeferred = async {
            if (bitmap != null) patternDetector.detect(bitmap) else Pattern.UNKNOWN
        }

        val subType = detection.subTypeDisplay
        val type = detection.clothingType

        val occasions = FashionLabelDictionary.SUBTYPE_TO_OCCASIONS[subType]
            ?: inferOccasionsFromType(type)

        val seasons = FashionLabelDictionary.SUBTYPE_TO_SEASONS[subType]
            ?: listOf(Season.ALL_SEASON)

        val styleTypes = FashionLabelDictionary.SUBTYPE_TO_STYLES[subType]
            ?: listOf(StyleType.CASUAL_CHIC)

        val pattern = patternDeferred.await()
        val gender = tagEngine.inferGenderRelevance(subType, colors)
        val formality = tagEngine.inferFormality(subType)

        val tags = tagEngine.generateTags(
            subType = subType,
            colors = colors,
            pattern = pattern,
            occasions = occasions,
            seasons = seasons,
            styleTypes = styleTypes
        )

        CategoryResult(
            type = type,
            subType = subType,
            occasions = occasions,
            seasons = seasons,
            pattern = pattern,
            styleTypes = styleTypes,
            genderRelevance = gender,
            tags = tags,
            formalityLevel = formality
        )
    }

    private fun inferOccasionsFromType(type: ClothingType): List<Occasion> = when (type) {
        ClothingType.TOP -> listOf(Occasion.CASUAL, Occasion.WORK)
        ClothingType.BOTTOM -> listOf(Occasion.CASUAL)
        ClothingType.FULL_BODY -> listOf(Occasion.CASUAL, Occasion.PARTY)
        ClothingType.FOOTWEAR -> listOf(Occasion.CASUAL)
        else -> listOf(Occasion.CASUAL)
    }
}
