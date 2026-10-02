package com.example.knotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R
import com.example.knotes.domain.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TasksHeaderCompose(
    completedCount: Int,
    pendingCount: Int,
    progressPercent: Int,
    tasks: List<Task>,
    onTaskClick: (Int) -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val nextUpTask = remember(tasks, currentTime) {
        tasks.filter { task ->
            !task.isCompleted && getTargetTime(task) != null && (getTargetTime(task)!! > currentTime)
        }.minByOrNull { task ->
            getTargetTime(task)!!
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Progress Ring & Motivation on Left, Next Up Countdown on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress Progress Gauge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(54.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier.size(54.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = when {
                                progressPercent >= 100 -> "All Done! 🎉"
                                progressPercent >= 50 -> "In the Flow! 🔥"
                                else -> "Daily Focus 🎯"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$pendingCount pending • $completedCount completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Next-Up Countdown Pill
                if (nextUpTask != null) {
                    val targetTime = getTargetTime(nextUpTask)!!
                    val remainingText = formatCountdown(targetTime, currentTime)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = remainingText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Bottom Section: Next-Up Task Title Banner
            if (nextUpTask != null) {
                val targetTime = getTargetTime(nextUpTask)!!
                val formattedSchedule = formatScheduleTime(targetTime)
                val categoryText = if (nextUpTask.tags.isNotEmpty()) nextUpTask.tags[0] else null

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = { onTaskClick(nextUpTask.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tasks),
                                    contentDescription = "Next Up",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = nextUpTask.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (categoryText != null) "$categoryText • $formattedSchedule" else formattedSchedule,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "Open task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getTargetTime(task: Task): Long? {
    return task.reminderTime ?: task.deadline
}

private fun formatScheduleTime(targetTime: Long): String {
    val calTarget = Calendar.getInstance().apply { timeInMillis = targetTime }
    val calNow = Calendar.getInstance()

    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())

    val isToday = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                  calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

    calNow.add(Calendar.DAY_OF_YEAR, 1)
    val isTomorrow = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                     calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

    return when {
        isToday -> "Today at ${sdfTime.format(Date(targetTime))}"
        isTomorrow -> "Tomorrow at ${sdfTime.format(Date(targetTime))}"
        else -> {
            val sdfDate = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
            sdfDate.format(Date(targetTime))
        }
    }
}

private fun formatCountdown(targetTime: Long, currentTime: Long): String {
    val diffMillis = targetTime - currentTime
    if (diffMillis <= 0) return "⏰ Due now"

    val seconds = (diffMillis / 1000) % 60
    val minutes = (diffMillis / (1000 * 60)) % 60
    val hours = (diffMillis / (1000 * 60 * 60)) % 24
    val days = diffMillis / (1000 * 60 * 60 * 24)

    return when {
        days > 0 -> "⏰ ${days}d ${hours}h remaining"
        hours > 0 -> "⏰ ${hours}h ${minutes}m remaining"
        minutes > 0 -> "⏰ ${minutes}m ${seconds}s remaining"
        else -> "⏰ ${seconds}s remaining"
    }
}
