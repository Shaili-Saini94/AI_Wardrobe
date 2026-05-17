package com.fashionai.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.fashionai.sdk.categorization.CategoryResult
import com.fashionai.sdk.categorization.SmartCategorizer
import com.fashionai.sdk.color.ColorAnalyzer
import com.fashionai.sdk.color.CompatibilityResult
import com.fashionai.sdk.color.ColorCompatibilityEngine
import com.fashionai.sdk.color.DetectedColor
import com.fashionai.sdk.detection.DetectionResult
import com.fashionai.sdk.detection.TFLiteClothingDetector
import com.fashionai.sdk.internal.FashionAILogger
import com.fashionai.sdk.model.ClothingItem
import com.fashionai.sdk.model.FashionAILogLevel
import com.fashionai.sdk.model.FashionAIMode
import com.fashionai.sdk.recommendation.OutfitContext
import com.fashionai.sdk.recommendation.OutfitSuggestion
import com.fashionai.sdk.recommendation.RuleEngine
import com.fashionai.sdk.recommendation.RankedOutfit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * # FashionAI SDK
 *
 * The single entry point for all AI-powered fashion intelligence features.
 *
 * ## Quick start
 *
 * ```kotlin
 * // In Application.onCreate():
 * val fashionAI = FashionAI.Builder(context)
 *     .setMode(FashionAIMode.ON_DEVICE)
 *     .build()
 *
 * // Detect clothing:
 * val result = fashionAI.detectClothing(bitmap)
 *
 * // Categorize:
 * val category = fashionAI.categorize(result, bitmap)
 *
 * // Generate outfits:
 * val outfits = fashionAI.generateOutfit(
 *     wardrobe = myItems,
 *     context = OutfitContext(occasion = Occasion.PARTY)
 * )
 * ```
 *
 * See [Builder] for all configuration options.
 */
