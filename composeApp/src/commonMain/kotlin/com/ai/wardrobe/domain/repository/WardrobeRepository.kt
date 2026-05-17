package com.ai.wardrobe.domain.repository

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import kotlinx.coroutines.flow.Flow

interface WardrobeRepository {
    fun getAllClothingItems(): Flow<List<ClothingItem>>
    suspend fun getClothingItemById(id: Long): ClothingItem?
    suspend fun insertClothingItem(item: ClothingItem)
    suspend fun deleteClothingItem(id: Long)
    suspend fun updateClothingItem(item: ClothingItem)

    fun getAllOutfits(): Flow<List<Outfit>>
    suspend fun insertOutfit(outfit: Outfit)
    suspend fun deleteOutfit(id: Long)
}
