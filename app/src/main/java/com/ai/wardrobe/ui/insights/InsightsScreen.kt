package com.ai.wardrobe.ui.insights

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ai.wardrobe.ai.CapsuleItem
import com.ai.wardrobe.ai.ColorPalette
import com.ai.wardrobe.ai.NaturalLanguageSearch
import com.ai.wardrobe.ai.StyleDna
import com.ai.wardrobe.ai.WardrobeGap
import com.ai.wardrobe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state         by viewModel.state.collectAsState()
    val query         by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching   by viewModel.isSearching.collectAsState()
    val focusManager  = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetBackground)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLOSETIQ", style = MaterialTheme.typography.displayMedium, color = ClosetBlack)
            Icon(Icons.Rounded.Lightbulb, null, tint = ClosetGold, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp)) {
            Text("Wardrobe Intelligence", style = MaterialTheme.typography.displayLarge, color = ClosetBlack)
            Spacer(Modifier.height(6.dp))
            Text("Smart insights from your wardrobe.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary)
        }

        Spacer(Modifier.height(20.dp))

        // ── Natural language search bar ───────────────────────────────────────
        OutlinedTextField(
            value         = query,
            onValueChange = { viewModel.search(it) },
            placeholder   = { Text("Try: cozy outfit for cold Sunday…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = ClosetSecondary) },
            trailingIcon  = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { viewModel.clearSearch(); focusManager.clearFocus() }) {
                        Icon(Icons.Rounded.Close, null, tint = ClosetSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ClosetGold,
                unfocusedBorderColor = ClosetBorder,
                focusedLabelColor    = ClosetGold
            ),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Spacer(Modifier.height(16.dp))

        if (isSearching && searchResults.isNotEmpty()) {
            // ── Search results ────────────────────────────────────────────────
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("${searchResults.size} results for \"$query\"",
                        style = MaterialTheme.typography.labelSmall, color = ClosetSecondary,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(searchResults, key = { it.item.id ?: 0L }) { result ->
                    SearchResultCard(result)
                }
            }
        } else if (isSearching && searchResults.isEmpty() && query.isNotBlank()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No items match \"$query\".\nTry different words.",
                    style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary, textAlign = TextAlign.Center)
            }
        } else {
            // ── Intelligence cards ────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ClosetBlack, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier            = Modifier.fillMaxSize()
                ) {
                    // Style DNA
                    state.styleDna?.let { dna ->
                        if (dna.dominant != "Undefined") {
                            item { StyleDnaCard(dna) }
                        }
                    }

                    // Color palette
                    state.palette?.let { palette ->
                        if (palette.total > 0) {
                            item { ColorPaletteCard(palette) }
                        }
                    }

                    // Wardrobe gaps
                    if (state.gaps.isNotEmpty()) {
                        item { GapsCard(state.gaps) }
                    }

                    // Similar / duplicate items
                    if (state.similars.isNotEmpty()) {
                        item { SimilarItemsCard(state.similars) }
                    }

                    // Capsule wardrobe
                    if (state.capsule.isNotEmpty()) {
                        item { CapsuleCard(state.capsule) }
                    }

                    if (state.capsule.isEmpty() && state.gaps.isEmpty() && state.styleDna?.dominant == "Undefined") {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Lightbulb, null, tint = ClosetGold.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Text("Add items to your wardrobe\nto unlock AI insights.",
                                        style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ── Style DNA card ────────────────────────────────────────────────────────────

@Composable
private fun StyleDnaCard(dna: StyleDna) {
    InsightCard(
        title = "Your Style DNA",
        icon  = Icons.Rounded.AutoAwesome,
        iconTint = ClosetGold
    ) {
        Text(
            text  = "You're ${(((dna.breakdown[dna.dominant] ?: 0f) * 100).toInt())}% ${dna.dominant}",
            style = MaterialTheme.typography.headlineMedium,
            color = ClosetBlack,
            fontWeight = FontWeight.Bold
        )
        if (dna.secondary != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "with a touch of ${dna.secondary}",
                style = MaterialTheme.typography.bodyMedium,
                color = ClosetSecondary
            )
        }

        Spacer(Modifier.height(14.dp))

        // Style bars
        dna.breakdown.entries.take(5).forEach { (style, pct) ->
            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(style, style = MaterialTheme.typography.labelSmall, color = ClosetBlack)
                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)).background(ClosetStatsBg)) {
                    Box(
                        Modifier.fillMaxWidth(pct).height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ClosetBlack)
                    )
                }
            }
        }
    }
}

// ── Color name → Compose Color mapping ───────────────────────────────────────

