package com.fashionai.sdk.weather

import com.fashionai.sdk.model.WeatherCondition

/**
 * Weather conditions used to adapt outfit recommendations.
 */
data class WeatherContext(
    /** Temperature in Celsius */
    val temperatureCelsius: Float,

    /** Current weather condition */
    val condition: WeatherCondition,

    /** Humidity percentage (0–100) */
    val humidity: Float = 50f,

    /** Optional location name */
    val locationName: String? = null
) {
    /** Derived temperature category for easier rule matching */
    val temperatureCategory: TemperatureCategory get() = when {
        temperatureCelsius >= 30f -> TemperatureCategory.HOT
        temperatureCelsius >= 20f -> TemperatureCategory.WARM
        temperatureCelsius >= 10f -> TemperatureCategory.MILD
        temperatureCelsius >= 0f -> TemperatureCategory.COLD
        else -> TemperatureCategory.FREEZING
    }
}

enum class TemperatureCategory { FREEZING, COLD, MILD, WARM, HOT }
