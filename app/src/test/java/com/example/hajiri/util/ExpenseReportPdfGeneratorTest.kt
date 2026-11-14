package com.example.hajiri.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.hajiri.R
import com.example.hajiri.model.ExpenseCategory
import com.example.hajiri.model.ExpenseRecord
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.viewmodel.HajiriViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpenseReportPdfGeneratorTest {

    @Test
    fun formatSignedInr_formatsPositiveZeroAndNegativeValues() {
        assertEquals("₹2,500", ExpenseReportPdfGenerator.formatSignedInr(2500.0))
        assertEquals("₹0", ExpenseReportPdfGenerator.formatSignedInr(0.0))
        assertEquals("-₹1,250", ExpenseReportPdfGenerator.formatSignedInr(-1250.0))
    }

    @Test
    fun renderToSink_rendersHindiExpenseReportAcrossMultiplePages() {
        val entries = (1..28).map { idx ->
            ExpenseRecord(
                id = "exp_$idx",
                amount = 120.0 + (idx * 15),
                category = ExpenseCategory.entries[idx % ExpenseCategory.entries.size].id,
                note = "दैनिक खर्च प्रविष्टि क्रमांक $idx — चाय, किराया व जरूरी सामान",
                dateKey = String.format("2026-11-%02d", ((idx - 1) % 28) + 1)
            )
        }
        val summary = HajiriViewModel.calculateExpenseSummary(
            monthKey = "2026-11",
            allExpenses = entries,
            monthStats = MonthlyStats(
                yearMonth = "2026-11",
                baseEarnings = 18000.0,
                otEarnings = 1500.0
            )
        )
        val data = ExpenseReportPdfData(
            userName = "रमेश कुमार",
            userPhone = "9876543210",
            userEmail = "ramesh@example.com",
            summary = summary
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val font = ResourcesCompat.getFont(context, R.font.noto_sans_devanagari)
        assertNotNull(font)

        val bitmaps = mutableListOf<Bitmap>()
        var finishedPages = 0
        val sink = object : ExpenseReportPdfGenerator.ExpensePageSink {
            override fun startNewPage(pageNumber: Int, width: Int, height: Int): Canvas {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmaps.add(bmp)
                return Canvas(bmp)
            }

            override fun finishCurrentPage() {
                finishedPages++
            }
        }

        val totalPages = ExpenseReportPdfGenerator.renderToSink(data, sink, context)
        assertTrue(totalPages >= 2)
        assertEquals(totalPages, bitmaps.size)
        assertEquals(totalPages, finishedPages)
    }
}
