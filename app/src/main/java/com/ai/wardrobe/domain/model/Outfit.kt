package com.ai.wardrobe.domain.model

data class Outfit(
    val id: Long? = null,
    val name: String,
    val styleType: String = "",        // Minimalist | Streetwear | Classic | Bohemian | Athleisure | Smart Casual | Formal | Funky
    val occasionType: String = "",     // Work | Date Night | Weekend | Party | Gym | Beach | Travel | Casual | Formal Event
    val items: List<ClothingItem>,
    val styleNote: String = "",
    val weatherNote: String = "",
    val colorStory: String = "",       // e.g. "Ivory and camel — a clean neutral palette"
    val whyItWorks: String = "",       // Fashion rationale
    val dateCreated: Long
)
