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
import com.example.hajiri.model.CategoryExpenseBreakdown
import com.example.hajiri.model.ExpenseCategory
import com.example.hajiri.model.ExpenseMonthSummary
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

data class ExpenseReportPdfData(
    val userName: String,
    val userPhone: String,
    val userEmail: String,
    val summary: ExpenseMonthSummary
)

object ExpenseReportPdfGenerator {

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

    internal interface ExpensePageSink {
        fun startNewPage(pageNumber: Int, width: Int, height: Int): Canvas
        fun finishCurrentPage()
    }

    private class AndroidPdfPageSink(
        private val document: PdfDocument
    ) : ExpensePageSink {
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
        private val sink: ExpensePageSink,
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

        private fun startNextPage() {
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

    fun createShareablePdfUri(context: Context, data: ExpenseReportPdfData): Uri {
        val slipsDir = File(context.cacheDir, "slips").apply {
            if (!exists()) mkdirs()
        }
        slipsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 3_600_000L) {
                oldFile.delete()
            }
        }
        val safeMonth = data.summary.monthKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(slipsDir, "Hajiri_Expense_Report_$safeMonth.pdf")
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
        data: ExpenseReportPdfData,
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
        data: ExpenseReportPdfData,
        sink: ExpensePageSink,
        context: Context? = null
    ): Int {
        val fonts = resolveFonts(context)
        val paginator = PdfPaginator(sink, fonts)

        paginator.y = drawHeader(paginator.canvas, data, fonts)
        paginator.y = drawUserSection(paginator.canvas, data, paginator.y, fonts)
        paginator.y = drawSummaryCards(paginator.canvas, data.summary, paginator.y, fonts)

        drawCategoryBreakdownTable(paginator, data.summary.categoryBreakdowns, fonts)
        drawExpenseEntriesTable(paginator, data.summary, fonts)

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

    private fun drawHeader(canvas: Canvas, data: ExpenseReportPdfData, fonts: FontSet): Float {
        val headerHeight = 96f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#831843")
        }
        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight), headerPaint)

        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EC4899")
        }
        canvas.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight + 2f, accentBar)

        val titlePaint = makeTextPaint(Color.WHITE, 20f, fonts.bold)
        canvas.drawText("हाजिरी  •  मासिक खर्च रिपोर्ट", MARGIN_H, 38f, titlePaint)

        val subtitlePaint = makeTextPaint(Color.parseColor("#FCE7F3"), 11.5f, fonts.regular)
        canvas.drawText("माह: ${data.summary.monthTitle}", MARGIN_H, 60f, subtitlePaint)

        val datePaint = makeTextPaint(Color.parseColor("#FBCFE8"), 9.5f, fonts.regular, Paint.Align.RIGHT)
        val generatedOn = DateUtils.formatHindiDate(LocalDate.now())
        canvas.drawText("रिपोर्ट तिथि: $generatedOn", PAGE_WIDTH - MARGIN_H, headerHeight - 16f, datePaint)

        return headerHeight + 18f
    }

    private fun drawUserSection(
        canvas: Canvas,
        data: ExpenseReportPdfData,
        startY: Float,
        fonts: FontSet
    ): Float {
        val labelPaint = makeTextPaint(Color.parseColor("#64748B"), 9f, fonts.regular)
        val valuePaint = makeTextPaint(Color.parseColor("#0F172A"), 10.5f, fonts.bold)
        val colWidth = ((CONTENT_WIDTH / 2f) - 24f).toInt()

        val name = data.userName.ifBlank { "हाजिरी यूजर" }
        val contact = listOfNotNull(
            data.userPhone.takeIf { it.isNotBlank() },
            data.userEmail.takeIf { it.isNotBlank() }
        ).joinToString(" • ").ifBlank { "-" }

        val nameLayout = buildStaticLayout(name, valuePaint, colWidth)
        val contactLayout = buildStaticLayout(contact, valuePaint, colWidth)
        val boxHeight = maxOf(58f, maxOf(nameLayout.height, contactLayout.height) + 32f)

        val rect = RectF(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + boxHeight)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val leftX = MARGIN_H + 14f
        val rightX = PAGE_WIDTH / 2f + 10f
        canvas.drawText("नाम", leftX, startY + 18f, labelPaint)
        drawStaticLayout(canvas, nameLayout, leftX, startY + 23f)

        canvas.drawText("मोबाइल / संपर्क", rightX, startY + 18f, labelPaint)
        drawStaticLayout(canvas, contactLayout, rightX, startY + 23f)

        return startY + boxHeight + 16f
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        summary: ExpenseMonthSummary,
        startY: Float,
        fonts: FontSet
    ): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 12f, fonts.bold)
        canvas.drawText("मासिक आय, खर्च व बचत सारांश", MARGIN_H, startY, headingPaint)

        val top = startY + 10f
        val boxHeight = 62f
        val gap = 10f
        val cardWidth = (CONTENT_WIDTH - (gap * 2f)) / 3f

        val savingsFormatted = formatSignedInr(summary.netSavings)
        val savingsColor = if (summary.netSavings >= 0.0) "#059669" else "#E11D48"
        val savingsBg = if (summary.netSavings >= 0.0) "#ECFDF5" else "#FFF1F2"

        val cards = listOf(
            Triple("इस महीने की कमाई", "₹${DateUtils.formatInr(summary.monthlyIncome)}", Pair("#0284C7", "#F0F9FF")),
            Triple("इस महीने का कुल खर्च", "₹${DateUtils.formatInr(summary.totalExpense)}", Pair("#DB2777", "#FDF2F8")),
            Triple("शुद्ध बचत", savingsFormatted, Pair(savingsColor, savingsBg))
        )

        cards.forEachIndexed { index, (label, amount, colors) ->
            val x = MARGIN_H + index * (cardWidth + gap)
            val rect = RectF(x, top, x + cardWidth, top + boxHeight)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(colors.second) }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(colors.first)
                alpha = 90
                style = Paint.Style.STROKE
                strokeWidth = 1.1f
            }
            canvas.drawRoundRect(rect, 8f, 8f, fill)
            canvas.drawRoundRect(rect, 8f, 8f, stroke)

            val lblPaint = makeTextPaint(Color.parseColor("#475569"), 9.5f, fonts.regular, Paint.Align.CENTER)
            val valPaint = makeTextPaint(Color.parseColor(colors.first), 15f, fonts.bold, Paint.Align.CENTER)
            canvas.drawText(label, x + cardWidth / 2f, top + 21f, lblPaint)
            canvas.drawText(amount, x + cardWidth / 2f, top + 45f, valPaint)
        }

        return top + boxHeight + 20f
    }

    private fun drawCategoryHeader(canvas: Canvas, startY: Float, fonts: FontSet, continued: Boolean): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 12f, fonts.bold)
        val heading = if (continued) "श्रेणीवार खर्च विवरण (जारी)" else "श्रेणीवार खर्च विवरण (Category Summary)"
        canvas.drawText(heading, MARGIN_H, startY, headingPaint)

        val y = startY + 10f
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FCE7F3") }
        canvas.drawRoundRect(RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 24f), 5f, 5f, headerBg)

        val thLeft = makeTextPaint(Color.parseColor("#831843"), 9.5f, fonts.bold)
        val thMid = makeTextPaint(Color.parseColor("#831843"), 9.5f, fonts.bold, Paint.Align.RIGHT)
        val thRight = makeTextPaint(Color.parseColor("#831843"), 9.5f, fonts.bold, Paint.Align.RIGHT)
        canvas.drawText("श्रेणी (Category)", MARGIN_H + 10f, y + 15.5f, thLeft)
        canvas.drawText("प्रविष्टियाँ / %", PAGE_WIDTH - MARGIN_H - 125f, y + 15.5f, thMid)
        canvas.drawText("राशि (Amount)", PAGE_WIDTH - MARGIN_H - 10f, y + 15.5f, thRight)
        return y + 28f
    }

    private fun drawCategoryBreakdownTable(
        paginator: PdfPaginator,
        breakdowns: List<CategoryExpenseBreakdown>,
        fonts: FontSet
    ) {
        paginator.ensureSpace(70f)
        paginator.y = drawCategoryHeader(paginator.canvas, paginator.y, fonts, continued = false)

        val labelPaint = makeTextPaint(Color.parseColor("#1E293B"), 10f, fonts.bold)
        val subPaint = makeTextPaint(Color.parseColor("#64748B"), 9.5f, fonts.regular, Paint.Align.RIGHT)
        val amtPaint = makeTextPaint(Color.parseColor("#0F172A"), 10.5f, fonts.bold, Paint.Align.RIGHT)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.7f
        }

        breakdowns.forEach { item ->
            val rowHeight = 24f
            paginator.ensureSpace(rowHeight + 4f) {
                paginator.y = drawCategoryHeader(paginator.canvas, paginator.y, fonts, continued = true)
            }
            val pct = (item.percentage * 100f).toInt().coerceIn(0, 100)
            paginator.canvas.drawText("${item.category.emoji} ${item.category.labelHindi}", MARGIN_H + 10f, paginator.y + 15f, labelPaint)
            paginator.canvas.drawText("${item.count} बार ($pct%)", PAGE_WIDTH - MARGIN_H - 125f, paginator.y + 15f, subPaint)
            paginator.canvas.drawText("₹${DateUtils.formatInr(item.totalAmount)}", PAGE_WIDTH - MARGIN_H - 10f, paginator.y + 15f, amtPaint)
            paginator.y += rowHeight
            paginator.canvas.drawLine(MARGIN_H, paginator.y, PAGE_WIDTH - MARGIN_H, paginator.y, linePaint)
        }
        paginator.y += 16f
    }

    private fun drawExpenseListHeader(canvas: Canvas, startY: Float, totalCount: Int, fonts: FontSet, continued: Boolean): Float {
        val headingPaint = makeTextPaint(Color.parseColor("#1E293B"), 12f, fonts.bold)
        val title = if (continued) {
            "तारीख़वार खर्च विवरण ($totalCount) — जारी"
        } else {
            "तारीख़वार खर्च विवरण ($totalCount प्रविष्टियाँ)"
        }
        canvas.drawText(title, MARGIN_H, startY, headingPaint)

        val top = startY + 10f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRoundRect(RectF(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + 22f), 4f, 4f, bg)

        val th = makeTextPaint(Color.parseColor("#334155"), 8.8f, fonts.bold)
        val thRight = makeTextPaint(Color.parseColor("#334155"), 8.8f, fonts.bold, Paint.Align.RIGHT)
        canvas.drawText("तारीख़ व दिन", MARGIN_H + 8f, top + 14.5f, th)
        canvas.drawText("श्रेणी", MARGIN_H + 145f, top + 14.5f, th)
        canvas.drawText("विवरण / नोट", MARGIN_H + 255f, top + 14.5f, th)
        canvas.drawText("राशि", PAGE_WIDTH - MARGIN_H - 8f, top + 14.5f, thRight)
        return top + 26f
    }

    private fun drawExpenseEntriesTable(
        paginator: PdfPaginator,
        summary: ExpenseMonthSummary,
        fonts: FontSet
    ) {
        val entries = summary.entries
        paginator.ensureSpace(64f)
        paginator.y = drawExpenseListHeader(paginator.canvas, paginator.y, entries.size, fonts, continued = false)

        if (entries.isEmpty()) {
            val emptyPaint = makeTextPaint(Color.parseColor("#64748B"), 10f, fonts.regular)
            paginator.canvas.drawText("इस महीने कोई खर्च दर्ज नहीं है।", MARGIN_H + 8f, paginator.y + 16f, emptyPaint)
            paginator.y += 28f
            return
        }

        val textPaint = makeTextPaint(Color.parseColor("#334155"), 9f, fonts.regular)
        val catPaint = makeTextPaint(Color.parseColor("#475569"), 9f, fonts.bold)
        val amtPaint = makeTextPaint(Color.parseColor("#DB2777"), 9.8f, fonts.bold, Paint.Align.RIGHT)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 0.7f
        }

        val colDateX = MARGIN_H + 8f
        val colCatX = MARGIN_H + 145f
        val colNoteX = MARGIN_H + 255f
        val dateColW = 132
        val catColW = 104
        val noteColW = (PAGE_WIDTH - MARGIN_H - colNoteX - 78f).toInt()

        entries.forEach { expense ->
            val category = ExpenseCategory.fromId(expense.category)
            val parsedDate = DateUtils.parseDateKey(expense.dateKey)
            val dateLabel = if (parsedDate != null) {
                val dayName = DateUtils.HINDI_DAYS[parsedDate.dayOfWeek.value - 1]
                "${DateUtils.formatHindiDate(parsedDate)} • $dayName"
            } else {
                expense.dateKey
            }
            val catLabel = "${category.emoji} ${category.labelHindi}"
            val noteText = expense.note.ifBlank { category.labelHindi }

            val dateLayout = buildStaticLayout(dateLabel, textPaint, dateColW)
            val catLayout = buildStaticLayout(catLabel, catPaint, catColW)
            val noteLayout = buildStaticLayout(noteText, textPaint, noteColW)

            val contentH = maxOf(dateLayout.height, maxOf(catLayout.height, noteLayout.height)).toFloat()
            val rowHeight = maxOf(20f, contentH + 8f)

            paginator.ensureSpace(rowHeight + 4f) {
                paginator.y = drawExpenseListHeader(paginator.canvas, paginator.y, entries.size, fonts, continued = true)
            }

            drawStaticLayout(paginator.canvas, dateLayout, colDateX, paginator.y + 4f)
            drawStaticLayout(paginator.canvas, catLayout, colCatX, paginator.y + 4f)
            drawStaticLayout(paginator.canvas, noteLayout, colNoteX, paginator.y + 4f)
            paginator.canvas.drawText(
                "₹${DateUtils.formatInr(expense.amount)}",
                PAGE_WIDTH - MARGIN_H - 8f,
                paginator.y + 14f,
                amtPaint
            )
            paginator.y += rowHeight
            paginator.canvas.drawLine(MARGIN_H, paginator.y, PAGE_WIDTH - MARGIN_H, paginator.y, divider)
            paginator.y += 2f
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, fonts: FontSet) {
        val footerLeft = makeTextPaint(Color.parseColor("#94A3B8"), 8.5f, fonts.regular)
        val footerRight = makeTextPaint(Color.parseColor("#94A3B8"), 8.5f, fonts.regular, Paint.Align.RIGHT)
        canvas.drawText(
            "हाजिरी ऐप द्वारा जनरेट की गई मासिक खर्च रिपोर्ट",
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

    internal fun formatSignedInr(amount: Double): String {
        return if (amount < -0.5) {
            "-₹${DateUtils.formatInr(abs(amount))}"
        } else {
            "₹${DateUtils.formatInr(maxOf(0.0, amount))}"
        }
    }
}
