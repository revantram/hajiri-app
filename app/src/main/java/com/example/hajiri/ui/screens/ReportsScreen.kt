package com.example.hajiri.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.AccentPurple
import com.example.hajiri.ui.theme.AdvancePink
import com.example.hajiri.ui.theme.GlassBorder
import com.example.hajiri.ui.theme.GlassCard
import com.example.hajiri.ui.theme.OtOrange
import com.example.hajiri.ui.theme.StatusBlue
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.TextMuted
import com.example.hajiri.ui.theme.TextPrimary
import com.example.hajiri.ui.theme.TextSecondary
import com.example.hajiri.util.DateUtils
import com.example.hajiri.viewmodel.HajiriViewModel
import java.time.YearMonth

@Composable
fun ReportsScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedReportMonth.collectAsState()
    val stats by viewModel.reportStats.collectAsState()
    val trends by viewModel.sixMonthTrends.collectAsState()

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
                    color = TextPrimary
                )
            }
        }

        // Horizontal Month Chips (Last 6 Months)
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
                    color = if (isSelected) AccentIndigo else GlassCard,
                    border = BorderStroke(1.dp, if (isSelected) AccentIndigo else GlassBorder),
                    modifier = Modifier.clickable { viewModel.setReportMonth(mKey) }
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Attendance Percentage Ring Card
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "उपस्थिति %",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.attendancePercentage}%",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stats.workedDays} दिन काम किए",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Circular Progress Arc
                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            style = Stroke(width = 9.dp.toPx())
                        )
                        drawArc(
                            brush = Brush.linearGradient(listOf(AccentIndigo, StatusGreen)),
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
                            color = TextPrimary
                        )
                        Text(
                            text = "उपस्थिति",
                            fontSize = 9.sp,
                            color = TextMuted
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
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
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
                                fontSize = 9.sp,
                                color = if (trend.isSelected) AccentIndigo else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((75 * barHeightFactor).dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (trend.isSelected) {
                                                listOf(AccentIndigo, AccentPurple)
                                            } else {
                                                listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                                            }
                                        ),
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    trends.forEach { trend ->
                        Text(
                            text = trend.labelShort,
                            fontSize = 10.sp,
                            color = if (trend.isSelected) AccentIndigo else TextMuted,
                            fontWeight = if (trend.isSelected) FontWeight.Bold else FontWeight.Normal,
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
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BreakdownRow("✅ हाजिर दिन", "${stats.presentDays} दिन", StatusGreen)
                BreakdownRow("❌ गैरहाजिर", "${stats.absentDays} दिन", StatusRed)
                BreakdownRow("🌓 आधा दिन", "${stats.halfDays} दिन", StatusYellow)
                BreakdownRow("✈️ छुट्टी", "${stats.leaveDays} दिन", StatusBlue)
                BreakdownRow("⏰ ओवरटाइम", "${stats.totalOtHours} घंटे", OtOrange)
                BreakdownRow("💳 एडवांस", "-₹${DateUtils.formatInr(stats.totalAdvance)}", AdvancePink)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🤑 हाथ में आएगा",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "₹${DateUtils.formatInr(stats.netTakeHome)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusGreen
                    )
                }
            }
        }

        // Export & Share
        GlassCard {
            Column {
                Text(
                    text = "Export & Share",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        val reportText = viewModel.getReportText(selectedMonth)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Hajiri Report", reportText)
                        clipboard.setPrimaryClip(clip)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("copy_report_btn")
                ) {
                    Text("📄 Report Copy करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("share_whatsapp_btn")
                ) {
                    Text("💬 WhatsApp पर Share करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BreakdownRow(label: String, value: String, valueColor: Color) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
    }
}
