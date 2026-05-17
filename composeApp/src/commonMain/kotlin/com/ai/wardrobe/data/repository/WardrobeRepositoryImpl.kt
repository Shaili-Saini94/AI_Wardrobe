package com.ai.wardrobe.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ai.wardrobe.data.mapper.toClothingItem
import com.ai.wardrobe.data.mapper.toOutfit
import com.ai.wardrobe.database.WardrobeDatabase
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
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

    private val queries = db.wardrobeDatabaseQueries

    override fun getAllClothingItems(): Flow<List<ClothingItem>> {
        return queries.selectAllClothingItems()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toClothingItem() } }
    }

    override suspend fun getClothingItemById(id: Long): ClothingItem? {
        return withContext(dispatcher) {
            queries.selectClothingItemById(id).executeAsOneOrNull()?.toClothingItem()
        }
    }

    override suspend fun insertClothingItem(item: ClothingItem) {
        withContext(dispatcher) {
            queries.insertClothingItem(
                imageUri = item.imageUri,
                category = item.category,
                tags = item.tags.joinToString(","),
                dateAdded = item.dateAdded
            )
        }
    }

    override suspend fun deleteClothingItem(id: Long) {
        withContext(dispatcher) {
            queries.deleteClothingItem(id)
        }
    }

    override suspend fun updateClothingItem(item: ClothingItem) {
        withContext(dispatcher) {
            queries.updateClothingItem(
                category = item.category,
                tags = item.tags.joinToString(","),
                id = item.id ?: 0L
            )
        }
    }

    override fun getAllOutfits(): Flow<List<Outfit>> {
        return queries.selectAllOutfits()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities ->
                entities.map { entity ->
                    val items = queries.selectItemsForOutfit(entity.id)
                        .executeAsList()
                        .map { it.toClothingItem() }
                    entity.toOutfit(items)
                }
            }
    }

    override suspend fun insertOutfit(outfit: Outfit) {
        withContext(dispatcher) {
            db.transaction {
                queries.insertOutfit(outfit.name, outfit.dateCreated)
                val outfitId = queries.lastInsertedOutfitId().executeAsOne()
                outfit.items.forEach { item ->
                    item.id?.let { itemId ->
                        queries.insertOutfitItem(outfitId, itemId)
                    }
                }
            }
        }
    }

    override suspend fun deleteOutfit(id: Long) {
        withContext(dispatcher) {
            queries.deleteOutfit(id)
        }
    }
}
