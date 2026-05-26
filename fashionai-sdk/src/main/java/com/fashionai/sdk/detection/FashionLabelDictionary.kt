package com.fashionai.sdk.detection

import com.fashionai.sdk.model.ClothingType
import com.fashionai.sdk.model.Occasion
import com.fashionai.sdk.model.Season
import com.fashionai.sdk.model.StyleType

/**
 * Maps raw model labels → ClothingType + display names.
 *
 * Handles BOTH formats:
 *  - Clean:    "t_shirt", "polo_shirt"
 *  - ImageNet: "jersey, T-shirt, T shirt", "suit, suit of clothes"
 *
 * EfficientNet-Lite / MobileNet from TFHub use ImageNet format
 * (comma-separated multi-word). This splits each token so every
 * ImageNet clothing class is correctly identified.
 */
internal object FashionLabelDictionary {

    // ─── Token → Display name ──────────────────────────────────────────────

    private val TOKEN_TO_DISPLAY: Map<String, String> = mapOf(
        "jersey" to "T-Shirt", "t-shirt" to "T-Shirt", "t shirt" to "T-Shirt",
        "tshirt" to "T-Shirt", "polo" to "Polo", "polo shirt" to "Polo",
        "dress shirt" to "Shirt", "shirt" to "Shirt", "button-down" to "Shirt",
        "button down" to "Shirt", "guernsey" to "Full Sleeve Top",
        "cardigan" to "Full Sleeve Top", "sweater" to "Full Sleeve Top",
        "pullover" to "Full Sleeve Top", "turtleneck" to "Full Sleeve Top",
        "sweatshirt" to "Sweatshirt", "hoodie" to "Hoodie", "hooded" to "Hoodie",
        "blouse" to "Sleeveless Top", "tank top" to "Sleeveless Top",
        "tank suit" to "Sleeveless Top", "brassiere" to "Sleeveless Top",
        "bra" to "Sleeveless Top", "bandeau" to "Crop Top", "crop top" to "Crop Top",
        "halter" to "Crop Top", "vest" to "Sleeveless Top", "waistcoat" to "Blazer",
        "blazer" to "Blazer", "suit" to "Blazer", "jacket" to "Jacket",
        "windbreaker" to "Jacket", "trench coat" to "Jacket", "trench" to "Jacket",
        "raincoat" to "Jacket", "overcoat" to "Jacket", "fur coat" to "Jacket",
        "lab coat" to "Jacket", "white coat" to "Jacket", "poncho" to "Jacket",
        "cape" to "Jacket", "military uniform" to "Jacket", "uniform" to "Shirt",

        "jean" to "Jeans", "jeans" to "Jeans", "denim" to "Jeans",
        "blue jean" to "Jeans", "trouser" to "Trousers", "trousers" to "Trousers",
        "pants" to "Trousers", "chino" to "Trousers", "slacks" to "Trousers",
        "jogger" to "Joggers", "sweatpants" to "Joggers", "track pants" to "Joggers",
        "cargo" to "Cargo Pants", "shorts" to "Shorts", "short" to "Shorts",
        "swimming trunks" to "Shorts", "bathing trunks" to "Shorts",
        "board shorts" to "Shorts", "skirt" to "Skirt", "miniskirt" to "Skirt",
        "mini" to "Skirt", "overskirt" to "Skirt", "sarong" to "Skirt",
        "kilt" to "Skirt",

        "gown" to "Dress", "dress" to "Dress", "frock" to "Dress",
        "sundress" to "Dress", "cocktail dress" to "Dress", "evening dress" to "Dress",
        "pinafore" to "Dress", "jumpsuit" to "One-piece", "romper" to "One-piece",
        "coverall" to "One-piece", "playsuit" to "One-piece", "maillot" to "One-piece",
        "swimsuit" to "One-piece", "bikini" to "One-piece", "kimono" to "Traditional Wear",
        "abaya" to "Traditional Wear", "sari" to "Traditional Wear",
        "saree" to "Traditional Wear", "kurta" to "Ethnic Wear",
        "sherwani" to "Traditional Wear", "vestment" to "Traditional Wear",
        "robe" to "Traditional Wear", "cloak" to "Traditional Wear",

        "sneaker" to "Sneakers", "running shoe" to "Sports Shoes",
        "athletic shoe" to "Sports Shoes", "gym shoe" to "Sports Shoes",
        "tennis shoe" to "Sneakers", "oxford" to "Formal Shoes",
        "oxford shoe" to "Formal Shoes", "loafer" to "Formal Shoes",
        "pump" to "Formal Shoes", "formal shoe" to "Formal Shoes",
        "dress shoe" to "Formal Shoes", "boot" to "Boots", "ankle boot" to "Boots",
        "chelsea boot" to "Boots", "cowboy boot" to "Boots", "knee boot" to "Boots",
        "stiletto" to "Heels", "high heel" to "Heels", "heel" to "Heels",
        "platform shoe" to "Heels", "wedge" to "Heels", "sandal" to "Sandals",
        "flip-flop" to "Slippers", "flip flop" to "Slippers",
        "slipper" to "Slippers", "moccasin" to "Slippers",
        "clog" to "Sandals", "espadrille" to "Sandals"
    )