private fun colorNameToComposeColor(name: String): Color {
    val n = name.lowercase()
    return when {
        "black"  in n || "jet" in n || "charcoal" in n  -> Color(0xFF1B1C1C)
        "white"  in n || "ivory" in n || "cream" in n || "ecru" in n || "pearl" in n -> Color(0xFFF8F5F0)
        "off-white" in n                                 -> Color(0xFFF0EDE8)
        "grey"   in n || "gray" in n || "silver" in n   -> Color(0xFF9E9E9E)
        "slate"  in n                                    -> Color(0xFF6D7C8A)
        "beige"  in n || "sand" in n                     -> Color(0xFFD4B896)
        "tan"    in n || "camel" in n                    -> Color(0xFFC19A6B)
        "stone"  in n || "taupe" in n                    -> Color(0xFFB0A090)
        "khaki"  in n                                    -> Color(0xFFC3B091)
        "navy"   in n || "indigo" in n                   -> Color(0xFF1E2E5C)
        "blue"   in n || "cobalt" in n                   -> Color(0xFF2563EB)
        "sky"    in n                                    -> Color(0xFF7DD3FC)
        "denim"  in n                                    -> Color(0xFF6B8CAE)
        "teal"   in n                                    -> Color(0xFF0D9488)
        "green"  in n || "forest" in n                   -> Color(0xFF16A34A)
        "olive"  in n || "sage" in n                     -> Color(0xFF6B7C4A)
        "mint"   in n || "emerald" in n                  -> Color(0xFF34D399)
        "red"    in n || "scarlet" in n                  -> Color(0xFFDC2626)
        "crimson" in n || "maroon" in n                  -> Color(0xFF7F1D1D)
        "burgundy" in n                                  -> Color(0xFF6D1A36)
        "orange" in n || "terracotta" in n               -> Color(0xFFEA580C)
        "rust"   in n                                    -> Color(0xFFC2410C)
        "coral"  in n || "peach" in n                    -> Color(0xFFFB923C)
        "yellow" in n || "lemon" in n                    -> Color(0xFFFACC15)
        "mustard" in n                                   -> Color(0xFFCA8A04)
        "gold"   in n                                    -> Color(0xFF92600A)
        "purple" in n || "violet" in n || "plum" in n   -> Color(0xFF7C3AED)
        "lavender" in n || "lilac" in n                  -> Color(0xFFA78BFA)
        "pink"   in n || "rose" in n                     -> Color(0xFFEC4899)
        "blush"  in n                                    -> Color(0xFFFBCFE8)
        "magenta" in n || "fuchsia" in n                 -> Color(0xFFD946EF)
        "brown"  in n || "chocolate" in n || "mocha" in n || "cognac" in n -> Color(0xFF92400E)
        else -> Color(0xFFD1C9C0)   // fallback warm neutral
    }
}

// ── Color palette card ────────────────────────────────────────────────────────

