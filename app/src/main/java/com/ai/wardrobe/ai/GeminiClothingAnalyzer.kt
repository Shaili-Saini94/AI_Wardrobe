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
    // Google retired the free tier for Gemini 2.0 (limit: 0) and removed 1.5 entirely.
    // The 2.5-series and the "latest" aliases still have free-tier quota available.
    private val models  = listOf(
        "gemini-2.5-flash-lite",      // fastest 2.5, free tier active
        "gemini-flash-lite-latest",   // alias → currently 2.5-flash-lite
        "gemini-2.5-flash",           // bigger 2.5, free tier active
        "gemini-flash-latest"         // alias → currently 2.5-flash
    )
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

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
                - ONLY include actual physical garments/clothing — NO jewelry, NO accessories
                - Each garment gets its OWN separate array entry
                - Only list occasions that genuinely apply (1–4 max)
                - Only list seasons that genuinely apply
                - Be SPECIFIC: "Black Skinny Jeans" not just "Jeans"; "Knitted Crew Neck Sweater" not just "Sweater"

                REJECT these immediately with isClothing: false — do NOT classify them as clothing:
                - Screenshots of phone apps, websites, or any digital UI
                - Product listings, catalogue images, or illustrations
                - Images of people without visible clothing items
                - Photos of rooms, objects, food, or anything non-clothing
                - Blurry or unclear images where no garment can be identified

                If NO real clothing/garments are clearly visible, return:
                [{"isClothing": false, "reason": "clear explanation — e.g. This is a screenshot of a mobile app, not a clothing photo"}]
            """.trimIndent()

            val responseText = callGemini(buildRequestBody(prompt, base64Image))
                ?: throw java.io.IOException("All Gemini models unavailable")

            Log.d("GeminiAI", "Multi-item analysis: $responseText")
            parseMultipleClothingItems(responseText.cleanJson())

        } catch (e: Exception) {
            Log.e("GeminiAI", "Multi-item analysis failed", e)
            throw e   // propagate so ImageLabeler can fall back to TFLite
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
     * Enum of available product-shot generation methods for the side-by-side test.
     */
    enum class ProductShotMethod(val label: String) {
        POLLINATIONS("Pollinations.ai (free, text→image)"),
        CLOUDFLARE_FLUX("Cloudflare FLUX (text→image)"),
        CLOUDFLARE_IMG2IMG("Cloudflare SDXL (image→image, keeps your garment)")
    }

    /**
     * Generates a product shot using the selected [method].  Returns the absolute
     * file path of the saved PNG, or null on failure.
     */
    suspend fun generateProductShot(
        method: ProductShotMethod,
        base64Image: String,
        category: String,
        dominantColor: String? = null,
        tags: List<String> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        when (method) {
            ProductShotMethod.POLLINATIONS         -> generateViaPollinations(category, dominantColor, tags)
            ProductShotMethod.CLOUDFLARE_FLUX      -> generateViaCloudflareFlux(category, dominantColor, tags)
            ProductShotMethod.CLOUDFLARE_IMG2IMG   -> generateViaCloudflareImg2img(base64Image, category, dominantColor)
        }
    }

    // ── 1. Pollinations.ai (free, no auth) ──────────────────────────────────

    private fun generateViaPollinations(category: String, color: String?, tags: List<String>): String? {
        return try {
            val prompt = buildShotPrompt(category, color, tags)
            val encoded = java.net.URLEncoder.encode(prompt, "UTF-8")
            val url = "https://image.pollinations.ai/prompt/$encoded" +
                "?width=768&height=768&nologo=true&enhance=true&seed=" + (0..999_999).random()

            Log.d("ProductShot", "Pollinations: $url")
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            val bytes    = response.body?.bytes()
            response.close()
            if (!response.isSuccessful || bytes == null || bytes.size < 1024) {
                Log.w("ProductShot", "Pollinations HTTP ${response.code}, bytes=${bytes?.size ?: 0}")
                return null
            }
            saveImageBytes(bytes, "product_polli", "pollinations")
        } catch (e: Exception) {
            Log.e("ProductShot", "Pollinations failed: ${e.message}"); null
        }
    }

    // ── 2. Cloudflare Workers AI — FLUX text-to-image ───────────────────────

    private fun generateViaCloudflareFlux(category: String, color: String?, tags: List<String>): String? {
        return try {
            val prompt = buildShotPrompt(category, color, tags)
            val body   = JSONObject().apply {
                put("prompt", prompt)
                put("steps", 8)  // flux-1-schnell supports up to 8 steps
            }.toString()
            val bytes = callCloudflareAi("@cf/black-forest-labs/flux-1-schnell", body) ?: return null
            saveImageBytes(bytes, "product_cf_flux", "cloudflare-flux")
        } catch (e: Exception) {
            Log.e("ProductShot", "CF FLUX failed: ${e.message}"); null
        }
    }

    // ── 3. Cloudflare Workers AI — Stable Diffusion v1.5 image-to-image ─────
    //     This is the ONLY Cloudflare model that accepts an "image" input tensor
    //     (the base SDXL model is text-to-image only).

    private fun generateViaCloudflareImg2img(base64Image: String, category: String, color: String?): String? {
        return try {
            val colorHint = if (!color.isNullOrBlank()) "${color.trim()} " else ""
            val prompt = "Professional e-commerce product photograph of the same $colorHint$category " +
                "shown in the input image, placed on a soft cream studio background hex F5F0E8, " +
                "bright even studio lighting, no shadows, no person, centered composition, " +
                "high detail, preserve original colors and patterns"

            // SD v1.5 img2img expects "image" as a JSON array of byte ints (0-255)
            val imgBytes = android.util.Base64.decode(base64Image, android.util.Base64.NO_WRAP)
            val imageArray = JSONArray()
            for (b in imgBytes) imageArray.put(b.toInt() and 0xFF)

            val body = JSONObject().apply {
                put("prompt", prompt)
                put("image", imageArray)
                put("strength", 0.65)   // 0=keep original, 1=fully regenerate; 0.65 = clean BG, keep garment
                put("num_steps", 20)
                put("guidance", 7.5)
            }.toString()

            val bytes = callCloudflareAi("@cf/runwayml/stable-diffusion-v1-5-img2img", body) ?: return null
            saveImageBytes(bytes, "product_cf_img2img", "cloudflare-sd15-img2img")
        } catch (e: Exception) {
            Log.e("ProductShot", "CF img2img failed: ${e.message}"); null
        }
    }

    /** Builds the standardised "product shot" prompt used by Pollinations + CF FLUX. */
    private fun buildShotPrompt(category: String, color: String?, tags: List<String>): String {
        val colorPart = if (!color.isNullOrBlank()) "${color.trim()} " else ""
        val tagPart   = tags
            .filter { it.length > 2 && it.lowercase() !in setOf("any", "everyday") }
            .take(3)
            .joinToString(", ")
            .let { if (it.isNotBlank()) ", $it" else "" }
        return "Professional ecommerce product photograph of a $colorPart$category$tagPart, " +
            "centered flat-lay composition on a soft cream studio background hex F5F0E8, " +
            "bright even studio lighting, no shadows, no person, no text, no watermark, " +
            "high detail, 4k, clean isolated subject"
    }

    /**
     * Calls a Cloudflare Workers AI model and returns the PNG bytes.
     * Handles both response shapes: raw image/png body, OR JSON with base64 in result.image.
     */
    private fun callCloudflareAi(model: String, jsonBody: String): ByteArray? {
        val token     = BuildConfig.CF_API_TOKEN
        val accountId = BuildConfig.CF_ACCOUNT_ID
        if (token.isBlank() || accountId.isBlank()) {
            Log.e("ProductShot", "CF credentials missing in BuildConfig"); return null
        }
        val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/$model"
        val req = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $token")
            .build()

        val resp = client.newCall(req).execute()
        val code = resp.code
        val contentType = resp.header("Content-Type") ?: ""
        if (!resp.isSuccessful) {
            val errBody = resp.body?.string()?.take(400)
            resp.close()
            Log.w("ProductShot", "CF $model HTTP $code: $errBody"); return null
        }

        return if (contentType.startsWith("image/")) {
            // Raw PNG bytes
            val bytes = resp.body?.bytes(); resp.close()
            Log.d("ProductShot", "CF $model returned raw image, ${bytes?.size ?: 0} bytes")
            bytes
        } else {
            // JSON wrapper {"result":{"image":"<base64>"},"success":true}
            val bodyStr = resp.body?.string(); resp.close()
            val b64 = JSONObject(bodyStr ?: "{}").optJSONObject("result")?.optString("image")
            if (b64.isNullOrBlank()) {
                Log.w("ProductShot", "CF $model returned JSON without image data"); null
            } else {
                Log.d("ProductShot", "CF $model returned base64 image (${b64.length} chars)")
                android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
            }
        }
    }

    /**
     * Calls Gemini's image-generation model with a prompt + input image, returns the
     * generated PNG bytes (decoded from inline base64 in the response).
     */
    private fun callGeminiForImage(prompt: String, base64Image: String): ByteArray? {
        val imageModel = "gemini-2.5-flash-image"
        val url        = "$baseUrl/$imageModel:generateContent?key=$apiKey"
        try {
            // Build request body: image part + text prompt + responseModalities=IMAGE
            val parts = JSONArray()
                .put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    })
                })
                .put(JSONObject().apply { put("text", prompt) })

            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply { put("parts", parts) }))
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("IMAGE"))
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code     = response.code
            val bodyStr  = response.body?.string()
            response.close()

            if (!response.isSuccessful || bodyStr.isNullOrBlank()) {
                Log.w("GeminiAI", "$imageModel HTTP $code body=${bodyStr?.take(200)}")
                return null
            }

            // Walk the response: candidates[0].content.parts[*].inlineData.data
            val parts2 = JSONObject(bodyStr)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return null

            for (i in 0 until parts2.length()) {
                val part = parts2.optJSONObject(i) ?: continue
                val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                val data   = inline?.optString("data")
                if (!data.isNullOrBlank()) {
                    Log.d("GeminiAI", "$imageModel returned image (${data.length} b64 chars)")
                    return android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
                }
            }
            Log.w("GeminiAI", "$imageModel response had no inline image data")
            return null
        } catch (e: Exception) {
            Log.e("GeminiAI", "$imageModel call failed: ${e.message}")
            return null
        }
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
        // Single attempt per model — no retry.  Retrying a 429 just burns more
        // of the per-minute rate budget without changing the outcome; better to
        // immediately roll to the next model (which has its own separate quota).
        for (model in models) {
            val url = "$baseUrl/$model:generateContent?key=$apiKey"
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val code     = response.code
                val bodyStr  = response.body?.string()
                response.close()

                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    val text = JSONObject(bodyStr)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        Log.d("GeminiAI", "Success with $model")
                        return text
                    }
                } else {
                    Log.w("GeminiAI", "$model HTTP $code — trying next model")
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "$model failed: ${e.message}")
            }
        }
        Log.e("GeminiAI", "All Gemini models failed")
        return null
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    /**
     * Canonical clothing type names.  Multi-word variants are listed first (most specific)
     * so "Anarkali Suit" matches before a plain "Kurta" would.
     */
    private val knownTypes = listOf(
        // Multi-word / specific variants first
        "Palazzo Set", "Co-ord Set", "Anarkali Suit", "Sharara Set",
        "Maxi Dress", "Midi Dress", "Mini Dress", "Shirt Dress", "Wrap Dress",
        "Bodycon Dress", "Slip Dress", "Sundress", "Fit & Flare", "A-Line Dress",
        "Slim Fit Jeans", "Straight Jeans", "Skinny Jeans", "Mom Jeans",
        "Wide-Leg Jeans", "Ripped Jeans", "Bootcut Jeans",
        "Formal Shirt", "Oxford Shirt", "Linen Shirt", "Flannel Shirt",
        "Oversized Shirt", "Crop Top", "Tank Top", "Polo Shirt", "Hawaiian Shirt",
        "Graphic Tee", "Plain T-Shirt", "Oversized Tee", "Crop Tee", "Striped Tee",
        "Polo Tee", "Muscle Tee", "Henley",
        "Trench Coat", "Puffer Jacket", "Denim Jacket", "Leather Jacket",
        "Bomber Jacket", "Windbreaker", "Nehru Jacket", "Bandhgala", "Shacket",
        "Crew Neck Sweater", "V-Neck Sweater", "Turtleneck", "Cable Knit",
        "Cardigan", "Sweatshirt", "Hoodie", "Shrug",
        "Straight Kurta", "A-Line Kurta", "Anarkali Kurta", "Pathani Kurta",
        "Churidar Kurta", "Indo-Western Kurta", "Kurti", "Sherwani",
        "Bridal Lehenga", "Party Lehenga", "A-Line Lehenga", "Mermaid Lehenga",
        "Chaniya Choli", "Ghagra",
        "Silk Saree", "Cotton Saree", "Georgette Saree", "Chiffon Saree",
        "Banarasi Saree", "Printed Saree", "Pre-Draped Saree",
        "Oxford Shoes", "Chelsea Boots", "High Heels", "Block Heels",
        "Platform Shoes", "Slip-On Shoes", "Sports Shoes", "Loafers", "Sneakers",
        "Flat Sandals", "Heeled Sandals", "Gladiator Sandals",
        "Kolhapuri", "Juttis", "Slides",
        "Baseball Cap", "Bucket Hat", "Beanie", "Straw Hat", "Fedora", "Turban",
        // Base labels last
        "Dress", "Kurta", "Lehenga", "Saree", "Shirt", "T-Shirt", "Jeans",
        "Pants", "Shorts", "Jacket", "Blazer", "Coat", "Sweater",
        "Shoes", "Sandals", "Hat", "Choli"
    )

    /** Strip verbose Gemini descriptions down to a canonical clothing type. */
    private fun normalizeGeminiType(rawType: String): String {
        val lower = rawType.lowercase()
        return knownTypes.firstOrNull { it.lowercase() in lower } ?: rawType.trim()
    }

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
                category    = normalizeGeminiType(obj.optString("type", "Clothing Item")),
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
