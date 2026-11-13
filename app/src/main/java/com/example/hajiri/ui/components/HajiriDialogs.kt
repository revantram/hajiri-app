package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.WageMode
import com.example.hajiri.ui.theme.LiquidDarkPalette
import com.example.hajiri.ui.theme.LiquidLightPalette
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OceanGlassPalette
import com.example.hajiri.ui.theme.RoseGoldPalette
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBright
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.ui.theme.StatusRedBright
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.StatusYellowBright
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.util.AppUpdateChecker
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.UpdateCheckState
import com.example.hajiri.viewmodel.PreviousMonthBalanceItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Composable
fun SalaryDialog(
    initialSalary: Double,
    initialDailyWage: Double,
    initialWageMode: WageMode,
    initialOtRate: Double,
    onDismiss: () -> Unit,
    onSave: (salary: Double, dailyWage: Double, wageMode: WageMode, otRate: Double) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var selectedMode by remember { mutableStateOf(initialWageMode) }
    var salaryText by remember { mutableStateOf(if (initialSalary > 0) initialSalary.toLong().toString() else "") }
    var dailyWageText by remember { mutableStateOf(if (initialDailyWage > 0) initialDailyWage.toLong().toString() else "") }
    var otRateText by remember { mutableStateOf(if (initialOtRate > 0) initialOtRate.toLong().toString() else "50") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.glassCardHigh),
            border = BorderStroke(1.2.dp, palette.glassBorderTop),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "💰 वेतन / मजदूरी सेटिंग",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "हिसाब करने का तरीका चुनें:",
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // One-click segmented selector: Monthly vs Daily Attendance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.segmentedTrackBg, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WageMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (isSelected) palette.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode }
                                .testTag("wage_mode_${mode.id}_btn")
                        ) {
                            Text(
                                text = mode.labelHindi,
                                color = if (isSelected) Color.White else palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMode == WageMode.MONTHLY) {
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("मासिक वेतन (₹ / महीना)") },
                        placeholder = { Text("जैसे: 15000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("salary_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = dailyWageText,
                        onValueChange = { dailyWageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("दिन की हाजिरी दर (₹ / दिन)") },
                        placeholder = { Text("जैसे: 600") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("daily_wage_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = otRateText,
                    onValueChange = { otRateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ओवरटाइम दर (₹/घंटा)") },
                    placeholder = { Text("जैसे: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("ot_rate_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(22.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val sal = salaryText.toDoubleOrNull() ?: 0.0
                            val daily = dailyWageText.toDoubleOrNull() ?: 0.0
                            val otr = otRateText.toDoubleOrNull() ?: 50.0
                            onSave(sal, daily, selectedMode, otr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_salary_button")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayDialog(
    dateKey: String,
    initialStatus: AttendanceStatus?,
    initialNote: String,
    initialOtHours: Float = 0f,
    hasExistingRecord: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (status: AttendanceStatus?, note: String, otHoursValue: Float?) -> Unit
) {
    val palette = LocalHajiriPalette.current
    val parsedDate = DateUtils.parseDateKey(dateKey)
    val dateTitle = if (parsedDate != null) DateUtils.formatHindiDate(parsedDate) else dateKey
    val daySubtitle = if (parsedDate != null) DateUtils.HINDI_DAYS[parsedDate.dayOfWeek.value - 1] else ""

    // Only P, A, HD are selectable in this popup per user request
    var selectedStatus by remember(dateKey) {
        mutableStateOf(
            when (initialStatus) {
                AttendanceStatus.PRESENT,
                AttendanceStatus.ABSENT,
                AttendanceStatus.HALF -> initialStatus
                else -> null
            }
        )
    }
    var noteText by remember(dateKey) { mutableStateOf(initialNote) }
    var otText by remember(dateKey, initialOtHours) {
        mutableStateOf(
            if (initialOtHours > 0f) {
                if (initialOtHours % 1f == 0f) initialOtHours.toInt().toString() else initialOtHours.toString()
            } else ""
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📅 $dateTitle",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        if (daySubtitle.isNotBlank()) {
                            Text(
                                text = daySubtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    if (hasExistingRecord || initialOtHours > 0f) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.testTag("calendar_popup_clear_btn")
                        ) {
                            Text(
                                text = "हटाएं",
                                color = StatusRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "हाजिरी चुनें (P / A / HD)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Exactly 3 options: P, A, HD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalendarStatusPill(
                        code = "P",
                        label = "हाजिर",
                        isSelected = selectedStatus == AttendanceStatus.PRESENT,
                        gradient = listOf(StatusGreenBright, StatusGreen),
                        modifier = Modifier.weight(1f).testTag("cal_popup_p_btn"),
                        onClick = { selectedStatus = AttendanceStatus.PRESENT }
                    )
                    CalendarStatusPill(
                        code = "A",
                        label = "गैरहाजिर",
                        isSelected = selectedStatus == AttendanceStatus.ABSENT,
                        gradient = listOf(StatusRedBright, StatusRed),
                        modifier = Modifier.weight(1f).testTag("cal_popup_a_btn"),
                        onClick = { selectedStatus = AttendanceStatus.ABSENT }
                    )
                    CalendarStatusPill(
                        code = "HD",
                        label = "आधा दिन",
                        isSelected = selectedStatus == AttendanceStatus.HALF,
                        gradient = listOf(StatusYellowBright, StatusYellow),
                        modifier = Modifier.weight(1f).testTag("cal_popup_hd_btn"),
                        onClick = { selectedStatus = AttendanceStatus.HALF }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overtime (OT) Edit / Delete Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏰ ओवरटाइम (OT) घंटे",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
                    val parsedOt = otText.toFloatOrNull() ?: 0f
                    if (parsedOt > 0f || initialOtHours > 0f) {
                        Text(
                            text = "🗑️ OT हटाएं (0h)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            modifier = Modifier
                                .clickable { otText = "" }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("calendar_popup_clear_ot_btn")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.segmentedTrackBg,
                        border = BorderStroke(1.dp, palette.glassBorderMid),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                val cur = otText.toFloatOrNull() ?: 0f
                                val next = ((cur - 0.5f).coerceAtLeast(0f) * 10f).roundToInt() / 10f
                                otText = if (next <= 0f) "" else if (next % 1f == 0f) next.toInt().toString() else next.toString()
                            }
                            .testTag("calendar_popup_ot_minus_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-0.5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OtOrange)
                        }
                    }

                    OutlinedTextField(
                        value = otText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) otText = filtered
                        },
                        placeholder = { Text("0 घंटे (जैसे: 2.5)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("calendar_popup_ot_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OtOrange.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, OtOrange.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                val cur = otText.toFloatOrNull() ?: 0f
                                val next = ((cur + 0.5f).coerceAtMost(24f) * 10f).roundToInt() / 10f
                                otText = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                            }
                            .testTag("calendar_popup_ot_plus_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+0.5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OtOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note section at bottom
                Text(
                    text = "📝 नोट जोड़ें",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("इस तारीख का नोट लिखें (वैकल्पिक)...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("calendar_popup_note_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                val otVal = (otText.toFloatOrNull() ?: 0f).coerceIn(0f, 24f)
                val otChanged = otVal != initialOtHours
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(selectedStatus, noteText, otVal) },
                        enabled = selectedStatus != null || noteText.isNotBlank() || hasExistingRecord || otChanged || otVal > 0f,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("calendar_popup_save_btn")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarStatusPill(
    code: String,
    label: String,
    isSelected: Boolean,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = if (isSelected) BorderStroke(2.5.dp, Color.White)
        else BorderStroke(1.dp, palette.fieldUnfocusedBorder),
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 1.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = gradient.last().copy(alpha = 0.35f)
            )
            .background(
                brush = if (isSelected) Brush.linearGradient(gradient)
                else Brush.linearGradient(listOf(palette.glassCard, palette.glassCard)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = code,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else gradient.last()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isSelected) "$label ✓" else label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ThemePickerDialog(
    currentTheme: AppThemeStyle,
    onDismiss: () -> Unit,
    onSelectTheme: (AppThemeStyle) -> Unit
) {
    val palette = LocalHajiriPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.glassCardHigh),
            border = BorderStroke(1.2.dp, palette.glassBorderTop),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🎨 ऐप थीम चुनें",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "अपनी पसंद की लिक्विड ग्लास थीम चुनें:",
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                AppThemeStyle.entries.forEach { themeOption ->
                    val optionPalette = when (themeOption) {
                        AppThemeStyle.LIQUID_LIGHT -> LiquidLightPalette
                        AppThemeStyle.OCEAN_GLASS -> OceanGlassPalette
                        AppThemeStyle.ROSE_GOLD -> RoseGoldPalette
                        AppThemeStyle.LIQUID_DARK -> LiquidDarkPalette
                    }
                    val isSelected = themeOption == currentTheme

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) palette.accent.copy(alpha = 0.14f) else palette.glassCard,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.accent else palette.fieldUnfocusedBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(themeOption) }
                            .testTag("theme_option_${themeOption.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(optionPalette.accent, optionPalette.meshTopLeft)
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = themeOption.emoji, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = themeOption.labelHindi,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "✓ लागू",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.fieldUnfocusedBorder)
                    ) {
                        Text("बंद करें", color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkDaysDialog(
    initialWorkDays: Int,
    onDismiss: () -> Unit,
    onSave: (workDays: Int) -> Unit
) {
    val palette = LocalHajiriPalette.current
    val isInitiallyAuto = initialWorkDays !in 1..31 || initialWorkDays == 26
    var isAutoMode by remember { mutableStateOf(isInitiallyAuto) }
    var text by remember {
        mutableStateOf(if (isInitiallyAuto) "" else initialWorkDays.toString())
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
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "📅 कार्यदिवस सेटिंग",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ऑटो मोड में फरवरी (28/29), अप्रैल (30) व 31 दिन वाले महीने अपने आप सेट होते हैं।",
                    fontSize = 11.sp,
                    color = palette.textMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAutoMode) palette.accent else palette.segmentedTrackBg,
                    border = BorderStroke(
                        width = if (isAutoMode) 1.5.dp else 1.dp,
                        color = if (isAutoMode) palette.accent else palette.glassBorderMid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isAutoMode = true
                            onSave(26)
                        }
                        .testTag("workdays_auto_btn")
                ) {
                    Text(
                        text = if (isAutoMode) {
                            "✓ ऑटो (महीने के अनुसार 30/31 दिन)"
                        } else {
                            "✨ ऑटो (महीने के अनुसार 30/31 दिन)"
                        },
                        color = if (isAutoMode) Color.White else palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it.filter { ch -> ch.isDigit() }
                        isAutoMode = false
                    },
                    label = { Text("या कस्टम दिन दर्ज करें (जैसे: 25, 28, 30)") },
                    placeholder = { Text("जैसे: 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("workdays_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(22.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val custom = text.toIntOrNull()
                            if (isAutoMode || custom == null) {
                                onSave(26)
                            } else {
                                onSave(custom.coerceIn(1, 31))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_workdays_button")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdvanceDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, reason: String, dateKey: String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedDateKey = DateUtils.toDateKey(selectedDate)
    val selectedDateLabel = DateUtils.formatHindiDate(selectedDate)

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val todayEndMillis = LocalDate.now().atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayEndMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("advance_datepicker_confirm_btn")
                ) {
                    Text("चुनें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("रद्द करें")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "💳 एडवांस / उधार जोड़ें",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accent.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("advance_date_picker_btn")
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
                                text = "📅 एडवांस लेने की तारीख",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = selectedDateLabel,
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("रकम (₹)") },
                    placeholder = { Text("जैसे: 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("advance_amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("कारण / विवरण") },
                    placeholder = { Text("जैसे: घर के लिए, दवाई, किराना...") },
                    modifier = Modifier.fillMaxWidth().testTag("advance_reason_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(22.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSave(amt, reasonText.ifBlank { "एडवांस" }, selectedDateKey)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_advance_button")
                    ) {
                        Text("💾 जोड़ें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String = "मिटाएं",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.glassCardHigh),
            border = BorderStroke(1.2.dp, palette.glassBorderTop),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors(): androidx.compose.material3.TextFieldColors {
    val palette = LocalHajiriPalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.fieldUnfocusedBorder,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.textSecondary,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        cursorColor = palette.accent,
        focusedContainerColor = palette.fieldFocusedBg,
        unfocusedContainerColor = palette.fieldUnfocusedBg
    )
}

@Composable
fun PhoneProfileDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    val palette = LocalHajiriPalette.current
    var nameText by remember { mutableStateOf(initialName) }
    var phoneText by remember { mutableStateOf(initialPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.glassCardHigh),
            border = BorderStroke(1.2.dp, palette.glassBorderTop),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "👤 प्रोफ़ाइल और मोबाइल नंबर",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("आपका नाम") },
                    placeholder = { Text("जैसे: Ramesh Kumar") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { if (it.length <= 15) phoneText = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                    placeholder = { Text("10 अंक का नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(nameText, phoneText) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    installedVersion: String,
    repoSlug: String,
    updateState: UpdateCheckState,
    autoCheckEnabled: Boolean,
    onToggleAutoCheck: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    onSaveRepo: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    var showRepoEditor by remember { mutableStateOf(false) }
    var repoInput by remember(repoSlug) { mutableStateOf(repoSlug) }

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
                        text = "🔄 ऐप अपडेट",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "v$installedVersion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (updateState) {
                    is UpdateCheckState.Checking -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.glassCard,
                            border = BorderStroke(1.dp, palette.glassBorderMid),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⏳ नया अपडेट चेक किया जा रहा है...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    is UpdateCheckState.UpdateAvailable -> {
                        val info = updateState.releaseInfo
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StatusGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🎉 नया अपडेट उपलब्ध है! (${info.latestTag})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusGreen
                                )
                                Text(
                                    text = info.releaseName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                                if (info.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = info.releaseNotes.take(260),
                                        fontSize = 11.sp,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onOpenUrl(info.downloadUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("one_click_update_btn")
                        ) {
                            Text(
                                text = "⬇️ 1-Click अपडेट करें (${info.latestTag})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        if (info.releasePageUrl.isNotBlank() && info.releasePageUrl != info.downloadUrl) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onOpenUrl(info.releasePageUrl) },
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("open_release_page_btn")
                            ) {
                                Text(
                                    text = "📄 रिलीज़ पेज खोलें",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    is UpdateCheckState.UpToDate -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StatusGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ आपका ऐप नवीनतम वर्ज़न (v${updateState.currentVersion} / ${updateState.latestTag}) पर है।",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreen,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    is UpdateCheckState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StatusRedBg,
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${updateState.message}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatusRed,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    UpdateCheckState.Idle -> {
                        Text(
                            text = "नया वर्ज़न आने पर 1 क्लिक में सीधे GitHub से अपडेट APK खोलें।",
                            fontSize = 13.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                if (updateState !is UpdateCheckState.UpdateAvailable) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onOpenUrl(AppUpdateChecker.buildLatestReleaseUrl(repoSlug)) },
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("dialog_open_github_direct_btn")
                    ) {
                        Text(
                            text = "⬇️ GitHub से सीधा APK पेज खोलें",
                            color = palette.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Auto Check row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ऑटो अपडेट चेक",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    androidx.compose.material3.Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = onToggleAutoCheck
                    )
                }

                Text(
                    text = if (showRepoEditor) "▲ रिपो सेटिंग छुपाएं" else "⚙️ अपडेट स्रोत बदलें ($repoSlug)",
                    fontSize = 11.sp,
                    color = palette.accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { showRepoEditor = !showRepoEditor }
                        .padding(vertical = 6.dp)
                )

                if (showRepoEditor) {
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("GitHub Repo (owner/repo)") },
                        placeholder = { Text("revantram/hajiri-app") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_repo_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSaveRepo(repoInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("स्रोत सेव करें व चेक करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.fieldUnfocusedBorder)
                    ) {
                        Text("बाद में", color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onCheckNow,
                        enabled = updateState !is UpdateCheckState.Checking,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("dialog_check_update_btn")
                    ) {
                        Text("🔄 अभी चेक करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PreviousBalanceDialog(
    months: List<PreviousMonthBalanceItem>,
    onToggleSettled: (monthKey: String, isSettled: Boolean) -> Unit,
    onPartialPayment: (monthKey: String, paidAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val totalPendingSum = months.sumOf { it.remainingAmount }
    var editingMonthKey by remember { mutableStateOf<String?>(null) }
    var partialInputText by remember { mutableStateOf("") }

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
                Text(
                    text = "💼 पिछले महीने के बाकी",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "पिछले सभी महीनों का बचा बकाया इस महीने की सैलरी में जुड़ता है। यदि आंशिक या पूरा पैसा मिला हो तो यहाँ दर्ज करें।",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (totalPendingSum > 0.01) OtOrange.copy(alpha = 0.12f) else StatusGreenBg,
                    border = BorderStroke(
                        1.dp,
                        if (totalPendingSum > 0.01) OtOrange.copy(alpha = 0.4f) else StatusGreen.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "कुल संचयी बकाया",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "₹${DateUtils.formatInr(totalPendingSum)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalPendingSum > 0.01) OtOrange else StatusGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 370.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    months.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.glassCard,
                            border = BorderStroke(1.dp, palette.glassBorderMid),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prev_balance_item_${item.monthKey}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.monthTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        if (item.earnedAmount > 0.0) {
                                            Text(
                                                text = "कुल: ₹${DateUtils.formatInr(item.earnedAmount)} • मिला: ₹${DateUtils.formatInr(item.paidAmount)}",
                                                fontSize = 10.sp,
                                                color = palette.textMuted
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (item.remainingAmount > 0.0) {
                                            "बाकी ₹${DateUtils.formatInr(item.remainingAmount)}"
                                        } else {
                                            "₹0"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            item.earnedAmount <= 0.0 -> palette.textMuted
                                            item.isReceived -> StatusGreen
                                            else -> OtOrange
                                        }
                                    )
                                }

                                if (item.earnedAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val unpaidActive = !item.isReceived && item.paidAmount <= 0.0
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (unpaidActive) OtOrange.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                            border = BorderStroke(
                                                width = if (unpaidActive) 1.5.dp else 1.dp,
                                                color = if (unpaidActive) OtOrange else palette.glassBorderMid
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    editingMonthKey = null
                                                    onToggleSettled(item.monthKey, false)
                                                }
                                                .testTag("dialog_unpaid_${item.monthKey}")
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unpaidActive) "✓ बाकी जोड़ें" else "बाकी जोड़ें",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (unpaidActive) OtOrange else palette.textSecondary
                                                )
                                            }
                                        }

                                        val partialActive = !item.isReceived && item.paidAmount > 0.0
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (partialActive || editingMonthKey == item.monthKey) palette.accent.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                            border = BorderStroke(
                                                width = if (partialActive || editingMonthKey == item.monthKey) 1.5.dp else 1.dp,
                                                color = if (partialActive || editingMonthKey == item.monthKey) palette.accent else palette.glassBorderMid
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (editingMonthKey == item.monthKey) {
                                                        editingMonthKey = null
                                                    } else {
                                                        editingMonthKey = item.monthKey
                                                        partialInputText = if (item.paidAmount > 0.0) item.paidAmount.toLong().toString() else ""
                                                    }
                                                }
                                                .testTag("dialog_partial_${item.monthKey}")
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "💵 आंशिक",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (partialActive || editingMonthKey == item.monthKey) palette.accent else palette.textSecondary
                                                )
                                            }
                                        }

                                        val paidActive = item.isReceived
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (paidActive) StatusGreen.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                            border = BorderStroke(
                                                width = if (paidActive) 1.5.dp else 1.dp,
                                                color = if (paidActive) StatusGreen else palette.glassBorderMid
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    editingMonthKey = null
                                                    onToggleSettled(item.monthKey, true)
                                                }
                                                .testTag("dialog_paid_${item.monthKey}")
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (paidActive) "✓ पूरा मिला" else "पूरा मिला",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (paidActive) StatusGreen else palette.textSecondary
                                                )
                                            }
                                        }
                                    }

                                    if (editingMonthKey == item.monthKey) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = partialInputText,
                                                onValueChange = { partialInputText = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("कितने ₹ मिले?", fontSize = 11.sp) },
                                                placeholder = { Text("जैसे: 10000", fontSize = 12.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = outlinedFieldColors(),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("partial_paid_input_${item.monthKey}")
                                            )
                                            Button(
                                                onClick = {
                                                    val paidVal = partialInputText.toDoubleOrNull() ?: 0.0
                                                    onPartialPayment(item.monthKey, paidVal)
                                                    editingMonthKey = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.testTag("partial_paid_save_${item.monthKey}")
                                            ) {
                                                Text("सेव ✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "कोई कमाई/बकाया नहीं",
                                        fontSize = 11.sp,
                                        color = palette.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("close_prev_balance_dialog_btn")
                    ) {
                        Text("ठीक है", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
