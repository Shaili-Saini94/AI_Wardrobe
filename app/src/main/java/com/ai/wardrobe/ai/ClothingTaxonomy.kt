package com.ai.wardrobe.ai

data class ClothingClassification(
    val group: String,
    val subcategory: String,
    val occasions: List<String>,
    val tags: List<String>,
)

object ClothingTaxonomy {

    fun classify(
        rawLabel: String,
        aiTags: List<String> = emptyList()
    ): ClothingClassification? {
        val text = buildString {
            append(rawLabel)
            append(" ")
            append(aiTags.joinToString(" "))
        }.lowercase()

        return when {
            // Prioritize Footwear to avoid misclassification as Bottom wear (e.g. slippers as trousers)
            text.hasAny("sneaker", "sneakers", "sports shoe", "sports shoes", "running shoe", "running shoes") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Sneakers",
                    occasions = listOf("Sports / Gym", "Casual outing", "Mountain / Travel trip", "Beach trip"),
                    tags = listOf("footwear", "sneakers", "sports")
                )

            text.hasAny("heel", "heels", "stiletto", "pumps") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Heels",
                    occasions = listOf("Party", "Clubbing", "Date night", "Office / Formal"),
                    tags = listOf("footwear", "heels", "formal", "party")
                )

            text.hasAny("sandal", "sandals", "slipper", "slippers", "flip flop", "flip-flop") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Sandals",
                    occasions = listOf("Beach trip", "Casual outing"),
                    tags = listOf("footwear", "sandals", "summer")
                )

            text.hasAny("boot", "boots") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Boots",
                    occasions = listOf("Mountain / Travel trip", "Date night", "Clubbing", "Party"),
                    tags = listOf("footwear", "boots", "winter")
                )

            text.hasAny("shoe", "shoes", "footwear") ->
                ClothingClassification(
                    group = "Footwear",
                    subcategory = "Shoes",
                    occasions = listOf("Casual outing", "Party", "Office / Formal"),
                    tags = listOf("footwear", "shoes")
                )

            // Tops
            text.hasAny("t-shirt", "tshirt", "tee") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "T-Shirt",
                    occasions = listOf("Casual outing", "Sports / Gym", "Beach trip"),
                    tags = listOf("top", "t-shirt", "casual", "comfortable")
                )

            text.hasAny("crop top", "croptop", "crop") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Crop Top",
                    occasions = listOf("Party", "Clubbing", "Date night", "Casual outing"),
                    tags = listOf("top", "crop-top", "party", "stylish")
                )

            text.hasAny("sleeveless", "tank top", "vest top", "cut sleeves", "cut sleeve") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Sleeveless Top",
                    occasions = listOf("Casual outing", "Sports / Gym", "Beach trip", "Clubbing"),
                    tags = listOf("top", "sleeveless", "summer")
                )

            text.hasAny("full sleeve", "full sleeves", "long sleeve", "long sleeves") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Full Sleeves Top",
                    occasions = listOf("Casual outing", "Office / Formal", "Mountain / Travel trip"),
                    tags = listOf("top", "full-sleeves")
                )

            text.hasAny("hoodie", "sweatshirt") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Hoodie",
                    occasions = listOf("Casual outing", "Mountain / Travel trip", "Sports / Gym"),
                    tags = listOf("top", "hoodie", "winter", "casual")
                )

            text.hasAny("blazer") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Blazer",
                    occasions = listOf("Office / Formal", "Date night", "Party"),
                    tags = listOf("top", "formal", "layering")
                )

            text.hasAny("jacket", "coat") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Jacket",
                    occasions = listOf("Casual outing", "Mountain / Travel trip", "Date night"),
                    tags = listOf("top", "jacket", "layering")
                )

            text.hasAny("shirt", "formal shirt", "button shirt", "button-down", "button down", "blouse") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Shirt",
                    occasions = listOf("Office / Formal", "Casual outing", "Date night", "Party"),
                    tags = listOf("top", "shirt", "formal", "smart")
                )

            text.hasAny("top", "upper wear", "upperwear") ->
                ClothingClassification(
                    group = "Top",
                    subcategory = "Top",
                    occasions = listOf("Casual outing", "Party", "Date night"),
                    tags = listOf("top")
                )

            // Bottoms
            text.hasAny("high waist", "high-waist", "high waisted", "high-waisted") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "High-Waist Jeans",
                    occasions = listOf("Party", "Clubbing", "Date night", "Casual outing"),
                    tags = listOf("bottom", "jeans", "high-waist", "stylish")
                )

            text.hasAny("jogger", "joggers", "track pant", "track pants", "sweatpants") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "Joggers",
                    occasions = listOf("Sports / Gym", "Casual outing", "Mountain / Travel trip"),
                    tags = listOf("bottom", "joggers", "sports", "comfortable")
                )

            text.hasAny("jeans", "denim") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "Jeans",
                    occasions = listOf("Casual outing", "Party", "Date night", "Mountain / Travel trip"),
                    tags = listOf("bottom", "jeans", "denim")
                )

            text.hasAny("trouser", "trousers", "pants", "pant") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "Pants",
                    occasions = listOf("Office / Formal", "Casual outing", "Date night"),
                    tags = listOf("bottom", "pants", "formal")
                )

            text.hasAny("shorts", "short") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "Shorts",
                    occasions = listOf("Beach trip", "Sports / Gym", "Casual outing"),
                    tags = listOf("bottom", "shorts", "summer")
                )

            text.hasAny("skirt") ->
                ClothingClassification(
                    group = "Bottom",
                    subcategory = "Skirt",
                    occasions = listOf("Party", "Clubbing", "Date night", "Office / Formal"),
                    tags = listOf("bottom", "skirt", "stylish")
                )

            // One Piece
            text.hasAny("jumpsuit", "jump suit") ->
                ClothingClassification(
                    group = "One Piece",
                    subcategory = "Jumpsuit",
                    occasions = listOf("Party", "Date night", "Casual outing"),
                    tags = listOf("one-piece", "jumpsuit", "complete-outfit")
                )

            text.hasAny("dress", "one piece", "one-piece", "gown") ->
                ClothingClassification(
                    group = "One Piece",
                    subcategory = "Dress",
                    occasions = listOf("Party", "Clubbing", "Date night", "Office / Formal", "Casual outing"),
                    tags = listOf("one-piece", "dress", "complete-outfit")
                )

            else -> null
        }
    }

    private fun String.hasAny(vararg keywords: String): Boolean {
        return keywords.any { keyword -> contains(keyword.lowercase()) }
    }
}
