package com.example.hajiri.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.WageMode
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.LiquidGlassButton
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.OtOrangeBg
import com.example.hajiri.ui.theme.StatusBlue
import com.example.hajiri.ui.theme.StatusBlueBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusGreenBright
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.StatusYellowBg
import com.example.hajiri.util.DailyReminderScheduler
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.UpdateCheckState
import com.example.hajiri.viewmodel.HajiriTab
import com.example.hajiri.viewmodel.HajiriViewModel
import java.time.YearMonth

@Composable
fun HomeScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val attendance by viewModel.attendance.collectAsState()
    val otHours by viewModel.otHours.collectAsState()
    val advances by viewModel.advances.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.homeStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()

    val palette = LocalHajiriPalette.current
    val activeSite = viewModel.getActiveSite(settings)
    val wageMode = WageMode.fromId(activeSite.wageMode)

    val todayKey by viewModel.todayKeyState.collectAsState()
    val todayMonthKey by viewModel.todayMonthKeyState.collectAsState()
    val todayRecord = attendance[todayKey]

    LaunchedEffect(todayKey, todayRecord) {
        DailyReminderScheduler.recordTodayAttendanceStatus(context, todayKey, todayRecord != null)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Clean Header (No subtitle, no cloud sync badge, no site dropdown)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(6.dp, CircleShape, spotColor = palette.accent.copy(alpha = 0.25f))
                            .background(
                                brush = Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📋", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "हाजिरी",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                }

                val displayName = currentUser?.name?.trim().orEmpty()
                if (displayName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.glassCardHigh,
                        border = BorderStroke(1.dp, palette.glassBorderMid),
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(50), spotColor = Color(0x1A000000))
                            .clickable { viewModel.setTab(HajiriTab.SETTINGS) }
                            .testTag("home_profile_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "👤", fontSize = 12.sp)
                            Text(
                                text = displayName,
                                fontSize = 12.sp,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        val availableUpdate = (updateState as? UpdateCheckState.UpdateAvailable)?.releaseInfo
        if (availableUpdate != null) {
            item {
                GlassCard(
                    borderColor = StatusGreen.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clickable { viewModel.showUpdateDialog.value = true }
                        .testTag("home_update_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎉 नया ऐप अपडेट (${availableUpdate.latestTag})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreen
                            )
                            Text(
                                text = "1 क्लिक में नया वर्ज़न प्राप्त करें",
                                fontSize = 11.sp,
                                color = palette.textSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StatusGreen
                        ) {
                            Text(
                                text = "अपडेट ⬇️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Embedded Interactive Attendance Calendar (Mark attendance directly from Home)
        item {
            CalendarAttendanceCard(viewModel = viewModel)
        }

        // 3. Stats Row (3 Equal-Width Liquid Frosted Pods: हाजिरी, गैरहाजिरी, आधा दिन)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("हाजिरी", stats.presentDays.toString(), StatusGreen, StatusGreenBg, Modifier.weight(1f))
                StatCard("गैरहाजिरी", stats.absentDays.toString(), StatusRed, StatusRedBg, Modifier.weight(1f))
                StatCard("आधा दिन", stats.halfDays.toString(), StatusYellow, StatusYellowBg, Modifier.weight(1f))
            }
        }

        // 4. Salary Card (Apple Finance Liquid Glass)
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💰 ", fontSize = 16.sp)
                        Text(
                            text = "इस महीने की कमाई",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Top Hero Row: Total Earnings (Left) vs Net Take-Home / हाथ में आएगा (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "कुल कमाई",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${DateUtils.formatInr(stats.baseEarnings + stats.otEarnings)}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "हाथ में आएगा",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${DateUtils.formatInr(stats.netTakeHome)}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Advance Deduction & Optional Carry Forward near the middle divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "एडवांस कटेगा",
                            fontSize = 13.sp,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "-₹${DateUtils.formatInr(stats.totalAdvance)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdvancePink
                        )
                    }

                    if (stats.carryForwardAdded > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💼 पिछला बाकी (${stats.previousMonthTitle})",
                                fontSize = 12.sp,
                                color = palette.textMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+₹${DateUtils.formatInr(stats.carryForwardAdded)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OtOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val daysInCurrentMonth = remember(todayMonthKey) {
                        runCatching { YearMonth.parse(todayMonthKey).lengthOfMonth() }
                            .getOrDefault(YearMonth.now().lengthOfMonth())
                    }
                    val effectiveWorkDays = if (activeSite.workDays in 1..31 && activeSite.workDays != 26) {
                        activeSite.workDays
                    } else {
                        daysInCurrentMonth
                    }
                    val monthlyTarget = if (wageMode == WageMode.DAILY) {
                        activeSite.dailyWage * effectiveWorkDays
                    } else {
                        activeSite.salary
                    }
                    val progress = if (monthlyTarget > 0.0) {
                        ((stats.baseEarnings + stats.otEarnings) / monthlyTarget).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = StatusGreenBright,
                        trackColor = palette.progressTrackBg
                    )
                }
            }
        }

        // 4B. Previous Month Remaining Balance Card ("पिछले महीने का बाकी" - Cumulative & Partial)
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💼 पिछले महीने का बाकी",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = when {
                                    stats.previousMonthsPendingCount > 1 ->
                                        "${stats.previousMonthsPendingCount} महीनों का संचयी बकाया जुड़ा है"
                                    stats.previousMonthTitle.isNotBlank() ->
                                        "${stats.previousMonthTitle} का बचा हिसाब"
                                    else -> "पिछले महीने का बचा हिसाब"
                                },
                                fontSize = 11.sp,
                                color = palette.textMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clickable { viewModel.showPrevBalanceDialog.value = true }
                                .testTag("open_prev_balance_home_btn")
                        ) {
                            Text(
                                text = "सभी महीने ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val displayPending = maxOf(stats.carryForwardAdded, stats.previousMonthRawBalance)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (displayPending > 0.0) {
                                    "₹${DateUtils.formatInr(displayPending)}"
                                } else {
                                    "₹0"
                                },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    displayPending <= 0.0 && stats.isPreviousMonthReceived -> StatusGreen
                                    displayPending <= 0.0 -> palette.textMuted
                                    else -> OtOrange
                                }
                            )
                            if (stats.previousMonthPaidAmount > 0.0 && !stats.isPreviousMonthReceived) {
                                Text(
                                    text = "मिला: ₹${DateUtils.formatInr(stats.previousMonthPaidAmount)} • बाकी: ₹${DateUtils.formatInr(stats.previousMonthRawBalance)}",
                                    fontSize = 10.sp,
                                    color = palette.accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = when {
                                displayPending <= 0.01 && stats.previousMonthPaidAmount > 0.0 -> "✅ पूरा ले लिया गया"
                                displayPending <= 0.01 -> "कोई बाकी नहीं"
                                else -> "➕ कुल सैलरी में जुड़ा है"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                displayPending <= 0.01 && stats.previousMonthPaidAmount > 0.0 -> StatusGreen
                                displayPending <= 0.01 -> palette.textMuted
                                else -> OtOrange
                            }
                        )
                    }

                    if ((stats.previousMonthRawBalance > 0.0 || stats.previousMonthPaidAmount > 0.0) && stats.previousMonthKey.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val unpaidActive = !stats.isPreviousMonthReceived && stats.previousMonthPaidAmount <= 0.0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (unpaidActive) OtOrange.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                border = BorderStroke(
                                    width = if (unpaidActive) 1.5.dp else 1.dp,
                                    color = if (unpaidActive) OtOrange else palette.glassBorderMid
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.toggleMonthSettled(stats.previousMonthKey, false)
                                    }
                                    .testTag("prev_month_unpaid_btn")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
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

                            val partialActive = !stats.isPreviousMonthReceived && stats.previousMonthPaidAmount > 0.0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (partialActive) palette.accent.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                border = BorderStroke(
                                    width = if (partialActive) 1.5.dp else 1.dp,
                                    color = if (partialActive) palette.accent else palette.glassBorderMid
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.showPrevBalanceDialog.value = true }
                                    .testTag("prev_month_partial_btn")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💵 आंशिक",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (partialActive) palette.accent else palette.textSecondary
                                    )
                                }
                            }

                            val paidActive = stats.isPreviousMonthReceived
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (paidActive) StatusGreen.copy(alpha = 0.16f) else palette.segmentedTrackBg,
                                border = BorderStroke(
                                    width = if (paidActive) 1.5.dp else 1.dp,
                                    color = if (paidActive) StatusGreen else palette.glassBorderMid
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.toggleMonthSettled(stats.previousMonthKey, true)
                                    }
                                    .testTag("prev_month_paid_btn")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
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
                    }
                }
            }
        }

        // 5. Advance / Loan Card
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "💳 एडवांस / उधार", fontSize = 13.sp, color = palette.textSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${DateUtils.formatInr(stats.totalAdvance)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AdvancePink
                            )
                        }

                        LiquidGlassButton(
                            onClick = { viewModel.showAdvanceDialog.value = true },
                            containerColor = AdvancePink,
                            secondaryColor = Color(0xFF9D174D),
                            shape = RoundedCornerShape(14.dp),
                            minHeight = 44.dp,
                            modifier = Modifier.testTag("open_advance_modal_btn")
                        ) {
                            Text("+ एडवांस जोड़ें", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val monthAdvances = advances.filter { it.dateKey.startsWith(todayMonthKey) }
                    if (monthAdvances.isEmpty()) {
                        Text(
                            text = "इस महीने कोई एडवांस दर्ज नहीं है",
                            fontSize = 12.sp,
                            color = palette.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            monthAdvances.take(4).forEach { adv ->
                                AdvanceItemRow(
                                    advance = adv,
                                    onDelete = { viewModel.deleteAdvance(adv.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Recent History
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "हालिया हाजिरी",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "सभी देखें →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    modifier = Modifier
                        .clickable { viewModel.openHistoryScreen() }
                        .testTag("home_see_all_history_btn")
                )
            }
        }

        val recentEntries = attendance.entries
            .sortedByDescending { it.key }
            .take(7)

        if (recentEntries.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "ऊपर हाजिरी दर्ज करें",
                        color = palette.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        } else {
            items(recentEntries.size) { idx ->
                val entry = recentEntries[idx]
                val date = DateUtils.parseDateKey(entry.key)
                val ot = otHours[entry.key]

                val (dotCol, bgCol, textCol) = when (entry.value.status) {
                    AttendanceStatus.PRESENT -> Triple(StatusGreen, StatusGreenBg, StatusGreen)
                    AttendanceStatus.ABSENT -> Triple(StatusRed, StatusRedBg, StatusRed)
                    AttendanceStatus.HALF -> Triple(StatusYellow, StatusYellowBg, StatusYellow)
                    AttendanceStatus.LEAVE -> Triple(StatusBlue, StatusBlueBg, StatusBlue)
                }

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(dotCol, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (date != null) DateUtils.formatHindiDate(date) else entry.key,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            val dayText = if (date != null) {
                                val dName = DateUtils.HINDI_DAYS[date.dayOfWeek.value - 1]
                                if (ot != null && ot > 0) "$dName • ${ot}h OT" else dName
                            } else ""
                            Text(
                                text = dayText,
                                fontSize = 11.sp,
                                color = palette.textSecondary
                            )
                            if (entry.value.note.isNotBlank()) {
                                Text(
                                    text = "📝 ${entry.value.note}",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = palette.textMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = bgCol
                        ) {
                            Text(
                                text = entry.value.status.labelHindi,
                                color = textCol,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}


@Composable
fun StatCard(
    label: String,
    count: String,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalHajiriPalette.current
    GlassCard(
        modifier = modifier,
        contentPadding = 10.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = palette.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AdvanceItemRow(
    advance: com.example.hajiri.model.AdvanceRecord,
    onDelete: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.glassCardHigh.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .border(1.dp, AdvancePink.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = advance.reason.ifBlank { "एडवांस" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            val d = DateUtils.parseDateKey(advance.dateKey)
            Text(
                text = if (d != null) "${d.dayOfMonth} ${DateUtils.HINDI_MONTHS[d.monthValue - 1]}" else advance.dateKey,
                fontSize = 10.sp,
                color = palette.textMuted
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "-₹${DateUtils.formatInr(advance.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AdvancePink
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Text(text = "🗑", fontSize = 16.sp)
            }
        }
    }
}
