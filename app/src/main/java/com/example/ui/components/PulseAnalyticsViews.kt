package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.ui.theme.*

@Composable
fun SchoolPulseCard(
    checkIns: List<CheckInRecord>,
    urgentTicketsCount: Int,
    modifier: Modifier = Modifier
) {
    val total = checkIns.size
    val positiveCount = checkIns.count { record ->
        try {
            EmotionType.valueOf(record.emotion).isPositiveOrStable
        } catch (_: Exception) { false }
    }
    val attentionCount = total - positiveCount
    val positivePercent = if (total > 0) ((positiveCount.toFloat() / total) * 100).toInt() else 100
    val attentionPercent = if (total > 0) 100 - positivePercent else 0

    val animatedPositive by animateFloatAsState(
        targetValue = positivePercent / 100f,
        label = "PositiveProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مؤشر النبض النفسي العام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "قياس فوري للحالة العاطفية لطلاب المدرسة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (positivePercent >= 70) Emerald600.copy(alpha = 0.2f) else EmotionAngry.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (positivePercent >= 70) Emerald500 else EmotionAngry
                    )
                ) {
                    Text(
                        text = if (positivePercent >= 70) "الحالة العامة: مستقرة ومطمئنة" else "تنبيه: مؤشر ضغط مرتفع",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (positivePercent >= 70) Emerald100 else EmotionAngryBg
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main KPI numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$positivePercent%",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald500
                    )
                    Text(
                        text = "في حالة إيجابية ومستقرة ($positiveCount طالب)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$attentionPercent%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (attentionPercent > 30) EmotionAngry else EmotionAnxious
                    )
                    Text(
                        text = "بحاجة إلى مساندة ($attentionCount طالب)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate800)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedPositive)
                            .background(Emerald500)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f, fill = true)
                            .background(if (attentionPercent > 30) EmotionAngry else EmotionAnxious)
                    )
                }
            }

            if (urgentTicketsCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = EmotionAngry.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmotionAngry.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = EmotionAngry,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يوجد $urgentTicketsCount طلبات دعم سري عاجلة تتطلب تواصل المرشد الطلابي",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmotionAngryBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodDistributionChart(
    checkIns: List<CheckInRecord>,
    modifier: Modifier = Modifier
) {
    val total = checkIns.size.coerceAtLeast(1)
    val countsByEmotion = remember(checkIns) {
        EmotionType.values().associateWith { emotion ->
            checkIns.count { it.emotion == emotion.name }
        }
    }

    var selectedEmotionFilter by remember { mutableStateOf<EmotionType?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "توزيع المشاعر (8 حالات عاطفية)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "انقر على أي شعور لرؤية تفاصيل الأسباب المرتبطة به",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }

                if (selectedEmotionFilter != null) {
                    TextButton(onClick = { selectedEmotionFilter = null }) {
                        Text("عرض الكل", color = Teal700)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bars for each of the 8 emotions
            EmotionType.values().forEach { emotion ->
                val count = countsByEmotion[emotion] ?: 0
                val percent = ((count.toFloat() / total) * 100).toInt()
                val isSelected = selectedEmotionFilter == emotion

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedEmotionFilter = if (isSelected) null else emotion
                        }
                        .background(if (isSelected) emotion.backgroundColor.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AbstractEmotionIcon(
                                emotion = emotion,
                                size = 24.dp,
                                tint = emotion.primaryColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = emotion.titleArabic,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        Text(
                            text = "$count طالب ($percent%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate100)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((count.toFloat() / total).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(emotion.primaryColor)
                        )
                    }

                    // If selected, display breakdown of reasons for this emotion
                    if (isSelected) {
                        val reasonsForEmotion = checkIns
                            .filter { it.emotion == emotion.name && !it.reasonCategory.isNullOrBlank() }
                            .groupBy { it.reasonCategory!! }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "أبرز أسباب شعور (${emotion.titleArabic}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (reasonsForEmotion.isEmpty()) {
                                    Text(
                                        text = "لم يحدد الطلاب أسباباً خاصة أو فضّلوا كتمانها.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                } else {
                                    reasonsForEmotion.take(3).forEach { (reason, countReason) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• $reason",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate700
                                            )
                                            Text(
                                                text = "$countReason مرات",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Teal700
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopStressorsAndContextsCard(
    checkIns: List<CheckInRecord>,
    modifier: Modifier = Modifier
) {
    val totalWithReason = checkIns.count { !it.reasonCategory.isNullOrBlank() }.coerceAtLeast(1)
    val groupedReasons = remember(checkIns) {
        checkIns.filter { !it.reasonCategory.isNullOrBlank() }
            .groupBy { it.reasonCategory!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "مجالات الضغط والمؤثرات المدرسية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = "تحليل القطاعات الأكثر تأثيراً على نفسية الطلاب في الدورة الحالية",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (groupedReasons.isEmpty()) {
                Text(
                    text = "لا توجد أسباب مسجلة بعد في هذه الدورة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            } else {
                groupedReasons.take(5).forEach { (reason, count) ->
                    val percent = ((count.toFloat() / totalWithReason) * 100).toInt()
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800
                            )
                            Text(
                                text = "$count طالب ($percent%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Teal800
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Slate100)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((count.toFloat() / totalWithReason).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Teal600)
                            )
                        }
                    }
                }
            }
        }
    }
}
