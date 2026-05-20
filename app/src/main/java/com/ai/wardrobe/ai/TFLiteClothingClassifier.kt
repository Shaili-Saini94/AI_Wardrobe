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
    private fun inferSubCategory(
        base: String,
        bitmap: Bitmap,
        dominantColor: String,
        cropRegion: String
    ): SubMeta? {
        val subs = subCategories[base] ?: return null
        if (subs.isEmpty()) return null

        val brightness   = computeBrightness(bitmap)    // 0..1
        val colorVar     = computeColorVariance(bitmap)  // 0..1
        val colorLower   = dominantColor.lowercase()

        // Score each sub-category
        val scored = subs.map { sub ->
            var score = 0.0

            // Color keyword match in tags
            if (sub.extraTags.any { it in colorLower || colorLower in it }) score += 3.0

            // Category-specific brightness heuristics
            when (base) {
                "Dress"   -> when {
                    sub.name == "Maxi Dress"      && brightness < 0.5f -> score += 2.0
                    sub.name == "Sundress"         && brightness > 0.6f -> score += 2.0
                    sub.name == "Bodycon Dress"    && colorVar   < 0.15f -> score += 1.5
                    sub.name == "Kaftan"           && colorVar   > 0.25f -> score += 1.5
                    sub.name == "Anarkali Suit"    && colorLower.any { c -> "red orange gold".contains(c.toString()) } -> score += 2.0
                }
                "Jacket"  -> when {
                    sub.name == "Leather Jacket"  && brightness < 0.35f -> score += 2.5
                    sub.name == "Denim Jacket"    && colorLower.contains("blue") -> score += 3.0
                    sub.name == "Puffer Jacket"   && brightness > 0.55f -> score += 1.5
                    sub.name == "Blazer"          && colorVar   < 0.1f  -> score += 2.0
                    sub.name == "Hoodie"          && colorVar   < 0.12f -> score += 1.5
                    sub.name == "Trench Coat"     && (colorLower.contains("beige") || colorLower.contains("tan")) -> score += 2.5
                    sub.name == "Cardigan"        && brightness > 0.5f  -> score += 1.5
                }
                "Shirt"   -> when {
                    sub.name == "Formal Shirt"    && colorVar   < 0.08f -> score += 2.0
                    sub.name == "Flannel Shirt"   && colorVar   > 0.2f  -> score += 2.0
                    sub.name == "Linen Shirt"     && brightness > 0.65f -> score += 2.0
                    sub.name == "Crop Top"        && cropRegion == "top" && brightness > 0.5f -> score += 1.5
                    sub.name == "Tank Top"        && brightness > 0.6f  -> score += 1.5
                    sub.name == "Hawaiian Shirt"  && colorVar   > 0.3f  -> score += 2.5
                }
                "T-Shirt" -> when {
                    sub.name == "Graphic Tee"     && colorVar   > 0.2f  -> score += 2.5
                    sub.name == "Plain T-Shirt"   && colorVar   < 0.08f -> score += 2.0
                    sub.name == "Striped Tee"     && colorVar   in 0.1f..0.25f -> score += 1.5
                    sub.name == "Oversized Tee"   && brightness < 0.5f  -> score += 1.0
                }
                "Sweater" -> when {
                    sub.name == "Turtleneck"      && brightness < 0.45f -> score += 2.0
                    sub.name == "Cable Knit"      && colorVar   in 0.08f..0.18f -> score += 1.5
                    sub.name == "Hoodie"          && colorVar   < 0.1f  -> score += 1.5
                    sub.name == "Sweatshirt"      && brightness < 0.4f  -> score += 1.5
                    sub.name == "Cardigan"        && brightness > 0.55f -> score += 1.5
                }
                "Pants"   -> when {
                    sub.name == "Trousers"        && colorVar   < 0.07f  -> score += 2.0
                    sub.name == "Cargo Pants"     && (colorLower.contains("khaki") || colorLower.contains("olive")) -> score += 2.5
                    sub.name == "Joggers"         && colorVar   < 0.1f   -> score += 1.5
                    sub.name == "Palazzo"         && colorVar   > 0.15f  -> score += 1.5
                    sub.name == "Leggings"        && brightness < 0.35f  -> score += 1.5
                    sub.name == "Salwar"          && colorVar   > 0.1f   -> score += 1.0
                }
                "Jeans"   -> when {
                    sub.name == "Skinny Jeans"    && brightness < 0.4f  -> score += 1.5
                    sub.name == "Wide-Leg Jeans"  && brightness > 0.45f -> score += 1.5
                    sub.name == "Ripped Jeans"    && colorVar   > 0.15f -> score += 2.0
                    sub.name == "Mom Jeans"       && brightness > 0.5f  -> score += 1.5
                }
                "Shoes"   -> when {
                    sub.name == "Sneakers"        && colorVar   > 0.12f -> score += 2.0
                    sub.name == "High Heels"      && brightness < 0.4f  -> score += 1.5
                    sub.name == "Oxford Shoes"    && brightness < 0.35f -> score += 2.0
                    sub.name == "Chelsea Boots"   && brightness < 0.3f  -> score += 2.0
                    sub.name == "Sports Shoes"    && colorVar   > 0.18f -> score += 2.5
                }
                "Kurta"   -> when {
                    sub.name == "Anarkali Kurta"  && colorVar   > 0.15f -> score += 2.0
                    sub.name == "Straight Kurta"  && colorVar   < 0.1f  -> score += 2.0
                    sub.name == "Sherwani"        && brightness < 0.4f  -> score += 2.0
                    sub.name == "Kurti"           && brightness > 0.55f -> score += 1.5
                }
                "Lehenga" -> when {
                    sub.name == "Bridal Lehenga"  && colorVar   > 0.25f -> score += 2.5
                    sub.name == "Chaniya Choli"   && colorVar   > 0.3f  -> score += 2.0
                    sub.name == "Party Lehenga"   && brightness < 0.4f  -> score += 1.5
                }
                "Saree"   -> when {
                    sub.name == "Silk Saree"      && brightness < 0.45f -> score += 2.0
                    sub.name == "Cotton Saree"    && brightness > 0.55f -> score += 2.0
                    sub.name == "Banarasi Saree"  && colorVar   > 0.2f  -> score += 2.5
                    sub.name == "Chiffon Saree"   && brightness > 0.6f  -> score += 2.0
                }
            }
            sub to score
        }

        val best = scored.maxByOrNull { it.second }
        // Only use sub-category if it scored > 0, else fall back to first option
        return best?.first ?: subs.first()
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

        val seen = mutableSetOf<String>()
        val results = mutableListOf<ImageAnalysisResult>()

        for ((idx, crop) in crops.withIndex()) {
            val cropRegion = if (cropNames[idx].contains("top")) "top"
                             else if (cropNames[idx].contains("bottom")) "bottom"
                             else "full"
            val result = classifyBitmap(crop, cropRegion)
            if (result == null) {
                Log.d("TFLite", "  crop[${cropNames[idx]}] → below threshold / null")
                continue
            }
            val displayName = result.subCategory ?: result.category
            Log.d("TFLite", "  crop[${cropNames[idx]}] → ${result.debugReason} isClothing=${result.isClothing}")
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

    private fun classifyBitmap(bitmap: Bitmap, cropRegion: String): ImageAnalysisResult? {
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
        if (confidence < 0.40f) return null

        val label = labels[topIdx]
        val baseMeta = categoryMeta[label] ?: Meta(
            tags = listOf(label.lowercase()), occasions = listOf("Casual"),
            seasons = listOf("Spring", "Summer", "Autumn", "Winter"),
            styles = listOf("Casual")
        )

        // Dominant color from this bitmap
        val dominantColor = DominantColorExtractor.extractFromBitmap(bitmap)

        // Infer sub-category using image signals
        val subMeta = inferSubCategory(label, bitmap, dominantColor, cropRegion)
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
            debugReason = "TFLite: $label @ ${(confidence * 100).toInt()}% → $displayCategory | color=$dominantColor"
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
