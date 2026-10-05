package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class BreathingPhase(val titleArabic: String, val instructionArabic: String, val seconds: Int) {
    INHALE("شهيق عميق", "املأ رئتيك بهواء نقي ببطء وهدوء...", 4),
    HOLD("حبس النفس", "ابقَ ثابتاً، اشعر بالسكينة والسلام الداخلي...", 4),
    EXHALE("زفير مريح", "أخرج الهواء بلطف وتخلص من كل التوتر...", 4)
}

@Composable
fun BreathingExerciseModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var secondsRemaining by remember { mutableIntStateOf(4) }
    var completedCycles by remember { mutableIntStateOf(1) }
    var isRunning by remember { mutableStateOf(true) }

    // Haptic feedback trigger
    fun triggerHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.os.VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            for (phase in BreathingPhase.values()) {
                currentPhase = phase
                triggerHaptic()
                for (sec in phase.seconds downTo 1) {
                    secondsRemaining = sec
                    delay(1000L)
                }
            }
            completedCycles++
        }
    }

    // Animation scale for the circle
    val targetScale = when (currentPhase) {
        BreathingPhase.INHALE -> 1.35f
        BreathingPhase.HOLD -> 1.35f
        BreathingPhase.EXHALE -> 0.85f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (currentPhase == BreathingPhase.HOLD) 400 else 3800,
            easing = FastOutSlowInEasing
        ),
        label = "BreathingScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            color = Slate900,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Slate400
                        )
                    }

                    Text(
                        text = "تمرين التنفس الهادئ (4-4-4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "الدورة $completedCycles",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal200
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Interactive Animated Visual
                Box(
                    modifier = Modifier
                        .size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer soothing glow ring
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(animatedScale * 1.15f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Teal500.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Middle animated breathing sphere
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Teal400,
                                        Teal700,
                                        Teal900
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$secondsRemaining",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ثواني",
                                style = MaterialTheme.typography.labelSmall,
                                color = Teal100
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Phase Title and reassuring instruction
                Surface(
                    color = Teal900.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentPhase.titleArabic,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Teal200
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentPhase.instructionArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate200,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isRunning = !isRunning
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Slate200
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "إيقاف مؤقت" else "استئناف")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("أشعر بتحسن الآن")
                    }
                }
            }
        }
    }
}
