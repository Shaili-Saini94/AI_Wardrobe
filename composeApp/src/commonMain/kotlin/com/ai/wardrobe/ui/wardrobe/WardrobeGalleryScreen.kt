package com.ai.wardrobe.ui.wardrobe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.ai.PlatformCamera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeGalleryScreen(
    viewModel: WardrobeViewModel,
    platformCamera: PlatformCamera,
    modifier: Modifier = Modifier
) {
    val items by viewModel.clothingItems.collectAsState()
    var showAddOptions by remember { mutableStateOf(false) }

    val photoPickerLauncher = platformCamera.rememberGalleryLauncher { uri ->
        viewModel.addClothingItem(uri)
    }

    val cameraLauncher = platformCamera.rememberCameraLauncher { uri ->
        viewModel.addClothingItem(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Closet", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddOptions = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Item")
            }
        },
        modifier = modifier
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Your closet is empty. Add some clothes!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(items, key = { it.id ?: 0L }) { item ->
                    ClothingItemCard(item = item)
                }
            }
        }

        if (showAddOptions) {
            ModalBottomSheet(onDismissRequest = { showAddOptions = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = { Icon(Icons.Rounded.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showAddOptions = false
                            cameraLauncher()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Rounded.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showAddOptions = false
                            photoPickerLauncher()
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ClothingItemCard(item: ClothingItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = item.imageUri,
                contentDescription = item.category,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.tags.isNotEmpty()) {
                    Text(
                        text = item.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
