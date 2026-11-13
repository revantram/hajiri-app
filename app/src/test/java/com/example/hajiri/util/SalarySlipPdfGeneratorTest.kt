package com.example.hajiri.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test
    fun writePdf_rendersAllAdvancesAndLongHindiDescriptionsAcrossPages() {
        val manyAdvances = (1..15).map { idx ->
            AdvanceRecord(
                id = "adv_$idx",
                amount = 250.0,
                reason = "लंबा हिंदी विवरण क्रमांक $idx — निर्माण सामग्री व पारिवारिक आवश्यकता हेतु एडवांस राशि",
                dateKey = String.format("2026-11-%02d", idx.coerceAtMost(28))
            )
        }
        val manyRows = (1..22).map { day ->
            MonthlySlipDailyRow(
                dateKey = String.format("2026-11-%02d", day),
                dateFormatted = "$day नवंबर",
                dayShort = "सोमवार",
                statusCode = "P",
                statusLabel = "हाजिर",
                otHours = 2.0f,
                note = "सुबह 8 बजे से रात 9 बजे तक अतिरिक्त काम व छत ढलाई का कार्य दिवस $day"
            )
        }
        val data = MonthlySlipData(
            monthKey = "2026-11",
            monthTitle = "नवंबर 2026",
            employeeName = "महेश प्रसाद शर्मा (मुख्य राजमिस्त्री)",
            employeePhone = "9876543210",
            employeeEmail = "mahesh@example.com",
            siteOrCompany = "महादेव बिल्डर्स एवं कंस्ट्रक्शन साइट",
            extraWorkBonus = 1500.0,
            extraWorkNote = "रविवार व रात्रि पाली विशेष अतिरिक्त कार्य भत्ता बोनस",
            settings = UserSettings(
                salary = 24000.0,
                dailyWage = 900.0,
                wageMode = WageMode.DAILY.id,
                otRate = 120.0,
                workDays = 26
            ),
            stats = MonthlyStats(
                yearMonth = "2026-11",
                presentDays = 22,
                absentDays = 0,
                halfDays = 0,
                leaveDays = 0,
                totalOtHours = 44f,
                totalAdvance = manyAdvances.sumOf { it.amount },
                baseEarnings = 19800.0,
                otEarnings = 5280.0,
                perDayRate = 900.0,
                netTakeHome = 21330.0,
                attendancePercentage = 73,
                workedDays = 22f
            ),
            advances = manyAdvances,
            dailyRows = manyRows
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val font = androidx.core.content.res.ResourcesCompat.getFont(
            context,
            com.example.hajiri.R.font.noto_sans_devanagari
        )
        org.junit.Assert.assertNotNull(font)

        val bitmaps = mutableListOf<android.graphics.Bitmap>()
        var finishedPages = 0
        val testSink = object : SalarySlipPdfGenerator.SlipPageSink {
            override fun startNewPage(pageNumber: Int, width: Int, height: Int): android.graphics.Canvas {
                val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                bitmaps.add(bmp)
                return android.graphics.Canvas(bmp)
            }

            override fun finishCurrentPage() {
                finishedPages++
            }
        }

        val totalPages = SalarySlipPdfGenerator.renderToSink(data, testSink, context)
        assertEquals(true, totalPages >= 2)
        assertEquals(totalPages, bitmaps.size)
        assertEquals(totalPages, finishedPages)
    }
}
