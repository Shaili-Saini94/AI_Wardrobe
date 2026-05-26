package com.fashionai.sdk.weather

import com.fashionai.sdk.model.ClothingItem
import com.fashionai.sdk.model.WeatherCondition

/**
 * Determines whether a clothing item is appropriate for given weather conditions.
 */
internal object WeatherCompatibilityMatrix {

    private data class WeatherRule(
        val preferred: Set<String>,
        val avoid: Set<String>
    )

    private val rules: Map<WeatherCondition, WeatherRule> = mapOf(

        WeatherCondition.HOT to WeatherRule(
            preferred = setOf("T-Shirt", "Shorts", "Sleeveless Top", "Crop Top",
                "Sandals", "Slippers", "One-piece", "Dress"),
            avoid = setOf("Hoodie", "Jacket", "Boots", "Joggers", "Sweatshirt",
                "Full Sleeve Top", "Trousers")
        ),

        WeatherCondition.SUNNY to WeatherRule(
            preferred = setOf("T-Shirt", "Polo", "Shorts", "Dress", "Sneakers",
                "Sandals", "Crop Top", "Sleeveless Top"),
            avoid = setOf("Hoodie", "Boots", "Sweatshirt")
        ),

        WeatherCondition.MILD to WeatherRule(
            preferred = setOf("Shirt", "Polo", "Jeans", "Trousers", "Sneakers",
                "Boots", "Full Sleeve Top", "Blazer"),
            avoid = emptySet()
        ),

        WeatherCondition.CLOUDY to WeatherRule(
            preferred = setOf("Shirt", "Polo", "Jeans", "Jacket", "Full Sleeve Top",
                "Sneakers", "Boots"),
            avoid = setOf("Sleeveless Top", "Shorts", "Crop Top", "Sandals")
        ),

        WeatherCondition.COLD to WeatherRule(
            preferred = setOf("Hoodie", "Jacket", "Sweatshirt", "Boots", "Joggers",
                "Trousers", "Full Sleeve Top"),
            avoid = setOf("Shorts", "Sleeveless Top", "Sandals", "Slippers", "Crop Top")
        ),

        WeatherCondition.RAINY to WeatherRule(
            preferred = setOf("Jacket", "Boots", "Trousers", "Jeans", "Full Sleeve Top"),
            avoid = setOf("Sandals", "Slippers", "Sports Shoes", "Sneakers")
        ),

        WeatherCondition.SNOWY to WeatherRule(
            preferred = setOf("Jacket", "Boots", "Hoodie", "Sweatshirt", "Trousers",
                "Full Sleeve Top"),
            avoid = setOf("Sandals", "Sneakers", "Shorts", "Crop Top", "Sleeveless Top",
                "Slippers", "Sports Shoes")
        ),

        WeatherCondition.HUMID to WeatherRule(
            preferred = setOf("T-Shirt", "Shorts", "Sleeveless Top", "Sandals"),
            avoid = setOf("Sweatshirt", "Hoodie", "Joggers", "Jacket", "Boots")
        ),

        WeatherCondition.WINDY to WeatherRule(
            preferred = setOf("Jacket", "Full Sleeve Top", "Jeans", "Trousers",
                "Boots", "Sneakers"),
            avoid = setOf("Skirt", "Dress", "Sandals", "Slippers")
        )
    )

    fun isCompatible(item: ClothingItem, weather: WeatherContext): Boolean {
        val rule = rules[weather.condition] ?: return true
        return item.subType !in rule.avoid
    }

    fun getScore(item: ClothingItem, weather: WeatherContext): Float {
        val rule = rules[weather.condition] ?: return 1.0f
        return when (item.subType) {
            in rule.preferred -> 1.0f
            in rule.avoid -> 0.0f
            else -> 0.70f
        }
    }

    fun getWeatherNote(items: List<ClothingItem>, weather: WeatherContext): String? {
        return when (weather.condition) {
            WeatherCondition.RAINY -> {
                val hasSuede = items.any { "suede" in it.tags.map(String::lowercase) }
                val hasSneakers = items.any { it.subType == "Sneakers" || it.subType == "Sports Shoes" }
                when {
                    hasSuede -> "Avoid suede in rain — swap for leather or rubber-soled footwear"
                    hasSneakers -> "Rainy day — consider waterproof footwear"
                    else -> null
                }
            }
            WeatherCondition.HOT -> {
                val hasHeavyFabric = items.any { item ->
                    item.tags.any { it in listOf("wool", "polyester", "thick") }
                }
                if (hasHeavyFabric) "Hot weather — prefer breathable linen or cotton" else null
            }
            WeatherCondition.COLD -> {
                val hasOuter = items.any {
                    it.subType in setOf("Jacket", "Hoodie", "Sweatshirt", "Blazer")
                }
                if (!hasOuter) "Cold weather — add an outer layer" else null
            }
            WeatherCondition.SNOWY -> "Layer up — keep extremities warm"
            WeatherCondition.WINDY -> {
                val hasSkirt = items.any { it.subType == "Skirt" }
                if (hasSkirt) "Windy day — consider trousers or a midi/maxi skirt" else null
            }
            else -> null
        }
    }
}
