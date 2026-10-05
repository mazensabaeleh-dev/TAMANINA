package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.data.model.InitiativeAnalyzer
import com.example.data.repository.TomaninaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TomaninaComprehensiveTestSuite {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TomaninaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TomaninaRepository(database, context)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==========================================
    // 1. UNIT TESTS: CORE FUNCTIONS & LOGIC
    // ==========================================

    @Test
    fun `test all 8 emotional categories and classifications`() {
        val allEmotions = EmotionType.values()
        assertEquals("Platform must maintain exactly 8 harmonized emotions", 8, allEmotions.size)

        // Positive or stable emotions
        assertTrue(EmotionType.JOY.isPositiveOrStable)
        assertTrue(EmotionType.CALM.isPositiveOrStable)
        assertTrue(EmotionType.BORED.isPositiveOrStable)

        // Distress emotions requiring proactive guidance
        assertTrue(EmotionType.ANXIOUS.needsAttention)
        assertTrue(EmotionType.SAD.needsAttention)
        assertTrue(EmotionType.ANGRY.needsAttention)
        assertTrue(EmotionType.FEAR.needsAttention)
        assertFalse(EmotionType.EXHAUSTED.needsAttention)
    }

    @Test
    fun `test initiative recommender logic for various emotional states`() {
        // Case 1: Anxious dominant
        val anxiousRecords = listOf(
            CheckInRecord(emotion = "ANXIOUS", reasonCategory = "اختبارات"),
            CheckInRecord(emotion = "ANXIOUS", reasonCategory = "ضغوطات"),
            CheckInRecord(emotion = "CALM", reasonCategory = "عام")
        )
        val (dominantAnxious, initiativesAnxious) = InitiativeAnalyzer.generateInitiatives(anxiousRecords, "فصل أول")
        assertEquals(EmotionType.ANXIOUS, dominantAnxious)
        assertTrue(initiativesAnxious.any { it.title.contains("واحة الهدوء") || it.category.contains("إرشاد") })

        // Case 2: Exhausted dominant
        val exhaustedRecords = listOf(
            CheckInRecord(emotion = "EXHAUSTED", reasonCategory = "صحة ونوم"),
            CheckInRecord(emotion = "EXHAUSTED", reasonCategory = "تعب"),
            CheckInRecord(emotion = "JOY", reasonCategory = "أصدقاء")
        )
        val (dominantExhausted, initiativesExhausted) = InitiativeAnalyzer.generateInitiatives(exhaustedRecords, "فصل أول")
        assertEquals(EmotionType.EXHAUSTED, dominantExhausted)
        assertTrue(initiativesExhausted.any { it.title.contains("واحة الهدوء") || it.category.contains("إرشاد") })

        // Case 3: Empty records fallback to general initiatives
        val (dominantEmpty, initiativesEmpty) = InitiativeAnalyzer.generateInitiatives(emptyList(), "فصل أول")
        assertNull(dominantEmpty)
        assertTrue(initiativesEmpty.isNotEmpty())
    }

    @Test
    fun `test CSV export formatting and special characters escaping`() {
        val records = listOf(
            CheckInRecord(
                id = 101,
                emotion = "JOY",
                reasonCategory = "الأصدقاء, والنشاط",
                optionalNote = "يوم دراسي جميل,\nمع الزملاء",
                timestamp = 1700000000000L
            )
        )
        val csv = repository.generateExportCsv(records)

        assertTrue("CSV should have standard headers", csv.startsWith("ID,Emotion,Emotion_Arabic"))
        assertTrue("Commas in fields should be escaped to semicolons", csv.contains("الأصدقاء; والنشاط"))
        assertFalse("Newlines should be cleaned up", csv.contains("\nمع الزملاء\n"))
    }

    // ==========================================
    // 2. INTEGRATION TESTS: DATABASE & REPO
    // ==========================================

    @Test
    fun `test check-in submission and retrieval integration`() = runBlocking {
        repository.recordCheckIn(
            emotion = EmotionType.CALM,
            reasonCategory = "راحة عامة",
            note = "أشعر بارتياح كبير اليوم"
        )

        val checkIns = repository.allCheckIns.first()
        assertTrue("Check-in should be persisted", checkIns.isNotEmpty())
        val latest = checkIns.first()
        assertEquals("CALM", latest.emotion)
        assertEquals("راحة عامة", latest.reasonCategory)
        assertEquals("أشعر بارتياح كبير اليوم", latest.optionalNote)
    }

    @Test
    fun `test confidential support ticket creation and status updates`() = runBlocking {
        repository.submitSupportTicket(
            message = "أحتاج موعد استشارة بخصوص تنظيم وقت المذاكرة",
            contactInfo = "طالب بالصف الثاني الثانوي",
            urgency = "HIGH"
        )

        val tickets = repository.allTickets.first()
        assertTrue("Ticket should be saved in DB", tickets.isNotEmpty())
        val ticket = tickets.find { it.message.contains("تنظيم وقت المذاكرة") }
        assertNotNull("Submitted ticket must be found in tickets list", ticket)
        assertEquals("NEW", ticket?.status)
        assertEquals("HIGH", ticket?.urgency)

        // Update status to IN_PROGRESS
        val ticketId = ticket!!.id
        repository.updateTicketStatus(ticketId, "IN_PROGRESS", "تم التواصل مع الطالب")
        val updatedTickets = repository.allTickets.first()
        val updated = updatedTickets.find { it.id == ticketId }
        assertNotNull(updated)
        assertEquals("IN_PROGRESS", updated?.status)
        assertEquals("تم التواصل مع الطالب", updated?.counselorNotes)
    }

    @Test
    fun `test school cycle creation and deactivation flow`() = runBlocking {
        repository.startNewCycle("الفصل الدراسي الثاني", "خطة التوجيه الطلابي للترم الثاني")

        val activeCycle = repository.activeCycle.first()
        assertNotNull("Active cycle must exist", activeCycle)
        assertEquals("الفصل الدراسي الثاني", activeCycle?.title)
        assertTrue(activeCycle?.isActive == true)

        // Starting another cycle deactivates previous ones
        repository.startNewCycle("الفصل الدراسي الثالث", "فصل الصيف")
        val newActive = repository.activeCycle.first()
        assertEquals("الفصل الدراسي الثالث", newActive?.title)
    }

    // ==========================================
    // 3. SECURITY TESTS: AUTH, SQLI, XSS, BYPASS
    // ==========================================

    @Test
    fun `security test - prevent empty password authentication bypass`() = runBlocking {
        val resultEmpty = repository.login("mazensabaeleh@gmail.com", "")
        assertFalse("Empty password must strictly fail authentication", resultEmpty)

        val resultWhitespace = repository.login("mazensabaeleh@gmail.com", "   ")
        assertFalse("Whitespace password must fail authentication", resultWhitespace)
    }

    @Test
    fun `security test - prevent arbitrary password bypass on accounts`() = runBlocking {
        val resultWrong = repository.login("mazensabaeleh@gmail.com", "random_unauthorized_pass")
        assertFalse("Incorrect password must be rejected", resultWrong)
    }

    @Test
    fun `security test - resilience against SQL Injection payloads in login`() = runBlocking {
        val sqliPayloads = listOf(
            "' OR 1=1 --",
            "admin'--",
            "\" OR \"\"=\"",
            "' UNION SELECT * FROM team_members --"
        )
        for (payload in sqliPayloads) {
            val result = repository.login(payload, "any_pass")
            assertFalse("SQL injection payload in email must not bypass auth: $payload", result)

            val passResult = repository.login("mazensabaeleh@gmail.com", payload)
            assertFalse("SQL injection payload in password must not bypass auth", passResult)
        }
    }

    @Test
    fun `security test - resilience against XSS payloads in tickets and checkins`() = runBlocking {
        val xssPayload = "<script>alert('XSS')</script><img src=x onerror=alert(1)>"
        repository.recordCheckIn(
            emotion = EmotionType.JOY,
            reasonCategory = xssPayload,
            note = xssPayload
        )

        val records = repository.allCheckIns.first()
        val inserted = records.first()
        assertEquals(xssPayload, inserted.optionalNote)
    }

    @Test
    fun `security test - valid supervisor credentials succeeds and establishes session`() = runBlocking {
        val success = repository.login("mazensabaeleh@gmail.com", "mazen@1435522")
        assertTrue("Valid authorized supervisor credentials must authenticate", success)

        val loggedIn = repository.currentLoggedInMember.first()
        assertNotNull("Logged in session member must be set", loggedIn)
        assertEquals("mazensabaeleh@gmail.com", loggedIn?.email)
        assertTrue(loggedIn?.isOwner == true)

        // Test logout
        repository.logout()
        assertNull("Logout must immediately clear active session", repository.currentLoggedInMember.first())
    }

    // ==========================================
    // 4. END-TO-END CUJ FLOW TEST
    // ==========================================

    @Test
    fun `e2e test - student checkin to counselor dashboard export and logout flow`() = runBlocking {
        // Step 1: Student records daily emotional pulse
        repository.recordCheckIn(
            emotion = EmotionType.FEAR,
            reasonCategory = "الامتحانات المدرسية",
            note = "قلق قبل اختبار الرياضيات"
        )

        // Step 2: Student submits confidential support ticket
        repository.submitSupportTicket(
            message = "أرجو تقديم استشارة خاصة في أسرع وقت",
            contactInfo = "طالب مجهول",
            urgency = "URGENT"
        )

        // Step 3: Counselor logs into administrative portal
        val loginSuccess = repository.login("mazensabaeleh@gmail.com", "mazen@1435522")
        assertTrue(loginSuccess)

        // Step 4: Counselor analyzes stats and generates export report
        val allCheckIns = repository.allCheckIns.first()
        assertTrue("Check-ins list should not be empty", allCheckIns.isNotEmpty())
        assertTrue("Check-ins should contain the student pulse", allCheckIns.any { it.reasonCategory == "الامتحانات المدرسية" })
        val csvReport = repository.generateExportCsv(allCheckIns)
        assertTrue(csvReport.contains("الامتحانات المدرسية"))

        // Step 5: Counselor reviews and resolves ticket
        val tickets = repository.allTickets.first()
        val targetTicket = tickets.find { it.message.contains("استشارة خاصة") }
        assertNotNull(targetTicket)
        val ticketId = targetTicket!!.id
        repository.updateTicketStatus(ticketId, "RESOLVED", "تم التواصل وتقديم جلسة التوجيه بنجاح")

        val resolvedTickets = repository.allTickets.first()
        val resolved = resolvedTickets.find { it.id == ticketId }
        assertEquals("RESOLVED", resolved?.status)

        // Step 6: Counselor logs out securely
        repository.logout()
        assertNull(repository.currentLoggedInMember.first())
    }
}