class FashionAI private constructor(
    private val context: Context,
    private val config: FashionAIConfig
) {

    private val detector = TFLiteClothingDetector(
        context = context,
        modelFileName = config.modelFileName,
        scoreThreshold = config.detectionConfidenceThreshold,
        useGpu = config.useGpuAcceleration
    )

    private val categorizer = SmartCategorizer()
    private val colorAnalyzer = ColorAnalyzer()
    private val colorEngine = ColorCompatibilityEngine()
    private val ruleEngine = RuleEngine()

    private var isReady = false

    init {
        FashionAILogger.level = config.logLevel
        FashionAILogger.i("FashionAI SDK initializing — mode=${config.mode}")
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Loads the TFLite model into memory. Call this once before using detection.
     * Safe to call multiple times — subsequent calls are no-ops.
     *
     * @return true if the model loaded successfully, false if the model file
     *         is missing. All other SDK functions still work without a model
     *         (detection returns low-confidence UNKNOWN results).
     */
    fun prepare(): Boolean {
        if (isReady) return true
        isReady = detector.initialize()
        if (isReady) {
            FashionAILogger.i("FashionAI ready — model loaded: ${config.modelFileName}")
        } else {
            FashionAILogger.w(
                "TFLite model not found at assets/${config.modelFileName}. " +
                "Detection will return fallback results. " +
                "Download a model from the FashionAI SDK docs and place it in your assets/ folder."
            )
        }
        return isReady
    }

    /** Releases TFLite resources. Call in Activity.onDestroy() or when SDK is no longer needed. */
    fun release() {
        detector.close()
        isReady = false
        FashionAILogger.i("FashionAI released")
    }

    // ─── Detection ─────────────────────────────────────────────────────────

    /**
     * Detects the clothing type in the given [bitmap].
     *
     * Runs on [Dispatchers.Default] — safe to call from any coroutine.
     *
     * @return [DetectionResult] with clothing type, sub-type, and confidence.
     *         If no model is loaded, returns a low-confidence UNKNOWN result.
     */
    suspend fun detectClothing(bitmap: Bitmap): DetectionResult {
        FashionAILogger.d("detectClothing called — bitmap ${bitmap.width}×${bitmap.height}")
        return detector.detect(bitmap)
    }

    /**
     * Detects clothing from a content URI (e.g. from a gallery picker or camera capture).
     */
    suspend fun detectClothing(uri: Uri): DetectionResult = withContext(Dispatchers.IO) {
        val bitmap = loadBitmapFromUri(uri)
            ?: return@withContext DetectionResult.unknown()
        detector.detect(bitmap)
    }

    /**
     * Detects clothing from a file path.
     */
    suspend fun detectClothing(filePath: String): DetectionResult = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(filePath)
            ?: return@withContext DetectionResult.unknown()
        detector.detect(bitmap)
    }

    // ─── Categorization ────────────────────────────────────────────────────

    /**
     * Enriches a [DetectionResult] with full category data:
     * occasions, seasons, pattern, style types, tags, and gender relevance.
     *
     * Optionally pass the original [bitmap] to enable pattern detection.
     * Optionally pass pre-extracted [colors] to skip re-extraction.
     */
    suspend fun categorize(
        detection: DetectionResult,
        bitmap: Bitmap? = null,
        colors: List<DetectedColor> = emptyList()
    ): CategoryResult {
        FashionAILogger.d("categorize: ${detection.subTypeDisplay} (${detection.confidence})")
        return categorizer.categorize(detection, bitmap, colors)
    }

    // ─── Color Analysis ────────────────────────────────────────────────────

    /**
     * Extracts the dominant colors from a clothing image.
     *
     * @param maxColors Maximum number of colors to return (default 5).
     * @return List of [DetectedColor] sorted by dominance.
     */
    suspend fun extractColors(bitmap: Bitmap, maxColors: Int = 5): List<DetectedColor> {
        FashionAILogger.d("extractColors: maxColors=$maxColors")
        return colorAnalyzer.extractColors(bitmap, maxColors)
    }

    /**
     * Evaluates color compatibility across a set of colors.
     * Useful for checking whether a combination of clothing items works together.
     *
     * @return [CompatibilityResult] with a score, harmony type, and styling notes.
     */
    fun getColorCompatibility(colors: List<DetectedColor>): CompatibilityResult {
        return colorEngine.evaluate(colors)
    }

    /**
     * Suggests complementary accent colors for the given [baseColors].
     */
    fun suggestComplementaryColors(baseColors: List<DetectedColor>): List<DetectedColor> {
        return colorEngine.suggestAccents(baseColors)
    }

    // ─── Outfit Generation ─────────────────────────────────────────────────

    /**
     * Generates outfit suggestions from the user's wardrobe for the given context.
     *
     * The engine:
     *  1. Filters wardrobe by weather and occasion suitability
     *  2. Generates item combinations (Top + Bottom + Footwear, or Full Body + Footwear)
     *  3. Scores each combination on color, occasion, weather, style, and freshness
     *  4. Returns the top [count] ranked outfits
     *
     * @param wardrobe All clothing items available in the user's wardrobe
     * @param context Occasion, weather, mood, and style preferences
     * @param count Maximum number of outfit suggestions to return (default 3)
     *
     * @return Ranked list of [OutfitSuggestion], empty if wardrobe has insufficient items
     */
    suspend fun generateOutfit(
        wardrobe: List<ClothingItem>,
        context: OutfitContext,
        count: Int = 3
    ): List<OutfitSuggestion> {
        FashionAILogger.d(
            "generateOutfit: occasion=${context.occasion.name}, " +
            "weather=${context.weather?.condition?.name}, " +
            "wardrobe=${wardrobe.size} items"
        )

        val ranked = ruleEngine.generateOutfits(wardrobe, context, count)

        return ranked.map { r ->
            OutfitSuggestion(
                items = r.items,
                compatibilityScore = r.totalScore,
                styleNote = r.styleNote,
                occasionFit = r.occasionScore,
                weatherFit = r.weatherScore.takeIf { context.weather != null },
                colorHarmonyScore = r.colorScore,
                weatherNote = r.weatherNote
            )
        }
    }

    /**
     * Returns style suggestions — clothing items from [wardrobe] that pair well with [item].
     * Useful for "Shop the look" or "What goes with this?" features.
     */
    suspend fun getStyleSuggestions(
        item: ClothingItem,
        wardrobe: List<ClothingItem>,
        limit: Int = 5
    ): List<ClothingItem> = withContext(Dispatchers.Default) {
        val others = wardrobe.filter { it.id != item.id && it.isActive }

        others
            .map { other ->
                val colors = item.colors + other.colors
                val colorScore = if (colors.isNotEmpty()) colorEngine.evaluate(colors).score else 0.7f
                val occasionOverlap = item.occasions.intersect(other.occasions.toSet()).size.toFloat() /
                        maxOf(item.occasions.size, 1).toFloat()
                val score = colorScore * 0.6f + occasionOverlap * 0.4f
                other to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    // ─── Internals ─────────────────────────────────────────────────────────

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            FashionAILogger.e("Failed to load bitmap from URI: ${e.message}")
            null
        }
    }

    // ─── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder for [FashionAI].
     *
     * ```kotlin
     * val fashionAI = FashionAI.Builder(context)
     *     .setMode(FashionAIMode.ON_DEVICE)
     *     .setModelFileName("my_fashion_model.tflite")
     *     .setLogLevel(FashionAILogLevel.DEBUG)
     *     .build()
     * ```
     */
    class Builder(private val context: Context) {

        private var mode: FashionAIMode = FashionAIMode.ON_DEVICE
        private var apiKey: String? = null
        private var modelFileName: String = "fashion_classifier.tflite"
        private var useGpu: Boolean = true
        private var confidenceThreshold: Float = 0.25f
        private var maxOutfitResults: Int = 5
        private var logLevel: FashionAILogLevel = FashionAILogLevel.WARN

        /** Sets the SDK processing mode (default: ON_DEVICE) */
        fun setMode(mode: FashionAIMode) = apply { this.mode = mode }

        /**
         * Sets the TFLite model file name (must be placed in your app's assets/ folder).
         * Default: "fashion_classifier.tflite"
         */
        fun setModelFileName(fileName: String) = apply { this.modelFileName = fileName }

        /** API key for cloud/hybrid LLM features */
        fun setApiKey(key: String) = apply { this.apiKey = key }

        /** Enable or disable GPU delegate for inference (default: true) */
        fun setGpuAcceleration(enabled: Boolean) = apply { this.useGpu = enabled }

        /** Minimum model confidence to accept a detection (default: 0.25) */
        fun setConfidenceThreshold(threshold: Float) = apply {
            require(threshold in 0f..1f) { "Threshold must be 0.0–1.0" }
            this.confidenceThreshold = threshold
        }

        /** Maximum outfit suggestions returned per call (default: 5) */
        fun setMaxOutfitResults(max: Int) = apply {
            require(max > 0) { "Max results must be > 0" }
            this.maxOutfitResults = max
        }

        /** Set SDK log verbosity (default: WARN — set DEBUG for development) */
        fun setLogLevel(level: FashionAILogLevel) = apply { this.logLevel = level }

        /** Builds and returns the configured [FashionAI] instance. */
        fun build(): FashionAI {
            val config = FashionAIConfig(
                mode = mode,
                apiKey = apiKey,
                modelFileName = modelFileName,
                useGpuAcceleration = useGpu,
                detectionConfidenceThreshold = confidenceThreshold,
                maxOutfitResults = maxOutfitResults,
                logLevel = logLevel
            )
            return FashionAI(context.applicationContext, config)
        }
    }
}
