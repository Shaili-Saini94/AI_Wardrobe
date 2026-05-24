package com.ai.wardrobe.ui.styling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.StylingInference
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StylingViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val stylingInference: StylingInference
) : ViewModel() {

    private val _suggestedOutfits = MutableStateFlow<List<Outfit>>(emptyList())
    val suggestedOutfits: StateFlow<List<Outfit>> = _suggestedOutfits.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val favoriteOutfits: StateFlow<List<Outfit>> = repository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allClothingItems = repository.getAllClothingItems()

    fun generateSuggestions(occasion: String) {
        viewModelScope.launch {
            val items = allClothingItems.first()

            if (items.isEmpty()) {
                _suggestedOutfits.value = emptyList()
                _message.value = "Please add clothing items to your wardrobe first."
                return@launch
            }

            val outfits = stylingInference.generateOutfits(
                items = items,
                occasion = occasion,
                count = 8
            )

            _suggestedOutfits.value = outfits

            _message.value = if (outfits.isEmpty()) {
                "No smart outfit found for $occasion. Add matching tops, bottoms, one-piece outfits, or footwear."
            } else {
                null
            }
        }
    }

    fun saveOutfit(outfit: Outfit) {
        viewModelScope.launch {
            repository.insertOutfit(outfit)
            _message.value = "Outfit saved to favorites."
        }
    }

    fun deleteFavoriteOutfit(id: Long) {
        viewModelScope.launch {
            repository.deleteOutfit(id)
            _message.value = "Outfit removed from favorites."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}