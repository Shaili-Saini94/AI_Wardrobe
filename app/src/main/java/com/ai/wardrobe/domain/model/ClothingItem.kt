package com.ai.wardrobe.domain.model

data class ClothingItem(
    val id: Long? = null,
    val imageUri: String,
    val category: String,
    val tags: List<String>,
    val dateAdded: Long
)
