package com.example.hajiri.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.hajiri.R
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
        val prevAdded = maxOf(0.0, data.stats.carryForwardAdded)
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
    private const val TOP_MARGIN_NEXT_PAGE = 42f
    private const val FOOTER_SAFE_Y = PAGE_HEIGHT - 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2f)

    private class FontSet(
        val regular: Typeface,
        val bold: Typeface
    )

    private fun resolveFonts(context: Context?): FontSet {
        val devanagari = context?.let { ctx ->
            runCatching { ResourcesCompat.getFont(ctx, R.font.noto_sans_devanagari) }.getOrNull()
        }
        val regular = devanagari ?: Typeface.DEFAULT
        val bold = runCatching { Typeface.create(regular, Typeface.BOLD) }
            .getOrDefault(Typeface.DEFAULT_BOLD)
        return FontSet(regular = regular, bold = bold)
    }

    internal interface SlipPageSink {
        fun startNewPage(pageNumber: Int, width: Int, height: Int): Canvas
        fun finishCurrentPage()
    }

    private class AndroidPdfPageSink(
        private val document: PdfDocument
    ) : SlipPageSink {
        private var activePage: PdfDocument.Page? = null

        override fun startNewPage(pageNumber: Int, width: Int, height: Int): Canvas {
            val info = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
            val page = document.startPage(info)
            activePage = page
            return page.canvas
        }

        override fun finishCurrentPage() {
            activePage?.let { document.finishPage(it) }
            activePage = null
        }
    }

    private class PdfPaginator(
        private val sink: SlipPageSink,
        private val fonts: FontSet
    ) {
        var pageNumber: Int = 1
            private set
        var canvas: Canvas = sink.startNewPage(pageNumber, PAGE_WIDTH, PAGE_HEIGHT)
            private set
        var y: Float = 0f

        fun ensureSpace(neededHeight: Float, onNewPage: (() -> Unit)? = null) {
            if (y + neededHeight <= FOOTER_SAFE_Y) return
            startNextPage()
            onNewPage?.invoke()
        }

        fun startNextPage() {
            drawFooter(canvas, pageNumber, fonts)
            sink.finishCurrentPage()
            pageNumber++
            canvas = sink.startNewPage(pageNumber, PAGE_WIDTH, PAGE_HEIGHT)
            y = TOP_MARGIN_NEXT_PAGE
        }

        fun finishLastPage() {
            drawFooter(canvas, pageNumber, fonts)
            sink.finishCurrentPage()
        }
    }

    fun createShareablePdfUri(context: Context, data: MonthlySlipData): Uri {
        val slipsDir = File(context.cacheDir, "slips").apply {
            if (!exists()) mkdirs()
        }
        slipsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 3_600_000L) {
                oldFile.delete()
            }
        }
        val safeMonth = data.monthKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(slipsDir, "Hajiri_Salary_Slip_$safeMonth.pdf")
        FileOutputStream(outFile).use { fos ->
            writePdf(data, fos, context)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun writePdf(
        data: MonthlySlipData,
        outputStream: OutputStream,
        context: Context? = null
    ) {
        val document = PdfDocument()
        try {
            renderToSink(data, AndroidPdfPageSink(document), context)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    internal fun renderToSink(
        data: MonthlySlipData,
        sink: SlipPageSink,
        context: Context? = null
    ): Int {
        val fonts = resolveFonts(context)
        val paginator = PdfPaginator(sink, fonts)

        paginator.y = drawHeader(paginator.canvas, data, fonts)
        paginator.y = drawEmployeeAndWageSection(paginator.canvas, data, paginator.y, fonts)
        paginator.y = drawAttendanceSummarySection(paginator.canvas, data, paginator.y, fonts)

        drawEarningsAndDeductionsTable(paginator, data, fonts)
        if (data.advances.isNotEmpty()) {
            drawAdvancesSection(paginator, data, fonts)
        }
        if (data.dailyRows.isNotEmpty()) {
            drawDailyLogSection(paginator, data, fonts)
        }

        paginator.ensureSpace(78f)
        drawSignatures(paginator.canvas, paginator.y + 14f, fonts)
        paginator.finishLastPage()
        return paginator.pageNumber
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, maxWidth: Int): StaticLayout {
        val safeWidth = maxWidth.coerceAtLeast(36)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1.0f)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun makeTextPaint(
        colorInt: Int,
        size: Float,
        tf: Typeface,
        align: Paint.Align = Paint.Align.LEFT
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            textSize = size
            typeface = tf
            textAlign = align
        }
    }

    private fun drawHeader(canvas: Canvas, data: MonthlySlipData, fonts: FontSet): Float {
        val subtitleText = buildString {
            append("माह: ${data.monthTitle}")
            if (data.siteOrCompany.isNotBlank()) {
                append("   |   साइट/कंपनी: ${data.siteOrCompany.trim()}")
            }
        }
        val subtitlePaint = makeTextPaint(Color.parseColor("#E0E7FF"), 11f, fonts.regular)
        val subtitleLayout = buildStaticLayout(subtitleText, subtitlePaint, (CONTENT_WIDTH - 140f).toInt())
        val headerHeight = maxOf(98f, 64f + subtitleLayout.height + 16f)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#312E81")
        }
        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight), headerPaint)

        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
        }
        canvas.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight + 2f, accentBar)

        val titlePaint = makeTextPaint(Color.WHITE, 20f, fonts.bold)
        canvas.drawText("हाजिरी  •  सैलरी स्लिप / मासिक बिल", MARGIN_H, 38f, titlePaint)

        drawStaticLayout(canvas, subtitleLayout, MARGIN_H, 50f)

        val datePaint = makeTextPaint(Color.parseColor("#C7D2FE"), 9.5f, fonts.regular, Paint.Align.RIGHT)
        val generatedOn = DateUtils.formatHindiDate(LocalDate.now())
        canvas.drawText("बिल तिथि: $generatedOn", PAGE_WIDTH - MARGIN_H, headerHeight - 16f, datePaint)

        return headerHeight + 18f
    }

    private fun drawEmployeeAndWageSection(
        canvas: Canvas,
        data: MonthlySlipData,
        startY: Float,
        fonts: FontSet
    ): Float {
        val labelPaint = makeTextPaint(Color.parseColor("#64748B"), 9f, fonts.regular)
        val valuePaint = makeTextPaint(Color.parseColor("#0F172A"), 10.5f, fonts.bold)
        val subValuePaint = makeTextPaint(Color.parseColor("#0F172A"), 9.8f, fonts.bold)

        val colWidth = ((CONTENT_WIDTH / 2f) - 24f).toInt()
        val empName = data.employeeName.ifBlank { "हाजिरी यूजर" }
        val contact = listOfNotNull(
            data.employeePhone.takeIf { it.isNotBlank() },
            data.employeeEmail.takeIf { it.isNotBlank() }
        ).joinToString(" • ").ifBlank { "-" }

        val wageMode = WageMode.fromId(data.settings.wageMode)
        val wageSummary = when (wageMode) {
            WageMode.DAILY -> "दिन की हाजिरी (₹${DateUtils.formatInr(data.settings.dailyWage)}/दिन)"
            WageMode.MONTHLY -> "मासिक वेतन (₹${DateUtils.formatInr(data.settings.salary)}/माह)"
        }
        val rateSummary =
            "₹${DateUtils.formatInr(data.stats.perDayRate)}/दिन  •  OT ₹${DateUtils.formatInr(data.settings.otRate)}/घंटा"

        val empNameLayout = buildStaticLayout(empName, valuePaint, colWidth)
        val contactLayout = buildStaticLayout(contact, subValuePaint, colWidth)
        val wageLayout = buildStaticLayout(wageSummary, valuePaint, colWidth)
        val rateLayout = buildStaticLayout(rateSummary, subValuePaint, colWidth)

        val leftHeight = 20f + empNameLayout.height + 16f + contactLayout.height + 14f
        val rightHeight = 20f + wageLayout.height + 16f + rateLayout.height + 14f
        val boxHeight = maxOf(86f, maxOf(leftHeight, rightHeight))

        val boxTop = startY
        val boxBottom = startY + boxHeight
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxBottom)
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val leftX = MARGIN_H + 14f
        val rightX = PAGE_WIDTH / 2f + 10f

        canvas.drawText("कर्मचारी का नाम", leftX, boxTop + 18f, labelPaint)
        drawStaticLayout(canvas, empNameLayout, leftX, boxTop + 23f)
        val contactLabelY = boxTop + 23f + empNameLayout.height + 14f
        canvas.drawText("संपर्क विवरण", leftX, contactLabelY, labelPaint)
        drawStaticLayout(canvas, contactLayout, leftX, contactLabelY + 5f)

        canvas.drawText("वेतन प्रणाली", rightX, boxTop + 18f, labelPaint)
        drawStaticLayout(canvas, wageLayout, rightX, boxTop + 23f)
        val rateLabelY = boxTop + 23f + wageLayout.height + 14f
        canvas.drawText("प्रति दिन / ओवरटाइम दर", rightX, rateLabelY, labelPaint)
        drawStaticLayout(canvas, rateLayout, rightX, rateLabelY + 5f)

        return boxBottom + 16f
    }

    private fun drawAttendanceSummarySection(
        canvas: Canvas,
        data: MonthlySlipData,
        startY: Float,
        fonts: FontSet
    ): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 12f, fonts.bold)
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
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(rect, 7f, 7f, fill)
            canvas.drawRoundRect(rect, 7f, 7f, stroke)

            val valPaint = makeTextPaint(Color.parseColor(hexColor), 13.5f, fonts.bold, Paint.Align.CENTER)
            val lblPaint = makeTextPaint(Color.parseColor("#64748B"), 8.5f, fonts.regular, Paint.Align.CENTER)
            canvas.drawText(value, x + cardWidth / 2f, top + 24f, valPaint)
            canvas.drawText(title, x + cardWidth / 2f, top + 41f, lblPaint)
        }

        return top + boxHeight + 18f
    }

    private fun drawSalaryTableHeader(canvas: Canvas, startY: Float, fonts: FontSet, continued: Boolean = false): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 12f, fonts.bold)
        val headingText = if (continued) {
            "वेतन एवं अधिक काम विवरण (जारी)"
        } else {
            "वेतन एवं अधिक काम विवरण (Salary Bill)"
        }
        canvas.drawText(headingText, MARGIN_H, startY, headingPaint)

        val y = startY + 10f
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EEF2FF") }
        canvas.drawRoundRect(RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 24f), 5f, 5f, tableHeaderPaint)

        val thLeft = makeTextPaint(Color.parseColor("#312E81"), 9.5f, fonts.bold)
        val thRight = makeTextPaint(Color.parseColor("#312E81"), 9.5f, fonts.bold, Paint.Align.RIGHT)
        canvas.drawText("विवरण (Description)", MARGIN_H + 10f, y + 15.5f, thLeft)
        canvas.drawText("राशि (Amount)", PAGE_WIDTH - MARGIN_H - 10f, y + 15.5f, thRight)
        return y + 28f
    }

    private fun drawEarningsAndDeductionsTable(
        paginator: PdfPaginator,
        data: MonthlySlipData,
        fonts: FontSet
    ) {
        paginator.ensureSpace(90f)
        paginator.y = drawSalaryTableHeader(paginator.canvas, paginator.y, fonts)

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
                "अतिरिक्त काम / बोनस (${data.extraWorkNote})"
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
        if (totals.previousBalanceAdded > 0.0 || data.stats.previousMonthRawBalance > 0.0) {
            if (totals.previousBalanceAdded > 0.0) {
                val prevLabel = if (data.stats.previousMonthsPendingCount > 1) {
                    "पिछला संचयी बाकी (${data.stats.previousMonthsPendingCount} माह)"
                } else {
                    "पिछले महीने का बाकी (${data.stats.previousMonthTitle})"
                }
                rows.add(
                    Triple(
                        prevLabel,
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

        val rowLabelPaint = makeTextPaint(Color.parseColor("#334155"), 10f, fonts.regular)
        val rowValuePaint = makeTextPaint(Color.parseColor("#0F172A"), 10.5f, fonts.bold, Paint.Align.RIGHT)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.8f
        }
        // Leave generous room on the right for formatted ₹ amount so desc never overlaps amt
        val descMaxWidth = (CONTENT_WIDTH - 132f).toInt()

        rows.forEach { (desc, amt, color) ->
            val descLayout = buildStaticLayout(desc, rowLabelPaint, descMaxWidth)
            val rowHeight = maxOf(22f, descLayout.height + 10f)
            paginator.ensureSpace(rowHeight + 4f) {
                paginator.y = drawSalaryTableHeader(paginator.canvas, paginator.y, fonts, continued = true)
            }

            drawStaticLayout(paginator.canvas, descLayout, MARGIN_H + 10f, paginator.y + 5f)
            rowValuePaint.color = color
            paginator.canvas.drawText(amt, PAGE_WIDTH - MARGIN_H - 10f, paginator.y + 15f, rowValuePaint)
            paginator.y += rowHeight
            paginator.canvas.drawLine(MARGIN_H, paginator.y, PAGE_WIDTH - MARGIN_H, paginator.y, linePaint)
        }

        paginator.ensureSpace(54f)
        val netPayable = totals.netPayable
        paginator.y += 8f
        val netBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ECFDF5") }
        val netBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val netRect = RectF(MARGIN_H, paginator.y, PAGE_WIDTH - MARGIN_H, paginator.y + 36f)
        paginator.canvas.drawRoundRect(netRect, 8f, 8f, netBg)
        paginator.canvas.drawRoundRect(netRect, 8f, 8f, netBorder)

        val netLabelPaint = makeTextPaint(Color.parseColor("#065F46"), 11.5f, fonts.bold)
        val netAmountPaint = makeTextPaint(Color.parseColor("#059669"), 15.5f, fonts.bold, Paint.Align.RIGHT)
        paginator.canvas.drawText("कुल हाथ में आने वाली सैलरी (Net Payable)", MARGIN_H + 14f, paginator.y + 22.5f, netLabelPaint)
        paginator.canvas.drawText("₹${DateUtils.formatInr(netPayable)}", PAGE_WIDTH - MARGIN_H - 14f, paginator.y + 23f, netAmountPaint)

        paginator.y += 52f
    }

    private fun drawAdvancesHeading(canvas: Canvas, startY: Float, totalCount: Int, fonts: FontSet, continued: Boolean): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 11f, fonts.bold)
        val title = if (continued) {
            "एडवांस / उधार सूची ($totalCount) — जारी"
        } else {
            "एडवांस / उधार सूची ($totalCount)"
        }
        canvas.drawText(title, MARGIN_H, startY, headingPaint)
        return startY + 10f
    }

    private fun drawAdvancesSection(
        paginator: PdfPaginator,
        data: MonthlySlipData,
        fonts: FontSet
    ) {
        paginator.ensureSpace(48f)
        paginator.y = drawAdvancesHeading(paginator.canvas, paginator.y, data.advances.size, fonts, continued = false)

        val rowPaint = makeTextPaint(Color.parseColor("#475569"), 9.5f, fonts.regular)
        val amtPaint = makeTextPaint(Color.parseColor("#DB2777"), 9.5f, fonts.bold, Paint.Align.RIGHT)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 0.6f
        }
        val descMaxWidth = (CONTENT_WIDTH - 122f).toInt()

        data.advances.forEach { adv ->
            val date = DateUtils.parseDateKey(adv.dateKey)
            val dateStr = if (date != null) DateUtils.formatHindiDate(date) else adv.dateKey
            val reason = adv.reason.ifBlank { "एडवांस" }
            val lineText = "• $dateStr — $reason"

            val descLayout = buildStaticLayout(lineText, rowPaint, descMaxWidth)
            val rowHeight = maxOf(18f, descLayout.height + 6f)

            paginator.ensureSpace(rowHeight + 6f) {
                paginator.y = drawAdvancesHeading(paginator.canvas, paginator.y, data.advances.size, fonts, continued = true)
            }

            drawStaticLayout(paginator.canvas, descLayout, MARGIN_H + 8f, paginator.y + 3f)
            paginator.canvas.drawText(
                "-₹${DateUtils.formatInr(adv.amount)}",
                PAGE_WIDTH - MARGIN_H - 8f,
                paginator.y + 12f,
                amtPaint
            )
            paginator.y += rowHeight
            paginator.canvas.drawLine(MARGIN_H + 6f, paginator.y, PAGE_WIDTH - MARGIN_H - 6f, paginator.y, divider)
            paginator.y += 2f
        }
        paginator.y += 10f
    }

    private fun drawDailyLogSection(
        paginator: PdfPaginator,
        data: MonthlySlipData,
        fonts: FontSet
    ) {
        paginator.ensureSpace(72f)
        paginator.y = drawDailyLogHeader(paginator.canvas, paginator.y, fonts, continued = false)

        val textPaint = makeTextPaint(Color.parseColor("#334155"), 8.5f, fonts.regular)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 0.7f
        }

        val colDateX = MARGIN_H + 8f
        val colStatusX = MARGIN_H + 140f
        val colOtX = MARGIN_H + 245f
        val colNoteX = MARGIN_H + 332f
        val statusColWidth = 98
        val noteColWidth = (PAGE_WIDTH - MARGIN_H - colNoteX - 6f).toInt()

        for (row in data.dailyRows) {
            val statusColor = when (row.statusCode) {
                "P" -> Color.parseColor("#059669")
                "A" -> Color.parseColor("#E11D48")
                "HD" -> Color.parseColor("#D97706")
                "L" -> Color.parseColor("#0284C7")
                else -> Color.parseColor("#64748B")
            }
            val statusPaint = makeTextPaint(statusColor, 8.5f, fonts.bold)
            val statusText = if (row.statusLabel.isNotBlank()) "${row.statusCode} • ${row.statusLabel}" else "-"
            val noteText = row.note.ifBlank { "-" }

            val statusLayout = buildStaticLayout(statusText, statusPaint, statusColWidth)
            val noteLayout = buildStaticLayout(noteText, textPaint, noteColWidth)
            val contentH = maxOf(12f, maxOf(statusLayout.height.toFloat(), noteLayout.height.toFloat()))
            val rowHeight = maxOf(18f, contentH + 6f)

            paginator.ensureSpace(rowHeight + 4f) {
                paginator.y = drawDailyLogHeader(paginator.canvas, paginator.y, fonts, continued = true)
            }

            paginator.canvas.drawText("${row.dateFormatted} (${row.dayShort})", colDateX, paginator.y + 11f, textPaint)
            drawStaticLayout(paginator.canvas, statusLayout, colStatusX, paginator.y + 3f)
            val otText = if (row.otHours > 0f) "${row.otHours} घंटे" else "-"
            paginator.canvas.drawText(otText, colOtX, paginator.y + 11f, textPaint)
            drawStaticLayout(paginator.canvas, noteLayout, colNoteX, paginator.y + 3f)

            paginator.y += rowHeight
            paginator.canvas.drawLine(MARGIN_H, paginator.y, PAGE_WIDTH - MARGIN_H, paginator.y, divider)
            paginator.y += 2f
        }
    }

    private fun drawDailyLogHeader(canvas: Canvas, startY: Float, fonts: FontSet, continued: Boolean): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 11f, fonts.bold)
        val heading = if (continued) {
            "दैनिक हाजिरी एवं अधिक काम लॉग (जारी)"
        } else {
            "दैनिक हाजिरी एवं अधिक काम लॉग"
        }
        canvas.drawText(heading, MARGIN_H, startY, headingPaint)

        val top = startY + 8f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRoundRect(RectF(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + 20f), 4f, 4f, bg)

        val th = makeTextPaint(Color.parseColor("#334155"), 8.5f, fonts.bold)
        canvas.drawText("तारीख़", MARGIN_H + 8f, top + 13.5f, th)
        canvas.drawText("स्थिति", MARGIN_H + 140f, top + 13.5f, th)
        canvas.drawText("अधिक काम (OT)", MARGIN_H + 245f, top + 13.5f, th)
        canvas.drawText("नोट", MARGIN_H + 332f, top + 13.5f, th)

        return top + 24f
    }

    private fun drawSignatures(canvas: Canvas, topY: Float, fonts: FontSet) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 0.8f
        }
        val lblPaint = makeTextPaint(Color.parseColor("#64748B"), 9f, fonts.regular)
        canvas.drawLine(MARGIN_H + 10f, topY + 22f, MARGIN_H + 150f, topY + 22f, linePaint)
        canvas.drawText("कर्मचारी हस्ताक्षर", MARGIN_H + 35f, topY + 35f, lblPaint)

        canvas.drawLine(PAGE_WIDTH - MARGIN_H - 150f, topY + 22f, PAGE_WIDTH - MARGIN_H - 10f, topY + 22f, linePaint)
        canvas.drawText("नियोक्ता / ठेकेदार हस्ताक्षर", PAGE_WIDTH - MARGIN_H - 135f, topY + 35f, lblPaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, fonts: FontSet) {
        val footerLeft = makeTextPaint(Color.parseColor("#94A3B8"), 8.5f, fonts.regular)
        val footerRight = makeTextPaint(Color.parseColor("#94A3B8"), 8.5f, fonts.regular, Paint.Align.RIGHT)
        canvas.drawText(
            "हाजिरी ऐप द्वारा जनरेट किया गया डिजिटल वेतन बिल",
            MARGIN_H,
            PAGE_HEIGHT - 20f,
            footerLeft
        )
        canvas.drawText(
            "पेज $pageNumber",
            PAGE_WIDTH - MARGIN_H,
            PAGE_HEIGHT - 20f,
            footerRight
        )
    }

    internal fun formatFloatDays(days: Float): String {
        return if (days % 1f == 0f) days.toInt().toString() else String.format(Locale.US, "%.1f", days)
    }
}
