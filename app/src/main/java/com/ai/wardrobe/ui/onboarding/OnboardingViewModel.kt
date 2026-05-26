package com.ai.wardrobe.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,                          // 0..4
    val gender: String = "",
    val selectedStyles: Set<String> = emptySet(),
    val selectedOccasions: Set<String> = emptySet(),
    val selectedPalette: String = "",
    val saving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: StyleProfileDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setGender(gender: String) = _state.update { it.copy(gender = gender) }

    fun toggleStyle(style: String) = _state.update {
        val updated = if (style in it.selectedStyles) it.selectedStyles - style
                      else it.selectedStyles + style
        it.copy(selectedStyles = updated)
    }

    fun toggleOccasion(occ: String) = _state.update {
        val updated = if (occ in it.selectedOccasions) it.selectedOccasions - occ
                      else it.selectedOccasions + occ
        it.copy(selectedOccasions = updated)
    }

    fun setPalette(palette: String) = _state.update { it.copy(selectedPalette = palette) }

    fun nextStep() = _state.update { it.copy(step = it.step + 1) }
    fun prevStep() = _state.update { it.copy(step = maxOf(0, it.step - 1)) }

    fun finishOnboarding(onDone: () -> Unit) {
        val s = _state.value
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (s.gender.isNotBlank())          dataStore.updateGender(s.gender)
            if (s.selectedStyles.isNotEmpty())  dataStore.updateStyles(s.selectedStyles.toList())
            if (s.selectedOccasions.isNotEmpty()) dataStore.updateOccasions(s.selectedOccasions.toList())
            if (s.selectedPalette.isNotBlank()) dataStore.updateColorPalette(s.selectedPalette)
            dataStore.setOnboardingComplete()
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
