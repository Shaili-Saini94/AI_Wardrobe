package com.ai.wardrobe.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ai.wardrobe.data.mapper.toCalendarEntry
import com.ai.wardrobe.data.mapper.toClothingItem
import com.ai.wardrobe.data.mapper.toOutfit
import com.ai.wardrobe.database.WardrobeDatabase
import com.ai.wardrobe.domain.model.CalendarEntry
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.WornRecord
import com.ai.wardrobe.domain.repository.WardrobeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WardrobeRepositoryImpl(
    private val db: WardrobeDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WardrobeRepository {

    private val q = db.wardrobeDatabaseQueries

    // ── Clothing items ────────────────────────────────────────────────────────

    override fun getAllClothingItems(): Flow<List<ClothingItem>> =
        q.selectAllClothingItems().asFlow().mapToList(dispatcher)
            .map { it.map { e -> e.toClothingItem() } }

    override suspend fun getClothingItemById(id: Long): ClothingItem? =
        withContext(dispatcher) { q.selectClothingItemById(id).executeAsOneOrNull()?.toClothingItem() }

    override suspend fun insertClothingItem(item: ClothingItem): Long =
        withContext(dispatcher) {
            db.transactionWithResult {
                q.insertClothingItem(
                    imageUri         = item.imageUri,
                    thumbnailUri     = item.thumbnailUri,
                    thumbnailBackUri = item.thumbnailBackUri,
                    category         = item.category,
                    tags             = item.tags.joinToString(","),
                    occasions        = item.occasions.joinToString(","),
                    seasons          = item.seasons.joinToString(","),
                    styleTypes       = item.styleTypes.joinToString(","),
                    mood             = item.mood,
                    weather          = item.weather,
                    dominantColor    = item.dominantColor,
                    dateAdded        = item.dateAdded
                )
                q.lastInsertedItemId().executeAsOne()
            }
        }

    override suspend fun deleteClothingItem(id: Long): Unit =
        withContext(dispatcher) { q.deleteClothingItem(id) }

    override suspend fun updateClothingItem(item: ClothingItem): Unit =
        withContext(dispatcher) {
            q.updateClothingItem(
                category     = item.category,
                tags         = item.tags.joinToString(","),
                occasions    = item.occasions.joinToString(","),
                seasons      = item.seasons.joinToString(","),
                styleTypes   = item.styleTypes.joinToString(","),
                dominantColor = item.dominantColor,
                id           = item.id ?: 0L
            )
        }

    override suspend fun updateThumbnailUri(id: Long, thumbnailUri: String): Unit =
        withContext(dispatcher) { q.updateThumbnailUri(thumbnailUri = thumbnailUri, id = id) }

    override suspend fun updateThumbnailBackUri(id: Long, thumbnailBackUri: String): Unit =
        withContext(dispatcher) { q.updateThumbnailBackUri(thumbnailBackUri = thumbnailBackUri, id = id) }

    // ── Outfits ───────────────────────────────────────────────────────────────

    override fun getAllOutfits(): Flow<List<Outfit>> =
        q.selectAllOutfits().asFlow().mapToList(dispatcher).map { entities ->
            entities.map { entity ->
                val items = q.selectItemsForOutfit(entity.id).executeAsList().map { it.toClothingItem() }
                entity.toOutfit(items)
            }
        }

    override suspend fun insertOutfit(outfit: Outfit) =
        withContext(dispatcher) {
            db.transaction {
                q.insertOutfit(
                    name         = outfit.name,
                    styleType    = outfit.styleType.ifBlank { null },
                    occasionType = outfit.occasionType.ifBlank { null },
                    styleNote    = outfit.styleNote.ifBlank { null },
                    weatherNote  = outfit.weatherNote.ifBlank { null },
                    colorStory   = outfit.colorStory.ifBlank { null },
                    whyItWorks   = outfit.whyItWorks.ifBlank { null },
                    dateCreated  = outfit.dateCreated
                )
                val outfitId = q.lastInsertedOutfitId().executeAsOne()
                outfit.items.forEach { item -> item.id?.let { q.insertOutfitItem(outfitId, it) } }
            }
        }

    override suspend fun deleteOutfit(id: Long): Unit =
        withContext(dispatcher) { q.deleteOutfit(id) }

    // ── Wear history ──────────────────────────────────────────────────────────

    override suspend fun logWorn(clothingItemId: Long): Unit =
        withContext(dispatcher) { q.insertWornRecord(clothingItemId, System.currentTimeMillis()) }

    override suspend fun getLastWornDate(clothingItemId: Long): Long? =
        withContext(dispatcher) { q.selectLastWornForItem(clothingItemId).executeAsOneOrNull() }

    override fun getItemsNotWornSince(cutoffMs: Long): Flow<List<ClothingItem>> =
        q.selectItemsNotWornSince(cutoffMs).asFlow().mapToList(dispatcher)
            .map { it.map { e -> e.toClothingItem() } }

    // ── Calendar ──────────────────────────────────────────────────────────────

    override fun getAllCalendarEntries(): Flow<List<CalendarEntry>> =
        q.selectAllCalendarEntries().asFlow().mapToList(dispatcher).map { entries ->
            entries.map { entry ->
                val outfit = entry.outfitId?.let { oid ->
                    val items = q.selectItemsForOutfit(oid).executeAsList().map { it.toClothingItem() }
                    q.selectAllOutfits().executeAsList().find { it.id == oid }?.toOutfit(items)
                }
                entry.toCalendarEntry(outfit)
            }
        }

    override suspend fun getCalendarEntriesForDate(date: String): List<CalendarEntry> =
        withContext(dispatcher) {
            q.selectCalendarEntriesForDate(date).executeAsList().map { entry ->
                val outfit = entry.outfitId?.let { oid ->
                    val items = q.selectItemsForOutfit(oid).executeAsList().map { it.toClothingItem() }
                    q.selectAllOutfits().executeAsList().find { it.id == oid }?.toOutfit(items)
                }
                entry.toCalendarEntry(outfit)
            }
        }

    override suspend fun insertCalendarEntry(entry: CalendarEntry): Unit =
        withContext(dispatcher) {
            q.insertCalendarEntry(
                outfitId    = entry.outfitId,
                plannedDate = entry.plannedDate,
                note        = entry.note
            )
        }

    override suspend fun deleteCalendarEntry(id: Long): Unit =
        withContext(dispatcher) { q.deleteCalendarEntry(id) }
}