@Composable
private fun ColorPaletteCard(palette: ColorPalette) {
    val total = palette.total.toFloat().coerceAtLeast(1f)
    val segments = listOf(
        Triple("Neutrals", palette.neutrals, Color(0xFFBDB5AD)),
        Triple("Blues",    palette.blues,    Color(0xFF2C4A7C)),
        Triple("Earthy",   palette.earthy,   Color(0xFF8B6914)),
        Triple("Warm",     palette.warm,     Color(0xFFB5179E)),
        Triple("Cool",     palette.cool,     Color(0xFF2D6A4F))
    ).filter { it.second > 0 }

    InsightCard(title = "Your Color Palette", icon = Icons.Rounded.Palette, iconTint = Color(0xFF775A19)) {

        // ── Harmony badge ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    palette.dominantFamily(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = ClosetBlack,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "dominant colour family",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ClosetStatsBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    palette.harmonyLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetGold,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Actual color swatches ─────────────────────────────────────────────
        if (palette.topColors.isNotEmpty()) {
            Text(
                "COLOURS IN YOUR WARDROBE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ClosetSecondary
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(palette.topColors) { (colorName, count) ->
                    val swatchColor = colorNameToComposeColor(colorName)
                    val isLight = swatchColor.red * 0.299f + swatchColor.green * 0.587f + swatchColor.blue * 0.114f > 0.7f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .then(
                                    if (isLight) Modifier.border(1.dp, ClosetBorder, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isLight) ClosetSecondary else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            colorName.replaceFirstChar(Char::titlecase),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ClosetSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Stacked proportion bar ────────────────────────────────────────────
        if (segments.isNotEmpty()) {
            Text(
                "COLOUR FAMILY BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ClosetSecondary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                segments.forEach { (_, count, color) ->
                    Box(
                        Modifier
                            .weight(count / total)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Legend in a wrap-style row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                segments.forEach { (name, count, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(
                            "$name · $count",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ClosetSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Harmony note ──────────────────────────────────────────────────────
        val harmonyNote = when {
            palette.neutrals > (palette.total * 0.6f) ->
                "Your wardrobe is anchored in neutrals — incredibly versatile and easy to mix."
            palette.blues > (palette.total * 0.4f) ->
                "Blues dominate your wardrobe — cool, classic, and always in style."
            palette.earthy > (palette.total * 0.4f) ->
                "Earthy tones give your wardrobe a warm, grounded character."
            palette.warm > (palette.total * 0.4f) ->
                "Bold warm tones — your wardrobe makes a statement."
            else ->
                "A diverse colour mix — great for building many different looks."
        }
        Text(
            harmonyNote,
            style = MaterialTheme.typography.bodyMedium,
            color = ClosetSecondary
        )
    }
}

// ── Gaps card ─────────────────────────────────────────────────────────────────

@Composable
private fun GapsCard(gaps: List<WardrobeGap>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    InsightCard(title = "Wardrobe Gaps", icon = Icons.Rounded.WarningAmber, iconTint = Color(0xFFF0A500)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            gaps.forEach { gap ->
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${gap.current}→${gap.recommended}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(0xFFF0A500),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(gap.slot, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold, color = ClosetBlack)
                            Spacer(Modifier.height(2.dp))
                            Text(gap.suggestion, style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Shopping deep-links
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val query = java.net.URLEncoder.encode(gap.slot, "UTF-8")
                        ShopButton(
                            label = "Myntra",
                            url = "https://www.myntra.com/${gap.slot.lowercase().replace(" ", "-")}",
                            context = context
                        )
                        ShopButton(
                            label = "Amazon",
                            url = "https://www.amazon.in/s?k=${query}",
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopButton(label: String, url: String, context: android.content.Context) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ClosetStatsBg)
            .clickable {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                )
                context.startActivity(intent)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "SHOP $label →",
            style = MaterialTheme.typography.labelSmall,
            color = ClosetSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Similar Items card ────────────────────────────────────────────────────────

@Composable
private fun SimilarItemsCard(similars: List<com.ai.wardrobe.ai.SimilarPair>) {
    InsightCard(
        title = "Similar Items Detected",
        icon = Icons.Rounded.ContentCopy,
        iconTint = Color(0xFF7B5EA7)
    ) {
        Text(
            "You may have near-duplicates in your wardrobe. Consider which one to keep.",
            style = MaterialTheme.typography.bodyMedium,
            color = ClosetSecondary
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            similars.forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ClosetStatsBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item A thumbnail
                    SimilarThumb(uri = pair.itemA.thumbnailUri)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${pair.itemA.category} vs ${pair.itemB.category}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ClosetBlack
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            pair.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClosetSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        val pct = (pair.similarityScore * 100).toInt()
                        Text(
                            "$pct% similar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7B5EA7),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Item B thumbnail
                    SimilarThumb(uri = pair.itemB.thumbnailUri)
                }
            }
        }
    }
}

@Composable
private fun SimilarThumb(uri: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ClosetCardBg),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            coil3.compose.AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(Icons.Rounded.Checkroom, null, tint = ClosetSecondary, modifier = Modifier.size(28.dp))
        }
    }
}

// ── Capsule card ──────────────────────────────────────────────────────────────

@Composable
private fun CapsuleCard(capsule: List<CapsuleItem>) {
    InsightCard(title = "Capsule Essentials", icon = Icons.Rounded.Stars, iconTint = ClosetGold) {
        Text(
            "Your most versatile items — ranked by how many outfits they unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = ClosetSecondary
        )
        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(capsule.take(8)) { capsuleItem ->
                CapsuleItemCard(capsuleItem)
            }
        }
    }
}

@Composable
private fun CapsuleItemCard(capsuleItem: CapsuleItem) {
    val item = capsuleItem.item
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ClosetStatsBg)
        ) {
            AsyncImage(
                model        = item.thumbnailUri ?: item.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier     = Modifier.fillMaxSize().padding(4.dp)
            )
            // Outfits badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ClosetBlack)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    "${capsuleItem.outfitsEnabled}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            item.category,
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color    = ClosetBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        item.dominantColor?.let {
            Text(it, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = ClosetSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Search result card ────────────────────────────────────────────────────────

@Composable
private fun SearchResultCard(result: NaturalLanguageSearch.SearchResult) {
    val item = result.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ClosetCardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(ClosetStatsBg)
        ) {
            AsyncImage(
                model        = item.thumbnailUri ?: item.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier     = Modifier.fillMaxSize().padding(4.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.category, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = ClosetBlack)
            item.dominantColor?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
            }
            if (item.occasions.isNotEmpty()) {
                Text(
                    item.occasions.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ClosetSecondary
                )
            }
        }
        // Match reasons as mini chips
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            result.matchReasons.take(2).forEach { reason ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ClosetGold.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(reason, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ClosetGold)
                }
            }
        }
    }
}

// ── Shared insight card container ─────────────────────────────────────────────

@Composable
private fun InsightCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ClosetCardBg)
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
