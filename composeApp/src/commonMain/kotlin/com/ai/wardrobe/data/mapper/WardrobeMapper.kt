package com.ai.wardrobe.data.mapper

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.database.ClothingItem as ClothingItemEntity
import com.ai.wardrobe.database.Outfit as OutfitEntity

fun ClothingItemEntity.toClothingItem(): ClothingItem {
    return ClothingItem(
        id = id,
        imageUri = imageUri,
        category = category,
        tags = tags?.split(",") ?: emptyList(),
        dateAdded = dateAdded
    )
}

fun OutfitEntity.toOutfit(items: List<ClothingItem>): Outfit {
    return Outfit(
        id = id,
        name = name,
        items = items,
        dateCreated = dateCreated
    )
}
