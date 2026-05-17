package com.fashionai.sdk.detection

import com.fashionai.sdk.model.ClothingType
import com.fashionai.sdk.model.Occasion
import com.fashionai.sdk.model.Season
import com.fashionai.sdk.model.StyleType

/**
 * Central mapping from raw model labels → structured SDK types.
 * Also contains occasion, season, and style inference tables.
 */
internal object FashionLabelDictionary {

    // ─── Label → Display Name ──────────────────────────────────────────────

    val LABEL_TO_DISPLAY: Map<String, String> = mapOf(
        // Tops
        "jersey" to "T-Shirt",
        "t_shirt" to "T-Shirt",
        "tshirt" to "T-Shirt",
        "polo_shirt" to "Polo",
        "polo" to "Polo",
        "dress_shirt" to "Shirt",
        "shirt" to "Shirt",
        "button_down" to "Shirt",
        "neck" to "Shirt",
        "apparel" to "Clothing",
        "clothing" to "Clothing",
        "garment" to "Clothing",
        "checked" to "Shirt",
        "plaid" to "Shirt",
        "patterned" to "Shirt",
        "top" to "Shirt",
        "fabric" to "Clothing",
        "hoodie" to "Hoodie",
        "sweatshirt" to "Sweatshirt",
        "windbreaker" to "Jacket",
        "jacket" to "Jacket",
        "blazer" to "Blazer",
        "cardigan" to "Full Sleeve Top",
        "crop_top" to "Crop Top",
        "tank_top" to "Sleeveless Top",
        "sleeveless" to "Sleeveless Top",
        "full_sleeve_top" to "Full Sleeve Top",
        "long_sleeve" to "Full Sleeve Top",
        "blouse" to "Blouse",
        "tunic" to "Blouse",
        "ruffle" to "Blouse",
        "frill" to "Blouse",
        "v_neck" to "Shirt",

        // Bottoms
        "denim" to "Jeans",
        "jeans" to "Jeans",
        "high_waist_jeans" to "High Waist Jeans",
        "chinos" to "Trousers",
        "trousers" to "Trousers",
        "pants" to "Trousers",
        "joggers" to "Joggers",
        "sweatpants" to "Joggers",
        "cargo" to "Cargo Pants",
        "cargo_pants" to "Cargo Pants",
        "miniskirt" to "Skirt",
        "midi_skirt" to "Skirt",
        "skirt" to "Skirt",
        "shorts" to "Shorts",
        "board_shorts" to "Shorts",

        // Full body
        "cocktail_dress" to "Dress",
        "maxi_dress" to "Dress",
        "mini_dress" to "Dress",
        "dress" to "Dress",
        "jumpsuit" to "One-piece",
        "one_piece" to "One-piece",
        "kurta" to "Ethnic Wear",
        "saree" to "Traditional Wear",
        "ethnic_wear" to "Ethnic Wear",
        "traditional_wear" to "Traditional Wear",
        "lehenga" to "Traditional Wear",
        "sherwani" to "Traditional Wear",

        // Footwear
        "running_shoe" to "Sports Shoes",
        "sneakers" to "Sneakers",
        "oxford_shoe" to "Formal Shoes",
        "formal_shoes" to "Formal Shoes",
        "ankle_boot" to "Boots",
        "boots" to "Boots",
        "stiletto" to "Heels",
        "heels" to "Heels",
        "flip_flop" to "Slippers",
        "slippers" to "Slippers",
        "strappy_sandal" to "Sandals",
        "sandals" to "Sandals",
        "sports_shoes" to "Sports Shoes"
    )

    // ─── Label → ClothingType ──────────────────────────────────────────────

    private val TOPS_LABELS = setOf(
        "jersey", "t_shirt", "tshirt", "polo_shirt", "polo", "dress_shirt", "shirt",
        "button_down", "hoodie", "sweatshirt", "windbreaker", "jacket", "blazer",
        "cardigan", "crop_top", "tank_top", "sleeveless", "full_sleeve_top", "long_sleeve",
        "neck", "apparel", "clothing", "garment", "checked", "plaid", "patterned", "top", "fabric",
        "blouse", "tunic", "ruffle", "frill", "v_neck"
    )

    private val BOTTOMS_LABELS = setOf(
        "denim", "jeans", "high_waist_jeans", "chinos", "trousers", "pants",
        "joggers", "sweatpants", "cargo", "cargo_pants", "miniskirt", "midi_skirt",
        "skirt", "shorts", "board_shorts"
    )

