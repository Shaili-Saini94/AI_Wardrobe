package com.ai.wardrobe.ui.wardrobe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.DominantColorExtractor
import com.ai.wardrobe.ai.GeminiClothingAnalyzer
import com.ai.wardrobe.ai.ImageAnalysisResult
import com.ai.wardrobe.ai.ImageLabeler
import com.ai.wardrobe.ai.NaturalLanguageSearch
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class PendingAnalysis(
    val sourceUri: Uri,
    val items: List<ImageAnalysisResult>
)

data class DuplicateWarning(
    val newCategory: String,
    val existingItem: ClothingItem
)

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val imageLabeler: ImageLabeler,
    private val gemini: GeminiClothingAnalyzer,
    private val nlSearch: NaturalLanguageSearch
) : ViewModel() {

    val clothingItems: StateFlow<List<ClothingItem>> = repository.getAllClothingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Items not worn in 30 days (for nudges)
    val neglectedItems: StateFlow<List<ClothingItem>> = repository
        .getItemsNotWornSince(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _pendingAnalysis = MutableStateFlow<PendingAnalysis?>(null)
    val pendingAnalysis: StateFlow<PendingAnalysis?> = _pendingAnalysis.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savingStatus = MutableStateFlow("")
    val savingStatus: StateFlow<String> = _savingStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _duplicateWarnings = MutableStateFlow<List<DuplicateWarning>>(emptyList())
    val duplicateWarnings: StateFlow<List<DuplicateWarning>> = _duplicateWarnings.asStateFlow()

    private val _itemToEdit = MutableStateFlow<ClothingItem?>(null)
    val itemToEdit: StateFlow<ClothingItem?> = _itemToEdit.asStateFlow()

    // ── Search & vibe filter ──────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVibe = MutableStateFlow<String?>(null)
    val selectedVibe: StateFlow<String?> = _selectedVibe.asStateFlow()

    val filteredItems: StateFlow<List<ClothingItem>> = combine(
        clothingItems, _searchQuery, _selectedVibe
    ) { items, query, vibe ->
        var result = items
        if (query.isNotBlank()) {
            result = nlSearch.search(query, result).map { it.item }
        }
        if (vibe != null) {
            result = result.filter { item ->
                vibeMatches(item, vibe)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setVibe(vibe: String?) { _selectedVibe.value = vibe }

    private fun vibeMatches(item: ClothingItem, vibe: String): Boolean = when (vibe) {
        "Office"        -> item.occasions.any { it.equals("Work", true) } || item.styleTypes.any { it in listOf("Classic", "Minimalist", "Formal", "Smart Casual") }
        "Weekend Chill" -> item.occasions.any { it.equals("Casual", true) } || item.styleTypes.any { it in listOf("Casual", "Athleisure", "Streetwear") }
        "Date Night"    -> item.occasions.any { it.equals("Date Night", true) } || item.styleTypes.any { it in listOf("Classic", "Formal", "Minimalist") }
        "Festival"      -> item.occasions.any { it.equals("Festival", true) } || item.styleTypes.any { it in listOf("Ethnic", "Bohemian", "Funky") }
        "Gym"           -> item.occasions.any { it.equals("Gym", true) } || item.styleTypes.any { it in listOf("Athleisure") }
        "Beach"         -> item.occasions.any { it.equals("Beach", true) } || item.seasons.any { it.equals("Summer", true) }
        "Ethnic"        -> item.styleTypes.any { it in listOf("Ethnic", "Bohemian") } || item.category in listOf("Kurta", "Saree", "Lehenga")
        else            -> true
    }

    // ── Analyze ───────────────────────────────────────────────────────────────

    fun analyzePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            try {
                val results = imageLabeler.analyzeAllItems(uri)
                val clothing = results.filter { it.isClothing }
                if (clothing.isEmpty()) {
                    _errorMessage.value = results.firstOrNull()?.debugReason
                        ?: "No clothing detected. Please try a clearer photo."
                } else {
                    // Duplicate detection
                    val existing = clothingItems.value
                    val warnings = clothing.mapNotNull { result ->
                        existing.firstOrNull { it.category.equals(result.category, ignoreCase = true) }
                            ?.let { DuplicateWarning(result.category, it) }
                    }
                    _duplicateWarnings.value = warnings
                    _pendingAnalysis.value = PendingAnalysis(sourceUri = uri, items = clothing)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Analysis failed: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun dismissPending() {
        _pendingAnalysis.value = null
        _duplicateWarnings.value = emptyList()
    }

    fun dismissDuplicateWarnings() { _duplicateWarnings.value = emptyList() }

    // ── Confirm add ───────────────────────────────────────────────────────────

    fun confirmAddItems(context: Context, selectedIndices: Set<Int>) {
        val pending = _pendingAnalysis.value ?: return
        val toAdd = pending.items.filterIndexed { i, _ -> i in selectedIndices }
        if (toAdd.isEmpty()) { _pendingAnalysis.value = null; return }

        _pendingAnalysis.value = null
        _duplicateWarnings.value = emptyList()

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val savedUri = withContext(Dispatchers.IO) {
                    val file = FileUtil.createImageFile(context)
                    FileUtil.copyUriToFile(context, pending.sourceUri, file)
                    android.net.Uri.fromFile(file).toString()
                }

                val base64 = withContext(Dispatchers.IO) { uriToBase64(context, pending.sourceUri) }

                // Extract dominant color once from the full image
                val dominantColor = withContext(Dispatchers.IO) {
                    DominantColorExtractor.extract(context, pending.sourceUri)
                }

                toAdd.forEachIndexed { idx, result ->
                    // Use refined sub-category (e.g. "Anarkali Suit") when available, else broad label
                    val displayCategory = result.subCategory ?: result.category
                    _savingStatus.value = "Saving $displayCategory (${idx + 1}/${toAdd.size})..."

                    val newItem = ClothingItem(
                        imageUri      = savedUri,
                        category      = displayCategory,
                        tags          = result.tags,
                        occasions     = result.occasions,
                        seasons       = result.seasons,
                        styleTypes    = result.styleTypes,
                        mood          = result.mood,
                        weather       = result.weather,
                        dominantColor = dominantColor,
                        dateAdded     = System.currentTimeMillis()
                    )
                    val insertedId = repository.insertClothingItem(newItem)

                    _savingStatus.value = "Saving photo for $displayCategory..."
                    val frontPath = if (base64 != null) {
                        gemini.generateFrontThumbnail(base64Image = base64, category = displayCategory, tags = result.tags)
                    } else null
                    if (frontPath != null) repository.updateThumbnailUri(insertedId, frontPath)

                    _savingStatus.value = "Saving back view for $displayCategory..."
                    val backPath = if (base64 != null) {
                        gemini.generateBackThumbnail(base64Image = base64, category = displayCategory, tags = result.tags)
                    } else null
                    if (backPath != null) repository.updateThumbnailBackUri(insertedId, backPath)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save items: ${e.message}"
            } finally {
                _isSaving.value = false
                _savingStatus.value = ""
            }
        }
    }

    // ── Edit item ─────────────────────────────────────────────────────────────

    fun openEditItem(item: ClothingItem) { _itemToEdit.value = item }
    fun dismissEditItem() { _itemToEdit.value = null }

    fun saveEditedItem(item: ClothingItem) {
        viewModelScope.launch {
            repository.updateClothingItem(item)
            _itemToEdit.value = null
        }
    }

    // ── Wear history ──────────────────────────────────────────────────────────

    fun logItemWorn(itemId: Long) {
        viewModelScope.launch { repository.logWorn(itemId) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteClothingItem(id: Long) {
        viewModelScope.launch { repository.deleteClothingItem(id) }
    }

    fun clearError() { _errorMessage.value = null }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null
            val maxDim = 1024
            val bitmap = if (raw.width > maxDim || raw.height > maxDim) {
                val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
                Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
            } else raw
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }
}
