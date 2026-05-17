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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun addClothingItem(context: Context, uri: Uri, category: String = "Uncategorized") {
        viewModelScope.launch {
            val file = FileUtil.createImageFile(context)
            FileUtil.copyUriToFile(context, uri, file)
            val savedUri = Uri.fromFile(file).toString()
            
            val tags = imageLabeler.labelImage(context, uri)
            
            val newItem = ClothingItem(
                imageUri = savedUri,
                category = category,
                tags = tags,
                dateAdded = System.currentTimeMillis()
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
