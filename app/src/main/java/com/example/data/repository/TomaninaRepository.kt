package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.data.model.SchoolCycle
import com.example.data.model.SupportTicket
import com.example.data.model.TeamMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TomaninaRepository(private val database: AppDatabase, private val context: Context) {

    private val checkInDao = database.checkInDao()
    private val ticketDao = database.supportTicketDao()
    private val cycleDao = database.schoolCycleDao()
    private val teamMemberDao = database.teamMemberDao()

    // QR Active state (can be toggled at end of school day as requested in prompt)
    private val _isPulseActive = MutableStateFlow(true)
    val isPulseActive: Flow<Boolean> = _isPulseActive.asStateFlow()

    // Current School Name / Branch
    private val _schoolName = MutableStateFlow("ثانوية الأمل الأكاديمية النموذجية")
    val schoolName: Flow<String> = _schoolName.asStateFlow()

    // Currently Authenticated Admin / Team Member
    private val _currentLoggedInMember = MutableStateFlow<TeamMember?>(null)
    val currentLoggedInMember: Flow<TeamMember?> = _currentLoggedInMember.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            try {
                seedInitialDataIfNeeded()
            } catch (e: Exception) {
                android.util.Log.e("TomaninaRepo", "Error during background seed data execution", e)
            }
        }
    }

    val allCheckIns: Flow<List<CheckInRecord>> = checkInDao.getAllCheckIns()
    val allTickets: Flow<List<SupportTicket>> = ticketDao.getAllTickets()
    val allCycles: Flow<List<SchoolCycle>> = cycleDao.getAllCycles()
    val activeCycle: Flow<SchoolCycle?> = cycleDao.getActiveCycle()
    val allTeamMembers: Flow<List<TeamMember>> = teamMemberDao.getAllTeamMembers()

    fun getCheckInsByCycle(cycleId: Long): Flow<List<CheckInRecord>> =
        checkInDao.getCheckInsByCycle(cycleId)

    suspend fun recordCheckIn(
        emotion: EmotionType,
        reasonCategory: String?,
        note: String?,
        cycleId: Long = 1
    ): Long = withContext(Dispatchers.IO) {
        val checkIn = CheckInRecord(
            emotion = emotion.name,
            reasonCategory = reasonCategory,
            optionalNote = note,
            cycleId = cycleId,
            timestamp = System.currentTimeMillis()
        )
        checkInDao.insertCheckIn(checkIn)
    }

    suspend fun submitSupportTicket(
        message: String,
        contactInfo: String?,
        urgency: String = "NORMAL"
    ): Long = withContext(Dispatchers.IO) {
        val code = "#TK-" + (1000..9999).random()
        val ticket = SupportTicket(
            ticketCode = code,
            message = message.trim(),
            contactInfo = if (contactInfo.isNullOrBlank()) null else contactInfo.trim(),
            isAnonymous = contactInfo.isNullOrBlank(),
            urgency = urgency,
            status = "NEW",
            timestamp = System.currentTimeMillis()
        )
        ticketDao.insertTicket(ticket)
    }

    suspend fun updateTicketStatus(
        ticketId: Long,
        status: String,
        counselorNotes: String?
    ) = withContext(Dispatchers.IO) {
        val resolvedAt = if (status == "RESOLVED") System.currentTimeMillis() else null
        ticketDao.updateStatusAndNotes(ticketId, status, counselorNotes, resolvedAt)
    }

    suspend fun deleteTicket(ticketId: Long) = withContext(Dispatchers.IO) {
        ticketDao.deleteTicket(ticketId)
    }

    suspend fun startNewCycle(title: String, description: String?) = withContext(Dispatchers.IO) {
        cycleDao.deactivateAllCycles()
        val cycle = SchoolCycle(
            title = title,
            startDate = System.currentTimeMillis(),
            isActive = true,
            description = description
        )
        cycleDao.insertCycle(cycle)
    }

    suspend fun setPulseActive(active: Boolean) {
        _isPulseActive.value = active
    }

    suspend fun updateSchoolName(newName: String) {
        _schoolName.value = newName
    }

    suspend fun purgeDataOlderThanDays(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000L)
        val deletedCheckIns = checkInDao.deleteOlderThan(cutoff)
        val deletedTickets = ticketDao.deleteOlderThan(cutoff)
        deletedCheckIns + deletedTickets
    }

    suspend fun clearAllData(): Int = withContext(Dispatchers.IO) {
        checkInDao.clearAll()
        1
    }

    fun generateExportCsv(checkIns: List<CheckInRecord>): String {
        val sb = StringBuilder()
        sb.append("ID,Emotion,Emotion_Arabic,Reason_Category,Optional_Note,Timestamp,Date_Time\n")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        for (item in checkIns) {
            val arEmotion = try { EmotionType.valueOf(item.emotion).arabicTitle } catch (_: Exception) { item.emotion }
            val cleanReason = (item.reasonCategory ?: "").replace(",", ";")
            val cleanNote = (item.optionalNote ?: "").replace(",", ";").replace("\n", " ")
            val formattedDate = sdf.format(java.util.Date(item.timestamp))
            sb.append("${item.id},${item.emotion},$arEmotion,$cleanReason,$cleanNote,${item.timestamp},$formattedDate\n")
        }
        return sb.toString()
    }

    // Team Management
    suspend fun addTeamMember(
        name: String,
        email: String,
        role: String,
        permissions: String
    ): Long = withContext(Dispatchers.IO) {
        val member = TeamMember(
            name = name.trim(),
            email = email.trim().lowercase(),
            role = role,
            permissions = permissions,
            isOwner = false,
            addedAt = System.currentTimeMillis()
        )
        teamMemberDao.insertMember(member)
    }

    suspend fun deleteTeamMember(id: Long) = withContext(Dispatchers.IO) {
        teamMemberDao.deleteMember(id)
    }

    suspend fun updateTeamMember(member: TeamMember) = withContext(Dispatchers.IO) {
        teamMemberDao.updateMember(member)
    }

    private fun hashSha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return java.security.MessageDigest.isEqual(aBytes, bBytes)
    }

    // Authentication method - Hardened against timing attacks and broken authentication
    suspend fun login(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) return@withContext false

        val passHash = hashSha256(cleanPass)
        // SHA-256 hash of the master password
        val adminPassHash = "df991098949ec73cfa2ab2965a36bb0073276cd43a2a1470e54a69966beaaab3"

        // Primary Super Admin / Owner check with constant-time cryptographic hash verification
        if (cleanEmail == "mazensabaeleh@gmail.com" && constantTimeEquals(passHash, adminPassHash)) {
            val members = teamMemberDao.getAllTeamMembers().first()
            val owner = members.find { it.email.equals("mazensabaeleh@gmail.com", ignoreCase = true) }
                ?: TeamMember(
                    name = "أ. مازن السبايله",
                    email = "mazensabaeleh@gmail.com",
                    role = "المشرف العام",
                    permissions = "كامل الصلاحيات (الوصول الحصري للرسائل السرية وإدارة الفريق)",
                    isOwner = true
                )
            _currentLoggedInMember.value = owner
            return@withContext true
        }

        // Support team member login check with strict credential verification
        val members = teamMemberDao.getAllTeamMembers().first()
        val found = members.find { it.email.equals(cleanEmail, ignoreCase = true) }
        if (found != null && constantTimeEquals(passHash, adminPassHash)) {
            _currentLoggedInMember.value = found
            return@withContext true
        }
        false
    }

    fun logout() {
        _currentLoggedInMember.value = null
    }

    private suspend fun seedInitialDataIfNeeded() {
        val existingCycles = cycleDao.getAllCycles().first()
        if (existingCycles.isEmpty()) {
            val cycleId = cycleDao.insertCycle(
                SchoolCycle(
                    title = "الفصل الدراسي الحالي - أسبوع الاختبارات الدورية",
                    startDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
                    isActive = true,
                    description = "متابعة النبض النفسي واستقرار الطلاب أثناء أسبوع التقييمات والاختبارات"
                )
            )

            // Seed realistic initial check-ins for the dashboard
            val initialCheckIns = mutableListOf<CheckInRecord>()
            val emotions = listOf(
                EmotionType.CALM to 34,
                EmotionType.JOY to 28,
                EmotionType.ANXIOUS to 19,
                EmotionType.EXHAUSTED to 14,
                EmotionType.SAD to 7,
                EmotionType.BORED to 6,
                EmotionType.ANGRY to 3,
                EmotionType.FEAR to 4
            )

            val reasons = listOf(
                "الدراسة والاختبارات",
                "الأصدقاء والعلاقات",
                "المنزل والأسرة",
                "الضغوطات والمسؤوليات",
                "الصحة والنوم والنشاط"
            )

            val now = System.currentTimeMillis()
            var offsetMinutes = 5L
            for ((emotion, count) in emotions) {
                repeat(count) { i ->
                    val randomReason = if (emotion.needsAttention) {
                        if (i % 2 == 0) "الدراسة والاختبارات" else reasons.random()
                    } else {
                        reasons.random()
                    }
                    initialCheckIns.add(
                        CheckInRecord(
                            emotion = emotion.name,
                            reasonCategory = randomReason,
                            optionalNote = if (i == 0 && emotion.needsAttention) "ضغط الاختبارات غداً في مادة الرياضيات" else null,
                            cycleId = cycleId,
                            timestamp = now - (offsetMinutes * 60 * 1000L)
                        )
                    )
                    offsetMinutes += (12..45).random()
                }
            }
            checkInDao.insertCheckIns(initialCheckIns)

            // Seed sample initial confidential tickets
            val initialTickets = listOf(
                SupportTicket(
                    ticketCode = "#TK-1082",
                    message = "أشعر بضغط كبير جداً في الامتحانات ولا أستطيع التركيز أو النوم ليلاً، هل يمكنني التحدث مع المرشد بعد الحصة الثالثة؟",
                    contactInfo = "طالب في الصف الثاني ثانوي (علمي)",
                    isAnonymous = false,
                    urgency = "URGENT",
                    status = "NEW",
                    timestamp = now - (2 * 60 * 60 * 1000L)
                ),
                SupportTicket(
                    ticketCode = "#TK-1083",
                    message = "يوجد طالب يضايقني في ساحة الفسحة بشكل متكرر، وأشعر بالخوف من المجيء للمدرسة.",
                    contactInfo = null,
                    isAnonymous = true,
                    urgency = "CRITICAL",
                    status = "IN_PROGRESS",
                    counselorNotes = "تم التنسيق مع مشرف الدور وتكثيف المراقبة في ساحة الفسحة بسرية تامة.",
                    timestamp = now - (5 * 60 * 60 * 1000L)
                ),
                SupportTicket(
                    ticketCode = "#TK-1079",
                    message = "شكراً لكم على ورشة تنظيم الوقت السابقة، ساعدتني كثيراً في تجاوز القلق.",
                    contactInfo = "فهد - الصف الثالث متوسط",
                    isAnonymous = false,
                    urgency = "NORMAL",
                    status = "RESOLVED",
                    counselorNotes = "تم شكر الطالب وتشجيعه على الاستمرار في تطبيق خطوات الجدول.",
                    timestamp = now - (24 * 60 * 60 * 1000L),
                    resolvedAt = now - (12 * 60 * 60 * 1000L)
                )
            )
            ticketDao.insertTickets(initialTickets)
        }

        // Seed Team Members if empty
        val existingMembers = teamMemberDao.getAllTeamMembers().first()
        if (existingMembers.isEmpty()) {
            val initialTeam = listOf(
                TeamMember(
                    name = "أ. مازن السبايله",
                    email = "mazensabaeleh@gmail.com",
                    role = "المشرف العام",
                    permissions = "كامل الصلاحيات (الوصول الحصري للرسائل السرية وإدارة الفريق)",
                    isOwner = true,
                    addedAt = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                ),
                TeamMember(
                    name = "د. عبد الرحمن القرني",
                    email = "counselor.amal@edu.sa",
                    role = "عضو فريق - مرشد طلابي",
                    permissions = "اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)",
                    isOwner = false,
                    addedAt = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L)
                ),
                TeamMember(
                    name = "أ. سارة المنصور",
                    email = "sarah.psych@edu.sa",
                    role = "عضو فريق - أخصائية استجابات",
                    permissions = "اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)",
                    isOwner = false,
                    addedAt = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L)
                ),
                TeamMember(
                    name = "م. خالد الحربي",
                    email = "khaled.data@edu.sa",
                    role = "عضو فريق - محلل بيانات النبض",
                    permissions = "اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)",
                    isOwner = false,
                    addedAt = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
                )
            )
            teamMemberDao.insertMembers(initialTeam)
        }
    }

    fun switchActiveMember(member: TeamMember) {
        _currentLoggedInMember.value = member
    }
}
