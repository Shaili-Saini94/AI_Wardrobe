package com.ai.wardrobe.domain.model

data class ClothingItem(
    val id: Long? = null,
    val imageUri: String,
    val thumbnailUri: String? = null,
    val thumbnailBackUri: String? = null,
    val category: String,
    val tags: List<String>,
    val occasions: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val styleTypes: List<String> = emptyList(),
    val mood: String? = null,
    val weather: String? = null,
    val dominantColor: String? = null,
    val dateAdded: Long
)
