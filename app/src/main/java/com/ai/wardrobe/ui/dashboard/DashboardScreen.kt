package com.ai.wardrobe.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onScanClick: () -> Unit,
    onStyleClick: () -> Unit,
    onInsightsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
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
                    Icons.Rounded.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(18.dp),
                    tint = ClosetSecondary
                )
            }
        }

        // Greeting
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 4.dp)
        ) {
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.displayLarge,
                color = ClosetBlack
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ready for a stylish day?",
                style = MaterialTheme.typography.bodyLarge,
                color = ClosetSecondary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClosetBlack,
                    contentColor   = Color.White
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CenterFocusWeak, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("SCAN ITEM", style = MaterialTheme.typography.labelLarge)
                }
            }

            Button(
                onClick = onStyleClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClosetSecondaryBtn,
                    contentColor   = ClosetBlack
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("GET STYLED", style = MaterialTheme.typography.labelLarge)
                }
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClosetSecondaryBtn,
                    contentColor   = ClosetBlack
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("PLAN WEEK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Wardrobe Pulse section
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = "Wardrobe Pulse",
                style = MaterialTheme.typography.titleMedium,
                color = ClosetBlack
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ClosetStatsBg)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ITEMS DIGITIZED",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClosetSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.itemCount.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = ClosetBlack
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ClosetBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Checkroom,
                            contentDescription = null,
                            tint = ClosetSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WardrobeSmallCard(title = "NEWEST",    item = uiState.newestItem,   modifier = Modifier.weight(1f))
                WardrobeSmallCard(title = "MOST WORN", item = uiState.mostWornItem, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Today's Look
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Look",
                    style = MaterialTheme.typography.titleMedium,
                    color = ClosetBlack
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.WbSunny, contentDescription = null, modifier = Modifier.size(14.dp), tint = ClosetGold)
                    Text(text = uiState.weather, style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?q=80&w=1000&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DashboardTag("#OFFICECHIC")
                        DashboardTag("#SPRING")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The Linen Edit",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Breathable layers for the commute and afternoon meetings.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Trends for You
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = "Trends for You",
                style = MaterialTheme.typography.titleMedium,
                color = ClosetBlack
            )

            Spacer(modifier = Modifier.height(14.dp))

            TrendCard(
                title = "Monochrome Magic",
                description = "You have 4 new ways to style your existing black pieces based on this week's runway looks."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrendCard(
                title = "Summer Transitions",
                description = "Incorporate your heavy knits into early spring evenings with these layered combinations."
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun DashboardTag(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = ClosetBlack)
    }
}

@Composable
fun WardrobeSmallCard(title: String, item: ClothingItem?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ClosetStatsBg)
            .padding(14.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            if (item != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = ClosetBlack,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            } else {
                Text("None yet", style = MaterialTheme.typography.labelSmall, color = ClosetInactive)
            }
        }
    }
}

@Composable
fun TrendCard(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ClosetCardBg)
            .padding(20.dp)
    ) {
        Column {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = ClosetGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = ClosetBlack)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyLarge, color = ClosetSecondary)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "VIEW MATCHES →",
                style = MaterialTheme.typography.labelSmall,
                color = ClosetGold,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
