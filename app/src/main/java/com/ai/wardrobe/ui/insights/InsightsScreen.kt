package com.ai.wardrobe.ui.insights

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

// ── Color palette card ────────────────────────────────────────────────────────

@Composable
private fun ColorPaletteCard(palette: ColorPalette) {
    val total = palette.total.toFloat().coerceAtLeast(1f)
    val segments = listOf(
        Triple("Neutrals", palette.neutrals, Color(0xFFE8E6E3)),
        Triple("Blues",    palette.blues,    Color(0xFF2C4A7C)),
        Triple("Earthy",   palette.earthy,   Color(0xFF8B6914)),
        Triple("Warm",     palette.warm,     Color(0xFFB5179E)),
        Triple("Cool",     palette.cool,     Color(0xFF2D6A4F))
    ).filter { it.second > 0 }

    InsightCard(title = "Your Color Palette", icon = Icons.Rounded.Palette, iconTint = Color(0xFF775A19)) {
        Text(
            "Dominant: ${palette.dominantFamily()}",
            style = MaterialTheme.typography.bodyMedium,
            color = ClosetSecondary
        )
        Spacer(Modifier.height(14.dp))

        // Horizontal stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            segments.forEach { (_, count, color) ->
                Box(
                    modifier = Modifier
                        .weight(count / total)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { (name, count, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Text("$name ($count)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ClosetSecondary)
                }
            }
        }
    }
}

// ── Gaps card ─────────────────────────────────────────────────────────────────

@Composable
private fun GapsCard(gaps: List<WardrobeGap>) {
    InsightCard(title = "Wardrobe Gaps", icon = Icons.Rounded.WarningAmber, iconTint = Color(0xFFF0A500)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            gaps.forEach { gap ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.Top
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
                    Column {
                        Text(gap.slot, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = ClosetBlack)
                        Spacer(Modifier.height(2.dp))
                        Text(gap.suggestion, style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                    }
                }
            }
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
