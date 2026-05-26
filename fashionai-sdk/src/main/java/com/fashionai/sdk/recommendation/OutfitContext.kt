package com.fashionai.sdk.recommendation

import com.fashionai.sdk.model.*
import com.fashionai.sdk.weather.WeatherContext

/**
 * Input context for outfit generation.
 * All fields except [occasion] are optional — the engine degrades gracefully.
 */
data class OutfitContext(
    val occasion: Occasion,
    val weather: WeatherContext? = null,
    val mood: Mood? = null,
    val timeOfDay: TimeOfDay? = null,
    val preferredStyle: StyleType? = null,
    val excludeItemIds: Set<String> = emptySet()
)

enum class Mood(val displayName: String) {
    MINIMAL("Minimal"),
    BOLD("Bold"),
    COMFY("Comfy"),
    SHARP("Sharp"),
    PLAYFUL("Playful"),
    ROMANTIC("Romantic"),
    POWER("Power")
}

enum class TimeOfDay(val displayName: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}
