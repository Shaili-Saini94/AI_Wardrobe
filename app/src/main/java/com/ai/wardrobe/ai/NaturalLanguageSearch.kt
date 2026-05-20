package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NaturalLanguageSearch @Inject constructor() {

    private val colorWords = setOf(
        "white", "black", "grey", "gray", "beige", "tan", "brown", "camel", "red", "crimson",
        "burgundy", "maroon", "orange", "coral", "peach", "rust", "terracotta", "yellow",
        "mustard", "gold", "green", "olive", "sage", "mint", "forest", "navy", "blue", "denim",
        "teal", "sky", "cobalt", "indigo", "purple", "lavender", "plum", "violet", "lilac",
        "pink", "blush", "rose", "magenta", "fuchsia", "cream", "ivory", "neutral"
    )

    private val occasionWords = mapOf(
        "office" to "Work", "work" to "Work", "meeting" to "Work", "professional" to "Work",
        "date" to "Date Night", "romantic" to "Date Night", "dinner" to "Date Night",
        "party" to "Party", "celebration" to "Party", "night out" to "Party",
        "casual" to "Casual", "everyday" to "Casual", "relaxed" to "Casual",
        "gym" to "Gym", "workout" to "Gym", "sport" to "Gym", "exercise" to "Gym",
        "beach" to "Beach", "pool" to "Beach", "summer" to "Beach",
        "travel" to "Travel", "trip" to "Travel", "airport" to "Travel",
        "formal" to "Formal Event", "wedding" to "Formal Event", "gala" to "Formal Event",
        "festival" to "Festival", "ethnic" to "Festival"
    )

    private val weatherWords = mapOf(
        "cold" to "Winter", "freezing" to "Winter", "winter" to "Winter", "snow" to "Winter",
        "warm" to "Summer", "hot" to "Summer", "summer" to "Summer", "sunny" to "Summer",
        "cool" to "Autumn", "crisp" to "Autumn", "autumn" to "Autumn", "fall" to "Autumn",
        "spring" to "Spring", "mild" to "Spring", "breezy" to "Spring",
        "rainy" to "Autumn", "monsoon" to "Autumn"
    )

    private val vibeWords = mapOf(
        "cozy" to listOf("Casual", "Minimalist"), "comfortable" to listOf("Casual", "Athleisure"),
        "chic" to listOf("Classic", "Minimalist"), "elegant" to listOf("Classic", "Formal"),
        "bold" to listOf("Funky", "Streetwear"), "funky" to listOf("Funky"),
        "minimal" to listOf("Minimalist"), "simple" to listOf("Minimalist"),
        "ethnic" to listOf("Ethnic", "Bohemian"), "traditional" to listOf("Ethnic"),
        "sporty" to listOf("Athleisure"), "athletic" to listOf("Athleisure"),
        "edgy" to listOf("Streetwear", "Funky"), "street" to listOf("Streetwear"),
        "boho" to listOf("Bohemian"), "bohemian" to listOf("Bohemian"),
        "smart" to listOf("Smart Casual"), "semi-formal" to listOf("Smart Casual"),
        "classic" to listOf("Classic"), "timeless" to listOf("Classic"),
        "formal" to listOf("Formal"), "business" to listOf("Smart Casual", "Formal")
    )

    private val categoryAliases = mapOf(
        "top" to "Shirt", "shirt" to "Shirt", "blouse" to "Shirt",
        "tee" to "T-Shirt", "t-shirt" to "T-Shirt", "tshirt" to "T-Shirt",
        "jeans" to "Jeans", "denim" to "Jeans",
        "pants" to "Pants", "trousers" to "Pants",
        "shorts" to "Shorts",
        "dress" to "Dress", "gown" to "Dress",
        "jacket" to "Jacket", "coat" to "Jacket", "blazer" to "Jacket",
        "sweater" to "Sweater", "hoodie" to "Sweater", "knit" to "Sweater",
        "kurta" to "Kurta", "kurti" to "Kurta",
        "saree" to "Saree", "sari" to "Saree",
        "lehenga" to "Lehenga",
        "shoes" to "Shoes", "sneakers" to "Shoes", "boots" to "Shoes",
        "sandals" to "Sandals", "slippers" to "Sandals",
        "hat" to "Hat", "cap" to "Hat"
    )

    data class SearchResult(val item: ClothingItem, val score: Int, val matchReasons: List<String>)

    fun search(query: String, items: List<ClothingItem>): List<SearchResult> {
        if (query.isBlank()) return items.map { SearchResult(it, 0, emptyList()) }

        val q = query.lowercase()
        val words = q.split(Regex("\\s+"))

        // Extract intents from query
        val matchedColors    = colorWords.filter { q.contains(it) }
        val matchedOccasions = occasionWords.filter { (k, _) -> q.contains(k) }.values.distinct()
        val matchedSeasons   = weatherWords.filter { (k, _) -> q.contains(k) }.values.distinct()
        val matchedVibes     = vibeWords.filter { (k, _) -> q.contains(k) }.values.flatten().distinct()
        val matchedCategory  = categoryAliases.filter { (k, _) -> q.contains(k) }.values.firstOrNull()

        return items.mapNotNull { item ->
            var score = 0
            val reasons = mutableListOf<String>()

            // Category match (highest weight)
            if (matchedCategory != null && item.category.equals(matchedCategory, ignoreCase = true)) {
                score += 4; reasons.add("category")
            }
            // Direct category word in query
            if (words.any { item.category.lowercase().contains(it) }) {
                score += 3; if ("category" !in reasons) reasons.add("category")
            }

            // Color match
            if (matchedColors.isNotEmpty()) {
                val itemColor = item.dominantColor?.lowercase() ?: ""
                val tagColors = item.tags.joinToString(" ").lowercase()
                if (matchedColors.any { itemColor.contains(it) || tagColors.contains(it) }) {
                    score += 3; reasons.add("color")
                }
            }

            // Occasion match
            for (occ in matchedOccasions) {
                if (item.occasions.any { it.equals(occ, ignoreCase = true) }) {
                    score += 2; reasons.add("occasion"); break
                }
            }

            // Season/weather match
            for (season in matchedSeasons) {
                if (item.seasons.any { it.equals(season, ignoreCase = true) }) {
                    score += 2; reasons.add("weather"); break
                }
            }

            // Vibe/style match
            for (vibe in matchedVibes) {
                if (item.styleTypes.any { it.equals(vibe, ignoreCase = true) }) {
                    score += 2; reasons.add("style"); break
                }
            }

            // Tag match — any word in the query matches a tag
            val tagText = item.tags.joinToString(" ").lowercase()
            for (word in words) {
                if (word.length > 3 && tagText.contains(word)) {
                    score += 1; reasons.add("tags"); break
                }
            }

            if (score > 0) SearchResult(item, score, reasons.distinct()) else null
        }.sortedByDescending { it.score }
    }
}
