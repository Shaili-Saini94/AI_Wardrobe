package com.fashionai.sdk.model

enum class Occasion(val displayName: String) {
    CASUAL("Casual"),
    WORK("Work"),
    FORMAL("Formal"),
    PARTY("Party"),
    SPORTS("Sports"),
    TRAVEL("Travel"),
    BEACH("Beach"),
    DATE("Date Night"),
    OUTDOOR("Outdoor"),
    HOME("Home")
}

enum class Season(val displayName: String) {
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter"),
    ALL_SEASON("All Season")
}

enum class Pattern(val displayName: String) {
    SOLID("Solid"),
    STRIPED("Striped"),
    CHECKED("Checked / Plaid"),
    FLORAL("Floral"),
    GEOMETRIC("Geometric"),
    ABSTRACT("Abstract"),
    ANIMAL_PRINT("Animal Print"),
    GRAPHIC("Graphic"),
    UNKNOWN("Unknown")
}

enum class StyleType(val displayName: String) {
    MINIMAL("Minimal"),
    STREETWEAR("Streetwear"),
    CLASSIC("Classic"),
    LUXURY("Luxury"),
    BOHEMIAN("Bohemian"),
    ATHLETIC("Athletic"),
    PREPPY("Preppy"),
    EDGY("Edgy"),
    CASUAL_CHIC("Casual Chic")
}

enum class GenderRelevance(val displayName: String) {
    MASCULINE("Masculine"),
    FEMININE("Feminine"),
    UNISEX("Unisex")
}

enum class FormalityLevel {
    FORMAL,
    SEMI_FORMAL,
    SMART_CASUAL,
    CASUAL,
    ATHLETIC
}

enum class ColorHarmony(val displayName: String) {
    COMPLEMENTARY("Complementary"),
    ANALOGOUS("Analogous"),
    TRIADIC("Triadic"),
    MONOCHROMATIC("Monochromatic"),
    NEUTRAL("Neutral"),
    SPLIT_COMPLEMENTARY("Split Complementary"),
    CLASH("Color Clash")
}

enum class WeatherCondition(val displayName: String) {
    SUNNY("Sunny"),
    CLOUDY("Cloudy"),
    RAINY("Rainy"),
    SNOWY("Snowy"),
    WINDY("Windy"),
    HUMID("Humid"),
    COLD("Cold"),
    HOT("Hot"),
    MILD("Mild")
}

enum class FashionAIMode {
    /** All processing on-device. No internet required. */
    ON_DEVICE,

    /** On-device detection + optional cloud LLM for outfit reasoning. */
    HYBRID,

    /** Cloud-first. Requires API key and internet. */
    CLOUD
}

enum class FashionAILogLevel {
    NONE, ERROR, WARN, INFO, DEBUG, VERBOSE
}
