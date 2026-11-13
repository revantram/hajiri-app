package com.example.hajiri.ui.screens

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import java.time.YearMonth

@Composable
fun HistoryScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedHistoryMonth.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val otHours by viewModel.otHours.collectAsState()
    val palette = LocalHajiriPalette.current

    val monthEntries = attendance.entries
        .filter { it.key.startsWith(selectedMonth) }
        .sortedByDescending { it.key }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📜 ", fontSize = 24.sp)
                    Text(
                        text = "इतिहास",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                }
            }
        }

        // Horizontal Month Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val now = YearMonth.now()
                for (i in 5 downTo 0) {
                    val ym = now.minusMonths(i.toLong())
                    val mKey = ym.toString()
                    val isSelected = mKey == selectedMonth
                    val label = DateUtils.formatHindiMonthYear(ym)

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) palette.accent else palette.glassCardHigh,
                        border = BorderStroke(1.dp, if (isSelected) palette.accent else palette.glassBorderMid),
                        modifier = Modifier
                            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(50), spotColor = palette.accent.copy(alpha = 0.18f))
                            .clickable { viewModel.setHistoryMonth(mKey) }
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

        if (monthEntries.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "इस महीने कोई रिकॉर्ड नहीं",
                        color = palette.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(monthEntries.size) { idx ->
                val entry = monthEntries[idx]
                val date = DateUtils.parseDateKey(entry.key)
                val ot = otHours[entry.key]

                val (dotCol, bgCol, textCol) = when (entry.value.status) {
                    AttendanceStatus.PRESENT -> Triple(StatusGreen, StatusGreenBg, StatusGreen)
                    AttendanceStatus.ABSENT -> Triple(StatusRed, StatusRedBg, StatusRed)
                    AttendanceStatus.HALF -> Triple(StatusYellow, StatusYellowBg, StatusYellow)
                    AttendanceStatus.LEAVE -> Triple(StatusBlue, StatusBlueBg, StatusBlue)
                }

                GlassCard(
                    modifier = Modifier.clickable {
                        viewModel.toggleCalendarDay(entry.key)
                    }
                ) {
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
