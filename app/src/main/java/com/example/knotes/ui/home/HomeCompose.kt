package com.example.knotes.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R
import com.example.knotes.domain.model.Task
import com.example.knotes.ui.components.HomeTopBarCompose
import com.example.knotes.ui.components.NextUpTaskCompose
import com.example.knotes.ui.components.QuickFeatureHubBottomSheet
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreenCompose(
    state: HomeViewModel.HomeState,
    currentThemeMode: Int,
    isAiEnabled: Boolean,
    aiConsentGiven: Boolean,
    isCloudSyncActive: Boolean,
    onSetThemeMode: (Int) -> Unit,
    onToggleAiEnabled: (Boolean) -> Unit,
    onTriggerSync: () -> Unit,
    onToggleTaskCompletion: (Task) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToEditTask: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
) {
    var isFeatureHubOpen by remember { mutableStateOf(false) }

    val animatedProgress by animateIntAsState(
        targetValue = state.todayProgressPercent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "todayProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Branded Home Top Bar
        HomeTopBarCompose(
            state = com.example.knotes.ui.notes.NotesViewModel.DashboardState(
                totalNotes = state.totalNotes,
                pendingTasks = state.pendingTasks,
                streak = state.streak
            ),
            onOpenFeatureHub = { isFeatureHubOpen = true }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // PART 5 — TODAY'S PROGRESS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TODAY'S PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$animatedProgress%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.todayTotal > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✓ ${state.todayCompleted} completed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.todayRemaining} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No tasks planned for today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PART 6 — NEXT UP TASK
            NextUpTaskCompose(
                tasks = state.allTasks,
                onTaskClick = onNavigateToEditTask
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PART 7 — TODAY'S TASKS PREVIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onNavigateToTasks) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more),
                        contentDescription = "Go to Tasks",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (state.todayTasks.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        state.todayTasks.forEach { task ->
                            TodayTaskItemRow(
                                task = task,
                                onToggleCompletion = { onToggleTaskCompletion(task) },
                                onClick = { onNavigateToEditTask(task.id) }
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks scheduled for today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PART 8 — NOTES + TASKS SUMMARY CARDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Notes Summary Card
                Card(
                    onClick = onNavigateToNotes,
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notes),
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "${state.totalNotes}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "total notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tasks Summary Card
                Card(
                    onClick = onNavigateToTasks,
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tasks),
                                contentDescription = "Tasks",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tasks",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "${state.pendingTasks}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PART 9 — QUICK ACTION BUTTON
            Button(
                onClick = onNavigateToCreateTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Add Task",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Interactive Feature Hub Dialog / BottomSheet
    if (isFeatureHubOpen) {
        QuickFeatureHubBottomSheet(
            dashboardState = com.example.knotes.ui.notes.NotesViewModel.DashboardState(
                totalNotes = state.totalNotes,
                pendingTasks = state.pendingTasks,
                streak = state.streak
            ),
            currentThemeMode = currentThemeMode,
            isAiEnabled = isAiEnabled,
            aiConsentGiven = aiConsentGiven,
            isCloudSyncActive = isCloudSyncActive,
            onSetThemeMode = onSetThemeMode,
            onToggleAiEnabled = onToggleAiEnabled,
            onTriggerSync = onTriggerSync,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToArchive = onNavigateToArchive,
            onNavigateToTrash = onNavigateToTrash,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToAiChat = onNavigateToAiChat,
            onExportData = onExportData,
            onImportData = onImportData,
            onDismiss = { isFeatureHubOpen = false }
        )
    }
}

@Composable
private fun TodayTaskItemRow(
    task: Task,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit
) {
    val targetTime = task.deadline ?: task.reminderTime
    val timeText = if (task.isCompleted) "Completed" else if (targetTime != null) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(targetTime))
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompletion() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
