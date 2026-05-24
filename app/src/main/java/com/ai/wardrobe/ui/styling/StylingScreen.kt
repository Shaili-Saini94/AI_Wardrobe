package com.ai.wardrobe.ui.styling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ai.wardrobe.domain.model.Outfit

private val occasionOptions = listOf(
    "Party",
    "Clubbing",
    "Sports / Gym",
    "Office / Formal",
    "Casual outing",
    "Beach trip",
    "Mountain / Travel trip",
    "Date night"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylingScreen(
    viewModel: StylingViewModel,
    modifier: Modifier = Modifier
) {
    val suggestedOutfits by viewModel.suggestedOutfits.collectAsState()
    val favoriteOutfits by viewModel.favoriteOutfits.collectAsState()
    val message by viewModel.message.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedOccasion by remember { mutableStateOf("Party") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val currentMessage = message
        if (!currentMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(currentMessage)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Style Center",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    selectedOccasion = selectedOccasion,
                    onOccasionSelected = { selectedOccasion = it },
                    outfits = suggestedOutfits,
                    onGenerate = { occasion ->
                        viewModel.generateSuggestions(occasion)
                    },
                    onSave = { outfit ->
                        viewModel.saveOutfit(outfit)
                    }
                )

                1 -> FavoritesContent(
                    outfits = favoriteOutfits,
                    onDelete = { outfit ->
                        outfit.id?.let { id ->
                            viewModel.deleteFavoriteOutfit(id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AISuggestionsContent(
    selectedOccasion: String,
    onOccasionSelected: (String) -> Unit,
    outfits: List<Outfit>,
    onGenerate: (String) -> Unit,
    onSave: (Outfit) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OccasionSelector(
            selectedOccasion = selectedOccasion,
            onOccasionSelected = onOccasionSelected
        )

        if (outfits.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select occasion and generate smart outfits",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Current occasion: $selectedOccasion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
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
            onClick = { onGenerate(selectedOccasion) },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate $selectedOccasion Outfits")
        }
    }
}

@Composable
private fun OccasionSelector(
    selectedOccasion: String,
    onOccasionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Choose occasion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(occasionOptions) { occasion ->
                FilterChip(
                    selected = selectedOccasion == occasion,
                    onClick = { onOccasionSelected(occasion) },
                    label = { Text(occasion) }
                )
            }
        }
    }
}

@Composable
fun FavoritesContent(
    outfits: List<Outfit>,
    onDelete: (Outfit) -> Unit
) {
    if (outfits.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No favorite outfits yet.",
                style = MaterialTheme.typography.bodyLarge
            )
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
    actionIcon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outfit.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = outfit.items.joinToString(" + ") { it.category },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "Action",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        contentDescription = item.category,
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