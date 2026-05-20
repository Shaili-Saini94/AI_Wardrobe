package com.ai.wardrobe.ui.styling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.ui.theme.*

// ── Style badge colours ───────────────────────────────────────────────────────

private fun styleBgColor(styleType: String): Color = when (styleType) {
    "Minimalist"   -> Color(0xFFE8E6E3)
    "Classic"      -> Color(0xFF775A19)
    "Smart Casual" -> Color(0xFF2C4A7C)
    "Streetwear"   -> Color(0xFF1B1C1C)
    "Funky"        -> Color(0xFFB5179E)
    "Bohemian"     -> Color(0xFF8B6914)
    "Athleisure"   -> Color(0xFF2D6A4F)
    "Formal"       -> Color(0xFF1A2744)
    else           -> Color(0xFFE8E6E3)
}

private fun styleTextColor(styleType: String): Color =
    if (styleType == "Minimalist") Color(0xFF444748) else Color.White

// ── Occasion filter chips ─────────────────────────────────────────────────────

private val OCCASION_FILTERS = listOf(
    "Any", "Work", "Date Night", "Weekend", "Casual",
    "Party", "Gym", "Beach", "Travel", "Formal Event"
)

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StylingScreen(
    viewModel: StylingViewModel,
    modifier: Modifier = Modifier
) {
    val filteredOutfits  by viewModel.filteredOutfits.collectAsState()
    val suggestedOutfits by viewModel.suggestedOutfits.collectAsState()
    val favoriteOutfits  by viewModel.favoriteOutfits.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val selectedOccasion by viewModel.selectedOccasion.collectAsState()
    val styleProfile     by viewModel.styleProfile.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(ClosetBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp).padding(top = 56.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("CLOSETIQ", style = MaterialTheme.typography.displayMedium, color = ClosetBlack)
                Icon(Icons.Rounded.AutoAwesome, null, tint = ClosetGold, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 4.dp)) {
                Text("Style Center", style = MaterialTheme.typography.displayLarge, color = ClosetBlack)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (styleProfile.preferredStyles.isEmpty()) "AI-powered outfit suggestions from your wardrobe."
                    else "Styled for: ${styleProfile.preferredStyles.take(2).joinToString(" & ")}",
                    style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Tabs ─────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("AI SUGGESTIONS", selectedTab == 0) { selectedTab = 0 }
                TabChip("SAVED",          selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> SuggestionsContent(
                    outfits          = filteredOutfits,
                    allOutfits       = suggestedOutfits,
                    isLoading        = isLoading,
                    selectedOccasion = selectedOccasion,
                    onOccasionSelect = { viewModel.setOccasionFilter(it) },
                    onGenerate       = { viewModel.generateSuggestions() },
                    onSave           = { viewModel.saveOutfit(it) }
                )
                1 -> FavoritesContent(
                    outfits  = favoriteOutfits,
                    onDelete = { it.id?.let { id -> viewModel.deleteFavoriteOutfit(id) } }
                )
            }
        }
    }
}

// ── Suggestions tab ───────────────────────────────────────────────────────────

