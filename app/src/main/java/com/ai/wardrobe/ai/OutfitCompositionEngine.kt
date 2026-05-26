package com.ai.wardrobe.ai

import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.StyleProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based outfit composition engine.
 * Produces ranked outfit combinations from a wardrobe using:
 *   - Color theory (harmony, complements, neutrals, monochromes)
 *   - Style consistency matrix (Minimalist + Classic = good; Formal + Streetwear = bad)
 *   - Occasion fit
 *   - Season fit
 *   - Profile alignment (user's preferred styles)
 *
 * 100% on-device — no API calls, no rate limits.
 */
@Singleton
class OutfitCompositionEngine @Inject constructor() {

    // ── Garment slot classification ──────────────────────────────────────────
    enum class Slot { TOP, BOTTOM, FULL, OUTER, FOOT, OTHER }

    private val topWords = setOf(
        // Basic tops
        "shirt", "t-shirt", "tshirt", "tee", "blouse", "top",
        // Knitwear & warm tops
        "sweater", "hoodie", "sweatshirt", "tunic",
        "turtleneck", "henley", "cable knit", "v-neck sweater", "crew neck",
        // Indian tops
        "kurta", "kurti", "choli", "pathani",
        // Crop / tank
        "crop", "tank", "muscle",
        // Polo / oxford
        "polo", "oxford shirt", "flannel shirt", "linen shirt", "hawaiian"
    )
    private val bottomWords = setOf(
        "jeans", "pants", "trouser", "shorts", "skirt",
        "leggings", "joggers", "chino", "salwar", "palazzo",
        "capri", "culottes", "harem", "bermuda", "cargo shorts",
        "cycling shorts", "board shorts", "linen shorts",
        "slim fit", "straight jeans", "skinny", "mom jeans",
        "wide-leg jeans", "bootcut", "ripped jeans", "denim shorts",
        // Indian bottoms
        "ghagra"
    )
    private val fullWords = setOf(
        "dress", "gown", "jumpsuit", "romper",
        "saree", "sari", "lehenga", "sherwani", "anarkali",
        // Full Indian outfits
        "chaniya", "sharara", "kaftan", "co-ord set",
        // Specific dress types
        "maxi dress", "mini dress", "midi dress", "wrap dress",
        "bodycon", "sundress", "slip dress", "off-shoulder dress",
        "fit & flare", "shirt dress", "a-line dress"
    )
    private val outerWords = setOf(
        "jacket", "coat", "blazer", "cardigan", "vest",
        "shrug", "dupatta", "trench",
        // Sub-category outer names
        "denim jacket", "leather jacket", "bomber", "windbreaker",
        "puffer", "nehru jacket", "bandhgala", "shacket",
        "indo-western"  // Indo-Western Kurta is technically outer/full, treat as top via topWords first
    )
    private val footWords = setOf(
        "shoe", "sneaker", "boot", "sandal", "heel",
        "flat", "loafer", "mule",
        // Sub-category foot names
        "oxford shoes", "chelsea", "platform", "slides",
        "kolhapuri", "jutti", "juttis", "sports shoes", "slip-on",
        "block heel", "high heel", "heeled sandal", "gladiator",
        "flat sandal"
    )

    private fun slotOf(item: ClothingItem): Slot {
        val c = item.category.lowercase()
        return when {
            fullWords.any   { c.contains(it) } -> Slot.FULL   // full-body first (e.g. "Anarkali" before "kurta")
            topWords.any    { c.contains(it) } -> Slot.TOP
            bottomWords.any { c.contains(it) } -> Slot.BOTTOM
            outerWords.any  { c.contains(it) } -> Slot.OUTER
            footWords.any   { c.contains(it) } -> Slot.FOOT
            else                               -> Slot.OTHER
        }
    }

    // ── Color analysis ───────────────────────────────────────────────────────
    private enum class ColorFamily {
        NEUTRAL_LIGHT, NEUTRAL_DARK, NEUTRAL_MID, NEUTRAL_WARM,
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK, BROWN, UNKNOWN
    }

    private val colorMap = mapOf(
        "white" to ColorFamily.NEUTRAL_LIGHT, "ivory" to ColorFamily.NEUTRAL_LIGHT,
        "cream" to ColorFamily.NEUTRAL_LIGHT, "off-white" to ColorFamily.NEUTRAL_LIGHT,
        "ecru" to ColorFamily.NEUTRAL_LIGHT, "pearl" to ColorFamily.NEUTRAL_LIGHT,

        "black" to ColorFamily.NEUTRAL_DARK, "charcoal" to ColorFamily.NEUTRAL_DARK,
        "jet" to ColorFamily.NEUTRAL_DARK,

        "grey" to ColorFamily.NEUTRAL_MID, "gray" to ColorFamily.NEUTRAL_MID,
        "silver" to ColorFamily.NEUTRAL_MID, "slate" to ColorFamily.NEUTRAL_MID,

        "beige" to ColorFamily.NEUTRAL_WARM, "tan" to ColorFamily.NEUTRAL_WARM,
        "khaki" to ColorFamily.NEUTRAL_WARM, "stone" to ColorFamily.NEUTRAL_WARM,
        "sand" to ColorFamily.NEUTRAL_WARM, "nude" to ColorFamily.NEUTRAL_WARM,
        "taupe" to ColorFamily.NEUTRAL_WARM,

        "red" to ColorFamily.RED, "crimson" to ColorFamily.RED,
        "burgundy" to ColorFamily.RED, "maroon" to ColorFamily.RED, "scarlet" to ColorFamily.RED,

        "orange" to ColorFamily.ORANGE, "rust" to ColorFamily.ORANGE,
        "terracotta" to ColorFamily.ORANGE, "coral" to ColorFamily.ORANGE,
        "peach" to ColorFamily.ORANGE,

        "yellow" to ColorFamily.YELLOW, "mustard" to ColorFamily.YELLOW,
        "gold" to ColorFamily.YELLOW, "lemon" to ColorFamily.YELLOW,

        "green" to ColorFamily.GREEN, "olive" to ColorFamily.GREEN,
        "forest" to ColorFamily.GREEN, "mint" to ColorFamily.GREEN,
        "emerald" to ColorFamily.GREEN, "sage" to ColorFamily.GREEN,

        "blue" to ColorFamily.BLUE, "navy" to ColorFamily.BLUE,
        "denim" to ColorFamily.BLUE, "teal" to ColorFamily.BLUE,
        "cobalt" to ColorFamily.BLUE, "sky" to ColorFamily.BLUE,
        "indigo" to ColorFamily.BLUE,

        "purple" to ColorFamily.PURPLE, "lavender" to ColorFamily.PURPLE,
        "plum" to ColorFamily.PURPLE, "violet" to ColorFamily.PURPLE,
        "lilac" to ColorFamily.PURPLE,

        "pink" to ColorFamily.PINK, "blush" to ColorFamily.PINK,
        "rose" to ColorFamily.PINK, "magenta" to ColorFamily.PINK,
        "fuchsia" to ColorFamily.PINK,

        "brown" to ColorFamily.BROWN, "chocolate" to ColorFamily.BROWN,
        "camel" to ColorFamily.BROWN, "cognac" to ColorFamily.BROWN,
        "mocha" to ColorFamily.BROWN
    )

    private fun extractColor(item: ClothingItem): ColorFamily {
        for (tag in item.tags) {
            val t = tag.lowercase()
            for ((name, family) in colorMap) {
                if (t.contains(name)) return family
            }
        }
        return ColorFamily.UNKNOWN
    }

    private fun colorHarmony(a: ColorFamily, b: ColorFamily): Float {
        if (a == ColorFamily.UNKNOWN || b == ColorFamily.UNKNOWN) return 0.55f
        val neutrals = setOf(ColorFamily.NEUTRAL_LIGHT, ColorFamily.NEUTRAL_DARK,
                             ColorFamily.NEUTRAL_MID, ColorFamily.NEUTRAL_WARM)
        val neutralA = a in neutrals
        val neutralB = b in neutrals

        if (neutralA && neutralB) return 0.95f
        if (neutralA || neutralB) return 0.90f
        if (a == b)               return 0.85f

        val complements = setOf(
            setOf(ColorFamily.RED,    ColorFamily.GREEN),
            setOf(ColorFamily.BLUE,   ColorFamily.ORANGE),
            setOf(ColorFamily.YELLOW, ColorFamily.PURPLE),
            setOf(ColorFamily.PINK,   ColorFamily.GREEN)
        )
        if (setOf(a, b) in complements) return 0.78f

        val analogous = listOf(
            setOf(ColorFamily.RED, ColorFamily.ORANGE, ColorFamily.YELLOW),
            setOf(ColorFamily.GREEN, ColorFamily.BLUE, ColorFamily.PURPLE),
            setOf(ColorFamily.PINK, ColorFamily.PURPLE, ColorFamily.RED),
            setOf(ColorFamily.BROWN, ColorFamily.ORANGE, ColorFamily.YELLOW)
        )
        if (analogous.any { a in it && b in it }) return 0.72f

        return 0.40f
    }

    private fun harmonyOfOutfit(items: List<ClothingItem>): Float {
        val colors = items.map(::extractColor)
        if (colors.size < 2) return 0.7f
        var sum = 0f; var n = 0
        for (i in colors.indices) for (j in i + 1 until colors.size) {
            sum += colorHarmony(colors[i], colors[j]); n++
        }
        return if (n == 0) 0.7f else sum / n
    }

    // ── Style affinity matrix ────────────────────────────────────────────────
    private val styleAffinity = mapOf(
        setOf("Minimalist", "Classic")      to 0.85f,
        setOf("Minimalist", "Smart Casual") to 0.80f,
        setOf("Classic", "Smart Casual")    to 0.85f,
        setOf("Classic", "Formal")          to 0.85f,
        setOf("Smart Casual", "Formal")     to 0.70f,
        setOf("Streetwear", "Athleisure")   to 0.75f,
        setOf("Bohemian", "Funky")          to 0.70f,
        // Conflicts
        setOf("Formal", "Streetwear")       to 0.10f,
        setOf("Formal", "Athleisure")       to 0.05f,
        setOf("Bohemian", "Athleisure")     to 0.20f,
        setOf("Minimalist", "Funky")        to 0.25f,
        setOf("Classic", "Streetwear")      to 0.25f,
        setOf("Classic", "Athleisure")      to 0.25f
    )

    private fun styleConsistency(items: List<ClothingItem>): Float {
        val styles = items.flatMap { it.styleTypes }.filter { it.isNotBlank() }.distinct()
        if (styles.size <= 1) return 0.95f
        var sum = 0f; var n = 0
        for (i in styles.indices) for (j in i + 1 until styles.size) {
            sum += styleAffinity[setOf(styles[i], styles[j])] ?: 0.5f; n++
        }
        return if (n == 0) 0.7f else sum / n
    }

    // ── Occasion / season / profile fit ──────────────────────────────────────
    private fun occasionFit(items: List<ClothingItem>, target: String): Float {
        if (target.isBlank() || target.equals("Any", ignoreCase = true)) return 1.0f
        val hits = items.count { item -> item.occasions.any { it.equals(target, true) } }
        return hits.toFloat() / items.size
    }

    private fun seasonFit(items: List<ClothingItem>, season: String): Float {
        if (season.isBlank()) return 1.0f
        val hits = items.count { item -> item.seasons.any { it.equals(season, true) } }
        return hits.toFloat() / items.size
    }

    private fun profileFit(items: List<ClothingItem>, profile: StyleProfile): Float {
        if (profile.preferredStyles.isEmpty()) return 1.0f
        val hits = items.count { item -> item.styleTypes.any { it in profile.preferredStyles } }
        return 0.5f + 0.5f * (hits.toFloat() / items.size)
    }

    private data class Scored(
        val items: List<ClothingItem>,
        val total: Float,
        val color: Float,
        val style: Float,
        val occ: Float,
        val sea: Float,
        val prof: Float
    )

    // ── Occasion hard-exclusion rules ────────────────────────────────────────
    // Maps occasion → categories that should NEVER appear in those outfits.
    // This prevents e.g. a Lehenga or Saree in a "Work" or "Gym" outfit.
    private val occasionExclusions: Map<String, Set<String>> = mapOf(
        "Work"         to setOf("lehenga", "saree", "ghagra", "chaniya", "sharara", "bridal", "gown"),
        "Gym"          to setOf("lehenga", "saree", "dress", "kurta", "blazer", "formal", "gown",
                                "ghagra", "chaniya", "sharara", "trench", "sherwani"),
        "Formal Event" to setOf("gym", "shorts", "jogger", "hoodie", "sweatshirt", "crop tee",
                                "board shorts", "cycling shorts", "muscle tee"),
        "Beach"        to setOf("blazer", "formal shirt", "trousers", "sherwani", "bandhgala",
                                "lehenga", "saree", "gown"),
        "Date Night"   to setOf("gym", "jogger", "sweatshirt", "cycling shorts")
    )

    /**
     * Returns true if this combo contains any item that is explicitly
     * excluded from the target occasion.
     */
    private fun hasOccasionConflict(combo: List<ClothingItem>, occasion: String): Boolean {
        if (occasion.isBlank() || occasion.equals("Any", ignoreCase = true)) return false
        val exclusions = occasionExclusions[occasion] ?: return false
        return combo.any { item ->
            val cat = item.category.lowercase()
            exclusions.any { excl -> cat.contains(excl) }
        }
    }

    // ── Ethnic female garments ────────────────────────────────────────────────
    // These should only be paired with ethnic/open footwear (sandals, juttis,
    // kolhapuri, heels) — never with western closed shoes (sneakers, oxfords,
    // chelsea boots, loafers etc.)

    private val ethnicFemaleKeywords = setOf(
        "kurta", "kurti", "lehenga", "saree", "anarkali", "sharara",
        "chaniya", "ghagra", "palazzo set", "sharara set", "salwar",
        "churidar", "indo-western kurta", "a-line kurta", "straight kurta"
    )

    // Western closed-toe shoes that clash with ethnic Indian female wear
    private val westernShoeKeywords = setOf(
        "sneaker", "oxford shoe", "chelsea", "loafer", "slip-on shoe",
        "sports shoe", "platform shoe", "oxford", "derby", "brogue",
        "boat shoe", "ballet flat"
    )

    /**
     * Returns true if the combo pairs ethnic female clothing with
     * western closed-toe shoes — culturally and stylistically mismatched.
     */
    private fun hasEthnicShoeConflict(combo: List<ClothingItem>): Boolean {
        val hasEthnic = combo.any { item ->
            val cat = item.category.lowercase()
            ethnicFemaleKeywords.any { cat.contains(it) }
        }
        if (!hasEthnic) return false

        return combo.any { item ->
            val cat = item.category.lowercase()
            westernShoeKeywords.any { cat.contains(it) }
        }
    }

    // ── Outfit generation ────────────────────────────────────────────────────
    fun generate(
        items: List<ClothingItem>,
        occasion: String = "Any",
        season: String = "",
        profile: StyleProfile = StyleProfile(),
        count: Int = 5
    ): List<Outfit> {
        if (items.isEmpty()) return emptyList()

        val buckets = items.groupBy { slotOf(it) }
        val tops    = buckets[Slot.TOP]    ?: emptyList()
        val bottoms = buckets[Slot.BOTTOM] ?: emptyList()
        val fulls   = buckets[Slot.FULL]   ?: emptyList()
        val outers  = buckets[Slot.OUTER]  ?: emptyList()
        val feet    = buckets[Slot.FOOT]   ?: emptyList()

        val candidates = mutableListOf<List<ClothingItem>>()

        // Top + Bottom (+ optional shoes, + optional outer)
        for (t in tops) for (b in bottoms) {
            candidates.add(listOf(t, b))
            for (s in feet) {
                candidates.add(listOf(t, b, s))
                for (o in outers) candidates.add(listOf(t, b, s, o))
            }
        }
        // Full body (+ optional shoes, + optional outer)
        for (f in fulls) {
            candidates.add(listOf(f))
            for (s in feet) {
                candidates.add(listOf(f, s))
                for (o in outers) candidates.add(listOf(f, s, o))
            }
        }
        // Top + Outer (no matching bottoms found)
        if (bottoms.isEmpty()) {
            for (t in tops) for (o in outers) candidates.add(listOf(t, o))
        }

        // ── Fallback: if slot-based produced nothing, pair any 2–3 items ──────
        if (candidates.isEmpty()) {
            val pool = items.take(20)  // limit combinations
            for (i in pool.indices) for (j in i + 1 until pool.size) {
                candidates.add(listOf(pool[i], pool[j]))
                if (j + 1 < pool.size) candidates.add(listOf(pool[i], pool[j], pool[j + 1]))
            }
        }

        if (candidates.isEmpty()) return emptyList()

        // Remove combos with explicit occasion conflict OR ethnic-shoe mismatch
        val filtered = candidates
            .filter { combo ->
                val okOccasion = occasion.isBlank() || occasion.equals("Any", ignoreCase = true)
                               || !hasOccasionConflict(combo, occasion)
                val okEthnic   = !hasEthnicShoeConflict(combo)
                okOccasion && okEthnic
            }
            .ifEmpty { candidates }  // if all are removed, fall back to unfiltered (lenient)

        // Minimum occasion score: when a specific occasion is requested (not "Any"),
        // drop outfits where NONE of the items match that occasion.
        val minOccScore = if (!occasion.isBlank() && !occasion.equals("Any", ignoreCase = true)) 0.01f else 0f

        val scored = filtered.map { combo ->
            val color = harmonyOfOutfit(combo)
            val style = styleConsistency(combo)
            val occ   = occasionFit(combo, occasion)
            val sea   = seasonFit(combo, season)
            val prof  = profileFit(combo, profile)
            val total = (color * 0.25f) + (style * 0.30f) + (occ * 0.20f) +
                        (sea * 0.15f) + (prof * 0.10f)
            Scored(combo, total, color, style, occ, sea, prof)
        }
        .filter { it.occ >= minOccScore }
        .ifEmpty {
            // If strict filtering removed everything, fall back without the occasion filter
            filtered.map { combo ->
                val color = harmonyOfOutfit(combo)
                val style = styleConsistency(combo)
                val occ   = occasionFit(combo, occasion)
                val sea   = seasonFit(combo, season)
                val prof  = profileFit(combo, profile)
                val total = (color * 0.25f) + (style * 0.30f) + (occ * 0.20f) +
                            (sea * 0.15f) + (prof * 0.10f)
                Scored(combo, total, color, style, occ, sea, prof)
            }
        }
        .sortedByDescending { it.total }
        .distinctBy { it.items.mapNotNull(ClothingItem::id).toSet() }
        .take(count)

        return scored.map { s ->
            val dominantStyle = s.items.flatMap { it.styleTypes }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key ?: "Casual"
            val outfitOccasion = if (occasion.isNotBlank() && !occasion.equals("Any", true)) occasion
                else s.items.flatMap { it.occasions }
                    .groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key ?: "Casual"

            val trend      = detectTrend(s.items)
            val colorStory = buildColorStory(s.items, s.color)
            val outfitName = buildOutfitName(s.items, dominantStyle, outfitOccasion, trend)
            val styleNote  = buildStyleNote(s.items, dominantStyle, trend)
            val whyWorks   = buildWhyItWorks(s, s.items, colorStory)

            Outfit(
                name         = outfitName,
                styleType    = dominantStyle,
                occasionType = outfitOccasion,
                items        = s.items,
                styleNote    = styleNote,
                weatherNote  = describeWeather(s.sea, season),
                colorStory   = colorStory,
                whyItWorks   = whyWorks,
                dateCreated  = System.currentTimeMillis()
            )
        }
    }

    // ── Trend detection ───────────────────────────────────────────────────────

    private enum class Trend {
        MONOCHROME, TONAL, COLOR_BLOCK, EARTH_TONES, PASTEL_POP,
        POWER_DRESSING, STREET_LUXE, ETHNIC_FUSION, ATHLEISURE_CHIC,
        COASTAL_COOL, NONE
    }

    private fun detectTrend(items: List<ClothingItem>): Trend {
        val colors   = items.map { getItemColor(it) }
        val families = colors.map { colorFamilyOf(it) }
        val styles   = items.flatMap { it.styleTypes }
        val cats     = items.map { it.category.lowercase() }

        val neutralFamilies = setOf(ColorFamily.NEUTRAL_LIGHT, ColorFamily.NEUTRAL_DARK,
                                    ColorFamily.NEUTRAL_MID, ColorFamily.NEUTRAL_WARM)

        // Monochrome — all items same color family
        if (families.distinct().size == 1) return Trend.MONOCHROME

        // Tonal — all items within 1–2 close families (e.g. beige + tan + cream)
        val allNeutral = families.all { it in neutralFamilies }
        if (allNeutral && families.distinct().size <= 2) return Trend.TONAL

        // Earth tones — majority warm neutrals + brown/olive
        val earthColors = setOf(ColorFamily.NEUTRAL_WARM, ColorFamily.BROWN, ColorFamily.ORANGE, ColorFamily.GREEN)
        if (families.count { it in earthColors } >= (families.size * 0.7f)) return Trend.EARTH_TONES

        // Pastel pop — pinks, lavenders, light blues, creams
        val pastelFamilies = setOf(ColorFamily.PINK, ColorFamily.PURPLE, ColorFamily.NEUTRAL_LIGHT)
        if (families.count { it in pastelFamilies } >= (families.size * 0.6f)) return Trend.PASTEL_POP

        // Color block — exactly 2 bold contrasting color families
        val boldFamilies = families.filter { it !in neutralFamilies }
        if (boldFamilies.distinct().size == 2 && items.size >= 2) return Trend.COLOR_BLOCK

        // Power dressing — blazer/formal + tailored trousers
        val hasBlazer  = cats.any { "blazer" in it || "formal" in it }
        val hasTailored = cats.any { "trouser" in it || "chino" in it || "formal pants" in it }
        if (hasBlazer && hasTailored) return Trend.POWER_DRESSING

        // Street luxe — streetwear items + one elevated piece (blazer/leather)
        val streetItems  = styles.count { it in setOf("Streetwear", "Athleisure") }
        val luxeItems    = cats.count { "blazer" in it || "leather" in it || "trench" in it }
        if (streetItems >= 1 && luxeItems >= 1) return Trend.STREET_LUXE

        // Ethnic fusion — mix of ethnic + western
        val ethnicItems  = styles.count { it == "Ethnic" }
        val westernItems = styles.count { it in setOf("Casual", "Classic", "Minimalist", "Smart Casual") }
        if (ethnicItems >= 1 && westernItems >= 1) return Trend.ETHNIC_FUSION

        // Athleisure chic — sporty items styled up
        val athItems = styles.count { it == "Athleisure" }
        if (athItems >= 2) return Trend.ATHLEISURE_CHIC

        // Coastal cool — light colors + sandals/shorts/linen
        val isCoastal = cats.any { "sandal" in it || "shorts" in it || "linen" in it }
        val isLight   = families.count { it == ColorFamily.NEUTRAL_LIGHT || it == ColorFamily.BLUE } >= (families.size * 0.5f)
        if (isCoastal && isLight) return Trend.COASTAL_COOL

        return Trend.NONE
    }

    // ── Color helpers ─────────────────────────────────────────────────────────

    /** Gets the best color for an item: dominantColor field first, then tags. */
    private fun getItemColor(item: ClothingItem): String {
        val dominant = item.dominantColor
        if (!dominant.isNullOrBlank()) return dominant.lowercase()
        return item.tags.firstOrNull { tag ->
            colorMap.keys.any { tag.lowercase().contains(it) }
        }?.lowercase() ?: "unknown"
    }

    private fun colorFamilyOf(colorStr: String): ColorFamily {
        val c = colorStr.lowercase()
        for ((name, family) in colorMap) {
            if (c.contains(name)) return family
        }
        return ColorFamily.UNKNOWN
    }

    private fun colorLabel(colorStr: String): String {
        val c = colorStr.lowercase()
        for (name in colorMap.keys) {
            if (c.contains(name)) return name.replaceFirstChar(Char::titlecase)
        }
        return colorStr.replaceFirstChar(Char::titlecase).ifBlank { "Neutral" }
    }

    // ── Smart outfit naming ───────────────────────────────────────────────────

    /** Maps color names to editorial / fashion-forward adjectives. */
    private fun colorEditorial(colorStr: String): String {
        val c = colorStr.lowercase()
        return when {
            "black"  in c || "charcoal" in c || "jet" in c  -> "Noir"
            "white"  in c || "ivory" in c || "cream" in c   -> "Blanc"
            "navy"   in c || "indigo" in c                   -> "Midnight"
            "grey"   in c || "gray"  in c || "slate" in c   -> "Slate"
            "beige"  in c || "sand"  in c || "ecru"  in c   -> "Sand"
            "tan"    in c || "camel" in c || "stone" in c   -> "Stone"
            "brown"  in c || "mocha" in c || "chocolate" in c -> "Cocoa"
            "red"    in c || "scarlet" in c                  -> "Scarlet"
            "crimson" in c || "burgundy" in c || "maroon" in c || "wine" in c -> "Bordeaux"
            "orange" in c || "rust" in c || "terracotta" in c -> "Rust"
            "coral"  in c || "peach" in c                    -> "Peach"
            "yellow" in c || "mustard" in c                  -> "Saffron"
            "gold"   in c                                    -> "Gold"
            "green"  in c || "olive" in c || "sage" in c    -> "Sage"
            "emerald" in c || "forest" in c                  -> "Forest"
            "mint"   in c || "teal" in c || "turquoise" in c  -> "Jade"
            "blue"   in c || "cobalt" in c || "sky" in c    -> "Azure"
            "denim"  in c                                    -> "Denim"
            "purple" in c || "plum" in c || "violet" in c   -> "Plum"
            "lavender" in c || "lilac" in c                  -> "Lavender"
            "pink"   in c || "blush" in c || "rose" in c    -> "Rose"
            "magenta" in c || "fuchsia" in c                 -> "Fuchsia"
            else -> colorStr.replaceFirstChar(Char::titlecase).ifBlank { "Neutral" }
        }
    }

    /** Gets a material/fabric hint from item tags. */
    private fun materialHint(items: List<ClothingItem>): String? {
        val fabricWords = listOf("linen", "denim", "silk", "knit", "leather", "cotton",
            "chiffon", "satin", "velvet", "wool", "cashmere", "suede", "flannel")
        return items.flatMap { it.tags }.firstOrNull { tag ->
            fabricWords.any { fab -> tag.lowercase().contains(fab) }
        }?.lowercase()?.replaceFirstChar(Char::titlecase)
    }

    private fun buildOutfitName(
        items: List<ClothingItem>, style: String, occasion: String, trend: Trend
    ): String {
        val keyItem  = items.firstOrNull { slotOf(it) == Slot.TOP || slotOf(it) == Slot.FULL }
            ?: items.first()
        val keyColor = colorEditorial(getItemColor(keyItem))   // editorial adjective
        val fabric   = materialHint(items)
        val cats     = items.map { it.category.lowercase() }
        val hasBlazer = cats.any { "blazer" in it }
        val hasJacket = cats.any { "jacket" in it || "coat" in it }
        val hasBoots  = cats.any { "boot" in it }
        val hasKurta  = cats.any { "kurta" in it || "kurti" in it }
        val hasLehenga = cats.any { "lehenga" in it }
        val hasSaree  = cats.any { "saree" in it }
        val hasSherwani = cats.any { "sherwani" in it }

        // Trend-aware names — specific and editorial
        val trendName: String? = when (trend) {
            Trend.MONOCHROME      -> when {
                keyColor == "Noir"  -> "All Black Everything"
                keyColor == "Blanc" -> "Clean White Edit"
                keyColor == "Slate" -> "Grey Zone"
                else                -> "$keyColor Head-to-Toe"
            }
            Trend.TONAL           -> "$keyColor Tonal Stack"
            Trend.COLOR_BLOCK     -> {
                val colors = items.map { colorEditorial(getItemColor(it)) }.distinct().take(2)
                if (colors.size >= 2) "${colors[0]} × ${colors[1]}" else "$keyColor Block"
            }
            Trend.EARTH_TONES     -> if (fabric != null) "The $fabric Earth Edit" else "Earthy Roots"
            Trend.PASTEL_POP      -> "$keyColor Soft Hour"
            Trend.POWER_DRESSING  -> if (hasBlazer) "Boardroom $keyColor" else "Power Shift"
            Trend.STREET_LUXE     -> "Elevated Street"
            Trend.ETHNIC_FUSION   -> "Heritage Modern"
            Trend.ATHLEISURE_CHIC -> "Move In Style"
            Trend.COASTAL_COOL    -> "Off-Duty $keyColor"
            Trend.NONE            -> null
        }
        if (trendName != null) return trendName

        // Occasion-specific names
        return when {
            occasion == "Work" && hasBlazer      -> "$keyColor Power Hour"
            occasion == "Work"                   -> "$keyColor Work Edit"
            occasion == "Date Night" && hasBoots -> "$keyColor Night Out"
            occasion == "Date Night"             -> "$keyColor After Dark"
            occasion == "Gym"                    -> "The Active Set"
            occasion == "Beach"  && fabric != null -> "$fabric Beach Day"
            occasion == "Beach"                  -> "$keyColor Shore Side"
            occasion == "Festival" && hasLehenga -> "$keyColor Festival Drape"
            occasion == "Festival"               -> "$keyColor Festival Edit"
            occasion == "Wedding" && hasLehenga  -> "The $keyColor Lehenga Look"
            occasion == "Wedding" && hasSaree    -> "The $keyColor Saree Edit"
            occasion == "Wedding" && hasSherwani -> "$keyColor Sherwani Moment"
            occasion == "Wedding"                -> "$keyColor Occasion Wear"
            style == "Ethnic" && hasKurta        -> "$keyColor Ethnic Story"
            style == "Ethnic"                    -> "Ethnic $keyColor"
            style == "Formal" && hasBlazer       -> "The $keyColor Suit"
            style == "Formal"                    -> "$keyColor Formal"
            style == "Minimalist" && fabric != null -> "The $fabric Minimal"
            style == "Minimalist"                -> "$keyColor Pared Back"
            style == "Streetwear" && hasJacket   -> "$keyColor Street Layer"
            style == "Streetwear"                -> "$keyColor Street Wear"
            style == "Bohemian"                  -> "$keyColor Free Spirit"
            style == "Classic"                   -> "The $keyColor Classic"
            style == "Smart Casual"              -> "$keyColor Smart Hour"
            style == "Athleisure"                -> "Move Easy"
            fabric != null                       -> "The $fabric $keyColor"
            else                                 -> "$keyColor Edit"
        }
    }

    // ── Smart color story ─────────────────────────────────────────────────────

    private fun buildColorStory(items: List<ClothingItem>, harmonyScore: Float): String {
        val colors = items.map { colorLabel(getItemColor(it)) }.distinct().take(3)
        if (colors.isEmpty() || colors.all { it == "Neutral" || it == "Unknown" }) {
            return "A clean, versatile palette that works across seasons."
        }
        val palette = colors.joinToString(" × ")
        return when {
            harmonyScore > 0.88f ->
                "$palette — a perfectly balanced palette. Zero effort, maximum impact."
            harmonyScore > 0.75f ->
                "$palette — a deliberate, harmonious combination with natural visual flow."
            harmonyScore > 0.60f ->
                "$palette — complementary tones that create a polished contrast."
            else ->
                "$palette — a bold, high-contrast statement that stands out."
        }
    }

    // ── Smart style note ──────────────────────────────────────────────────────

    private fun buildStyleNote(items: List<ClothingItem>, style: String, trend: Trend): String {
        val itemNames = items.map { it.category }.take(3)
        val colorNames = items.map { colorLabel(getItemColor(it)) }.distinct().take(2)
        val palette = colorNames.joinToString(" and ")

        return when (trend) {
            Trend.MONOCHROME ->
                "A head-to-toe $palette monochrome look — one of the strongest signals in current fashion."
            Trend.TONAL ->
                "Tonal dressing at its best: layering $palette shades for effortless depth."
            Trend.COLOR_BLOCK ->
                "Bold colour blocking with ${colorNames.getOrElse(0) { "contrasting" }} and ${colorNames.getOrElse(1) { "pieces" }} — a runway-ready technique."
            Trend.EARTH_TONES ->
                "Earth tones are dominating runways this season. Warm, grounded, and effortlessly chic."
            Trend.PASTEL_POP ->
                "Soft pastels for a dreamy, feminine edit — one of the standout trends this season."
            Trend.POWER_DRESSING ->
                "Sharp power dressing: structured ${itemNames.firstOrNull { "blazer" in it.lowercase() || "jacket" in it.lowercase() } ?: "jacket"} elevates the entire look."
            Trend.STREET_LUXE ->
                "Street meets luxury — the elevated casual aesthetic dominating global street style."
            Trend.ETHNIC_FUSION ->
                "Indo-western fusion: traditional meets contemporary for a modern cultural statement."
            Trend.ATHLEISURE_CHIC ->
                "Athleisure done right — sporty pieces styled with intention."
            Trend.COASTAL_COOL ->
                "Light fabrics, open silhouettes — effortlessly coastal and season-appropriate."
            Trend.NONE ->
                "A well-composed $style look: ${itemNames.take(2).joinToString(" + ")} in $palette."
        }
    }

    // ── Smart "why it works" ──────────────────────────────────────────────────

    private fun buildWhyItWorks(s: Scored, items: List<ClothingItem>, colorStory: String): String {
        val colors    = items.map { colorLabel(getItemColor(it)) }.distinct()
        val families  = colors.map { colorFamilyOf(it) }
        val neutralFamilies = setOf(ColorFamily.NEUTRAL_LIGHT, ColorFamily.NEUTRAL_DARK,
                                    ColorFamily.NEUTRAL_MID, ColorFamily.NEUTRAL_WARM)
        val allNeutral = families.all { it in neutralFamilies }

        val colorReason = when {
            s.color > 0.88f && allNeutral ->
                "The all-neutral palette is timeless — these tones never clash."
            s.color > 0.88f ->
                "${colors.take(2).joinToString(" and ")} sit in the same colour family, creating harmony."
            s.color > 0.72f ->
                "The ${colors.firstOrNull() ?: "colors"} act as a strong anchor, letting the other pieces support it."
            else ->
                "The contrast between ${colors.getOrElse(0) { "these" }} and ${colors.getOrElse(1) { "those" }} creates intentional visual tension."
        }

        val styleReason = when {
            s.style > 0.85f -> "Style codes align tightly — every piece speaks the same language."
            s.style > 0.65f -> "The style mix is intentional and reads as modern layering."
            else            -> "The varied styles give this look an eclectic, editorial quality."
        }

        val formality = items.flatMap { it.styleTypes }
        val formalCount  = formality.count { it in setOf("Formal", "Classic", "Smart Casual") }
        val casualCount  = formality.count { it in setOf("Casual", "Streetwear", "Athleisure") }
        val formalReason = when {
            formalCount > 0 && casualCount == 0 -> "Fully formal — dressed for impact."
            casualCount > 0 && formalCount == 0 -> "Relaxed and cohesive — great for off-duty dressing."
            formalCount > 0 && casualCount > 0  -> "Smart-casual balance — versatile enough to dress up or down."
            else -> ""
        }

        return listOf(colorReason, styleReason, formalReason)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    // ── Weather ───────────────────────────────────────────────────────────────

    private fun describeWeather(score: Float, season: String): String =
        if (season.isBlank()) "Works across seasons — layer up or down as needed."
        else if (score > 0.7f) "Well-suited for $season — the fabrics and layers work for the conditions."
        else "Adaptable for $season with a light layer adjustment."
}
