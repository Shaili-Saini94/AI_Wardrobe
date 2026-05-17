package com.ai.wardrobe.ui.styling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.StylingInference
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StylingViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val stylingInference: StylingInference
) : ViewModel() {

    private val _suggestedOutfits = MutableStateFlow<List<Outfit>>(emptyList())
    val suggestedOutfits: StateFlow<List<Outfit>> = _suggestedOutfits.asStateFlow()

    val favoriteOutfits: StateFlow<List<Outfit>> = repository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allClothingItems = repository.getAllClothingItems()

    fun generateSuggestions() {
        viewModelScope.launch {
            val items = allClothingItems.first()
            _suggestedOutfits.value = stylingInference.generateOutfits(items)
        }
    }

    fun saveOutfit(outfit: Outfit) {
        viewModelScope.launch {
            repository.insertOutfit(outfit)
        }
    }

    fun deleteFavoriteOutfit(id: Long) {
        viewModelScope.launch {
            repository.deleteOutfit(id)
        }
    }
}