@Composable
private fun SuggestionsContent(
    outfits: List<Outfit>,
    allOutfits: List<Outfit>,
    isLoading: Boolean,
    selectedOccasion: String,
    onOccasionSelect: (String) -> Unit,
    onGenerate: () -> Unit,
    onSave: (Outfit) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Occasion filter strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OCCASION_FILTERS.forEach { occasion ->
                OccasionChip(
                    text     = occasion,
                    selected = selectedOccasion == occasion,
                    onClick  = { onOccasionSelect(occasion) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (outfits.isEmpty() && !isLoading) {
            if (allOutfits.isEmpty()) {
                // Never generated
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(48.dp), tint = ClosetGold.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("No outfits yet", style = MaterialTheme.typography.headlineMedium, color = ClosetBlack, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap Generate and AI will build fashion-forward outfits using your style profile and wardrobe.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // Generated but nothing for this filter
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No $selectedOccasion outfits in this set.\nTry a different filter or regenerate.",
                        style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.weight(1f),
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(outfits) { outfit ->
                    SmartOutfitCard(outfit = outfit, onSave = { onSave(outfit) })
                }
            }
        }

        // Loading indicator
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = ClosetBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI IS STYLING YOUR WARDROBE...", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
            }
        }

        // Generate button
        Button(
            onClick  = onGenerate,
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(52.dp),
            shape    = RoundedCornerShape(4.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = ClosetBlack, contentColor = Color.White,
                disabledContainerColor = ClosetSecondaryBtn, disabledContentColor = ClosetSecondary
            )
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Text(
                    text = if (outfits.isEmpty()) "GENERATE OUTFITS" else "REGENERATE",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Smart outfit card ─────────────────────────────────────────────────────────

@Composable
fun SmartOutfitCard(outfit: Outfit, onSave: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ClosetCardBg)
    ) {
        Column {
            // ── Style + occasion badges ──────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Style badge
                if (outfit.styleType.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(styleBgColor(outfit.styleType))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text  = outfit.styleType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = styleTextColor(outfit.styleType),
                            fontSize = 10.sp
                        )
                    }
                }
                // Occasion badge
                if (outfit.occasionType.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ClosetStatsBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text  = outfit.occasionType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ClosetSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // Save button
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.FavoriteBorder, "Save", tint = ClosetGold, modifier = Modifier.size(20.dp))
                }
            }

            // ── Outfit name ──────────────────────────────────────
            Text(
                text     = outfit.name,
                style    = MaterialTheme.typography.headlineMedium,
                color    = ClosetBlack,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // ── Color story ──────────────────────────────────────
            if (outfit.colorStory.isNotBlank()) {
                Text(
                    text     = "\"${outfit.colorStory}\"",
                    style    = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color    = ClosetSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                )
            }

            // ── Item thumbnails ──────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(outfit.items) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF7F5F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model        = item.thumbnailUri ?: item.imageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier     = Modifier.fillMaxSize().padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = item.category.split(" ").last(),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = ClosetSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Style note ───────────────────────────────────────
            if (outfit.styleNote.isNotBlank()) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = ClosetGold, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Text(outfit.styleNote, style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── "Why it works" expandable ────────────────────────
            if (outfit.whyItWorks.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ClosetBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "WHY IT WORKS",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = ClosetGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint     = ClosetGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text     = outfit.whyItWorks,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = ClosetSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    )
                }

                if (!expanded) Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Favorites tab ─────────────────────────────────────────────────────────────

@Composable
private fun FavoritesContent(outfits: List<Outfit>, onDelete: (Outfit) -> Unit) {
    if (outfits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No saved outfits", style = MaterialTheme.typography.headlineMedium, color = ClosetBlack, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap ♡ on any suggestion to save it here.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(outfits, key = { it.id ?: 0L }) { outfit ->
                SavedOutfitCard(outfit = outfit, onDelete = { onDelete(outfit) })
            }
        }
    }
}

@Composable
private fun SavedOutfitCard(outfit: Outfit, onDelete: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ClosetCardBg)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (outfit.styleType.isNotBlank()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(styleBgColor(outfit.styleType)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(outfit.styleType.uppercase(), style = MaterialTheme.typography.labelSmall, color = styleTextColor(outfit.styleType), fontSize = 10.sp)
                    }
                }
                if (outfit.occasionType.isNotBlank()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ClosetStatsBg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(outfit.occasionType.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFB3261E), modifier = Modifier.size(18.dp))
                }
            }

            Text(outfit.name, style = MaterialTheme.typography.headlineMedium, color = ClosetBlack, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

            if (outfit.colorStory.isNotBlank()) {
                Text("\"${outfit.colorStory}\"", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = ClosetSecondary, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp))
            }

            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(outfit.items) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF7F5F3)), contentAlignment = Alignment.Center) {
                            AsyncImage(model = item.thumbnailUri ?: item.imageUri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.category.split(" ").last(), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            if (outfit.styleNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = ClosetGold, modifier = Modifier.size(14.dp))
                    Text(outfit.styleNote, style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Shared chip components ────────────────────────────────────────────────────

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        shape    = RoundedCornerShape(4.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (selected) ClosetBlack else ClosetSecondaryBtn,
            contentColor   = if (selected) Color.White else ClosetBlack
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier       = Modifier.height(38.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OccasionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg  by animateColorAsState(if (selected) ClosetBlack else ClosetSecondaryBtn, label = "bg")
    val txt by animateColorAsState(if (selected) Color.White else ClosetSecondary, label = "txt")
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = txt)
    }
}
