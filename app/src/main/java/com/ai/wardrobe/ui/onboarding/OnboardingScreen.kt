package com.ai.wardrobe.ui.onboarding

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
    var showPermissionDialog by remember { mutableStateOf(value = false) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission to use camera") },
            text = { Text("We need your permission to use your camera.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        cameraPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Effect to check if permission was granted after request
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status.isGranted) {
            onStartScanning()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            Text(
                text = "CLOSETIQ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            TextButton(onClick = onSkip) {
                Text("SKIP", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Headline
        Text(
            text = "Digitize Your\nWardrobe.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Point your camera at any garment. Our AI instantly detects fabric, color, and silhouette to create your personal lookbook.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Image Preview Area with Tags
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            // Using a placeholder for the garment
            AsyncImage(
                model = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?q=80&w=1000&auto=format&fit=crop",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlays (Tags)
            TagItem(
                text = "SILK BLOUSE",
                modifier = Modifier.align(Alignment.TopStart).padding(start = 32.dp, top = 64.dp),
                dotColor = Color(0xFF8B4513)
            )

            TagItem(
                text = "#OATMEAL",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp, bottom = 32.dp),
                dotColor = Color(0xFFD2B48C)
            )

            TagItem(
                text = "MINIMALIST",
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 64.dp),
                icon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(12.dp)) }
            )
            
            // Corner indicators
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Top-left
                Box(modifier = Modifier.size(24.dp).align(Alignment.TopStart).border(2.dp, Color.White, RoundedCornerShape(topStart = 8.dp)))
                // Top-right
                Box(modifier = Modifier.size(24.dp).align(Alignment.TopEnd).border(2.dp, Color.White, RoundedCornerShape(topEnd = 8.dp)))
                // Bottom-left
                Box(modifier = Modifier.size(24.dp).align(Alignment.BottomStart).border(2.dp, Color.White, RoundedCornerShape(bottomStart = 8.dp)))
                // Bottom-right
                Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).border(2.dp, Color.White, RoundedCornerShape(bottomEnd = 8.dp)))
            }

            // Bottom status
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ANALYZING GARMENT...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Scanning Button
        Button(
            onClick = {
                if (cameraPermissionState.status.isGranted) {
                    onStartScanning()
                } else {
                    showPermissionDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("START SCANNING", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer
        Text(
            text = "ALLOW CAMERA ACCESS ON NEXT SCREEN",
            style = MaterialTheme.typography.labelMedium,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TagItem(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
