package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class StylingInference @Inject constructor() {

    fun generateOutfits(
        items: List<ClothingItem>,
        occasion: String,
        count: Int = 5
    ): List<Outfit> {
        if (items.isEmpty()) return emptyList()

        val usableItems = items.filter { item ->
            item.occasions.any { it.equals(occasion, ignoreCase = true) } ||
                    item.scoreForOccasion(occasion) > 0f
        }

        if (usableItems.isEmpty()) return emptyList()

        val tops = usableItems.filter { it.isTop() }
        val bottoms = usableItems.filter { it.isBottom() }
        val onePieces = usableItems.filter { it.isOnePiece() }
        val footwear = usableItems.filter { it.isFootwear() }

        val scoredOutfits = mutableListOf<ScoredOutfit>()

        for (top in tops) {
            for (bottom in bottoms) {
                val bestShoes = footwear
                    .sortedByDescending { it.scoreForOccasion(occasion) }
                    .take(3)

                if (bestShoes.isEmpty()) {
                    val outfitItems = listOf(top, bottom)
                    scoredOutfits += createScoredOutfit(
                        occasion = occasion,
                        items = outfitItems,
                        index = scoredOutfits.size + 1
                    )
                } else {
                    bestShoes.forEach { shoe ->
                        val outfitItems = listOf(top, bottom, shoe)
                        scoredOutfits += createScoredOutfit(
                            occasion = occasion,
                            items = outfitItems,
                            index = scoredOutfits.size + 1
                        )
                    }
                }
            }
        }

        for (onePiece in onePieces) {
            val bestShoes = footwear
                .sortedByDescending { it.scoreForOccasion(occasion) }
                .take(3)

            if (bestShoes.isEmpty()) {
                scoredOutfits += createScoredOutfit(
                    occasion = occasion,
                    items = listOf(onePiece),
                    index = scoredOutfits.size + 1
                )
            } else {
                bestShoes.forEach { shoe ->
                    scoredOutfits += createScoredOutfit(
                        occasion = occasion,
                        items = listOf(onePiece, shoe),
                        index = scoredOutfits.size + 1
                    )
                }
            }
        }

        return scoredOutfits
            .filter { it.score > 0f }
            .sortedByDescending { it.score }
            .distinctBy { scored ->
                scored.outfit.items.mapNotNull { it.id }.sorted()
            }
            .take(count)
            .mapIndexed { index, scored ->
                scored.outfit.copy(
                    name = "${occasion.cleanOccasionName()} Look ${index + 1}"
                )
            }
    }

    private fun createScoredOutfit(
        occasion: String,
        items: List<ClothingItem>,
        index: Int
    ): ScoredOutfit {
        val score = items.sumOf { item ->
            item.scoreForOccasion(occasion).toDouble()
        }.toFloat() + categoryCompletenessScore(items)

        return ScoredOutfit(
            score = score,
            outfit = Outfit(
                name = "${occasion.cleanOccasionName()} Look $index",
                items = items,
                dateCreated = System.currentTimeMillis()
            )
        )
    }

    private data class ScoredOutfit(
        val score: Float,
        val outfit: Outfit
    )

    private fun categoryCompletenessScore(items: List<ClothingItem>): Float {
        val hasTop = items.any { it.isTop() }
        val hasBottom = items.any { it.isBottom() }
        val hasOnePiece = items.any { it.isOnePiece() }
        val hasFootwear = items.any { it.isFootwear() }

        return when {
            hasOnePiece && hasFootwear -> 5f
            hasTop && hasBottom && hasFootwear -> 6f
            hasTop && hasBottom -> 4f
            hasOnePiece -> 3f
            else -> 0f
        }
    }

    private fun ClothingItem.isTop(): Boolean {
        val text = searchableText()

        return text.hasAny(
            "top",
            "shirt",
            "t-shirt",
            "tshirt",
            "crop",
            "sleeveless",
            "full sleeves",
            "hoodie",
            "jacket",
            "blazer",
            "blouse"
        )
    }

    private fun ClothingItem.isBottom(): Boolean {
        val text = searchableText()

        return text.hasAny(
            "bottom",
            "jeans",
            "denim",
            "pants",
            "trousers",
            "shorts",
            "skirt",
            "joggers",
            "high-waist",
            "high waist"
        )
    }

    private fun ClothingItem.isOnePiece(): Boolean {
        val text = searchableText()

        return text.hasAny(
            "one-piece",
            "one piece",
            "dress",
            "jumpsuit",
            "complete-outfit"
        )
    }

    private fun ClothingItem.isFootwear(): Boolean {
        val text = searchableText()

        return text.hasAny(
            "footwear",
            "shoe",
            "shoes",
            "sneaker",
            "sneakers",
            "heel",
            "heels",
            "sandal",
            "sandals",
            "boot",
            "boots",
            "slipper"
        )
    }

    private fun ClothingItem.scoreForOccasion(occasion: String): Float {
        val text = searchableText()
        var score = 0f

        if (occasions.any { it.equals(occasion, ignoreCase = true) }) {
            score += 8f
        }

        score += when (occasion.lowercase()) {
            "party" -> when {
                text.hasAny("dress", "crop", "skirt", "heels", "boots", "high-waist", "blazer") -> 6f
                text.hasAny("jeans", "shirt", "top", "shoes") -> 3f
                text.hasAny("joggers", "sports", "gym") -> -5f
                else -> 0f
            }

            "clubbing" -> when {
                text.hasAny("crop", "skirt", "dress", "heels", "boots", "high-waist") -> 7f
                text.hasAny("jeans", "top") -> 3f
                text.hasAny("joggers", "formal shirt", "office") -> -4f
                else -> 0f
            }

            "sports / gym" -> when {
                text.hasAny("t-shirt", "tshirt", "sleeveless", "joggers", "shorts", "sneakers", "sports") -> 8f
                text.hasAny("heels", "dress", "blazer", "skirt", "boots") -> -8f
                else -> 0f
            }

            "office / formal" -> when {
                text.hasAny("shirt", "blazer", "trousers", "pants", "heels", "formal") -> 8f
                text.hasAny("crop", "shorts", "slipper", "beach", "gym") -> -7f
                text.hasAny("jeans") -> 2f
                else -> 0f
            }

            "casual outing" -> when {
                text.hasAny("t-shirt", "shirt", "jeans", "pants", "sneakers", "sandals", "hoodie", "top") -> 6f
                text.hasAny("heels", "dress", "skirt") -> 3f
                else -> 1f
            }

            "beach trip" -> when {
                text.hasAny("shorts", "sandals", "slipper", "sleeveless", "t-shirt", "summer") -> 8f
                text.hasAny("boots", "blazer", "heels", "jacket") -> -7f
                else -> 0f
            }

            "mountain / travel trip" -> when {
                text.hasAny("hoodie", "jacket", "jeans", "boots", "sneakers", "winter", "comfortable") -> 8f
                text.hasAny("heels", "sandals", "crop", "beach") -> -6f
                else -> 0f
            }

            "date night" -> when {
                text.hasAny("dress", "shirt", "skirt", "heels", "blazer", "boots", "crop", "high-waist") -> 7f
                text.hasAny("joggers", "sports", "gym") -> -5f
                else -> 1f
            }

            else -> 1f
        }

        return score
    }

    private fun ClothingItem.searchableText(): String {
        return buildString {
            append(category)
            append(" ")
            append(tags.joinToString(" "))
            append(" ")
            append(occasions.joinToString(" "))
            append(" ")
            append(seasons.joinToString(" "))
            append(" ")
            append(styleTypes.joinToString(" "))
            append(" ")
            append(mood.orEmpty())
            append(" ")
            append(weather.orEmpty())
        }.lowercase()
    }

    private fun String.hasAny(vararg keywords: String): Boolean {
        return keywords.any { keyword -> contains(keyword.lowercase()) }
    }

    private fun String.cleanOccasionName(): String {
        return replace("/", " / ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}