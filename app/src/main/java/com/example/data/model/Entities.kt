package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_ins",
    indices = [
        Index(value = ["cycleId"]),
        Index(value = ["timestamp"])
    ]
)
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val emotion: String, // EmotionType name (e.g. "CALM")
    val reasonCategory: String? = null,
    val optionalNote: String? = null,
    val cycleId: Long = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val anonymousTag: String = "STD-" + (1000..9999).random()
)

@Entity(
    tableName = "support_tickets",
    indices = [
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class SupportTicket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticketCode: String,
    val message: String,
    val contactInfo: String? = null, // e.g. "أحمد فهد - 2/ب" or null if 100% anonymous
    val isAnonymous: Boolean = true,
    val urgency: String = "NORMAL", // NORMAL, URGENT, CRITICAL
    val status: String = "NEW", // NEW, IN_PROGRESS, CONTACTED, RESOLVED
    val counselorNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(
    tableName = "school_cycles",
    indices = [
        Index(value = ["isActive"])
    ]
)
data class SchoolCycle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val description: String? = null
)

@Entity(
    tableName = "team_members",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class TeamMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val role: String, // "مالك المنصة", "مدير المدرسة", "مرشد طلابي", "أخصائي نفسي", "محلل بيانات"
    val permissions: String = "كامل الصلاحيات", // "كامل الصلاحيات", "إدارة ومعالجة", "اطلاع وتحليل"
    val isOwner: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
