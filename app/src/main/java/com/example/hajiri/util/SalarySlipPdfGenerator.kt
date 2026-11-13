package com.example.hajiri.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.MonthlyStats
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.Locale

data class MonthlySlipDailyRow(
    val dateKey: String,
    val dateFormatted: String,
    val dayShort: String,
    val statusCode: String,
    val statusLabel: String,
    val otHours: Float,
    val note: String
)

data class MonthlySlipData(
    val monthKey: String,
    val monthTitle: String,
    val employeeName: String,
    val employeePhone: String,
    val employeeEmail: String,
    val siteOrCompany: String = "",
    val extraWorkBonus: Double = 0.0,
    val extraWorkNote: String = "",
    val settings: UserSettings,
    val stats: MonthlyStats,
    val advances: List<AdvanceRecord>,
    val dailyRows: List<MonthlySlipDailyRow>
)

data class SlipTotals(
    val baseEarnings: Double,
    val otEarnings: Double,
    val extraBonus: Double,
    val previousBalanceAdded: Double,
    val grossEarnings: Double,
    val advanceDeduction: Double,
    val netPayable: Double
)

object SalarySlipPdfGenerator {

    fun calculateSlipTotals(data: MonthlySlipData): SlipTotals {
        val base = data.stats.baseEarnings
        val ot = data.stats.otEarnings
        val bonus = maxOf(0.0, data.extraWorkBonus)
        val prevAdded = if (!data.stats.isPreviousMonthReceived) maxOf(0.0, data.stats.carryForwardAdded) else 0.0
        val gross = base + ot + bonus + prevAdded
        val adv = maxOf(0.0, data.stats.totalAdvance)
        val net = maxOf(0.0, gross - adv)
        return SlipTotals(
            baseEarnings = base,
            otEarnings = ot,
            extraBonus = bonus,
            previousBalanceAdded = prevAdded,
            grossEarnings = gross,
            advanceDeduction = adv,
            netPayable = net
        )
    }

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2f)

    fun createShareablePdfUri(context: Context, data: MonthlySlipData): Uri {
        val slipsDir = File(context.cacheDir, "slips").apply {
            if (!exists()) mkdirs()
        }
        // Clean up older slip files to keep cache tidy
        slipsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 3_600_000L) {
                oldFile.delete()
            }
        }
        val safeMonth = data.monthKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(slipsDir, "Hajiri_Salary_Slip_$safeMonth.pdf")
        FileOutputStream(outFile).use { fos ->
            writePdf(data, fos)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun writePdf(data: MonthlySlipData, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            var y = drawHeader(canvas, data)
            y = drawEmployeeAndWageSection(canvas, data, y)
            y = drawAttendanceSummarySection(canvas, data, y)
            y = drawEarningsAndDeductionsTable(canvas, data, y)

            if (data.advances.isNotEmpty()) {
                if (y > PAGE_HEIGHT - 160f) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 42f
                }
                y = drawAdvancesSection(canvas, data, y)
            }

            if (data.dailyRows.isNotEmpty()) {
                if (y > PAGE_HEIGHT - 160f) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 42f
                }
                y = drawDailyLogHeader(canvas, y)
                for (row in data.dailyRows) {
                    if (y > PAGE_HEIGHT - 84f) {
                        drawFooter(canvas, pageNumber)
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        y = drawDailyLogHeader(canvas, 42f)
                    }
                    y = drawDailyLogRow(canvas, row, y)
                }
            }

            // Signature block at bottom if space permits, otherwise next page
            if (y > PAGE_HEIGHT - 95f) {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }
            drawSignatures(canvas, y + 18f)
            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawHeader(canvas: Canvas, data: MonthlySlipData): Float {
        val headerRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), 98f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#312E81")
        }
        canvas.drawRect(headerRect, headerPaint)

        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
        }
        canvas.drawRect(0f, 94f, PAGE_WIDTH.toFloat(), 100f, accentBar)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("हाजिरी  •  सैलरी स्लिप / मासिक बिल", MARGIN_H, 40f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E7FF")
            textSize = 11.5f
        }
        val subtitle = buildString {
            append("माह: ${data.monthTitle}")
            if (data.siteOrCompany.isNotBlank()) {
                append("   |   साइट/कंपनी: ${data.siteOrCompany.take(36)}")
            }
        }
        canvas.drawText(subtitle, MARGIN_H, 63f, subtitlePaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val generatedOn = DateUtils.formatHindiDate(LocalDate.now())
        canvas.drawText("बिल तिथि: $generatedOn", PAGE_WIDTH - MARGIN_H, 80f, datePaint)

        return 118f
    }

    private fun drawEmployeeAndWageSection(canvas: Canvas, data: MonthlySlipData, startY: Float): Float {
        val boxTop = startY
        val boxBottom = startY + 86f
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxBottom)
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val leftX = MARGIN_H + 14f
        val rightX = PAGE_WIDTH / 2f + 12f
        val wageMode = WageMode.fromId(data.settings.wageMode)

        canvas.drawText("कर्मचारी का नाम", leftX, boxTop + 20f, labelPaint)
        canvas.drawText(
            data.employeeName.ifBlank { "हाजिरी यूजर" }.take(30),
            leftX,
            boxTop + 36f,
            valuePaint
        )

        val contact = listOfNotNull(
            data.employeePhone.takeIf { it.isNotBlank() },
            data.employeeEmail.takeIf { it.isNotBlank() }
        ).joinToString(" • ").ifBlank { "-" }
        canvas.drawText("संपर्क विवरण", leftX, boxTop + 56f, labelPaint)
        canvas.drawText(contact.take(40), leftX, boxTop + 72f, valuePaint.apply { textSize = 10f })

        valuePaint.textSize = 11f
        canvas.drawText("वेतन प्रणाली", rightX, boxTop + 20f, labelPaint)
        val wageSummary = when (wageMode) {
            WageMode.DAILY -> "दिन की हाजिरी (₹${DateUtils.formatInr(data.settings.dailyWage)}/दिन)"
            WageMode.MONTHLY -> "मासिक वेतन (₹${DateUtils.formatInr(data.settings.salary)}/माह)"
        }
        canvas.drawText(wageSummary, rightX, boxTop + 36f, valuePaint)

        canvas.drawText("प्रति दिन / ओवरटाइम दर", rightX, boxTop + 56f, labelPaint)
        canvas.drawText(
            "₹${DateUtils.formatInr(data.stats.perDayRate)}/दिन  •  OT ₹${DateUtils.formatInr(data.settings.otRate)}/घंटा",
            rightX,
            boxTop + 72f,
            valuePaint.apply { textSize = 10f }
        )

        return boxBottom + 16f
    }

    private fun drawAttendanceSummarySection(canvas: Canvas, data: MonthlySlipData, startY: Float): Float {
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("हाजिरी सारांश (Attendance Summary)", MARGIN_H, startY, headingPaint)

        val top = startY + 8f
        val boxHeight = 52f
        val gap = 8f
        val cardWidth = (CONTENT_WIDTH - (gap * 4f)) / 5f

        val cards = listOf(
            Triple("कुल काम", "${formatFloatDays(data.stats.workedDays)} दिन", "#4F46E5"),
            Triple("हाजिर (P)", "${data.stats.presentDays}", "#059669"),
            Triple("गैरहाजिर (A)", "${data.stats.absentDays}", "#E11D48"),
            Triple("आधा दिन (HD)", "${data.stats.halfDays}", "#D97706"),
            Triple("उपस्थिति", "${data.stats.attendancePercentage}%", "#0284C7")
        )

        cards.forEachIndexed { index, (title, value, hexColor) ->
            val x = MARGIN_H + index * (cardWidth + gap)
            val rect = RectF(x, top, x + cardWidth, top + boxHeight)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
            }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(rect, 7f, 7f, fill)
            canvas.drawRoundRect(rect, 7f, 7f, stroke)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(hexColor)
                textSize = 14f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(value, x + cardWidth / 2f, top + 24f, valPaint)
            canvas.drawText(title, x + cardWidth / 2f, top + 41f, lblPaint)
        }

        return top + boxHeight + 18f
    }

    private fun drawEarningsAndDeductionsTable(canvas: Canvas, data: MonthlySlipData, startY: Float): Float {
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("वेतन एवं अधिक काम विवरण (Salary Bill)", MARGIN_H, startY, headingPaint)

        var y = startY + 10f
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEF2FF")
        }
        canvas.drawRoundRect(RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 24f), 5f, 5f, tableHeaderPaint)

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#312E81")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("विवरण (Description)", MARGIN_H + 10f, y + 15.5f, thPaint)
        canvas.drawText("राशि (Amount)", PAGE_WIDTH - MARGIN_H - 10f, y + 15.5f, thPaint.apply {
            textAlign = Paint.Align.RIGHT
        })
        y += 28f

        val rows = mutableListOf<Triple<String, String, Int>>()
        rows.add(
            Triple(
                "मूल कार्य मजदूरी (${formatFloatDays(data.stats.workedDays)} दिन × ₹${DateUtils.formatInr(data.stats.perDayRate)})",
                "₹${DateUtils.formatInr(data.stats.baseEarnings)}",
                Color.parseColor("#0F172A")
            )
        )
        rows.add(
            Triple(
                "अधिक काम / ओवरटाइम (${data.stats.totalOtHours} घंटे × ₹${DateUtils.formatInr(data.settings.otRate)})",
                "+₹${DateUtils.formatInr(data.stats.otEarnings)}",
                Color.parseColor("#EA580C")
            )
        )
        if (data.extraWorkBonus > 0.0) {
            val bonusTitle = if (data.extraWorkNote.isNotBlank()) {
                "अतिरिक्त काम / बोनस (${data.extraWorkNote.take(28)})"
            } else {
                "अतिरिक्त काम / बोनस"
            }
            rows.add(
                Triple(
                    bonusTitle,
                    "+₹${DateUtils.formatInr(data.extraWorkBonus)}",
                    Color.parseColor("#4F46E5")
                )
            )
        }

        val totals = calculateSlipTotals(data)
        if (data.stats.previousMonthRawBalance > 0.0) {
            if (!data.stats.isPreviousMonthReceived && totals.previousBalanceAdded > 0.0) {
                rows.add(
                    Triple(
                        "पिछले महीने का बाकी (${data.stats.previousMonthTitle})",
                        "+₹${DateUtils.formatInr(totals.previousBalanceAdded)}",
                        Color.parseColor("#059669")
                    )
                )
            } else {
                rows.add(
                    Triple(
                        "पिछला माह (${data.stats.previousMonthTitle}) — ले लिया गया ✓",
                        "₹0",
                        Color.parseColor("#64748B")
                    )
                )
            }
        }
        rows.add(
            Triple(
                "कुल कमाई (Gross + बाकी)",
                "₹${DateUtils.formatInr(totals.grossEarnings)}",
                Color.parseColor("#0F172A")
            )
        )
        rows.add(
            Triple(
                "एडवांस / उधार कटौती (${data.advances.size} प्रविष्टियाँ)",
                "-₹${DateUtils.formatInr(totals.advanceDeduction)}",
                Color.parseColor("#DB2777")
            )
        )

        val rowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
        }
        val rowValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.8f
        }

        rows.forEach { (desc, amt, color) ->
            canvas.drawText(desc, MARGIN_H + 10f, y + 14f, rowLabelPaint)
            rowValuePaint.color = color
            canvas.drawText(amt, PAGE_WIDTH - MARGIN_H - 10f, y + 14f, rowValuePaint)
            y += 22f
            canvas.drawLine(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y, linePaint)
        }

        val netPayable = totals.netPayable
        y += 8f
        val netBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECFDF5")
        }
        val netBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val netRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 36f)
        canvas.drawRoundRect(netRect, 8f, 8f, netBg)
        canvas.drawRoundRect(netRect, 8f, 8f, netBorder)

        val netLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#065F46")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val netAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 15.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("कुल हाथ में आने वाली सैलरी (Net Payable)", MARGIN_H + 14f, y + 22.5f, netLabelPaint)
        canvas.drawText("₹${DateUtils.formatInr(netPayable)}", PAGE_WIDTH - MARGIN_H - 14f, y + 23f, netAmountPaint)

        return y + 54f
    }

    private fun drawAdvancesSection(canvas: Canvas, data: MonthlySlipData, startY: Float): Float {
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("एडवांस / उधार सूची (${data.advances.size})", MARGIN_H, startY, headingPaint)

        var y = startY + 12f
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
        }
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DB2777")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        data.advances.take(8).forEach { adv ->
            val date = DateUtils.parseDateKey(adv.dateKey)
            val dateStr = if (date != null) DateUtils.formatHindiDate(date) else adv.dateKey
            val reason = adv.reason.ifBlank { "एडवांस" }.take(34)
            canvas.drawText("• $dateStr — $reason", MARGIN_H + 8f, y + 8f, rowPaint)
            canvas.drawText("-₹${DateUtils.formatInr(adv.amount)}", PAGE_WIDTH - MARGIN_H - 8f, y + 8f, amtPaint)
            y += 16f
        }
        return y + 12f
    }

    private fun drawDailyLogHeader(canvas: Canvas, startY: Float): Float {
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("दैनिक हाजिरी एवं अधिक काम लॉग", MARGIN_H, startY, headingPaint)

        val top = startY + 8f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
        }
        canvas.drawRoundRect(RectF(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + 20f), 4f, 4f, bg)

        val th = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("तारीख़", MARGIN_H + 8f, top + 13.5f, th)
        canvas.drawText("स्थिति", MARGIN_H + 145f, top + 13.5f, th)
        canvas.drawText("अधिक काम (OT)", MARGIN_H + 255f, top + 13.5f, th)
        canvas.drawText("नोट", MARGIN_H + 345f, top + 13.5f, th)

        return top + 24f
    }

    private fun drawDailyLogRow(canvas: Canvas, row: MonthlySlipDailyRow, startY: Float): Float {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 8.5f
        }
        val statusColor = when (row.statusCode) {
            "P" -> Color.parseColor("#059669")
            "A" -> Color.parseColor("#E11D48")
            "HD" -> Color.parseColor("#D97706")
            "L" -> Color.parseColor("#0284C7")
            else -> Color.parseColor("#64748B")
        }
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 0.7f
        }

        canvas.drawText("${row.dateFormatted} (${row.dayShort})", MARGIN_H + 8f, startY + 11f, textPaint)
        val statusText = if (row.statusLabel.isNotBlank()) "${row.statusCode} • ${row.statusLabel}" else "-"
        canvas.drawText(statusText, MARGIN_H + 145f, startY + 11f, statusPaint)
        val otText = if (row.otHours > 0f) "${row.otHours} घंटे" else "-"
        canvas.drawText(otText, MARGIN_H + 255f, startY + 11f, textPaint)
        canvas.drawText(row.note.ifBlank { "-" }.take(30), MARGIN_H + 345f, startY + 11f, textPaint)

        val nextY = startY + 16f
        canvas.drawLine(MARGIN_H, nextY, PAGE_WIDTH - MARGIN_H, nextY, divider)
        return nextY + 2f
    }

    private fun drawSignatures(canvas: Canvas, topY: Float) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 0.8f
        }
        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }
        canvas.drawLine(MARGIN_H + 10f, topY + 22f, MARGIN_H + 150f, topY + 22f, linePaint)
        canvas.drawText("कर्मचारी हस्ताक्षर", MARGIN_H + 35f, topY + 35f, lblPaint)

        canvas.drawLine(PAGE_WIDTH - MARGIN_H - 150f, topY + 22f, PAGE_WIDTH - MARGIN_H - 10f, topY + 22f, linePaint)
        canvas.drawText("नियोक्ता / ठेकेदार हस्ताक्षर", PAGE_WIDTH - MARGIN_H - 135f, topY + 35f, lblPaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
        }
        canvas.drawText(
            "हाजिरी ऐप द्वारा जनरेट किया गया डिजिटल वेतन बिल",
            MARGIN_H,
            PAGE_HEIGHT - 20f,
            footerPaint
        )
        canvas.drawText(
            "पेज $pageNumber",
            PAGE_WIDTH - MARGIN_H,
            PAGE_HEIGHT - 20f,
            footerPaint.apply { textAlign = Paint.Align.RIGHT }
        )
    }

    internal fun formatFloatDays(days: Float): String {
        return if (days % 1f == 0f) days.toInt().toString() else String.format(Locale.US, "%.1f", days)
    }
}
