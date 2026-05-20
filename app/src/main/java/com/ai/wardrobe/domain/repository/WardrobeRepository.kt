package com.ai.wardrobe.domain.repository

import com.ai.wardrobe.domain.model.CalendarEntry
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.WornRecord
import kotlinx.coroutines.flow.Flow

interface WardrobeRepository {
    // Clothing items
    fun getAllClothingItems(): Flow<List<ClothingItem>>
    suspend fun getClothingItemById(id: Long): ClothingItem?
    suspend fun insertClothingItem(item: ClothingItem): Long
    suspend fun deleteClothingItem(id: Long)
    suspend fun updateClothingItem(item: ClothingItem)
    suspend fun updateThumbnailUri(id: Long, thumbnailUri: String)
    suspend fun updateThumbnailBackUri(id: Long, thumbnailBackUri: String)

    // Outfits
    fun getAllOutfits(): Flow<List<Outfit>>
    suspend fun insertOutfit(outfit: Outfit)
    suspend fun deleteOutfit(id: Long)

    // Wear history
    suspend fun logWorn(clothingItemId: Long)
    suspend fun getLastWornDate(clothingItemId: Long): Long?
    fun getItemsNotWornSince(cutoffMs: Long): Flow<List<ClothingItem>>

    // Calendar
    fun getAllCalendarEntries(): Flow<List<CalendarEntry>>
    suspend fun getCalendarEntriesForDate(date: String): List<CalendarEntry>
    suspend fun insertCalendarEntry(entry: CalendarEntry)
    suspend fun deleteCalendarEntry(id: Long)
}
