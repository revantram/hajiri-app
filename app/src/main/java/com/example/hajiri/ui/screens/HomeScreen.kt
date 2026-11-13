package com.example.hajiri.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.WageMode
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.outlinedFieldColors
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.AdvancePinkBg
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
import com.example.hajiri.viewmodel.SyncStatus
import java.time.LocalDate
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
    val streakPair by viewModel.streakPair.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val palette = LocalHajiriPalette.current
    val activeSite = viewModel.getActiveSite(settings)
    val wageMode = WageMode.fromId(activeSite.wageMode)

    val todayKey by viewModel.todayKeyState.collectAsState()
    val todayMonthKey by viewModel.todayMonthKeyState.collectAsState()
    val today = remember(todayKey) { DateUtils.parseDateKey(todayKey) ?: LocalDate.now() }
    val todayRecord = attendance[todayKey]
    val todayOt = otHours[todayKey] ?: 0f

    LaunchedEffect(todayKey, todayRecord) {
        DailyReminderScheduler.recordTodayAttendanceStatus(context, todayKey, todayRecord != null)
    }

    var otInputText by remember { mutableStateOf("") }
    var noteInputText by remember(todayKey, todayRecord?.note) { mutableStateOf(todayRecord?.note ?: "") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header: Apple Dynamic Island / Capsule Header style
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
                    Column {
                        Text(
                            text = "हाजिरी",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Daily Attendance & Salary",
                            fontSize = 11.sp,
                            color = palette.textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val pillDotColor = when (syncStatus) {
                    SyncStatus.OFFLINE_SAVED -> OtOrange
                    SyncStatus.SYNCING_PENDING -> palette.accent
                    SyncStatus.SYNCED -> StatusGreen
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.glassCardHigh,
                    border = BorderStroke(1.dp, pillDotColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(50), spotColor = Color(0x1A000000))
                        .clickable { viewModel.setTab(HajiriTab.SETTINGS) }
                        .testTag("home_cloud_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(pillDotColor, CircleShape)
                        )
                        Text(
                            text = currentUser?.name?.ifBlank { "क्लाउड सिंक" } ?: "क्लाउड सिंक",
                            fontSize = 12.sp,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Offline-First & Live Sync Status Strip + Active Site Switcher Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .weight(1f)
                        .testTag("offline_sync_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = syncIcon, fontSize = 11.sp)
                        Text(
                            text = syncStatus.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = syncFg,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.glassCardHigh,
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .clickable { viewModel.showSitesDialog.value = true }
                        .testTag("home_site_switcher_pill")
                ) {
                    Text(
                        text = "🏗️ ${activeSite.name.take(14)} ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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

        // 1. Today Card (iPhone 18 Liquid Glass widget)
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "आज की हाजिरी ◦ ${DateUtils.formatHindiDate(today)}",
                            fontSize = 12.sp,
                            color = palette.textMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier
                                .background(palette.accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateUtils.formatHindiDayAndDate(today),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2x2 marking buttons: iPhone 3D Liquid tactile buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AttendanceButton(
                            title = "हाजिर",
                            icon = "✅",
                            status = AttendanceStatus.PRESENT,
                            isSelected = todayRecord?.status == AttendanceStatus.PRESENT,
                            gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                            modifier = Modifier.weight(1f).testTag("mark_present_btn"),
                            onClick = { viewModel.markToday(AttendanceStatus.PRESENT) }
                        )
                        AttendanceButton(
                            title = "गैरहाजिर",
                            icon = "❌",
                            status = AttendanceStatus.ABSENT,
                            isSelected = todayRecord?.status == AttendanceStatus.ABSENT,
                            gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48)),
                            modifier = Modifier.weight(1f).testTag("mark_absent_btn"),
                            onClick = { viewModel.markToday(AttendanceStatus.ABSENT) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AttendanceButton(
                            title = "आधा दिन",
                            icon = "🌓",
                            status = AttendanceStatus.HALF,
                            isSelected = todayRecord?.status == AttendanceStatus.HALF,
                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                            modifier = Modifier.weight(1f).testTag("mark_half_btn"),
                            onClick = { viewModel.markToday(AttendanceStatus.HALF) }
                        )
                        AttendanceButton(
                            title = "छुट्टी",
                            icon = "✈️",
                            status = AttendanceStatus.LEAVE,
                            isSelected = todayRecord?.status == AttendanceStatus.LEAVE,
                            gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                            modifier = Modifier.weight(1f).testTag("mark_leave_btn"),
                            onClick = { viewModel.markToday(AttendanceStatus.LEAVE) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Capsule Pill
                    val (statusBg, statusTextColor) = when (todayRecord?.status) {
                        AttendanceStatus.PRESENT -> Pair(StatusGreenBg, StatusGreen)
                        AttendanceStatus.ABSENT -> Pair(StatusRedBg, StatusRed)
                        AttendanceStatus.HALF -> Pair(StatusYellowBg, StatusYellow)
                        AttendanceStatus.LEAVE -> Pair(StatusBlueBg, StatusBlue)
                        null -> Pair(palette.segmentedTrackBg, palette.textSecondary)
                    }

                    Row(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusTextColor, CircleShape)
                        )
                        Text(
                            text = if (todayRecord != null) "आज दर्ज स्थिति: ${todayRecord.status.labelHindi}" else "आज की हाजिरी अभी बाकी है",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }

                    // Daily Note Section
                    AnimatedVisibility(visible = todayRecord != null) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            OutlinedTextField(
                                value = noteInputText,
                                onValueChange = { noteInputText = it },
                                placeholder = { Text("आज का नोट लिखें (वैकल्पिक)...", color = palette.textMuted, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("today_note_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { viewModel.saveTodayNote(noteInputText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("save_note_btn")
                                ) {
                                    Text("नोट सेव करें ✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Streak Banner (Liquid Glass Flame Pod)
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(OtOrangeBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🔥", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${streakPair.first} दिन",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OtOrange
                            )
                            Text(
                                text = "लगातार हाजिरी स्ट्रीक",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${streakPair.second} दिन",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusYellow
                        )
                        Text(
                            text = "🏆 सबसे बड़ा स्ट्रीक",
                            fontSize = 10.sp,
                            color = palette.textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Stats Row (4 Liquid Frosted Pods)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("हाजिर", stats.presentDays.toString(), StatusGreen, StatusGreenBg, Modifier.weight(1f))
                StatCard("गैरहाजिर", stats.absentDays.toString(), StatusRed, StatusRedBg, Modifier.weight(1f))
                StatCard("आधा दिन", stats.halfDays.toString(), StatusYellow, StatusYellowBg, Modifier.weight(1f))
                StatCard("छुट्टी", stats.leaveDays.toString(), StatusBlue, StatusBlueBg, Modifier.weight(1f))
            }
        }

        // 4. Salary Card (Apple Finance Liquid Glass)
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💰 ", fontSize = 16.sp)
                            Text(
                                text = "इस महीने की कमाई",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable { viewModel.showSalaryDialog.value = true }
                        ) {
                            Text(
                                text = "⚙️ ${wageMode.labelHindi}",
                                fontSize = 11.sp,
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "₹${DateUtils.formatInr(stats.baseEarnings)}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusGreen
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "₹${DateUtils.formatInr(stats.perDayRate)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(text = "प्रति दिन", fontSize = 10.sp, color = palette.textMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "₹${DateUtils.formatInr(stats.otEarnings)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OtOrange)
                            Text(text = "ओवरटाइम", fontSize = 10.sp, color = palette.textMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "-₹${DateUtils.formatInr(stats.totalAdvance)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdvancePink)
                            Text(text = "एडवांस कटेगा", fontSize = 10.sp, color = palette.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

                    if (stats.carryForwardAdded > 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💼 पिछला बाकी (${stats.previousMonthTitle}): ",
                                fontSize = 11.sp,
                                color = palette.textMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+₹${DateUtils.formatInr(stats.carryForwardAdded)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OtOrange
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "हाथ में आएगा: ",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹${DateUtils.formatInr(stats.netTakeHome)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusGreen
                        )
                    }
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

        // 5. Overtime Card
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "⏰ ओवरटाइम", fontSize = 12.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${stats.totalOtHours} घंटे",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = OtOrange
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "OT कमाई", fontSize = 12.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${DateUtils.formatInr(stats.otEarnings)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OtOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = otInputText,
                            onValueChange = { otInputText = it },
                            placeholder = { Text("आज के OT घंटे (जैसे: 1.5)", fontSize = 13.sp, color = palette.textMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("ot_hours_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val hrs = otInputText.toFloatOrNull() ?: 0f
                                if (hrs > 0f) {
                                    viewModel.addTodayOvertime(hrs)
                                    otInputText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OtOrange),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("add_ot_btn")
                        ) {
                            Text("➕ जोड़ें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 6. Advance / Loan Card
        item {
            GlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "💳 एडवांस / उधार", fontSize = 12.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${DateUtils.formatInr(stats.totalAdvance)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdvancePink
                            )
                        }

                        Button(
                            onClick = { viewModel.showAdvanceDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AdvancePink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_advance_modal_btn")
                        ) {
                            Text("+ एडवांस जोड़ें", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.clickable { viewModel.setTab(HajiriTab.HISTORY) }
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
fun AttendanceButton(
    title: String,
    icon: String,
    status: AttendanceStatus,
    isSelected: Boolean,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        border = if (isSelected) BorderStroke(2.5.dp, Color.White) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = gradientColors.first().copy(alpha = 0.4f)
            )
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(16.dp)
            )
            .height(52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSelected) "$title ✓" else title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
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
