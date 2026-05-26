package com.ai.wardrobe.ui.main

import androidx.lifecycle.ViewModel
import com.ai.wardrobe.data.datastore.StyleProfileDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    dataStore: StyleProfileDataStore
) : ViewModel() {
    val onboardingComplete: Flow<Boolean> = dataStore.onboardingComplete
}
