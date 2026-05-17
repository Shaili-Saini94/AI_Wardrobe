package com.ai.wardrobe.ui.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.ImageLabeler
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WardrobeViewModel(
    private val repository: WardrobeRepository,
    private val imageLabeler: ImageLabeler
) : ViewModel() {

    val clothingItems: StateFlow<List<ClothingItem>> = repository.getAllClothingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addClothingItem(uri: String, category: String = "Uncategorized") {
        viewModelScope.launch {
            val tags = imageLabeler.labelImage(uri)
            
            val newItem = ClothingItem(
                imageUri = uri,
                category = category,
                tags = tags,
                dateAdded = 0L // In real app, get current time multiplatform way
            )
            repository.insertClothingItem(newItem)
        }
    }

    fun deleteClothingItem(id: Long) {
        viewModelScope.launch {
            repository.deleteClothingItem(id)
        }
    }
}
