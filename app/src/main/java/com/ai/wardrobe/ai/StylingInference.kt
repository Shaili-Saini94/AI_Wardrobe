package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class StylingInference @Inject constructor() {

    /**
     * Simulates AI outfit generation.
     * In a real scenario, this would use a LiteRT model to predict pairings.
     * Here, it uses a categorization and color-matching simulation.
     */
    fun generateOutfits(items: List<ClothingItem>, count: Int = 3): List<Outfit> {
        if (items.size < 2) return emptyList()

        val tops = items.filter { it.category.contains("shirt", true) || it.category.contains("top", true) || it.category.contains("jacket", true) }
        val bottoms = items.filter { it.category.contains("pants", true) || it.category.contains("jean", true) || it.category.contains("skirt", true) || it.category.contains("short", true) }
        val shoes = items.filter { it.category.contains("shoe", true) || it.category.contains("sneaker", true) || it.category.contains("boot", true) }

        val suggestions = mutableListOf<Outfit>()
        
        repeat(count) {
            val outfitItems = mutableListOf<ClothingItem>()
            
            val top = tops.randomOrNull()
            val bottom = bottoms.randomOrNull()
            val shoe = shoes.randomOrNull()

            if (top != null) outfitItems.add(top)
            if (bottom != null) outfitItems.add(bottom)
            if (shoe != null) outfitItems.add(shoe)

            if (outfitItems.size >= 2) {
                suggestions.add(
                    Outfit(
                        name = "Style Suggestion ${suggestions.size + 1}",
                        items = outfitItems,
                        dateCreated = System.currentTimeMillis()
                    )
                )
            }
        }

        // If specific categories are empty, just pick random items as a fallback
        if (suggestions.isEmpty()) {
            repeat(count) {
                val shuffled = items.shuffled()
                val randomCount = Random.nextInt(2, minOf(4, items.size + 1))
                suggestions.add(
                    Outfit(
                        name = "Eclectic Mix ${suggestions.size + 1}",
                        items = shuffled.take(randomCount),
                        dateCreated = System.currentTimeMillis()
                    )
                )
            }
        }

        return suggestions.distinctBy { it.items.map { item -> item.id }.toSet() }
    }
}
