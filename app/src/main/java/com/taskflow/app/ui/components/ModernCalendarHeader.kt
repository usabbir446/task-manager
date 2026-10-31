package com.taskflow.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private data class MonthDayItem(
    val calendar: Calendar,
    val dayNumber: Int,
    val isCurrentMonth: Boolean
)

@Composable
fun ModernCalendarHeader(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    tasks: List<TaskEntity>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var isExpanded by remember { mutableStateOf(true) }

    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDate.timeInMillis })
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val keyFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }

    // Map scheduled dates to list of tasks
    val tasksByDate = remember(tasks) {
        val map = mutableMapOf<String, MutableList<TaskEntity>>()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        for (task in tasks) {
            val dateStamp = task.dueDate ?: task.createdAt
            cal.timeInMillis = dateStamp
            val key = fmt.format(cal.time)
            map.getOrPut(key) { mutableListOf() }.add(task)
        }
        map
    }

    val todayKey = remember { keyFormat.format(Date()) }
    val selectedKey = remember(selectedDate) { keyFormat.format(selectedDate.time) }

    // Calculate full 7-column grid days for current month
    val monthDays = remember(currentMonthCalendar.get(Calendar.YEAR), currentMonthCalendar.get(Calendar.MONTH)) {
        val year = currentMonthCalendar.get(Calendar.YEAR)
        val month = currentMonthCalendar.get(Calendar.MONTH)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
        val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month days to fill leading row
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            add(Calendar.MONTH, -1)
        }
        val prevMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startOffset = firstDayOfWeek - Calendar.SUNDAY // 0 to 6

        val list = mutableListOf<MonthDayItem>()

        // 1. Leading days from previous month
        for (i in startOffset downTo 1) {
            val dayNum = prevMaxDays - i + 1
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = prevCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, dayNum)
            }
            list.add(MonthDayItem(dayCal, dayNum, isCurrentMonth = false))
        }

        // 2. Days of the current month
        for (i in 1..maxDaysInCurrentMonth) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(MonthDayItem(dayCal, i, isCurrentMonth = true))
        }

        // 3. Trailing days from next month to complete the week rows
        val totalCells = list.size
        val trailingCount = (7 - (totalCells % 7)) % 7
        if (trailingCount > 0) {
            val nextCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.MONTH, 1)
            }
            for (i in 1..trailingCount) {
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = nextCal.timeInMillis
                    set(Calendar.DAY_OF_MONTH, i)
                }
                list.add(MonthDayItem(dayCal, i, isCurrentMonth = false))
            }
        }

        list
    }

    val allWeeks = remember(monthDays) { monthDays.chunked(7) }
    val visibleWeeks = if (isExpanded) {
        allWeeks
    } else {
        val weekWithSelected = allWeeks.find { week ->
            week.any { keyFormat.format(it.calendar.time) == selectedKey }
        } ?: allWeeks.firstOrNull()
        if (weekWithSelected != null) listOf(weekWithSelected) else emptyList()
    }

    val dayLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    // Theme adaptive colors
    val cardBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F1513), Color(0xFF070B0A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAFCFB)))
    }

    val cardBorderColor = if (isDark) {
        Brush.verticalGradient(listOf(AmoledGlowTeal.copy(alpha = 0.35f), Color(0xFF1E2825)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE2E8E5), Color(0xFFDEE5E1)))
    }

    val headerTextColor = if (isDark) Color.White else Color(0xFF121816)
    val mutedTextColor = if (isDark) AmoledMutedText else Color(0xFF6B7A75)
    val activeTeal = if (isDark) AmoledGlowTeal else Color(0xFF006B5E)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(width = 1.dp, brush = cardBorderColor, shape = RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Top Header: Month Name & Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = monthFormatter.format(currentMonthCalendar.time),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = headerTextColor
                )

                Spacer(Modifier.width(6.dp))

                // "Today" Jump Pill
                val isViewingCurrentMonth = currentMonthCalendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                        currentMonthCalendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)

                if (!isViewingCurrentMonth || selectedKey != todayKey) {
                    Surface(
                        onClick = {
                            val today = Calendar.getInstance()
                            onDateSelected(today)
                            currentMonthCalendar = Calendar.getInstance()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = activeTeal.copy(alpha = if (isDark) 0.15f else 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            activeTeal.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = activeTeal,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Month navigation arrows (< and >) & Expand/Collapse Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = currentMonthCalendar.timeInMillis
                            add(Calendar.MONTH, -1)
                        }
                        currentMonthCalendar = newCal
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = mutedTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = currentMonthCalendar.timeInMillis
                            add(Calendar.MONTH, 1)
                        }
                        currentMonthCalendar = newCal
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = mutedTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = mutedTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Weekday Column Header (SUN - SAT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = mutedTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month Grid / Week View
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            visibleWeeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { item ->
                        val itemKey = keyFormat.format(item.calendar.time)
                        val isSelected = itemKey == selectedKey
                        val isToday = itemKey == todayKey
                        val dayTasks = tasksByDate[itemKey] ?: emptyList()
                        val taskCount = dayTasks.size
                        val hasHighPriority = dayTasks.any { it.priority == 3 && !it.isCompleted }

                        // Background & border for cells with events
                        val cellBg = when {
                            isSelected -> if (isDark) AmoledGlowTeal else activeTeal
                            isToday -> if (isDark) activeTeal.copy(alpha = 0.16f) else activeTeal.copy(alpha = 0.10f)
                            taskCount > 0 -> if (isDark) Color(0xFF0F1E19) else Color(0xFFEDF7F4)
                            else -> Color.Transparent
                        }

                        val cellBorderModifier = when {
                            isSelected -> Modifier
                            isToday -> Modifier.border(
                                width = 1.dp,
                                color = activeTeal.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            taskCount > 0 -> Modifier.border(
                                width = 1.dp,
                                color = if (hasHighPriority) Color(0xFFFF5252).copy(alpha = 0.6f)
                                        else activeTeal.copy(alpha = if (isDark) 0.4f else 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            else -> Modifier
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cellBg)
                                .then(cellBorderModifier)
                                .clickable {
                                    onDateSelected(item.calendar)
                                    if (!item.isCurrentMonth) {
                                        currentMonthCalendar = Calendar.getInstance().apply {
                                            timeInMillis = item.calendar.timeInMillis
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${item.dayNumber}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday || taskCount > 0) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = when {
                                        isSelected -> if (isDark) Color(0xFF00201B) else Color.White
                                        !item.isCurrentMonth -> if (isDark) Color(0xFF384441) else Color(0xFFB4BFBC)
                                        isToday -> activeTeal
                                        taskCount > 0 -> if (isDark) Color.White else Color(0xFF0B1F19)
                                        else -> if (isDark) Color(0xFFE6ECE9) else Color(0xFF19201E)
                                    }
                                )

                                // Prominent Event Badge (Capsule with Count)
                                if (taskCount > 0) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    val badgeBg = when {
                                        isSelected -> if (isDark) Color(0xFF00201B) else Color.White
                                        hasHighPriority -> Color(0xFFFF3B30)
                                        else -> if (isDark) AmoledGlowTeal else Color(0xFF006B5E)
                                    }
                                    val badgeTextColor = when {
                                        isSelected -> if (isDark) AmoledGlowTeal else activeTeal
                                        hasHighPriority -> Color.White
                                        else -> if (isDark) Color(0xFF00201B) else Color.White
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(11.dp)
                                            .defaultMinSize(minWidth = 16.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (taskCount > 9) "9+" else "$taskCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                lineHeight = 8.5.sp
                                            ),
                                            color = badgeTextColor
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Event Preview Bar for Selected Date
        val selectedDayTasks = tasksByDate[selectedKey] ?: emptyList()
        if (selectedDayTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF101C18) else Color(0xFFEBF6F3),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (isDark) AmoledGlowTeal.copy(alpha = 0.35f) else Color(0xFF80CBC4)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Event,
                        contentDescription = null,
                        tint = activeTeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val firstTask = selectedDayTasks.first()
                    val extraCount = selectedDayTasks.size - 1
                    val timeStr = if (firstTask.dueDate != null) {
                        " • " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(firstTask.dueDate))
                    } else ""
                    val summaryText = if (extraCount > 0) {
                        "${firstTask.title}$timeStr (+${extraCount} more)"
                    } else {
                        "${firstTask.title}$timeStr"
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE0EAE6) else Color(0xFF142B23),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
