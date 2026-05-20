package com.ai.wardrobe.ui.onboarding

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ai.wardrobe.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onStartScanning: () -> Unit,
    onSkip: () -> Unit,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission to use camera", style = MaterialTheme.typography.titleMedium) },
            text = { Text("We need your permission to use your camera.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    cameraPermissionState.launchPermissionRequest()
                }) { Text("ALLOW") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("DENY") }
            }
        )
    }

    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status.isGranted) onStartScanning()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClosetBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(96.dp))

            // Heading
            Text(
                text = "Digitize Your\nWardrobe.",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                color = ClosetBlack
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-heading
            Text(
                text = "Click the photo of any cloth using camera. Our AI instantly detects fabric, color, and silhouette to create your personal lookbook.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = ClosetSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scanner interface card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(446.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE4E2E2))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400&auto=format",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // Scanner corner reticles
                val reticleColor = Color(0xCCFBF9F8)
                val cornerSize = 32.dp
                val cornerPad = 24.dp
                val strokeDp = 2.dp
                // Top-left
                Box(modifier = Modifier.size(cornerSize).align(Alignment.TopStart).padding(start = cornerPad, top = cornerPad)
                    .border(width = strokeDp, color = reticleColor, shape = RoundedCornerShape(topStart = 8.dp)))
                // Top-right
                Box(modifier = Modifier.size(cornerSize).align(Alignment.TopEnd).padding(end = cornerPad, top = cornerPad)
                    .border(width = strokeDp, color = reticleColor, shape = RoundedCornerShape(topEnd = 8.dp)))
                // Bottom-left
                Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomStart).padding(start = cornerPad, bottom = 48.dp)
                    .border(width = strokeDp, color = reticleColor, shape = RoundedCornerShape(bottomStart = 8.dp)))
                // Bottom-right
                Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomEnd).padding(end = cornerPad, bottom = 48.dp)
                    .border(width = strokeDp, color = reticleColor, shape = RoundedCornerShape(bottomEnd = 8.dp)))

                // AI Tags
                OnboardingTag(
                    text = "SILK BLOUSE",
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 56.dp, top = 90.dp),
                    dotColor = Color(0xFF775A19)
                )
                OnboardingTag(
                    text = "#OATMEAL",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp, bottom = 24.dp)
                )
                OnboardingTag(
                    text = "MINIMALIST",
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 64.dp)
                )

                // Analyzing indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = Color.White,
                        strokeWidth = 1.5.dp
                    )
                    Text(
                        text = "ANALYZING GARMENT...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary action button
            Button(
                onClick = {
                    if (cameraPermissionState.status.isGranted) onStartScanning()
                    else showPermissionDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClosetBlack)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "START SCANNING",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ALLOW CAMERA ACCESS ON NEXT SCREEN",
                style = MaterialTheme.typography.labelSmall,
                color = ClosetSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Header overlay (top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ClosetBackground.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            Text(
                text = "CLOSETIQ",
                style = MaterialTheme.typography.displayMedium,
                color = ClosetBlack
            )
            TextButton(onClick = onSkip) {
                Text(
                    text = "SKIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClosetSecondary
                )
            }
        }
    }
}

@Composable
private fun OnboardingTag(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ClosetBackground.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ClosetBlack
        )
    }
}
