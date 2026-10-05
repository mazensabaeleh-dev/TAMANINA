package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.data.model.InitiativeAnalyzer
import com.example.data.model.SchoolCycle
import com.example.data.model.SchoolInitiative
import com.example.data.model.SupportTicket
import com.example.data.model.TeamMember
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CounselorDashboardScreen(
    schoolName: String,
    checkIns: List<CheckInRecord>,
    tickets: List<SupportTicket>,
    cycles: List<SchoolCycle>,
    activeCycle: SchoolCycle?,
    isPulseActive: Boolean,
    teamMembers: List<TeamMember>,
    currentLoggedInMember: TeamMember?,
    onTogglePulse: (Boolean) -> Unit,
    onUpdateSchoolName: (String) -> Unit,
    onUpdateTicketStatus: (Long, String, String?) -> Unit,
    onDeleteTicket: (Long) -> Unit,
    onStartNewCycle: (String, String?) -> Unit,
    onPurgeOldData: (Int) -> Unit,
    onClearAllData: () -> Unit,
    onGenerateCsv: (List<CheckInRecord>) -> String,
    onAddTeamMember: (String, String, String, String) -> Unit,
    onDeleteTeamMember: (Long) -> Unit,
    onSwitchActiveMember: (TeamMember) -> Unit = {},
    onLogout: () -> Unit,
    onSwitchToStudentMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentRole by remember { mutableStateOf(currentLoggedInMember?.role ?: "المرشد الطلابي") }

    val urgentTicketsCount = tickets.count { it.status == "NEW" && (it.urgency == "URGENT" || it.urgency == "CRITICAL") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Dashboard Top Bar
        Surface(
            color = Slate900,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Teal700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لوحة المرشد الطلابي والإدارة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentLoggedInMember?.name?.let { "$it • ${currentLoggedInMember.role}" }
                                    ?: (activeCycle?.title ?: "الدورة الحالية"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Teal200,
                                maxLines = 1
                            )
                        }
                    }

                    // Switch back to Student Mode
                    FilledTonalButton(
                        onClick = onSwitchToStudentMode,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Teal800,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("واجهة الطالب", fontSize = 12.sp)
                    }
                }
            }
        }

        // Navigation Tabs (6 Tabs)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Teal800,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("نبض المدرسة", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("المبادرات المقترحة", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmotionJoy) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("طلبات الدعم", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        if (urgentTicketsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = EmotionAngry) {
                                Text("$urgentTicketsCount", color = Color.White)
                            }
                        }
                    }
                },
                icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("فريق الدعم", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = Teal100) {
                            Text("${teamMembers.size}", color = Teal900)
                        }
                    }
                },
                icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("رمز QR والبوستر", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                text = { Text("البيانات والتصدير والخصوصية", fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> PulseOverviewTab(
                    checkIns = checkIns,
                    urgentTicketsCount = urgentTicketsCount,
                    schoolName = schoolName,
                    activeCycle = activeCycle,
                    onNavigateToInitiatives = { selectedTab = 1 }
                )
                1 -> ProposedInitiativesTab(
                    checkIns = checkIns,
                    schoolName = schoolName
                )
                2 -> SupportTicketsTab(
                    tickets = tickets,
                    currentLoggedInMember = currentLoggedInMember,
                    onUpdateTicketStatus = onUpdateTicketStatus,
                    onDeleteTicket = onDeleteTicket
                )
                3 -> SupportTeamTab(
                    teamMembers = teamMembers,
                    currentLoggedInMember = currentLoggedInMember,
                    onAddTeamMember = onAddTeamMember,
                    onDeleteTeamMember = onDeleteTeamMember,
                    onSwitchActiveMember = onSwitchActiveMember,
                    onLogout = onLogout
                )
                4 -> SchoolPosterGeneratorView(
                    schoolName = schoolName,
                    isPulseActive = isPulseActive,
                    onTogglePulse = onTogglePulse,
                    onUpdateSchoolName = onUpdateSchoolName
                )
                5 -> DataExportAndPrivacyTab(
                    cycles = cycles,
                    activeCycle = activeCycle,
                    currentRole = currentRole,
                    onRoleChange = { currentRole = it },
                    onStartNewCycle = onStartNewCycle,
                    onPurgeOldData = onPurgeOldData,
                    onClearAllData = onClearAllData,
                    onGenerateCsv = onGenerateCsv,
                    checkIns = checkIns,
                    schoolName = schoolName,
                    currentLoggedInMember = currentLoggedInMember
                )
            }
        }
    }
}

