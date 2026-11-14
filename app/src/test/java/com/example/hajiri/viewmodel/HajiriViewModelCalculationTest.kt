package com.example.hajiri.viewmodel

import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.OvertimeRecord
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import com.example.hajiri.model.WorkSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HajiriViewModelCalculationTest {

    @Test
    fun calculateMonthlyStats_multiSiteAndCumulativePartialCarryForward() {
        val siteA = WorkSite(
            id = "site_a",
            name = "साइट A",
            wageMode = WageMode.DAILY.id,
            dailyWage = 600.0,
            otRate = 80.0,
            workDays = 26
        )
        val siteB = WorkSite(
            id = "site_b",
            name = "साइट B",
            wageMode = WageMode.DAILY.id,
            dailyWage = 1000.0,
            otRate = 120.0,
            workDays = 26
        )

        val attendance = mapOf(
            // August 2026: 2 days at siteA (1200) -> completely unpaid -> 1200 carried forward
            "2026-08-01" to AttendanceRecord(status = AttendanceStatus.PRESENT, siteId = "site_a"),
            "2026-08-02" to AttendanceRecord(status = AttendanceStatus.PRESENT, siteId = "site_a"),
            // September 2026: 2 days at siteB (2000) -> 1500 partial paid -> 500 remaining
            "2026-09-05" to AttendanceRecord(status = AttendanceStatus.PRESENT, siteId = "site_b"),
            "2026-09-06" to AttendanceRecord(status = AttendanceStatus.PRESENT, siteId = "site_b"),
            // October 2026: 1 day at siteA (600) + 1 half-day at siteB (500) = 1100
            "2026-10-10" to AttendanceRecord(status = AttendanceStatus.PRESENT, siteId = "site_a"),
            "2026-10-11" to AttendanceRecord(status = AttendanceStatus.HALF, siteId = "site_b")
        )

        val overtime = mapOf(
            "2026-10-10" to OvertimeRecord(dateKey = "2026-10-10", hours = 2.0, siteId = "site_b") // 240
        )

        val advances = listOf(
            AdvanceRecord(id = "adv1", amount = 300.0, reason = "दवाई", dateKey = "2026-10-09", siteId = "site_a")
        )

        val settings = UserSettings(
            wageMode = WageMode.DAILY.id,
            dailyWage = 600.0,
            otRate = 80.0,
            activeSiteId = "site_a",
            sites = listOf(siteA, siteB),
            monthPayments = mapOf("2026-09" to 1500.0)
        )

        val stats = HajiriViewModel.calculateMonthlyStats(
            monthKey = "2026-10",
            att = attendance,
            ot = overtime,
            adv = advances,
            sett = settings
        )

        // Base earnings for Oct = 600 + 500 = 1100
        assertEquals(1100.0, stats.baseEarnings, 0.01)
        // OT earnings for Oct = 2 * 120 = 240
        assertEquals(240.0, stats.otEarnings, 0.01)
        // Advances for Oct = 300
        assertEquals(300.0, stats.totalAdvance, 0.01)
        // Carry forward = Aug (1200) + Sept remaining (2000 - 1500 = 500) = 1700
        assertEquals(1700.0, stats.carryForwardAdded, 0.01)
        assertEquals(2, stats.previousMonthsPendingCount)
        assertEquals(1500.0, stats.previousMonthPaidAmount, 0.01)
        assertEquals(500.0, stats.previousMonthRawBalance, 0.01)
        assertFalse(stats.isPreviousMonthReceived)
        // Net take home = 1100 + 240 - 300 + 1700 = 2740
        assertEquals(2740.0, stats.netTakeHome, 0.01)
    }

    @Test
    fun calculateMonthlyStats_autoMonthDaysForMonthlySalaryAndPercentage() {
        val monthlyAutoSettings = UserSettings(
            wageMode = WageMode.MONTHLY.id,
            salary = 28000.0,
            workDays = 26 // 26 triggers auto calendar month length (28/29/30/31)
        )

        // Feb 2026 has 28 days -> 28000 / 28 = 1000/day, 14 worked days = 50%
        val febAttendance = (1..14).associate { day ->
            String.format("2026-02-%02d", day) to AttendanceRecord(status = AttendanceStatus.PRESENT)
        }
        val febStats = HajiriViewModel.calculateMonthlyStats(
            monthKey = "2026-02",
            att = febAttendance,
            ot = emptyMap(),
            adv = emptyList(),
            sett = monthlyAutoSettings
        )
        assertEquals(1000.0, febStats.perDayRate, 0.01)
        assertEquals(14000.0, febStats.baseEarnings, 0.01)
        assertEquals(50, febStats.attendancePercentage)

        // April 2026 has 30 days -> 30000 / 30 = 1000/day, 15 worked days = 50%
        val aprSettings = monthlyAutoSettings.copy(salary = 30000.0)
        val aprAttendance = (1..15).associate { day ->
            String.format("2026-04-%02d", day) to AttendanceRecord(status = AttendanceStatus.PRESENT)
        }
        val aprStats = HajiriViewModel.calculateMonthlyStats(
            monthKey = "2026-04",
            att = aprAttendance,
            ot = emptyMap(),
            adv = emptyList(),
            sett = aprSettings
        )
        assertEquals(1000.0, aprStats.perDayRate, 0.01)
        assertEquals(15000.0, aprStats.baseEarnings, 0.01)
        assertEquals(50, aprStats.attendancePercentage)

        // Custom workDays (25) overrides calendar month length
        val customSettings = aprSettings.copy(workDays = 25)
        val customStats = HajiriViewModel.calculateMonthlyStats(
            monthKey = "2026-04",
            att = aprAttendance,
            ot = emptyMap(),
            adv = emptyList(),
            sett = customSettings
        )
        assertEquals(1200.0, customStats.perDayRate, 0.01)
        assertEquals(18000.0, customStats.baseEarnings, 0.01)
        assertEquals(60, customStats.attendancePercentage)
    }

    @Test
    fun calculateExpenseSummary_aggregatesCategoriesSortsLatestFirstAndComputesSavings() {
        val monthStats = com.example.hajiri.model.MonthlyStats(
            yearMonth = "2026-11",
            baseEarnings = 15000.0,
            otEarnings = 1200.0
        )
        val expenses = listOf(
            com.example.hajiri.model.ExpenseRecord(
                id = "e1",
                amount = 150.0,
                category = com.example.hajiri.model.ExpenseCategory.FOOD.id,
                note = "चाय व खाना",
                dateKey = "2026-11-05"
            ),
            com.example.hajiri.model.ExpenseRecord(
                id = "e2",
                amount = 650.0,
                category = com.example.hajiri.model.ExpenseCategory.TRANSPORT.id,
                note = "पेट्रोल",
                dateKey = "2026-11-14"
            ),
            com.example.hajiri.model.ExpenseRecord(
                id = "e3",
                amount = 200.0,
                category = com.example.hajiri.model.ExpenseCategory.FOOD.id,
                note = "दोपहर का खाना",
                dateKey = "2026-11-10"
            ),
            com.example.hajiri.model.ExpenseRecord(
                id = "e_old",
                amount = 999.0,
                category = com.example.hajiri.model.ExpenseCategory.TOOLS.id,
                note = "अक्टूबर खर्च",
                dateKey = "2026-10-20"
            )
        )

        val summary = HajiriViewModel.calculateExpenseSummary(
            monthKey = "2026-11",
            allExpenses = expenses,
            monthStats = monthStats
        )

        assertEquals(1000.0, summary.totalExpense, 0.01)
        assertEquals(16200.0, summary.monthlyIncome, 0.01)
        assertEquals(15200.0, summary.netSavings, 0.01)
        assertEquals(listOf("e2", "e3", "e1"), summary.entries.map { it.id })

        val transport = summary.categoryBreakdowns.first { it.category == com.example.hajiri.model.ExpenseCategory.TRANSPORT }
        val food = summary.categoryBreakdowns.first { it.category == com.example.hajiri.model.ExpenseCategory.FOOD }
        assertEquals(650.0, transport.totalAmount, 0.01)
        assertEquals(0.65f, transport.percentage, 0.001f)
        assertEquals(350.0, food.totalAmount, 0.01)
        assertEquals(2, food.count)
    }
}
