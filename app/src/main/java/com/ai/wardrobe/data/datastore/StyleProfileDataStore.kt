package com.ai.wardrobe.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.wardrobe.domain.model.StyleProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.styleDataStore: DataStore<Preferences> by preferencesDataStore("style_profile")

@Singleton
class StyleProfileDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_STYLES         = stringSetPreferencesKey("preferred_styles")
        val KEY_OCCASIONS      = stringSetPreferencesKey("favorite_occasions")
        val KEY_PALETTE        = stringPreferencesKey("color_palette")
        val KEY_GENDER         = stringPreferencesKey("gender")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
    }

    val onboardingComplete: Flow<Boolean> = context.styleDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.styleDataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    val profile: Flow<StyleProfile> = context.styleDataStore.data.map { prefs ->
        StyleProfile(
            preferredStyles   = prefs[KEY_STYLES]?.toList()    ?: emptyList(),
            favoriteOccasions = prefs[KEY_OCCASIONS]?.toList() ?: emptyList(),
            colorPalette      = prefs[KEY_PALETTE]             ?: "All Colors",
            gender            = prefs[KEY_GENDER]              ?: "Unisex"
        )
    }

    suspend fun updateStyles(styles: List<String>) {
        context.styleDataStore.edit { it[KEY_STYLES] = styles.toSet() }
    }

    suspend fun updateOccasions(occasions: List<String>) {
        context.styleDataStore.edit { it[KEY_OCCASIONS] = occasions.toSet() }
    }

    suspend fun updateColorPalette(palette: String) {
        context.styleDataStore.edit { it[KEY_PALETTE] = palette }
    }

    suspend fun updateGender(gender: String) {
        context.styleDataStore.edit { it[KEY_GENDER] = gender }
    }
}
