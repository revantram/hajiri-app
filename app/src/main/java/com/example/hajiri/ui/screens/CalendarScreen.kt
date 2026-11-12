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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.GlassBorder
import com.example.hajiri.ui.theme.GlassCard
import com.example.hajiri.ui.theme.StatusBlue
import com.example.hajiri.ui.theme.StatusBlueBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.StatusYellowBg
import com.example.hajiri.ui.theme.TextMuted
import com.example.hajiri.ui.theme.TextPrimary
import com.example.hajiri.ui.theme.TextSecondary
import com.example.hajiri.util.DateUtils
import com.example.hajiri.viewmodel.HajiriViewModel
import java.time.LocalDate

@Composable
fun CalendarScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val todayKey = viewModel.todayKey

    val year = calendarMonth.year
    val month = calendarMonth.monthValue
    val daysInMonth = calendarMonth.lengthOfMonth()
    val firstDayOfWeek = calendarMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ...

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅 ", fontSize = 24.sp)
                Text(
                    text = "कैलेंडर",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = GlassCard,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Text(
                    text = DateUtils.formatHindiMonthYear(calendarMonth),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        GlassCard {
            Column {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.formatHindiMonthYear(calendarMonth),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = GlassCard,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { viewModel.changeCalendarMonth(-1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "‹", fontSize = 20.sp, color = TextPrimary)
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = GlassCard,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { viewModel.changeCalendarMonth(1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "›", fontSize = 20.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day-of-week header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    DateUtils.HINDI_DAYS_SHORT.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
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
                            .padding(vertical = 4.dp),
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
                                    onClick = { viewModel.toggleCalendarDay(dateKey) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(StatusGreen, "हाजिर")
                    LegendItem(StatusRed, "गैरहाजिर")
                    LegendItem(StatusYellow, "आधा")
                    LegendItem(StatusBlue, "छुट्टी")
                    LegendItem(AccentIndigo, "नोट")
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
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
    val (bgColor, textColor) = when (status) {
        AttendanceStatus.PRESENT -> Pair(StatusGreenBg, StatusGreen)
        AttendanceStatus.ABSENT -> Pair(StatusRedBg, StatusRed)
        AttendanceStatus.HALF -> Pair(StatusYellowBg, StatusYellow)
        AttendanceStatus.LEAVE -> Pair(StatusBlueBg, StatusBlue)
        null -> Pair(Color.Transparent, if (isSunday) StatusRed.copy(alpha = 0.6f) else TextPrimary)
    }

    Box(
        modifier = modifier
            .background(bgColor, CircleShape)
            .then(
                if (isToday) Modifier.border(2.dp, AccentIndigo, CircleShape)
                else Modifier
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
                fontWeight = if (isToday || status != null) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (hasNote) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(AccentIndigo, CircleShape)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}
