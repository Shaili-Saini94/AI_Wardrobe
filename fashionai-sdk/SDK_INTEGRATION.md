# FashionAI SDK — Android Integration Guide

## Step 1: Add the SDK module to your project

### Option A — Local module (same project)

In your root `settings.gradle.kts`:
```kotlin
include(":fashionai-sdk")
project(":fashionai-sdk").projectDir = File("path/to/fashionai-sdk")
```

In your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":fashionai-sdk"))
}
```

### Option B — Published AAR (Maven Local)

From the SDK root, run:
```bash
./gradlew :fashionai-sdk:publishToMavenLocal
```

Then in your app:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("io.closetiq:fashionai-sdk:1.0.0")
}
```

---

## Step 2: Add the TFLite model (for clothing detection)

Place your `.tflite` model file in:
```
yourapp/src/main/assets/fashion_classifier.tflite
```

**Quick-start model (general purpose):**
1. Go to: https://www.kaggle.com/models/google/efficientnet/tfLite/lite2-classification
2. Download `efficientnet_lite2_classification_2.tflite`
3. Rename to `fashion_classifier.tflite`
4. Drop it into `src/main/assets/`

**Note:** The general model will detect broad clothing types but won't know
sub-types like "Cargo Pants" vs "Trousers". For full sub-type detection,
train a custom model on DeepFashion2 (see PHASE3_AI_DETECTION.md).

---

## Step 3: Initialize the SDK

```kotlin
// MyApplication.kt
class MyApplication : Application() {

    lateinit var fashionAI: FashionAI
        private set

    override fun onCreate() {
        super.onCreate()

        fashionAI = FashionAI.Builder(this)
            .setMode(FashionAIMode.ON_DEVICE)
            .setModelFileName("fashion_classifier.tflite")
            .setLogLevel(
                if (BuildConfig.DEBUG) FashionAILogLevel.DEBUG
                else FashionAILogLevel.WARN
            )
            .build()

        // Loads the TFLite model into memory — non-blocking
        fashionAI.prepare()
    }

    override fun onTerminate() {
        fashionAI.release()
        super.onTerminate()
    }
}
```

Register in `AndroidManifest.xml`:
```xml
<application
    android:name=".MyApplication"
    ...>
```

---

## Step 4: Detect a clothing item

```kotlin
// In a ViewModel or Repository:
viewModelScope.launch {
    val bitmap = /* from camera or gallery */

    // 1. Detect type
    val detection = fashionAI.detectClothing(bitmap)
    println(detection.clothingType)    // TOP
    println(detection.subTypeDisplay)  // "Polo"
    println(detection.confidence)      // 0.87

    // 2. Extract colors
    val colors = fashionAI.extractColors(bitmap)
    println(colors[0].name)  // "Navy Blue"
    println(colors[0].hex)   // "#001F5B"

    // 3. Full categorization
    val category = fashionAI.categorize(detection, bitmap, colors)
    println(category.occasions)   // [CASUAL, WORK, TRAVEL]
    println(category.tags)        // ["smart casual", "versatile", ...]
    println(category.pattern)     // SOLID

    // 4. Build and save ClothingItem
    val item = ClothingItem(
        id = UUID.randomUUID().toString(),
        imageUri = imageUri.toString(),
        type = category.type,
        subType = category.subType,
        colors = colors,
        tags = category.tags,
        occasions = category.occasions,
        seasons = category.seasons,
        pattern = category.pattern,
        styleTypes = category.styleTypes
    )
    // Save item to your Room DB or wherever you store wardrobe data
}
```

---

## Step 5: Generate outfit recommendations

```kotlin
viewModelScope.launch {
    val outfits = fashionAI.generateOutfit(
        wardrobe = myWardrobeItems,  // List<ClothingItem>
        context = OutfitContext(
            occasion = Occasion.WORK,
            weather = WeatherContext(
                temperatureCelsius = 22f,
                condition = WeatherCondition.SUNNY
            ),
            mood = Mood.SHARP
        ),
        count = 3
    )

    outfits.forEach { outfit ->
        println("Score: ${outfit.scorePercent}")
        println("Style: ${outfit.styleNote}")
        println("Items: ${outfit.items.map { it.subType }}")
        outfit.weatherNote?.let { println("Weather: $it") }
    }
}
```

---

## Step 6: Check color compatibility

```kotlin
val colors = listOf(
    DetectedColor("#000080", "Navy Blue", 0.7f),
    DetectedColor("#FFFFFF", "White", 0.3f)
)

val result = fashionAI.getColorCompatibility(colors)
println(result.summaryText)   // "Excellent — Complementary harmony"
println(result.score)         // 0.88
println(result.notes)         // ["High contrast — confident statement"]
```

---

## Permissions

The SDK requires these permissions (declared in its own manifest — you don't need to add them):

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

For Android 12 and below, also add:
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

Request these at runtime using `ActivityResultContracts.RequestPermission` before calling `detectClothing(uri)`.

---

## Minimum Requirements

| Requirement | Value |
|---|---|
| Min Android SDK | 24 (Android 7.0) |
| Target SDK | 35 |
| Kotlin | 1.9+ |
| Coroutines | 1.7+ |
| Memory (model loaded) | ~50–80 MB heap |
| Model file size | 4–15 MB (depending on variant) |

---

## Proguard / R8

The SDK ships a `consumer-rules.pro` file that is automatically applied to your app when using R8/ProGuard. No manual rules needed.

---

## SDK File Layout

```
fashionai-sdk/
├── build.gradle.kts
├── consumer-rules.pro
├── proguard-rules.pro
├── SDK_INTEGRATION.md                   ← This file
└── src/main/
    ├── AndroidManifest.xml
    └── java/com/fashionai/sdk/
        ├── FashionAI.kt                 ← Public entry point (start here)
        ├── FashionAIConfig.kt
        ├── FashionAISample.kt           ← Full usage examples
        ├── internal/
        │   └── FashionAILogger.kt
        ├── model/
        │   ├── ClothingItem.kt
        │   ├── ClothingType.kt
        │   └── Enums.kt                 ← Occasion, Season, Pattern, etc.
        ├── detection/
        │   ├── TFLiteClothingDetector.kt
        │   ├── DetectionResult.kt
        │   └── FashionLabelDictionary.kt
        ├── categorization/
        │   ├── SmartCategorizer.kt
        │   ├── CategoryResult.kt
        │   ├── TagEngine.kt
        │   └── PatternDetector.kt
        ├── color/
        │   ├── ColorAnalyzer.kt
        │   ├── ColorCompatibilityEngine.kt
        │   ├── CompatibilityResult.kt
        │   ├── DetectedColor.kt
        │   └── FashionColorDictionary.kt
        ├── recommendation/
        │   ├── OutfitContext.kt
        │   ├── OutfitSuggestion.kt
        │   └── RuleEngine.kt
        └── weather/
            ├── WeatherContext.kt
            └── WeatherCompatibilityMatrix.kt
```
