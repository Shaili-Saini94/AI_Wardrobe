package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.StyleProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outfit suggestions — now powered by on-device [OutfitCompositionEngine].
 * No API calls, no rate limits, instant results.
 */
@Singleton
class StylingInference @Inject constructor(
    private val engine: OutfitCompositionEngine
) {
    suspend fun generateOutfits(
        items: List<ClothingItem>,
        occasion: String = "Any",
        weather: String = "mild weather, around 22°C",
        styleProfile: StyleProfile = StyleProfile(),
        count: Int = 5
    ): List<Outfit> {
        if (items.isEmpty()) return emptyList()
        return engine.generate(
            items    = items,
            occasion = occasion,
            season   = extractSeason(weather),
            profile  = styleProfile,
            count    = count
        )
    }

    private fun extractSeason(weather: String): String {
        val w = weather.lowercase()
        return when {
            "summer" in w || "hot" in w           -> "Summer"
            "winter" in w || "cold" in w          -> "Winter"
            "spring" in w                         -> "Spring"
            "autumn" in w || "fall" in w          -> "Autumn"
            "monsoon" in w || "rain" in w         -> "Autumn"
            else                                  -> ""
        }
    }
}
