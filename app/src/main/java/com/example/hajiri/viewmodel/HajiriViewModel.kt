package com.example.hajiri.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.UserProfile
import com.example.hajiri.model.UserSettings
import com.example.hajiri.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

class HajiriViewModel(application: Application) : AndroidViewModel(application) {
    val repository = HajiriRepository(application)

    private val _currentTab = MutableStateFlow(HajiriTab.HOME)
    val currentTab: StateFlow<HajiriTab> = _currentTab.asStateFlow()

    private val _isAuthScreenVisible = MutableStateFlow(false)
    val isAuthScreenVisible: StateFlow<Boolean> = _isAuthScreenVisible.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val attendance: StateFlow<Map<String, AttendanceRecord>> = repository.attendance
    val otHours: StateFlow<Map<String, Float>> = repository.otHours
    val advances: StateFlow<List<AdvanceRecord>> = repository.advances
    val settings: StateFlow<UserSettings> = repository.settings
    val currentUser: StateFlow<UserProfile?> = repository.currentUser

    val todayKey = DateUtils.getTodayKey()
    val todayMonthKey = DateUtils.getTodayMonthKey()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _selectedReportMonth = MutableStateFlow(todayMonthKey)
    val selectedReportMonth: StateFlow<String> = _selectedReportMonth.asStateFlow()

    private val _selectedHistoryMonth = MutableStateFlow(todayMonthKey)
    val selectedHistoryMonth: StateFlow<String> = _selectedHistoryMonth.asStateFlow()

    // Dialog state holders
    var showSalaryDialog = MutableStateFlow(false)
    var showWorkDaysDialog = MutableStateFlow(false)
    var showAdvanceDialog = MutableStateFlow(false)
    var showClearMonthDialog = MutableStateFlow(false)
    var showClearAllDialog = MutableStateFlow(false)

    // Current Month Stats for Home
    val homeStats: StateFlow<MonthlyStats> = combine(
        attendance, otHours, advances, settings
    ) { att, ot, adv, sett ->
        calculateMonthlyStats(todayMonthKey, att, ot, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthlyStats(todayMonthKey))

    // Selected Report Month Stats
    val reportStats: StateFlow<MonthlyStats> = combine(
        selectedReportMonth, attendance, otHours, advances, settings
    ) { mKey, att, ot, adv, sett ->
        calculateMonthlyStats(mKey, att, ot, adv, sett)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthlyStats(todayMonthKey))

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Streak stats
    val streakPair: StateFlow<Pair<Int, Int>> = attendance.combine(settings) { att, _ ->
        DateUtils.calculateStreak(att, todayKey)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0, 0))

    fun setTab(tab: HajiriTab) {
        _currentTab.value = tab
    }

    fun showAuth(show: Boolean) {
        _isAuthScreenVisible.value = show
    }

    fun markToday(status: AttendanceStatus) {
        viewModelScope.launch {
            val current = attendance.value[todayKey]?.status
            if (current == status) {
                repository.removeAttendance(todayKey)
                _toastEvent.emit("हाजिरी हटाई")
            } else {
                repository.setAttendance(todayKey, status)
                _toastEvent.emit("${status.labelHindi} दर्ज ✓")
            }
        }
    }

    fun saveTodayNote(note: String) {
        viewModelScope.launch {
            repository.saveNote(todayKey, note)
            _toastEvent.emit("📝 नोट सेव!")
        }
    }

    fun addTodayOvertime(hours: Float) {
        viewModelScope.launch {
            if (hours <= 0f) {
                _toastEvent.emit("⚠️ सही घंटे डालें")
                return@launch
            }
            repository.addOvertime(todayKey, hours)
            _toastEvent.emit("⏰ ${hours}h OT जोड़ा")
        }
    }