@Composable
private fun PulseOverviewTab(
    checkIns: List<CheckInRecord>,
    urgentTicketsCount: Int,
    schoolName: String,
    activeCycle: SchoolCycle?,
    onNavigateToInitiatives: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Time filter states
    var timeFilter by remember { mutableStateOf("MONTH") } // TODAY, WEEK, MONTH, ALL, CUSTOM
    var customDays by remember { mutableFloatStateOf(14f) }

    val now = remember { System.currentTimeMillis() }
    val filteredCheckIns = remember(checkIns, timeFilter, customDays) {
        val cutoff = when (timeFilter) {
            "TODAY" -> now - (24 * 60 * 60 * 1000L)
            "WEEK" -> now - (7 * 24 * 60 * 60 * 1000L)
            "MONTH" -> now - (30 * 24 * 60 * 60 * 1000L)
            "CUSTOM" -> now - (customDays.toLong() * 24 * 60 * 60 * 1000L)
            else -> 0L
        }
        if (cutoff == 0L) checkIns else checkIns.filter { it.timestamp >= cutoff }
    }

    // Dominant Emotion Calculation
    val dominantEmotionInfo = remember(filteredCheckIns) {
        if (filteredCheckIns.isEmpty()) null
        else {
            val counts = mutableMapOf<EmotionType, Int>()
            for (rec in filteredCheckIns) {
                try {
                    val emo = EmotionType.valueOf(rec.emotion)
                    counts[emo] = (counts[emo] ?: 0) + 1
                } catch (_: Exception) {}
            }
            val dominant = counts.maxByOrNull { it.value }
            if (dominant != null) {
                val percent = (dominant.value * 100) / filteredCheckIns.size
                Triple(dominant.key, dominant.value, percent)
            } else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Time Period Filter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Teal700, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تحديد الفترة الزمنية للتحليل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                    Text(
                        text = "${filteredCheckIns.size} استجابة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Teal800
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "TODAY" to "اليوم (24 س)",
                        "WEEK" to "أسبوع (7 أيام)",
                        "MONTH" to "شهر (30 يوماً)",
                        "ALL" to "كامل الفترة",
                        "CUSTOM" to "مخصص"
                    ).forEach { (code, title) ->
                        val isSel = timeFilter == code
                        FilterChip(
                            selected = isSel,
                            onClick = { timeFilter = code },
                            label = { Text(title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal700,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (timeFilter == "CUSTOM") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اختر عدد الأيام السابقة: ${customDays.toInt()} يوماً",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                    Slider(
                        value = customDays,
                        onValueChange = { customDays = it },
                        valueRange = 1f..90f,
                        steps = 89,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dominant Emotion & Quick Initiatives Teaser Card
        dominantEmotionInfo?.let { (emotion, count, percent) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = emotion.emoji,
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الشعور السائد خلال هذه الفترة:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate300
                                )
                                Text(
                                    text = "${emotion.arabicTitle} بنسبة $percent% ($count طالب)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToInitiatives,
                            colors = ButtonDefaults.buttonColors(containerColor = EmotionJoy),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate900)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المبادرات المقترحة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // School Pulse KPI Card
        SchoolPulseCard(
            checkIns = filteredCheckIns,
            urgentTicketsCount = urgentTicketsCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mood Distribution Breakdown (8 Emotions)
        MoodDistributionChart(checkIns = filteredCheckIns)

        Spacer(modifier = Modifier.height(16.dp))

        // Top Stressors & Contexts Card
        TopStressorsAndContextsCard(checkIns = filteredCheckIns)

        Spacer(modifier = Modifier.height(16.dp))

        // Export Quick Stats Button
        Button(
            onClick = {
                val total = filteredCheckIns.size
                val positive = filteredCheckIns.count {
                    try { EmotionType.valueOf(it.emotion).isPositiveOrStable } catch (_: Exception) { false }
                }
                val percent = if (total > 0) ((positive.toFloat() / total) * 100).toInt() else 100
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "📊 تقرير النبض النفسي - $schoolName\n" +
                        "الفترة الزمنية: ${when (timeFilter) { "TODAY" -> "آخر 24 ساعة"; "WEEK" -> "آخر 7 أيام"; "MONTH" -> "آخر 30 يوماً"; else -> "كامل السجلات" }}\n" +
                        "إجمالي النبضات المرصودة: $total\n" +
                        "مؤشر الاستقرار الإيجابي: $percent%\n" +
                        "الحالات التي تتطلب مساندة: ${total - positive}\n\n" +
                        "صادر من منصة طمأنينة المدرسية."
                    )
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير النبض النفسي"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("مشاركة تقرير تحليلي للفترة المحددة")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProposedInitiativesTab(
    checkIns: List<CheckInRecord>,
    schoolName: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Use current month / last 30 days check-ins for the monthly analysis
    val now = remember { System.currentTimeMillis() }
    val monthCutoff = now - (30 * 24 * 60 * 60 * 1000L)
    val monthlyCheckIns = remember(checkIns) {
        val filtered = checkIns.filter { it.timestamp >= monthCutoff }
        if (filtered.isNotEmpty()) filtered else checkIns
    }

    val (dominantEmotion, initiatives) = remember(monthlyCheckIns) {
        InitiativeAnalyzer.generateInitiatives(monthlyCheckIns, "شهر التطبيق الحالي")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header Card with AI / Algorithmic Analysis Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmotionJoy.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = EmotionJoy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "اقتراح المبادرات التربوية التوجيهية الذكية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "تحليل نتائج مشاعر الطلاب خلال الشهر واقتراح حلول ملموسة",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal200
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ملخص تحليل شهر التطبيق:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Teal300
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (dominantEmotion != null) {
                                "تم رصد أن الشعور السائد والأكثر تكراراً بين طلاب المدرسة هو «${dominantEmotion.arabicTitle} ${dominantEmotion.emoji}». بناءً على هذا النمط، تم إنشاء خطة مبادرات وقائية وتدخلية لمعالجة الأسباب وتعزيز استقرار البيئة التعليمية."
                            } else {
                                "لا توجد سجلات كافية للشهر حتى الآن. تم إدراج المبادرات الإرشادية العامة للمحافظة على التوازن النفسي."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Full Initiatives Plan Button
                Button(
                    onClick = {
                        val text = buildString {
                            append("📑 خطة المبادرات الإرشادية المقترحة - $schoolName\n")
                            append("المبنية على تحليل مشاعر الطلاب خلال شهر التطبيق\n")
                            append("الشعور السائد المرصود: ${dominantEmotion?.arabicTitle ?: "عام"}\n")
                            append("--------------------------------------------------\n\n")
                            initiatives.forEachIndexed { idx, init ->
                                append("${idx + 1}. ${init.title} (${init.priorityLevel})\n")
                                append("• الفئة المستهدفة: ${init.targetAudience} | المدة: ${init.duration}\n")
                                append("• الهدف: ${init.objective}\n")
                                append("• خطوات التنفيذ:\n")
                                init.executionSteps.forEach { step -> append("  - $step\n") }
                                append("• الأثر المتوقع: ${init.expectedImpact}\n\n")
                            }
                            append("صادر من منصة طمأنينة المدرسية.")
                        }
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "تصدير خطة المبادرات المدرسية"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير ومشاركة خطة المبادرات بالكامل للجنة التوجيه")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "المبادرات المقترحة للتطبيق بالمدرسة (${initiatives.size} مبادرات):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Initiatives Cards List
        initiatives.forEach { init ->
            InitiativeCard(
                initiative = init,
                onCopy = {
                    val formatted = "${init.title}\n${init.objective}\nخطوات التنفيذ:\n" + init.executionSteps.joinToString("\n") { "• $it" }
                    clipboardManager.setText(AnnotatedString(formatted))
                    snackbarMessage = "تم نسخ المبادرة «${init.title}» إلى الحافظة بنجاح"
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (snackbarMessage != null) {
            Surface(
                color = Slate900,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = snackbarMessage ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InitiativeCard(
    initiative: SchoolInitiative,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (initiative.priorityLevel == "أولوية قصوى") EmotionAngry.copy(alpha = 0.4f) else Slate200
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (initiative.priorityLevel == "أولوية قصوى") EmotionAngryBg else Teal50
                    ) {
                        Text(
                            text = initiative.priorityLevel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (initiative.priorityLevel == "أولوية قصوى") EmotionAngry else Teal800
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100
                    ) {
                        Text(
                            text = initiative.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate700
                        )
                    }
                }

                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ المبادرة", tint = Slate500, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = initiative.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "سبب الاقتراح: ${initiative.triggerReason}",
                style = MaterialTheme.typography.bodySmall,
                color = Teal800,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = initiative.objective,
                style = MaterialTheme.typography.bodySmall,
                color = Slate700,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "👥 الفئة: ${initiative.targetAudience}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )
                Text(
                    text = "⏳ المدة: ${initiative.duration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "خطوات التنفيذ الموصى بها:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(6.dp))

            initiative.executionSteps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${i + 1}. ",
                        fontWeight = FontWeight.Bold,
                        color = Teal700,
                        fontSize = 12.sp
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate800,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Emerald100.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎯 الأثر المتوقع: ${initiative.expectedImpact}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                )
            }
        }
    }
}

@Composable
private fun DataExportAndPrivacyTab(
    cycles: List<SchoolCycle>,
    activeCycle: SchoolCycle?,
    currentRole: String,
    onRoleChange: (String) -> Unit,
    onStartNewCycle: (String, String?) -> Unit,
    onPurgeOldData: (Int) -> Unit,
    onClearAllData: () -> Unit,
    onGenerateCsv: (List<CheckInRecord>) -> String,
    checkIns: List<CheckInRecord>,
    schoolName: String,
    currentLoggedInMember: TeamMember?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showNewCycleDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var newCycleTitle by remember { mutableStateOf("") }
    var newCycleDesc by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Download / Export to Computer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Teal700),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تنزيل وتصدير البيانات على جهاز الحاسوب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "حفظ السجلات بصيغة CSV متوافقة مع Excel وبرامج الإحصاء",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal200
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "إجمالي السجلات الجاهزة للتنزيل: ${checkIns.size} سجل",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val csvData = onGenerateCsv(checkIns)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvData)
                                putExtra(Intent.EXTRA_SUBJECT, "بيانات نبض مدرسة $schoolName - CSV")
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "تنزيل أو حفظ ملف CSV على جهازك"))
                            feedbackMessage = "تم تجهيز ملف CSV للتنزيل والحفظ على الحاسوب"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنزيل CSV للحاسوب", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val csvData = onGenerateCsv(checkIns)
                            clipboardManager.setText(AnnotatedString(csvData))
                            feedbackMessage = "تم نسخ نص بيانات CSV بالكامل إلى الحافظة"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ نص البيانات", fontSize = 11.sp)
                    }
                }

                // Sample CSV Header Preview
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "معاينة ترويسة الأعمدة:\nID, Emotion, Emotion_Arabic, Reason_Category, Optional_Note, Timestamp, Date_Time",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Teal300,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Wipe & Clear Permission Card (Admin / Owner privilege)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmotionAngry.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = EmotionAngry, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "صلاحية مسح وتطهير البيانات (Data Wipe)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmotionAngry
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = EmotionAngryBg) {
                        Text(
                            text = "للمشرف العام",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmotionAngry,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "بصفتك مالك المنصة والمشرف العام (mazensabaeleh@gmail.com)، تمتلك الصلاحية الكاملة لتصفير سجلات النبضات نهائياً عند بدء فصل دراسي جديد أو بعد أرشفة البيانات للحاسوب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearConfirmationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmotionAngry),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح كافة سجلات النبضات وإعادة التهيئة")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Cycles Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إدارة دورات البيانات الإحصائية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "الدورة النشطة: ${activeCycle?.title ?: "غير محددة"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal800
                        )
                    }

                    Button(
                        onClick = { showNewCycleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دورة جديدة", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                cycles.forEach { cycle ->
                    Surface(
                        color = if (cycle.isActive) Teal50 else Slate50,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (cycle.isActive) Teal600 else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cycle.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                if (!cycle.description.isNullOrBlank()) {
                                    Text(
                                        text = cycle.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                            }
                            if (cycle.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Teal700
                                ) {
                                    Text(
                                        text = "نشطة حالياً",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Retention Charter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ميثاق الخصوصية وحفظ البيانات (Privacy & Retention)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "وفقاً للسياسات التربوية وحماية خصوصية الطلاب، تُحفظ سجلات النبضات بدون معرفات شخصية، وتخضع لسياسة أرشفة وتطهير دوري لضمان الأمان التام.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onPurgeOldData(30)
                            feedbackMessage = "تم حذف السجلات الأقدم من 30 يوماً بنجاح"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حذف أقدم من 30 يوم", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onPurgeOldData(60)
                            feedbackMessage = "تم حذف السجلات الأقدم من 60 يوماً بنجاح"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حذف أقدم من 60 يوم", fontSize = 11.sp)
                    }
                }

                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMessage ?: "",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald700,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirmation Dialog for Clearing All Data
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmotionAngry)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد مسح كافة البيانات", fontWeight = FontWeight.Bold, color = EmotionAngry)
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف كافة سجلات النبضات وإعادة ضبط البيانات؟\nيوصى بتنزيل نسخة CSV إلى الحاسوب قبل إتمام العملية.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmationDialog = false
                        feedbackMessage = "تم مسح كافة البيانات وتصفير قاعدة البيانات بنجاح"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmotionAngry)
                ) {
                    Text("نعم، مسح البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // New Cycle Dialog
    if (showNewCycleDialog) {
        AlertDialog(
            onDismissRequest = { showNewCycleDialog = false },
            title = { Text("بدء دورة إحصائية جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCycleTitle,
                        onValueChange = { newCycleTitle = it },
                        label = { Text("عنوان الدورة") },
                        placeholder = { Text("مثال: أسبوع الاختبارات النهائية - الفصل الأول") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCycleDesc,
                        onValueChange = { newCycleDesc = it },
                        label = { Text("وصف مختصر للهدف (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCycleTitle.isNotBlank()) {
                            onStartNewCycle(newCycleTitle.trim(), newCycleDesc.ifBlank { null })
                            newCycleTitle = ""
                            newCycleDesc = ""
                            showNewCycleDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                ) {
                    Text("بدء الدورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCycleDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportTeamTab(
    teamMembers: List<TeamMember>,
    currentLoggedInMember: TeamMember?,
    onAddTeamMember: (String, String, String, String) -> Unit,
    onDeleteTeamMember: (Long) -> Unit,
    onSwitchActiveMember: (TeamMember) -> Unit,
    onLogout: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToDelete by remember { mutableStateOf<TeamMember?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Current Logged-in User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (currentLoggedInMember?.isOwner == true) EmotionJoy else Teal700),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentLoggedInMember?.isOwner == true) Icons.Default.Stars else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentLoggedInMember?.name ?: "أ. مازن السبايله",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (currentLoggedInMember?.isOwner == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EmotionJoy.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmotionJoy)
                                ) {
                                    Text(
                                        text = "المشرف العام",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmotionJoy,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = currentLoggedInMember?.email ?: "mazensabaeleh@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300
                        )
                        Text(
                            text = "${currentLoggedInMember?.role ?: "المشرف العام"} • ${currentLoggedInMember?.permissions ?: "كامل الصلاحيات (الوصول الحصري للرسائل السرية)"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal200
                        )
                    }
                }

                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmotionAngryBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmotionAngry.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmotionAngry)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروج", fontSize = 12.sp, color = EmotionAngry)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Team Header with Add Member Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "أعضاء فريق العمل والمتابعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "إضافة أعضاء للاطلاع على الاستجابات وحجب الرسائل السرية",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }

            if (currentLoggedInMember?.isOwner == true) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة عضو فريق", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Members List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(teamMembers, key = { it.id }) { member ->
                TeamMemberCard(
                    member = member,
                    isActive = member.id == currentLoggedInMember?.id,
                    onSwitchClick = { onSwitchActiveMember(member) },
                    onDeleteClick = { memberToDelete = member }
                )
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("مرشد طلابي") }
        var selectedPermissions by remember { mutableStateOf("إدارة ومعالجة التذاكر") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Teal700)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة عضو جديد لفريق الدعم", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("الاسم الكامل") },
                        placeholder = { Text("مثال: أ. محمد العتيبي") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        placeholder = { Text("example@school.edu.sa") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "الدور والمسؤولية:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("مرشد طلابي", "مدير المدرسة", "أخصائي نفسي", "محلل بيانات", "عضو دعم").forEach { role ->
                            val isSel = selectedRole == role
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedRole = role },
                                label = { Text(role, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Teal700,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "الصلاحيات:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        "كامل الصلاحيات" to "تحكم كامل بكافة التقارير والتذاكر والفريق",
                        "إدارة ومعالجة التذاكر" to "متابعة الحالات وكتابة الملاحظات",
                        "اطلاع وتحليل فقط" to "مشاهدة لوحة النبض والرسوم البيانية"
                    ).forEach { (perm, desc) ->
                        val isSel = selectedPermissions == perm
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Teal50 else Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Teal600 else Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedPermissions = perm }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = perm,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Teal900 else Slate900
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            onAddTeamMember(newName.trim(), newEmail.trim(), selectedRole, selectedPermissions)
                            showAddDialog = false
                        }
                    },
                    enabled = newName.isNotBlank() && newEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                ) {
                    Text("إضافة العضو")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Member Confirmation
    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("حذف عضو من الفريق", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من رغبتك في إلغاء صلاحيات (${member.name}) وحذفه من فريق الدعم؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTeamMember(member.id)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmotionAngry)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun TeamMemberCard(
    member: TeamMember,
    isActive: Boolean = false,
    onSwitchClick: (() -> Unit)? = null,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Teal50.copy(alpha = 0.4f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isActive) Teal600 else (if (member.isOwner) EmotionJoy.copy(alpha = 0.5f) else Slate200)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (member.isOwner) EmotionJoyBg else Teal50),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(1),
                        fontWeight = FontWeight.Bold,
                        color = if (member.isOwner) EmotionJoy else Teal800,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        if (member.isOwner) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmotionJoyBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "المشرف العام",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmotionJoy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Teal100,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "نشط",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Teal900,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Teal50) {
                            Text(
                                text = member.role,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Teal800,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                            Text(
                                text = member.permissions,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (member.isOwner) Teal800 else EmotionAngry
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isActive && onSwitchClick != null) {
                    OutlinedButton(
                        onClick = onSwitchClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal700)
                    ) {
                        Text("تبديل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!member.isOwner) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف العضو من فريق الدعم",
                            tint = EmotionAngry,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportTicketsTab(
    tickets: List<SupportTicket>,
    currentLoggedInMember: TeamMember?,
    onUpdateTicketStatus: (Long, String, String?) -> Unit,
    onDeleteTicket: (Long) -> Unit
) {
    val isSuperAdmin = currentLoggedInMember?.isOwner == true

    if (!isSuperAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "صلاحية مقيّدة • خصوصية وسرية تامة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "الرسائل السرية وطلبات الدعم والاستشارة مقتصرة حصرياً على المشرف العام (أ. مازن السبايله) لحماية خصوصية الطلاب وسرية بياناتهم التامة وفق اللائحة الإرشادية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate300,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Teal400, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "صلاحيتك الحالية كعضو فريق عمل: الاطلاع والتحليل على نبضات واستجابات المشاعر العامة فقط.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Teal200,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        return
    }

    var statusFilter by remember { mutableStateOf("ALL") }
    var selectedTicketForDetails by remember { mutableStateOf<SupportTicket?>(null) }

    val filteredTickets = remember(tickets, statusFilter) {
        if (statusFilter == "ALL") tickets else tickets.filter { it.status == statusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "الكل (${tickets.size})",
                "NEW" to "جديدة (${tickets.count { it.status == "NEW" }})",
                "IN_PROGRESS" to "قيد المعالجة",
                "RESOLVED" to "مكتملة"
            ).forEach { (code, title) ->
                val isSelected = statusFilter == code
                FilterChip(
                    selected = isSelected,
                    onClick = { statusFilter = code },
                    label = { Text(title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Teal700,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد طلبات دعم في هذا التصنيف حالياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        onClick = { selectedTicketForDetails = ticket }
                    )
                }
            }
        }
    }

    // Ticket Details & Counselor Action Dialog
    selectedTicketForDetails?.let { ticket ->
        var tempStatus by remember { mutableStateOf(ticket.status) }
        var tempNotes by remember { mutableStateOf(ticket.counselorNotes ?: "") }

        AlertDialog(
            onDismissRequest = { selectedTicketForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "طلب دعم: ${ticket.ticketCode}",
                        fontWeight = FontWeight.Bold
                    )
                    UrgencyBadge(urgency = ticket.urgency)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "رسالة الطالب:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = ticket.message,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "بيانات الهوية والتواصل:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Text(
                        text = ticket.contactInfo ?: "مجهول الهوية 100% (لم يترك الطالب اسماً)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ticket.contactInfo == null) Teal700 else Slate900,
                        fontWeight = if (ticket.contactInfo == null) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "حالة الطلب:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "NEW" to "جديدة",
                            "IN_PROGRESS" to "قيد المعالجة",
                            "CONTACTED" to "تم التواصل",
                            "RESOLVED" to "مكتملة"
                        ).forEach { (st, label) ->
                            val isSel = tempStatus == st
                            FilterChip(
                                selected = isSel,
                                onClick = { tempStatus = st },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Teal700,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempNotes,
                        onValueChange = { tempNotes = it },
                        label = { Text("ملاحظات المرشد السرية (خاصة بك)") },
                        placeholder = { Text("مثال: تم الجلوس مع الطالب ومناقشة جدول المذاكرة...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTicketStatus(ticket.id, tempStatus, tempNotes.ifBlank { null })
                        selectedTicketForDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                ) {
                    Text("حفظ التحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicketForDetails = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun TicketCard(ticket: SupportTicket, onClick: () -> Unit) {
    val dateStr = remember(ticket.timestamp) {
        SimpleDateFormat("hh:mm a - yyyy/MM/dd", Locale.getDefault()).format(Date(ticket.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (ticket.urgency == "CRITICAL") EmotionAngry else Slate200
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ticket.ticketCode,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = ticket.status)
                }
                UrgencyBadge(urgency = ticket.urgency)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ticket.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate800,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ticket.isAnonymous) "طالب (مجهول الهوية)" else (ticket.contactInfo ?: "طالب مسجل"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, bg, fg) = when (status) {
        "NEW" -> Triple("جديدة", EmotionSadBg, EmotionSad)
        "IN_PROGRESS" -> Triple("قيد المعالجة", EmotionJoyBg, EmotionJoy)
        "CONTACTED" -> Triple("تم التواصل", Teal100, Teal800)
        "RESOLVED" -> Triple("مكتملة", Emerald100, Emerald700)
        else -> Triple(status, Slate100, Slate700)
    }

    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun UrgencyBadge(urgency: String) {
    val (text, bg, fg) = when (urgency) {
        "CRITICAL" -> Triple("عاجل جداً", EmotionAngryBg, EmotionAngry)
        "URGENT" -> Triple("ملحّ", EmotionJoyBg, EmotionJoy)
        else -> Triple("عادي", Teal50, Teal800)
    }

    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
