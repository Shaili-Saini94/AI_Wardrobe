package com.ai.wardrobe.domain.model

data class Outfit(
    val id: Long? = null,
    val name: String,
    val items: List<ClothingItem>,
    val dateCreated: Long
)
