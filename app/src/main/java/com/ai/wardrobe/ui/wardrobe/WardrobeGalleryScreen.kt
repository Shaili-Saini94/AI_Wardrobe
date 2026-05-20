package com.ai.wardrobe.ui.wardrobe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ai.wardrobe.ai.ImageAnalysisResult
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.ui.theme.*
import com.ai.wardrobe.util.FileUtil
import androidx.core.content.FileProvider

private val VIBES = listOf("Office", "Weekend Chill", "Date Night", "Festival", "Gym", "Beach", "Ethnic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeGalleryScreen(
    viewModel: WardrobeViewModel,
    modifier: Modifier = Modifier
) {
    val items           by viewModel.filteredItems.collectAsState()
    val allItems        by viewModel.clothingItems.collectAsState()
    val isAnalyzing     by viewModel.isAnalyzing.collectAsState()
    val isSaving        by viewModel.isSaving.collectAsState()
    val savingStatus    by viewModel.savingStatus.collectAsState()
    val pendingAnalysis by viewModel.pendingAnalysis.collectAsState()
    val errorMessage    by viewModel.errorMessage.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val selectedVibe    by viewModel.selectedVibe.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager    = LocalFocusManager.current

    val context = LocalContext.current
    var showAddOptions by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedItem by remember { mutableStateOf<ClothingItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzePhoto(context, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) capturedImageUri?.let { viewModel.analyzePhoto(context, it) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CLOSETIQ", style = MaterialTheme.typography.displayMedium, color = ClosetBlack)
                Text("${items.size} ITEMS", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Text("Digital Closet", style = MaterialTheme.typography.displayLarge, color = ClosetBlack)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your wardrobe, digitized and organized.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Search bar ──────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder   = { Text("Search: navy shirt, cozy top…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = ClosetSecondary) },
                trailingIcon  = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery(""); focusManager.clearFocus() }) {
                            Icon(Icons.Rounded.Close, null, tint = ClosetSecondary)
                        }
                    }
                },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ClosetGold,
                    unfocusedBorderColor = ClosetBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Vibe filter chips ───────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VIBES.forEach { vibe ->
                    val isSelected = selectedVibe == vibe
                    val bg  by animateColorAsState(if (isSelected) ClosetBlack else ClosetSecondaryBtn, label = "vibe_bg")
                    val txt by animateColorAsState(if (isSelected) Color.White else ClosetSecondary, label = "vibe_txt")
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { viewModel.setVibe(if (isSelected) null else vibe) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(vibe, style = MaterialTheme.typography.labelSmall, color = txt)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Item count / filter label ───────────────────────────
            if (searchQuery.isNotBlank() || selectedVibe != null) {
                Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${items.size} of ${allItems.size} items", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                    if (searchQuery.isNotBlank() || selectedVibe != null) {
                        TextButton(onClick = { viewModel.setSearchQuery(""); viewModel.setVibe(null) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("CLEAR", style = MaterialTheme.typography.labelSmall, color = ClosetGold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Add Button ──────────────────────────────────────────
            Button(
                onClick = { showAddOptions = true },
                enabled = !isAnalyzing && !isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClosetBlack,
                    contentColor   = Color.White,
                    disabledContainerColor = ClosetSecondaryBtn,
                    disabledContentColor   = ClosetSecondary
                )
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Text("ADD CLOTHING", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Grid / Empty ────────────────────────────────────────
            if (items.isEmpty() && !isAnalyzing && !isSaving) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (allItems.isEmpty()) {
                            Text("Your closet is empty", style = MaterialTheme.typography.headlineMedium, color = ClosetBlack, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scan or upload your first item to start building your digital wardrobe.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center)
                        } else {
                            Text("No matching items", style = MaterialTheme.typography.headlineMedium, color = ClosetBlack, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Try different search terms or a different vibe filter.", style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id ?: 0L }) { item ->
                        ClothingItemCard(item = item, onClick = { selectedItem = item })
                    }
                }
            }
        }

        // ── Analyzing overlay ───────────────────────────────────────
        AnimatedVisibility(visible = isAnalyzing, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(ClosetBackground.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ClosetBlack, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("SCANNING YOUR PHOTO...", style = MaterialTheme.typography.labelSmall, color = ClosetBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("AI is detecting all clothing items", style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                }
            }
        }

        // ── Saving overlay ──────────────────────────────────────────
        AnimatedVisibility(visible = isSaving, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(ClosetBackground.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ClosetBlack, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(savingStatus.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClosetBlack, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
        }

        // ── Snackbar ────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { data ->
            Snackbar(snackbarData = data, containerColor = ClosetBlack, contentColor = Color.White, shape = RoundedCornerShape(8.dp))
        }

        // ── Add Options Sheet ───────────────────────────────────────
        if (showAddOptions) {
            ModalBottomSheet(
                onDismissRequest = { showAddOptions = false },
                containerColor   = ClosetBackground,
                shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                    Text("Add to Wardrobe", style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("AI will detect all clothing items in your photo automatically.", style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showAddOptions = false
                            val file = FileUtil.createImageFile(context)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            capturedImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ClosetBlack, contentColor = Color.White)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Text("TAKE PHOTO", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showAddOptions = false; photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ClosetSecondaryBtn, contentColor = ClosetBlack)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                            Text("CHOOSE FROM GALLERY", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // ── Confirmation Sheet ──────────────────────────────────────
        val pending = pendingAnalysis
        if (pending != null) {
            val selectedIndices = remember(pending) {
                mutableStateOf(pending.items.indices.toMutableSet<Int>())
            }

            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissPending() },
                containerColor   = ClosetBackground,
                shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(ClosetGold), contentAlignment = Alignment.Center) {
                                Text("${pending.items.size}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
                            Text(
                                text  = if (pending.items.size == 1) "1 item detected" else "${pending.items.size} items detected",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ClosetBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Review and select the items you want to add to your wardrobe.", style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Photo preview strip
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(12.dp)).background(ClosetStatsBg),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pending.sourceUri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detected item cards (scrollable)
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(pending.items.size) { idx ->
                            val item = pending.items[idx]
                            val isSelected = idx in selectedIndices.value
                            DetectedItemCard(
                                item       = item,
                                isSelected = isSelected,
                                onToggle   = {
                                    val next = selectedIndices.value.toMutableSet()
                                    if (isSelected) next.remove(idx) else next.add(idx)
                                    selectedIndices.value = next
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val count = selectedIndices.value.size
                        Button(
                            onClick  = { viewModel.confirmAddItems(context, selectedIndices.value) },
                            enabled  = count > 0,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(4.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = ClosetBlack,
                                contentColor           = Color.White,
                                disabledContainerColor = ClosetSecondaryBtn,
                                disabledContentColor   = ClosetSecondary
                            )
                        ) {
                            Text(
                                text  = if (count == 1) "ADD 1 ITEM TO WARDROBE" else "ADD $count ITEMS TO WARDROBE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        TextButton(
                            onClick  = { viewModel.dismissPending() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CANCEL", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                        }
                    }
                }
            }
        }

        // ── Item Detail Sheet ───────────────────────────────────────
        if (selectedItem != null) {
            val item = selectedItem!!
            ModalBottomSheet(
                onDismissRequest = { selectedItem = null },
                containerColor   = ClosetBackground,
                shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                    AsyncImage(
                        model = item.thumbnailUri ?: item.imageUri,
                        contentDescription = item.category,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7F5F3))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(item.category, style = MaterialTheme.typography.headlineMedium, color = ClosetBlack)
                    if (item.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            item.tags.take(5).forEach { tag ->
                                Box(modifier = Modifier.clip(CircleShape).background(ClosetStatsBg).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(tag.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                                }
                            }
                        }
                    }
                    if (item.occasions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("OCCASIONS", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.occasions.joinToString(" / "), style = MaterialTheme.typography.bodyLarge, color = ClosetBlack)
                    }
                    if (item.seasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SEASONS", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.seasons.joinToString(" / "), style = MaterialTheme.typography.bodyLarge, color = ClosetBlack)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { item.id?.let { viewModel.deleteClothingItem(it) }; selectedItem = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                            Text("REMOVE FROM WARDROBE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

// ── Detected item confirmation card ──────────────────────────────────────────

@Composable
fun DetectedItemCard(
    item: ImageAnalysisResult,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ClosetCardBg else ClosetStatsBg)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) ClosetBlack else ClosetBackground),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Item info
        Column(modifier = Modifier.weight(1f)) {
            Text(item.category, style = MaterialTheme.typography.titleMedium, color = ClosetBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(item.tags.take(4).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.occasions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.occasions.take(3).joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = ClosetSecondary, maxLines = 1)
            }
        }

        // Season chips
        if (item.seasons.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.seasons.take(2).forEach { season ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ClosetStatsBg)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(season.uppercase().take(3), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// ── Wardrobe grid card ────────────────────────────────────────────────────────

@Composable
fun ClothingItemCard(item: ClothingItem, onClick: () -> Unit) {
    val hasFront = item.thumbnailUri != null
    val hasBack  = item.thumbnailBackUri != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ClosetCardBg)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0xFFF7F5F3))
            ) {
                if (hasFront || hasBack) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Front
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF7F5F3)), contentAlignment = Alignment.Center) {
                            AsyncImage(model = item.thumbnailUri ?: item.imageUri, contentDescription = "Front", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ClosetBackground.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("FRONT", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, fontSize = 8.sp)
                            }
                        }
                        Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(ClosetBorder))
                        // Back
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF7F5F3)), contentAlignment = Alignment.Center) {
                            if (hasBack) {
                                AsyncImage(model = item.thumbnailBackUri, contentDescription = "Back", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
                            } else {
                                CircularProgressIndicator(color = ClosetSecondary.copy(alpha = 0.35f), strokeWidth = 1.5.dp, modifier = Modifier.size(20.dp))
                            }
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ClosetBackground.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("BACK", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, fontSize = 8.sp)
                            }
                        }
                    }
                } else {
                    AsyncImage(model = item.imageUri, contentDescription = item.category, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                if (hasFront || hasBack) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(ClosetGold).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text("AI", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(item.category, style = MaterialTheme.typography.titleMedium, color = ClosetBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.tags.take(3).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = ClosetSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
