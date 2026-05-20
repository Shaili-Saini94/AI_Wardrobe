package com.ai.wardrobe.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.ui.theme.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val clothingItems by viewModel.clothingItems.collectAsState()
    val savedOutfits  by viewModel.savedOutfits.collectAsState()
    val styleProfile  by viewModel.styleProfile.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLOSETIQ",
                style = MaterialTheme.typography.displayMedium,
                color = ClosetBlack
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ClosetStatsBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(18.dp),
                    tint = ClosetSecondary
                )
            }
        }

        // ── Title ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            Text(
                text = "Your Profile",
                style = MaterialTheme.typography.displayLarge,
                color = ClosetBlack
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Style preferences & wardrobe insights.",
                style = MaterialTheme.typography.bodyLarge,
                color = ClosetSecondary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Avatar ───────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ClosetStatsBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = ClosetSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Fashion Enthusiast",
                style = MaterialTheme.typography.headlineMedium,
                color = ClosetBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (styleProfile.preferredStyles.isNotEmpty()) {
                Text(
                    text = styleProfile.preferredStyles.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetGold,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    text = "Powered by Gemini AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetGold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Style DNA ────────────────────────────────────────────
        ProfileSection(title = "Style DNA", subtitle = "Pick your aesthetic — AI will match outfits to it") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyleProfile.ALL_STYLES.forEach { style ->
                    val selected = style in styleProfile.preferredStyles
                    StyleDnaChip(
                        label = style,
                        selected = selected,
                        badgeColor = Color(StyleProfile.STYLE_COLORS[style] ?: 0xFF1B1C1C),
                        textOnDark = style in StyleProfile.STYLE_TEXT_ON_DARK,
                        onClick = { viewModel.toggleStyle(style) }
                    )
                }
            }
        }

        // ── Favourite Occasions ──────────────────────────────────
        ProfileSection(title = "Favourite Occasions", subtitle = "AI prioritises these when building outfits") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyleProfile.ALL_OCCASIONS.forEach { occ ->
                    val selected = occ in styleProfile.favoriteOccasions
                    OccasionToggleChip(
                        label = occ,
                        selected = selected,
                        onClick = { viewModel.toggleOccasion(occ) }
                    )
                }
            }
        }

        // ── Colour Palette ───────────────────────────────────────
        ProfileSection(title = "Colour Palette", subtitle = "Guides colour harmony in suggestions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StyleProfile.ALL_PALETTES.forEach { palette ->
                    val selected = styleProfile.colorPalette == palette
                    PaletteRow(
                        label = palette,
                        selected = selected,
                        onClick = { viewModel.setColorPalette(palette) }
                    )
                }
            }
        }

        // ── Gender / Fit Preference ──────────────────────────────
        ProfileSection(title = "Fit Preference", subtitle = "Tailors recommendations to your body fit") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Unisex", "Menswear", "Womenswear").forEach { g ->
                    val selected = styleProfile.gender == g
                    GenderChip(
                        label = g,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setGender(g) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = ClosetStatsBg
        )
        Spacer(modifier = Modifier.height(28.dp))

        // ── Wardrobe Stats ──────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Wardrobe Analytics", style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(
                    label = "TOTAL ITEMS",
                    value = clothingItems.size.toString(),
                    icon = Icons.Rounded.Checkroom,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    label = "OUTFITS SAVED",
                    value = savedOutfits.size.toString(),
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val uniqueSeasons = clothingItems.flatMap { it.seasons }.distinct().size
                val uniqueOccasions = clothingItems.flatMap { it.occasions }.distinct().size
                ProfileStatCard(
                    label = "SEASONS",
                    value = uniqueSeasons.toString(),
                    icon = Icons.Rounded.WbSunny,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    label = "OCCASIONS",
                    value = uniqueOccasions.toString(),
                    icon = Icons.Rounded.Event,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Category Breakdown ──────────────────────────────────
        if (clothingItems.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
                Spacer(modifier = Modifier.height(14.dp))

                val categories = clothingItems
                    .groupBy {
                        when {
                            it.category.contains("shirt", true) || it.category.contains("top", true) ||
                            it.category.contains("blouse", true) || it.category.contains("hoodie", true) ||
                            it.category.contains("t-shirt", true) || it.category.contains("sweatshirt", true) -> "Tops"
                            it.category.contains("pants", true) || it.category.contains("jean", true) ||
                            it.category.contains("skirt", true) || it.category.contains("short", true) ||
                            it.category.contains("trouser", true) -> "Bottoms"
                            it.category.contains("dress", true) || it.category.contains("jumpsuit", true) -> "Dresses"
                            it.category.contains("shoe", true) || it.category.contains("sneaker", true) ||
                            it.category.contains("boot", true) || it.category.contains("sandal", true) -> "Shoes"
                            it.category.contains("jacket", true) || it.category.contains("coat", true) -> "Outerwear"
                            else -> "Other"
                        }
                    }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClosetCardBg)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        categories.forEach { (category, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category, style = MaterialTheme.typography.bodyLarge, color = ClosetBlack)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val fraction = count.toFloat() / clothingItems.size.coerceAtLeast(1)
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(ClosetStatsBg)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(CircleShape)
                                                .background(ClosetBlack)
                                        )
                                    }
                                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── About Card ──────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("About", style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ClosetCardBg)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ClosetGold, modifier = Modifier.size(18.dp))
                        Text("ClosetIQ v1.0", style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI-powered wardrobe manager. Scan your clothes, build a digital closet, and get smart outfit suggestions for any occasion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClosetSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("POWERED BY GEMINI AI", style = MaterialTheme.typography.labelSmall, color = ClosetGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

// ── Sub-components ─────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ClosetSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun StyleDnaChip(
    label: String,
    selected: Boolean,
    badgeColor: Color,
    textOnDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) badgeColor else ClosetStatsBg,
        animationSpec = tween(200), label = "style_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            if (textOnDark) Color.White else ClosetBlack
        } else ClosetSecondary,
        animationSpec = tween(200), label = "style_chip_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun OccasionToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) ClosetBlack else ClosetStatsBg,
        animationSpec = tween(200), label = "occ_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else ClosetSecondary,
        animationSpec = tween(200), label = "occ_chip_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun PaletteRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ClosetCardBg else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ClosetBlack.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) ClosetBlack else ClosetSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ClosetBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) ClosetBlack else ClosetStatsBg,
        animationSpec = tween(200), label = "gender_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else ClosetSecondary,
        animationSpec = tween(200), label = "gender_chip_text"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ClosetStatsBg)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                Icon(icon, contentDescription = null, tint = ClosetSecondary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                color = ClosetBlack
            )
        }
    }
}
