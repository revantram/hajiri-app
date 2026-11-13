package com.example.hajiri.util

import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SalarySlipPdfGeneratorTest {

    @Test
    fun calculateSlipTotals_includesExtraWorkBonusAndSubtractsAdvances() {
        val data = MonthlySlipData(
            monthKey = "2026-11",
            monthTitle = "नवंबर 2026",
            employeeName = "रमेश कुमार",
            employeePhone = "9876543210",
            employeeEmail = "ramesh@example.com",
            siteOrCompany = "गुप्ता कंस्ट्रक्शन",
            extraWorkBonus = 1200.0,
            extraWorkNote = "संडे डबल ड्यूटी",
            settings = UserSettings(
                salary = 24000.0,
                dailyWage = 900.0,
                wageMode = WageMode.DAILY.id,
                otRate = 120.0,
                workDays = 26
            ),
            stats = MonthlyStats(
                yearMonth = "2026-11",
                presentDays = 26,
                absentDays = 0,
                halfDays = 4,
                leaveDays = 0,
                totalOtHours = 20f,
                totalAdvance = 2500.0,
                baseEarnings = 25200.0,
                otEarnings = 2400.0,
                perDayRate = 900.0,
                netTakeHome = 25100.0,
                attendancePercentage = 93,
                workedDays = 28f
            ),
            advances = listOf(
                AdvanceRecord(id = "a1", amount = 1500.0, reason = "दवाई", dateKey = "2026-11-08"),
                AdvanceRecord(id = "a2", amount = 1000.0, reason = "राशन", dateKey = "2026-11-18")
            ),
            dailyRows = listOf(
                MonthlySlipDailyRow(
                    dateKey = "2026-11-01",
                    dateFormatted = "1 नवंबर",
                    dayShort = "रविवार",
                    statusCode = "P",
                    statusLabel = "हाजिर",
                    otHours = 2.0f,
                    note = "ओवरटाइम साइट A"
                )
            )
        )

        val totals = SalarySlipPdfGenerator.calculateSlipTotals(data)

        assertEquals(25200.0, totals.baseEarnings, 0.01)
        assertEquals(2400.0, totals.otEarnings, 0.01)
        assertEquals(1200.0, totals.extraBonus, 0.01)
        assertEquals(0.0, totals.previousBalanceAdded, 0.01)
        assertEquals(28800.0, totals.grossEarnings, 0.01)
        assertEquals(2500.0, totals.advanceDeduction, 0.01)
        assertEquals(26300.0, totals.netPayable, 0.01)
        assertEquals("28", SalarySlipPdfGenerator.formatFloatDays(28f))
        assertEquals("26.5", SalarySlipPdfGenerator.formatFloatDays(26.5f))

        val withUnpaidPrev = data.copy(
            stats = data.stats.copy(
                previousMonthKey = "2026-10",
                previousMonthTitle = "अक्टूबर 2026",
                previousMonthRawBalance = 3500.0,
                isPreviousMonthReceived = false,
                carryForwardAdded = 3500.0
            )
        )
        val totalsWithPrev = SalarySlipPdfGenerator.calculateSlipTotals(withUnpaidPrev)
        assertEquals(3500.0, totalsWithPrev.previousBalanceAdded, 0.01)
        assertEquals(32300.0, totalsWithPrev.grossEarnings, 0.01)
        assertEquals(29800.0, totalsWithPrev.netPayable, 0.01)

        val withSettledPrev = withUnpaidPrev.copy(
            stats = withUnpaidPrev.stats.copy(
                isPreviousMonthReceived = true,
                carryForwardAdded = 0.0
            )
        )
        val totalsSettled = SalarySlipPdfGenerator.calculateSlipTotals(withSettledPrev)
        assertEquals(0.0, totalsSettled.previousBalanceAdded, 0.01)
        assertEquals(28800.0, totalsSettled.grossEarnings, 0.01)
        assertEquals(26300.0, totalsSettled.netPayable, 0.01)
    }
}
