package com.example.hajiri.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AttendanceStatus(val id: String, val labelHindi: String, val shortCode: String, val dotColorHex: Long) {
    PRESENT("present", "हाजिर", "P", 0xFF22C55E),
    ABSENT("absent", "गैरहाजिर", "A", 0xFFEF4444),
    HALF("half", "आधा दिन", "HD", 0xFFF59E0B),
    LEAVE("leave", "छुट्टी", "L", 0xFF3B82F6);

    companion object {
        fun fromId(id: String?): AttendanceStatus? {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

enum class WageMode(val id: String, val labelHindi: String) {
    MONTHLY("monthly", "मासिक वेतन"),
    DAILY("daily", "दिन की हाजिरी");

    companion object {
        fun fromId(id: String?): WageMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class AppThemeStyle(val id: String, val labelHindi: String, val emoji: String) {
    LIQUID_LIGHT("liquid_light", "लिक्विड लाइट", "🤍"),
    OCEAN_GLASS("ocean_glass", "ओशन ब्लू", "🌊"),
    ROSE_GOLD("rose_gold", "रोज़ गोल्ड", "🌸"),
    LIQUID_DARK("liquid_dark", "लिक्विड डार्क", "🌙");

    companion object {
        fun fromId(id: String?): AppThemeStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LIQUID_LIGHT
        }
    }
}

@IgnoreExtraProperties
data class WorkSite(
    val id: String = "default",
    val name: String = "मुख्य काम",
    val wageMode: String = WageMode.MONTHLY.id,
    val salary: Double = 0.0,
    val dailyWage: Double = 0.0,
    val otRate: Double = 50.0,
    val workDays: Int = 26
)

@IgnoreExtraProperties
data class AttendanceFirestoreDto(
    @DocumentId val id: String = "",
    val status: String = AttendanceStatus.PRESENT.id,
    val note: String = "",
    val siteId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AttendanceRecord(
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val note: String = "",
    val siteId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OvertimeRecord(
    @DocumentId val dateKey: String = "",
    val hours: Double = 0.0,
    val siteId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdvanceRecord(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val reason: String = "",
    val dateKey: String = "",
    val siteId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val salary: Double = 0.0,
    val dailyWage: Double = 0.0,
    val wageMode: String = WageMode.MONTHLY.id,
    val otRate: Double = 50.0,
    val workDays: Int = 26,
    val themeStyle: String = AppThemeStyle.LIQUID_LIGHT.id,
    val settledMonths: List<String> = emptyList(),
    val monthPayments: Map<String, Double> = emptyMap(),
    val activeSiteId: String = "default",
    val sites: List<WorkSite> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
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
    val previousMonthKey: String = "",
    val previousMonthTitle: String = "",
    val previousMonthRawBalance: Double = 0.0,
    val previousMonthPaidAmount: Double = 0.0,
    val previousMonthsPendingCount: Int = 0,
    val isPreviousMonthReceived: Boolean = false,
    val carryForwardAdded: Double = 0.0,
    val netTakeHome: Double = 0.0,
    val attendancePercentage: Int = 0,
    val workedDays: Float = 0f
)

enum class ExpenseCategory(
    val id: String,
    val labelHindi: String,
    val emoji: String,
    val colorHex: Long
) {
    FOOD("food", "खाना / चाय", "🍔", 0xFFF59E0B),
    TRANSPORT("transport", "पेट्रोल / किराया", "⛽", 0xFF0EA5E9),
    TOOLS("tools", "सामान / औजार", "🛠️", 0xFF8B5CF6),
    GROCERY("grocery", "राशन / कमरा", "🏠", 0xFF10B981),
    RECHARGE("recharge", "रिचार्ज / अन्य", "📱", 0xFFEC4899);

    companion object {
        fun fromId(id: String?): ExpenseCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: RECHARGE
        }
    }
}

@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.RECHARGE.id,
    val note: String = "",
    val dateKey: String = "",
    val siteId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CategoryExpenseBreakdown(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class ExpenseMonthSummary(
    val monthKey: String,
    val monthTitle: String,
    val totalExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val netSavings: Double = 0.0,
    val entries: List<ExpenseRecord> = emptyList(),
    val categoryBreakdowns: List<CategoryExpenseBreakdown> = emptyList()
)

