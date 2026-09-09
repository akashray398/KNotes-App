package com.example.knotes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R
import com.example.knotes.ui.notes.NotesViewModel

@Composable
fun DashboardCompose(
    state: NotesViewModel.DashboardState,
    onCardClick: (String) -> Unit
) {
    val animatedProductivity by animateIntAsState(
        targetValue = state.weeklyCompletionRate,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "productivity"
    )

    val items: List<DashboardItem> = remember(state, animatedProductivity) {
        val brandPurple = Color(0xFFD0BCFF)
        val brandGold = Color(0xFFFFD54F)
        val mutedWhite = Color(0xFFE6E1E5)

        listOf(
            DashboardItem("Notes", state.totalNotes.toString(), brandPurple, R.drawable.ic_notes, "notes"),
            DashboardItem("Streak", if (state.streak > 0) "🔥 ${state.streak}d" else "0d", brandGold, R.drawable.ic_ai_assist, "streak"),
            DashboardItem("Best", "${state.highestStreak}d", brandGold, R.drawable.ic_ai_assist, "highest_streak"),
            DashboardItem("Score", "$animatedProductivity%", brandGold, R.drawable.ic_ai_assist, "score"),
            DashboardItem("Done", state.completedTasks.toString(), brandPurple, R.drawable.ic_tasks, "completed"),
            DashboardItem("Pending", state.pendingTasks.toString(), mutedWhite, R.drawable.ic_tasks, "pending"),
            DashboardItem("Due", state.dueToday.toString(), brandPurple, R.drawable.ic_reminder, "due_today"),
            DashboardItem("Overdue", state.overdue.toString(), Color(0xFFF44336), R.drawable.ic_reminder, "overdue"),
            DashboardItem("Archived", state.archivedCount.toString(), mutedWhite, R.drawable.ic_sort, "archive"),
            DashboardItem("Trash", state.trashedCount.toString(), mutedWhite, R.drawable.ic_back, "trash")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item: DashboardItem ->
                DashboardCard(item, onCardClick)
            }
        }
    }
}

data class DashboardItem(
    val label: String,
    val value: String,
    val color: Color,
    val iconRes: Int,
    val id: String
)

@Composable
fun DashboardCard(
    item: DashboardItem,
    onClick: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        label = "DashboardCardVisibility"
    ) {
        Card(
            onClick = { onClick(item.id) },
            modifier = Modifier
                .width(110.dp)
                .height(100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = CardDefaults.outlinedCardBorder(enabled = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = "${item.label} icon",
                        modifier = Modifier.size(16.dp),
                        tint = item.color
                    )
                }

                Column {
                    AnimatedContent(
                        targetState = item.value,
                        transitionSpec = {
                            (slideInVertically { height: Int -> height } + fadeIn() togetherWith
                                    slideOutVertically { height: Int -> -height } + fadeOut())
                                .using(SizeTransform(clip = false))
                        }, label = "value"
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}
