package com.ai.wardrobe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val itemCount: Int = 0,
    val newestItem: ClothingItem? = null,
    val mostWornItem: ClothingItem? = null,
    val greeting: String = "Good morning, Alex.",
    val weather: String = "72°F Sunny"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: WardrobeRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.getAllClothingItems()
        .map { items ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good morning, Alex."
                hour < 17 -> "Good afternoon, Alex."
                else -> "Good evening, Alex."
            }
            
            DashboardUiState(
                itemCount = items.size,
                newestItem = items.maxByOrNull { it.dateAdded },
                mostWornItem = items.randomOrNull(), // Simplified for demo
                greeting = greeting
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
