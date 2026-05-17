package com.ai.wardrobe.ui.styling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ai.wardrobe.domain.model.Outfit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylingScreen(
    viewModel: StylingViewModel,
    modifier: Modifier = Modifier
) {
    val suggestedOutfits by viewModel.suggestedOutfits.collectAsState()
    val favoriteOutfits by viewModel.favoriteOutfits.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Style Center", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("AI Suggestions") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favorites") }
                )
            }

            when (selectedTab) {
                0 -> AISuggestionsContent(
                    outfits = suggestedOutfits,
                    onGenerate = { viewModel.generateSuggestions() },
                    onSave = { viewModel.saveOutfit(it) }
                )
                1 -> FavoritesContent(
                    outfits = favoriteOutfits,
                    onDelete = { it.id?.let { id -> viewModel.deleteFavoriteOutfit(id) } }
                )
            }
        }
    }
}

@Composable
fun AISuggestionsContent(
    outfits: List<Outfit>,
    onGenerate: () -> Unit,
    onSave: (Outfit) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (outfits.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap below to generate smart outfits",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(outfits) { outfit ->
                    OutfitCard(
                        outfit = outfit,
                        onAction = { onSave(outfit) },
                        actionIcon = Icons.Rounded.FavoriteBorder
                    )
                }
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Outfits")
        }
    }
}

@Composable
fun FavoritesContent(
    outfits: List<Outfit>,
    onDelete: (Outfit) -> Unit
) {
    if (outfits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorite outfits yet.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(outfits, key = { it.id ?: 0L }) { outfit ->
                OutfitCard(
                    outfit = outfit,
                    onAction = { onDelete(outfit) },
                    actionIcon = Icons.Rounded.Favorite
                )
            }
        }
    }
}

@Composable
fun OutfitCard(
    outfit: Outfit,
    onAction: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = outfit.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAction) {
                    Icon(actionIcon, contentDescription = "Action", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(outfit.items) { item ->
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}
