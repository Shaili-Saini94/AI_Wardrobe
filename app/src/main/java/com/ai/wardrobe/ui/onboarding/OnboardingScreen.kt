package com.ai.wardrobe.ui.onboarding

import android.Manifest
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.wardrobe.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ─── Step metadata ──────────────────────────────────────────────────────────

private val GENDER_OPTIONS = listOf("Men", "Women", "Unisex / Non-binary")

private val STYLE_OPTIONS = listOf(
    "Minimalist", "Classic / Preppy", "Streetwear", "Bohemian",
    "Business Formal", "Smart Casual", "Athleisure", "Romantic / Feminine",
    "Edgy / Alternative", "Traditional / Ethnic"
)

private val OCCASION_OPTIONS = listOf(
    "Work / Office", "Casual Everyday", "Date Night", "Gym / Sport",
    "Party / Night-out", "Festive / Wedding", "Outdoor / Travel", "Loungewear"
)

private val PALETTE_OPTIONS = listOf(
    "Neutrals"     to listOf(Color(0xFFEDE8E3), Color(0xFF9E9589), Color(0xFF3D3835)),
    "Earth Tones"  to listOf(Color(0xFFC9A97A), Color(0xFF8B6347), Color(0xFF4A3728)),
    "Pastels"      to listOf(Color(0xFFFFD6E0), Color(0xFFBDE0FE), Color(0xFFCBF3F0)),
    "Bolds"        to listOf(Color(0xFFE63946), Color(0xFF2196F3), Color(0xFF4CAF50)),
    "Monochromes"  to listOf(Color(0xFF000000), Color(0xFF666666), Color(0xFFFFFFFF)),
    "All Colors"   to listOf(Color(0xFFE63946), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFEB3B))
)

private val STEP_TITLES = listOf(
    "Tell us about\nyourself.",
    "Your style\nidentity.",
    "Where do you\nwear it?",
    "Your color\npalette.",
    "Allow camera\naccess."
)

private val STEP_SUBTITLES = listOf(
    "We'll tailor outfit suggestions just for you.",
    "Pick all that feel like you. You can always change this later.",
    "Which settings do you dress for most?",
    "What colour story does your wardrobe tell?",
    "ClosetIQ uses your camera to scan and digitise clothes instantly."
)

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onStartScanning: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermission.status) {
        if (cameraPermission.status.isGranted && state.step == 4) {
            viewModel.finishOnboarding { onStartScanning() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClosetBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.step > 0) {
                    IconButton(onClick = { viewModel.prevStep() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = ClosetBlack
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text(
                    text = "CLOSETIQ",
                    style = MaterialTheme.typography.displayMedium,
                    color = ClosetBlack
                )
                TextButton(onClick = {
                    viewModel.finishOnboarding { onSkip() }
                }) {
                    Text("SKIP", style = MaterialTheme.typography.labelSmall, color = ClosetSecondary)
                }
            }

            // ── Progress dots ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= state.step) ClosetBlack
                                else ClosetSecondary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Animated content area ─────────────────────────────────────
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    slideInHorizontally(tween(300)) { if (targetState > initialState) it else -it } +
                        fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { if (targetState > initialState) -it else it } +
                        fadeOut(tween(300))
                },
                label = "step_transition"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = STEP_TITLES[step],
                        style = MaterialTheme.typography.displayLarge,
                        color = ClosetBlack
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = STEP_SUBTITLES[step],
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = ClosetSecondary
                    )
                    Spacer(Modifier.height(28.dp))

                    when (step) {
                        0 -> GenderStep(
                            selected = state.gender,
                            onSelect = { viewModel.setGender(it) }
                        )
                        1 -> MultiSelectStep(
                            options = STYLE_OPTIONS,
                            selected = state.selectedStyles,
                            onToggle = { viewModel.toggleStyle(it) }
                        )
                        2 -> MultiSelectStep(
                            options = OCCASION_OPTIONS,
                            selected = state.selectedOccasions,
                            onToggle = { viewModel.toggleOccasion(it) }
                        )
                        3 -> PaletteStep(
                            selected = state.selectedPalette,
                            onSelect = { viewModel.setPalette(it) }
                        )
                        4 -> CameraPermissionStep(
                            isGranted = cameraPermission.status.isGranted
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // ── Primary CTA ──────────────────────────────────────
                    val isLast = step == 4
                    val ctaEnabled = when (step) {
                        0 -> state.gender.isNotBlank()
                        1 -> state.selectedStyles.isNotEmpty()
                        2 -> state.selectedOccasions.isNotEmpty()
                        3 -> state.selectedPalette.isNotBlank()
                        else -> true
                    }

                    Button(
                        onClick = {
                            when (step) {
                                4 -> {
                                    if (cameraPermission.status.isGranted) {
                                        viewModel.finishOnboarding { onStartScanning() }
                                    } else {
                                        cameraPermission.launchPermissionRequest()
                                    }
                                }
                                else -> viewModel.nextStep()
                            }
                        },
                        enabled = ctaEnabled || isLast,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClosetBlack,
                            disabledContainerColor = ClosetBlack.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = when (step) {
                                        4 -> if (cameraPermission.status.isGranted) "LET'S GO →" else "ALLOW CAMERA"
                                        3 -> "NEXT"
                                        else -> "CONTINUE"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                if (!state.saving) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isLast) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.finishOnboarding { onSkip() } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Skip for now",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClosetSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ─── Step composables ─────────────────────────────────────────────────────────

@Composable
private fun GenderStep(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GENDER_OPTIONS.forEach { option ->
            val isSelected = selected == option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ClosetBlack else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) ClosetBlack else ClosetSecondary.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (isSelected) Color.White else ClosetBlack
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(ClosetBlack, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSelectStep(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    // Wrap-around chip grid using Column + Row chunked
    val rows = options.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { option ->
                    val isSelected = option in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ClosetBlack else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) ClosetBlack else ClosetSecondary.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggle(option) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isSelected) Color.White else ClosetBlack,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // fill last row if odd number
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PaletteStep(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PALETTE_OPTIONS.forEach { (name, colors) ->
            val isSelected = selected == name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ClosetBlack else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) ClosetBlack else ClosetSecondary.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(name) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (isSelected) Color.White else ClosetBlack
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionStep(isGranted: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Camera icon illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ClosetBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("📷", fontSize = 52.sp)
        }

        if (isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A7A3A).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFF1A7A3A).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "✓  Camera access granted",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1A7A3A)
                )
            }
        }

        val features = listOf(
            "Instant clothing detection",
            "AI fabric & color analysis",
            "Style tag auto-generation"
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { feat ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ClosetBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 12.sp)
                    }
                    Text(
                        text = feat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClosetBlack
                    )
                }
            }
        }
    }
}
