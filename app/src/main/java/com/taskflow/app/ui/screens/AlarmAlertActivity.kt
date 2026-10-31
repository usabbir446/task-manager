package com.taskflow.app.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.local.AppDatabase
import com.taskflow.app.data.repository.TaskRepository
import com.taskflow.app.service.AlarmService
import com.taskflow.app.ui.theme.TaskFlowTheme
import com.taskflow.app.util.TaskAlarmScheduler
import com.taskflow.app.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen, unlock keyguard and show directly on top of lockscreen/current app
        turnScreenOnAndKeyguard()

        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        val taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

        setContent {
            TaskFlowTheme {
                AlarmAlertScreen(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    onDismiss = {
                        AlarmService.stopAlarm(this)
                        finish()
                    },
                    onMarkDone = {
                        AlarmService.stopAlarm(this)
                        if (taskId != -1L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = AppDatabase.getDatabase(this@AlarmAlertActivity)
                                val repo = TaskRepository(db.taskDao())
                                val task = repo.getTaskById(taskId)
                                if (task != null) {
                                    repo.toggleTaskCompletion(task)
                                    TaskWidgetProvider.updateAllWidgets(this@AlarmAlertActivity)
                                }
                            }
                        }
                        finish()
                    },
                    onSnooze = {
                        AlarmService.stopAlarm(this)
                        if (taskId != -1L) {
                            val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000L)
                            TaskAlarmScheduler.scheduleTaskReminder(
                                this@AlarmAlertActivity, taskId, taskTitle, snoozeTime
                            )
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        // Window flags ensuring screen turns on and stays bright over everything
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }
}

@Composable
fun AlarmAlertScreen(
    taskId: Long,
    taskTitle: String,
    onDismiss: () -> Unit,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF003730),
                        Color(0xFF006B5E),
                        Color(0xFF0F1413)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Alarm Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(scale)
                        .background(Color(0xFF5DDBC4).copy(alpha = 0.35f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = "Alarm",
                    tint = Color(0xFF7CF8E0),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "🔔 TASK DUE NOW",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF7CF8E0),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = taskTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap an action below to stop alarm",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(48.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Snooze 5m
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Snooze, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Snooze 5m", fontWeight = FontWeight.SemiBold)
                }

                // Mark Done
                Button(
                    onClick = onMarkDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5DDBC4),
                        contentColor = Color(0xFF003730)
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dismiss Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(8.dp))
                Text("Dismiss Alarm", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
