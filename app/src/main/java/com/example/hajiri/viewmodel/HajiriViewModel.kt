package com.example.hajiri.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.CategoryExpenseBreakdown
import com.example.hajiri.model.ExpenseCategory
import com.example.hajiri.model.ExpenseMonthSummary
import com.example.hajiri.model.ExpenseRecord
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.OvertimeRecord
import com.example.hajiri.model.UserProfile
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import com.example.hajiri.model.WorkSite
import android.content.Context
import com.example.hajiri.util.AppUpdateChecker
import com.example.hajiri.util.DailyReminderScheduler
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.ExpenseReportPdfData
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException

enum class HajiriTab(val label: String, val iconText: String) {
    HOME("होम", "🏠"),
    CALENDAR("कैलेंडर", "📅"),
    REPORTS("रिपोर्ट", "📊"),
    EXPENSE("खर्च", "💸"),
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
    val earnedAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val isReceived: Boolean
)

enum class SyncStatus(val badgeText: String) {
    OFFLINE_SAVED("ऑफ़लाइन मोड (डेटा फोन में सेव है)"),
    SYNCING_PENDING("क्लाउड पर सिंक हो रहा है..."),
    SYNCED("क्लाउड सिंक हो गया ✓")
}

class HajiriViewModel(
    private val repository: HajiriRepository,
    private val currentUserId: String,
    isOnlineFlow: Flow<Boolean> = flowOf(true)
) : ViewModel() {

    private val _currentTab = MutableStateFlow(HajiriTab.HOME)
    val currentTab: StateFlow<HajiriTab> = _currentTab.asStateFlow()

    private val _showHistoryScreen = MutableStateFlow(false)
    val showHistoryScreen: StateFlow<Boolean> = _showHistoryScreen.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val isOnline: StateFlow<Boolean> = isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    private val hasPendingWrites: StateFlow<Boolean> =
        repository.observeHasPendingWrites(currentUserId)
            .catch { emit(false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val syncStatus: StateFlow<SyncStatus> = combine(isOnline, hasPendingWrites) { online, pending ->
        when {
            !online -> SyncStatus.OFFLINE_SAVED
            pending -> SyncStatus.SYNCING_PENDING
            else -> SyncStatus.SYNCED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SyncStatus.SYNCED)

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

    val overtimeRecords: StateFlow<Map<String, OvertimeRecord>> =
        repository.observeOvertimeRecords(currentUserId)
            .guardStream(emptyMap(), "overtime")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val otHours: StateFlow<Map<String, Float>> = overtimeRecords
        .map { records -> records.mapValues { it.value.hours.toFloat() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val advances: StateFlow<List<AdvanceRecord>> =
        repository.observeAdvances(currentUserId)
            .guardStream(emptyList(), "advances")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val expenses: StateFlow<List<ExpenseRecord>> =
        repository.observeExpenses(currentUserId)
            .guardStream(emptyList(), "expenses")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val settings: StateFlow<UserSettings> =
        repository.observeSettings(currentUserId)
            .guardStream(UserSettings(), "settings")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserSettings())

    val currentUser: StateFlow<UserProfile?> =
        repository.observeProfile(currentUserId)
            .guardStream(null, "profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val _todayKey = MutableStateFlow(DateUtils.getTodayKey())
    val todayKeyState: StateFlow<String> = _todayKey.asStateFlow()
    val todayKey: String
        get() = _todayKey.value

    private val _todayMonthKey = MutableStateFlow(DateUtils.getTodayMonthKey())
    val todayMonthKeyState: StateFlow<String> = _todayMonthKey.asStateFlow()
    val todayMonthKey: String
        get() = _todayMonthKey.value

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _selectedReportMonth = MutableStateFlow(_todayMonthKey.value)
    val selectedReportMonth: StateFlow<String> = _selectedReportMonth.asStateFlow()

    private val _selectedHistoryMonth = MutableStateFlow(_todayMonthKey.value)
    val selectedHistoryMonth: StateFlow<String> = _selectedHistoryMonth.asStateFlow()

    private val _selectedExpenseMonth = MutableStateFlow(_todayMonthKey.value)
    val selectedExpenseMonth: StateFlow<String> = _selectedExpenseMonth.asStateFlow()

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
    var showSitesDialog = MutableStateFlow(false)

    // Daily reminder state
    private val _reminderEnabled = MutableStateFlow(false)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(20)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    // Auto-update state
    private val _autoUpdateCheckEnabled = MutableStateFlow(true)
    val autoUpdateCheckEnabled: StateFlow<Boolean> = _autoUpdateCheckEnabled.asStateFlow()

    private val _updateRepoSlug = MutableStateFlow(AppUpdateChecker.DEFAULT_GITHUB_REPO)
    val updateRepoSlug: StateFlow<String> = _updateRepoSlug.asStateFlow()

    private val _installedVersionName = MutableStateFlow("1.3")
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private var hasRunStartupUpdateCheck = false

    // Current Month Stats for Home (reacts automatically when todayMonthKey rolls over)
    val homeStats: StateFlow<MonthlyStats> = combine(
        todayMonthKeyState, attendance, overtimeRecords, advances, settings
    ) { mKey, att, otRecs, adv, sett ->
        calculateMonthlyStats(mKey, att, otRecs, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthlyStats(_todayMonthKey.value))

    // Selected Report Month Stats
    val reportStats: StateFlow<MonthlyStats> = combine(
        selectedReportMonth, attendance, overtimeRecords, advances, settings
    ) { mKey, att, otRecs, adv, sett ->
        calculateMonthlyStats(mKey, att, otRecs, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthlyStats(_todayMonthKey.value))

    // Selected Expense Month Stats & Breakdown
    private val expenseMonthStats: StateFlow<MonthlyStats> = combine(
        selectedExpenseMonth, attendance, overtimeRecords, advances, settings
    ) { mKey, att, otRecs, adv, sett ->
        calculateMonthlyStats(mKey, att, otRecs, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthlyStats(_todayMonthKey.value))

    val expenseMonthSummary: StateFlow<ExpenseMonthSummary> = combine(
        selectedExpenseMonth, expenses, expenseMonthStats
    ) { mKey, allExpenses, stats ->
        calculateExpenseSummary(mKey, allExpenses, stats)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        ExpenseMonthSummary(
            monthKey = _todayMonthKey.value,
            monthTitle = DateUtils.formatHindiMonthYear(_todayMonthKey.value)
        )
    )

    // 6-Month trends for report screen (anchors on selected older month if > 6 months back)
    val sixMonthTrends: StateFlow<List<MonthTrend>> = combine(
        selectedReportMonth, todayMonthKeyState, attendance
    ) { selMonth, tMonthKey, att ->
        val nowYm = runCatching { YearMonth.parse(tMonthKey) }.getOrNull() ?: YearMonth.now()
        val selYm = runCatching { YearMonth.parse(selMonth) }.getOrNull()
        val anchorYm = if (selYm != null && selYm.isBefore(nowYm.minusMonths(5))) selYm else nowYm
        val list = mutableListOf<MonthTrend>()
        for (i in 5 downTo 0) {
            val ym = anchorYm.minusMonths(i.toLong())
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
    val streakPair: StateFlow<Pair<Int, Int>> = combine(todayKeyState, attendance, settings) { tKey, att, _ ->
        DateUtils.calculateStreak(att, tKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Pair(0, 0))

    // Previous completed months' remaining balance & partial payment summary (past 24 months / recorded years)
    val previousMonthsSummary: StateFlow<List<PreviousMonthBalanceItem>> = combine(
        todayMonthKeyState, attendance, overtimeRecords, advances, settings
    ) { tMonthKey, att, otRecs, adv, sett ->
        val now = runCatching { YearMonth.parse(tMonthKey) }.getOrNull() ?: YearMonth.now()
        (1..24).mapNotNull { offset ->
            val ym = now.minusMonths(offset.toLong())
            val mKey = ym.toString()
            val earned = calculateSingleMonthRawNet(mKey, att, otRecs, adv, sett)
            val isLegacySettled = mKey in sett.settledMonths
            val recordedPaid = if (isLegacySettled) earned else (sett.monthPayments[mKey] ?: 0.0).coerceIn(0.0, earned)
            val remaining = maxOf(0.0, earned - recordedPaid)
            if (offset <= 6 || earned > 0.0 || recordedPaid > 0.0) {
                PreviousMonthBalanceItem(
                    monthKey = mKey,
                    monthTitle = DateUtils.formatHindiMonthYear(ym),
                    earnedAmount = earned,
                    paidAmount = recordedPaid,
                    remainingAmount = remaining,
                    isReceived = earned > 0.0 && remaining <= 0.01
                )
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Earliest year recorded in user's data so Month-Year Picker covers full history
    val earliestDataYear: StateFlow<Int> = combine(
        todayMonthKeyState, attendance, overtimeRecords, advances, expenses
    ) { tMonthKey, att, otRecs, adv, exp ->
        val currentYear = runCatching { YearMonth.parse(tMonthKey).year }.getOrNull() ?: LocalDate.now().year
        val minRecorded = (att.keys + otRecs.keys + adv.map { it.dateKey } + exp.map { it.dateKey })
            .mapNotNull { it.takeIf { s -> s.length >= 4 }?.substring(0, 4)?.toIntOrNull() }
            .minOrNull()
        minOf(currentYear - 4, minRecorded ?: (currentYear - 4)).coerceAtLeast(2020)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LocalDate.now().year - 4)

    fun refreshTodayDate(newDate: LocalDate = LocalDate.now()): Boolean {
        val newTodayKey = DateUtils.toDateKey(newDate)
        val newMonthKey = DateUtils.toMonthKey(newDate)
        val previousTodayKey = _todayKey.value
        val previousMonthKey = _todayMonthKey.value
        if (newTodayKey == previousTodayKey && newMonthKey == previousMonthKey) {
            return false
        }
        _todayKey.value = newTodayKey
        _todayMonthKey.value = newMonthKey
        if (newMonthKey != previousMonthKey) {
            val newYm = YearMonth.from(newDate)
            val prevYm = runCatching { YearMonth.parse(previousMonthKey) }.getOrNull()
            if (prevYm != null && _calendarMonth.value == prevYm) {
                _calendarMonth.value = newYm
            }
            if (_selectedReportMonth.value == previousMonthKey) {
                _selectedReportMonth.value = newMonthKey
            }
            if (_selectedHistoryMonth.value == previousMonthKey) {
                _selectedHistoryMonth.value = newMonthKey
            }
            if (_selectedExpenseMonth.value == previousMonthKey) {
                _selectedExpenseMonth.value = newMonthKey
            }
        }
        return true
    }

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
        _showHistoryScreen.value = false
        _currentTab.value = tab
    }

    fun openHistoryScreen() {
        _showHistoryScreen.value = true
    }

    fun closeHistoryScreen() {
        _showHistoryScreen.value = false
    }

    fun markToday(status: AttendanceStatus) {
        viewModelScope.launch {
            try {
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                val current = attendance.value[todayKey]?.status
                if (current == status) {
                    repository.removeAttendance(todayKey)
                    _toastEvent.emit("हाजिरी हटाई")
                } else {
                    repository.setAttendance(todayKey, status, siteId = activeSiteId)
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
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                repository.addOvertime(todayKey, hours, siteId = activeSiteId)
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

    fun saveCalendarDayAttendance(
        dateKey: String,
        status: AttendanceStatus?,
        note: String,
        otHoursValue: Float? = null
    ) {
        viewModelScope.launch {
            try {
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                when {
                    status != null -> {
                        repository.setAttendance(dateKey, status, note, siteId = activeSiteId)
                    }
                    note.isNotBlank() -> {
                        repository.saveNote(dateKey, note)
                    }
                    else -> {
                        repository.removeAttendance(dateKey)
                    }
                }
                if (otHoursValue != null) {
                    repository.setOvertime(dateKey, otHoursValue, siteId = activeSiteId)
                }
                val msg = when {
                    status != null && (otHoursValue ?: 0f) > 0f -> "${status.shortCode} + ${otHoursValue}h OT सेव ✓"
                    status != null -> "${status.shortCode} (${status.labelHindi}) दर्ज ✓"
                    (otHoursValue ?: 0f) > 0f -> "⏰ ${otHoursValue}h OT सेव ✓"
                    otHoursValue == 0f && otHours.value.containsKey(dateKey) -> "🗑️ OT हटाया गया"
                    else -> "हाजिरी अपडेट ✓"
                }
                _toastEvent.emit(msg)
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
                repository.setOvertime(dateKey, 0f)
                _selectedCalendarDateKey.value = null
                _toastEvent.emit("हाजिरी व OT हटाया गया")
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

    fun setExpenseMonth(monthKey: String) {
        _selectedExpenseMonth.value = monthKey
    }

    fun addExpense(
        amount: Double,
        category: ExpenseCategory,
        note: String,
        dateKey: String = todayKey
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                _toastEvent.emit("⚠️ सही खर्च राशि डालें")
                return@launch
            }
            val safeDateKey = dateKey.takeIf { DateUtils.parseDateKey(it) != null } ?: todayKey
            try {
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                repository.addExpense(
                    amount = amount,
                    category = category,
                    note = note,
                    dateKey = safeDateKey,
                    siteId = activeSiteId
                )
                val targetMonth = safeDateKey.take(7)
                if (targetMonth.length == 7 && targetMonth != _selectedExpenseMonth.value) {
                    _selectedExpenseMonth.value = targetMonth
                }
                _toastEvent.emit("💸 ₹${DateUtils.formatInr(amount)} खर्च सेव हो गया ✓")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ खर्च सेव नहीं हो सका")
            }
        }
    }

    fun updateExpense(
        id: String,
        amount: Double,
        category: ExpenseCategory,
        note: String,
        dateKey: String
    ) {
        if (id.isBlank()) return
        viewModelScope.launch {
            if (amount <= 0.0) {
                _toastEvent.emit("⚠️ सही खर्च राशि डालें")
                return@launch
            }
            val safeDateKey = dateKey.takeIf { DateUtils.parseDateKey(it) != null } ?: todayKey
            try {
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                repository.updateExpense(
                    id = id,
                    amount = amount,
                    category = category,
                    note = note,
                    dateKey = safeDateKey,
                    siteId = activeSiteId
                )
                val targetMonth = safeDateKey.take(7)
                if (targetMonth.length == 7 && targetMonth != _selectedExpenseMonth.value) {
                    _selectedExpenseMonth.value = targetMonth
                }
                _toastEvent.emit("✏️ खर्च अपडेट हो गया ✓")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ खर्च अपडेट नहीं हो सका")
            }
        }
    }

    fun deleteExpense(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                repository.deleteExpense(id)
                _toastEvent.emit("🗑️ खर्च हटाया गया")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ खर्च हटाया नहीं जा सका")
            }
        }
    }

    fun addAdvance(amount: Double, reason: String, dateKey: String = todayKey) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                _toastEvent.emit("⚠️ रकम डालें")
                return@launch
            }
            val safeDateKey = dateKey.takeIf { DateUtils.parseDateKey(it) != null } ?: todayKey
            try {
                val activeSiteId = settings.value.activeSiteId.ifBlank { "default" }
                repository.addAdvance(amount, reason, safeDateKey, siteId = activeSiteId)
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
                val currentSites = resolveSitesList(cur)
                val updatedSites = currentSites.map { site ->
                    if (site.id == (cur.activeSiteId.ifBlank { "default" })) {
                        site.copy(
                            wageMode = wageMode.id,
                            salary = salary,
                            dailyWage = dailyWage,
                            otRate = otRate
                        )
                    } else site
                }
                if (cur.sites.isNotEmpty()) {
                    repository.saveSitesAndActiveSite(
                        sites = updatedSites,
                        activeSiteId = cur.activeSiteId.ifBlank { "default" }
                    )
                } else {
                    repository.saveSettings(
                        salary = salary,
                        dailyWage = dailyWage,
                        wageMode = wageMode.id,
                        otRate = otRate,
                        workDays = cur.workDays,
                        themeStyle = cur.themeStyle
                    )
                }
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
                if (cur.sites.isNotEmpty()) {
                    val activeId = cur.activeSiteId.ifBlank { "default" }
                    val updatedSites = cur.sites.map { site ->
                        if (site.id == activeId) site.copy(workDays = workDays) else site
                    }
                    repository.saveSitesAndActiveSite(updatedSites, activeId)
                } else {
                    repository.saveSettings(
                        salary = cur.salary,
                        dailyWage = cur.dailyWage,
                        wageMode = cur.wageMode,
                        otRate = cur.otRate,
                        workDays = workDays,
                        themeStyle = cur.themeStyle
                    )
                }
                _toastEvent.emit("✅ कार्यदिवस सेव!")
                showWorkDaysDialog.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ कार्यदिवस सेव नहीं हो सका")
            }
        }
    }

    fun resolveSitesList(sett: UserSettings = settings.value): List<WorkSite> {
        return if (sett.sites.isNotEmpty()) {
            sett.sites
        } else {
            listOf(
                WorkSite(
                    id = "default",
                    name = "मुख्य काम",
                    wageMode = sett.wageMode,
                    salary = sett.salary,
                    dailyWage = sett.dailyWage,
                    otRate = sett.otRate,
                    workDays = sett.workDays
                )
            )
        }
    }

    fun getActiveSite(sett: UserSettings = settings.value): WorkSite {
        val sitesList = resolveSitesList(sett)
        val activeId = sett.activeSiteId.ifBlank { "default" }
        return sitesList.firstOrNull { it.id == activeId } ?: sitesList.first()
    }

    fun switchActiveSite(siteId: String) {
        viewModelScope.launch {
            try {
                val curList = resolveSitesList(settings.value)
                val target = curList.firstOrNull { it.id == siteId } ?: return@launch
                repository.saveSitesAndActiveSite(curList, target.id)
                _toastEvent.emit("🏗️ सक्रिय साइट: ${target.name}")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ साइट बदलने में त्रुटि")
            }
        }
    }

    fun addOrUpdateWorkSite(site: WorkSite) {
        viewModelScope.launch {
            try {
                val curList = resolveSitesList(settings.value).toMutableList()
                val idx = curList.indexOfFirst { it.id == site.id }
                if (idx >= 0) {
                    curList[idx] = site
                } else {
                    curList.add(site)
                }
                repository.saveSitesAndActiveSite(curList, site.id)
                _toastEvent.emit("🏗️ साइट सेव: ${site.name}")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ साइट सेव नहीं हो सकी")
            }
        }
    }

    fun deleteWorkSite(siteId: String) {
        viewModelScope.launch {
            val curList = resolveSitesList(settings.value).toMutableList()
            if (curList.size <= 1) {
                _toastEvent.emit("⚠️ कम से कम 1 साइट रहना जरूरी है")
                return@launch
            }
            try {
                curList.removeAll { it.id == siteId }
                val nextActiveId = if (settings.value.activeSiteId == siteId) {
                    curList.first().id
                } else settings.value.activeSiteId
                repository.saveSitesAndActiveSite(curList, nextActiveId)
                _toastEvent.emit("🗑️ साइट हटाई गई")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ साइट हटाई नहीं जा सकी")
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
            overtimeRecords.value,
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
            if (stats.carryForwardAdded > 0.0 || stats.previousMonthRawBalance > 0.0) {
                val prevStatus = if (stats.carryForwardAdded <= 0.0) {
                    "ले लिया गया (₹0 जोड़ा)"
                } else {
                    "+₹${DateUtils.formatInr(stats.carryForwardAdded)}"
                }
                append("💼 पिछला बाकी (${stats.previousMonthsPendingCount} माह): $prevStatus\n")
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
                val earned = calculateSingleMonthRawNet(
                    monthKey = monthKey,
                    att = attendance.value,
                    ot = overtimeRecords.value,
                    adv = advances.value,
                    sett = settings.value
                )
                repository.setMonthPayment(
                    monthKey = monthKey,
                    paidAmount = if (isSettled) earned else 0.0,
                    isSettled = isSettled
                )
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

    fun recordMonthPartialPayment(monthKey: String, paidAmount: Double) {
        if (monthKey.isBlank()) return
        viewModelScope.launch {
            try {
                val earned = calculateSingleMonthRawNet(
                    monthKey = monthKey,
                    att = attendance.value,
                    ot = overtimeRecords.value,
                    adv = advances.value,
                    sett = settings.value
                )
                val cleanPaid = paidAmount.coerceIn(0.0, maxOf(earned, paidAmount))
                val isFullySettled = earned > 0.0 && cleanPaid >= earned
                repository.setMonthPayment(
                    monthKey = monthKey,
                    paidAmount = cleanPaid,
                    isSettled = isFullySettled
                )
                val title = DateUtils.formatHindiMonthYear(monthKey)
                val remaining = maxOf(0.0, earned - cleanPaid)
                _toastEvent.emit(
                    when {
                        isFullySettled -> "✅ $title पूरा भुगतान (₹${DateUtils.formatInr(cleanPaid)}) दर्ज"
                        cleanPaid <= 0.0 -> "💼 $title पूरा बाकी सैलरी में जोड़ा गया"
                        else -> "💵 $title: ₹${DateUtils.formatInr(cleanPaid)} मिला • ₹${DateUtils.formatInr(remaining)} बाकी"
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _toastEvent.emit("⚠️ आंशिक भुगतान सेव नहीं हो सका")
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    fun initReminderState(context: Context) {
        val appContext = context.applicationContext
        _reminderEnabled.value = DailyReminderScheduler.isEnabled(appContext)
        _reminderHour.value = DailyReminderScheduler.getReminderHour(appContext)
    }

    fun setDailyReminder(context: Context, enabled: Boolean, hourOfDay: Int = _reminderHour.value) {
        val appContext = context.applicationContext
        DailyReminderScheduler.setReminder(appContext, enabled, hourOfDay)
        _reminderEnabled.value = enabled
        _reminderHour.value = if (hourOfDay == 21) 21 else 20
        val timeLabel = if (_reminderHour.value == 21) "रात 9:00 बजे" else "रात 8:00 बजे"
        showMessage(
            if (enabled) "🔔 दैनिक रिमाइंडर चालू ($timeLabel)"
            else "🔕 दैनिक रिमाइंडर बंद किया गया"
        )
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
        val currentOtRecs = overtimeRecords.value
        val currentOt = otHours.value
        val currentAdv = advances.value
        val currentSett = settings.value
        val profile = currentUser.value

        val stats = calculateMonthlyStats(
            monthKey = monthKey,
            att = currentAtt,
            ot = currentOtRecs,
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

        val resolvedSite = siteOrCompany.trim().ifBlank {
            if (currentSett.sites.isNotEmpty()) getActiveSite(currentSett).name else ""
        }

        return MonthlySlipData(
            monthKey = monthKey,
            monthTitle = DateUtils.formatHindiMonthYear(monthKey),
            employeeName = profile?.name?.ifBlank { "हाजिरी यूजर" } ?: "हाजिरी यूजर",
            employeePhone = profile?.phone ?: "",
            employeeEmail = profile?.email ?: "",
            siteOrCompany = resolvedSite,
            extraWorkBonus = maxOf(0.0, extraWorkBonus),
            extraWorkNote = extraWorkNote.trim(),
            settings = currentSett,
            stats = stats,
            advances = monthAdvances,
            dailyRows = dailyRows
        )
    }

    fun buildExpenseReportData(monthKey: String): ExpenseReportPdfData {
        val profile = currentUser.value
        val stats = calculateMonthlyStats(
            monthKey = monthKey,
            att = attendance.value,
            ot = overtimeRecords.value,
            adv = advances.value,
            sett = settings.value
        )
        val summary = calculateExpenseSummary(
            monthKey = monthKey,
            allExpenses = expenses.value,
            monthStats = stats
        )
        return ExpenseReportPdfData(
            userName = profile?.name?.ifBlank { "हाजिरी यूजर" } ?: "हाजिरी यूजर",
            userPhone = profile?.phone ?: "",
            userEmail = profile?.email ?: "",
            summary = summary
        )
    }

    companion object {
        private const val TAG = "HajiriVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun calculateExpenseSummary(
            monthKey: String,
            allExpenses: List<ExpenseRecord>,
            monthStats: MonthlyStats
        ): ExpenseMonthSummary {
            val monthEntries = allExpenses
                .filter { it.dateKey.startsWith(monthKey) }
                .sortedWith(
                    compareByDescending<ExpenseRecord> { it.dateKey }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )
            val totalExpense = monthEntries.sumOf { maxOf(0.0, it.amount) }
            val monthlyIncome = maxOf(0.0, monthStats.baseEarnings + monthStats.otEarnings)
            val netSavings = monthlyIncome - totalExpense

            val grouped = monthEntries.groupBy { ExpenseCategory.fromId(it.category) }
            val breakdowns = ExpenseCategory.entries.map { cat ->
                val list = grouped[cat].orEmpty()
                val catTotal = list.sumOf { maxOf(0.0, it.amount) }
                val pct = if (totalExpense > 0.0) (catTotal / totalExpense).toFloat().coerceIn(0f, 1f) else 0f
                CategoryExpenseBreakdown(
                    category = cat,
                    totalAmount = catTotal,
                    count = list.size,
                    percentage = pct
                )
            }.sortedByDescending { it.totalAmount }

            return ExpenseMonthSummary(
                monthKey = monthKey,
                monthTitle = DateUtils.formatHindiMonthYear(monthKey),
                totalExpense = totalExpense,
                monthlyIncome = monthlyIncome,
                netSavings = netSavings,
                entries = monthEntries,
                categoryBreakdowns = breakdowns
            )
        }

        private fun effectiveWorkDaysFor(monthKey: String, siteId: String, sett: UserSettings): Int {
            val daysInMonth = runCatching { YearMonth.parse(monthKey).lengthOfMonth() }.getOrDefault(30)
            val matched = if (siteId.isNotBlank()) sett.sites.firstOrNull { it.id == siteId } else null
            val configuredWorkDays = matched?.workDays ?: sett.workDays
            return if (configuredWorkDays in 1..31 && configuredWorkDays != 26) {
                configuredWorkDays
            } else {
                daysInMonth
            }
        }

        private fun perDayForSite(monthKey: String, siteId: String, sett: UserSettings): Double {
            val matched = if (siteId.isNotBlank()) sett.sites.firstOrNull { it.id == siteId } else null
            val workDays = effectiveWorkDaysFor(monthKey, siteId, sett)
            val mode = WageMode.fromId(matched?.wageMode ?: sett.wageMode)
            val daily = matched?.dailyWage ?: sett.dailyWage
            val monthly = matched?.salary ?: sett.salary
            return when (mode) {
                WageMode.DAILY -> daily
                WageMode.MONTHLY -> if (monthly > 0) monthly / workDays else 0.0
            }
        }

        private fun otRateForSite(siteId: String, sett: UserSettings): Double {
            val matched = if (siteId.isNotBlank()) sett.sites.firstOrNull { it.id == siteId } else null
            return matched?.otRate ?: sett.otRate
        }

        fun calculateSingleMonthRawNet(
            monthKey: String,
            att: Map<String, AttendanceRecord>,
            ot: Map<String, OvertimeRecord>,
            adv: List<AdvanceRecord>,
            sett: UserSettings
        ): Double {
            var baseEarnings = 0.0
            var otEarnings = 0.0
            var totalAdv = 0.0

            att.forEach { (k, v) ->
                if (k.startsWith(monthKey)) {
                    val dayRate = perDayForSite(monthKey, v.siteId, sett)
                    when (v.status) {
                        AttendanceStatus.PRESENT -> baseEarnings += dayRate
                        AttendanceStatus.HALF -> baseEarnings += (dayRate / 2.0)
                        else -> Unit
                    }
                }
            }
            ot.forEach { (k, rec) ->
                if (k.startsWith(monthKey)) {
                    otEarnings += rec.hours * otRateForSite(rec.siteId, sett)
                }
            }
            adv.forEach {
                if (it.dateKey.startsWith(monthKey)) totalAdv += it.amount
            }

            return maxOf(0.0, baseEarnings + otEarnings - totalAdv)
        }

        fun calculateMonthlyStats(
            monthKey: String,
            att: Map<String, AttendanceRecord>,
            ot: Map<String, OvertimeRecord>,
            adv: List<AdvanceRecord>,
            sett: UserSettings
        ): MonthlyStats {
            var p = 0
            var a = 0
            var h = 0
            var l = 0
            var baseEarnings = 0.0
            var totalOt = 0f
            var otEarnings = 0.0
            var totalAdv = 0.0

            att.forEach { (k, v) ->
                if (k.startsWith(monthKey)) {
                    val dayRate = perDayForSite(monthKey, v.siteId, sett)
                    when (v.status) {
                        AttendanceStatus.PRESENT -> {
                            p++
                            baseEarnings += dayRate
                        }
                        AttendanceStatus.ABSENT -> a++
                        AttendanceStatus.HALF -> {
                            h++
                            baseEarnings += (dayRate / 2.0)
                        }
                        AttendanceStatus.LEAVE -> l++
                    }
                }
            }

            ot.forEach { (k, rec) ->
                if (k.startsWith(monthKey)) {
                    totalOt += rec.hours.toFloat()
                    otEarnings += rec.hours * otRateForSite(rec.siteId, sett)
                }
            }

            adv.forEach {
                if (it.dateKey.startsWith(monthKey)) {
                    totalAdv += it.amount
                }
            }

            val perDayRate = perDayForSite(monthKey, sett.activeSiteId, sett)
            val effectiveWorkDays = effectiveWorkDaysFor(monthKey, sett.activeSiteId, sett)

            // Cumulative unpaid balance chain across all prior months (< monthKey)
            val targetYm = runCatching { YearMonth.parse(monthKey) }.getOrNull()
            val candidateMonths = mutableSetOf<String>()
            if (targetYm != null) {
                for (offset in 1..24) {
                    candidateMonths.add(targetYm.minusMonths(offset.toLong()).toString())
                }
            }
            att.keys.mapNotNull { it.takeIf { k -> k.length >= 7 }?.substring(0, 7) }
                .filter { it < monthKey }
                .forEach { candidateMonths.add(it) }
            ot.keys.mapNotNull { it.takeIf { k -> k.length >= 7 }?.substring(0, 7) }
                .filter { it < monthKey }
                .forEach { candidateMonths.add(it) }
            adv.mapNotNull { it.dateKey.takeIf { k -> k.length >= 7 }?.substring(0, 7) }
                .filter { it < monthKey }
                .forEach { candidateMonths.add(it) }

            var cumulativeCarryForward = 0.0
            var pendingMonthsCount = 0
            candidateMonths.forEach { priorMonth ->
                val earned = calculateSingleMonthRawNet(priorMonth, att, ot, adv, sett)
                val paid = if (priorMonth in sett.settledMonths) {
                    earned
                } else {
                    (sett.monthPayments[priorMonth] ?: 0.0).coerceIn(0.0, earned)
                }
                val rem = maxOf(0.0, earned - paid)
                if (rem > 0.01) {
                    cumulativeCarryForward += rem
                    pendingMonthsCount++
                }
            }

            val prevYearMonth = targetYm?.minusMonths(1)
            val prevMonthKey = prevYearMonth?.toString() ?: ""
            val prevMonthTitle = if (prevYearMonth != null) DateUtils.formatHindiMonthYear(prevYearMonth) else ""
            val prevEarnedBalance = if (prevMonthKey.isNotBlank()) {
                calculateSingleMonthRawNet(prevMonthKey, att, ot, adv, sett)
            } else 0.0
            val prevPaidAmount = when {
                prevMonthKey.isBlank() -> 0.0
                prevMonthKey in sett.settledMonths -> prevEarnedBalance
                else -> (sett.monthPayments[prevMonthKey] ?: 0.0).coerceIn(0.0, prevEarnedBalance)
            }
            val prevRemaining = maxOf(0.0, prevEarnedBalance - prevPaidAmount)
            val isPrevSettled = prevMonthKey.isNotBlank() && prevEarnedBalance > 0.0 && prevRemaining <= 0.01

            val netTakeHome = maxOf(0.0, baseEarnings + otEarnings - totalAdv + cumulativeCarryForward)
            val workedDays = p + (h * 0.5f)

            // महीने के कुल दिनों (28/29/30/31 या कस्टम कार्यदिवस) के आधार पर सटीक उपस्थिति प्रतिशत
            val pct = if (effectiveWorkDays > 0) {
                ((workedDays / effectiveWorkDays) * 100).toInt().coerceIn(0, 100)
            } else 0

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
                previousMonthRawBalance = prevRemaining,
                previousMonthPaidAmount = prevPaidAmount,
                previousMonthsPendingCount = pendingMonthsCount,
                isPreviousMonthReceived = if (prevEarnedBalance > 0.0) isPrevSettled else cumulativeCarryForward <= 0.01,
                carryForwardAdded = cumulativeCarryForward,
                netTakeHome = netTakeHome,
                attendancePercentage = pct,
                workedDays = workedDays
            )
        }
    }
}
