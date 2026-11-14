package com.example.hajiri.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.model.ExpenseCategory
import com.example.hajiri.model.ExpenseRecord
import com.example.hajiri.ui.components.ConfirmActionDialog
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.MonthYearPickerDialog
import com.example.hajiri.ui.components.outlinedFieldColors
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.OtOrangeBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.ExpenseReportPdfGenerator
import com.example.hajiri.viewmodel.HajiriViewModel
import com.example.hajiri.viewmodel.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalHajiriPalette.current

    val selectedMonth by viewModel.selectedExpenseMonth.collectAsState()
    val currentMonthKey by viewModel.todayMonthKeyState.collectAsState()
    val todayKey by viewModel.todayKeyState.collectAsState()
    val earliestYear by viewModel.earliestDataYear.collectAsState()
    val summary by viewModel.expenseMonthSummary.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showMonthPicker by remember { mutableStateOf(false) }
    var isAddExpanded by remember { mutableStateOf(true) }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var noteInput by remember { mutableStateOf("") }
    var selectedDate by remember(todayKey) {
        mutableStateOf(DateUtils.parseDateKey(todayKey) ?: LocalDate.now())
    }
    var showAddDatePicker by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
    var isPdfBusy by remember { mutableStateOf(false) }

    val nowYm = remember(currentMonthKey) {
        runCatching { YearMonth.parse(currentMonthKey) }.getOrNull() ?: YearMonth.now()
    }
    val selectedYm = remember(selectedMonth, nowYm) {
        runCatching { YearMonth.parse(selectedMonth) }.getOrNull() ?: nowYm
    }
    val canGoNextMonth = selectedYm.isBefore(nowYm)

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialMonthKey = selectedMonth,
            maxYearMonth = nowYm,
            minYear = earliestYear,
            onDismiss = { showMonthPicker = false },
            onMonthSelected = { pickedKey ->
                showMonthPicker = false
                viewModel.setExpenseMonth(pickedKey)
            }
        )
    }

    if (showAddDatePicker) {
        ExpenseDatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showAddDatePicker = false },
            onDateSelected = { picked ->
                selectedDate = picked
                showAddDatePicker = false
            }
        )
    }

    editingExpense?.let { target ->
        EditExpenseDialog(
            expense = target,
            onDismiss = { editingExpense = null },
            onSave = { amount, category, note, dateKey ->
                viewModel.updateExpense(
                    id = target.id,
                    amount = amount,
                    category = category,
                    note = note,
                    dateKey = dateKey
                )
                editingExpense = null
            }
        )
    }

    deletingExpense?.let { target ->
        val cat = ExpenseCategory.fromId(target.category)
        val label = target.note.ifBlank { cat.labelHindi }
        ConfirmActionDialog(
            title = "🗑️ खर्च मिटाएं?",
            message = "क्या आप ₹${DateUtils.formatInr(target.amount)} ($label) का रिकॉर्ड हटाना चाहते हैं?",
            onDismiss = { deletingExpense = null },
            onConfirm = {
                viewModel.deleteExpense(target.id)
                deletingExpense = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Month Selector Pill
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💸 खर्च",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = palette.glassCardHigh,
                            border = BorderStroke(1.dp, palette.glassBorderMid),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    viewModel.setExpenseMonth(selectedYm.minusMonths(1).toString())
                                }
                                .testTag("expense_prev_month_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("◀", fontSize = 11.sp, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.2.dp, palette.accent.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clickable { showMonthPicker = true }
                                .testTag("expense_month_picker_pill")
                        ) {
                            Text(
                                text = "📅 ${DateUtils.formatHindiMonthYear(selectedMonth)} ▼",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (canGoNextMonth) palette.glassCardHigh else palette.segmentedTrackBg.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, palette.glassBorderMid),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = canGoNextMonth) {
                                    viewModel.setExpenseMonth(selectedYm.plusMonths(1).toString())
                                }
                                .testTag("expense_next_month_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "▶",
                                    fontSize = 11.sp,
                                    color = if (canGoNextMonth) palette.textPrimary else palette.textMuted.copy(alpha = 0.45f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Live Sync Status Indicator
                val (syncBg, syncFg, syncIcon) = when (syncStatus) {
                    SyncStatus.OFFLINE_SAVED -> Triple(OtOrangeBg, OtOrange, "📴")
                    SyncStatus.SYNCING_PENDING -> Triple(palette.accent.copy(alpha = 0.12f), palette.accent, "🔄")
                    SyncStatus.SYNCED -> Triple(StatusGreenBg, StatusGreen, "☁️")
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = syncBg,
                    border = BorderStroke(1.dp, syncFg.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_sync_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = syncIcon, fontSize = 11.sp)
                        Text(
                            text = syncStatus.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = syncFg
                        )
                    }
                }
            }
        }

        // 2. Top Summary Card ("इस महीने का कुल खर्च")
        item {
            GlassCard(
                borderColor = AdvancePink.copy(alpha = 0.35f),
                modifier = Modifier.testTag("expense_summary_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "इस महीने का कुल खर्च",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AdvancePink.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${summary.entries.size} प्रविष्टियाँ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdvancePink,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "₹${DateUtils.formatInr(summary.totalExpense)}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdvancePink,
                        modifier = Modifier.testTag("expense_total_amount_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    val savingsColor = if (summary.netSavings >= 0.0) StatusGreen else StatusRed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "इस महीने की कमाई: ₹${DateUtils.formatInr(summary.monthlyIncome)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "बचत: ${ExpenseReportPdfGenerator.formatSignedInr(summary.netSavings)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = savingsColor,
                            modifier = Modifier.testTag("expense_savings_text")
                        )
                    }
                }
            }
        }

        // 3. Add Expense Section ("+ खर्च जोड़ें")
        item {
            GlassCard(modifier = Modifier.testTag("add_expense_card")) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAddExpanded = !isAddExpanded }
                            .testTag("toggle_add_expense_btn"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ खर्च जोड़ें",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isAddExpanded) "छुपाएं ▲" else "खोलें ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isAddExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { raw -> amountInput = sanitizeExpenseAmountInput(raw) },
                                label = { Text("राशि ₹") },
                                placeholder = { Text("जैसे: 150", color = palette.textMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_amount_input")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "श्रेणी चुनें",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textSecondary
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExpenseCategory.entries.forEach { cat ->
                                        val isSelected = cat == selectedCategory
                                        val catColor = Color(cat.colorHex)
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isSelected) catColor.copy(alpha = 0.18f) else palette.segmentedTrackBg,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) catColor else palette.glassBorderMid
                                            ),
                                            modifier = Modifier
                                                .clickable { selectedCategory = cat }
                                                .testTag("expense_cat_chip_${cat.id}")
                                        ) {
                                            Text(
                                                text = "${cat.emoji} ${cat.labelHindi}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) catColor else palette.textSecondary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("किस चीज़ पर खर्च हुआ? (वैकल्पिक)") },
                                placeholder = { Text("जैसे: दोपहर का खाना, बाइक पेट्रोल...", color = palette.textMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_note_input")
                            )

                            // Date Picker Row
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.accent.copy(alpha = 0.09f),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddDatePicker = true }
                                    .testTag("expense_date_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "📅 खर्च की तारीख़",
                                            fontSize = 11.sp,
                                            color = palette.textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val dayName = DateUtils.HINDI_DAYS[selectedDate.dayOfWeek.value - 1]
                                        Text(
                                            text = "${DateUtils.formatHindiDate(selectedDate)} • $dayName",
                                            fontSize = 14.sp,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "बदलें ›",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accent
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                                    if (amt <= 0.0) {
                                        viewModel.showMessage("⚠️ सही खर्च राशि डालें")
                                    } else {
                                        viewModel.addExpense(
                                            amount = amt,
                                            category = selectedCategory,
                                            note = noteInput,
                                            dateKey = DateUtils.toDateKey(selectedDate)
                                        )
                                        amountInput = ""
                                        noteInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_expense_btn")
                            ) {
                                Text(
                                    text = "खर्च सेव करें ✓",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Category-Wise Breakdown ("किस-किस पर कितना खर्च हुआ")
        item {
            GlassCard(modifier = Modifier.testTag("expense_category_breakdown_card")) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "किस-किस पर कितना खर्च हुआ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )

                    if (summary.totalExpense <= 0.0) {
                        Text(
                            text = "${summary.monthTitle} में अभी कोई खर्च दर्ज नहीं है।",
                            fontSize = 12.sp,
                            color = palette.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        summary.categoryBreakdowns.forEach { item ->
                            val catColor = Color(item.category.colorHex)
                            val pctInt = (item.percentage * 100f).toInt().coerceIn(0, 100)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.category.emoji} ${item.category.labelHindi}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "₹${DateUtils.formatInr(item.totalAmount)} ($pctInt%)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.totalAmount > 0.0) catColor else palette.textMuted
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = catColor,
                                    trackColor = palette.progressTrackBg
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Direct PDF Export & Share Button
        item {
            Button(
                onClick = {
                    isPdfBusy = true
                    val pdfData = viewModel.buildExpenseReportData(selectedMonth)
                    scope.launch {
                        val uriResult = withContext(Dispatchers.IO) {
                            runCatching {
                                ExpenseReportPdfGenerator.createShareablePdfUri(context, pdfData)
                            }
                        }
                        isPdfBusy = false
                        uriResult.onSuccess { pdfUri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, pdfUri)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "हाजिरी खर्च रिपोर्ट - ${pdfData.summary.monthTitle}"
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${pdfData.userName} (${pdfData.summary.monthTitle}) की मासिक खर्च रिपोर्ट PDF — कुल खर्च: ₹${DateUtils.formatInr(pdfData.summary.totalExpense)}, बचत: ${ExpenseReportPdfGenerator.formatSignedInr(pdfData.summary.netSavings)}"
                                )
                                clipData = ClipData.newRawUri("Expense Report PDF", pdfUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "खर्च रिपोर्ट PDF शेयर करें")
                            )
                        }.onFailure {
                            viewModel.showMessage("⚠️ खर्च PDF बनाने में समस्या आई")
                        }
                    }
                },
                enabled = !isPdfBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x3325D366))
                    .testTag("export_expense_pdf_btn")
            ) {
                Text(
                    text = if (isPdfBusy) "⏳ खर्च PDF तैयार हो रहा है..." else "📄 खर्च रिपोर्ट PDF बनाएं और शेयर करें",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }

        // 6. Date-Wise Expense History ("खर्च का विवरण")
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "खर्च का विवरण",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Text(
                    text = "${summary.monthTitle} (${summary.entries.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textMuted
                )
            }
        }

        if (summary.entries.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "${summary.monthTitle} में कोई खर्च नहीं मिला। ऊपर '+ खर्च जोड़ें' से नया खर्च लिखें।",
                        color = palette.textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
        } else {
            items(summary.entries, key = { it.id.ifBlank { "${it.dateKey}_${it.amount}_${it.note}" } }) { expense ->
                ExpenseHistoryRow(
                    expense = expense,
                    onEdit = { editingExpense = expense },
                    onDelete = { deletingExpense = expense }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(92.dp))
        }
    }
}

@Composable
private fun ExpenseHistoryRow(
    expense: ExpenseRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val category = ExpenseCategory.fromId(expense.category)
    val catColor = Color(category.colorHex)
    val parsedDate = DateUtils.parseDateKey(expense.dateKey)
    val dateText = if (parsedDate != null) {
        val dayName = DateUtils.HINDI_DAYS[parsedDate.dayOfWeek.value - 1]
        "${DateUtils.formatHindiDate(parsedDate)} • $dayName"
    } else {
        expense.dateKey
    }

    GlassCard(
        modifier = Modifier.testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(catColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, catColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note.ifBlank { category.labelHindi },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (expense.note.isNotBlank()) {
                    Text(
                        text = category.labelHindi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = catColor
                    )
                }
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = palette.textMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${DateUtils.formatInr(expense.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdvancePink
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_expense_${expense.id}")
                    ) {
                        Text(text = "✏️", fontSize = 15.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_expense_${expense.id}")
                    ) {
                        Text(text = "🗑️", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerModal(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val todayEndMillis = LocalDate.now().atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayEndMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = datePickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    } ?: initialDate
                    onDateSelected(picked)
                },
                modifier = Modifier.testTag("expense_datepicker_confirm_btn")
            ) {
                Text("चुनें", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditExpenseDialog(
    expense: ExpenseRecord,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: ExpenseCategory, note: String, dateKey: String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var amountText by remember(expense.id) {
        val rounded = if (expense.amount % 1.0 == 0.0) expense.amount.toLong().toString() else expense.amount.toString()
        mutableStateOf(rounded)
    }
    var selectedCategory by remember(expense.id) {
        mutableStateOf(ExpenseCategory.fromId(expense.category))
    }
    var noteText by remember(expense.id) { mutableStateOf(expense.note) }
    var selectedDate by remember(expense.id) {
        mutableStateOf(DateUtils.parseDateKey(expense.dateKey) ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        ExpenseDatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { picked ->
                selectedDate = picked
                showDatePicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.glassCardHigh),
            border = BorderStroke(1.2.dp, palette.glassBorderTop),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✏️ खर्च सुधारें",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = sanitizeExpenseAmountInput(it) },
                    label = { Text("राशि ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_expense_amount_input")
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val catColor = Color(cat.colorHex)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) catColor.copy(alpha = 0.18f) else palette.segmentedTrackBg,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) catColor else palette.glassBorderMid
                            ),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("edit_expense_cat_${cat.id}")
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.labelHindi}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) catColor else palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("किस चीज़ पर खर्च हुआ? (वैकल्पिक)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_expense_note_input")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accent.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("edit_expense_date_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${DateUtils.formatHindiDate(selectedDate)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "बदलें ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.fieldUnfocusedBorder)
                    ) {
                        Text("रद्द करें", color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSave(amt, selectedCategory, noteText, DateUtils.toDateKey(selectedDate))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("confirm_edit_expense_btn")
                    ) {
                        Text("अपडेट करें ✓", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

internal fun sanitizeExpenseAmountInput(raw: String): String {
    var hasDot = false
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !hasDot) {
            hasDot = true
            sb.append(ch)
        }
    }
    return sb.toString()
}
