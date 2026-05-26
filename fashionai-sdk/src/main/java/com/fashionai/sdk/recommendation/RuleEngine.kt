package com.fashionai.sdk.recommendation

import com.fashionai.sdk.color.ColorCompatibilityEngine
import com.fashionai.sdk.model.ClothingItem
import com.fashionai.sdk.model.ClothingType
import com.fashionai.sdk.model.Occasion
import com.fashionai.sdk.model.StyleType
import com.fashionai.sdk.weather.WeatherCompatibilityMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device outfit recommendation engine.
 * Fully offline — no network required.
 *
 * Pipeline:
 *  1. Hard filter (weather + occasion eligibility)
 *  2. Combination generation (capped to avoid O(n³) explosion)
 *  3. Multi-factor scoring
 *  4. Ranking + deduplication
 */
internal class RuleEngine {

    private val colorEngine = ColorCompatibilityEngine()

    suspend fun generateOutfits(
        wardrobe: List<ClothingItem>,
        context: OutfitContext,
        maxResults: Int = 5
    ): List<RankedOutfit> = withContext(Dispatchers.Default) {

        val active = wardrobe.filter { it.isActive && it.id !in context.excludeItemIds }

        val tops = active.filter { it.type == ClothingType.TOP }
            .filter { passes(it, context) }
        val bottoms = active.filter { it.type == ClothingType.BOTTOM }
            .filter { passes(it, context) }
        val fullBody = active.filter { it.type == ClothingType.FULL_BODY }
            .filter { passes(it, context) }
        val footwear = active.filter { it.type == ClothingType.FOOTWEAR }
            .filter { passes(it, context) }

        val combinations = mutableListOf<List<ClothingItem>>()

        // Full-body + footwear
        for (fb in fullBody) {
            for (shoe in footwear) {
                combinations.add(listOf(fb, shoe))
            }
            if (footwear.isEmpty()) {
                combinations.add(listOf(fb))
            }
        }

        // Top + Bottom + Footwear (capped to 300 combos)
        val capTops = tops.sortedByDescending { occasionScore(it, context.occasion) }.take(10)
        val capBottoms = bottoms.sortedByDescending { occasionScore(it, context.occasion) }.take(10)
        val capShoes = footwear.sortedByDescending { occasionScore(it, context.occasion) }.take(5)

        outer@ for (top in capTops) {
            for (bottom in capBottoms) {
                if (capShoes.isEmpty()) {
                    combinations.add(listOf(top, bottom))
                } else {
                    for (shoe in capShoes) {
                        combinations.add(listOf(top, bottom, shoe))
                        if (combinations.size >= 300) break@outer
                    }
                }
            }
        }

        combinations
            .map { items -> score(items, context) }
            .filter { it.totalScore >= MIN_SCORE }
            .sortedByDescending { it.totalScore }
            .distinctBy { outfit ->
                // Deduplicate by item ID set
                outfit.items.map { it.id }.sorted().hashCode()
            }
            .take(maxResults)
    }

    // ─── Hard filter ───────────────────────────────────────────────────────

    private fun passes(item: ClothingItem, context: OutfitContext): Boolean {
        val weatherOk = context.weather == null ||
                WeatherCompatibilityMatrix.isCompatible(item, context.weather)
        return weatherOk
    }

    // ─── Scoring ───────────────────────────────────────────────────────────

    private fun score(items: List<ClothingItem>, context: OutfitContext): RankedOutfit {
        val colorScore = colorScore(items)
        val occasionScore = items.map { occasionScore(it, context.occasion) }.average().toFloat()
        val weatherScore = if (context.weather != null) {
            items.map { WeatherCompatibilityMatrix.getScore(it, context.weather) }.average().toFloat()
        } else 1.0f
        val styleScore = styleScore(items, context.preferredStyle)
        val freshnessScore = freshnessScore(items)

        val total = (colorScore * 0.30f) +
                (occasionScore * 0.30f) +
                (weatherScore * 0.20f) +
                (styleScore * 0.15f) +
                (freshnessScore * 0.05f)

        val weatherNote = context.weather?.let {
            WeatherCompatibilityMatrix.getWeatherNote(items, it)
        }

        val styleNote = buildStyleNote(items, context, colorScore, occasionScore)

        return RankedOutfit(
            items = items,
            totalScore = total,
            colorScore = colorScore,
            occasionScore = occasionScore,
            weatherScore = weatherScore,
            styleScore = styleScore,
            styleNote = styleNote,
            weatherNote = weatherNote
        )
    }

    private fun colorScore(items: List<ClothingItem>): Float {
        val colors = items.flatMap { it.colors }
        if (colors.isEmpty()) return 0.70f
        return colorEngine.evaluate(colors).score
    }

    private fun occasionScore(item: ClothingItem, occasion: Occasion): Float {
        if (item.occasions.isEmpty()) return 0.60f
        return if (occasion in item.occasions) 1.0f else 0.20f
    }

    private fun styleScore(items: List<ClothingItem>, preferred: StyleType?): Float {
        if (preferred == null) return 0.80f
        val matches = items.count { preferred in it.styleTypes }
        return (matches.toFloat() / items.size.toFloat()).coerceIn(0f, 1f)
    }

    private fun freshnessScore(items: List<ClothingItem>): Float {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val fresh = items.count { it.lastWornAt == null || it.lastWornAt < sevenDaysAgo }
        return fresh.toFloat() / items.size.toFloat()
    }

    private fun buildStyleNote(
        items: List<ClothingItem>,
        context: OutfitContext,
        colorScore: Float,
        occasionScore: Float
    ): String {
        val primaryColors = items.flatMap { it.colors }.take(2).map { it.name }
        val colorDescription = when {
            primaryColors.size >= 2 -> "${primaryColors[0]} & ${primaryColors[1]}"
            primaryColors.size == 1 -> primaryColors[0]
            else -> "neutral"
        }

        val styleAdjective = when {
            colorScore >= 0.90f -> "harmonious"
            colorScore >= 0.75f -> "well-balanced"
            else -> "contrasting"
        }

        val occasionWord = context.occasion.displayName.lowercase()

        return when (context.mood) {
            Mood.MINIMAL -> "Clean $styleAdjective look in $colorDescription tones — perfect for a minimal $occasionWord vibe"
            Mood.BOLD -> "Bold $styleAdjective combination — confident $colorDescription statement"
            Mood.COMFY -> "Relaxed $styleAdjective $colorDescription look — comfortable $occasionWord outfit"
            Mood.SHARP -> "Sharp $styleAdjective outfit — polished $colorDescription for $occasionWord"
            Mood.ROMANTIC -> "Romantic $styleAdjective look in $colorDescription — dreamy $occasionWord outfit"
            Mood.POWER -> "Power outfit — structured $styleAdjective look in $colorDescription"
            else -> "Stylish $styleAdjective combination in $colorDescription — great for $occasionWord"
        }
    }

    companion object {
        private const val MIN_SCORE = 0.45f
    }
}

internal data class RankedOutfit(
    val items: List<ClothingItem>,
    val totalScore: Float,
    val colorScore: Float,
    val occasionScore: Float,
    val weatherScore: Float,
    val styleScore: Float,
    val styleNote: String,
    val weatherNote: String?
)
