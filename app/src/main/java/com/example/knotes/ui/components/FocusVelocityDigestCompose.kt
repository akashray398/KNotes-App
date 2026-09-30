package com.example.knotes.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R
import com.example.knotes.ui.home.HomeViewModel
import com.example.knotes.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

enum class EnergyLevel(val label: String, val icon: String) {
    HIGH("High Energy", "⚡"),
    DEEP("Deep Focus", "🧠"),
    QUICK("Quick Wins", "☕")
}

@Composable
fun FocusVelocityDigestCompose(
    state: HomeViewModel.HomeState,
    selectedEnergyLevel: EnergyLevel?,
    onEnergyLevelSelected: (EnergyLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocusTimerActive by remember { mutableStateOf(false) }
    var remainingTimerSeconds by remember { mutableIntStateOf(15 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Live Ticker for Focus Burst Timer
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && remainingTimerSeconds > 0 && isActive) {
            delay(1000L)
            remainingTimerSeconds--
        }
        if (remainingTimerSeconds == 0) {
            isTimerRunning = false
        }
    }

    // Velocity Score Calculation (0 - 100%)
    val velocityScore = remember(state) {
        val progressComponent = (state.todayProgressPercent * 0.5f).toInt()
        val streakComponent = minOf(state.streak * 10, 30)
        val noteComponent = if (state.totalNotes > 0) 20 else 0
        minOf(progressComponent + streakComponent + noteComponent, 100)
    }

    val velocityStatus = when {
        velocityScore >= 75 -> "⚡ High Momentum"
        velocityScore >= 45 -> "🎯 Steady Flow"
        else -> "☕ Warm-Up Mode"
    }

    val velocityColor = when {
        velocityScore >= 75 -> Color(0xFF4CAF50)
        velocityScore >= 45 -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFFF9800)
    }

    // AI Daily Focus Digest Banner Message
    val aiDigestMessage = remember(state) {
        when {
            state.streak >= 3 && state.todayRemaining > 0 ->
                "🔥 ${state.streak}-day streak active! You have ${state.todayRemaining} tasks planned today. Recommended focus window: Morning."
            state.todayRemaining > 0 ->
                "💡 You have ${state.todayRemaining} pending tasks for today. Start with quick wins or high-priority items to build momentum."
            state.todayCompleted > 0 ->
                "🎉 Fantastic work! You've completed all planned tasks for today. Take time to review notes or unwind."
            else ->
                "✨ Welcome to your workspace! Capture a new note or schedule a task to kickstart your daily velocity."
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize()
        ) {
            // Header Row: AI Digest Sparkle + Velocity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai_assist),
                        contentDescription = "AI Digest",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Focus Digest & Velocity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = velocityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = velocityStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = velocityColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Smart Digest Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = aiDigestMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Productivity Velocity Score Gauge Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocity Output Score",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$velocityScore%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = velocityColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { velocityScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = velocityColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mindful Energy Filter Chips
            Text(
                text = "How is your energy level right now?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnergyLevel.entries.forEach { level ->
                    val isSelected = selectedEnergyLevel == level
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onEnergyLevelSelected(if (isSelected) null else level)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = level.icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = level.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 15-Min Focus Burst Timer Section
            if (!isFocusTimerActive) {
                OutlinedButton(
                    onClick = {
                        isFocusTimerActive = true
                        isTimerRunning = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_reminder),
                        contentDescription = "Focus Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start 15-Min Focus Burst 🎯",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "🎯 FOCUS BURST ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val minutes = remainingTimerSeconds / 60
                            val seconds = remainingTimerSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = { isTimerRunning = !isTimerRunning },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(if (isTimerRunning) "⏸" else "▶", fontSize = 14.sp)
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    isTimerRunning = false
                                    isFocusTimerActive = false
                                    remainingTimerSeconds = 15 * 60
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("✕", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
