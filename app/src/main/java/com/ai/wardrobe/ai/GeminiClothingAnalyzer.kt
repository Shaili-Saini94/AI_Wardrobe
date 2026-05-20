package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.ai.wardrobe.BuildConfig
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.StyleProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Shared result type ───────────────────────────────────────────────────────
data class ImageAnalysisResult(
    val isClothing: Boolean,
    val category: String,          // broad TFLite label (e.g. "Dress")
    val subCategory: String? = null, // refined type (e.g. "Maxi Dress", "Anarkali Suit")
    val tags: List<String>,
    val occasions: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val styleTypes: List<String> = emptyList(),
    val mood: String? = null,
    val weather: String? = null,
    val debugReason: String? = null
)

data class GeminiOutfitSuggestion(
    val name: String,
    val styleType: String,
    val occasionType: String,
    val itemIds: List<Long>,
    val styleNote: String,
    val weatherNote: String,
    val colorStory: String,
    val whyItWorks: String
)

/**
 * Calls Gemini 1.5 Flash via the v1 REST API using OkHttp.
 * Analyzes clothing photos and generates outfit suggestions.
 */
@Singleton
class GeminiClothingAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKey  = BuildConfig.GEMINI_API_KEY
    private val models  = listOf(
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-1.5-flash-latest"
    )
    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models"
    private val baseUrlV1b = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Clothing Analysis ────────────────────────────────────────────────────

    /**
     * Detects and analyzes ALL clothing items in an image.
     * Returns a list — multiple items if the photo contains an outfit/multiple pieces.
     */
    suspend fun analyzeAllClothing(uri: Uri): List<ImageAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadAndResizeBitmap(uri)
                ?: return@withContext listOf(errorResult("Could not load image"))
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                Examine this image and identify ONLY wearable clothing/garment items.

                INCLUDE: shirts, tops, blouses, t-shirts, dresses, lehenga, saree, choli, dupatta,
                pants, jeans, trousers, skirts, shorts, jackets, coats, hoodies, sweaters,
                suits, blazers, kurta, sherwani, jumpsuit, romper, shorts, leggings, shoes,
                sneakers, boots, sandals, heels, ethnic wear, western wear.

                EXCLUDE (do NOT detect these): earrings, necklace, bangles, bracelets, rings,
                watch, sunglasses, belt, bag, purse, hat, cap, hair accessories, jewelry of any kind.

                Return ONLY a valid JSON array with NO markdown, NO code blocks, NO extra text.
                Each element represents ONE clothing/garment item:
                [
                  {
                    "isClothing": true,
                    "type": "specific descriptive name e.g. Pink Embroidered Choli, Floral Maxi Dress, Slim Denim Jeans",
                    "color": ["primary color", "secondary color if any"],
                    "fabric": "e.g. Cotton, Polyester, Silk, Net, Georgette, Denim, Linen, Wool",
                    "pattern": "e.g. Solid, Embroidered, Floral, Plaid, Graphic, Sequined",
                    "style": "e.g. Casual, Formal, Ethnic, Traditional, Smart Casual, Streetwear, Bohemian",
                    "occasions": ["Casual", "Work", "Party", "Date Night", "Wedding", "Festival", "Formal"],
                    "seasons": ["Spring", "Summer", "Autumn", "Winter"],
                    "fit": "e.g. Slim, Regular, Flared, Fitted, Relaxed",
                    "gender": "Men / Women / Unisex",
                    "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
                    "weatherSuitability": "e.g. Warm weather, Cold weather, All weather",
                    "mood": "e.g. Professional, Elegant, Bold, Romantic, Festive"
                  }
                ]

                RULES:
                - ONLY include actual garments/clothing — NO jewelry, NO accessories
                - Each garment gets its OWN array entry
                - Only list occasions that genuinely apply (1-4 max)
                - Only list seasons that genuinely apply

                If NO clothing/garments are visible, return:
                [{"isClothing": false, "reason": "brief description of what you see"}]
            """.trimIndent()

            val responseText = callGemini(buildRequestBody(prompt, base64Image))
                ?: return@withContext listOf(errorResult("AI is busy. Please wait a moment and try again."))

            Log.d("GeminiAI", "Multi-item analysis: $responseText")
            parseMultipleClothingItems(responseText.cleanJson())

        } catch (e: Exception) {
            Log.e("GeminiAI", "Multi-item analysis failed", e)
            listOf(errorResult("Analysis failed: ${e.message}"))
        }
    }

    // Keep single-item version for backward compat
    suspend fun analyzeClothing(uri: Uri): ImageAnalysisResult =
        analyzeAllClothing(uri).firstOrNull() ?: errorResult("No items detected")

    // ── Smart Outfit Suggestions ─────────────────────────────────────────────

    suspend fun generateOutfits(
        items: List<ClothingItem>,
        occasion: String = "Any",
        weather: String = "mild weather, around 22°C",
        styleProfile: StyleProfile = StyleProfile(),
        count: Int = 5
    ): List<GeminiOutfitSuggestion> = withContext(Dispatchers.IO) {
        if (items.size < 2) return@withContext emptyList()

        try {
            val wardrobeList = items.joinToString("\n") { item ->
                "ID:${item.id} | ${item.category} | " +
                "Colors:${item.tags.take(2).joinToString(",")} | " +
                "Style:${item.styleTypes.firstOrNull() ?: "?"} | " +
                "Occasions:${item.occasions.take(3).joinToString(",")} | " +
                "Seasons:${item.seasons.take(2).joinToString(",")}"
            }

            val profileSection = buildString {
                val styles = styleProfile.preferredStyles
                val occasions = styleProfile.favoriteOccasions
                if (styles.isNotEmpty()) appendLine("- Preferred styles: ${styles.joinToString(", ")}")
                else appendLine("- Preferred styles: Open to all styles")
                if (occasions.isNotEmpty()) appendLine("- Favourite occasions: ${occasions.joinToString(", ")}")
                appendLine("- Color palette: ${styleProfile.colorPalette}")
                appendLine("- Gender: ${styleProfile.gender}")
            }

            val prompt = """
                You are a world-class fashion stylist with expertise in color theory and editorial styling.

                USER STYLE PROFILE:
                $profileSection
                WARDROBE (use ONLY these item IDs):
                $wardrobeList

                CONTEXT:
                - Target occasion: $occasion
                - Weather/season: $weather

                ════════════════════════════════════
                FASHION RULES — FOLLOW STRICTLY
                ════════════════════════════════════

                1. COLOR HARMONY — every outfit must use ONE strategy:
                   • Monochromatic: shades/tints of one color
                   • Neutral base + one accent (e.g. all-black + white sneaker)
                   • Complementary: opposite on color wheel (blue + orange, etc.)
                   • Classic neutrals: black / white / grey / beige / navy only
                   NEVER combine more than 3 colors. NEVER clash (red + orange, green + purple).

                2. STYLE DNA — all items in one outfit must share the same aesthetic:
                   • Minimalist = clean silhouettes, no logos, neutral/earth palette, subtle details
                   • Classic = timeless cuts, quality fabrics, muted tones, no trendy pieces
                   • Smart Casual = polished-casual balance (chinos + white shirt, blazer + dark jeans)
                   • Streetwear = oversized, graphic tees, bold sneakers, urban attitude
                   • Funky = bold prints, clashing textures, statement pieces, maximalist energy
                   • Bohemian = flowy, earthy palette, pattern-mixing, relaxed layering
                   • Athleisure = performance fabrics, sporty cuts, functional design
                   • Formal = structured tailoring, dark palette, polished footwear
                   FORBIDDEN MIXES: Formal + Streetwear | Bohemian + Athleisure | Minimalist + Funky

                3. OCCASION FIT — every item must genuinely suit the target occasion.
                4. WEATHER FIT — fabric weight and coverage must match conditions.
                5. PROPORTION — pair fitted top with relaxed bottom (or vice versa).
                6. COMPLETE LOOK — minimum: one top/dress + one bottom (or full-body piece).
                   Add shoes if available in wardrobe.

                ════════════════════════════════════
                NAMING CONVENTION
                ════════════════════════════════════
                Format: "[StyleType] [OccasionDescriptor]"
                Examples: "Minimalist Work Day", "Funky Date Night", "Classic Weekend Edit",
                           "Streetwear Casual Friday", "Bohemian Festival Look", "Smart Casual Brunch"

                styleType MUST be exactly one of:
                Minimalist | Classic | Smart Casual | Streetwear | Funky | Bohemian | Athleisure | Formal

                occasionType MUST be exactly one of:
                Work | Date Night | Weekend | Casual | Party | Gym | Beach | Travel | Formal Event

                ════════════════════════════════════
                OUTPUT — return ONLY a valid JSON array, NO markdown:
                ════════════════════════════════════
                [
                  {
                    "name": "Minimalist Work Day",
                    "styleType": "Minimalist",
                    "occasionType": "Work",
                    "itemIds": [1, 3, 5],
                    "styleNote": "Clean, polished silhouette that reads intentional without effort.",
                    "weatherNote": "Breathable cotton layers ideal for mild, air-conditioned environments.",
                    "colorStory": "Ivory and stone — a quiet neutral palette that always photographs well.",
                    "whyItWorks": "The slim-fit trousers anchor the oversized shirt; tonal dressing eliminates the need for accessories."
                  }
                ]

                Generate exactly $count outfits. Each outfit must use DIFFERENT item combinations.
                Prioritise the user's preferred styles. If occasion is 'Any', generate a variety.
            """.trimIndent()

            val responseText = callGemini(buildRequestBody(prompt)) ?: return@withContext emptyList()

            Log.d("GeminiAI", "Smart outfit suggestions: $responseText")
            parseOutfitSuggestions(responseText.cleanJson())

        } catch (e: Exception) {
            Log.e("GeminiAI", "Outfit generation failed", e)
            emptyList()
        }
    }

    // ── Thumbnail Generation ─────────────────────────────────────────────────
    // Uses the ACTUAL uploaded photo — crops it to a clean square thumbnail.
    // No external API call; instant and always shows the real clothing item.

    /**
     * Crops the uploaded photo to a square and saves as the front thumbnail.
     */
    suspend fun generateFrontThumbnail(
        base64Image: String,
        category: String,
        tags: List<String>
    ): String? = withContext(Dispatchers.IO) {
        cropBase64ToThumbnail(base64Image, "front")
    }

    /**
     * Crops the uploaded photo to a square and saves as the back thumbnail.
     * Since we only have one angle, the same photo is used — no random fake image.
     */
    suspend fun generateBackThumbnail(
        base64Image: String?,
        category: String,
        tags: List<String>
    ): String? = withContext(Dispatchers.IO) {
        if (base64Image == null) return@withContext null
        cropBase64ToThumbnail(base64Image, "back")
    }

    /**
     * Decodes base64 → Bitmap, center-crops to square, scales to 512×512, saves to file.
     */
    private fun cropBase64ToThumbnail(base64Image: String, suffix: String): String? {
        return try {
            val bytes = android.util.Base64.decode(base64Image, android.util.Base64.NO_WRAP)
            val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: run {
                Log.w("GeminiAI", "Thumbnail[$suffix]: failed to decode base64 bitmap")
                return null
            }

            // Center-crop to square
            val side = minOf(raw.width, raw.height)
            val x = (raw.width - side) / 2
            val y = (raw.height - side) / 2
            val cropped = Bitmap.createBitmap(raw, x, y, side, side)

            // Scale to 512×512
            val scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true)

            // Save as JPEG
            val thumbDir = File(context.filesDir, "thumbnails").also { it.mkdirs() }
            val thumbFile = File(thumbDir, "${UUID.randomUUID()}_$suffix.jpg")
            thumbFile.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            Log.d("GeminiAI", "Thumbnail[$suffix] saved from real photo: ${thumbFile.absolutePath}")
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e("GeminiAI", "Thumbnail[$suffix] crop failed: ${e.message}")
            null
        }
    }

    private fun saveImageBytes(bytes: ByteArray, suffix: String, model: String): String? {
        return try {
            val thumbDir = File(context.filesDir, "thumbnails").also { it.mkdirs() }
            val thumbFile = File(thumbDir, "${UUID.randomUUID()}_$suffix.png")
            thumbFile.writeBytes(bytes)
            Log.d("GeminiAI", "Thumbnail[$suffix] saved via $model: ${thumbFile.absolutePath} (${bytes.size} bytes)")
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e("GeminiAI", "Failed to save thumbnail: ${e.message}")
            null
        }
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private fun buildRequestBody(prompt: String, base64Image: String? = null): String {
        val parts = JSONArray()
        if (base64Image != null) {
            parts.put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Image)
                })
            })
        }
        parts.put(JSONObject().apply { put("text", prompt) })
        return JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply { put("parts", parts) }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 4096)
            })
        }.toString()
    }

    private fun callGemini(body: String): String? {
        for (model in models) {
            try {
                val base = if (model.startsWith("gemini-1.5")) baseUrlV1b else baseUrl
                val url = "$base/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@use
                    if (response.isSuccessful) {
                        Log.d("GeminiAI", "Success with $model")
                        val text = JSONObject(responseBody)
                            .optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")
                        if (text != null) return text
                    }
                    Log.w("GeminiAI", "$model HTTP ${response.code} — trying next")
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "$model failed: ${e.message}")
            }
        }
        Log.e("GeminiAI", "All Gemini models failed")
        return null
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    private fun parseClothingAnalysis(json: String): ImageAnalysisResult {
        return try {
            val obj = JSONObject(json)

            if (!obj.optBoolean("isClothing", true)) {
                return errorResult(obj.optString("reason", "Not a clothing item"))
            }

            val colors   = obj.optJSONArray("color")?.toStringList() ?: emptyList()
            val occasions = obj.optJSONArray("occasions")?.toStringList() ?: emptyList()
            val seasons  = obj.optJSONArray("seasons")?.toStringList() ?: emptyList()
            val tags     = obj.optJSONArray("tags")?.toStringList() ?: emptyList()
            val fabric   = obj.optString("fabric", "")
            val pattern  = obj.optString("pattern", "")
            val fit      = obj.optString("fit", "")
            val style    = obj.optString("style", "Casual")

            val enrichedTags = buildList {
                addAll(colors)
                if (fabric.isNotBlank()) add(fabric)
                if (pattern.isNotBlank() && pattern != "Solid") add(pattern)
                if (fit.isNotBlank()) add(fit)
                addAll(tags)
            }.distinct().take(10)

            ImageAnalysisResult(
                isClothing  = true,
                category    = obj.optString("type", "Clothing Item").trim(),
                tags        = enrichedTags,
                occasions   = occasions,
                seasons     = seasons,
                styleTypes  = if (style.isNotBlank()) listOf(style) else emptyList(),
                mood        = obj.optString("mood").takeIf { it.isNotBlank() },
                weather     = obj.optString("weatherSuitability").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e("GeminiAI", "Parse error: $json", e)
            errorResult("Could not parse AI response")
        }
    }

    private fun parseMultipleClothingItems(json: String): List<ImageAnalysisResult> {
        return try {
            // Response must be a JSON array
            val array = when {
                json.trimStart().startsWith("[") -> JSONArray(json)
                else -> JSONArray().put(JSONObject(json))  // fallback: single object
            }

            val results = mutableListOf<ImageAnalysisResult>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (!obj.optBoolean("isClothing", true)) {
                    // Only add a "not clothing" result if it's the ONLY item
                    if (array.length() == 1) {
                        results.add(errorResult(obj.optString("reason", "Not a clothing item")))
                    }
                    continue
                }
                results.add(parseClothingAnalysis(obj.toString()))
            }

            results.ifEmpty { listOf(errorResult("No clothing items detected")) }
        } catch (e: Exception) {
            Log.e("GeminiAI", "Multi-item parse error: $json", e)
            listOf(errorResult("Could not parse AI response"))
        }
    }

    private fun parseOutfitSuggestions(json: String): List<GeminiOutfitSuggestion> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val idsArray = obj.optJSONArray("itemIds")
                val ids = (0 until (idsArray?.length() ?: 0)).map { idsArray!!.getLong(it) }
                if (ids.isEmpty()) return@mapNotNull null
                GeminiOutfitSuggestion(
                    name         = obj.optString("name", "Style ${i + 1}"),
                    styleType    = obj.optString("styleType", "Classic"),
                    occasionType = obj.optString("occasionType", "Casual"),
                    itemIds      = ids,
                    styleNote    = obj.optString("styleNote", ""),
                    weatherNote  = obj.optString("weatherNote", ""),
                    colorStory   = obj.optString("colorStory", ""),
                    whyItWorks   = obj.optString("whyItWorks", "")
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiAI", "Outfit parse error", e)
            emptyList()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun loadAndResizeBitmap(uri: Uri): Bitmap? {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null
            val maxDim = 1024
            if (raw.width <= maxDim && raw.height <= maxDim) return raw
            val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
            val matrix = Matrix().apply { postScale(scale, scale) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        } catch (e: Exception) { null }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.cleanJson(): String = this
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    private fun JSONArray.toStringList(): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until length()) {
            val s = this.getString(i)
            if (s.isNotBlank()) result.add(s)
        }
        return result
    }

    private fun errorResult(reason: String) = ImageAnalysisResult(
        isClothing  = false,
        category    = "Unknown",
        tags        = emptyList(),
        debugReason = reason
    )
}