    private val FULL_BODY_LABELS = setOf(
        "cocktail_dress", "maxi_dress", "mini_dress", "dress", "jumpsuit", "one_piece",
        "kurta", "saree", "ethnic_wear", "traditional_wear", "lehenga", "sherwani"
    )

    private val FOOTWEAR_LABELS = setOf(
        "running_shoe", "sneakers", "oxford_shoe", "formal_shoes", "ankle_boot", "boots",
        "stiletto", "heels", "flip_flop", "slippers", "strappy_sandal", "sandals",
        "sports_shoes"
    )

    fun typeForLabel(label: String): ClothingType {
        val normalized = label.lowercase().replace(" ", "_")
        return when {
            TOPS_LABELS.any { normalized.contains(it) } -> ClothingType.TOP
            BOTTOMS_LABELS.any { normalized.contains(it) } -> ClothingType.BOTTOM
            FULL_BODY_LABELS.any { normalized.contains(it) } -> ClothingType.FULL_BODY
            FOOTWEAR_LABELS.any { normalized.contains(it) } -> ClothingType.FOOTWEAR
            else -> ClothingType.UNKNOWN
        }
    }

    fun displayNameForLabel(label: String): String {
        val normalized = label.lowercase().replace(" ", "_")
        return LABEL_TO_DISPLAY[normalized]
            ?: LABEL_TO_DISPLAY.entries.firstOrNull { normalized.contains(it.key) }?.value
            ?: label.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    // ─── Sub-type → Occasions ──────────────────────────────────────────────

    val SUBTYPE_TO_OCCASIONS: Map<String, List<Occasion>> = mapOf(
        "T-Shirt" to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.TRAVEL, Occasion.HOME),
        "Polo" to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL, Occasion.DATE),
        "Shirt" to listOf(Occasion.WORK, Occasion.CASUAL, Occasion.FORMAL, Occasion.DATE),
        "Hoodie" to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.OUTDOOR, Occasion.HOME),
        "Sweatshirt" to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.HOME),
        "Jacket" to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL, Occasion.OUTDOOR),
        "Blazer" to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE, Occasion.PARTY),
        "Full Sleeve Top" to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.TRAVEL),
        "Crop Top" to listOf(Occasion.CASUAL, Occasion.PARTY, Occasion.BEACH, Occasion.DATE),
        "Sleeveless Top" to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.SPORTS, Occasion.PARTY),

        "Jeans" to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.DATE, Occasion.TRAVEL),
        "High Waist Jeans" to listOf(Occasion.CASUAL, Occasion.DATE, Occasion.PARTY),
        "Trousers" to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE),
        "Joggers" to listOf(Occasion.SPORTS, Occasion.CASUAL, Occasion.HOME),
        "Cargo Pants" to listOf(Occasion.CASUAL, Occasion.OUTDOOR, Occasion.TRAVEL),
        "Shorts" to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.SPORTS, Occasion.HOME),
        "Skirt" to listOf(Occasion.CASUAL, Occasion.WORK, Occasion.DATE, Occasion.PARTY),

        "Dress" to listOf(Occasion.PARTY, Occasion.DATE, Occasion.FORMAL, Occasion.CASUAL),
        "One-piece" to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.PARTY),
        "Ethnic Wear" to listOf(Occasion.FORMAL, Occasion.PARTY, Occasion.WORK),
        "Traditional Wear" to listOf(Occasion.FORMAL, Occasion.PARTY),

        "Sneakers" to listOf(Occasion.CASUAL, Occasion.SPORTS, Occasion.TRAVEL),
        "Sports Shoes" to listOf(Occasion.SPORTS, Occasion.CASUAL, Occasion.OUTDOOR),
        "Formal Shoes" to listOf(Occasion.WORK, Occasion.FORMAL, Occasion.DATE),
        "Boots" to listOf(Occasion.CASUAL, Occasion.OUTDOOR, Occasion.DATE, Occasion.WORK),
        "Heels" to listOf(Occasion.PARTY, Occasion.FORMAL, Occasion.DATE, Occasion.WORK),
        "Sandals" to listOf(Occasion.CASUAL, Occasion.BEACH, Occasion.TRAVEL),
        "Slippers" to listOf(Occasion.HOME, Occasion.CASUAL)
    )

    // ─── Sub-type → Seasons ────────────────────────────────────────────────

    val SUBTYPE_TO_SEASONS: Map<String, List<Season>> = mapOf(
        "T-Shirt" to listOf(Season.SPRING, Season.SUMMER, Season.ALL_SEASON),
        "Polo" to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "Shirt" to listOf(Season.ALL_SEASON),
        "Hoodie" to listOf(Season.AUTUMN, Season.WINTER),
        "Sweatshirt" to listOf(Season.AUTUMN, Season.WINTER),
        "Jacket" to listOf(Season.AUTUMN, Season.WINTER, Season.SPRING),
        "Blazer" to listOf(Season.ALL_SEASON),
        "Full Sleeve Top" to listOf(Season.AUTUMN, Season.WINTER, Season.SPRING),
        "Crop Top" to listOf(Season.SUMMER, Season.SPRING),
        "Sleeveless Top" to listOf(Season.SUMMER),

        "Jeans" to listOf(Season.ALL_SEASON),
        "High Waist Jeans" to listOf(Season.ALL_SEASON),
        "Trousers" to listOf(Season.ALL_SEASON),
        "Joggers" to listOf(Season.AUTUMN, Season.WINTER, Season.ALL_SEASON),
        "Cargo Pants" to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "Shorts" to listOf(Season.SUMMER, Season.SPRING),
        "Skirt" to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),

        "Dress" to listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        "One-piece" to listOf(Season.SPRING, Season.SUMMER),
        "Ethnic Wear" to listOf(Season.ALL_SEASON),
        "Traditional Wear" to listOf(Season.ALL_SEASON),

        "Sneakers" to listOf(Season.ALL_SEASON),
        "Sports Shoes" to listOf(Season.ALL_SEASON),
        "Formal Shoes" to listOf(Season.ALL_SEASON),
        "Boots" to listOf(Season.AUTUMN, Season.WINTER),
        "Heels" to listOf(Season.ALL_SEASON),
        "Sandals" to listOf(Season.SPRING, Season.SUMMER),
        "Slippers" to listOf(Season.ALL_SEASON)
    )

    // ─── Sub-type → Style Types ────────────────────────────────────────────

    val SUBTYPE_TO_STYLES: Map<String, List<StyleType>> = mapOf(
        "T-Shirt" to listOf(StyleType.CASUAL_CHIC, StyleType.STREETWEAR, StyleType.MINIMAL),
        "Polo" to listOf(StyleType.PREPPY, StyleType.CLASSIC, StyleType.CASUAL_CHIC),
        "Shirt" to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.PREPPY),
        "Hoodie" to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.ATHLETIC),
        "Sweatshirt" to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC),
        "Jacket" to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.CLASSIC),
        "Blazer" to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.LUXURY),
        "Crop Top" to listOf(StyleType.STREETWEAR, StyleType.EDGY, StyleType.BOHEMIAN),
        "Sleeveless Top" to listOf(StyleType.MINIMAL, StyleType.CASUAL_CHIC, StyleType.BOHEMIAN),

        "Jeans" to listOf(StyleType.CASUAL_CHIC, StyleType.STREETWEAR, StyleType.CLASSIC),
        "High Waist Jeans" to listOf(StyleType.STREETWEAR, StyleType.EDGY, StyleType.CASUAL_CHIC),
        "Trousers" to listOf(StyleType.CLASSIC, StyleType.MINIMAL, StyleType.LUXURY),
        "Joggers" to listOf(StyleType.ATHLETIC, StyleType.STREETWEAR),
        "Cargo Pants" to listOf(StyleType.STREETWEAR, StyleType.EDGY),
        "Shorts" to listOf(StyleType.CASUAL_CHIC, StyleType.ATHLETIC, StyleType.BOHEMIAN),
        "Skirt" to listOf(StyleType.BOHEMIAN, StyleType.CLASSIC, StyleType.PREPPY, StyleType.EDGY),

        "Dress" to listOf(StyleType.CLASSIC, StyleType.BOHEMIAN, StyleType.LUXURY, StyleType.MINIMAL),
        "Ethnic Wear" to listOf(StyleType.CLASSIC, StyleType.LUXURY),
        "Traditional Wear" to listOf(StyleType.CLASSIC, StyleType.LUXURY),

        "Sneakers" to listOf(StyleType.STREETWEAR, StyleType.CASUAL_CHIC, StyleType.ATHLETIC),
        "Sports Shoes" to listOf(StyleType.ATHLETIC),
        "Formal Shoes" to listOf(StyleType.CLASSIC, StyleType.LUXURY),
        "Boots" to listOf(StyleType.EDGY, StyleType.CLASSIC, StyleType.STREETWEAR),
        "Heels" to listOf(StyleType.LUXURY, StyleType.CLASSIC, StyleType.EDGY),
        "Sandals" to listOf(StyleType.BOHEMIAN, StyleType.CASUAL_CHIC, StyleType.MINIMAL)
    )
}
