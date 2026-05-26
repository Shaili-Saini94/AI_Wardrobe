package com.fashionai.sdk.categorization

import com.fashionai.sdk.color.DetectedColor
import com.fashionai.sdk.color.FashionColorDictionary
import com.fashionai.sdk.model.*

/**
 * Generates descriptive fashion tags for a clothing item
 * based on its detected type, colors, and pattern.
 */
internal class TagEngine {

    fun generateTags(
        subType: String,
        colors: List<DetectedColor>,
        pattern: Pattern,
        occasions: List<Occasion>,
        seasons: List<Season>,
        styleTypes: List<StyleType>
    ): List<String> {
        val tags = mutableSetOf<String>()

        // Sub-type inherent tags
        tags += subTypeTags(subType)

        // Color-based tags
        tags += colorTags(colors)

        // Pattern-based tags
        tags += patternTags(pattern)

        // Occasion-based tags
        tags += occasionTags(occasions)

        // Season-based tags
        tags += seasonTags(seasons)

        // Style-based tags
        tags += styleTags(styleTypes)

        return tags.toList().take(12) // Cap at 12 tags
    }

    private fun subTypeTags(subType: String): Set<String> = when (subType) {
        "T-Shirt" -> setOf("casual", "everyday", "versatile", "comfortable")
        "Polo" -> setOf("smart casual", "preppy", "versatile", "business casual")
        "Shirt" -> setOf("versatile", "classic", "professional", "layerable")
        "Hoodie" -> setOf("comfortable", "cozy", "casual", "layerable", "streetwear")
        "Sweatshirt" -> setOf("comfortable", "casual", "cozy", "laid-back")
        "Jacket" -> setOf("layering piece", "outerwear", "versatile", "statement piece")
        "Blazer" -> setOf("power piece", "professional", "elevating", "smart")
        "Crop Top" -> setOf("trendy", "fashion-forward", "summer", "casual")
        "Sleeveless Top" -> setOf("summer", "breathable", "casual", "layerable")
        "Full Sleeve Top" -> setOf("layerable", "classic", "versatile")
        "Jeans" -> setOf("versatile", "classic", "denim", "all-rounder", "everyday")
        "High Waist Jeans" -> setOf("trendy", "flattering", "figure-enhancing", "denim")
        "Trousers" -> setOf("professional", "classic", "tailored", "versatile")
        "Joggers" -> setOf("athletic", "comfortable", "casual", "sporty", "relaxed")
        "Cargo Pants" -> setOf("utilitarian", "edgy", "pockets", "casual", "streetwear")
        "Shorts" -> setOf("summer", "casual", "breathable", "relaxed")
        "Skirt" -> setOf("feminine", "versatile", "stylish")
        "Dress" -> setOf("effortless", "complete outfit", "feminine", "versatile")
        "One-piece" -> setOf("complete outfit", "effortless", "chic")
        "Ethnic Wear" -> setOf("traditional", "cultural", "occasion wear", "elegant")
        "Traditional Wear" -> setOf("traditional", "cultural", "heritage", "occasion wear")
        "Sneakers" -> setOf("casual", "comfortable", "versatile", "sporty", "everyday")
        "Sports Shoes" -> setOf("athletic", "performance", "comfortable", "sporty")
        "Formal Shoes" -> setOf("professional", "polished", "occasion wear", "dress shoes")
        "Boots" -> setOf("statement footwear", "durable", "versatile", "edgy")
        "Heels" -> setOf("elevating", "occasion footwear", "feminine", "polished")
        "Sandals" -> setOf("summer", "breathable", "casual", "beach-ready")
        "Slippers" -> setOf("comfort", "home", "relaxed", "casual")
        else -> setOf("versatile")
    }

