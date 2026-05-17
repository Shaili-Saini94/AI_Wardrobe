package com.fashionai.sdk

/**
 * # FashionAI SDK — Integration Samples
 *
 * This file shows complete real-world usage patterns.
 * Copy the pattern that fits your use case into your app.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SETUP (do this once in your Application class)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * class MyApp : Application() {
 *
 *     lateinit var fashionAI: FashionAI
 *         private set
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         fashionAI = FashionAI.Builder(this)
 *             .setMode(FashionAIMode.ON_DEVICE)   // No internet required
 *             .setModelFileName("fashion_classifier.tflite")  // Put in assets/
 *             .setLogLevel(FashionAILogLevel.DEBUG)           // Change to WARN for release
 *             .build()
 *
 *         // Load model in background (does not block UI)
 *         fashionAI.prepare()
 *     }
 *
 *     override fun onTerminate() {
 *         fashionAI.release()
 *         super.onTerminate()
 *     }
 * }
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────
 * USE CASE 1: Add a clothing item from camera/gallery
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * class AddClothingViewModel(
 *     private val app: Application,
 *     private val fashionAI: FashionAI
 * ) : ViewModel() {
 *
 *     private val _result = MutableStateFlow<AddItemState>(AddItemState.Idle)
 *     val result: StateFlow<AddItemState> = _result.asStateFlow()
 *
 *     fun processImage(uri: Uri) {
 *         viewModelScope.launch {
 *             _result.value = AddItemState.Loading
 *             try {
 *                 // Step 1: Load bitmap
 *                 val bitmap = withContext(Dispatchers.IO) {
 *                     app.contentResolver.openInputStream(uri)
 *                         ?.use { BitmapFactory.decodeStream(it) }
 *                 } ?: return@launch
 *
 *                 // Step 2: Detect clothing type
 *                 val detection = fashionAI.detectClothing(bitmap)
 *                 // detection.clothingType  → ClothingType.TOP
 *                 // detection.subTypeDisplay → "Polo"
 *                 // detection.confidence    → 0.87f
 *
 *                 // Step 3: Extract colors
 *                 val colors = fashionAI.extractColors(bitmap)
 *                 // colors[0] → DetectedColor(hex="#FFFFFF", name="White", percentage=0.72)
 *
 *                 // Step 4: Full categorization (pattern, occasions, seasons, tags)
 *                 val category = fashionAI.categorize(detection, bitmap, colors)
 *                 // category.occasions  → [CASUAL, WORK, TRAVEL]
 *                 // category.seasons    → [ALL_SEASON]
 *                 // category.tags       → ["smart casual", "versatile", "easy to pair", ...]
 *                 // category.pattern    → Pattern.SOLID
 *
 *                 // Step 5: Build your ClothingItem to save to DB
 *                 val item = ClothingItem(
 *                     id = UUID.randomUUID().toString(),
 *                     imageUri = uri.toString(),
 *                     type = category.type,
 *                     subType = category.subType,
 *                     colors = colors,
 *                     tags = category.tags,
 *                     occasions = category.occasions,
 *                     seasons = category.seasons,
 *                     pattern = category.pattern,
 *                     styleTypes = category.styleTypes,
 *                     genderRelevance = category.genderRelevance
 *                 )
 *
 *                 _result.value = AddItemState.Success(item)
 *             } catch (e: Exception) {
 *                 _result.value = AddItemState.Error(e.message ?: "Detection failed")
 *             }
 *         }
 *     }
 * }
 *
 * sealed class AddItemState {
 *     object Idle : AddItemState()
 *     object Loading : AddItemState()
 *     data class Success(val item: ClothingItem) : AddItemState()
 *     data class Error(val message: String) : AddItemState()
 * }
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────
 * USE CASE 2: Generate outfit recommendations
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * viewModelScope.launch {
 *     val outfits = fashionAI.generateOutfit(
 *         wardrobe = userWardrobe,         // List<ClothingItem> from your DB
 *         context = OutfitContext(
 *             occasion = Occasion.PARTY,
 *             weather = WeatherContext(
 *                 temperatureCelsius = 18f,
 *                 condition = WeatherCondition.CLOUDY
 *             ),
 *             mood = Mood.BOLD,
 *             preferredStyle = StyleType.MINIMAL
 *         ),
 *         count = 3
 *     )
 *
 *     // outfits[0].items            → [BlackJeans, WhiteShirt, LeatherBoots]
 *     // outfits[0].compatibilityScore → 0.88f
 *     // outfits[0].styleNote        → "Sharp harmonious outfit — great for party"
 *     // outfits[0].weatherNote      → null (outfit is appropriate for cloudy 18°C)
 *     // outfits[0].colorHarmonyScore → 0.91f
 * }
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────
 * USE CASE 3: Color compatibility check
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * val colors = listOf(
 *     DetectedColor("#000000", "Black", 0.6f),
 *     DetectedColor("#FFFFFF", "White", 0.4f)
 * )
 *
 * val result = fashionAI.getColorCompatibility(colors)
 * // result.score       → 0.92f
 * // result.harmonyType → ColorHarmony.NEUTRAL
 * // result.notes       → ["Timeless neutral palette — always stylish"]
 * // result.summaryText → "Excellent — Neutral harmony"
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────
 * USE CASE 4: "What goes with this?" — style suggestions
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * val selectedItem = userWardrobe.first { it.subType == "Blazer" }
 *
 * val suggestions = fashionAI.getStyleSuggestions(
 *     item = selectedItem,
 *     wardrobe = userWardrobe,
 *     limit = 5
 * )
 * // Returns top 5 wardrobe items that best pair with the blazer
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────
 * TF LITE MODEL: How to get a working model
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Option A — Use a pre-trained general-purpose model (quick start):
 *   1. Download EfficientNet-Lite2 for image classification from TFHub:
 *      https://www.kaggle.com/models/google/efficientnet/tfLite/lite2-classification
 *   2. Rename to "fashion_classifier.tflite"
 *   3. Place in: yourapp/src/main/assets/fashion_classifier.tflite
 *   Note: General model accuracy on clothing will be ~60-70%.
 *         Labels won't map perfectly — subTypeDisplay may show raw ImageNet labels.
 *
 * Option B — Fine-tuned fashion model (recommended for production):
 *   Train or fine-tune EfficientNet-Lite2 on DeepFashion2 or iMaterialist dataset.
 *   See PHASE3_AI_DETECTION.md for the full training pipeline.
 *   Target accuracy: 85%+ on clothing sub-types.
 *
 * Option C — No model (SDK still works for color + outfit generation):
 *   FashionAI.prepare() returns false. detectClothing() returns UNKNOWN.
 *   All other SDK features (color analysis, outfit generation, compatibility)
 *   work fully with your manually entered ClothingItem data.
 */
@Suppress("unused")
private object FashionAISampleDoc
