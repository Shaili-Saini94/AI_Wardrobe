package com.ai.wardrobe.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.wardrobe.domain.model.CalendarEntry
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val allEntries   by viewModel.allEntries.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val entriesForDate by viewModel.entriesForDate.collectAsState()
    val savedOutfits by viewModel.savedOutfits.collectAsState()

    var showPlanSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text("Outfit Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Plan what to wear", style = MaterialTheme.typography.bodyMedium, color = ClosetBlack.copy(alpha = 0.5f))

        Spacer(Modifier.height(20.dp))

        // 14-day strip
        val today = LocalDate.now()
        val days = (0..13).map { today.plusDays(it.toLong()) }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { day ->
                val dateStr = day.format(fmt)
                val isSelected = dateStr == selectedDate
                val hasEntry = allEntries.any { it.plannedDate == dateStr }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ClosetBlack else ClosetNavBg)
                        .clickable { viewModel.selectDate(dateStr) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text  = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) ClosetBackground else ClosetBlack.copy(alpha = 0.5f)
                    )
                    Text(
                        text       = day.dayOfMonth.toString(),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (isSelected) ClosetBackground else ClosetBlack
                    )
                    if (hasEntry) {
                        Box(
                            Modifier.size(5.dp).clip(CircleShape)
                                .background(if (isSelected) ClosetBackground else ClosetGold)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayDate = try {
                val d = LocalDate.parse(selectedDate, fmt)
                "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${d.dayOfMonth}"
            } catch (e: Exception) { selectedDate }

            Text(displayDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            IconButton(onClick = { showPlanSheet = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Plan outfit", tint = ClosetGold)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (entriesForDate.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("No outfit planned — tap + to add one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClosetBlack.copy(alpha = 0.4f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entriesForDate, key = { it.id ?: 0L }) { entry ->
                    CalendarEntryCard(entry = entry, onDelete = { entry.id?.let { viewModel.deleteEntry(it) } })
                }
            }
        }
    }

    if (showPlanSheet) {
        PlanOutfitSheet(
            outfits  = savedOutfits,
            onPlan   = { outfitId, note -> viewModel.planOutfit(outfitId, note); showPlanSheet = false },
            onDismiss = { showPlanSheet = false }
        )
    }
}

@Composable
private fun CalendarEntryCard(entry: CalendarEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = ClosetNavBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = entry.outfit?.name ?: "Custom Plan",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!entry.note.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(entry.note, style = MaterialTheme.typography.bodySmall,
                        color = ClosetBlack.copy(alpha = 0.5f))
                }
                if (entry.outfit != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${entry.outfit.items.size} items · ${entry.outfit.occasionType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClosetBlack.copy(alpha = 0.4f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = ClosetBlack.copy(alpha = 0.3f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanOutfitSheet(
    outfits: List<Outfit>,
    onPlan: (Long?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    var selectedOutfit by remember { mutableStateOf<Outfit?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClosetBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ClosetBlack.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Plan an Outfit", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            if (outfits.isEmpty()) {
                Text("Save some outfits from the Stylist tab first.",
                    style = MaterialTheme.typography.bodyMedium, color = ClosetBlack.copy(alpha = 0.5f))
            } else {
                Text("Choose outfit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(outfits) { outfit ->
                        val selected = outfit == selectedOutfit
                        Card(
                            modifier  = Modifier.fillMaxWidth().clickable { selectedOutfit = outfit },
                            shape     = RoundedCornerShape(10.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = if (selected) ClosetBlack else ClosetNavBg
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Text(
                                text      = outfit.name,
                                style     = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color     = if (selected) ClosetBackground else ClosetBlack,
                                modifier  = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Note (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClosetGold,
                    focusedLabelColor  = ClosetGold
                )
            )

            Button(
                onClick  = { onPlan(selectedOutfit?.id, note.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = ClosetBlack)
            ) {
                Text("Add to Calendar", color = ClosetBackground)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