    private fun colorTags(colors: List<DetectedColor>): Set<String> {
        val tags = mutableSetOf<String>()
        val primaryColor = colors.firstOrNull() ?: return tags
        val isNeutral = FashionColorDictionary.isNeutral(primaryColor.hex)

        if (isNeutral) {
            tags += "neutral"
            tags += "easy to match"
            tags += "wardrobe staple"
        } else {
            tags += "colored"
            tags += "statement color"
        }

        when (primaryColor.name.lowercase()) {
            "black" -> tags += listOf("classic", "timeless", "versatile", "slimming")
            "white" -> tags += listOf("fresh", "clean", "versatile", "crisp")
            "navy blue" -> tags += listOf("nautical", "classic", "versatile", "polished")
            "beige" -> tags += listOf("neutral", "earth tone", "versatile", "minimalist")
            "khaki" -> tags += listOf("utilitarian", "classic", "casual", "earthy")
            "grey", "charcoal" -> tags += listOf("classic", "neutral", "timeless")
            else -> {}
        }

        return tags
    }

    private fun patternTags(pattern: Pattern): Set<String> = when (pattern) {
        Pattern.SOLID -> setOf("solid", "clean", "easy to pair")
        Pattern.STRIPED -> setOf("striped", "nautical", "preppy", "classic pattern")
        Pattern.CHECKED -> setOf("checked", "plaid", "heritage pattern", "classic")
        Pattern.FLORAL -> setOf("floral", "feminine", "romantic", "spring vibes")
        Pattern.GEOMETRIC -> setOf("geometric", "modern", "graphic", "contemporary")
        Pattern.ABSTRACT -> setOf("abstract", "artistic", "statement", "unique")
        Pattern.ANIMAL_PRINT -> setOf("animal print", "bold", "fierce", "statement")
        Pattern.GRAPHIC -> setOf("graphic", "expressive", "streetwear", "bold")
        Pattern.UNKNOWN -> emptySet()
    }

    private fun occasionTags(occasions: List<Occasion>): Set<String> = buildSet {
        if (Occasion.WORK in occasions) add("office ready")
        if (Occasion.FORMAL in occasions) add("occasion wear")
        if (Occasion.PARTY in occasions) add("party ready")
        if (Occasion.SPORTS in occasions) add("activewear")
        if (Occasion.BEACH in occasions) add("beach ready")
        if (Occasion.TRAVEL in occasions) add("travel friendly")
        if (Occasion.DATE in occasions) add("date night")
        if (Occasion.OUTDOOR in occasions) add("outdoor ready")
    }

    private fun seasonTags(seasons: List<Season>): Set<String> = buildSet {
        if (Season.ALL_SEASON in seasons) add("year-round")
        if (Season.SUMMER in seasons) add("summer")
        if (Season.WINTER in seasons) add("winter")
        if (Season.SPRING in seasons) add("spring")
        if (Season.AUTUMN in seasons) add("autumn")
    }

    private fun styleTags(styleTypes: List<StyleType>): Set<String> = buildSet {
        styleTypes.forEach { style ->
            add(style.displayName.lowercase())
        }
    }

    fun inferGenderRelevance(subType: String, colors: List<DetectedColor>): GenderRelevance {
        val feminine = setOf(
            "Heels", "Skirt", "Crop Top", "Dress", "One-piece",
            "Traditional Wear (Women)", "High Waist Jeans"
        )
        val masculine = setOf(
            "Sherwani", "Traditional Wear (Men)"
        )
        return when {
            subType in feminine -> GenderRelevance.FEMININE
            subType in masculine -> GenderRelevance.MASCULINE
            else -> GenderRelevance.UNISEX
        }
    }

    fun inferFormality(subType: String): FormalityLevel = when (subType) {
        "Blazer", "Formal Shoes", "Heels", "Dress", "Trousers",
        "Ethnic Wear", "Traditional Wear" -> FormalityLevel.SEMI_FORMAL
        "Shirt", "Polo", "Boots" -> FormalityLevel.SMART_CASUAL
        "T-Shirt", "Jeans", "Sneakers", "Shorts",
        "Crop Top", "Skirt", "Sandals" -> FormalityLevel.CASUAL
        "Sports Shoes", "Joggers" -> FormalityLevel.ATHLETIC
        else -> FormalityLevel.CASUAL
    }
}
