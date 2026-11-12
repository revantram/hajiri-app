package com.example.hajiri.data

import android.content.Context
import android.content.SharedPreferences
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.UserProfile
import com.example.hajiri.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HajiriRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hajiri_storage", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val _attendance = MutableStateFlow<Map<String, AttendanceRecord>>(emptyMap())
    val attendance: StateFlow<Map<String, AttendanceRecord>> = _attendance.asStateFlow()

    private val _otHours = MutableStateFlow<Map<String, Float>>(emptyMap())
    val otHours: StateFlow<Map<String, Float>> = _otHours.asStateFlow()

    private val _advances = MutableStateFlow<List<AdvanceRecord>>(emptyList())
    val advances: StateFlow<List<AdvanceRecord>> = _advances.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val attJson = prefs.getString("h_att", null)
            if (!attJson.isNullOrEmpty()) {
                val attMap = json.decodeFromString<Map<String, AttendanceRecord>>(attJson)
                _attendance.value = attMap
            }
        } catch (_: Exception) {}

        try {
            val otJson = prefs.getString("h_ot", null)
            if (!otJson.isNullOrEmpty()) {
                val otMap = json.decodeFromString<Map<String, Float>>(otJson)
                _otHours.value = otMap
            }
        } catch (_: Exception) {}

        try {
            val advJson = prefs.getString("h_adv", null)
            if (!advJson.isNullOrEmpty()) {
                val advList = json.decodeFromString<List<AdvanceRecord>>(advJson)
                _advances.value = advList
            }
        } catch (_: Exception) {}

        try {
            val setJson = prefs.getString("h_set", null)
            if (!setJson.isNullOrEmpty()) {
                val set = json.decodeFromString<UserSettings>(setJson)
                _settings.value = set
            }
        } catch (_: Exception) {}

        try {
            val userJson = prefs.getString("h_user", null)
            if (!userJson.isNullOrEmpty()) {
                val user = json.decodeFromString<UserProfile>(userJson)
                _currentUser.value = user
            }
        } catch (_: Exception) {}
    }

    private fun saveAttendanceInternal(map: Map<String, AttendanceRecord>) {
        _attendance.value = map
        prefs.edit().putString("h_att", json.encodeToString(map)).apply()
    }

    private fun saveOtInternal(map: Map<String, Float>) {
        _otHours.value = map
        prefs.edit().putString("h_ot", json.encodeToString(map)).apply()
    }

    private fun saveAdvancesInternal(list: List<AdvanceRecord>) {
        _advances.value = list
        prefs.edit().putString("h_adv", json.encodeToString(list)).apply()
    }

    fun setAttendance(dateKey: String, status: AttendanceStatus, note: String? = null) {
        val current = _attendance.value.toMutableMap()
        val existingNote = note ?: current[dateKey]?.note ?: ""
        current[dateKey] = AttendanceRecord(status = status, note = existingNote)
        saveAttendanceInternal(current)
    }

    fun removeAttendance(dateKey: String) {
        val current = _attendance.value.toMutableMap()
        current.remove(dateKey)
        saveAttendanceInternal(current)
    }

    fun toggleOrCycleAttendance(dateKey: String): AttendanceStatus? {
        val current = _attendance.value.toMutableMap()
        val curRecord = current[dateKey]
        val existingNote = curRecord?.note ?: ""

        val statuses = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.ABSENT,
            AttendanceStatus.HALF,
            AttendanceStatus.LEAVE
        )

        return if (curRecord == null) {
            current[dateKey] = AttendanceRecord(status = AttendanceStatus.PRESENT, note = existingNote)
            saveAttendanceInternal(current)
            AttendanceStatus.PRESENT
        } else {
            val curIdx = statuses.indexOf(curRecord.status)
            if (curIdx == statuses.lastIndex) {
                current.remove(dateKey)
                saveAttendanceInternal(current)
                null
            } else {
                val next = statuses[curIdx + 1]
                current[dateKey] = AttendanceRecord(status = next, note = existingNote)
                saveAttendanceInternal(current)
                next
            }
        }
    }

    fun saveNote(dateKey: String, note: String) {
        val current = _attendance.value.toMutableMap()
        val existing = current[dateKey] ?: AttendanceRecord(status = AttendanceStatus.PRESENT)
        current[dateKey] = existing.copy(note = note.trim())
        saveAttendanceInternal(current)
    }

    fun addOvertime(dateKey: String, hours: Float) {
        val current = _otHours.value.toMutableMap()
        val prev = current[dateKey] ?: 0f
        current[dateKey] = prev + hours
        saveOtInternal(current)
    }

    fun setOvertime(dateKey: String, hours: Float) {
        val current = _otHours.value.toMutableMap()
        if (hours <= 0f) {
            current.remove(dateKey)
        } else {
            current[dateKey] = hours
        }
        saveOtInternal(current)
    }

    fun addAdvance(amount: Double, reason: String, dateKey: String) {
        val current = _advances.value.toMutableList()
        val record = AdvanceRecord(
            id = "adv_" + System.currentTimeMillis(),
            amount = amount,
            reason = reason.trim(),
            dateKey = dateKey
        )
        current.add(0, record)
        saveAdvancesInternal(current)
    }

    fun deleteAdvance(id: String) {
        val current = _advances.value.filterNot { it.id == id }
        saveAdvancesInternal(current)
    }

    fun saveSettings(salary: Double, otRate: Double, workDays: Int) {
        val newSettings = UserSettings(salary = salary, otRate = otRate, workDays = workDays)
        _settings.value = newSettings
        prefs.edit().putString("h_set", json.encodeToString(newSettings)).apply()
    }

    fun saveProfile(profile: UserProfile?) {
        _currentUser.value = profile
        if (profile != null) {
            prefs.edit().putString("h_user", json.encodeToString(profile)).apply()
        } else {
            prefs.edit().remove("h_user").apply()
        }
    }

    fun clearMonth(monthKey: String) {
        val att = _attendance.value.filterNot { it.key.startsWith(monthKey) }
        val ot = _otHours.value.filterNot { it.key.startsWith(monthKey) }
        val adv = _advances.value.filterNot { it.dateKey.startsWith(monthKey) }
        saveAttendanceInternal(att)
        saveOtInternal(ot)
        saveAdvancesInternal(adv)
    }

    fun clearAllData() {
        _attendance.value = emptyMap()
        _otHours.value = emptyMap()
        _advances.value = emptyList()
        prefs.edit()
            .remove("h_att")
            .remove("h_ot")
            .remove("h_adv")
            .apply()
    }
}
