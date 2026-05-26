package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class TFLiteClothingClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imgSize = 224
    private val interpreter: Interpreter by lazy { Interpreter(loadModel()) }
    private val labels: List<String> by lazy { loadLabels() }

    // ── Enriched metadata per TFLite base category ───────────────────────────

    private data class Meta(
        val tags: List<String>,
        val occasions: List<String>,
        val seasons: List<String>,
        val styles: List<String>,
        val mood: String = "Everyday",
        val weather: String = "Any"
    )

    private val categoryMeta = mapOf(
        "Dress" to Meta(
            tags      = listOf("dress", "feminine", "one-piece"),
            occasions = listOf("Casual", "Party", "Date Night", "Wedding"),
            seasons   = listOf("Spring", "Summer"),
            styles    = listOf("Feminine", "Classic", "Bohemian", "Minimalist"),
            mood      = "Elegant", weather = "Warm"
        ),
        "Hat" to Meta(
            tags      = listOf("hat", "headwear", "accessory"),
            occasions = listOf("Casual", "Outdoor", "Beach"),
            seasons   = listOf("Summer", "Winter"),
            styles    = listOf("Casual", "Streetwear", "Bohemian"),
            mood      = "Playful", weather = "Any"
        ),
        "Jacket" to Meta(
            tags      = listOf("jacket", "outerwear", "layer"),
            occasions = listOf("Casual", "Work", "Outdoor"),
            seasons   = listOf("Autumn", "Winter"),
            styles    = listOf("Smart Casual", "Streetwear", "Classic"),
            mood      = "Cool", weather = "Cold"
        ),
        "Jeans" to Meta(
            tags      = listOf("jeans", "denim", "bottoms"),
            occasions = listOf("Casual", "Outdoor", "Smart Casual"),
            seasons   = listOf("Spring", "Autumn", "Winter"),
            styles    = listOf("Casual", "Streetwear", "Classic"),
            mood      = "Relaxed", weather = "Cool"
        ),
        "Kurta" to Meta(
            tags      = listOf("kurta", "ethnic", "indian", "tunic"),
            occasions = listOf("Casual", "Festival", "Wedding", "Work"),
            seasons   = listOf("Spring", "Summer", "Autumn"),
            styles    = listOf("Ethnic", "Bohemian", "Smart Casual"),
            mood      = "Cultural", weather = "Warm"
        ),
        "Lehenga" to Meta(
            tags      = listOf("lehenga", "ethnic", "festive", "indian", "bridal"),
            occasions = listOf("Wedding", "Festival", "Party"),
            seasons   = listOf("Spring", "Summer", "Winter"),
            styles    = listOf("Ethnic", "Formal", "Feminine"),
            mood      = "Festive", weather = "Any"
        ),
        "Pants" to Meta(
            tags      = listOf("pants", "trousers", "bottoms", "formal"),
            occasions = listOf("Work", "Casual", "Formal", "Smart Casual"),
            seasons   = listOf("Spring", "Autumn", "Winter"),
            styles    = listOf("Classic", "Minimalist", "Formal"),
            mood      = "Professional", weather = "Cool"
        ),
        "Sandals" to Meta(
            tags      = listOf("sandals", "footwear", "open-toe"),
            occasions = listOf("Casual", "Beach", "Outdoor"),
            seasons   = listOf("Summer", "Spring"),
            styles    = listOf("Casual", "Bohemian", "Minimalist"),
            mood      = "Breezy", weather = "Warm"
        ),
        "Saree" to Meta(
            tags      = listOf("saree", "ethnic", "traditional", "indian", "silk"),
            occasions = listOf("Wedding", "Festival", "Formal", "Puja"),
            seasons   = listOf("Spring", "Summer", "Winter"),
            styles    = listOf("Ethnic", "Classic", "Formal"),
            mood      = "Graceful", weather = "Any"
        ),
        "Shirt" to Meta(
            tags      = listOf("shirt", "top", "formal", "button-down"),
            occasions = listOf("Work", "Formal", "Smart Casual"),
            seasons   = listOf("Spring", "Summer", "Autumn"),
            styles    = listOf("Classic", "Minimalist", "Formal"),
            mood      = "Polished", weather = "Mild"
        ),
        "Shoes" to Meta(
            tags      = listOf("shoes", "footwear", "closed-toe"),
            occasions = listOf("Work", "Casual", "Formal"),
            seasons   = listOf("Spring", "Autumn", "Winter"),
            styles    = listOf("Classic", "Minimalist", "Formal"),
            mood      = "Grounded", weather = "Cool"
        ),
        "Shorts" to Meta(
            tags      = listOf("shorts", "bottoms", "casual", "summer"),
            occasions = listOf("Casual", "Beach", "Outdoor", "Gym"),
            seasons   = listOf("Summer"),
            styles    = listOf("Casual", "Athleisure", "Streetwear"),
            mood      = "Casual", weather = "Hot"
        ),
        "Sweater" to Meta(
            tags      = listOf("sweater", "knit", "warm", "layer"),
            occasions = listOf("Casual", "Work", "Outdoor"),
            seasons   = listOf("Autumn", "Winter"),
            styles    = listOf("Classic", "Minimalist", "Cozy"),
            mood      = "Cozy", weather = "Cold"
        ),
        "T-Shirt" to Meta(
            tags      = listOf("t-shirt", "top", "casual", "everyday"),
            occasions = listOf("Casual", "Outdoor", "Gym"),
            seasons   = listOf("Spring", "Summer"),
            styles    = listOf("Casual", "Streetwear", "Athleisure"),
            mood      = "Casual", weather = "Warm"
        )
    )

    // ── Sub-category definitions ──────────────────────────────────────────────
    // Each entry: (subCategory name, tags override, occasions override, styles override)

    private data class SubMeta(
        val name: String,
        val extraTags: List<String> = emptyList(),
        val occasionsOverride: List<String>? = null,
        val stylesOverride: List<String>? = null,
        val mood: String? = null,
        val weather: String? = null
    )

    /**
     * Sub-categories for each TFLite label.
     * Selected by heuristics: dominant color, brightness, crop region, color variance.
     */
    private val subCategories: Map<String, List<SubMeta>> = mapOf(

        // ── Dress (16 sub-types) ─────────────────────────────────────────────
        "Dress" to listOf(
            SubMeta("Maxi Dress",       extraTags = listOf("floor-length", "flowy"),    occasionsOverride = listOf("Casual", "Beach", "Date Night"), mood = "Bohemian", weather = "Warm"),
            SubMeta("Mini Dress",       extraTags = listOf("short", "flirty"),           occasionsOverride = listOf("Party", "Date Night"),            mood = "Bold",    weather = "Warm"),
            SubMeta("Midi Dress",       extraTags = listOf("knee-length", "elegant"),    occasionsOverride = listOf("Work", "Casual", "Date Night"),    mood = "Chic",    weather = "Mild"),
            SubMeta("Wrap Dress",       extraTags = listOf("wrap", "v-neck"),            occasionsOverride = listOf("Date Night", "Work", "Casual"),    mood = "Feminine"),
            SubMeta("Bodycon Dress",    extraTags = listOf("fitted", "stretch"),          occasionsOverride = listOf("Party", "Date Night"),            mood = "Confident"),
            SubMeta("A-Line Dress",     extraTags = listOf("a-line", "flared-skirt"),    occasionsOverride = listOf("Casual", "Date Night", "Work"),    mood = "Classic"),
            SubMeta("Shirt Dress",      extraTags = listOf("shirt-style", "collared"),   occasionsOverride = listOf("Work", "Casual"),                  mood = "Smart", stylesOverride = listOf("Classic", "Minimalist")),
            SubMeta("Slip Dress",       extraTags = listOf("satin", "slip", "lingerie-inspired"), occasionsOverride = listOf("Date Night", "Party"), mood = "Sensual"),
            SubMeta("Sundress",         extraTags = listOf("summer", "light", "floral"), occasionsOverride = listOf("Casual", "Beach"),                 mood = "Breezy", weather = "Hot"),
            SubMeta("Kaftan",           extraTags = listOf("kaftan", "ethnic", "loose"), occasionsOverride = listOf("Casual", "Beach", "Festival"),    mood = "Relaxed", stylesOverride = listOf("Bohemian", "Ethnic")),
            SubMeta("Anarkali Suit",    extraTags = listOf("anarkali", "ethnic", "indian", "flared-kurta"), occasionsOverride = listOf("Festival", "Wedding", "Casual"), stylesOverride = listOf("Ethnic", "Feminine"), mood = "Cultural"),
            SubMeta("Sharara Set",      extraTags = listOf("sharara", "ethnic", "indian", "wide-leg"), occasionsOverride = listOf("Wedding", "Festival"), stylesOverride = listOf("Ethnic", "Formal"), mood = "Festive"),
            SubMeta("Palazzo Set",      extraTags = listOf("palazzo", "ethnic", "wide-leg", "co-ord"), occasionsOverride = listOf("Casual", "Festival"), stylesOverride = listOf("Ethnic", "Bohemian"), mood = "Flowy"),
            SubMeta("Co-ord Set",       extraTags = listOf("matching-set", "co-ord"),    occasionsOverride = listOf("Casual", "Party"),                 mood = "Trendy", stylesOverride = listOf("Streetwear", "Minimalist")),
            SubMeta("Fit & Flare",      extraTags = listOf("fitted-top", "flared-skirt"), occasionsOverride = listOf("Party", "Date Night", "Casual"), mood = "Playful"),
            SubMeta("Off-Shoulder Dress", extraTags = listOf("off-shoulder", "strapless"), occasionsOverride = listOf("Party", "Date Night"),          mood = "Romantic")
        ),

        // ── Jacket (12 sub-types) ────────────────────────────────────────────
        "Jacket" to listOf(
            SubMeta("Denim Jacket",     extraTags = listOf("denim", "blue", "casual"),  stylesOverride = listOf("Casual", "Streetwear"),  mood = "Cool",       weather = "Mild"),
            SubMeta("Leather Jacket",   extraTags = listOf("leather", "edgy"),          stylesOverride = listOf("Streetwear", "Rock"),     mood = "Edgy",       weather = "Cool"),
            SubMeta("Blazer",           extraTags = listOf("blazer", "structured", "formal"), stylesOverride = listOf("Formal", "Classic", "Smart Casual"), mood = "Professional", weather = "Mild", occasionsOverride = listOf("Work", "Formal", "Smart Casual")),
            SubMeta("Bomber Jacket",    extraTags = listOf("bomber", "sporty"),          stylesOverride = listOf("Streetwear", "Casual"),  mood = "Street",     weather = "Cool"),
            SubMeta("Windbreaker",      extraTags = listOf("windbreaker", "sporty", "nylon"), stylesOverride = listOf("Athleisure", "Streetwear"), mood = "Active", weather = "Windy", occasionsOverride = listOf("Outdoor", "Gym", "Casual")),
            SubMeta("Trench Coat",      extraTags = listOf("trench", "belted", "long"), stylesOverride = listOf("Classic", "Minimalist"), mood = "Sophisticated", weather = "Rainy"),
            SubMeta("Puffer Jacket",    extraTags = listOf("puffer", "quilted", "warm"), stylesOverride = listOf("Casual", "Athleisure"), mood = "Cozy",       weather = "Very Cold"),
            SubMeta("Hoodie",           extraTags = listOf("hoodie", "hooded", "fleece"), stylesOverride = listOf("Casual", "Streetwear", "Athleisure"), mood = "Relaxed", weather = "Cool"),
            SubMeta("Cardigan",         extraTags = listOf("cardigan", "knit", "open-front"), stylesOverride = listOf("Casual", "Classic", "Minimalist"), mood = "Cozy", weather = "Mild"),
            SubMeta("Shacket",          extraTags = listOf("shirt-jacket", "flannel", "overshirt"), stylesOverride = listOf("Casual", "Streetwear"), mood = "Laid-back"),
            SubMeta("Nehru Jacket",     extraTags = listOf("nehru", "ethnic", "mandarin-collar"), stylesOverride = listOf("Ethnic", "Smart Casual"), mood = "Cultural", occasionsOverride = listOf("Festival", "Wedding", "Work")),
            SubMeta("Bandhgala",        extraTags = listOf("bandhgala", "ethnic", "indian-formal"), stylesOverride = listOf("Ethnic", "Formal"), mood = "Regal", occasionsOverride = listOf("Wedding", "Formal", "Festival"))
        ),

        // ── Shirt (10 sub-types) ─────────────────────────────────────────────
        "Shirt" to listOf(
            SubMeta("Formal Shirt",     extraTags = listOf("formal", "collared", "button-down"), occasionsOverride = listOf("Work", "Formal"), stylesOverride = listOf("Formal", "Classic")),
            SubMeta("Oxford Shirt",     extraTags = listOf("oxford", "cotton", "smart"), occasionsOverride = listOf("Work", "Smart Casual"), stylesOverride = listOf("Classic", "Preppy")),
            SubMeta("Linen Shirt",      extraTags = listOf("linen", "breathable", "summer"), occasionsOverride = listOf("Casual", "Beach", "Smart Casual"), weather = "Hot", mood = "Breezy"),
            SubMeta("Flannel Shirt",    extraTags = listOf("flannel", "check", "plaid"), stylesOverride = listOf("Casual", "Streetwear"), weather = "Cold"),
            SubMeta("Oversized Shirt",  extraTags = listOf("oversized", "relaxed-fit"), stylesOverride = listOf("Streetwear", "Casual"), mood = "Trendy"),
            SubMeta("Crop Top",         extraTags = listOf("crop", "midriff", "short"), occasionsOverride = listOf("Casual", "Party", "Beach"), stylesOverride = listOf("Casual", "Streetwear"), mood = "Bold"),
            SubMeta("Tank Top",         extraTags = listOf("tank", "sleeveless", "summer"), occasionsOverride = listOf("Casual", "Gym", "Beach"), stylesOverride = listOf("Casual", "Athleisure"), weather = "Hot"),
            SubMeta("Polo Shirt",       extraTags = listOf("polo", "collar", "sporty"), occasionsOverride = listOf("Casual", "Smart Casual", "Outdoor"), stylesOverride = listOf("Classic", "Preppy")),
            SubMeta("Tunic",            extraTags = listOf("tunic", "long-top", "flowy"), occasionsOverride = listOf("Casual", "Work"), stylesOverride = listOf("Bohemian", "Minimalist")),
            SubMeta("Hawaiian Shirt",   extraTags = listOf("hawaiian", "tropical", "printed"), occasionsOverride = listOf("Beach", "Casual", "Festival"), stylesOverride = listOf("Casual", "Funky"), mood = "Fun", weather = "Hot")
        ),

        // ── T-Shirt (8 sub-types) ────────────────────────────────────────────
        "T-Shirt" to listOf(
            SubMeta("Graphic Tee",      extraTags = listOf("graphic", "print", "statement"), stylesOverride = listOf("Streetwear", "Casual")),
            SubMeta("Plain T-Shirt",    extraTags = listOf("plain", "basic", "essential"), stylesOverride = listOf("Minimalist", "Casual")),
            SubMeta("Oversized Tee",    extraTags = listOf("oversized", "baggy"), stylesOverride = listOf("Streetwear", "Casual")),
            SubMeta("Crop Tee",         extraTags = listOf("crop", "short"), occasionsOverride = listOf("Casual", "Gym", "Party"), stylesOverride = listOf("Streetwear", "Athleisure")),
            SubMeta("Striped Tee",      extraTags = listOf("striped", "nautical"), stylesOverride = listOf("Casual", "Classic")),
            SubMeta("Polo Tee",         extraTags = listOf("polo", "collared"), stylesOverride = listOf("Classic", "Preppy"), occasionsOverride = listOf("Casual", "Smart Casual")),
            SubMeta("Muscle Tee",       extraTags = listOf("sleeveless", "vest", "gym"), occasionsOverride = listOf("Gym", "Casual"), stylesOverride = listOf("Athleisure"), weather = "Hot"),
            SubMeta("Henley",           extraTags = listOf("henley", "button-neck"), stylesOverride = listOf("Casual", "Classic"))
        ),

        // ── Sweater (8 sub-types) ────────────────────────────────────────────
        "Sweater" to listOf(
            SubMeta("Crew Neck Sweater", extraTags = listOf("crew-neck", "knit"),         stylesOverride = listOf("Classic", "Minimalist")),
            SubMeta("V-Neck Sweater",    extraTags = listOf("v-neck", "smart"),            stylesOverride = listOf("Classic", "Smart Casual"), occasionsOverride = listOf("Work", "Casual")),
            SubMeta("Turtleneck",        extraTags = listOf("turtleneck", "high-neck"),    stylesOverride = listOf("Minimalist", "Classic"),    mood = "Sophisticated"),
            SubMeta("Hoodie",            extraTags = listOf("hoodie", "hooded", "casual"), stylesOverride = listOf("Streetwear", "Athleisure"), mood = "Relaxed"),
            SubMeta("Cable Knit",        extraTags = listOf("cable-knit", "textured"),     stylesOverride = listOf("Classic", "Preppy"),        mood = "Cozy"),
            SubMeta("Cardigan",          extraTags = listOf("cardigan", "open-front", "button"), stylesOverride = listOf("Classic", "Bohemian"), mood = "Cozy"),
            SubMeta("Sweatshirt",        extraTags = listOf("sweatshirt", "fleece"),       stylesOverride = listOf("Casual", "Streetwear"),    mood = "Relaxed"),
            SubMeta("Shrug",             extraTags = listOf("shrug", "cropped", "layer"),  stylesOverride = listOf("Bohemian", "Feminine"),    occasionsOverride = listOf("Casual", "Date Night"))
        ),

        // ── Pants (10 sub-types) ─────────────────────────────────────────────
        "Pants" to listOf(
            SubMeta("Trousers",         extraTags = listOf("formal-pants", "tailored"),   stylesOverride = listOf("Formal", "Classic"),       occasionsOverride = listOf("Work", "Formal")),
            SubMeta("Chinos",           extraTags = listOf("chinos", "cotton", "smart"),  stylesOverride = listOf("Smart Casual", "Classic"), occasionsOverride = listOf("Work", "Smart Casual", "Casual")),
            SubMeta("Cargo Pants",      extraTags = listOf("cargo", "utility", "pockets"), stylesOverride = listOf("Streetwear", "Casual"),   mood = "Utility"),
            SubMeta("Wide-Leg Pants",   extraTags = listOf("wide-leg", "flowy"),          stylesOverride = listOf("Bohemian", "Minimalist"),  mood = "Relaxed"),
            SubMeta("Joggers",          extraTags = listOf("joggers", "sporty", "elastic-waist"), stylesOverride = listOf("Athleisure", "Casual"), occasionsOverride = listOf("Gym", "Casual"), mood = "Active"),
            SubMeta("Palazzo",          extraTags = listOf("palazzo", "flowy", "ethnic"), stylesOverride = listOf("Ethnic", "Bohemian"),      occasionsOverride = listOf("Casual", "Festival")),
            SubMeta("Harem Pants",      extraTags = listOf("harem", "drop-crotch", "bohemian"), stylesOverride = listOf("Bohemian", "Ethnic"), mood = "Free-spirited"),
            SubMeta("Leggings",         extraTags = listOf("leggings", "stretch", "fitted"), stylesOverride = listOf("Athleisure", "Casual"), occasionsOverride = listOf("Gym", "Casual"), weather = "Cool"),
            SubMeta("Culottes",         extraTags = listOf("culottes", "mid-calf", "wide"), stylesOverride = listOf("Classic", "Minimalist"), occasionsOverride = listOf("Casual", "Work")),
            SubMeta("Salwar",           extraTags = listOf("salwar", "ethnic", "indian", "loose"), stylesOverride = listOf("Ethnic", "Casual"), occasionsOverride = listOf("Casual", "Festival", "Work"))
        ),

        // ── Jeans (7 sub-types) ──────────────────────────────────────────────
        "Jeans" to listOf(
            SubMeta("Slim Fit Jeans",   extraTags = listOf("slim", "fitted", "denim")),
            SubMeta("Straight Jeans",   extraTags = listOf("straight-cut", "classic-denim")),
            SubMeta("Bootcut Jeans",    extraTags = listOf("bootcut", "flared-bottom")),
            SubMeta("Skinny Jeans",     extraTags = listOf("skinny", "tight", "denim")),
            SubMeta("Wide-Leg Jeans",   extraTags = listOf("wide-leg", "baggy", "90s"), stylesOverride = listOf("Streetwear", "Casual")),
            SubMeta("Mom Jeans",        extraTags = listOf("mom-jeans", "high-waist", "relaxed"), stylesOverride = listOf("Casual", "Vintage")),
            SubMeta("Ripped Jeans",     extraTags = listOf("ripped", "distressed", "edgy"), stylesOverride = listOf("Streetwear", "Rock"))
        ),

        // ── Shorts (6 sub-types) ─────────────────────────────────────────────
        "Shorts" to listOf(
            SubMeta("Denim Shorts",     extraTags = listOf("denim", "cutoffs"),    stylesOverride = listOf("Casual", "Streetwear")),
            SubMeta("Bermuda Shorts",   extraTags = listOf("knee-length", "smart"), stylesOverride = listOf("Smart Casual", "Classic")),
            SubMeta("Cycling Shorts",   extraTags = listOf("cycling", "spandex"),  stylesOverride = listOf("Athleisure"), occasionsOverride = listOf("Gym", "Casual")),
            SubMeta("Cargo Shorts",     extraTags = listOf("cargo", "utility"),    stylesOverride = listOf("Casual", "Streetwear")),
            SubMeta("Board Shorts",     extraTags = listOf("swim", "beach", "water"), stylesOverride = listOf("Casual"), occasionsOverride = listOf("Beach", "Outdoor"), weather = "Hot"),
            SubMeta("Linen Shorts",     extraTags = listOf("linen", "breezy"),     stylesOverride = listOf("Casual", "Minimalist"), weather = "Hot")
        ),

        // ── Kurta (8 sub-types) ──────────────────────────────────────────────
        "Kurta" to listOf(
            SubMeta("Straight Kurta",   extraTags = listOf("straight-cut", "casual-ethnic"), stylesOverride = listOf("Ethnic", "Casual")),
            SubMeta("A-Line Kurta",     extraTags = listOf("a-line", "flared-hem"),           stylesOverride = listOf("Ethnic", "Feminine")),
            SubMeta("Anarkali Kurta",   extraTags = listOf("anarkali", "flared", "layered"),  stylesOverride = listOf("Ethnic", "Feminine"), occasionsOverride = listOf("Festival", "Wedding", "Casual"), mood = "Regal"),
            SubMeta("Pathani Kurta",    extraTags = listOf("pathani", "salwar", "mens-ethnic"), stylesOverride = listOf("Ethnic", "Classic"), occasionsOverride = listOf("Casual", "Festival")),
            SubMeta("Kurti",            extraTags = listOf("kurti", "short-kurta", "casual"), stylesOverride = listOf("Ethnic", "Casual")),
            SubMeta("Churidar Kurta",   extraTags = listOf("churidar", "fitted-bottom"),      stylesOverride = listOf("Ethnic", "Smart Casual"), occasionsOverride = listOf("Work", "Festival", "Casual")),
            SubMeta("Sherwani",         extraTags = listOf("sherwani", "groom", "formal-ethnic"), stylesOverride = listOf("Ethnic", "Formal"), occasionsOverride = listOf("Wedding", "Festival"), mood = "Majestic"),
            SubMeta("Indo-Western Kurta", extraTags = listOf("indo-western", "fusion"),       stylesOverride = listOf("Ethnic", "Smart Casual", "Minimalist"), occasionsOverride = listOf("Party", "Work", "Festival"))
        ),

        // ── Lehenga (6 sub-types) ────────────────────────────────────────────
        "Lehenga" to listOf(
            SubMeta("Bridal Lehenga",       extraTags = listOf("bridal", "heavy", "embroidered"),   occasionsOverride = listOf("Wedding"),             mood = "Bridal",    stylesOverride = listOf("Ethnic", "Formal")),
            SubMeta("Party Lehenga",        extraTags = listOf("party", "sequin", "festive"),        occasionsOverride = listOf("Festival", "Party"),    mood = "Festive",   stylesOverride = listOf("Ethnic", "Formal")),
            SubMeta("A-Line Lehenga",       extraTags = listOf("a-line", "flared", "princess"),      occasionsOverride = listOf("Wedding", "Festival")),
            SubMeta("Mermaid Lehenga",      extraTags = listOf("mermaid", "fitted", "flare-bottom"), occasionsOverride = listOf("Wedding", "Party")),
            SubMeta("Chaniya Choli",        extraTags = listOf("chaniya", "navratri", "garba"),       occasionsOverride = listOf("Festival"),            mood = "Festive",   stylesOverride = listOf("Ethnic", "Funky")),
            SubMeta("Ghagra",               extraTags = listOf("ghagra", "skirt", "folk"),            occasionsOverride = listOf("Festival", "Casual"),  mood = "Folksy",    stylesOverride = listOf("Ethnic", "Bohemian"))
        ),

        // ── Saree (7 sub-types) ──────────────────────────────────────────────
        "Saree" to listOf(
            SubMeta("Silk Saree",       extraTags = listOf("silk", "rich", "lustrous"),  occasionsOverride = listOf("Wedding", "Festival", "Formal"), mood = "Regal"),
            SubMeta("Cotton Saree",     extraTags = listOf("cotton", "handloom"),         occasionsOverride = listOf("Casual", "Work", "Puja"),        mood = "Earthy"),
            SubMeta("Georgette Saree",  extraTags = listOf("georgette", "flowy"),         occasionsOverride = listOf("Party", "Wedding"),              mood = "Elegant"),
            SubMeta("Chiffon Saree",    extraTags = listOf("chiffon", "light", "sheer"),  occasionsOverride = listOf("Casual", "Party"),               mood = "Breezy", weather = "Warm"),
            SubMeta("Banarasi Saree",   extraTags = listOf("banarasi", "brocade", "gold-zari"), occasionsOverride = listOf("Wedding", "Festival"),     mood = "Royal",     stylesOverride = listOf("Ethnic", "Formal")),
            SubMeta("Printed Saree",    extraTags = listOf("printed", "casual"),          occasionsOverride = listOf("Casual", "Work"),                mood = "Cheerful"),
            SubMeta("Pre-Draped Saree", extraTags = listOf("pre-draped", "easy", "modern"), occasionsOverride = listOf("Casual", "Party"),             mood = "Modern",    stylesOverride = listOf("Ethnic", "Minimalist"))
        ),

        // ── Shoes (9 sub-types) ──────────────────────────────────────────────
        "Shoes" to listOf(
            SubMeta("Sneakers",         extraTags = listOf("sneakers", "trainers", "sporty"),  stylesOverride = listOf("Casual", "Streetwear", "Athleisure")),
            SubMeta("Oxford Shoes",     extraTags = listOf("oxford", "formal", "lace-up"),    stylesOverride = listOf("Formal", "Classic"),       occasionsOverride = listOf("Work", "Formal")),
            SubMeta("Loafers",          extraTags = listOf("loafers", "slip-on", "smart"),    stylesOverride = listOf("Smart Casual", "Classic"), occasionsOverride = listOf("Work", "Casual")),
            SubMeta("Chelsea Boots",    extraTags = listOf("chelsea", "boots", "ankle"),      stylesOverride = listOf("Classic", "Streetwear"),   weather = "Cold"),
            SubMeta("High Heels",       extraTags = listOf("heels", "stiletto", "elevated"),  stylesOverride = listOf("Formal", "Feminine"),      occasionsOverride = listOf("Party", "Date Night", "Formal"), mood = "Confident"),
            SubMeta("Block Heels",      extraTags = listOf("block-heel", "comfortable-heel"), stylesOverride = listOf("Classic", "Feminine"),     occasionsOverride = listOf("Work", "Date Night")),
            SubMeta("Platform Shoes",   extraTags = listOf("platform", "chunky"),             stylesOverride = listOf("Streetwear", "Funky")),
            SubMeta("Slip-On Shoes",    extraTags = listOf("slip-on", "easy"),                stylesOverride = listOf("Casual", "Minimalist")),
            SubMeta("Sports Shoes",     extraTags = listOf("running", "athletic", "gym"),     stylesOverride = listOf("Athleisure"),              occasionsOverride = listOf("Gym", "Outdoor"), mood = "Active")
        ),

        // ── Sandals (6 sub-types) ────────────────────────────────────────────
        "Sandals" to listOf(
            SubMeta("Flat Sandals",     extraTags = listOf("flat", "casual-sandals"),   stylesOverride = listOf("Casual", "Minimalist")),
            SubMeta("Heeled Sandals",   extraTags = listOf("heeled-sandals", "dressy"), stylesOverride = listOf("Formal", "Feminine"),   occasionsOverride = listOf("Date Night", "Party")),
            SubMeta("Gladiator Sandals",extraTags = listOf("gladiator", "strappy"),     stylesOverride = listOf("Bohemian", "Casual"),   mood = "Bold"),
            SubMeta("Kolhapuri",        extraTags = listOf("kolhapuri", "ethnic", "handcrafted"), stylesOverride = listOf("Ethnic", "Casual"), occasionsOverride = listOf("Casual", "Festival")),
            SubMeta("Juttis",           extraTags = listOf("jutti", "ethnic-shoes", "embroidered"), stylesOverride = listOf("Ethnic", "Classic"), occasionsOverride = listOf("Festival", "Wedding")),
            SubMeta("Slides",           extraTags = listOf("slides", "slip-on", "pool"), stylesOverride = listOf("Casual", "Athleisure"), weather = "Hot")
        ),

        // ── Hat (6 sub-types) ────────────────────────────────────────────────
        "Hat" to listOf(
            SubMeta("Baseball Cap",     extraTags = listOf("cap", "casual"),            stylesOverride = listOf("Streetwear", "Casual")),
            SubMeta("Bucket Hat",       extraTags = listOf("bucket-hat", "trendy"),     stylesOverride = listOf("Streetwear", "Casual"),   mood = "Playful"),
            SubMeta("Beanie",           extraTags = listOf("beanie", "knit-hat"),       stylesOverride = listOf("Casual", "Streetwear"),   weather = "Cold"),
            SubMeta("Straw Hat",        extraTags = listOf("straw", "sun-hat", "beach"), stylesOverride = listOf("Bohemian", "Casual"),   weather = "Hot",  occasionsOverride = listOf("Beach", "Outdoor")),
            SubMeta("Fedora",           extraTags = listOf("fedora", "brim"),           stylesOverride = listOf("Classic", "Bohemian"),   mood = "Stylish"),
            SubMeta("Turban",           extraTags = listOf("turban", "ethnic", "headwrap"), stylesOverride = listOf("Ethnic", "Bohemian"), occasionsOverride = listOf("Festival", "Casual"))
        )
    )

    // ── Sub-category inference ────────────────────────────────────────────────

    /**
     * Picks the best sub-category using image signals:
     *  - dominantColor name for color matching
     *  - cropRegion ("top" / "bottom" / "full") for garment position hints
     *  - brightness (dark = formal/winter, bright = casual/summer)
     *  - colorVariance (high = printed/patterned, low = plain/solid)
     */

    /** Collapses verbose color names to canonical single-word equivalents so tag matching works. */
    private fun normalizeColor(raw: String): String {
        val r = raw.lowercase().trim()
        return when {
            r.contains("navy") || r.contains("indigo")                               -> "navy"
            r.contains("maroon") || r.contains("burgundy") || r.contains("wine")
                                  || r.contains("crimson")                           -> "red"
            r.contains("forest") || r.contains("olive") || r.contains("khaki")      -> "green"
            r.contains("cream") || r.contains("ivory") || r.contains("off-white")
                                  || r.contains("off white")                         -> "white"
            r.contains("sky") || r.contains("powder blue")                          -> "blue"
            r.contains("coral") || r.contains("salmon") || r.contains("terracotta") -> "orange"
            r.contains("lavender") || r.contains("lilac") || r.contains("violet")   -> "purple"
            r.contains("rose") || r.contains("blush")                               -> "pink"
            r.contains("charcoal") || r.contains("ash") || r.contains("slate")      -> "grey"
            r.contains("tan") || r.contains("camel") || r.contains("beige")         -> "beige"
            else -> raw.trim()
        }
    }

    private fun inferSubCategory(
        base: String,
        bitmap: Bitmap,
        dominantColor: String,
        cropRegion: String
    ): SubMeta? {
        val subs = subCategories[base] ?: return null
        if (subs.isEmpty()) return null

        val brightness   = computeBrightness(bitmap)     // 0..1
        val colorVar     = computeColorVariance(bitmap)   // 0..1
        val saturation   = computeSaturation(bitmap)      // 0..1
        val hue          = computeDominantHue(bitmap)     // named bucket
        val ar           = aspectRatio(bitmap)            // h/w ratio
        val edgeDensity  = computeEdgeDensity(bitmap)     // 0..1 (high=textured)
        val colorLower   = normalizeColor(dominantColor)

        val isTextured      = edgeDensity > 0.18f         // cable knit, denim, flannel, embroidery
        val isSmooth        = edgeDensity < 0.08f         // silk, satin, plain jersey, chiffon
        val isMediumTexture = edgeDensity in 0.08f..0.18f // plain jersey, light weave, non-printed shirt

        val isBlueish    = hue == "blue"  || colorLower.contains("blue") || colorLower.contains("denim") || colorLower.contains("indigo")
        val isEarthTone  = hue in listOf("orange","yellow") || colorLower.any { c -> "beige tan khaki camel brown cream".contains(c.toString()) }
        val isBoldColor  = saturation > 0.45f
        val isVibrant    = saturation > 0.55f && colorVar > 0.15f   // ethnic / festive
        val isDark       = brightness < 0.35f
        val isLight      = brightness > 0.6f
        val isTall       = ar > 1.4f   // floor-length / long garment
        val isMediumTall = ar in 1.1f..1.4f

        // Score each sub-category
        val scored = subs.map { sub ->
            var score = 0.0

            // Color keyword match in tags
            if (sub.extraTags.any { tag -> tag in colorLower || colorLower.contains(tag) }) score += 3.0
            // Hue match in extra tags
            if (sub.extraTags.any { tag -> tag == hue }) score += 1.5

            // Category-specific heuristics — now uses saturation, hue, ar as well
            when (base) {

                // ── Dress ────────────────────────────────────────────────────
                "Dress"   -> when {
                    sub.name == "Maxi Dress"         && (isTall || brightness < 0.5f) -> score += 2.5
                    sub.name == "Mini Dress"          && !isTall && brightness > 0.5f -> score += 1.5
                    sub.name == "Midi Dress"          && isMediumTall                  -> score += 2.0
                    sub.name == "Sundress"            && isLight && isVibrant           -> score += 2.5
                    sub.name == "Sundress"            && isLight                        -> score += 1.5
                    sub.name == "Bodycon Dress"       && colorVar < 0.12f && !isTall   -> score += 2.0
                    sub.name == "Kaftan"              && colorVar > 0.25f && saturation > 0.3f -> score += 2.0
                    sub.name == "Anarkali Suit"       && isVibrant                      -> score += 2.5
                    sub.name == "Anarkali Suit"       && hue in listOf("red","orange","yellow","purple","pink") -> score += 2.0
                    sub.name == "Wrap Dress"          && !isDark && colorVar < 0.2f    -> score += 1.5
                    sub.name == "Slip Dress"          && isDark && colorVar < 0.1f     -> score += 2.0
                    sub.name == "Sharara Set"         && isVibrant && isTall            -> score += 2.5
                    sub.name == "Palazzo Set"         && colorVar > 0.15f && !isTall   -> score += 1.5
                    sub.name == "Co-ord Set"          && colorVar < 0.15f && !isVibrant -> score += 1.5
                    sub.name == "Off-Shoulder Dress"  && isLight && saturation > 0.3f  -> score += 1.5
                    sub.name == "Fit & Flare"         && !isDark && isMediumTall        -> score += 1.5
                    sub.name == "A-Line Dress"        && !isTall && colorVar < 0.15f   -> score += 1.5
                    sub.name == "Shirt Dress"         && colorVar < 0.1f && isLight    -> score += 1.5
                }

                // ── Jacket ───────────────────────────────────────────────────
                "Jacket"  -> when {
                    sub.name == "Leather Jacket"  && isDark && colorVar < 0.08f        -> score += 3.0
                    sub.name == "Leather Jacket"  && isDark && hue == "neutral"        -> score += 1.5
                    sub.name == "Denim Jacket"    && isBlueish                         -> score += 3.5
                    sub.name == "Blazer"          && colorVar < 0.1f && saturation < 0.35f -> score += 2.5
                    sub.name == "Blazer"          && !isVibrant && !isBlueish           -> score += 1.0
                    sub.name == "Puffer Jacket"   && (brightness > 0.55f || isLight)   -> score += 2.0
                    sub.name == "Hoodie"          && colorVar < 0.12f && saturation < 0.4f -> score += 2.0
                    sub.name == "Trench Coat"     && isEarthTone                        -> score += 3.0
                    sub.name == "Cardigan"        && isLight && colorVar < 0.15f        -> score += 2.0
                    sub.name == "Bomber Jacket"   && !isDark && colorVar < 0.18f        -> score += 1.5
                    sub.name == "Windbreaker"     && isBoldColor && colorVar > 0.15f   -> score += 2.0
                    sub.name == "Shacket"         && colorVar > 0.15f && isEarthTone   -> score += 2.0
                    sub.name == "Nehru Jacket"    && saturation > 0.3f && !isBlueish   -> score += 1.5
                    sub.name == "Bandhgala"       && isDark && saturation < 0.3f       -> score += 2.0
                }

                // ── Shirt ────────────────────────────────────────────────────
                "Shirt"   -> when {
                    sub.name == "Formal Shirt"    && colorVar < 0.08f && saturation < 0.3f -> score += 2.5
                    sub.name == "Formal Shirt"    && isLight && colorVar < 0.1f         -> score += 1.5
                    sub.name == "Oxford Shirt"    && isLight && saturation < 0.35f      -> score += 2.0
                    sub.name == "Linen Shirt"     && isLight && saturation < 0.3f       -> score += 2.5
                    sub.name == "Flannel Shirt"   && colorVar > 0.2f && !isVibrant      -> score += 2.5
                    sub.name == "Oversized Shirt" && !isDark && ar < 1.1f               -> score += 1.5
                    sub.name == "Crop Top"        && cropRegion == "top" && isLight     -> score += 2.0
                    sub.name == "Tank Top"        && isLight && saturation < 0.4f       -> score += 1.5
                    sub.name == "Hawaiian Shirt"  && isVibrant && colorVar > 0.3f       -> score += 3.0
                    sub.name == "Polo Shirt"      && colorVar < 0.12f && isLight        -> score += 1.5
                    sub.name == "Tunic"           && isMediumTall && saturation < 0.4f  -> score += 1.5
                    sub.name == "Flannel Shirt"   && isTextured && colorVar > 0.15f           -> score += 2.0
                    sub.name == "Formal Shirt"    && isSmooth                                 -> score += 1.5
                    sub.name == "Linen Shirt"     && !isTextured && isLight                   -> score += 1.0
                    sub.name == "Formal Shirt"    && isMediumTexture && colorVar < 0.10f      -> score += 1.5
                    sub.name == "Flannel Shirt"   && isMediumTexture && colorVar > 0.12f      -> score += 2.0
                }

                // ── T-Shirt ──────────────────────────────────────────────────
                "T-Shirt" -> when {
                    sub.name == "Graphic Tee"     && colorVar > 0.2f                    -> score += 2.5
                    sub.name == "Graphic Tee"     && isVibrant                          -> score += 1.5
                    sub.name == "Plain T-Shirt"   && colorVar < 0.08f && saturation < 0.5f -> score += 2.5
                    sub.name == "Oversized Tee"   && isDark && colorVar < 0.12f         -> score += 2.0
                    sub.name == "Striped Tee"     && colorVar in 0.08f..0.22f            -> score += 2.0
                    sub.name == "Crop Tee"        && cropRegion == "top" && isLight     -> score += 2.0
                    sub.name == "Muscle Tee"      && saturation < 0.3f && colorVar < 0.1f -> score += 1.0
                    sub.name == "Polo Tee"        && isLight && colorVar < 0.1f         -> score += 1.5
                    sub.name == "Henley"          && colorVar < 0.15f && !isDark        -> score += 1.5
                    sub.name == "Graphic Tee"     && isTextured                               -> score += 1.5
                    sub.name == "Plain T-Shirt"   && isSmooth                                 -> score += 1.5
                    sub.name == "Plain T-Shirt"   && isMediumTexture && colorVar < 0.12f      -> score += 1.5
                }

                // ── Sweater ──────────────────────────────────────────────────
                "Sweater" -> when {
                    sub.name == "Turtleneck"       && isDark && colorVar < 0.12f         -> score += 3.0
                    sub.name == "Turtleneck"       && brightness < 0.45f                 -> score += 1.5
                    sub.name == "Cable Knit"       && colorVar in 0.08f..0.18f && isLight -> score += 2.0
                    sub.name == "Cable Knit"       && isEarthTone                         -> score += 1.5
                    sub.name == "Crew Neck Sweater"&& colorVar < 0.12f && saturation < 0.5f -> score += 2.0
                    sub.name == "V-Neck Sweater"   && isLight && saturation < 0.4f       -> score += 1.5
                    sub.name == "Hoodie"           && colorVar < 0.1f && saturation < 0.4f -> score += 2.0
                    sub.name == "Cardigan"         && isLight && colorVar < 0.15f         -> score += 2.0
                    sub.name == "Sweatshirt"       && isDark && colorVar < 0.12f          -> score += 2.0
                    sub.name == "Shrug"            && isLight && saturation > 0.25f       -> score += 1.5
                    sub.name == "Cable Knit"       && isTextured                              -> score += 2.5
                    sub.name == "Turtleneck"       && isSmooth && saturation < 0.35f          -> score += 1.5
                    sub.name == "Sweatshirt"       && !isTextured && isDark                   -> score += 1.0
                }

                // ── Pants ────────────────────────────────────────────────────
                "Pants"   -> when {
                    sub.name == "Trousers"        && colorVar < 0.07f && saturation < 0.35f -> score += 2.5
                    sub.name == "Trousers"        && isDark && hue in listOf("neutral","blue") -> score += 1.5
                    sub.name == "Chinos"          && isEarthTone && colorVar < 0.1f       -> score += 2.5
                    sub.name == "Cargo Pants"     && (colorLower.contains("khaki") || colorLower.contains("olive") || hue == "green") -> score += 3.0
                    sub.name == "Cargo Pants"     && colorVar > 0.1f && saturation < 0.4f -> score += 1.5
                    sub.name == "Joggers"         && colorVar < 0.1f && saturation < 0.35f -> score += 2.0
                    sub.name == "Wide-Leg Pants"  && isTall && colorVar > 0.1f             -> score += 2.0
                    sub.name == "Palazzo"         && isVibrant                             -> score += 2.5
                    sub.name == "Harem Pants"     && isVibrant && isTall                   -> score += 2.0
                    sub.name == "Leggings"        && isDark && colorVar < 0.1f             -> score += 2.5
                    sub.name == "Culottes"        && isMediumTall && !isVibrant            -> score += 1.5
                    sub.name == "Salwar"          && saturation > 0.25f && colorVar > 0.1f -> score += 2.0
                    sub.name == "Trousers"        && isMediumTexture && colorVar < 0.10f      -> score += 1.5
                }

                // ── Jeans ────────────────────────────────────────────────────
                "Jeans"   -> when {
                    sub.name == "Skinny Jeans"    && isDark && isBlueish                  -> score += 2.5
                    sub.name == "Skinny Jeans"    && isDark && colorVar < 0.12f           -> score += 1.5
                    sub.name == "Straight Jeans"  && !isDark && !isLight && isBlueish     -> score += 2.0
                    sub.name == "Straight Jeans"  && colorVar < 0.12f                     -> score += 1.0
                    sub.name == "Wide-Leg Jeans"  && isLight && isBlueish                 -> score += 2.5
                    sub.name == "Wide-Leg Jeans"  && colorVar > 0.12f                     -> score += 1.5
                    sub.name == "Mom Jeans"       && isLight && colorVar < 0.15f          -> score += 2.5
                    sub.name == "Ripped Jeans"    && colorVar > 0.15f                     -> score += 2.5
                    sub.name == "Bootcut Jeans"   && isDark && colorVar < 0.12f           -> score += 1.5
                    sub.name == "Ripped Jeans"    && isTextured                               -> score += 2.0
                    sub.name == "Skinny Jeans"    && !isTextured                              -> score += 1.0
                    sub.name == "Straight Jeans"  && isMediumTexture && isBlueish            -> score += 1.5
                }

                // ── Shorts ───────────────────────────────────────────────────
                "Shorts"  -> when {
                    sub.name == "Denim Shorts"    && isBlueish                             -> score += 3.0
                    sub.name == "Bermuda Shorts"  && isEarthTone && colorVar < 0.15f       -> score += 2.0
                    sub.name == "Cycling Shorts"  && isDark && colorVar < 0.08f            -> score += 2.5
                    sub.name == "Cargo Shorts"    && isEarthTone && colorVar > 0.08f       -> score += 2.0
                    sub.name == "Board Shorts"    && isVibrant                             -> score += 2.5
                    sub.name == "Linen Shorts"    && isLight && saturation < 0.35f         -> score += 2.0
                }

                // ── Kurta ────────────────────────────────────────────────────
                "Kurta"   -> when {
                    sub.name == "Anarkali Kurta"  && isVibrant && isTall                   -> score += 3.0
                    sub.name == "Anarkali Kurta"  && hue in listOf("red","orange","purple","pink") -> score += 2.0
                    sub.name == "Straight Kurta"  && colorVar < 0.1f && saturation < 0.45f -> score += 2.5
                    sub.name == "A-Line Kurta"    && isMediumTall && !isVibrant            -> score += 2.0
                    sub.name == "Sherwani"        && isDark && saturation < 0.35f          -> score += 3.0
                    sub.name == "Sherwani"        && hue in listOf("red","blue","neutral") && isTall -> score += 2.0
                    sub.name == "Kurti"           && isLight && !isTall                    -> score += 2.5
                    sub.name == "Churidar Kurta"  && colorVar < 0.12f && isTall            -> score += 1.5
                    sub.name == "Pathani Kurta"   && isLight && saturation < 0.4f          -> score += 1.5
                    sub.name == "Indo-Western Kurta" && !isVibrant && colorVar < 0.15f     -> score += 1.5
                }

                // ── Lehenga ──────────────────────────────────────────────────
                "Lehenga" -> when {
                    sub.name == "Bridal Lehenga"  && isVibrant && hue in listOf("red","orange","pink") -> score += 3.5
                    sub.name == "Bridal Lehenga"  && colorVar > 0.25f && isTall            -> score += 2.0
                    sub.name == "Chaniya Choli"   && isVibrant && colorVar > 0.3f          -> score += 3.0
                    sub.name == "Chaniya Choli"   && hue in listOf("red","orange","yellow") -> score += 2.0
                    sub.name == "Party Lehenga"   && isDark && saturation > 0.35f          -> score += 2.5
                    sub.name == "Party Lehenga"   && hue in listOf("blue","purple","green") && isBoldColor -> score += 2.0
                    sub.name == "A-Line Lehenga"  && !isVibrant && isTall                  -> score += 2.0
                    sub.name == "Mermaid Lehenga" && isDark && colorVar < 0.2f             -> score += 2.0
                    sub.name == "Ghagra"          && isVibrant && !isTall                  -> score += 2.0
                    sub.name == "Bridal Lehenga"  && isTextured                               -> score += 2.0
                    sub.name == "Chaniya Choli"   && isTextured && isVibrant                  -> score += 1.5
                }

                // ── Saree ────────────────────────────────────────────────────
                "Saree"   -> when {
                    sub.name == "Silk Saree"      && isDark && saturation > 0.35f          -> score += 3.0
                    sub.name == "Silk Saree"      && hue in listOf("red","purple","blue","green") -> score += 2.0
                    sub.name == "Cotton Saree"    && isLight && saturation < 0.4f          -> score += 2.5
                    sub.name == "Banarasi Saree"  && colorVar > 0.2f && isDark             -> score += 3.0
                    sub.name == "Banarasi Saree"  && hue in listOf("red","orange","yellow") && isBoldColor -> score += 2.0
                    sub.name == "Georgette Saree" && isMediumTall && saturation > 0.3f     -> score += 2.0
                    sub.name == "Chiffon Saree"   && isLight && saturation > 0.25f         -> score += 2.5
                    sub.name == "Printed Saree"   && colorVar > 0.2f && !isDark            -> score += 2.5
                    sub.name == "Pre-Draped Saree"&& colorVar < 0.15f && saturation < 0.5f -> score += 1.5
                    sub.name == "Silk Saree"      && isSmooth                                 -> score += 2.5
                    sub.name == "Chiffon Saree"   && isSmooth && isLight                      -> score += 2.0
                    sub.name == "Banarasi Saree"  && isTextured                               -> score += 2.0
                    sub.name == "Cotton Saree"    && !isSmooth && !isTextured                 -> score += 1.5
                }

                // ── Shoes ────────────────────────────────────────────────────
                "Shoes"   -> when {
                    sub.name == "Sneakers"        && (isLight || colorVar > 0.15f)         -> score += 2.5
                    sub.name == "Sports Shoes"    && isVibrant && colorVar > 0.18f         -> score += 3.0
                    sub.name == "Oxford Shoes"    && isDark && colorVar < 0.08f            -> score += 3.0
                    sub.name == "Chelsea Boots"   && isDark && colorVar < 0.06f            -> score += 3.0
                    sub.name == "Loafers"         && !isDark && colorVar < 0.1f            -> score += 2.0
                    sub.name == "High Heels"      && isDark && saturation < 0.4f           -> score += 2.0
                    sub.name == "High Heels"      && hue in listOf("red","black","neutral") -> score += 1.5
                    sub.name == "Block Heels"     && isEarthTone && colorVar < 0.12f       -> score += 2.0
                    sub.name == "Platform Shoes"  && (isVibrant || colorVar > 0.15f)       -> score += 2.0
                    sub.name == "Slip-On Shoes"   && isLight && colorVar < 0.1f            -> score += 1.5
                }

                // ── Sandals ──────────────────────────────────────────────────
                "Sandals" -> when {
                    sub.name == "Flat Sandals"      && isLight && colorVar < 0.12f          -> score += 2.5
                    sub.name == "Heeled Sandals"    && isDark && colorVar < 0.1f            -> score += 2.5
                    sub.name == "Heeled Sandals"    && hue in listOf("red","neutral")        -> score += 1.5
                    sub.name == "Gladiator Sandals" && isEarthTone                           -> score += 2.5
                    sub.name == "Kolhapuri"         && isEarthTone && colorVar > 0.1f       -> score += 3.0
                    sub.name == "Juttis"            && isVibrant && colorVar > 0.15f        -> score += 3.0
                    sub.name == "Juttis"            && hue in listOf("red","orange","pink","purple") -> score += 2.0
                    sub.name == "Slides"            && isLight && colorVar < 0.08f           -> score += 2.0
                }

                // ── Hat ──────────────────────────────────────────────────────
                "Hat"     -> when {
                    sub.name == "Baseball Cap"    && colorVar < 0.15f && saturation < 0.5f -> score += 2.0
                    sub.name == "Bucket Hat"      && (isVibrant || isEarthTone)             -> score += 2.5
                    sub.name == "Beanie"          && isDark && colorVar < 0.12f             -> score += 3.0
                    sub.name == "Beanie"          && brightness < 0.45f                     -> score += 1.5
                    sub.name == "Straw Hat"       && isEarthTone && isLight                 -> score += 3.5
                    sub.name == "Fedora"          && isEarthTone                            -> score += 2.5
                    sub.name == "Fedora"          && isDark && colorVar < 0.1f              -> score += 2.0
                    sub.name == "Turban"          && isVibrant && colorVar > 0.1f           -> score += 3.0
                }
            }
            sub to score
        }

        val best = scored.maxByOrNull { it.second }
        // Only assign a sub-category when signals are strong enough (score ≥ 1.5).
        // Below that threshold return null so the caller falls back to the plain TFLite label.
        return if ((best?.second ?: 0.0) >= 1.5) best?.first else null
    }

    // ── Pixel analysis helpers ────────────────────────────────────────────────

    /** Average luminance of the bitmap (0=black, 1=white). */
    private fun computeBrightness(bitmap: Bitmap): Float {
        val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        var total = 0L
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8)  and 0xFF
            val b =  px         and 0xFF
            total += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        }
        return total / (pixels.size * 255f)
    }

    /** Normalized color variance — high for patterns/prints, low for solid colors. */
    private fun computeColorVariance(bitmap: Bitmap): Float {
        val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        val rVals = pixels.map { (it shr 16) and 0xFF }
        val gVals = pixels.map { (it shr 8)  and 0xFF }
        val bVals = pixels.map {  it         and 0xFF }
        fun variance(vals: List<Int>): Double {
            val mean = vals.average()
            return vals.sumOf { (it - mean) * (it - mean) } / vals.size
        }
        val totalVar = (variance(rVals) + variance(gVals) + variance(bVals)) / 3.0
        // Normalize: sqrt(variance) / 128  →  0..~1.4  →  clamp to 0..1
        return (sqrt(totalVar) / 128.0).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Average HSV saturation (0=grey/white/black, 1=fully saturated colour).
     * High saturation → ethnic, festive, printed, bold.
     * Low  saturation → minimal, formal, monochrome, denim (faded blue).
     */
    private fun computeSaturation(bitmap: Bitmap): Float {
        val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        val hsv = FloatArray(3)
        var total = 0.0
        for (px in pixels) {
            android.graphics.Color.RGBToHSV(
                (px shr 16) and 0xFF,
                (px shr 8)  and 0xFF,
                 px         and 0xFF,
                hsv
            )
            total += hsv[1]
        }
        return (total / pixels.size).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Dominant hue bucket (0–360 collapsed into named group).
     * Returns one of: "red", "orange", "yellow", "green", "blue", "purple", "pink", "neutral"
     */
    private fun computeDominantHue(bitmap: Bitmap): String {
        val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        val hsv  = FloatArray(3)
        val hueBuckets = IntArray(8)          // red,orange,yellow,green,blue,purple,pink,neutral
        for (px in pixels) {
            android.graphics.Color.RGBToHSV(
                (px shr 16) and 0xFF,
                (px shr 8)  and 0xFF,
                 px         and 0xFF,
                hsv
            )
            val s = hsv[1]; val v = hsv[2]; val h = hsv[0]
            if (s < 0.12f || v < 0.15f) {
                // Dark-red rescue: maroon/burgundy/wine pixels have low saturation but red hue
                if (s >= 0.05f && v >= 0.08f && (h < 25f || h >= 345f)) {
                    hueBuckets[0]++  // red
                } else {
                    hueBuckets[7]++  // neutral/grey
                }
                continue
            }
            when {
                h < 15  || h >= 345 -> hueBuckets[0]++  // red
                h < 40              -> hueBuckets[1]++  // orange
                h < 70              -> hueBuckets[2]++  // yellow
                h < 150             -> hueBuckets[3]++  // green
                h < 250             -> hueBuckets[4]++  // blue
                h < 290             -> hueBuckets[5]++  // purple
                h < 345             -> hueBuckets[6]++  // pink
            }
        }
        val names = listOf("red","orange","yellow","green","blue","purple","pink","neutral")
        return names[hueBuckets.indices.maxByOrNull { hueBuckets[it] } ?: 7]
    }

    /**
     * Aspect ratio of the bitmap (height / width).
     * >1.8  → very tall  (floor-length dress, lehenga, saree drape)
     * >1.2  → tall       (kurta, maxi dress, trousers)
     * ~1.0  → square     (top, shirt, jacket, t-shirt)
     * <0.7  → wide       (wide-leg pants, shorts, hat in overhead shot)
     */
    private fun aspectRatio(bitmap: Bitmap): Float = bitmap.height.toFloat() / bitmap.width.toFloat()

    /**
     * Edge density via simple finite-difference gradient on a 32×32 luminance thumbnail.
     * High  → textured fabrics: cable knit, denim weave, flannel plaid, bridal embroidery, graphic prints
     * Low   → smooth fabrics: silk, satin, plain jersey, chiffon, formal trousers
     */
    private fun computeEdgeDensity(bitmap: Bitmap): Float {
        val s = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        s.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        val lum = DoubleArray(32 * 32) { i ->
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8)  and 0xFF
            val b =  px         and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        }
        var edgeSum = 0.0
        for (y in 0 until 31) {
            for (x in 0 until 31) {
                val gx = lum[y * 32 + (x + 1)] - lum[y * 32 + x]
                val gy = lum[(y + 1) * 32 + x] - lum[y * 32 + x]
                edgeSum += sqrt(gx * gx + gy * gy)
            }
        }
        // Max possible: 31*31 steps each up to sqrt(2) ≈ 1.414
        return (edgeSum / (31 * 31 * 1.414)).toFloat().coerceIn(0f, 1f)
    }

    // ── Pre-validation helper ─────────────────────────────────────────────────

    /**
     * Returns the raw TFLite top-1 confidence score for the full image without
     * applying any threshold.  Used by [ImagePreValidator] to detect "person too far"
     * before the multi-crop pipeline runs.
     */
    fun getMaxConfidence(uri: Uri): Float {
        val bitmap = loadBitmap(uri) ?: return 0f
        val scaled = Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, true)
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(bitmapToByteBuffer(scaled), output)
        return output[0].maxOrNull() ?: 0f
    }

    // ── Single-item classify ─────────────────────────────────────────────────

    fun classify(imageUri: Uri): ImageAnalysisResult {
        val bitmap = loadBitmap(imageUri) ?: return noClothingResult()
        return classifyBitmap(bitmap, "full") ?: noClothingResult()
    }

    // ── Multi-item detect via crops ──────────────────────────────────────────

    fun detectAll(imageUri: Uri): List<ImageAnalysisResult> {
        val bitmap = loadBitmap(imageUri) ?: run {
            Log.e("TFLite", "detectAll: failed to load bitmap from $imageUri")
            return emptyList()
        }
        val w = bitmap.width; val h = bitmap.height
        Log.d("TFLite", "detectAll: image ${w}x${h}")

        val cropNames = listOf("full", "top", "bottom", "top-left", "top-right", "bottom-left", "bottom-right")
        val crops = listOf(
            bitmap,
            Bitmap.createBitmap(bitmap, 0, 0, w, h / 2),
            Bitmap.createBitmap(bitmap, 0, h / 2, w, h / 2),
            Bitmap.createBitmap(bitmap, 0, 0, w / 2, h / 2),
            Bitmap.createBitmap(bitmap, w / 2, 0, w / 2, h / 2),
            Bitmap.createBitmap(bitmap, 0, h / 2, w / 2, h / 2),
            Bitmap.createBitmap(bitmap, w / 2, h / 2, w / 2, h / 2)
        )

        // Categories that are complete outfits by themselves.
        // If one is detected in the FULL-image crop, we suppress any tops/bottoms/bottoms
        // from partial crops — they are just fragments of the same garment.
        val fullBodyCategories  = setOf("Dress", "Lehenga", "Saree", "Kurta")
        // T-Shirt/Shirt are false positives from the torso region of an ethnic outfit
        val topCategories       = setOf("T-Shirt", "Shirt")
        // Pants/Jeans/Shorts are false positives from the hem region of a Lehenga/Dress/Saree
        val bottomCategories    = setOf("Pants", "Jeans", "Shorts")

        val seen = mutableSetOf<String>()
        val results = mutableListOf<ImageAnalysisResult>()
        var fullBodyDetected = false   // set true when FULL crop yields a full-body garment

        for ((idx, crop) in crops.withIndex()) {
            val cropName   = cropNames[idx]
            val isFullCrop = cropName == "full"
            val cropRegion = when {
                cropName.contains("top")    -> "top"
                cropName.contains("bottom") -> "bottom"
                else                        -> "full"
            }
            // Full image: strict 40% threshold. Partial crops: lenient 28% —
            // a bottom-half crop showing just jeans will naturally score lower.
            val threshold = if (isFullCrop) 0.40f else 0.28f
            val result = classifyBitmap(crop, cropRegion, threshold)
            if (result == null) {
                Log.d("TFLite", "  crop[$cropName] → below threshold / null")
                continue
            }

            // Track whether the full image is a complete outfit
            if (isFullCrop && result.isClothing && result.category in fullBodyCategories) {
                fullBodyDetected = true
                Log.d("TFLite", "  crop[$cropName] → full-body garment detected: ${result.category}. Partial-crop tops/bottoms will be suppressed.")
            }

            // Suppress tops AND bottoms detected from partial crops when a full-body garment found
            if (!isFullCrop && fullBodyDetected &&
                (result.category in topCategories || result.category in bottomCategories)) {
                Log.d("TFLite", "  crop[$cropName] → SUPPRESSED ${result.category} (partial crop of full-body garment)")
                continue
            }

            val displayName = result.subCategory ?: result.category
            Log.d("TFLite", "  crop[$cropName] → ${result.debugReason} isClothing=${result.isClothing}")
            if (result.isClothing && seen.add(result.category)) {
                Log.d("TFLite", "  → ADDED ${result.category} as \"$displayName\"")
                results.add(result)
            } else if (result.isClothing) {
                Log.d("TFLite", "  → DUPLICATE ${result.category}, skipped")
            }
        }

        Log.d("TFLite", "detectAll: found ${results.size} item(s): ${results.map { it.subCategory ?: it.category }}")
        return results
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun classifyBitmap(bitmap: Bitmap, cropRegion: String,
                               confidenceThreshold: Float = 0.40f): ImageAnalysisResult? {
        val scaled = Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, true)
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(bitmapToByteBuffer(scaled), output)

        val scores = output[0]
        // Log top-3 predictions
        val top3 = scores.indices.sortedByDescending { scores[it] }.take(3)
        val top3Str = top3.joinToString(", ") { "${labels[it]}=${(scores[it]*100).toInt()}%" }
        Log.v("TFLite", "    scores top3: $top3Str")

        val topIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val confidence = scores[topIdx]
        if (confidence < confidenceThreshold) return null

        val label = labels[topIdx]
        val baseMeta = categoryMeta[label] ?: Meta(
            tags = listOf(label.lowercase()), occasions = listOf("Casual"),
            seasons = listOf("Spring", "Summer", "Autumn", "Winter"),
            styles = listOf("Casual")
        )

        // Dominant color from this bitmap
        val dominantColor = DominantColorExtractor.extractFromBitmap(bitmap)

        // Infer sub-category using image signals.
        // Only attempt refinement when confidence is ≥ 50%; below that the base label is more reliable.
        val subMeta = if (confidence >= 0.50f) inferSubCategory(label, bitmap, dominantColor, cropRegion) else null
        val displayCategory = subMeta?.name ?: label

        val finalTags     = (baseMeta.tags + (subMeta?.extraTags ?: emptyList()) + listOf(dominantColor.lowercase())).distinct()
        val finalOcc      = subMeta?.occasionsOverride ?: baseMeta.occasions
        val finalStyles   = subMeta?.stylesOverride    ?: baseMeta.styles
        val finalMood     = subMeta?.mood              ?: baseMeta.mood
        val finalWeather  = subMeta?.weather           ?: baseMeta.weather

        return ImageAnalysisResult(
            isClothing  = true,
            category    = label,             // raw TFLite label
            subCategory = displayCategory,   // refined display name
            tags        = finalTags,
            occasions   = finalOcc,
            seasons     = baseMeta.seasons,
            styleTypes  = finalStyles,
            mood        = finalMood,
            weather     = finalWeather,
            debugReason = "TFLite: $label @ ${(confidence * 100).toInt()}% → $displayCategory | color=$dominantColor | sub=${subMeta?.name ?: "none"}"
        )
    }

    private fun loadBitmap(uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3)
        buf.order(ByteOrder.nativeOrder())
        val pixels = IntArray(imgSize * imgSize)
        bitmap.getPixels(pixels, 0, imgSize, 0, 0, imgSize, imgSize)
        for (px in pixels) {
            buf.putFloat(((px shr 16) and 0xFF) / 255f)
            buf.putFloat(((px shr 8)  and 0xFF) / 255f)
            buf.putFloat(( px         and 0xFF) / 255f)
        }
        return buf
    }

    private fun loadModel(): MappedByteBuffer {
        val fd = context.assets.openFd("clothing_model.tflite")
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    private fun loadLabels(): List<String> =
        context.assets.open("clothing_labels.txt").bufferedReader().readLines().filter { it.isNotBlank() }

    private fun noClothingResult(reason: String = "Not clothing") = ImageAnalysisResult(
        isClothing = false, category = "", tags = emptyList(), debugReason = reason
    )
}
