package com.ai.wardrobe.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.util.BackupResult
import com.ai.wardrobe.util.WardrobeBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val profileDataStore: StyleProfileDataStore,
    private val backupManager: WardrobeBackupManager
) : ViewModel() {

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun exportWardrobe() {
        viewModelScope.launch {
            val items = clothingItems.value
            when (val result = backupManager.exportToJson(items)) {
                is BackupResult.Success ->
                    _backupMessage.value = "✓ Backup saved (${result.count} items)\n${result.path}"
                is BackupResult.Failure ->
                    _backupMessage.value = "✗ Backup failed: ${result.error}"
            }
        }
    }

    fun importWardrobe(uri: Uri) {
        viewModelScope.launch {
            val parsed = backupManager.parseItemsFromUri(uri)
            if (parsed.isEmpty()) {
                _backupMessage.value = "✗ No items found in backup file"
                return@launch
            }
            parsed.forEach { repository.insertClothingItem(it) }
            _backupMessage.value = "✓ Restored ${parsed.size} items to wardrobe"
        }
    }

    fun clearBackupMessage() { _backupMessage.value = null }

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
