package com.ai.wardrobe.domain.model

data class StyleProfile(
    val preferredStyles: List<String> = emptyList(),   // Minimalist, Funky, Classic …
    val favoriteOccasions: List<String> = emptyList(), // Work, Date Night, Weekend …
    val colorPalette: String = "All Colors",           // Neutrals, Bold & Bright, Pastels …
    val gender: String = "Unisex"
) {
    companion object {
        val ALL_STYLES = listOf(
            "Minimalist", "Classic", "Smart Casual",
            "Streetwear", "Funky", "Bohemian",
            "Athleisure", "Formal"
        )
        val ALL_OCCASIONS = listOf(
            "Work", "Date Night", "Weekend", "Casual",
            "Party", "Gym", "Beach", "Travel", "Formal Event"
        )
        val ALL_PALETTES = listOf(
            "All Colors", "Neutrals Only", "Bold & Bright",
            "Pastels", "Earth Tones", "Monochrome"
        )
        val ALL_GENDERS = listOf("Men", "Women", "Unisex")

        // Maps style → badge background color (hex string)
        val STYLE_COLORS = mapOf(
            "Minimalist"   to 0xFFE8E6E3,
            "Classic"      to 0xFF775A19,
            "Smart Casual" to 0xFF2C4A7C,
            "Streetwear"   to 0xFF1B1C1C,
            "Funky"        to 0xFFB5179E,
            "Bohemian"     to 0xFF8B6914,
            "Athleisure"   to 0xFF2D6A4F,
            "Formal"       to 0xFF1A2744
        )

        // Text color on badge
        val STYLE_TEXT_ON_DARK = setOf("Classic", "Smart Casual", "Streetwear", "Funky", "Bohemian", "Athleisure", "Formal")
    }
}
