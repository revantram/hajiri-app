package com.example.hajiri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.ui.components.AddAdvanceDialog
import com.example.hajiri.ui.components.ConfirmActionDialog
import com.example.hajiri.ui.components.SalaryDialog
import com.example.hajiri.ui.components.WorkDaysDialog
import com.example.hajiri.ui.screens.AuthScreen
import com.example.hajiri.ui.screens.CalendarScreen
import com.example.hajiri.ui.screens.HistoryScreen
import com.example.hajiri.ui.screens.HomeScreen
import com.example.hajiri.ui.screens.ReportsScreen
import com.example.hajiri.ui.screens.SettingsScreen
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.DarkBackground
import com.example.hajiri.ui.theme.GlassBorder
import com.example.hajiri.ui.theme.HajiriTheme
import com.example.hajiri.ui.theme.TextMuted
import com.example.hajiri.ui.theme.TextSecondary
import com.example.hajiri.viewmodel.HajiriTab
import com.example.hajiri.viewmodel.HajiriViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HajiriViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HajiriTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: HajiriViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isAuthVisible by viewModel.isAuthScreenVisible.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val showSalary by viewModel.showSalaryDialog.collectAsState()
    val showWorkDays by viewModel.showWorkDaysDialog.collectAsState()
    val showAdvance by viewModel.showAdvanceDialog.collectAsState()
    val showClearMonth by viewModel.showClearMonthDialog.collectAsState()
    val showClearAll by viewModel.showClearAllDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Android back navigation: if not on Home screen, Back returns to Home
    BackHandler(enabled = currentTab != HajiriTab.HOME || isAuthVisible) {
        if (isAuthVisible) {
            viewModel.showAuth(false)
        } else {
            viewModel.setTab(HajiriTab.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 75.dp)
                )
            },
            bottomBar = {
                HajiriBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    HajiriTab.HOME -> HomeScreen(viewModel = viewModel)
                    HajiriTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                    HajiriTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                    HajiriTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    HajiriTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Modals & Dialogs
        if (showSalary) {
            SalaryDialog(
                initialSalary = settings.salary,
                initialOtRate = settings.otRate,
                onDismiss = { viewModel.showSalaryDialog.value = false },
                onSave = { sal, otr -> viewModel.updateSalarySettings(sal, otr) }
            )
        }

        if (showWorkDays) {
            WorkDaysDialog(
                initialWorkDays = settings.workDays,
                onDismiss = { viewModel.showWorkDaysDialog.value = false },
                onSave = { wd -> viewModel.updateWorkDays(wd) }
            )
        }

        if (showAdvance) {
            AddAdvanceDialog(
                onDismiss = { viewModel.showAdvanceDialog.value = false },
                onSave = { amt, reason -> viewModel.addAdvance(amt, reason) }
            )
        }

        if (showClearMonth) {
            ConfirmActionDialog(
                title = "🗑️ इस महीने का डेटा मिटाएं?",
                message = "क्या आप वर्तमान महीने की सभी हाजिरी, OT और एडवांस रिकॉर्ड मिटाना चाहते हैं?",
                onDismiss = { viewModel.showClearMonthDialog.value = false },
                onConfirm = { viewModel.clearMonthData() }
            )
        }

        if (showClearAll) {
            ConfirmActionDialog(
                title = "⛔ सारा डेटा मिटाएं?",
                message = "सावधानी: यह कार्रवाई सभी महीनों की हाजिरी, OT, एडवांस और रिकॉर्ड को हमेशा के लिए हटा देगी!",
                onDismiss = { viewModel.showClearAllDialog.value = false },
                onConfirm = { viewModel.clearAllData() }
            )
        }

        // Auth Screen Overlay
        AnimatedVisibility(
            visible = isAuthVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AuthScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun HajiriBottomNavBar(
    selectedTab: HajiriTab,
    onTabSelected: (HajiriTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xF207071A),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        val navInsets = WindowInsets.navigationBars.asPaddingValues()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp + navInsets.calculateBottomPadding()
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HajiriTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) AccentIndigo.copy(alpha = 0.16f) else Color.Transparent,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab.iconText,
                            fontSize = 20.sp
                        )
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentIndigo else TextMuted
                        )
                    }
                }
            }
        }
    }
}
