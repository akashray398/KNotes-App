package com.example.knotes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun NextUpTaskCompose(
    tasks: List<Task>,
    onTaskClick: (Int) -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Live countdown ticker loop updating every second
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Automatically detect nearest upcoming incomplete task
    val nextUpTask = remember(tasks, currentTime) {
        tasks.filter { task ->
            !task.isCompleted && getTargetTime(task) != null && (getTargetTime(task)!! > currentTime)
        }.minByOrNull { task ->
            getTargetTime(task)!!
        }
    }

    Card(
        onClick = {
            nextUpTask?.let { onTaskClick(it.id) }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        AnimatedContent(
            targetState = nextUpTask,
            transitionSpec = {
                (fadeIn() + slideInVertically { it }) togetherWith (fadeOut() + slideOutVertically { -it })
            },
            label = "NextUpTransition"
        ) { task ->
            if (task != null) {
                val targetTime = getTargetTime(task)!!
                val formattedSchedule = formatScheduleTime(targetTime)
                val remainingText = formatCountdown(targetTime, currentTime)
                val categoryText = if (task.tags.isNotEmpty()) task.tags[0] else null

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tasks),
                                    contentDescription = "Next Up",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (categoryText != null) "Next Up — $categoryText" else "Next Up",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }

                        // Countdown Pill Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = remainingText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedSchedule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            } else {
                // Empty state when no upcoming task
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No upcoming tasks 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
