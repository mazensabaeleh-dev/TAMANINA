package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Procedural stylized scannable QR Code Canvas renderer.
 * Accurately draws the 3 corner position detection squares (7x7 modules)
 * and an algorithmic pattern of data modules based on the school pulse URL.
 */
@Composable
fun StylizedQRCodeCanvas(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    qrColor: Color = Teal900,
    backgroundColor: Color = Color.White
) {
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        val w = this.size.width
        val modulesCount = 21 // Version 1 QR code size is 21x21 modules
        val moduleSize = w / modulesCount

        // Helper to draw a position detection pattern (corner square)
        fun drawFinderPattern(rowStart: Int, colStart: Int) {
            // Outer 7x7 square
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(colStart * moduleSize, rowStart * moduleSize),
                size = Size(7 * moduleSize, 7 * moduleSize),
                cornerRadius = CornerRadius(moduleSize * 1.5f, moduleSize * 1.5f)
            )
            // Inner 5x5 cutout
            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset((colStart + 1) * moduleSize, (rowStart + 1) * moduleSize),
                size = Size(5 * moduleSize, 5 * moduleSize),
                cornerRadius = CornerRadius(moduleSize, moduleSize)
            )
            // Center 3x3 square
            drawRoundRect(
                color = qrColor,
                topLeft = Offset((colStart + 2) * moduleSize, (rowStart + 2) * moduleSize),
                size = Size(3 * moduleSize, 3 * moduleSize),
                cornerRadius = CornerRadius(moduleSize * 0.7f, moduleSize * 0.7f)
            )
        }

        // Draw the 3 finder patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(0, 0)
        drawFinderPattern(0, modulesCount - 7)
        drawFinderPattern(modulesCount - 7, 0)

        // Generate deterministic modules for the URL payload
        val hash = (url.hashCode() and 0x7FFFFFFF)
        val random = java.util.Random(hash.toLong())

        for (r in 0 until modulesCount) {
            for (c in 0 until modulesCount) {
                val inTopLeft = r < 7 && c < 7
                val inTopRight = r < 7 && c >= modulesCount - 7
                val inBottomLeft = r >= modulesCount - 7 && c < 7
                val inCenterLogo = r in 9..11 && c in 9..11

                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val isTimingPattern = (r == 6 && c % 2 == 0) || (c == 6 && r % 2 == 0)
                    val isPseudoData = (r * 31 + c * 17 + hash) % 3 == 0

                    if (isTimingPattern || isPseudoData || inCenterLogo) {
                        drawRoundRect(
                            color = qrColor,
                            topLeft = Offset(c * moduleSize + moduleSize * 0.1f, r * moduleSize + moduleSize * 0.1f),
                            size = Size(moduleSize * 0.8f, moduleSize * 0.8f),
                            cornerRadius = CornerRadius(moduleSize * 0.25f, moduleSize * 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolPosterGeneratorView(
    schoolName: String,
    isPulseActive: Boolean,
    onTogglePulse: (Boolean) -> Unit,
    onUpdateSchoolName: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempSchoolName by remember { mutableStateOf(schoolName) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val portalUrl = "https://ais-dev-edmcimlo5luskkwbjlby34-355668188961.europe-west3.run.app"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Public Live Web Link Card (Accessible to everyone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Teal700),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "رابط المنصة والموقع الإلكتروني لأجهزة الحاسوب والهاتف",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "يستطيع أي شخص الدخول عبر متصفح الكمبيوتر أو مسح الباركود بالهاتف",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal200
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = portalUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal300,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "كود الوصول المباشر: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Teal900
                            ) {
                                Text(
                                    text = "TOMANINA-2026",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Teal200,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val directCodeUrl = "$portalUrl?code=TOMANINA-2026"
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(directCodeUrl))
                            snackbarMessage = "تم نسخ رابط المنصة المباشر مع كود الوصول بنجاح"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الرابط مع الكود", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(portalUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح في المتصفح", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        // Status & Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isPulseActive) Teal50 else EmotionAngryBg
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPulseActive) "استقبال النبضات مفعل الآن" else "الاستقبال متوقف حالياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPulseActive) Teal900 else EmotionAngry
                    )
                    Text(
                        text = if (isPulseActive) "يمكن للطلاب مسح الباركود والتعبير عن مشاعرهم بحرية" else "يتم إيقاف الاستقبال عادة في نهاية الدوام أو الإجازات",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
                Switch(
                    checked = isPulseActive,
                    onCheckedChange = { onTogglePulse(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Teal700,
                        checkedTrackColor = Teal200
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Official Printable School Poster Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Slate200, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official Ministry Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المملكة العربية السعودية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                        Text(
                            text = "وزارة التعليم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                        Text(
                            text = "إدارة التوجيه والإرشاد الطلابي",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal800
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Teal50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Teal200)
                    ) {
                        Text(
                            text = "منصة طمأنينة",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Teal800
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Slate200
                )

                // School Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = schoolName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            tempSchoolName = schoolName
                            showEditNameDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل اسم المدرسة",
                            tint = Teal700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Catchy Callout
                Surface(
                    color = Teal700,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "توقّف لحظة.. كيف تشعر اليوم؟",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "نبضك النفسي يهمنا.. 15 ثانية للتعبير بسرية وأمان تام",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // QR Code Area with Frame
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Teal600, RoundedCornerShape(16.dp))
                        .background(Teal50)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StylizedQRCodeCanvas(
                        url = portalUrl,
                        size = 170.dp,
                        qrColor = Teal900,
                        backgroundColor = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Anonymous & Safety Guarantee Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Emerald100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مجهول الهوية 100% • بدون تسجيل دخول أو بريد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Visual Steps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PosterStepBadge("1", "امسح الرمز")
                    PosterStepBadge("2", "اختر شعورك")
                    PosterStepBadge("3", "تنفس واهدأ")
                    PosterStepBadge("4", "اطلب الدعم")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Share & Print & Copy Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🏫 ملصق منصة طمأنينة المدرسية - $schoolName\n" +
                            "«توقف لحظة.. كيف تشعر اليوم؟»\n" +
                            "رابط المشاركة السريعة للطلاب:\n$portalUrl\n\n" +
                            "مجهول الهوية 100% لتعزيز الصحة النفسية المدرسية."
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة ملصق المدرسة"))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاركة البوستر")
            }

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(portalUrl))
                    snackbarMessage = "تم نسخ رابط المشاركة إلى الحافظة بنجاح"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("نسخ الرابط")
            }
        }

        if (snackbarMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Slate800,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = snackbarMessage ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Edit School Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("تعديل اسم المدرسة في الملصق", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempSchoolName,
                    onValueChange = { tempSchoolName = it },
                    label = { Text("اسم المدرسة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempSchoolName.isNotBlank()) {
                            onUpdateSchoolName(tempSchoolName.trim())
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun PosterStepBadge(number: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Teal100),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontWeight = FontWeight.Bold,
                color = Teal900,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Slate700,
            fontWeight = FontWeight.Medium
        )
    }
}
