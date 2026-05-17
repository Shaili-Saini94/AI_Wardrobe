package com.ai.wardrobe.ui.wardrobe

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.ImageLabeler
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val imageLabeler: ImageLabeler
) : ViewModel() {

    val clothingItems: StateFlow<List<ClothingItem>> = repository.getAllClothingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun addClothingItem(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            
            try {
                val result = imageLabeler.analyzeImage(uri)
                
                if (result.isClothing) {
                    val file = FileUtil.createImageFile(context)
                    FileUtil.copyUriToFile(context, uri, file)
                    val savedUri = Uri.fromFile(file).toString()

                    val newItem = ClothingItem(
                        imageUri = savedUri,
                        category = result.category,
                        tags = result.tags,
                        dateAdded = System.currentTimeMillis()
                    )
                    repository.insertClothingItem(newItem)
                } else {
                    _errorMessage.value = "This item doesn't look like clothing. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to analyze image: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteClothingItem(id: Long) {
        viewModelScope.launch {
            repository.deleteClothingItem(id)
        }
    }
}
