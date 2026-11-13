package com.example.hajiri.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.MonthSelectorWithPickerRow
import com.example.hajiri.ui.components.outlinedFieldColors
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.StatusBlue
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.SalarySlipPdfGenerator
import com.example.hajiri.viewmodel.HajiriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@Composable
fun ReportsScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedMonth by viewModel.selectedReportMonth.collectAsState()
    val currentMonthKey by viewModel.todayMonthKeyState.collectAsState()
    val earliestYear by viewModel.earliestDataYear.collectAsState()
    val stats by viewModel.reportStats.collectAsState()
    val trends by viewModel.sixMonthTrends.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val palette = LocalHajiriPalette.current

    var showExtraSlipOptions by remember { mutableStateOf(false) }
    var siteOrCompanyInput by remember { mutableStateOf("") }
    var extraBonusInput by remember { mutableStateOf("") }
    var extraWorkNoteInput by remember { mutableStateOf("") }
    var isPdfBusy by remember { mutableStateOf(false) }

    val extraBonusAmount = extraBonusInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val slipNetAmount = maxOf(0.0, stats.netTakeHome + extraBonusAmount)

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isPdfBusy = true
        val slipData = viewModel.buildMonthlySlipData(
            monthKey = selectedMonth,
            siteOrCompany = siteOrCompanyInput,
            extraWorkBonus = extraBonusAmount,
            extraWorkNote = extraWorkNoteInput
        )
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        SalarySlipPdfGenerator.writePdf(slipData, out, context)
                    } ?: error("Output stream unavailable")
                }.isSuccess
            }
            isPdfBusy = false
            if (saved) {
                viewModel.showMessage("✅ PDF सैलरी स्लिप फोन में सेव हो गई!")
            } else {
                viewModel.showMessage("⚠️ PDF सेव करने में समस्या आई")
            }
        }
    }

    val animatedSweep by animateFloatAsState(
        targetValue = (stats.attendancePercentage / 100f) * 360f,
        label = "sweep_anim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📊 ", fontSize = 24.sp)
                Text(
                    text = "रिपोर्ट",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
            }
        }

        // Month Selector Chips + Month-Year Picker (unrestricted history)
        MonthSelectorWithPickerRow(
            selectedMonth = selectedMonth,
            currentMonthKey = currentMonthKey,
            earliestYear = earliestYear,
            testTagPrefix = "reports",
            onSelectMonth = { viewModel.setReportMonth(it) }
        )

        // Attendance Percentage Ring Card
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "उपस्थिति प्रतिशत",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.attendancePercentage}%",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    val totalMonthDays = remember(selectedMonth) {
                        runCatching { YearMonth.parse(selectedMonth).lengthOfMonth() }.getOrDefault(30)
                    }
                    Text(
                        text = "${SalarySlipPdfGenerator.formatFloatDays(stats.workedDays)} / $totalMonthDays दिन काम किए",
                        fontSize = 13.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Circular Progress Arc
                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = palette.progressTrackBg,
                            style = Stroke(width = 9.dp.toPx())
                        )
                        drawArc(
                            brush = Brush.linearGradient(listOf(palette.accent, StatusGreen)),
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${stats.attendancePercentage}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "उपस्थिति",
                            fontSize = 9.sp,
                            color = palette.textMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 6-Month Comparison Bar Chart
        GlassCard {
            Column {
                Text(
                    text = "पिछले 6 महीने (काम के दिन)",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val maxDays = trends.maxOfOrNull { it.workedDays }?.coerceAtLeast(1f) ?: 1f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    trends.forEach { trend ->
                        val barHeightFactor = (trend.workedDays / maxDays).coerceIn(0.08f, 1f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setReportMonth(trend.monthKey) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (trend.workedDays > 0) trend.workedDays.toInt().toString() else "",
                                fontSize = 10.sp,
                                color = if (trend.isSelected) palette.accent else palette.textMuted,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((75 * barHeightFactor).dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (trend.isSelected) {
                                                listOf(palette.accent, palette.accentSecondary)
                                            } else {
                                                listOf(palette.textMuted.copy(alpha = 0.5f), palette.progressTrackBg)
                                            }
                                        ),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    trends.forEach { trend ->
                        Text(
                            text = trend.labelShort,
                            fontSize = 11.sp,
                            color = if (trend.isSelected) palette.accent else palette.textSecondary,
                            fontWeight = if (trend.isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Breakdown Card
        GlassCard {
            Column {
                Text(
                    text = "इस महीने का हिसाब",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BreakdownRow("✅ हाजिर दिन", "${stats.presentDays} दिन", StatusGreen)
                BreakdownRow("❌ गैरहाजिर", "${stats.absentDays} दिन", StatusRed)
                BreakdownRow("🌓 आधा दिन", "${stats.halfDays} दिन", StatusYellow)
                BreakdownRow("✈️ छुट्टी", "${stats.leaveDays} दिन", StatusBlue)
                BreakdownRow("⏰ ओवरटाइम", "${stats.totalOtHours} घंटे", OtOrange)
                BreakdownRow("💳 एडवांस", "-₹${DateUtils.formatInr(stats.totalAdvance)}", AdvancePink)

                if (stats.previousMonthRawBalance > 0.0 && stats.previousMonthKey.isNotBlank()) {
                    val prevValueText = if (stats.isPreviousMonthReceived) {
                        "₹0 (ले लिया गया)"
                    } else {
                        "+₹${DateUtils.formatInr(stats.carryForwardAdded)}"
                    }
                    val prevValueColor = if (stats.isPreviousMonthReceived) palette.textMuted else OtOrange
                    BreakdownRow(
                        label = "💼 पिछला बाकी (${stats.previousMonthTitle})",
                        value = prevValueText,
                        valueColor = prevValueColor
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (stats.isPreviousMonthReceived) StatusGreen.copy(alpha = 0.14f) else OtOrange.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                if (stats.isPreviousMonthReceived) StatusGreen.copy(alpha = 0.45f) else OtOrange.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.toggleMonthSettled(
                                        stats.previousMonthKey,
                                        !stats.isPreviousMonthReceived
                                    )
                                }
                                .testTag("report_prev_balance_toggle_btn")
                        ) {
                            Text(
                                text = if (stats.isPreviousMonthReceived) {
                                    "✓ ले लिया गया (क्लिक कर सैलरी में जोड़ें)"
                                } else {
                                    "💼 ₹${DateUtils.formatInr(stats.previousMonthRawBalance)} बाकी जुड़ा है (ले लिया हो तो क्लिक करें)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isPreviousMonthReceived) StatusGreen else OtOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accent.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable { viewModel.showPrevBalanceDialog.value = true }
                                .testTag("report_open_all_prev_balance_btn")
                        ) {
                            Text(
                                text = "सभी महीने",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val netCardBg = if (palette.isDark) {
                    listOf(Color(0xFF064E3B), Color(0xFF065F46))
                } else {
                    listOf(Color(0xFFECFDF5), StatusGreenBg)
                }
                val netLabelColor = if (palette.isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                val netValueColor = if (palette.isDark) Color(0xFF34D399) else StatusGreen

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color(0x1F10B981))
                        .background(
                            brush = Brush.linearGradient(netCardBg),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🤑 हाथ में आएगा",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = netLabelColor
                    )
                    Text(
                        text = "₹${DateUtils.formatInr(stats.netTakeHome)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = netValueColor
                    )
                }
            }
        }

        // PDF Salary Slip / Monthly Bill Card
        GlassCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧾 PDF सैलरी स्लिप / मंथली बिल",
                            fontSize = 15.sp,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "नाम, काम के दिन, अधिक काम, एडवांस व कुल सैलरी सहित",
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = DateUtils.formatHindiMonthYear(selectedMonth),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Slip Summary Strip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassCardHigh.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, palette.glassBorderMid),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 ${currentUser?.name?.ifBlank { "हाजिरी यूजर" } ?: "हाजिरी यूजर"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${stats.workedDays} दिन • ${stats.totalOtHours}h OT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "एडवांस कटौती: -₹${DateUtils.formatInr(stats.totalAdvance)}",
                                fontSize = 11.sp,
                                color = AdvancePink,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "कुल बिल: ₹${DateUtils.formatInr(slipNetAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreen
                            )
                        }
                        if (stats.carryForwardAdded > 0.0) {
                            Text(
                                text = "💼 पिछला बाकी (${stats.previousMonthTitle}) +₹${DateUtils.formatInr(stats.carryForwardAdded)} भी शामिल है",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OtOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Extra Work / Site Details toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.segmentedTrackBg.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExtraSlipOptions = !showExtraSlipOptions }
                        .testTag("toggle_pdf_extra_work_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "➕ अधिक काम / कंपनी विवरण जोड़ें (वैकल्पिक)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                        Text(
                            text = if (showExtraSlipOptions) "▲" else "▼",
                            fontSize = 11.sp,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(visible = showExtraSlipOptions) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = siteOrCompanyInput,
                            onValueChange = { siteOrCompanyInput = it },
                            label = { Text("कंपनी / साइट / ठेकेदार का नाम", fontSize = 12.sp) },
                            placeholder = { Text("जैसे: गुप्ता कंस्ट्रक्शन साइट", fontSize = 12.sp, color = palette.textMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_site_company_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = extraBonusInput,
                                onValueChange = { extraBonusInput = it },
                                label = { Text("अधिक काम / बोनस ₹", fontSize = 11.sp) },
                                placeholder = { Text("0", fontSize = 12.sp, color = palette.textMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("pdf_extra_bonus_input")
                            )
                            OutlinedTextField(
                                value = extraWorkNoteInput,
                                onValueChange = { extraWorkNoteInput = it },
                                label = { Text("अधिक काम विवरण", fontSize = 11.sp) },
                                placeholder = { Text("जैसे: संडे काम / भत्ता", fontSize = 12.sp, color = palette.textMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("pdf_extra_work_note_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save PDF to Phone (Zero-permission SAF CreateDocument)
                Button(
                    onClick = {
                        val safeMonth = selectedMonth.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        createPdfLauncher.launch("Hajiri_Salary_Slip_$safeMonth.pdf")
                    },
                    enabled = !isPdfBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_pdf_slip_btn")
                ) {
                    Text(
                        text = if (isPdfBusy) "⏳ PDF तैयार हो रहा है..." else "📥 PDF फोन में सेव करें",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share PDF directly on WhatsApp / Apps
                    Button(
                        onClick = {
                            isPdfBusy = true
                            val slipData = viewModel.buildMonthlySlipData(
                                monthKey = selectedMonth,
                                siteOrCompany = siteOrCompanyInput,
                                extraWorkBonus = extraBonusAmount,
                                extraWorkNote = extraWorkNoteInput
                            )
                            scope.launch {
                                val uriResult = withContext(Dispatchers.IO) {
                                    runCatching {
                                        SalarySlipPdfGenerator.createShareablePdfUri(context, slipData)
                                    }
                                }
                                isPdfBusy = false
                                uriResult.onSuccess { pdfUri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "हाजिरी सैलरी स्लिप - ${slipData.monthTitle}"
                                        )
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${slipData.employeeName} (${slipData.monthTitle}) की सैलरी स्लिप PDF — कुल हाथ में: ₹${DateUtils.formatInr(slipNetAmount)}"
                                        )
                                        clipData = ClipData.newRawUri("Salary Slip PDF", pdfUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "PDF सैलरी स्लिप शेयर करें")
                                    )
                                }.onFailure {
                                    viewModel.showMessage("⚠️ PDF बनाने में समस्या आई")
                                }
                            }
                        },
                        enabled = !isPdfBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("share_pdf_slip_btn")
                    ) {
                        Text(
                            text = "📤 WhatsApp / Share PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Preview / Open PDF
                    OutlinedButton(
                        onClick = {
                            isPdfBusy = true
                            val slipData = viewModel.buildMonthlySlipData(
                                monthKey = selectedMonth,
                                siteOrCompany = siteOrCompanyInput,
                                extraWorkBonus = extraBonusAmount,
                                extraWorkNote = extraWorkNoteInput
                            )
                            scope.launch {
                                val uriResult = withContext(Dispatchers.IO) {
                                    runCatching {
                                        SalarySlipPdfGenerator.createShareablePdfUri(context, slipData)
                                    }
                                }
                                isPdfBusy = false
                                uriResult.onSuccess { pdfUri ->
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(pdfUri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(viewIntent)
                                    } catch (_: ActivityNotFoundException) {
                                        viewModel.showMessage("⚠️ PDF खोलने के लिए ऐप नहीं मिला — सेव या शेयर करें")
                                    }
                                }.onFailure {
                                    viewModel.showMessage("⚠️ PDF बनाने में समस्या आई")
                                }
                            }
                        },
                        enabled = !isPdfBusy,
                        border = BorderStroke(1.2.dp, palette.accent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("preview_pdf_slip_btn")
                    ) {
                        Text(
                            text = "👁️ देखें",
                            color = palette.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Text Export & Share
        GlassCard {
            Column {
                Text(
                    text = "टेक्स्ट रिपोर्ट (Quick Copy & Share)",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        val reportText = viewModel.getReportText(selectedMonth)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Hajiri Report", reportText)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showMessage("📋 रिपोर्ट कॉपी हो गई!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentSecondary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_report_btn")
                ) {
                    Text("📄 टेक्स्ट Report Copy करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val reportText = viewModel.getReportText(selectedMonth)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Hajiri Report"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_whatsapp_btn")
                ) {
                    Text("💬 WhatsApp टेक्स्ट Share करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun BreakdownRow(label: String, value: String, valueColor: Color) {
    val palette = LocalHajiriPalette.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 13.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
        HorizontalDivider(color = palette.divider)
    }
}