    // ─── Token sets per type ───────────────────────────────────────────────

    private val TOP_TOKENS = setOf(
        "jersey", "t-shirt", "t shirt", "tshirt", "polo", "polo shirt",
        "dress shirt", "shirt", "button-down", "button down", "guernsey",
        "cardigan", "sweater", "pullover", "turtleneck", "sweatshirt",
        "hoodie", "hooded", "blouse", "tank top", "tank suit", "brassiere",
        "bra", "bandeau", "crop top", "halter", "vest", "waistcoat",
        "blazer", "suit", "jacket", "windbreaker", "trench coat", "trench",
        "raincoat", "overcoat", "fur coat", "lab coat", "white coat",
        "poncho", "cape", "military uniform", "uniform"
    )

    private val BOTTOM_TOKENS = setOf(
        "jean", "jeans", "denim", "blue jean", "trouser", "trousers",
        "pants", "chino", "slacks", "jogger", "sweatpants", "track pants",
        "cargo", "shorts", "short", "swimming trunks", "bathing trunks",
        "board shorts", "skirt", "miniskirt", "mini", "overskirt",
        "sarong", "kilt"
    )

    private val FULL_BODY_TOKENS = setOf(
        "gown", "dress", "frock", "sundress", "cocktail dress", "evening dress",
        "pinafore", "jumpsuit", "romper", "coverall", "playsuit", "maillot",
        "swimsuit", "bikini", "kimono", "abaya", "sari", "saree", "kurta",
        "sherwani", "vestment", "robe", "cloak"
    )

    private val FOOTWEAR_TOKENS = setOf(
        "sneaker", "running shoe", "athletic shoe", "gym shoe", "tennis shoe",
        "oxford", "oxford shoe", "loafer", "pump", "formal shoe", "dress shoe",
        "boot", "ankle boot", "chelsea boot", "cowboy boot", "knee boot",
        "stiletto", "high heel", "heel", "platform shoe", "wedge", "sandal",
        "flip-flop", "flip flop", "slipper", "moccasin", "clog", "espadrille"
    )

    private val ACCESSORY_TOKENS = setOf(
        "hat", "cap", "sunglasses", "scarf", "stole", "tie", "bow tie",
        "bolo tie", "belt", "handbag", "purse", "backpack", "wallet",
        "watch", "bracelet", "necklace", "ring", "glove", "mitten", "sock"
    )

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Returns [ClothingType] for any label format — clean or ImageNet.
     * "jersey, T-shirt, T shirt" → TOP
     * "t_shirt" → TOP
     */
    fun typeForLabel(rawLabel: String): ClothingType {
        for (token in splitAndNormalize(rawLabel)) {
            when {
                TOP_TOKENS.any { token.contains(it) }       -> return ClothingType.TOP
                BOTTOM_TOKENS.any { token.contains(it) }    -> return ClothingType.BOTTOM
                FULL_BODY_TOKENS.any { token.contains(it) } -> return ClothingType.FULL_BODY
                FOOTWEAR_TOKENS.any { token.contains(it) }  -> return ClothingType.FOOTWEAR
                ACCESSORY_TOKENS.any { token.contains(it) } -> return ClothingType.ACCESSORY
            }
        }
        return ClothingType.UNKNOWN
    }

