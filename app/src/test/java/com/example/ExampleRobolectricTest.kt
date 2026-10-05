package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.data.model.InitiativeAnalyzer
import com.example.data.repository.TomaninaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("طمأنينة", appName)
    }

    @Test
    fun `verify eight emotional states exist and configured`() {
        val emotions = EmotionType.values()
        assertEquals(8, emotions.size)
        assertTrue(EmotionType.JOY.isPositiveOrStable)
        assertTrue(EmotionType.CALM.isPositiveOrStable)
        assertTrue(EmotionType.ANXIOUS.needsAttention)
        assertTrue(EmotionType.SAD.needsAttention)
    }

    @Test
    fun `verify initiative analyzer generates recommendations for anxious dominant emotion`() {
        val checkIns = listOf(
            CheckInRecord(emotion = "ANXIOUS", reasonCategory = "الدراسة والاختبارات"),
            CheckInRecord(emotion = "ANXIOUS", reasonCategory = "الضغوطات والمسؤوليات"),
            CheckInRecord(emotion = "EXHAUSTED", reasonCategory = "الصحة والنوم والنشاط"),
            CheckInRecord(emotion = "CALM", reasonCategory = "عام")
        )
        val (dominant, initiatives) = InitiativeAnalyzer.generateInitiatives(checkIns, "شهر الاختبارات")
        assertEquals(EmotionType.ANXIOUS, dominant)
        assertTrue(initiatives.isNotEmpty())
        assertTrue(initiatives.any { it.title.contains("واحة الهدوء") })
    }

    @Test
    fun `verify CSV export generation for computer download`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repo = TomaninaRepository(db, context)
        val records = listOf(
            CheckInRecord(id = 1, emotion = "JOY", reasonCategory = "الأصدقاء والعلاقات", optionalNote = "يوم جميل")
        )
        val csv = repo.generateExportCsv(records)
        assertTrue(csv.contains("ID,Emotion,Emotion_Arabic"))
        assertTrue(csv.contains("JOY,الفرح,الأصدقاء والعلاقات"))
    }

    @Test
    fun `launch MainActivity without crash and verify content view`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertNotNull(activity.window.decorView)
    }
}
