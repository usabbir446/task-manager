package com.taskflow.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.local.TaskEntity
import com.taskflow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernCalendarHeader(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    tasks: List<TaskEntity>,
    modifier: Modifier = Modifier
) {
    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDate.timeInMillis })
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayOfWeekFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    // Generate dates for current week or month window
    val daysInMonth = remember(currentMonthCalendar.get(Calendar.YEAR), currentMonthCalendar.get(Calendar.MONTH)) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentMonthCalendar.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Calendar>()
        for (i in 1..maxDays) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(dayCal)
        }
        list
    }

    // Map dates to task count
    val taskDateMap = remember(tasks) {
        val map = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance()
        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        for (task in tasks) {
            val dateStamp = task.dueDate ?: task.createdAt
            cal.timeInMillis = dateStamp
            val key = keyFormat.format(cal.time)
            map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    val keyFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val isTodayKey = remember { keyFormat.format(Date()) }
    val selectedKey = remember(selectedDate) { keyFormat.format(selectedDate.time) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1614),
                        Color(0xFF070B0A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AmoledGlowTeal.copy(alpha = 0.35f),
                        Color(0xFF1E2825)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(16.dp)
    ) {
        // Month and Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = monthFormatter.format(currentMonthCalendar.time),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )

                // Today Pill Button
                if (selectedKey != isTodayKey) {
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = {
                            val today = Calendar.getInstance()
                            onDateSelected(today)
                            currentMonthCalendar = Calendar.getInstance()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = AmoledGlowTeal.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmoledGlowTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmoledGlowTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Month navigation arrows
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = currentMonthCalendar.timeInMillis
                            add(Calendar.MONTH, -1)
                        }
                        currentMonthCalendar = newCal
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = AmoledMutedText,
                        modifier = Modifier.size(16.dp)
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = AmoledMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Scrollable Day Strip
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysInMonth, key = { it.timeInMillis }) { dayCal ->
                val dayKey = keyFormat.format(dayCal.time)
                val isSelected = dayKey == selectedKey
                val isToday = dayKey == isTodayKey
                val taskCount = taskDateMap[dayKey] ?: 0

                val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
                val dayOfWeek = dayOfWeekFormatter.format(dayCal.time)

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onDateSelected(dayCal) }
                        .background(
                            when {
                                isSelected -> Brush.verticalGradient(
                                    colors = listOf(AmoledGlowTeal, Color(0xFF00BFA5))
                                )
                                isToday -> Brush.verticalGradient(
                                    colors = listOf(Color(0xFF142421), Color(0xFF0A1412))
                                )
                                else -> Brush.verticalGradient(
                                    colors = listOf(Color(0xFF101615), Color(0xFF0C100F))
                                )
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.5.dp,
                            color = when {
                                isSelected -> AmoledGlowTeal
                                isToday -> AmoledGlowTeal.copy(alpha = 0.5f)
                                else -> Color(0xFF222B29)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayOfWeek.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isSelected) Color(0xFF00201B) else AmoledMutedText
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "$dayNumber",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = if (isSelected) Color(0xFF00201B) else Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Task indicator dot
                        if (taskCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFF00201B) else AmoledGlowTeal
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                    }
                }
            }
        }
    }
}
