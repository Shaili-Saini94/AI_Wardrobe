package com.ai.wardrobe.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.wardrobe.domain.model.CalendarEntry
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: WardrobeRepository
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val allEntries: StateFlow<List<CalendarEntry>> = repository.getAllCalendarEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedOutfits: StateFlow<List<Outfit>> = repository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(fmt))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _entriesForDate = MutableStateFlow<List<CalendarEntry>>(emptyList())
    val entriesForDate: StateFlow<List<CalendarEntry>> = _entriesForDate.asStateFlow()

    init { loadEntriesForDate(LocalDate.now().format(fmt)) }

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadEntriesForDate(date)
    }

    private fun loadEntriesForDate(date: String) {
        viewModelScope.launch {
            _entriesForDate.value = repository.getCalendarEntriesForDate(date)
        }
    }

    fun planOutfit(outfitId: Long?, note: String? = null) {
        viewModelScope.launch {
            repository.insertCalendarEntry(
                CalendarEntry(
                    outfitId    = outfitId,
                    plannedDate = _selectedDate.value,
                    note        = note
                )
            )
            loadEntriesForDate(_selectedDate.value)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEntry(id)
            loadEntriesForDate(_selectedDate.value)
        }
    }
}
