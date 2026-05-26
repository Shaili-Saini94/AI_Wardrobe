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

data class SimilarPair(
    val itemA: ClothingItem,
    val itemB: ClothingItem,
    val reason: String,         // human-readable explanation
    val similarityScore: Float  // 0..1
)

data class ColorPalette(
    val neutrals: Int,
    val blues: Int,
    val earthy: Int,
    val warm: Int,
    val cool: Int,
    val total: Int,
    /** Top actual color names (from dominantColor field), sorted by frequency. Max 10. */
    val topColors: List<Pair<String, Int>> = emptyList()
) {
    fun dominantFamily(): String {
        val map = mapOf("Neutrals" to neutrals, "Blues" to blues,
            "Earthy" to earthy, "Warm" to warm, "Cool" to cool)
        return map.maxByOrNull { it.value }?.key ?: "Mixed"
    }
    /** Harmony score 0–1: high when wardrobe is mostly neutrals + 1 accent family. */
    fun harmonyScore(): Float {
        if (total == 0) return 0f
        val neutralRatio = neutrals / total.toFloat()
        val accentFamilies = listOf(blues, earthy, warm, cool).count { it > 0 }
        return when {
            neutralRatio > 0.7f && accentFamilies <= 1 -> 0.95f
            neutralRatio > 0.5f && accentFamilies <= 2 -> 0.80f
            neutralRatio > 0.3f && accentFamilies <= 3 -> 0.65f
            else -> 0.45f
        }
    }
    fun harmonyLabel(): String = when {
        harmonyScore() >= 0.90f -> "Perfectly curated"
        harmonyScore() >= 0.75f -> "Well balanced"
        harmonyScore() >= 0.60f -> "Diverse mix"
        else                    -> "Bold & eclectic"
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
        val neutralColors = setOf("white", "black", "grey", "gray", "silver", "beige", "cream", "ivory", "ecru", "pearl", "off-white", "charcoal", "slate")
        val blueColors    = setOf("navy", "blue", "denim", "teal", "cobalt", "sky", "indigo")
        val earthyColors  = setOf("brown", "camel", "tan", "olive", "sage", "mustard", "khaki", "stone", "sand", "taupe", "mocha", "cognac", "chocolate")
        val warmColors    = setOf("red", "orange", "yellow", "pink", "coral", "rust", "terracotta", "scarlet", "crimson", "burgundy", "maroon", "rose", "blush", "gold", "peach", "magenta", "fuchsia")
        val coolColors    = setOf("green", "purple", "lavender", "plum", "violet", "lilac", "forest", "mint", "emerald")

        var neutrals = 0; var blues = 0; var earthy = 0; var warm = 0; var cool = 0

        // Track actual color names for swatches
        val colorCounts = mutableMapOf<String, Int>()

        for (item in items) {
            val raw = item.dominantColor?.lowercase()?.trim() ?: continue
            // Normalize to canonical color name (first matching keyword)
            val canonical = (neutralColors + blueColors + earthyColors + warmColors + coolColors)
                .firstOrNull { raw.contains(it) } ?: raw.ifBlank { null } ?: continue
            colorCounts[canonical] = (colorCounts[canonical] ?: 0) + 1
            when {
                neutralColors.any { raw.contains(it) } -> neutrals++
                blueColors.any    { raw.contains(it) } -> blues++
                earthyColors.any  { raw.contains(it) } -> earthy++
                warmColors.any    { raw.contains(it) } -> warm++
                coolColors.any    { raw.contains(it) } -> cool++
            }
        }

        val topColors = colorCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { Pair(it.key, it.value) }

        return ColorPalette(neutrals, blues, earthy, warm, cool, items.size, topColors)
    }

    // ── Duplicate / Similar Item Detector ────────────────────────────────────

    fun detectSimilars(items: List<ClothingItem>): List<SimilarPair> {
        val results = mutableListOf<SimilarPair>()
        for (i in items.indices) {
            for (j in i + 1 until items.size) {
                val a = items[i]
                val b = items[j]
                val score = computeSimilarityScore(a, b)
                if (score >= 0.65f) {
                    results.add(SimilarPair(a, b, buildSimilarityReason(a, b, score), score))
                }
            }
        }
        return results.sortedByDescending { it.similarityScore }.take(8)
    }

    private fun computeSimilarityScore(a: ClothingItem, b: ClothingItem): Float {
        var score = 0f

        // Same base category (strong signal)
        if (normalizeCategory(a.category) == normalizeCategory(b.category)) score += 0.40f

        // Same dominant color
        val colorA = a.dominantColor?.lowercase()?.trim()
        val colorB = b.dominantColor?.lowercase()?.trim()
        if (colorA != null && colorA == colorB) score += 0.25f

        // Shared occasions
        val sharedOccasions = a.occasions.intersect(b.occasions.toSet()).size
        score += (sharedOccasions.coerceAtMost(2) * 0.10f)

        // Shared style types
        val sharedStyles = a.styleTypes.intersect(b.styleTypes.toSet()).size
        score += (sharedStyles.coerceAtMost(2) * 0.10f)

        // Shared tags (fabric-type overlap)
        val sharedTags = a.tags.intersect(b.tags.toSet()).size
        score += (sharedTags.coerceAtMost(3) * 0.05f)

        return score.coerceIn(0f, 1f)
    }

    private fun normalizeCategory(cat: String): String {
        val c = cat.lowercase()
        return when {
            c.contains("shirt") || c.contains("tee") || c.contains("blouse") || c.contains("top") -> "top"
            c.contains("pant") || c.contains("jean") || c.contains("trouser") -> "bottom"
            c.contains("short") -> "shorts"
            c.contains("skirt") -> "skirt"
            c.contains("dress") -> "dress"
            c.contains("shoe") || c.contains("sneaker") || c.contains("boot") -> "footwear"
            c.contains("jacket") || c.contains("coat") -> "outerwear"
            else -> c.take(6)
        }
    }

    private fun buildSimilarityReason(a: ClothingItem, b: ClothingItem, score: Float): String {
        val parts = mutableListOf<String>()
        if (normalizeCategory(a.category) == normalizeCategory(b.category))
            parts.add("same category (${a.category})")
        if (!a.dominantColor.isNullOrBlank() && a.dominantColor.equals(b.dominantColor, ignoreCase = true))
            parts.add("same color (${a.dominantColor})")
        val sharedOcc = a.occasions.intersect(b.occasions.toSet())
        if (sharedOcc.isNotEmpty()) parts.add("both for ${sharedOcc.first()}")
        if (parts.isEmpty()) parts.add("similar styling and tags")
        return parts.joinToString(", ")
            .replaceFirstChar { it.uppercase() }
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
