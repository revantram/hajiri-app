package com.example.hajiri.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceStatus(val id: String, val labelHindi: String, val dotColorHex: Long) {
    PRESENT("present", "हाजिर", 0xFF22C55E),
    ABSENT("absent", "गैरहाजिर", 0xFFEF4444),
    HALF("half", "आधा दिन", 0xFFF59E0B),
    LEAVE("leave", "छुट्टी", 0xFF3B82F6);

    companion object {
        fun fromId(id: String?): AttendanceStatus? {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

@Serializable
data class AttendanceRecord(
    val status: AttendanceStatus,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class AdvanceRecord(
    val id: String,
    val amount: Double,
    val reason: String,
    val dateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserSettings(
    val salary: Double = 0.0,
    val otRate: Double = 50.0,
    val workDays: Int = 26
)

@Serializable
data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val pin: String = ""
)

data class MonthlyStats(
    val yearMonth: String,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val totalOtHours: Float = 0f,
    val totalAdvance: Double = 0.0,
    val baseEarnings: Double = 0.0,
    val otEarnings: Double = 0.0,
    val perDayRate: Double = 0.0,
    val netTakeHome: Double = 0.0,
    val attendancePercentage: Int = 0,
    val workedDays: Float = 0f
)
