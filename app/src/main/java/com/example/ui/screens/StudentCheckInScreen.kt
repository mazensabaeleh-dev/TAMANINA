package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ALL_REASON_CATEGORIES
import com.example.data.model.EmotionType
import com.example.ui.components.AbstractEmotionIcon
import com.example.ui.components.BreathingExerciseModal
import com.example.ui.theme.*

enum class StudentStep {
    SELECT_EMOTION,
    CHOOSE_REASON,
    REASSURANCE_AND_SUPPORT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentCheckInScreen(
    schoolName: String,
    isPulseActive: Boolean,
    onCheckInSubmit: (EmotionType, String?, String?) -> Unit,
    onSubmitTicket: (String, String?, String) -> Unit,
    onSwitchToCounselor: () -> Unit,
    onOpenWebPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentStep by remember { mutableStateOf(StudentStep.SELECT_EMOTION) }
    var selectedEmotion by remember { mutableStateOf<EmotionType?>(null) }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var optionalNote by remember { mutableStateOf("") }

    var showBreathingModal by remember { mutableStateOf(false) }

    // Support ticket dialog/sheet state
    var showTicketSheet by remember { mutableStateOf(false) }
    var ticketMessage by remember { mutableStateOf("") }
    var ticketContact by remember { mutableStateOf("") }
    var ticketUrgency by remember { mutableStateOf("NORMAL") }
    var ticketSubmittedMessage by remember { mutableStateOf<String?>(null) }

    // If pulse is paused outside school hours
    if (!isPulseActive) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Teal100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Teal800,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "استقبال النبضات متوقف مؤقتاً",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ينتهي استقبال النبضات اليومية مع نهاية الدوام المدرسي، ويعود للعمل صباح اليوم الدراسي القادم.\nنتمنى لك أوقاتاً مطمئنة ومريحة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onSwitchToCounselor) {
                        Text("الدخول كمرشد طلابي", color = Teal700)
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Top Bar
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentStep != StudentStep.SELECT_EMOTION && currentStep != StudentStep.REASSURANCE_AND_SUPPORT) {
                        IconButton(
                            onClick = {
                                if (currentStep == StudentStep.CHOOSE_REASON) {
                                    currentStep = StudentStep.SELECT_EMOTION
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Slate700
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Column {
                        Text(
                            text = "طمأنينة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Teal800
                        )
                        Text(
                            text = schoolName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            maxLines = 1
                        )
                    }
                }

                // Discreet and Aesthetic Top Actions (Hiding clutter for the public audience)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shareUrl = "https://ais-dev-edmcimlo5luskkwbjlby34-355668188961.europe-west3.run.app"
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "✨ منصة طمأنينة المدرسية - $schoolName\n" +
                                    "«توقف لحظة.. كيف تشعر اليوم؟»\n" +
                                    "رابط الوصول العام والمباشر:\n$shareUrl\n\n" +
                                    "مجهول الهوية وسري 100% بدون تسجيل دخول."
                                )
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة رابط المنصة"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "المزيد من الخيارات",
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("الموقع ونسخة الحاسوب") },
                                onClick = {
                                    showMenu = false
                                    onOpenWebPortal()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Computer, contentDescription = null, tint = Teal700)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("دخول المرشد الطلابي") },
                                onClick = {
                                    showMenu = false
                                    onSwitchToCounselor()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Slate600)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Main Screen Content based on Step - Centered & Adaptive for Desktop/PC screens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 960.dp)
            ) {
            when (currentStep) {
                StudentStep.SELECT_EMOTION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        // Badge: 100% Anonymous & Safe
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Emerald100,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مجهول الهوية وسري 100% • 15 ثانية فقط",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "كيف تشعر في هذه اللحظة؟",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "اختر الحالة الأقرب لشعورك الحالي.. كل مشاعرك مقدرة ومهمة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 16.dp)
                        )

                        // 8 Emotion Cards Grid (2 columns)
                        val emotions = EmotionType.values()
                        for (i in emotions.indices step 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EmotionGridItem(
                                    emotion = emotions[i],
                                    isSelected = selectedEmotion == emotions[i],
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedEmotion = emotions[i]
                                        currentStep = StudentStep.CHOOSE_REASON
                                    }
                                )
                                if (i + 1 < emotions.size) {
                                    EmotionGridItem(
                                        emotion = emotions[i + 1],
                                        isSelected = selectedEmotion == emotions[i + 1],
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedEmotion = emotions[i + 1]
                                            currentStep = StudentStep.CHOOSE_REASON
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.safe_environment_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                StudentStep.CHOOSE_REASON -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        // Selected Emotion Banner
                        selectedEmotion?.let { emotion ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = emotion.backgroundColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AbstractEmotionIcon(
                                        emotion = emotion,
                                        size = 40.dp,
                                        tint = emotion.primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "شعورك المختار: ${emotion.titleArabic}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = emotion.descriptionArabic,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate700
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "ما المجال المرتبط بهذا الشعور؟ (اختياري)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Text(
                            text = "يساعد ذلك المرشد الطلابي على فهم التحديات العامة وتقديم البرامج المناسبة",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Reason Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ALL_REASON_CATEGORIES.forEach { reason ->
                                val isSelected = selectedReason == reason
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedReason = if (isSelected) null else reason
                                    },
                                    label = {
                                        Text(
                                            text = reason,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Teal700,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Optional short note
                        OutlinedTextField(
                            value = optionalNote,
                            onValueChange = { optionalNote = it },
                            label = { Text("ملاحظة إضافية تود تفريغها (اختياري وسري)") },
                            placeholder = { Text("مثلاً: أود بعض النصائح لتنظيم جدول المذاكرة...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_note_input"),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                selectedEmotion?.let { emotion ->
                                    onCheckInSubmit(emotion, selectedReason, optionalNote.ifBlank { null })
                                }
                                currentStep = StudentStep.REASSURANCE_AND_SUPPORT
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_emotion_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تأكيد وإرسال نبضتي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                StudentStep.REASSURANCE_AND_SUPPORT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Success & Reassurance Hero
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Teal100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Teal700,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "شكراً لصدقك.. صوتك مسموع",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Personalized Psychological Reassurance Message Card
                        selectedEmotion?.let { emotion ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = emotion.backgroundColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AbstractEmotionIcon(
                                            emotion = emotion,
                                            size = 28.dp,
                                            tint = emotion.primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "رسالة مساندة لك:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = emotion.supportMessageArabic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate800,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Guided Breathing Trigger Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBreathingModal = true }
                                .testTag("breathing_exercise_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Teal700),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تمرين التنفس المهدئ (4-4-4)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "دقيقة واحدة من التنفس التفاعلي لاستعادة توازنك وهدوئك فورياً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate300
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Teal300,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confidential Support Request Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTicketSheet = true }
                                .testTag("confidential_support_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Teal50),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = Teal800
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "طلب مساندة سرية من المرشد الطلابي",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "إن كنت تمر بظرف صعب أو تحتاج شخصاً تثق به ليسمعك، نحن هنا لأجلك",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Slate400
                                )
                            }
                        }

                        if (ticketSubmittedMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald100,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ticketSubmittedMessage ?: "",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Emerald700,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        OutlinedButton(
                            onClick = {
                                // Reset flow for a fresh check-in
                                currentStep = StudentStep.SELECT_EMOTION
                                selectedEmotion = null
                                selectedReason = null
                                optionalNote = ""
                                ticketSubmittedMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                        ) {
                            Text("تسجيل نبضة أخرى أو إنهاء")
                        }
                    }
                }
            }
        }
    }
    }

    // Interactive Breathing Modal
    if (showBreathingModal) {
        BreathingExerciseModal(
            onDismiss = { showBreathingModal = false }
        )
    }

    // Confidential Support Ticket Dialog
    if (showTicketSheet) {
        AlertDialog(
            onDismissRequest = { showTicketSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Teal700,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "طلب مساندة سرية",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "رسالتك تصل حصرياً للمرشد الطلابي في المدرسة. يمكنك إرسالها مجهولة تماماً أو كتابة اسمك/صفك إن رغبت بالتواصل معك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = ticketMessage,
                        onValueChange = { ticketMessage = it },
                        label = { Text("اكتب ما يجول في خاطرك...") },
                        placeholder = { Text("أواجه صعوبة في... أو أحتاج استشارة...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("ticket_message_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ticketContact,
                        onValueChange = { ticketContact = it },
                        label = { Text("اسمك وصفك أو رقم التواصل (اختياري)") },
                        placeholder = { Text("اتركه فارغاً لتبقى مجهولاً تماماً") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ticket_contact_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "درجة الإلحاح:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NORMAL" to "عادي", "URGENT" to "ملحّ", "CRITICAL" to "عاجل جداً").forEach { (code, label) ->
                            val isSelected = ticketUrgency == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { ticketUrgency = code },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (code == "CRITICAL") EmotionAngry else Teal700,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ticketMessage.isNotBlank()) {
                            onSubmitTicket(
                                ticketMessage.trim(),
                                ticketContact.ifBlank { null },
                                ticketUrgency
                            )
                            ticketSubmittedMessage = "تم إرسال طلب المساندة بنجاح إلى المرشد الطلابي بسرية تامة. شكراً لثقتك."
                            ticketMessage = ""
                            ticketContact = ""
                            showTicketSheet = false
                        }
                    },
                    enabled = ticketMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                    modifier = Modifier.testTag("send_ticket_button")
                ) {
                    Text("إرسال بسرية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTicketSheet = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun EmotionGridItem(
    emotion: EmotionType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, emotion.primaryColor, RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, Slate200, RoundedCornerShape(18.dp))
            )
            .testTag("emotion_${emotion.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = emotion.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AbstractEmotionIcon(
                emotion = emotion,
                size = 38.dp,
                tint = emotion.primaryColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = emotion.titleArabic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center
            )
            Text(
                text = emotion.descriptionArabic.take(24) + "...",
                style = MaterialTheme.typography.labelSmall,
                color = Slate600,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
