package com.ai.wardrobe.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.CapsuleItem
import com.ai.wardrobe.ai.ColorPalette
import com.ai.wardrobe.ai.NaturalLanguageSearch
import com.ai.wardrobe.ai.SimilarPair
import com.ai.wardrobe.ai.StyleDna
import com.ai.wardrobe.ai.WardrobeGap
import com.ai.wardrobe.ai.WardrobeIntelligence
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val capsule: List<CapsuleItem> = emptyList(),
    val gaps: List<WardrobeGap> = emptyList(),
    val palette: ColorPalette? = null,
    val styleDna: StyleDna? = null,
    val similars: List<SimilarPair> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val intelligence: WardrobeIntelligence,
    private val nlSearch: NaturalLanguageSearch,
    private val profileDataStore: StyleProfileDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsUiState(isLoading = true))
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NaturalLanguageSearch.SearchResult>>(emptyList())
    val searchResults: StateFlow<List<NaturalLanguageSearch.SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllClothingItems(),
                profileDataStore.profile
            ) { items, profile -> Pair(items, profile) }
                .collect { (items, profile) ->
                    _state.value = InsightsUiState(
                        capsule   = intelligence.analyzeCapsule(items, profile).take(10),
                        gaps      = intelligence.detectGaps(items),
                        palette   = intelligence.buildColorPalette(items),
                        styleDna  = intelligence.buildStyleDna(items),
                        similars  = intelligence.detectSimilars(items),
                        isLoading = false
                    )
                }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            val items = repository.getAllClothingItems().first()
            _searchResults.value = nlSearch.search(query, items)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
