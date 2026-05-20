package com.ai.wardrobe.ui.styling

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.ai.StylingInference
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import com.ai.wardrobe.data.weather.WeatherInfo
import com.ai.wardrobe.data.weather.WeatherService
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StylingViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val stylingInference: StylingInference,
    private val profileDataStore: StyleProfileDataStore,
    private val weatherService: WeatherService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val styleProfile: StateFlow<StyleProfile> = profileDataStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StyleProfile())

    private val _selectedOccasion = MutableStateFlow("Any")
    val selectedOccasion: StateFlow<String> = _selectedOccasion.asStateFlow()

    private val _suggestedOutfits = MutableStateFlow<List<Outfit>>(emptyList())
    val suggestedOutfits: StateFlow<List<Outfit>> = _suggestedOutfits.asStateFlow()

    val filteredOutfits: StateFlow<List<Outfit>> = combine(
        _suggestedOutfits, _selectedOccasion
    ) { outfits, occasion ->
        if (occasion == "Any") outfits
        else outfits.filter { it.occasionType.equals(occasion, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val favoriteOutfits: StateFlow<List<Outfit>> = repository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allClothingItems = repository.getAllClothingItems()

    // ── Weather ───────────────────────────────────────────────────────────────
    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()

    // ── "What goes with this?" ────────────────────────────────────────────────
    private val _pairingItem = MutableStateFlow<ClothingItem?>(null)
    val pairingItem: StateFlow<ClothingItem?> = _pairingItem.asStateFlow()

    private val _pairingResults = MutableStateFlow<List<Outfit>>(emptyList())
    val pairingResults: StateFlow<List<Outfit>> = _pairingResults.asStateFlow()

    init {
        // Auto-generate outfits when wardrobe items first become available
        viewModelScope.launch {
            allClothingItems
                .filter { it.isNotEmpty() }
                .take(1)
                .collect { generateSuggestions() }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun setOccasionFilter(occasion: String) { _selectedOccasion.value = occasion }

    fun generateSuggestions(weatherOverride: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items    = allClothingItems.first()
                val profile  = styleProfile.value
                val occasion = _selectedOccasion.value
                val weatherStr = weatherOverride
                    ?: _weather.value?.let { "${it.description}, ${it.tempCelsius.toInt()}°C" }
                    ?: "mild weather, around 22°C"
                _suggestedOutfits.value = stylingInference.generateOutfits(
                    items        = items,
                    occasion     = occasion,
                    weather      = weatherStr,
                    styleProfile = profile,
                    count        = 5
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchWeatherAndGenerate() {
        viewModelScope.launch {
            _weatherLoading.value = true
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = lm.getProviders(true)
                var lat = 28.6139; var lon = 77.2090  // fallback: New Delhi
                for (provider in providers) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) { lat = loc.latitude; lon = loc.longitude; break }
                }
                val info = weatherService.getWeather(lat, lon)
                _weather.value = info
                generateSuggestions(info?.let { "${it.description}, ${it.tempCelsius.toInt()}°C" })
            } catch (e: Exception) {
                generateSuggestions()
            } finally {
                _weatherLoading.value = false
            }
        }
    }

    fun findPairingsFor(item: ClothingItem) {
        _pairingItem.value = item
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allItems = allClothingItems.first()
                val profile  = styleProfile.value
                val outfits  = stylingInference.generateOutfits(
                    items        = allItems,
                    occasion     = _selectedOccasion.value,
                    styleProfile = profile,
                    count        = 10
                )
                // Keep only outfits that contain the selected item
                _pairingResults.value = outfits.filter { o -> o.items.any { it.id == item.id } }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPairing() { _pairingItem.value = null; _pairingResults.value = emptyList() }

    fun saveOutfit(outfit: Outfit) {
        viewModelScope.launch { repository.insertOutfit(outfit) }
    }

    fun deleteFavoriteOutfit(id: Long) {
        viewModelScope.launch { repository.deleteOutfit(id) }
    }
}
