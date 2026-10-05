package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.TomaninaViewModel
import com.example.ui.components.AdminLoginDialog
import com.example.ui.screens.CounselorDashboardScreen
import com.example.ui.screens.StudentCheckInScreen
import com.example.ui.screens.WebPortalScreen
import com.example.ui.theme.TomaninaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TomaninaTheme {
                // Ensure native right-to-left layout for Arabic text and components
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val viewModel: TomaninaViewModel = viewModel()
                    var currentDestination by remember { mutableStateOf(AppDestination.STUDENT_CHECK_IN) }
                    var showLoginDialog by remember { mutableStateOf(false) }

                    val checkIns by viewModel.checkIns.collectAsStateWithLifecycle()
                    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
                    val cycles by viewModel.cycles.collectAsStateWithLifecycle()
                    val activeCycle by viewModel.activeCycle.collectAsStateWithLifecycle()
                    val isPulseActive by viewModel.isPulseActive.collectAsStateWithLifecycle()
                    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
                    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
                    val currentLoggedInMember by viewModel.currentLoggedInMember.collectAsStateWithLifecycle()

                    BackHandler(enabled = currentDestination != AppDestination.STUDENT_CHECK_IN) {
                        currentDestination = AppDestination.STUDENT_CHECK_IN
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        when (currentDestination) {
                            AppDestination.STUDENT_CHECK_IN -> {
                                StudentCheckInScreen(
                                    schoolName = schoolName,
                                    isPulseActive = isPulseActive,
                                    onCheckInSubmit = { emotion, reason, note ->
                                        viewModel.recordCheckIn(emotion, reason, note)
                                    },
                                    onSubmitTicket = { message, contact, urgency ->
                                        viewModel.submitSupportTicket(message, contact, urgency)
                                    },
                                    onSwitchToCounselor = {
                                        if (currentLoggedInMember != null) {
                                            currentDestination = AppDestination.COUNSELOR_DASHBOARD
                                        } else {
                                            showLoginDialog = true
                                        }
                                    },
                                    onOpenWebPortal = {
                                        currentDestination = AppDestination.WEB_PORTAL
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppDestination.COUNSELOR_DASHBOARD -> {
                                CounselorDashboardScreen(
                                    schoolName = schoolName,
                                    checkIns = checkIns,
                                    tickets = tickets,
                                    cycles = cycles,
                                    activeCycle = activeCycle,
                                    isPulseActive = isPulseActive,
                                    teamMembers = teamMembers,
                                    currentLoggedInMember = currentLoggedInMember,
                                    onTogglePulse = { viewModel.togglePulse(it) },
                                    onUpdateSchoolName = { viewModel.updateSchoolName(it) },
                                    onUpdateTicketStatus = { id, status, notes ->
                                        viewModel.updateTicketStatus(id, status, notes)
                                    },
                                    onDeleteTicket = { id ->
                                        viewModel.deleteTicket(id)
                                    },
                                    onStartNewCycle = { title, desc ->
                                        viewModel.startNewCycle(title, desc)
                                    },
                                    onPurgeOldData = { days ->
                                        viewModel.purgeOldData(days)
                                    },
                                    onClearAllData = {
                                        viewModel.clearAllData()
                                    },
                                    onGenerateCsv = {
                                        viewModel.generateCsv(it)
                                    },
                                    onAddTeamMember = { name, email, role, perms ->
                                        viewModel.addTeamMember(name, email, role, perms)
                                    },
                                    onDeleteTeamMember = { id ->
                                        viewModel.deleteTeamMember(id)
                                    },
                                    onSwitchActiveMember = { member ->
                                        viewModel.switchActiveMember(member)
                                    },
                                    onLogout = {
                                        viewModel.logout()
                                        currentDestination = AppDestination.STUDENT_CHECK_IN
                                    },
                                    onSwitchToStudentMode = {
                                        currentDestination = AppDestination.STUDENT_CHECK_IN
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppDestination.WEB_PORTAL -> {
                                WebPortalScreen(
                                    onBack = {
                                        currentDestination = AppDestination.STUDENT_CHECK_IN
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }

                        if (showLoginDialog) {
                            AdminLoginDialog(
                                onDismiss = { showLoginDialog = false },
                                onLoginSubmit = { email, pass, onResult ->
                                    viewModel.login(email, pass) { success, err ->
                                        if (success) {
                                            showLoginDialog = false
                                            currentDestination = AppDestination.COUNSELOR_DASHBOARD
                                        }
                                        onResult(success, err)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
