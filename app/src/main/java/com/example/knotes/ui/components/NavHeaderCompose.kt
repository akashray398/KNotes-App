package com.example.knotes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R

@Composable
fun NavHeaderCompose(
    displayName: String,
    email: String,
    streak: Int,
    isSignedIn: Boolean,
    onHeaderClick: () -> Unit
) {
    // Breathing aura animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "NavHeaderAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() },
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo Badge + Streak Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "KNotes Logo",
                                modifier = Modifier.size(28.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }

                // Streak Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (streak > 0) Color(0xFFFFD54F).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (streak > 0) "🔥 ${streak}d Streak" else "⚡ Pro Workspace",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (streak > 0) Color(0xFFF57F17) else MaterialTheme.colorScheme.primary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Info
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (isSignedIn) email else "Tap to sign in & enable Cloud Sync ☁️",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
