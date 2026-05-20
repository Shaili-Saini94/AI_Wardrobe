package com.ai.wardrobe.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val profileDataStore: StyleProfileDataStore
) : ViewModel() {

    val styleProfile: StateFlow<StyleProfile> = profileDataStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StyleProfile())

    val clothingItems: StateFlow<List<ClothingItem>> = repository.getAllClothingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedOutfits: StateFlow<List<Outfit>> = repository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleStyle(style: String) {
        viewModelScope.launch {
            val current = styleProfile.value.preferredStyles.toMutableList()
            if (style in current) current.remove(style) else current.add(style)
            profileDataStore.updateStyles(current)
        }
    }

    fun toggleOccasion(occasion: String) {
        viewModelScope.launch {
            val current = styleProfile.value.favoriteOccasions.toMutableList()
            if (occasion in current) current.remove(occasion) else current.add(occasion)
            profileDataStore.updateOccasions(current)
        }
    }

    fun setColorPalette(palette: String) {
        viewModelScope.launch { profileDataStore.updateColorPalette(palette) }
    }

    fun setGender(gender: String) {
        viewModelScope.launch { profileDataStore.updateGender(gender) }
    }
}
