package com.example.hajiri

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.data.authStateFlow
import com.example.hajiri.data.signOutFromGoogle
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.WageMode
import com.example.hajiri.ui.components.AddAdvanceDialog
import com.example.hajiri.ui.components.AppUpdateDialog
import com.example.hajiri.ui.components.CalendarDayDialog
import com.example.hajiri.ui.components.PreviousBalanceDialog
import com.example.hajiri.ui.components.ConfirmActionDialog
import com.example.hajiri.ui.components.PhoneProfileDialog
import com.example.hajiri.ui.components.SalaryDialog
import com.example.hajiri.ui.components.ThemePickerDialog
import com.example.hajiri.ui.components.WorkDaysDialog
import com.example.hajiri.ui.components.WorkSitesDialog
import com.example.hajiri.ui.screens.AuthScreen
import com.example.hajiri.ui.screens.ExpenseScreen
import com.example.hajiri.ui.screens.HistoryScreen
import com.example.hajiri.ui.screens.HomeScreen
import com.example.hajiri.ui.screens.ReportsScreen
import com.example.hajiri.ui.screens.SettingsScreen
import com.example.hajiri.ui.theme.HajiriTheme
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.util.DailyReminderScheduler
import com.example.hajiri.util.NetworkConnectivityObserver
import com.example.hajiri.viewmodel.HajiriTab
import com.example.hajiri.viewmodel.HajiriViewModel
import com.example.hajiri.widget.HajiriWidgetManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    private val openHomeTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeReminderIntent(intent)

        setContent {
            HajiriTheme {
                AppContent(openHomeTrigger = openHomeTrigger.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeReminderIntent(intent)
    }

    private fun consumeReminderIntent(launchIntent: Intent?) {
        if (launchIntent?.getBooleanExtra(DailyReminderScheduler.EXTRA_OPEN_HOME_ATTENDANCE, false) == true) {
            launchIntent.removeExtra(DailyReminderScheduler.EXTRA_OPEN_HOME_ATTENDANCE)
            openHomeTrigger.intValue += 1
        }
    }
}

@Composable
fun AppContent(openHomeTrigger: Int = 0) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* authStateFlow updates automatically */ }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: HajiriViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId).apply {
                            runCatching {
                                firestoreSettings = firestoreSettings {
                                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                                }
                            }
                        }
                        val connectivityObserver = NetworkConnectivityObserver(app)
                        HajiriViewModel(
                            repository = HajiriRepository(db, auth),
                            currentUserId = uid,
                            isOnlineFlow = connectivityObserver.isOnlineFlow
                        )
                    }
                }
            )

            LaunchedEffect(uid) {
                viewModel.syncGoogleProfileIfNeeded(
                    name = user.displayName ?: "हाजिरी यूजर",
                    email = user.email ?: ""
                )
                viewModel.initUpdateState(context)
                viewModel.initReminderState(context)
            }

            val settings by viewModel.settings.collectAsState()
            val selectedTheme = AppThemeStyle.fromId(settings.themeStyle)

            HajiriTheme(themeStyle = selectedTheme) {
                MainApp(
                    viewModel = viewModel,
                    googleEmail = user.email ?: "",
                    openHomeTrigger = openHomeTrigger,
                    onSignOut = {
                        DailyReminderScheduler.clearTodayAttendanceStatus(context)
                        signOutFromGoogle(credentialManager, scope)
                    }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: HajiriViewModel,
    googleEmail: String,
    openHomeTrigger: Int = 0,
    onSignOut: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val currentTab by viewModel.currentTab.collectAsState()
    val showHistoryScreen by viewModel.showHistoryScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val otHours by viewModel.otHours.collectAsState()
    val homeStats by viewModel.homeStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val selectedCalendarDateKey by viewModel.selectedCalendarDateKey.collectAsState()

    val showSalary by viewModel.showSalaryDialog.collectAsState()
    val showWorkDays by viewModel.showWorkDaysDialog.collectAsState()
    val showAdvance by viewModel.showAdvanceDialog.collectAsState()
    val showProfile by viewModel.showProfileDialog.collectAsState()
    val showTheme by viewModel.showThemeDialog.collectAsState()
    val showUpdate by viewModel.showUpdateDialog.collectAsState()
    val showPrevBalance by viewModel.showPrevBalanceDialog.collectAsState()
    val showSites by viewModel.showSitesDialog.collectAsState()
    val previousMonthsSummary by viewModel.previousMonthsSummary.collectAsState()
    val autoUpdateEnabled by viewModel.autoUpdateCheckEnabled.collectAsState()
    val installedVersion by viewModel.installedVersionName.collectAsState()
    val updateRepoSlug by viewModel.updateRepoSlug.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()
    val updateDownloadState by viewModel.updateDownloadState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val activeSite = viewModel.getActiveSite(settings)
    val todayKey by viewModel.todayKeyState.collectAsState()
    val todayRecord = attendance[todayKey]

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(openHomeTrigger) {
        if (openHomeTrigger > 0) {
            viewModel.closeHistoryScreen()
            viewModel.setTab(HajiriTab.HOME)
            viewModel.refreshTodayDate()
        }
    }

    LaunchedEffect(todayKey, homeStats, todayRecord, activeSite.id, activeSite.name) {
        DailyReminderScheduler.recordTodayAttendanceStatus(context, todayKey, todayRecord != null)
        HajiriWidgetManager.syncFromApp(
            context = context,
            todayKey = todayKey,
            todayStatus = todayRecord?.status,
            totalMonthlyEarnings = homeStats.baseEarnings + homeStats.otEarnings,
            workedDays = homeStats.workedDays,
            activeSiteId = activeSite.id,
            activeSiteName = activeSite.name,
            isSignedIn = true
        )
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(syncError) {
        if (!syncError.isNullOrBlank()) {
            snackbarHostState.showSnackbar("⚠️ $syncError")
            viewModel.dismissSyncError()
        }
    }

    // Android back navigation: close History overlay first, or return to Home tab
    BackHandler(enabled = showHistoryScreen || currentTab != HajiriTab.HOME) {
        if (showHistoryScreen) {
            viewModel.closeHistoryScreen()
        } else {
            viewModel.setTab(HajiriTab.HOME)
        }
    }

    // Ethereal iPhone Liquid Glass Iridescent Ambient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.meshTopLeft.copy(alpha = if (palette.isDark) 0.92f else 0.72f),
                        palette.background,
                        palette.meshCenter.copy(alpha = if (palette.isDark) 0.85f else 0.58f),
                        palette.meshTopRight.copy(alpha = if (palette.isDark) 0.80f else 0.55f),
                        palette.meshBottom.copy(alpha = if (palette.isDark) 0.90f else 0.68f)
                    )
                )
            )
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 95.dp)
                )
            },
            bottomBar = {
                FloatingLiquidDock(
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
                if (showHistoryScreen) {
                    HistoryScreen(viewModel = viewModel)
                } else {
                    when (currentTab) {
                        HajiriTab.HOME -> HomeScreen(viewModel = viewModel)
                        HajiriTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                        HajiriTab.EXPENSE -> ExpenseScreen(viewModel = viewModel)
                        HajiriTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            googleEmail = googleEmail,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }

        // Modals & Dialogs
        val calDate = selectedCalendarDateKey
        if (calDate != null) {
            val existingRecord = attendance[calDate]
            val existingOt = otHours[calDate] ?: 0f
            CalendarDayDialog(
                dateKey = calDate,
                initialStatus = existingRecord?.status,
                initialNote = existingRecord?.note ?: "",
                initialOtHours = existingOt,
                hasExistingRecord = existingRecord != null,
                onDismiss = { viewModel.closeCalendarDayDialog() },
                onClear = { viewModel.clearCalendarDayAttendance(calDate) },
                onSave = { status, note, otVal ->
                    viewModel.saveCalendarDayAttendance(calDate, status, note, otVal)
                }
            )
        }

        if (showSalary) {
            SalaryDialog(
                initialSalary = activeSite.salary,
                initialDailyWage = activeSite.dailyWage,
                initialWageMode = WageMode.fromId(activeSite.wageMode),
                initialOtRate = activeSite.otRate,
                onDismiss = { viewModel.showSalaryDialog.value = false },
                onSave = { sal, daily, mode, otr ->
                    viewModel.updateSalarySettings(sal, daily, mode, otr)
                }
            )
        }

        if (showWorkDays) {
            WorkDaysDialog(
                initialWorkDays = activeSite.workDays,
                onDismiss = { viewModel.showWorkDaysDialog.value = false },
                onSave = { wd -> viewModel.updateWorkDays(wd) }
            )
        }

        if (showTheme) {
            ThemePickerDialog(
                currentTheme = AppThemeStyle.fromId(settings.themeStyle),
                onDismiss = { viewModel.showThemeDialog.value = false },
                onSelectTheme = { theme -> viewModel.updateThemeStyle(theme) }
            )
        }

        if (showAdvance) {
            AddAdvanceDialog(
                onDismiss = { viewModel.showAdvanceDialog.value = false },
                onSave = { amt, reason, dateKey -> viewModel.addAdvance(amt, reason, dateKey) }
            )
        }

        if (showProfile) {
            PhoneProfileDialog(
                initialName = currentUser?.name ?: "",
                initialPhone = currentUser?.phone ?: "",
                onDismiss = { viewModel.showProfileDialog.value = false },
                onSave = { name, phone -> viewModel.updateProfileDetails(name, phone) }
            )
        }

        if (showUpdate) {
            AppUpdateDialog(
                installedVersion = installedVersion,
                repoSlug = updateRepoSlug,
                updateState = updateState,
                downloadState = updateDownloadState,
                autoCheckEnabled = autoUpdateEnabled,
                onToggleAutoCheck = { enabled ->
                    viewModel.setAutoUpdateCheckEnabled(context, enabled)
                },
                onCheckNow = {
                    viewModel.checkForAppUpdate(context, userInitiated = true)
                },
                onSaveRepo = { slug ->
                    viewModel.saveUpdateRepoSlug(context, slug)
                },
                onStartDownload = { releaseInfo ->
                    viewModel.startInAppUpdateDownload(context, releaseInfo)
                },
                onCancelDownload = {
                    viewModel.cancelInAppUpdateDownload()
                },
                onInstallDownloaded = {
                    viewModel.installDownloadedUpdate(context)
                },
                onDismiss = { viewModel.showUpdateDialog.value = false }
            )
        }

        if (showPrevBalance) {
            PreviousBalanceDialog(
                months = previousMonthsSummary,
                onToggleSettled = { mKey, settled ->
                    viewModel.toggleMonthSettled(mKey, settled)
                },
                onPartialPayment = { mKey, paid ->
                    viewModel.recordMonthPartialPayment(mKey, paid)
                },
                onDismiss = { viewModel.showPrevBalanceDialog.value = false }
            )
        }

        if (showSites) {
            WorkSitesDialog(
                sites = viewModel.resolveSitesList(settings),
                activeSiteId = activeSite.id,
                onSwitchSite = { siteId -> viewModel.switchActiveSite(siteId) },
                onSaveSite = { site -> viewModel.addOrUpdateWorkSite(site) },
                onDeleteSite = { siteId -> viewModel.deleteWorkSite(siteId) },
                onDismiss = { viewModel.showSitesDialog.value = false }
            )
        }
    }
}

@Composable
fun FloatingLiquidDock(
    selectedTab: HajiriTab,
    onTabSelected: (HajiriTab) -> Unit
) {
    val palette = LocalHajiriPalette.current
    val navInsets = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                end = 18.dp,
                bottom = 10.dp + navInsets.calculateBottomPadding()
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color(0x29000000),
                    spotColor = palette.accent.copy(alpha = 0.28f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = palette.glassCardHigh.copy(alpha = if (palette.isDark) 0.88f else 0.86f),
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.glassBorderTop,
                        palette.glassBorderMid,
                        palette.glassBorderBottom
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to palette.glassSheenTop,
                            0.45f to Color.Transparent
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HajiriTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isSelected) palette.accent.copy(alpha = if (palette.isDark) 0.28f else 0.16f) else Color.Transparent,
                        border = if (isSelected) {
                            BorderStroke(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        palette.glassBorderTop,
                                        palette.accent.copy(alpha = 0.48f)
                                    )
                                )
                            )
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTabSelected(tab) }
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = tab.iconText,
                                fontSize = 19.sp
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) palette.accent else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
