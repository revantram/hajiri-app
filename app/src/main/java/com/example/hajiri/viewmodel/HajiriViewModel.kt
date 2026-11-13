package com.example.hajiri.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.UserProfile
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import android.content.Context
import com.example.hajiri.util.AppUpdateChecker
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.MonthlySlipDailyRow
import com.example.hajiri.util.MonthlySlipData
import com.example.hajiri.util.UpdateCheckState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException

enum class HajiriTab(val label: String, val iconText: String) {
    HOME("होम", "🏠"),
    CALENDAR("कैलेंडर", "📅"),
    REPORTS("रिपोर्ट", "📊"),
    HISTORY("इतिहास", "📜"),
    SETTINGS("सेटिंग्स", "⚙️")
}

data class MonthTrend(
    val monthKey: String,
    val labelShort: String,
    val workedDays: Float,
    val isSelected: Boolean
)

data class PreviousMonthBalanceItem(
    val monthKey: String,
    val monthTitle: String,
    val remainingAmount: Double,
    val isReceived: Boolean
)

class HajiriViewModel(
    private val repository: HajiriRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _currentTab = MutableStateFlow(HajiriTab.HOME)
    val currentTab: StateFlow<HajiriTab> = _currentTab.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private fun <T> Flow<T>.guardStream(fallback: T, label: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        _syncError.value = rootError.message ?: "क्लाउड डेटा सिंक करने में समस्या"
        emit(fallback)
    }

    val attendance: StateFlow<Map<String, AttendanceRecord>> =
        repository.observeAttendance(currentUserId)
            .guardStream(emptyMap(), "attendance")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val otHours: StateFlow<Map<String, Float>> =
        repository.observeOvertime(currentUserId)
            .guardStream(emptyMap(), "overtime")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val advances: StateFlow<List<AdvanceRecord>> =
        repository.observeAdvances(currentUserId)
            .guardStream(emptyList(), "advances")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val settings: StateFlow<UserSettings> =
        repository.observeSettings(currentUserId)
            .guardStream(UserSettings(), "settings")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserSettings())

    val currentUser: StateFlow<UserProfile?> =
        repository.observeProfile(currentUserId)
            .guardStream(null, "profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val todayKey = DateUtils.getTodayKey()
    val todayMonthKey = DateUtils.getTodayMonthKey()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _selectedReportMonth = MutableStateFlow(todayMonthKey)
    val selectedReportMonth: StateFlow<String> = _selectedReportMonth.asStateFlow()

    private val _selectedHistoryMonth = MutableStateFlow(todayMonthKey)
    val selectedHistoryMonth: StateFlow<String> = _selectedHistoryMonth.asStateFlow()

    // Selected date key for Calendar day popup dialog
    private val _selectedCalendarDateKey = MutableStateFlow<String?>(null)
    val selectedCalendarDateKey: StateFlow<String?> = _selectedCalendarDateKey.asStateFlow()

    // Dialog state holders
    var showSalaryDialog = MutableStateFlow(false)
    var showWorkDaysDialog = MutableStateFlow(false)
    var showAdvanceDialog = MutableStateFlow(false)
    var showClearMonthDialog = MutableStateFlow(false)
    var showClearAllDialog = MutableStateFlow(false)
    var showProfileDialog = MutableStateFlow(false)
    var showThemeDialog = MutableStateFlow(false)
    var showUpdateDialog = MutableStateFlow(false)
    var showPrevBalanceDialog = MutableStateFlow(false)

    // Auto-update state
    private val _autoUpdateCheckEnabled = MutableStateFlow(true)
    val autoUpdateCheckEnabled: StateFlow<Boolean> = _autoUpdateCheckEnabled.asStateFlow()

    private val _updateRepoSlug = MutableStateFlow(AppUpdateChecker.DEFAULT_GITHUB_REPO)
    val updateRepoSlug: StateFlow<String> = _updateRepoSlug.asStateFlow()

    private val _installedVersionName = MutableStateFlow("1.0")
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private var hasRunStartupUpdateCheck = false

    // Current Month Stats for Home
    val homeStats: StateFlow<MonthlyStats> = combine(
        attendance, otHours, advances, settings
    ) { att, ot, adv, sett ->
        calculateMonthlyStats(todayMonthKey, att, ot, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthlyStats(todayMonthKey))

    // Selected Report Month Stats
    val reportStats: StateFlow<MonthlyStats> = combine(
        selectedReportMonth, attendance, otHours, advances, settings
    ) { mKey, att, ot, adv, sett ->
        calculateMonthlyStats(mKey, att, ot, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthlyStats(todayMonthKey))

    // 6-Month trends for report screen
    val sixMonthTrends: StateFlow<List<MonthTrend>> = combine(
        selectedReportMonth, attendance
    ) { selMonth, att ->
        val now = YearMonth.now()
        val list = mutableListOf<MonthTrend>()
        for (i in 5 downTo 0) {
            val ym = now.minusMonths(i.toLong())
            val mKey = ym.toString()
            var p = 0
            var h = 0
            att.forEach { (k, v) ->
                if (k.startsWith(mKey)) {
                    if (v.status == AttendanceStatus.PRESENT) p++
                    else if (v.status == AttendanceStatus.HALF) h++
                }
            }
            val worked = p + (h * 0.5f)
            val monthIdx = ym.monthValue - 1
            val shortLabel = DateUtils.HINDI_MONTHS[monthIdx].take(3)
            list.add(MonthTrend(mKey, shortLabel, worked, mKey == selMonth))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Streak stats
    val streakPair: StateFlow<Pair<Int, Int>> = combine(attendance, settings) { att, _ ->
        DateUtils.calculateStreak(att, todayKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Pair(0, 0))

    // Previous 6 completed months' remaining balance summary
    val previousMonthsSummary: StateFlow<List<PreviousMonthBalanceItem>> = combine(
        attendance, otHours, advances, settings
    ) { att, ot, adv, sett ->
        val now = YearMonth.now()
        (1..6).map { offset ->
            val ym = now.minusMonths(offset.toLong())
            val mKey = ym.toString()
            val rawBal = calculateSingleMonthRawNet(mKey, att, ot, adv, sett)
            PreviousMonthBalanceItem(
                monthKey = mKey,
                monthTitle = DateUtils.formatHindiMonthYear(ym),
                remainingAmount = rawBal,
                isReceived = mKey in sett.settledMonths
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun syncGoogleProfileIfNeeded(name: String, email: String) {
        viewModelScope.launch {
            try {
                repository.ensureUserProfile(name = name, email = email)
                _syncError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile sync warning", e)
            }
        }
    }

    fun updateProfileDetails(name: String, phone: String) {
        viewModelScope.launch {
            try {
                repository.updateProfileDetails(name, phone)
                showProfileDialog.value = false
                _toastEvent.emit("✅ प्रोफ़ाइल अपडेट हो गई!")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ प्रोफ़ाइल सेव नहीं हो सकी")
            }
        }
    }

    fun dismissSyncError() {
        _syncError.value = null
    }

    fun setTab(tab: HajiriTab) {
        _currentTab.value = tab
    }

    fun markToday(status: AttendanceStatus) {
        viewModelScope.launch {
            try {
                val current = attendance.value[todayKey]?.status
                if (current == status) {
                    repository.removeAttendance(todayKey)
                    _toastEvent.emit("हाजिरी हटाई")
                } else {
                    repository.setAttendance(todayKey, status)
                    _toastEvent.emit("${status.labelHindi} दर्ज ✓")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ क्लाउड सिंक में त्रुटि")
            }
        }
    }

    fun saveTodayNote(note: String) {
        viewModelScope.launch {
            try {
                repository.saveNote(todayKey, note)
                _toastEvent.emit("📝 नोट क्लाउड में सेव!")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ नोट सेव नहीं हो सका")
            }
        }
    }

    fun addTodayOvertime(hours: Float) {
        viewModelScope.launch {
            if (hours <= 0f) {
                _toastEvent.emit("⚠️ सही घंटे डालें")
                return@launch
            }
            try {
                repository.addOvertime(todayKey, hours)
                _toastEvent.emit("⏰ ${hours}h OT जोड़ा")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ OT सेव नहीं हो सका")
            }
        }
    }

    fun openCalendarDayDialog(dateKey: String) {
        viewModelScope.launch {
            val date = DateUtils.parseDateKey(dateKey)
            if (date != null && date.isAfter(LocalDate.now())) {
                _toastEvent.emit("⏳ भविष्य की तारीख नहीं")
                return@launch
            }
            _selectedCalendarDateKey.value = dateKey
        }
    }

    fun closeCalendarDayDialog() {
        _selectedCalendarDateKey.value = null
    }

    fun saveCalendarDayAttendance(dateKey: String, status: AttendanceStatus?, note: String) {
        viewModelScope.launch {
            try {
                when {
                    status != null -> {
                        repository.setAttendance(dateKey, status, note)
                        _toastEvent.emit("${status.shortCode} (${status.labelHindi}) दर्ज ✓")
                    }
                    note.isNotBlank() -> {
                        repository.saveNote(dateKey, note)
                        _toastEvent.emit("📝 नोट सेव!")
                    }
                    else -> {
                        repository.removeAttendance(dateKey)
                        _toastEvent.emit("हाजिरी हटाई")
                    }
                }
                _selectedCalendarDateKey.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ हाजिरी सेव नहीं हो सकी")
            }
        }
    }

    fun clearCalendarDayAttendance(dateKey: String) {
        viewModelScope.launch {
            try {
                repository.removeAttendance(dateKey)
                _selectedCalendarDateKey.value = null
                _toastEvent.emit("हाजिरी हटाई")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ हाजिरी हटाई नहीं जा सकी")
            }
        }
    }

    fun toggleCalendarDay(dateKey: String) {
        openCalendarDayDialog(dateKey)
    }

    fun changeCalendarMonth(offset: Int) {
        _calendarMonth.value = _calendarMonth.value.plusMonths(offset.toLong())
    }

    fun setReportMonth(monthKey: String) {
        _selectedReportMonth.value = monthKey
    }

    fun setHistoryMonth(monthKey: String) {
        _selectedHistoryMonth.value = monthKey
    }

    fun addAdvance(amount: Double, reason: String) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                _toastEvent.emit("⚠️ रकम डालें")
                return@launch
            }
            try {
                repository.addAdvance(amount, reason, todayKey)
                _toastEvent.emit("💳 ₹${DateUtils.formatInr(amount)} एडवांस दर्ज")
                showAdvanceDialog.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ एडवांस सेव नहीं हो सका")
            }
        }
    }

    fun deleteAdvance(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteAdvance(id)
                _toastEvent.emit("🗑️ एडवांस मिटाया")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ एडवांस हटाने में त्रुटि")
            }
        }
    }

    fun updateSalarySettings(
        salary: Double,
        dailyWage: Double,
        wageMode: WageMode,
        otRate: Double
    ) {
        viewModelScope.launch {
            try {
                val cur = settings.value
                repository.saveSettings(
                    salary = salary,
                    dailyWage = dailyWage,
                    wageMode = wageMode.id,
                    otRate = otRate,
                    workDays = cur.workDays,
                    themeStyle = cur.themeStyle
                )
                _toastEvent.emit("💰 ${wageMode.labelHindi} सेटिंग सेव!")
                showSalaryDialog.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ सेटिंग सेव नहीं हो सकी")
            }
        }
    }

    fun updateWorkDays(workDays: Int) {
        viewModelScope.launch {
            try {
                val cur = settings.value
                repository.saveSettings(
                    salary = cur.salary,
                    dailyWage = cur.dailyWage,
                    wageMode = cur.wageMode,
                    otRate = cur.otRate,
                    workDays = workDays,
                    themeStyle = cur.themeStyle
                )
                _toastEvent.emit("✅ कार्यदिवस सेव!")
                showWorkDaysDialog.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ कार्यदिवस सेव नहीं हो सका")
            }
        }
    }

    fun updateThemeStyle(themeStyle: AppThemeStyle) {
        viewModelScope.launch {
            try {
                repository.saveThemeStyle(themeStyle)
                showThemeDialog.value = false
                _toastEvent.emit("🎨 थीम सेट: ${themeStyle.labelHindi}")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ थीम सेव नहीं हो सकी")
            }
        }
    }

    fun clearMonthData() {
        viewModelScope.launch {
            try {
                repository.clearMonth(todayMonthKey)
                showClearMonthDialog.value = false
                _toastEvent.emit("🗑️ इस महीने का डेटा मिटाया")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ डेटा मिटाने में त्रुटि")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                showClearAllDialog.value = false
                _toastEvent.emit("⛔ सारा डेटा मिटाया")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ डेटा मिटाने में त्रुटि")
            }
        }
    }

    fun getReportText(monthKey: String): String {
        val stats = calculateMonthlyStats(
            monthKey,
            attendance.value,
            otHours.value,
            advances.value,
            settings.value
        )
        val name = currentUser.value?.name ?: ""
        val hindiMonth = DateUtils.formatHindiMonthYear(monthKey)

        return buildString {
            append("📋 हाजिरी रिपोर्ट")
            if (name.isNotBlank()) append(" — $name")
            append("\n$hindiMonth\n")
            append("━━━━━━━━━━━━━━━━━\n")
            append("✅ हाजिर: ${stats.presentDays} दिन\n")
            append("❌ गैरहाजिर: ${stats.absentDays} दिन\n")
            append("🌓 आधा दिन: ${stats.halfDays} दिन\n")
            append("✈️ छुट्टी: ${stats.leaveDays} दिन\n")
            append("⏰ ओवरटाइम: ${stats.totalOtHours} घंटे\n")
            append("💳 एडवांस: ₹${DateUtils.formatInr(stats.totalAdvance)}\n")
            if (stats.previousMonthRawBalance > 0.0) {
                val prevStatus = if (stats.isPreviousMonthReceived) "ले लिया गया (₹0 जोड़ा)" else "+₹${DateUtils.formatInr(stats.carryForwardAdded)}"
                append("💼 पिछला बाकी (${stats.previousMonthTitle}): $prevStatus\n")
            }
            append("━━━━━━━━━━━━━━━━━\n")
            append("📊 उपस्थिति: ${stats.attendancePercentage}%\n")
            append("💰 कुल कमाई: ₹${DateUtils.formatInr(stats.baseEarnings + stats.otEarnings)}\n")
            append("🤑 हाथ में: ₹${DateUtils.formatInr(stats.netTakeHome)}\n")
            append("━━━━━━━━━━━━━━━━━\n")
            append("📱 हाजिरी ऐप")
        }
    }

    fun toggleMonthSettled(monthKey: String, isSettled: Boolean) {
        if (monthKey.isBlank()) return
        viewModelScope.launch {
            try {
                repository.setMonthSettled(monthKey, isSettled)
                val title = DateUtils.formatHindiMonthYear(monthKey)
                _toastEvent.emit(
                    if (isSettled) "✅ $title की सैलरी 'ले लिया गया' मार्क की गई"
                    else "💼 $title का बाकी पैसा कुल सैलरी में जोड़ा गया"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ पिछला हिसाब अपडेट नहीं हो सका")
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    fun initUpdateState(context: Context) {
        val appContext = context.applicationContext
        _autoUpdateCheckEnabled.value = AppUpdateChecker.isAutoCheckEnabled(appContext)
        _updateRepoSlug.value = AppUpdateChecker.getSavedRepoSlug(appContext)
        _installedVersionName.value = AppUpdateChecker.getInstalledVersionName(appContext)
        if (_autoUpdateCheckEnabled.value && !hasRunStartupUpdateCheck) {
            hasRunStartupUpdateCheck = true
            checkForAppUpdate(appContext, userInitiated = false)
        }
    }

    fun setAutoUpdateCheckEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        AppUpdateChecker.setAutoCheckEnabled(appContext, enabled)
        _autoUpdateCheckEnabled.value = enabled
        showMessage(
            if (enabled) "🔄 ऑटो अपडेट चेक चालू किया गया"
            else "⏸️ ऑटो अपडेट चेक बंद किया गया"
        )
    }

    fun saveUpdateRepoSlug(context: Context, rawSlug: String) {
        val appContext = context.applicationContext
        val saved = AppUpdateChecker.saveRepoSlug(appContext, rawSlug)
        _updateRepoSlug.value = saved
        checkForAppUpdate(appContext, userInitiated = true)
    }

    fun checkForAppUpdate(context: Context, userInitiated: Boolean = true) {
        val appContext = context.applicationContext
        _installedVersionName.value = AppUpdateChecker.getInstalledVersionName(appContext)
        val repoSlug = _updateRepoSlug.value.ifBlank { AppUpdateChecker.getSavedRepoSlug(appContext) }
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            val result = AppUpdateChecker.checkForUpdate(appContext, repoSlug)
            _updateCheckState.value = result
            when (result) {
                is UpdateCheckState.UpdateAvailable -> {
                    showUpdateDialog.value = true
                }
                is UpdateCheckState.UpToDate -> {
                    if (userInitiated) {
                        _toastEvent.emit("✅ आपका ऐप पहले से अपडेट है (v${result.currentVersion})")
                    }
                }
                is UpdateCheckState.Error -> {
                    if (userInitiated) {
                        _toastEvent.emit("⚠️ ${result.message}")
                    }
                }
                else -> Unit
            }
        }
    }

    fun buildMonthlySlipData(
        monthKey: String,
        siteOrCompany: String = "",
        extraWorkBonus: Double = 0.0,
        extraWorkNote: String = ""
    ): MonthlySlipData {
        val currentAtt = attendance.value
        val currentOt = otHours.value
        val currentAdv = advances.value
        val currentSett = settings.value
        val profile = currentUser.value

        val stats = calculateMonthlyStats(
            monthKey = monthKey,
            att = currentAtt,
            ot = currentOt,
            adv = currentAdv,
            sett = currentSett
        )

        val monthAdvances = currentAdv.filter { it.dateKey.startsWith(monthKey) }
        val allDates = (currentAtt.keys + currentOt.keys)
            .filter { it.startsWith(monthKey) }
            .distinct()
            .sorted()

        val dailyRows = allDates.map { dateKey ->
            val rec = currentAtt[dateKey]
            val ot = currentOt[dateKey] ?: 0f
            val parsedDate = DateUtils.parseDateKey(dateKey)
            val dateFormatted = if (parsedDate != null) DateUtils.formatHindiDate(parsedDate) else dateKey
            val dayShort = if (parsedDate != null) {
                DateUtils.HINDI_DAYS[parsedDate.dayOfWeek.value - 1]
            } else ""
            MonthlySlipDailyRow(
                dateKey = dateKey,
                dateFormatted = dateFormatted,
                dayShort = dayShort,
                statusCode = rec?.status?.shortCode ?: "-",
                statusLabel = rec?.status?.labelHindi ?: "",
                otHours = ot,
                note = rec?.note ?: ""
            )
        }

        return MonthlySlipData(
            monthKey = monthKey,
            monthTitle = DateUtils.formatHindiMonthYear(monthKey),
            employeeName = profile?.name?.ifBlank { "हाजिरी यूजर" } ?: "हाजिरी यूजर",
            employeePhone = profile?.phone ?: "",
            employeeEmail = profile?.email ?: "",
            siteOrCompany = siteOrCompany.trim(),
            extraWorkBonus = maxOf(0.0, extraWorkBonus),
            extraWorkNote = extraWorkNote.trim(),
            settings = currentSett,
            stats = stats,
            advances = monthAdvances,
            dailyRows = dailyRows
        )
    }

    private fun calculateSingleMonthRawNet(
        monthKey: String,
        att: Map<String, AttendanceRecord>,
        ot: Map<String, Float>,
        adv: List<AdvanceRecord>,
        sett: UserSettings
    ): Double {
        var p = 0
        var h = 0
        var totalOt = 0f
        var totalAdv = 0.0

        att.forEach { (k, v) ->
            if (k.startsWith(monthKey)) {
                when (v.status) {
                    AttendanceStatus.PRESENT -> p++
                    AttendanceStatus.HALF -> h++
                    else -> Unit
                }
            }
        }
        ot.forEach { (k, hrs) ->
            if (k.startsWith(monthKey)) totalOt += hrs
        }
        adv.forEach {
            if (it.dateKey.startsWith(monthKey)) totalAdv += it.amount
        }

        val workDays = if (sett.workDays > 0) sett.workDays else 26
        val wageMode = WageMode.fromId(sett.wageMode)
        val perDayRate = when (wageMode) {
            WageMode.DAILY -> sett.dailyWage
            WageMode.MONTHLY -> if (sett.salary > 0) sett.salary / workDays else 0.0
        }
        val baseEarnings = (p * perDayRate) + (h * (perDayRate / 2.0))
        val otEarnings = totalOt * sett.otRate
        return maxOf(0.0, baseEarnings + otEarnings - totalAdv)
    }

    private fun calculateMonthlyStats(
        monthKey: String,
        att: Map<String, AttendanceRecord>,
        ot: Map<String, Float>,
        adv: List<AdvanceRecord>,
        sett: UserSettings
    ): MonthlyStats {
        var p = 0
        var a = 0
        var h = 0
        var l = 0
        var totalOt = 0f
        var totalAdv = 0.0

        att.forEach { (k, v) ->
            if (k.startsWith(monthKey)) {
                when (v.status) {
                    AttendanceStatus.PRESENT -> p++
                    AttendanceStatus.ABSENT -> a++
                    AttendanceStatus.HALF -> h++
                    AttendanceStatus.LEAVE -> l++
                }
            }
        }

        ot.forEach { (k, hrs) ->
            if (k.startsWith(monthKey)) {
                totalOt += hrs
            }
        }

        adv.forEach {
            if (it.dateKey.startsWith(monthKey)) {
                totalAdv += it.amount
            }
        }

        val workDays = if (sett.workDays > 0) sett.workDays else 26
        val wageMode = WageMode.fromId(sett.wageMode)
        val perDayRate = when (wageMode) {
            WageMode.DAILY -> sett.dailyWage
            WageMode.MONTHLY -> if (sett.salary > 0) sett.salary / workDays else 0.0
        }
        val baseEarnings = (p * perDayRate) + (h * (perDayRate / 2.0))
        val otEarnings = totalOt * sett.otRate

        val prevYearMonth = runCatching { YearMonth.parse(monthKey).minusMonths(1) }.getOrNull()
        val prevMonthKey = prevYearMonth?.toString() ?: ""
        val prevMonthTitle = if (prevYearMonth != null) DateUtils.formatHindiMonthYear(prevYearMonth) else ""
        val prevRawBalance = if (prevMonthKey.isNotBlank()) {
            calculateSingleMonthRawNet(prevMonthKey, att, ot, adv, sett)
        } else 0.0
        val isPrevReceived = prevMonthKey.isNotBlank() && prevMonthKey in sett.settledMonths
        val carryForwardAdded = if (!isPrevReceived) prevRawBalance else 0.0

        val netTakeHome = maxOf(0.0, baseEarnings + otEarnings - totalAdv + carryForwardAdded)
        val workedDays = p + (h * 0.5f)
        val totalMarked = p + a + h + l
        val pct = if (totalMarked > 0) ((workedDays / totalMarked) * 100).toInt() else 0

        return MonthlyStats(
            yearMonth = monthKey,
            presentDays = p,
            absentDays = a,
            halfDays = h,
            leaveDays = l,
            totalOtHours = totalOt,
            totalAdvance = totalAdv,
            baseEarnings = baseEarnings,
            otEarnings = otEarnings,
            perDayRate = perDayRate,
            previousMonthKey = prevMonthKey,
            previousMonthTitle = prevMonthTitle,
            previousMonthRawBalance = prevRawBalance,
            isPreviousMonthReceived = isPrevReceived,
            carryForwardAdded = carryForwardAdded,
            netTakeHome = netTakeHome,
            attendancePercentage = pct,
            workedDays = workedDays
        )
    }

    private companion object {
        const val TAG = "HajiriVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
