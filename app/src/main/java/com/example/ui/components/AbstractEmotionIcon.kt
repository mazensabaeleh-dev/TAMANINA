package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.EmotionType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas-based abstract geometric iconography for each of the 8 emotional states.
 * Replaces generic emojis with dignified, aesthetic, balanced geometric symbols.
 */
@Composable
fun AbstractEmotionIcon(
    emotion: EmotionType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = emotion.primaryColor
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val strokeWidth = (w * 0.08f).coerceAtLeast(3f)

        when (emotion) {
            EmotionType.JOY -> {
                // Radiating harmonious starburst / sun rays
                val coreRadius = w * 0.2f
                drawCircle(
                    color = tint,
                    radius = coreRadius,
                    center = center
                )
                val rayCount = 8
                val innerR = w * 0.28f
                val outerR = w * 0.44f
                for (i in 0 until rayCount) {
                    val angle = (i * (360f / rayCount)) * (Math.PI / 180.0)
                    val start = Offset(
                        (center.x + innerR * cos(angle)).toFloat(),
                        (center.y + innerR * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        (center.x + outerR * cos(angle)).toFloat(),
                        (center.y + outerR * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = tint,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            EmotionType.CALM -> {
                // Serene parallel harmonic horizontal sinusoidal waves
                val waveY1 = h * 0.32f
                val waveY2 = h * 0.50f
                val waveY3 = h * 0.68f

                listOf(waveY1, waveY2, waveY3).forEachIndexed { index, yPos ->
                    val path = Path().apply {
                        moveTo(w * 0.15f, yPos)
                        cubicTo(
                            w * 0.35f, yPos - (h * 0.09f),
                            w * 0.65f, yPos + (h * 0.09f),
                            w * 0.85f, yPos
                        )
                    }
                    drawPath(
                        path = path,
                        color = tint.copy(alpha = if (index == 1) 1f else 0.75f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            EmotionType.SAD -> {
                // Gentle falling arc droplets / calm weeping curves
                val path = Path().apply {
                    moveTo(center.x, h * 0.18f)
                    cubicTo(
                        w * 0.82f, h * 0.55f,
                        w * 0.68f, h * 0.82f,
                        center.x, h * 0.82f
                    )
                    cubicTo(
                        w * 0.32f, h * 0.82f,
                        w * 0.18f, h * 0.55f,
                        center.x, h * 0.18f
                    )
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Gentle inner ripple
                drawCircle(
                    color = tint.copy(alpha = 0.5f),
                    radius = w * 0.12f,
                    center = Offset(center.x, h * 0.62f)
                )
            }

            EmotionType.ANXIOUS -> {
                // Intertwined tangled labyrinth curves / vibration spirals
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.5f)
                    cubicTo(w * 0.25f, h * 0.2f, w * 0.75f, h * 0.2f, w * 0.8f, h * 0.5f)
                    cubicTo(w * 0.85f, h * 0.8f, w * 0.35f, h * 0.8f, w * 0.35f, h * 0.5f)
                    cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, h * 0.35f, w * 0.65f, h * 0.5f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Small dynamic tension dots
                drawCircle(tint, radius = strokeWidth * 0.9f, center = Offset(w * 0.2f, h * 0.5f))
                drawCircle(tint, radius = strokeWidth * 0.9f, center = Offset(w * 0.65f, h * 0.5f))
            }

            EmotionType.EXHAUSTED -> {
                // Fading horizontal step levels / sinking battery balance
                val levels = listOf(0.3f, 0.45f, 0.6f, 0.75f)
                levels.forEachIndexed { i, yFraction ->
                    val startX = w * (0.2f + (i * 0.05f))
                    val endX = w * (0.8f - (i * 0.05f))
                    drawLine(
                        color = tint.copy(alpha = 1f - (i * 0.22f)),
                        start = Offset(startX, h * yFraction),
                        end = Offset(endX, h * yFraction),
                        strokeWidth = strokeWidth * 1.1f,
                        cap = StrokeCap.Round
                    )
                }
            }

            EmotionType.BORED -> {
                // Minimalist balanced horizon lines with floating observer dot
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.5f),
                    end = Offset(w * 0.85f, h * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint.copy(alpha = 0.5f),
                    start = Offset(w * 0.3f, h * 0.66f),
                    end = Offset(w * 0.7f, h * 0.66f),
                    strokeWidth = strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = strokeWidth * 1.3f,
                    center = Offset(center.x, h * 0.32f)
                )
            }

            EmotionType.ANGRY -> {
                // Sharp dynamic lightning / jagged energy peaks
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.65f)
                    lineTo(w * 0.38f, h * 0.25f)
                    lineTo(w * 0.5f, h * 0.55f)
                    lineTo(w * 0.68f, h * 0.2f)
                    lineTo(w * 0.8f, h * 0.75f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth * 1.1f, cap = StrokeCap.Round)
                )
            }

            EmotionType.FEAR -> {
                // Protective nested geometric shield
                val path = Path().apply {
                    moveTo(center.x, h * 0.15f)
                    lineTo(w * 0.8f, h * 0.32f)
                    lineTo(w * 0.7f, h * 0.75f)
                    lineTo(center.x, h * 0.88f)
                    lineTo(w * 0.3f, h * 0.75f)
                    lineTo(w * 0.2f, h * 0.32f)
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = tint,
                    radius = strokeWidth * 1.1f,
                    center = center
                )
            }
        }
    }
}
