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

        val scored = candidates.map { combo ->
            val color = harmonyOfOutfit(combo)
            val style = styleConsistency(combo)
            val occ   = occasionFit(combo, occasion)
            val sea   = seasonFit(combo, season)
            val prof  = profileFit(combo, profile)
            val total = (color * 0.25f) + (style * 0.30f) + (occ * 0.20f) +
                        (sea * 0.15f) + (prof * 0.10f)
            Scored(combo, total, color, style, occ, sea, prof)
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

            Outfit(
                name         = "$dominantStyle $outfitOccasion",
                styleType    = dominantStyle,
                occasionType = outfitOccasion,
                items        = s.items,
                styleNote    = describeStyle(s.style, dominantStyle),
                weatherNote  = describeWeather(s.sea, season),
                colorStory   = describePalette(s.items, s.color),
                whyItWorks   = describeWhy(s),
                dateCreated  = System.currentTimeMillis()
            )
        }
    }

    // ── Descriptions ─────────────────────────────────────────────────────────
    private fun describePalette(items: List<ClothingItem>, harmonyScore: Float): String {
        val foundColors = items.flatMap { it.tags }
            .mapNotNull { tag -> colorMap.entries.firstOrNull { tag.lowercase().contains(it.key) }?.key }
            .distinct().take(2)
        if (foundColors.isEmpty()) return "A balanced everyday palette."
        val joined = foundColors.joinToString(" and ") { it.replaceFirstChar(Char::titlecase) }
        return when {
            harmonyScore > 0.85f -> "$joined — a quietly confident palette."
            harmonyScore > 0.70f -> "$joined — a deliberate, harmonious pairing."
            else                 -> "$joined — a bold contrast play."
        }
    }

    private fun describeStyle(score: Float, style: String): String = when {
        score > 0.85f -> "Consistent $style styling — every piece earns its place."
        score > 0.65f -> "$style direction with a confident, intentional feel."
        else          -> "A mixed-style play that reads modern and unexpected."
    }

    private fun describeWeather(score: Float, season: String): String =
        if (season.isBlank()) "Year-round versatility."
        else if (score > 0.7f) "Built for $season — fabrics and coverage suit the conditions."
        else "Works for $season with light layering."

    private fun describeWhy(s: Scored): String {
        val top = listOf(
            "color"    to s.color,
            "style"    to s.style,
            "occasion" to s.occ,
            "season"   to s.sea
        ).maxByOrNull { it.second }
        return when (top?.first) {
            "color"    -> "Strongest on color harmony — the palette pulls everything together."
            "style"    -> "Tight style consistency keeps it polished and intentional."
            "occasion" -> "Every piece is appropriate for the moment."
            "season"   -> "Seasonally coherent — wearable as a real outfit today."
            else       -> "A balanced match to your style preferences."
        }
    }
}