    /**
     * Returns a human-readable sub-type name for any label format.
     * "jersey, T-shirt, T shirt" → "T-Shirt"
     */
    fun displayNameForLabel(rawLabel: String): String {
        for (token in splitAndNormalize(rawLabel)) {
            val match = TOKEN_TO_DISPLAY.entries
                .firstOrNull { (key, _) -> token.contains(key) }
            if (match != null) return match.value
        }
        return rawLabel.split(",").first().trim()
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    /**
     * Splits "jersey, T-shirt, T shirt" into ["jersey", "t-shirt", "t shirt"]
     * and also includes the full normalized string for single-token labels.
     */
    private fun splitAndNormalize(raw: String): List<String> {
        val normalized = raw.lowercase().trim().replace("_", " ")
        val tokens = normalized.split(",").map { it.trim() }
        return (tokens + normalized).distinct().filter { it.isNotBlank() }
    }

    // ─── Occasion / Season / Style lookups ────────────────────────────────

    val SUBTYPE_TO_OCCASIONS: Map<String, List<Occasion>> = mapOf(
        "T-Shirt"          to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.TRAVEL, Occasion.HOME),
        "Polo"             to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL, Occasion.DATE),
        "Shirt"            to listOf(Occasion.WORK, Occasion.CASUAL, Occasion.FORMAL, Occasion.DATE),
        "Hoodie"           to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.OUTDOOR, Occasion.HOME),
        "Sweatshirt"       to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.HOME),
        "Jacket"           to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL, Occasion.OUTDOOR),
        "Blazer"           to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE, Occasion.PARTY),
        "Full Sleeve Top"  to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL),
        "Crop Top"         to listOf(Occasion.CASUAL, Occasion.PARTY, Occasion.BEACH, Occasion.DATE),
        "Sleeveless Top"   to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.SPORTS, Occasion.PARTY),
        "Jeans"            to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.DATE, Occasion.TRAVEL),
        "High Waist Jeans" to listOf(Occasion.CASUAL, Occasion.DATE, Occasion.PARTY),
        "Trousers"         to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE),
        "Joggers"          to listOf(Occasion.SPORTS, Occasion.CASUAL, Occasion.HOME),
        "Cargo Pants"      to listOf(Occasion.CASUAL, Occasion.OUTDOOR, Occasion.TRAVEL),
        "Shorts"           to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.SPORTS, Occasion.HOME),
        "Skirt"            to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.DATE, Occasion.PARTY),
        "Dress"            to listOf(Occasion.PARTY, Occasion.DATE, Occasion.FORMAL, Occasion.CASUAL),
        "One-piece"        to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.PARTY),
        "Ethnic Wear"      to listOf(Occasion.FORMAL, Occasion.PARTY, Occasion.WORK),
        "Traditional Wear" to listOf(Occasion.FORMAL, Occasion.PARTY),
        "Sneakers"         to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.TRAVEL),
        "Sports Shoes"     to listOf(Occasion.SPORTS, Occasion.CASUAL, Occasion.OUTDOOR),
        "Formal Shoes"     to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE),
        "Boots"            to listOf(Occasion.CASUAL, Occasion.OUTDOOR, Occasion.DATE, Occasion.WORK),
        "Heels"            to listOf(Occasion.PARTY, Occasion.FORMAL, Occasion.DATE, Occasion.WORK),
        "Sandals"          to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.TRAVEL),
        "Slippers"         to listOf(Occasion.HOME, Occasion.CASUAL)
    )

    val SUBTYPE_TO_SEASONS: Map<String, List<Season>> = mapOf(
        "T-Shirt"          to listOf(Season.SPRING, Season.SUMMER, Season.ALL_SEASON),
        "Polo"             to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "Shirt"            to listOf(Season.ALL_SEASON),
        "Hoodie"           to listOf(Season.AUTUMN, Season.WINTER),
        "Sweatshirt"       to listOf(Season.AUTUMN, Season.WINTER),
        "Jacket"           to listOf(Season.AUTUMN, Season.WINTER, Season.SPRING),
        "Blazer"           to listOf(Season.ALL_SEASON),
        "Full Sleeve Top"  to listOf(Season.AUTUMN, Season.WINTER, Season.SPRING),
        "Crop Top"         to listOf(Season.SUMMER, Season.SPRING),
        "Sleeveless Top"   to listOf(Season.SUMMER),
        "Jeans"            to listOf(Season.ALL_SEASON),
        "Trousers"         to listOf(Season.ALL_SEASON),
        "Joggers"          to listOf(Season.AUTUMN, Season.WINTER, Season.ALL_SEASON),
        "Cargo Pants"      to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "Shorts"           to listOf(Season.SUMMER, Season.SPRING),
        "Skirt"            to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "Dress"            to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "One-piece"        to listOf(Season.SPRING, Season.SUMMER),
        "Ethnic Wear"      to listOf(Season.ALL_SEASON),
        "Traditional Wear" to listOf(Season.ALL_SEASON),
        "Sneakers"         to listOf(Season.ALL_SEASON),
        "Sports Shoes"     to listOf(Season.ALL_SEASON),
        "Formal Shoes"     to listOf(Season.ALL_SEASON),
        "Boots"            to listOf(Season.AUTUMN, Season.WINTER),
        "Heels"            to listOf(Season.ALL_SEASON),
        "Sandals"          to listOf(Season.SPRING, Season.SUMMER),
        "Slippers"         to listOf(Season.ALL_SEASON)
    )

    val SUBTYPE_TO_STYLES: Map<String, List<StyleType>> = mapOf(
        "T-Shirt"          to listOf(StyleType.CASUAL_CHIC, StyleType.STREETWEAR, StyleType.MINIMAL),
        "Polo"             to listOf(StyleType.PREPPY, StyleType.CLASSIC, StyleType.CASUAL_CHIC),
        "Shirt"            to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.PREPPY),
        "Hoodie"           to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.ATHLETIC),
        "Sweatshirt"       to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC),
        "Jacket"           to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.CLASSIC),
        "Blazer"           to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.LUXURY),
        "Crop Top"         to listOf(StyleType.STREETWEAR, StyleType.EDGY, StyleType.BOHEMIAN),
        "Sleeveless Top"   to listOf(StyleType.MINIMAL, StyleType.CASUAL_CHIC, StyleType.BOHEMIAN),
        "Jeans"            to listOf(StyleType.CASUAL_CHIC, StyleType.STREETWEAR, StyleType.CLASSIC),
        "Trousers"         to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.LUXURY),
        "Joggers"          to listOf(StyleType.ATHLETIC, StyleType.STREETWEAR),
        "Cargo Pants"      to listOf(StyleType.STREETWEAR, StyleType.EDGY),
        "Shorts"           to listOf(StyleType.CASUAL_CHIC, StyleType.ATHLETIC, StyleType.BOHEMIAN),
        "Skirt"            to listOf(StyleType.BOHEMIAN, StyleType.CLASSIC, StyleType.PREPPY),
        "Dress"            to listOf(StyleType.CLASSIC, StyleType.BOHEMIAN, StyleType.LUXURY),
        "Ethnic Wear"      to listOf(StyleType.CLASSIC, StyleType.LUXURY),
        "Traditional Wear" to listOf(StyleType.CLASSIC, StyleType.LUXURY),
        "Sneakers"         to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.ATHLETIC),
        "Sports Shoes"     to listOf(StyleType.ATHLETIC),
        "Formal Shoes"     to listOf(StyleType.CLASSIC, StyleType.LUXURY),
        "Boots"            to listOf(StyleType.EDGY, StyleType.CLASSIC, StyleType.STREETWEAR),
        "Heels"            to listOf(StyleType.LUXURY, StyleType.CLASSIC, StyleType.EDGY),
        "Sandals"          to listOf(StyleType.BOHEMIAN, StyleType.CASUAL_CHIC, StyleType.MINIMAL)
    )
}
