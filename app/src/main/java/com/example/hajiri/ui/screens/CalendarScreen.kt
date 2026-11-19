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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.StatusBlue
import com.example.hajiri.ui.theme.StatusBlueBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.StatusYellowBg
import com.example.hajiri.util.DateUtils
import com.example.hajiri.viewmodel.HajiriViewModel
import java.time.LocalDate

/**
 * Interactive Liquid Glass Attendance Calendar embedded directly on the Home screen
 * so users can mark or edit any date's attendance with a single tap.
 */
@Composable
fun CalendarAttendanceCard(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalHajiriPalette.current
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val todayKey by viewModel.todayKeyState.collectAsState()

    val year = calendarMonth.year
    val month = calendarMonth.monthValue
    val daysInMonth = calendarMonth.lengthOfMonth()
    val firstDayOfWeek = calendarMonth.atDay(1).dayOfWeek.value % 7

    GlassCard(modifier = modifier.testTag("home_calendar_card")) {
        Column {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📅 ${DateUtils.formatHindiMonthYear(calendarMonth)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "तारीख पर क्लिक करके सीधे P / A / HD और नोट लगाएं",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = palette.glassCardHigh,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.verticalGradient(listOf(palette.glassBorderTop, palette.fieldUnfocusedBorder))
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(5.dp, CircleShape, spotColor = palette.accent.copy(alpha = 0.2f))
                            .clickable { viewModel.changeCalendarMonth(-1) }
                            .testTag("cal_prev_month_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "‹", fontSize = 22.sp, color = palette.textPrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = palette.glassCardHigh,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.verticalGradient(listOf(palette.glassBorderTop, palette.fieldUnfocusedBorder))
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(5.dp, CircleShape, spotColor = palette.accent.copy(alpha = 0.2f))
                            .clickable { viewModel.changeCalendarMonth(1) }
                            .testTag("cal_next_month_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "›", fontSize = 22.sp, color = palette.textPrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day-of-week header row
            Row(modifier = Modifier.fillMaxWidth()) {
                DateUtils.HINDI_DAYS_SHORT.forEachIndexed { idx, dayName ->
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (idx == 0) StatusRed else palette.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid calculation
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0..6) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeek + 1

                        if (cellIndex < firstDayOfWeek || dayNum > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = LocalDate.of(year, month, dayNum)
                            val dateKey = DateUtils.toDateKey(date)
                            val record = attendance[dateKey]
                            val isToday = dateKey == todayKey
                            val isSunday = c == 0
                            val hasNote = record?.note?.isNotBlank() == true

                            CalendarDayCell(
                                day = dayNum,
                                status = record?.status,
                                isToday = isToday,
                                isSunday = isSunday,
                                hasNote = hasNote,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("cal_day_$dayNum"),
                                onClick = { viewModel.openCalendarDayDialog(dateKey) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(StatusGreen, "P हाजिर")
                LegendItem(StatusRed, "A गैरहाजिर")
                LegendItem(StatusYellow, "HD आधा")
                LegendItem(palette.accent, "नोट")
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    status: AttendanceStatus?,
    isToday: Boolean,
    isSunday: Boolean,
    hasNote: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    val (bgColor, textColor) = when (status) {
        AttendanceStatus.PRESENT -> Pair(StatusGreenBg, StatusGreen)
        AttendanceStatus.ABSENT -> Pair(StatusRedBg, StatusRed)
        AttendanceStatus.HALF -> Pair(StatusYellowBg, StatusYellow)
        AttendanceStatus.LEAVE -> Pair(StatusBlueBg, StatusBlue)
        null -> Pair(
            palette.glassCardHigh.copy(alpha = if (palette.isDark) 0.35f else 0.45f),
            if (isSunday) StatusRed else palette.textPrimary
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, CircleShape)
            .then(
                if (isToday) Modifier.border(2.2.dp, palette.accent, CircleShape)
                else Modifier.border(0.8.dp, palette.glassBorderMid.copy(alpha = 0.6f), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || status != null) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor
            )
            if (hasNote) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(palette.accent, CircleShape)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    val palette = LocalHajiriPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(text = label, fontSize = 11.sp, color = palette.textPrimary, fontWeight = FontWeight.Bold)
    }
}
