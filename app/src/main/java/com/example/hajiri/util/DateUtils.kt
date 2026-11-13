package com.example.hajiri.util

import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    val HINDI_MONTHS = listOf(
        "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    val HINDI_DAYS = listOf(
        "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार", "रविवार"
    )

    val HINDI_DAYS_SHORT = listOf(
        "रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि"
    )

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
    private val INR_FORMAT = DecimalFormat("#,##,###")

    fun getTodayKey(): String = LocalDate.now().format(DATE_FORMATTER)
    fun getTodayMonthKey(): String = LocalDate.now().format(MONTH_FORMATTER)

    fun toDateKey(date: LocalDate): String = date.format(DATE_FORMATTER)
    fun toMonthKey(date: LocalDate): String = date.format(MONTH_FORMATTER)

    fun parseDateKey(key: String): LocalDate? {
        return try {
            LocalDate.parse(key, DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    fun formatHindiDate(date: LocalDate): String {
        val day = date.dayOfMonth
        val month = HINDI_MONTHS[date.monthValue - 1]
        val year = date.year
        return "$day $month $year"
    }

    fun formatHindiDayAndDate(date: LocalDate): String {
        val dayOfWeekIndex = date.dayOfWeek.value - 1 // 0 = Monday, 6 = Sunday
        val dayName = HINDI_DAYS[dayOfWeekIndex]
        val day = date.dayOfMonth
        val month = HINDI_MONTHS[date.monthValue - 1]
        return "$dayName, $day $month"
    }

    fun formatHindiMonthYear(yearMonth: YearMonth): String {
        val month = HINDI_MONTHS[yearMonth.monthValue - 1]
        return "$month ${yearMonth.year}"
    }

    fun formatHindiMonthYear(monthKey: String): String {
        return try {
            val ym = YearMonth.parse(monthKey, MONTH_FORMATTER)
            formatHindiMonthYear(ym)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun formatInr(amount: Double): String {
        return try {
            INR_FORMAT.format(Math.round(amount))
        } catch (_: Exception) {
            Math.round(amount).toString()
        }
    }

    fun calculateStreak(attendance: Map<String, AttendanceRecord>, todayKey: String): Pair<Int, Int> {
        var streak = 0
        var best = 0
        var cur = 0

        val today = parseDateKey(todayKey) ?: LocalDate.now()
        // Check past 365 days going backward from yesterday
        for (i in 1..365) {
            val d = today.minusDays(i.toLong())
            val k = toDateKey(d)
            val st = attendance[k]?.status
            if (st == AttendanceStatus.PRESENT || st == AttendanceStatus.HALF) {
                cur++
                if (i <= streak + 1) {
                    streak = cur
                }
            } else if (st == AttendanceStatus.ABSENT) {
                best = maxOf(best, cur)
                cur = 0
            }
        }
        best = maxOf(best, cur)

        // Also add today if marked present or half
        val todayStatus = attendance[todayKey]?.status
        if (todayStatus == AttendanceStatus.PRESENT || todayStatus == AttendanceStatus.HALF) {
            streak++
        }
        best = maxOf(best, streak)

        return Pair(streak, best)
    }
}
