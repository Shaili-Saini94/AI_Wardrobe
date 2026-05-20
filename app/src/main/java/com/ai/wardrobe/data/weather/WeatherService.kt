package com.ai.wardrobe.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherInfo(
    val tempCelsius: Float,
    val description: String,   // "Sunny", "Rainy", "Cloudy", "Snowy"
    val season: String,        // "Summer", "Winter", "Spring", "Autumn"
    val summary: String        // Human-readable for outfit engine
)

@Singleton
class WeatherService @Inject constructor() {

    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weathercode" +
                    "&timezone=auto"
                val json = JSONObject(URL(url).readText())
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toFloat()
                val code = current.getInt("weathercode")

                val description = weatherCodeToDescription(code)
                val season = tempToSeason(temp)
                val summary = "$description, ${temp.toInt()}°C"

                WeatherInfo(temp, description, season, summary)
            } catch (e: Exception) { null }
        }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0             -> "Sunny"
        1, 2, 3       -> "Partly Cloudy"
        45, 48        -> "Foggy"
        51, 53, 55    -> "Drizzly"
        61, 63, 65    -> "Rainy"
        71, 73, 75    -> "Snowy"
        80, 81, 82    -> "Showery"
        95, 96, 99    -> "Stormy"
        else          -> "Cloudy"
    }

    private fun tempToSeason(temp: Float): String = when {
        temp >= 28f  -> "Summer"
        temp >= 15f  -> "Spring"
        temp >= 5f   -> "Autumn"
        else         -> "Winter"
    }
}
