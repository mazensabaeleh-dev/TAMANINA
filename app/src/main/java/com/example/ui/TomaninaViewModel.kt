package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CheckInRecord
import com.example.data.model.EmotionType
import com.example.data.model.SchoolCycle
import com.example.data.model.SupportTicket
import com.example.data.model.TeamMember
import com.example.data.repository.TomaninaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
    STUDENT_CHECK_IN,
    COUNSELOR_DASHBOARD,
    WEB_PORTAL
}

class TomaninaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TomaninaViewModel"
    }

    private val repository: TomaninaRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TomaninaRepository(database, application)
    }

    val checkIns: StateFlow<List<CheckInRecord>> = repository.allCheckIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<SupportTicket>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycles: StateFlow<List<SchoolCycle>> = repository.allCycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCycle: StateFlow<SchoolCycle?> = repository.activeCycle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPulseActive: StateFlow<Boolean> = repository.isPulseActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val schoolName: StateFlow<String> = repository.schoolName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ثانوية الأمل الأكاديمية النموذجية")

    val teamMembers: StateFlow<List<TeamMember>> = repository.allTeamMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLoggedInMember: StateFlow<TeamMember?> = repository.currentLoggedInMember
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiFeedbackMessage = MutableStateFlow<String?>(null)
    val uiFeedbackMessage: StateFlow<String?> = _uiFeedbackMessage.asStateFlow()

    fun clearFeedbackMessage() {
        _uiFeedbackMessage.value = null
    }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cleanEmail = email.trim()
                val cleanPass = pass.trim()
                if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
                    onResult(false, "يرجى إدخال البريد الإلكتروني وكلمة المرور")
                    return@launch
                }
                val success = repository.login(cleanEmail, cleanPass)
                if (success) {
                    Log.i(TAG, "Counselor login successful for: $cleanEmail")
                    onResult(true, null)
                } else {
                    Log.w(TAG, "Counselor login failed for: $cleanEmail")
                    onResult(false, "البريد الإلكتروني أو كلمة المرور غير صحيحة")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login exception", e)
                onResult(false, "حدث خطأ غير متوقع أثناء تسجيل الدخول. يرجى المحاولة لاحقاً.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        try {
            repository.logout()
            Log.i(TAG, "User logged out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
        }
    }

    fun recordCheckIn(emotion: EmotionType, reasonCategory: String?, note: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cycleId = activeCycle.value?.id ?: 1L
                repository.recordCheckIn(emotion, reasonCategory, note, cycleId)
                Log.i(TAG, "Check-in recorded: ${emotion.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording check-in", e)
                _uiFeedbackMessage.value = "تعذر تسجيل النبضة، يرجى المحاولة مرة أخرى."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitSupportTicket(message: String, contactInfo: String?, urgency: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (message.isBlank()) {
                    _uiFeedbackMessage.value = "يرجى كتابة رسالتك أولاً"
                    return@launch
                }
                repository.submitSupportTicket(message, contactInfo, urgency)
                Log.i(TAG, "Support ticket submitted with urgency: $urgency")
                _uiFeedbackMessage.value = "تم إرسال طلبك بسرية تامة إلى فريق التوجيه الطلابي."
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting support ticket", e)
                _uiFeedbackMessage.value = "تعذر إرسال الطلب في الوقت الحالي. يرجى المحاولة لاحقاً."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTicketStatus(ticketId: Long, status: String, notes: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateTicketStatus(ticketId, status, notes)
                Log.i(TAG, "Updated ticket $ticketId status to: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ticket status", e)
                _uiFeedbackMessage.value = "تعذر تحديث حالة التذكرة."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTicket(ticketId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTicket(ticketId)
                Log.i(TAG, "Deleted ticket $ticketId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ticket", e)
                _uiFeedbackMessage.value = "تعذر حذف التذكرة."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePulse(active: Boolean) {
        viewModelScope.launch {
            try {
                repository.setPulseActive(active)
                Log.i(TAG, "Pulse active state changed to: $active")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling pulse", e)
            }
        }
    }

    fun updateSchoolName(newName: String) {
        viewModelScope.launch {
            try {
                if (newName.isNotBlank()) {
                    repository.updateSchoolName(newName.trim())
                    Log.i(TAG, "School name updated to: $newName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating school name", e)
            }
        }
    }

    fun startNewCycle(title: String, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (title.isBlank()) {
                    _uiFeedbackMessage.value = "يرجى إدخال عنوان الفصل الدراسي"
                    return@launch
                }
                repository.startNewCycle(title.trim(), description?.trim())
                Log.i(TAG, "Started new school cycle: $title")
                _uiFeedbackMessage.value = "تم بدء دورة النبض المدرسي بنجاح."
            } catch (e: Exception) {
                Log.e(TAG, "Error starting cycle", e)
                _uiFeedbackMessage.value = "تعذر بدء الدورة الجديدة."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun purgeOldData(days: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val purgedCount = repository.purgeDataOlderThanDays(days)
                Log.i(TAG, "Purged $purgedCount old records older than $days days")
                _uiFeedbackMessage.value = "تم تطهير $purgedCount سجلاً قديماً بنجاح."
            } catch (e: Exception) {
                Log.e(TAG, "Error purging old data", e)
                _uiFeedbackMessage.value = "تعذر تطهير البيانات القديمة."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearAllData()
                Log.i(TAG, "All check-ins cleared from database")
                _uiFeedbackMessage.value = "تمت إعادة ضبط البيانات ومسح السجلات."
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all data", e)
                _uiFeedbackMessage.value = "تعذر مسح البيانات."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateCsv(records: List<CheckInRecord>): String {
        return try {
            repository.generateExportCsv(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV", e)
            ""
        }
    }

    fun addTeamMember(name: String, email: String, role: String, permissions: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (name.isBlank() || email.isBlank()) {
                    _uiFeedbackMessage.value = "يرجى تعبئة الاسم والبريد الإلكتروني"
                    return@launch
                }
                repository.addTeamMember(name, email, role, permissions)
                Log.i(TAG, "Added team member: $email")
                _uiFeedbackMessage.value = "تم إضافة عضو الفريق بنجاح."
            } catch (e: Exception) {
                Log.e(TAG, "Error adding team member", e)
                _uiFeedbackMessage.value = "تعذر إضافة العضو، تحقق من صحة البيانات."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTeamMember(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTeamMember(id)
                Log.i(TAG, "Deleted team member: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting team member", e)
                _uiFeedbackMessage.value = "تعذر حذف العضو."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTeamMember(member: TeamMember) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateTeamMember(member)
                Log.i(TAG, "Updated team member: ${member.email}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating team member", e)
                _uiFeedbackMessage.value = "تعذر تحديث بيانات العضو."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun switchActiveMember(member: TeamMember) {
        repository.switchActiveMember(member)
        _uiFeedbackMessage.value = "تم التبديل إلى حساب: ${member.name} (${member.role})"
    }
}