    fun toggleCalendarDay(dateKey: String) {
        viewModelScope.launch {
            val date = DateUtils.parseDateKey(dateKey)
            if (date != null && date.isAfter(LocalDate.now())) {
                _toastEvent.emit("⏳ भविष्य की तारीख नहीं")
                return@launch
            }
            val newStatus = repository.toggleOrCycleAttendance(dateKey)
            if (newStatus != null) {
                _toastEvent.emit("${newStatus.labelHindi} दर्ज ✓")
            } else {
                _toastEvent.emit("हाजिरी हटाई")
            }
        }
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
            repository.addAdvance(amount, reason, todayKey)
            _toastEvent.emit("💳 ₹${DateUtils.formatInr(amount)} एडवांस दर्ज")
            showAdvanceDialog.value = false
        }
    }

    fun deleteAdvance(id: String) {
        viewModelScope.launch {
            repository.deleteAdvance(id)
            _toastEvent.emit("🗑️ एडवांस मिटाया")
        }
    }

    fun updateSalarySettings(salary: Double, otRate: Double) {
        viewModelScope.launch {
            val cur = settings.value
            repository.saveSettings(salary, otRate, cur.workDays)
            _toastEvent.emit("💰 तनख्वाह सेटिंग सेव!")
            showSalaryDialog.value = false
        }
    }

    fun updateWorkDays(workDays: Int) {
        viewModelScope.launch {
            val cur = settings.value
            repository.saveSettings(cur.salary, cur.otRate, workDays)
            _toastEvent.emit("✅ कार्यदिवस सेव!")
            showWorkDaysDialog.value = false
        }
    }

    fun login(phone: String, pin: String) {
        viewModelScope.launch {
            if (phone.length < 10 || pin.length < 4) {
                _toastEvent.emit("⚠️ सही नंबर/PIN डालें")
                return@launch
            }
            val profile = UserProfile(name = "यूजर ($phone)", phone = phone, pin = pin)
            repository.saveProfile(profile)
            _isAuthScreenVisible.value = false
            _toastEvent.emit("🎉 लॉगिन सफल: ${profile.name}")
        }
    }

    fun register(name: String, phone: String, pin: String) {
        viewModelScope.launch {
            if (name.isBlank() || phone.length < 10 || pin.length < 4) {
                _toastEvent.emit("⚠️ सभी फील्ड भरें")
                return@launch
            }
            val profile = UserProfile(name = name, phone = phone, pin = pin)
            repository.saveProfile(profile)
            _isAuthScreenVisible.value = false
            _toastEvent.emit("🎉 स्वागत है, $name!")
        }
    }

    fun continueOffline() {
        _isAuthScreenVisible.value = false
        viewModelScope.launch {
            _toastEvent.emit("🔌 ऑफलाइन मोड चालू")
        }
    }

    fun logout() {
        repository.saveProfile(null)
        _isAuthScreenVisible.value = true
        viewModelScope.launch {
            _toastEvent.emit("लॉगआउट किया गया")
        }
    }

    fun clearMonthData() {
        repository.clearMonth(todayMonthKey)
        showClearMonthDialog.value = false
        viewModelScope.launch {
            _toastEvent.emit("🗑️ इस महीने का डेटा मिटाया")
        }
    }

    fun clearAllData() {
        repository.clearAllData()
        showClearAllDialog.value = false
        viewModelScope.launch {
            _toastEvent.emit("⛔ सारा डेटा मिटाया")
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
            append("━━━━━━━━━━━━━━━━━\n")
            append("📊 उपस्थिति: ${stats.attendancePercentage}%\n")
            append("💰 कुल कमाई: ₹${DateUtils.formatInr(stats.baseEarnings + stats.otEarnings)}\n")
            append("🤑 हाथ में: ₹${DateUtils.formatInr(stats.netTakeHome)}\n")
            append("━━━━━━━━━━━━━━━━━\n")
            append("📱 हाजिरी ऐप")
        }
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
        val perDayRate = if (sett.salary > 0) sett.salary / workDays else 0.0
        val baseEarnings = (p * perDayRate) + (h * (perDayRate / 2.0))
        val otEarnings = totalOt * sett.otRate
        val netTakeHome = maxOf(0.0, baseEarnings + otEarnings - totalAdv)
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
            netTakeHome = netTakeHome,
            attendancePercentage = pct,
            workedDays = workedDays
        )
    }
}
