package com.example.hajiri.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.model.PersonLedgerSummary
import com.example.hajiri.model.TransactionRecord
import com.example.hajiri.model.TransactionType
import com.example.hajiri.ui.components.ConfirmActionDialog
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.LiquidGlassButton
import com.example.hajiri.ui.components.LiquidGlassDialogCard
import com.example.hajiri.ui.components.LiquidGlassOutlinedButton
import com.example.hajiri.ui.components.outlinedFieldColors
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.OtOrangeBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.util.DateUtils
import com.example.hajiri.viewmodel.HajiriViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

private enum class TransactionFilter(val label: String) {
    ALL("सभी"),
    GIVEN("दिए"),
    RECEIVED("लिए")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalHajiriPalette.current
    val todayKey by viewModel.todayKeyState.collectAsState()
    val overview by viewModel.transactionsOverview.collectAsState()

    var isAddExpanded by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf(TransactionType.GIVEN) }
    var personNameInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedDate by remember(todayKey) {
        mutableStateOf(DateUtils.parseDateKey(todayKey) ?: LocalDate.now())
    }
    var showAddDatePicker by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedPersonKey by remember { mutableStateOf<String?>(null) }

    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

    val filteredEntries = remember(overview.entries, selectedFilter, selectedPersonKey) {
        overview.entries.filter { tx ->
            val matchesType = when (selectedFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.GIVEN -> TransactionType.fromId(tx.type) == TransactionType.GIVEN
                TransactionFilter.RECEIVED -> TransactionType.fromId(tx.type) == TransactionType.RECEIVED
            }
            val matchesPerson = selectedPersonKey == null ||
                tx.personName.trim().lowercase() == selectedPersonKey
            matchesType && matchesPerson
        }
    }

    val activePersonSummary = remember(overview.personSummaries, selectedPersonKey) {
        if (selectedPersonKey == null) null
        else overview.personSummaries.firstOrNull { it.personKey == selectedPersonKey }
    }

    if (showAddDatePicker) {
        TransactionDatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showAddDatePicker = false },
            onDateSelected = { picked ->
                selectedDate = picked
                showAddDatePicker = false
            }
        )
    }

    editingTransaction?.let { target ->
        EditTransactionDialog(
            transaction = target,
            onDismiss = { editingTransaction = null },
            onSave = { name, amount, type, dateKey, note ->
                viewModel.updateTransaction(
                    id = target.id,
                    personName = name,
                    amount = amount,
                    type = type,
                    dateKey = dateKey,
                    note = note
                )
                editingTransaction = null
            }
        )
    }

    deletingTransaction?.let { target ->
        val actionText = if (TransactionType.fromId(target.type) == TransactionType.GIVEN) "दिए" else "लिए"
        ConfirmActionDialog(
            title = "लेनदेन हटाएं?",
            message = "${target.personName} • ₹${DateUtils.formatInr(target.amount)} ($actionText) को हटाना चाहते हैं?",
            confirmText = "हटाएं",
            onDismiss = { deletingTransaction = null },
            onConfirm = {
                viewModel.deleteTransaction(target.id)
                deletingTransaction = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Clean Header (No Cloud Sync badge per requirement)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🤝 ", fontSize = 24.sp)
                    Text(
                        text = "लेनदेन",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                }

                if (overview.personSummaries.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${overview.personSummaries.size} व्यक्ति",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 2. Total Balance Summary Card (कुल बकाया + दिए / लिए)
        item {
            val net = overview.netBalance
            val (netColor, netBadgeBg, netStatusLabel) = when {
                net > 0.01 -> Triple(palette.statusGreen, palette.statusGreenBg, "लेना बाकी")
                net < -0.01 -> Triple(palette.expenseRed, palette.statusRedBg, "देना बाकी")
                else -> Triple(palette.textSecondary, palette.segmentedTrackBg, "हिसाब बराबर ✓")
            }

            GlassCard(
                borderColor = netColor.copy(alpha = 0.35f),
                modifier = Modifier.testTag("transactions_summary_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "कुल बकाया हिसाब",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = netBadgeBg,
                            border = BorderStroke(1.dp, netColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = netStatusLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = netColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "₹${DateUtils.formatInr(abs(net))}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor,
                        modifier = Modifier.testTag("transactions_net_balance_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (palette.isOled) palette.otAmberBg else OtOrangeBg.copy(alpha = if (palette.isDark) 0.16f else 0.72f),
                            border = BorderStroke(1.dp, palette.otAmber.copy(alpha = 0.28f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "↗️ कुल दिए / उधार दिए",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "₹${DateUtils.formatInr(overview.totalGiven)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.otAmber
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (palette.isOled) palette.statusGreenBg else StatusGreenBg.copy(alpha = if (palette.isDark) 0.16f else 0.72f),
                            border = BorderStroke(1.dp, palette.statusGreen.copy(alpha = 0.28f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "↙️ कुल लिए / जमा किए",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "₹${DateUtils.formatInr(overview.totalReceived)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.statusGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Add Transaction Card (दिए / लिए)
        item {
            GlassCard(modifier = Modifier.testTag("add_transaction_card")) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAddExpanded = !isAddExpanded }
                            .testTag("toggle_add_transaction_btn"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "➕ नया लेनदेन जोड़ें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isAddExpanded) "बंद करें ▲" else "खोलें ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isAddExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Given / Received Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.segmentedTrackBg, RoundedCornerShape(14.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TransactionType.entries.forEach { type ->
                                    val isSelected = selectedType == type
                                    val activeColor = if (type == TransactionType.GIVEN) palette.otAmber else palette.statusGreen
                                    val prefix = if (type == TransactionType.GIVEN) "↗️ " else "↙️ "
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = if (isSelected) activeColor else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedType = type }
                                            .testTag("tx_type_${type.id}_btn")
                                    ) {
                                        Text(
                                            text = "$prefix${type.labelHindi}",
                                            color = if (isSelected) Color.White else palette.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Person Name Input
                            OutlinedTextField(
                                value = personNameInput,
                                onValueChange = { personNameInput = it },
                                label = { Text("व्यक्ति का नाम *", fontSize = 12.sp) },
                                placeholder = {
                                    Text("जैसे: रमेश भाई, सुरेश जी", fontSize = 12.sp, color = palette.textMuted)
                                },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tx_person_name_input")
                            )

                            // Quick-fill chips for existing people
                            if (overview.personSummaries.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    overview.personSummaries.take(8).forEach { person ->
                                        val isMatched = personNameInput.trim().equals(person.displayName, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isMatched) palette.accent.copy(alpha = 0.20f) else palette.glassCardHigh.copy(alpha = 0.72f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isMatched) palette.accent else palette.glassBorderMid
                                            ),
                                            modifier = Modifier.clickable {
                                                personNameInput = person.displayName
                                            }
                                        ) {
                                            Text(
                                                text = "👤 ${person.displayName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isMatched) palette.accent else palette.textSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Amount + Date row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { raw ->
                                        val filtered = raw.filter { it.isDigit() || it == '.' }
                                        if (filtered.count { it == '.' } <= 1) {
                                            amountInput = filtered
                                        }
                                    },
                                    label = { Text("राशि (₹) *", fontSize = 12.sp) },
                                    placeholder = { Text("0", fontSize = 13.sp, color = palette.textMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = outlinedFieldColors(),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("tx_amount_input")
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.glassCardHigh.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .height(56.dp)
                                        .clickable { showAddDatePicker = true }
                                        .testTag("tx_date_picker_btn")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "📅 तारीख",
                                            fontSize = 10.sp,
                                            color = palette.textMuted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${selectedDate.dayOfMonth} ${DateUtils.HINDI_MONTHS[selectedDate.monthValue - 1].take(4)} ${selectedDate.year}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accent,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Notes Input
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("नोट्स / विवरण (वैकल्पिक)", fontSize = 12.sp) },
                                placeholder = {
                                    Text("जैसे: उधार दिए, नकद वापस मिले, UPI", fontSize = 12.sp, color = palette.textMuted)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tx_note_input")
                            )

                            val buttonPrimary = if (selectedType == TransactionType.GIVEN) palette.otAmber else palette.statusGreen
                            val buttonSecondary = if (selectedType == TransactionType.GIVEN) Color(0xFFC2410C) else Color(0xFF065F46)

                            LiquidGlassButton(
                                onClick = {
                                    val amt = amountInput.trim().toDoubleOrNull() ?: 0.0
                                    if (personNameInput.trim().isBlank()) {
                                        viewModel.showMessage("⚠️ कृपया व्यक्ति का नाम लिखें")
                                        return@LiquidGlassButton
                                    }
                                    if (amt <= 0.0) {
                                        viewModel.showMessage("⚠️ सही राशि डालें")
                                        return@LiquidGlassButton
                                    }
                                    viewModel.addTransaction(
                                        personName = personNameInput.trim(),
                                        amount = amt,
                                        type = selectedType,
                                        dateKey = DateUtils.toDateKey(selectedDate),
                                        note = noteInput.trim()
                                    )
                                    amountInput = ""
                                    noteInput = ""
                                },
                                containerColor = buttonPrimary,
                                secondaryColor = buttonSecondary,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_transaction_btn")
                            ) {
                                val label = if (selectedType == TransactionType.GIVEN) {
                                    "💾 दिए / उधार सेव करें"
                                } else {
                                    "💾 लिए / जमा सेव करें"
                                }
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Person-Wise Balance Summary ("व्यक्ति-वार बकाया")
        if (overview.personSummaries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "व्यक्ति-वार हिसाब",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    if (selectedPersonKey != null) {
                        Text(
                            text = "✕ फ़िल्टर हटाएं",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier
                                .clickable { selectedPersonKey = null }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("clear_person_filter_btn")
                        )
                    }
                }
            }

            items(overview.personSummaries.size) { index ->
                val person = overview.personSummaries[index]
                val isSelected = selectedPersonKey == person.personKey
                PersonBalanceCard(
                    summary = person,
                    isSelected = isSelected,
                    onClick = {
                        selectedPersonKey = if (isSelected) null else person.personKey
                        if (!isSelected) {
                            personNameInput = person.displayName
                        }
                    }
                )
            }
        }

        // 5. Transaction History Header + Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val heading = if (activePersonSummary != null) {
                        "${activePersonSummary.displayName} का लेनदेन"
                    } else {
                        "लेनदेन का विवरण"
                    }
                    Text(
                        text = heading,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${filteredEntries.size} प्रविष्टियां",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.segmentedTrackBg, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionFilter.entries.forEach { filter ->
                        val active = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) palette.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = filter }
                                .testTag("tx_filter_${filter.name.lowercase()}_btn")
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (active) Color.White else palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Transaction Entries List
        if (filteredEntries.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🤝", fontSize = 30.sp)
                        Text(
                            text = "अभी कोई लेनदेन दर्ज नहीं है",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "ऊपर नाम और राशि डालकर किसी को दिए या लिए पैसों का हिसाब रखें",
                            fontSize = 12.sp,
                            color = palette.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredEntries.size) { index ->
                val tx = filteredEntries[index]
                TransactionEntryCard(
                    transaction = tx,
                    onEdit = { editingTransaction = tx },
                    onDelete = { deletingTransaction = tx }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(95.dp))
        }
    }
}

@Composable
private fun PersonBalanceCard(
    summary: PersonLedgerSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val (balanceColor, balanceBg, statusText) = when {
        summary.netBalance > 0.01 -> Triple(palette.statusGreen, palette.statusGreenBg, "लेना बाकी")
        summary.netBalance < -0.01 -> Triple(palette.expenseRed, palette.statusRedBg, "देना बाकी")
        else -> Triple(palette.textMuted, palette.segmentedTrackBg, "बराबर ✓")
    }

    GlassCard(
        borderColor = if (isSelected) palette.accent else balanceColor.copy(alpha = 0.28f),
        contentPadding = 14.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("person_summary_card_${summary.personKey}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = balanceColor.copy(alpha = 0.16f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.displayName.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = balanceColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "दिए: ₹${DateUtils.formatInr(summary.totalGiven)} • लिए: ₹${DateUtils.formatInr(summary.totalReceived)}",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${DateUtils.formatInr(abs(summary.netBalance))}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = balanceColor
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = balanceBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionEntryCard(
    transaction: TransactionRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val txType = TransactionType.fromId(transaction.type)
    val isGiven = txType == TransactionType.GIVEN
    val accentColor = if (isGiven) palette.otAmber else palette.statusGreen
    val pillBg = if (isGiven) palette.otAmberBg else palette.statusGreenBg
    val parsedDate = DateUtils.parseDateKey(transaction.dateKey)
    val dateLabel = if (parsedDate != null) {
        "${parsedDate.dayOfMonth} ${DateUtils.HINDI_MONTHS[parsedDate.monthValue - 1]} ${parsedDate.year}"
    } else {
        transaction.dateKey
    }

    GlassCard(
        borderColor = accentColor.copy(alpha = 0.25f),
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = transaction.personName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillBg
                    ) {
                        Text(
                            text = if (isGiven) "↗️ दिए" else "↙️ लिए",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "📅 $dateLabel",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ${transaction.note}",
                        fontSize = 11.sp,
                        color = palette.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isGiven) "-" else "+"}₹${DateUtils.formatInr(transaction.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(text = "✏️", fontSize = 15.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
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
private fun EditTransactionDialog(
    transaction: TransactionRecord,
    onDismiss: () -> Unit,
    onSave: (personName: String, amount: Double, type: TransactionType, dateKey: String, note: String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var personName by remember(transaction.id) { mutableStateOf(transaction.personName) }
    var amountText by remember(transaction.id) {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString()
            else transaction.amount.toString()
        )
    }
    var selectedType by remember(transaction.id) { mutableStateOf(TransactionType.fromId(transaction.type)) }
    var noteText by remember(transaction.id) { mutableStateOf(transaction.note) }
    var selectedDate by remember(transaction.id) {
        mutableStateOf(DateUtils.parseDateKey(transaction.dateKey) ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        TransactionDatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { picked ->
                selectedDate = picked
                showDatePicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassDialogCard(contentPadding = 22.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "✏️ लेनदेन बदलें",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.segmentedTrackBg, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        val active = selectedType == type
                        val activeColor = if (type == TransactionType.GIVEN) OtOrange else StatusGreen
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) activeColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = type.labelHindi,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (active) Color.White else palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("व्यक्ति का नाम *", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) amountText = filtered
                        },
                        label = { Text("राशि (₹)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.glassCardHigh,
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable { showDatePicker = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "📅 ${selectedDate.dayOfMonth} ${DateUtils.HINDI_MONTHS[selectedDate.monthValue - 1].take(4)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("नोट्स / विवरण", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassOutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करें", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                    LiquidGlassButton(
                        onClick = {
                            val amt = amountText.trim().toDoubleOrNull() ?: 0.0
                            if (personName.trim().isNotBlank() && amt > 0.0) {
                                onSave(
                                    personName.trim(),
                                    amt,
                                    selectedType,
                                    DateUtils.toDateKey(selectedDate),
                                    noteText.trim()
                                )
                            }
                        },
                        containerColor = palette.accent,
                        secondaryColor = palette.accentSecondary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("सेव करें", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerModal(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayUtcMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(picked)
                    } else {
                        onDismiss()
                    }
                }
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
