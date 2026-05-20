package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.StyleProfile
import javax.inject.Inject
import javax.inject.Singleton

// ── Data classes ──────────────────────────────────────────────────────────────

data class CapsuleItem(
    val item: ClothingItem,
    val outfitsEnabled: Int,    // how many outfits this item participates in
    val versatilityScore: Float // 0–1
)

data class WardrobeGap(
    val slot: String,           // "Tops", "Bottoms", "Shoes", etc.
    val current: Int,
    val recommended: Int,
    val suggestion: String
)

data class ColorPalette(
    val neutrals: Int,
    val blues: Int,
    val earthy: Int,
    val warm: Int,
    val cool: Int,
    val total: Int
) {
    fun dominantFamily(): String {
        val map = mapOf("Neutrals" to neutrals, "Blues" to blues,
            "Earthy" to earthy, "Warm" to warm, "Cool" to cool)
        return map.maxByOrNull { it.value }?.key ?: "Mixed"
    }
}

data class StyleDna(
    val breakdown: Map<String, Float>,  // style → percentage (0–1)
    val dominant: String,
    val secondary: String?
)

// ── Engine ────────────────────────────────────────────────────────────────────

@Singleton
class WardrobeIntelligence @Inject constructor(
    private val engine: OutfitCompositionEngine
) {
    // ── Capsule wardrobe ─────────────────────────────────────────────────────

    fun analyzeCapsule(items: List<ClothingItem>, profile: StyleProfile = StyleProfile()): List<CapsuleItem> {
        if (items.size < 2) return items.map { CapsuleItem(it, 0, 0f) }

        val outfits = engine.generate(items, occasion = "Any", season = "", profile = profile, count = 50)
        val itemOutfitCount = mutableMapOf<Long, Int>()

        for (outfit in outfits) {
            for (item in outfit.items) {
                val id = item.id ?: continue
                itemOutfitCount[id] = (itemOutfitCount[id] ?: 0) + 1
            }
        }

        val max = itemOutfitCount.values.maxOrNull()?.toFloat() ?: 1f
        return items.map { item ->
            val count = itemOutfitCount[item.id] ?: 0
            CapsuleItem(item, count, count / max)
        }.sortedByDescending { it.outfitsEnabled }
    }

    // ── Gap detection ─────────────────────────────────────────────────────────

    fun detectGaps(items: List<ClothingItem>): List<WardrobeGap> {
        val slots = mapOf(
            "Tops"    to setOf("shirt", "t-shirt", "tshirt", "tee", "blouse", "top", "sweater", "hoodie", "kurta", "kurti", "crop"),
            "Bottoms" to setOf("jeans", "pants", "trouser", "shorts", "skirt", "leggings", "joggers", "chino", "palazzo"),
            "Dresses" to setOf("dress", "gown", "jumpsuit", "romper", "saree", "lehenga", "anarkali"),
            "Outerwear" to setOf("jacket", "coat", "blazer", "cardigan", "vest", "shrug", "trench"),
            "Shoes"   to setOf("shoe", "sneaker", "boot", "sandal", "heel", "flat", "loafer", "mule")
        )

        val counts = slots.mapValues { (_, keywords) ->
            items.count { item -> keywords.any { item.category.lowercase().contains(it) } }
        }

        val tops    = counts["Tops"]    ?: 0
        val bottoms = counts["Bottoms"] ?: 0
        val dresses = counts["Dresses"] ?: 0
        val outer   = counts["Outerwear"] ?: 0
        val shoes   = counts["Shoes"]   ?: 0
        val total   = items.size

        if (total == 0) return emptyList()

        val gaps = mutableListOf<WardrobeGap>()

        // Only analyze if wardrobe has at least 3 items
        if (total < 3) return emptyList()

        val usableTops = tops + dresses
        if (bottoms > 0 && usableTops == 0)
            gaps.add(WardrobeGap("Tops", tops, bottoms, "Add some tops — you have $bottoms bottoms but no tops to pair them with."))
        else if (tops > 0 && bottoms == 0 && dresses == 0)
            gaps.add(WardrobeGap("Bottoms", bottoms, tops, "Add some bottoms — you have $tops tops but nothing to wear below."))
        else if (tops > 0 && bottoms in 1..(tops / 3))
            gaps.add(WardrobeGap("Bottoms", bottoms, tops / 2, "You have $tops tops but only $bottoms bottoms — add more variety."))
        else if (bottoms > 0 && tops in 1..(bottoms / 3))
            gaps.add(WardrobeGap("Tops", tops, bottoms / 2, "You have $bottoms bottoms but only $tops tops — expand your top collection."))

        if (total >= 5 && outer == 0)
            gaps.add(WardrobeGap("Outerwear", 0, 1, "No outerwear detected — a jacket or blazer adds polish and versatility to most outfits."))

        if (total >= 4 && shoes == 0)
            gaps.add(WardrobeGap("Shoes", 0, 1, "No footwear in your wardrobe — add shoes to complete outfit suggestions."))
        else if (total >= 10 && shoes == 1)
            gaps.add(WardrobeGap("Shoes", 1, 2, "One pair of shoes limits variety — a second pair (casual + formal) opens many more outfits."))

        return gaps
    }

    // ── Color palette ─────────────────────────────────────────────────────────

    fun buildColorPalette(items: List<ClothingItem>): ColorPalette {
        val neutralColors = setOf("white", "black", "grey", "gray", "silver", "beige", "cream", "ivory", "ecru", "pearl", "off-white")
        val blueColors    = setOf("navy", "blue", "denim", "teal", "cobalt", "sky", "indigo", "sky blue")
        val earthyColors  = setOf("brown", "camel", "tan", "olive", "sage", "mustard", "khaki", "stone", "sand", "taupe", "mocha", "cognac", "chocolate")
        val warmColors    = setOf("red", "orange", "yellow", "pink", "coral", "rust", "terracotta", "scarlet", "crimson", "burgundy", "maroon", "rose", "blush", "gold", "peach", "magenta", "fuchsia")
        val coolColors    = setOf("green", "purple", "lavender", "plum", "violet", "lilac", "forest", "mint", "emerald")

        var neutrals = 0; var blues = 0; var earthy = 0; var warm = 0; var cool = 0

        for (item in items) {
            val color = item.dominantColor?.lowercase() ?: continue
            when {
                neutralColors.any { color.contains(it) } -> neutrals++
                blueColors.any    { color.contains(it) } -> blues++
                earthyColors.any  { color.contains(it) } -> earthy++
                warmColors.any    { color.contains(it) } -> warm++
                coolColors.any    { color.contains(it) } -> cool++
            }
        }

        return ColorPalette(neutrals, blues, earthy, warm, cool, items.size)
    }

    // ── Style DNA ─────────────────────────────────────────────────────────────

    fun buildStyleDna(items: List<ClothingItem>): StyleDna {
        if (items.isEmpty()) return StyleDna(emptyMap(), "Undefined", null)

        val counts = mutableMapOf<String, Int>()
        for (item in items) {
            for (style in item.styleTypes) {
                if (style.isNotBlank()) counts[style] = (counts[style] ?: 0) + 1
            }
        }

        if (counts.isEmpty()) return StyleDna(emptyMap(), "Undefined", null)

        val total = counts.values.sum().toFloat()
        val breakdown = counts.mapValues { it.value / total }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }

        val sorted = breakdown.entries.toList()
        return StyleDna(
            breakdown = breakdown,
            dominant  = sorted.firstOrNull()?.key ?: "Undefined",
            secondary = sorted.getOrNull(1)?.key
        )
    }
}
