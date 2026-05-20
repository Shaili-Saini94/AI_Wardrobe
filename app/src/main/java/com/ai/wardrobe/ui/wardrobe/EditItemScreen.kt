package com.ai.wardrobe.ui.wardrobe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.StyleProfile
import com.ai.wardrobe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: ClothingItem,
    onSave: (ClothingItem) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(item.category) }
    var tagsText by remember { mutableStateOf(item.tags.joinToString(", ")) }
    var dominantColor by remember { mutableStateOf(item.dominantColor ?: "") }

    val allOccasions = StyleProfile.ALL_OCCASIONS
    val allSeasons   = listOf("Spring", "Summer", "Autumn", "Winter")
    val allStyles    = StyleProfile.ALL_STYLES

    val selectedOccasions = remember { mutableStateSetOf<String>().also { it.addAll(item.occasions) } }
    val selectedSeasons   = remember { mutableStateSetOf<String>().also { it.addAll(item.seasons) } }
    val selectedStyles    = remember { mutableStateSetOf<String>().also { it.addAll(item.styleTypes) } }

    val allCategories = listOf("Dress", "Hat", "Jacket", "Jeans", "Kurta", "Lehenga",
        "Pants", "Sandals", "Saree", "Shirt", "Shoes", "Shorts", "Sweater", "T-Shirt")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClosetBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Item", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = ClosetBlack)
                    }
                    IconButton(onClick = {
                        onSave(
                            item.copy(
                                category      = category,
                                tags          = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                occasions     = selectedOccasions.toList(),
                                seasons       = selectedSeasons.toList(),
                                styleTypes    = selectedStyles.toList(),
                                dominantColor = dominantColor.ifBlank { null }
                            )
                        )
                    }) {
                        Icon(Icons.Rounded.Done, contentDescription = "Save", tint = ClosetGold)
                    }
                }
            }

            // Category picker
            SectionLabel("Category")
            SingleChoiceChips(
                options = allCategories,
                selected = category,
                onSelect = { category = it }
            )

            // Dominant color
            SectionLabel("Color")
            OutlinedTextField(
                value = dominantColor,
                onValueChange = { dominantColor = it },
                label = { Text("e.g. Navy, White, Beige") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClosetGold,
                    focusedLabelColor  = ClosetGold
                )
            )

            // Tags
            SectionLabel("Tags (comma-separated)")
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("e.g. casual, cotton, slim-fit") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClosetGold,
                    focusedLabelColor  = ClosetGold
                )
            )

            // Occasions
            SectionLabel("Occasions")
            MultiChoiceChips(options = allOccasions, selected = selectedOccasions)

            // Seasons
            SectionLabel("Seasons")
            MultiChoiceChips(options = allSeasons, selected = selectedSeasons)

            // Styles
            SectionLabel("Style Types")
            MultiChoiceChips(options = allStyles, selected = selectedStyles)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold, color = ClosetBlack.copy(alpha = 0.6f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick  = { onSelect(option) },
                label    = { Text(option, style = MaterialTheme.typography.labelSmall) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ClosetBlack,
                    selectedLabelColor     = ClosetBackground
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiChoiceChips(options: List<String>, selected: MutableSet<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick  = { if (option in selected) selected.remove(option) else selected.add(option) },
                label    = { Text(option, style = MaterialTheme.typography.labelSmall) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ClosetGold,
                    selectedLabelColor     = ClosetBackground
                )
            )
        }
    }
}
