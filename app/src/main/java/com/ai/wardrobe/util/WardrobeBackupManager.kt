package com.ai.wardrobe.util

import android.content.Context
import android.net.Uri
import com.ai.wardrobe.domain.model.ClothingItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Simple serialisable wrapper so Moshi can handle all fields easily
@JsonClass(generateAdapter = true)
data class ClothingItemBackup(
    val id: Long?,
    val imageUri: String,
    val thumbnailUri: String?,
    val thumbnailBackUri: String?,
    val category: String,
    val tags: List<String>,
    val occasions: List<String>,
    val seasons: List<String>,
    val styleTypes: List<String>,
    val mood: String?,
    val weather: String?,
    val dominantColor: String?,
    val dateAdded: Long
)

sealed class BackupResult {
    data class Success(val path: String, val count: Int) : BackupResult()
    data class Failure(val error: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val count: Int) : RestoreResult()
    data class Failure(val error: String) : RestoreResult()
}

@Singleton
class WardrobeBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, ClothingItemBackup::class.java)
    private val adapter = moshi.adapter<List<ClothingItemBackup>>(listType)

    suspend fun exportToJson(items: List<ClothingItem>): BackupResult = withContext(Dispatchers.IO) {
        try {
            val backupList = items.map { it.toBackup() }
            val json = adapter.toJson(backupList)

            val dir = File(context.filesDir, "backups").also { it.mkdirs() }
            val file = File(dir, "wardrobe_backup_${System.currentTimeMillis()}.json")
            file.writeText(json)

            BackupResult.Success(path = file.absolutePath, count = items.size)
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun importFromUri(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText()
                ?: return@withContext RestoreResult.Failure("Could not open file")

            val backupList = adapter.fromJson(json)
                ?: return@withContext RestoreResult.Failure("Invalid backup format")

            RestoreResult.Success(count = backupList.size)
        } catch (e: Exception) {
            RestoreResult.Failure(e.message ?: "Unknown error")
        }
    }

    /** Parses items from a URI for bulk insertion by caller */
    suspend fun parseItemsFromUri(uri: Uri): List<ClothingItem> = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText() ?: return@withContext emptyList()
        adapter.fromJson(json)?.map { it.toClothingItem() } ?: emptyList()
    }

    private fun ClothingItem.toBackup() = ClothingItemBackup(
        id = id, imageUri = imageUri, thumbnailUri = thumbnailUri,
        thumbnailBackUri = thumbnailBackUri, category = category, tags = tags,
        occasions = occasions, seasons = seasons, styleTypes = styleTypes,
        mood = mood, weather = weather, dominantColor = dominantColor, dateAdded = dateAdded
    )

    private fun ClothingItemBackup.toClothingItem() = ClothingItem(
        id = null, // new IDs on restore
        imageUri = imageUri, thumbnailUri = thumbnailUri,
        thumbnailBackUri = thumbnailBackUri, category = category, tags = tags,
        occasions = occasions, seasons = seasons, styleTypes = styleTypes,
        mood = mood, weather = weather, dominantColor = dominantColor,
        dateAdded = System.currentTimeMillis()
    )
}
