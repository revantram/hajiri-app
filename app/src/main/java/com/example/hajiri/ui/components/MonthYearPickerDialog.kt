package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.util.DateUtils
import java.time.YearMonth

@Composable
fun MonthSelectorWithPickerRow(
    selectedMonth: String,
    currentMonthKey: String,
    earliestYear: Int,
    testTagPrefix: String,
    onSelectMonth: (String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var showPickerDialog by remember { mutableStateOf(false) }

    val nowYm = remember(currentMonthKey) {
        runCatching { YearMonth.parse(currentMonthKey) }.getOrNull() ?: YearMonth.now()
    }
    val recentSixKeys = remember(nowYm) {
        (5 downTo 0).map { offset -> nowYm.minusMonths(offset.toLong()) }
    }
    val isOlderMonthSelected = remember(selectedMonth, recentSixKeys) {
        recentSixKeys.none { it.toString() == selectedMonth }
    }

    if (showPickerDialog) {
        MonthYearPickerDialog(
            initialMonthKey = selectedMonth,
            maxYearMonth = nowYm,
            minYear = earliestYear,
            onDismiss = { showPickerDialog = false },
            onMonthSelected = { pickedKey ->
                showPickerDialog = false
                onSelectMonth(pickedKey)
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month-Year Picker Trigger Button
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isOlderMonthSelected) palette.accent.copy(alpha = 0.16f) else palette.glassCardHigh,
            border = BorderStroke(
                1.2.dp,
                if (isOlderMonthSelected) palette.accent else palette.accent.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .shadow(3.dp, RoundedCornerShape(50), spotColor = palette.accent.copy(alpha = 0.16f))
                .clickable { showPickerDialog = true }
                .testTag("${testTagPrefix}_month_picker_btn")
        ) {
            Text(
                text = "📅 पुराना महीना चुनें",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
            )
        }

        // If user picked a month older than the 6-month window, keep its chip visible & active
        if (isOlderMonthSelected) {
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.accent,
                border = BorderStroke(1.dp, palette.accent),
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(50), spotColor = palette.accent.copy(alpha = 0.2f))
                    .clickable { showPickerDialog = true }
                    .testTag("${testTagPrefix}_older_month_chip")
            ) {
                Text(
                    text = "${DateUtils.formatHindiMonthYear(selectedMonth)} ✓",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        recentSixKeys.forEach { ym ->
            val mKey = ym.toString()
            val isSelected = mKey == selectedMonth
            val label = DateUtils.formatHindiMonthYear(ym)

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) palette.accent else palette.glassCardHigh,
                border = BorderStroke(1.dp, if (isSelected) palette.accent else palette.glassBorderMid),
                modifier = Modifier
                    .shadow(
                        if (isSelected) 6.dp else 2.dp,
                        RoundedCornerShape(50),
                        spotColor = palette.accent.copy(alpha = 0.18f)
                    )
                    .clickable { onSelectMonth(mKey) }
                    .testTag("${testTagPrefix}_month_chip_$mKey")
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    initialMonthKey: String,
    maxYearMonth: YearMonth,
    minYear: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    val initialYm = remember(initialMonthKey, maxYearMonth) {
        runCatching { YearMonth.parse(initialMonthKey) }.getOrNull() ?: maxYearMonth
    }
    val boundedMinYear = minYear.coerceAtMost(maxYearMonth.year)
    val years = remember(boundedMinYear, maxYearMonth.year) {
        (maxYearMonth.year downTo boundedMinYear).toList()
    }

    var selectedYear by remember(initialYm) { mutableIntStateOf(initialYm.year.coerceIn(boundedMinYear, maxYearMonth.year)) }
    var selectedMonthValue by remember(initialYm) { mutableIntStateOf(initialYm.monthValue) }

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
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 महीना व वर्ष चुनें",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.accent.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onMonthSelected(maxYearMonth.toString()) }
                            .testTag("picker_jump_current_month_btn")
                    ) {
                        Text(
                            text = "इस माह",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1-2 साल पुराना डेटा व सैलरी स्लिप देखने के लिए वर्ष और महीना चुनें",
                    fontSize = 11.sp,
                    color = palette.textMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "वर्ष चुनें",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.forEach { yr ->
                        val isYearSelected = yr == selectedYear
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isYearSelected) palette.accent else palette.segmentedTrackBg,
                            border = BorderStroke(
                                1.dp,
                                if (isYearSelected) palette.accent else palette.glassBorderMid
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedYear = yr
                                    if (yr == maxYearMonth.year && selectedMonthValue > maxYearMonth.monthValue) {
                                        selectedMonthValue = maxYearMonth.monthValue
                                    }
                                }
                                .testTag("picker_year_$yr")
                        ) {
                            Text(
                                text = yr.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isYearSelected) Color.White else palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "महीना चुनें ($selectedYear)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 3 columns x 4 rows Hindi months grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthNum = row * 3 + col + 1
                                val candidate = YearMonth.of(selectedYear, monthNum)
                                val isFuture = candidate.isAfter(maxYearMonth)
                                val isSelected = selectedYear == selectedYear && monthNum == selectedMonthValue && !isFuture
                                val monthName = DateUtils.HINDI_MONTHS[monthNum - 1]

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isSelected -> palette.accent
                                        isFuture -> palette.segmentedTrackBg.copy(alpha = 0.45f)
                                        else -> palette.glassCard
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            isSelected -> palette.accent
                                            isFuture -> palette.glassBorderMid.copy(alpha = 0.4f)
                                            else -> palette.glassBorderMid
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = !isFuture) {
                                            selectedMonthValue = monthNum
                                        }
                                        .testTag("picker_month_$monthNum")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = when {
                                                isSelected -> Color.White
                                                isFuture -> palette.textMuted.copy(alpha = 0.45f)
                                                else -> palette.textPrimary
                                            },
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val previewYm = YearMonth.of(selectedYear, selectedMonthValue)
                val safePreviewYm = if (previewYm.isAfter(maxYearMonth)) maxYearMonth else previewYm
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
                    Spacer(modifier = Modifier.height(0.dp).padding(horizontal = 6.dp))
                    Button(
                        onClick = { onMonthSelected(safePreviewYm.toString()) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("picker_confirm_btn")
                    ) {
                        Text(
                            text = "${DateUtils.formatHindiMonthYear(safePreviewYm)} देखें ✓",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
