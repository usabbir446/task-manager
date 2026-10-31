package com.taskflow.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.app.ui.theme.AmoledGlowTeal
import com.taskflow.app.ui.theme.AmoledMutedText

@Composable
fun ProductivityCard(
    streakDays: Int,
    completedToday: Int,
    mostProductiveWindow: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardBg = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1412),
                Color(0xFF070B0A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFAFCFB)
            )
        )
    }

    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF9F0A).copy(alpha = 0.35f),
                Color(0xFF1E2825)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF9F0A).copy(alpha = 0.3f),
                Color(0xFFE2E8E5)
            )
        )
    }

    val titleColor = if (isDark) Color.White else Color(0xFF121816)
    val mutedColor = if (isDark) AmoledMutedText else Color(0xFF6B7A75)
    val tealAccent = if (isDark) AmoledGlowTeal else Color(0xFF006B5E)
    val dividerColor = if (isDark) Color(0xFF1A2622) else Color(0xFFE6ECE9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = cardBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Flame
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF9F0A),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$streakDays-Day Streak 🔥",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    Text(
                        text = "$completedToday tasks finished today",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle Insights",
                        tint = mutedColor
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = dividerColor)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Peak Productivity Window:",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedColor
                            )
                        }
                        Text(
                            text = mostProductiveWindow,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tealAccent
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tip: Focus on high-priority tasks during your peak window for maximum efficiency.",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
