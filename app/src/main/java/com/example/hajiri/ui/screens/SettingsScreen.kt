package com.example.hajiri.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.AdvancePinkBg
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import com.example.hajiri.ui.theme.TextMuted
import com.example.hajiri.ui.theme.TextPrimary
import com.example.hajiri.ui.theme.TextSecondary
import com.example.hajiri.util.DateUtils
import com.example.hajiri.viewmodel.HajiriViewModel

@Composable
fun SettingsScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚙️ ", fontSize = 24.sp)
            Text(
                text = "सेटिंग्स",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        // Settings group (Apple Settings Group style)
        GlassCard(contentPadding = 0.dp) {
            Column {
                SettingsOptionRow(
                    iconText = "💰",
                    iconBg = AccentIndigo.copy(alpha = 0.12f),
                    title = "तनख्वाह सेटिंग",
                    subtitle = "मासिक तनख्वाह, OT दर",
                    valueText = if (settings.salary > 0) "₹${DateUtils.formatInr(settings.salary)}" else "—",
                    onClick = { viewModel.showSalaryDialog.value = true },
                    modifier = Modifier.testTag("open_salary_settings_btn")
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                SettingsOptionRow(
                    iconText = "📅",
                    iconBg = StatusGreenBg,
                    title = "काम के दिन",
                    subtitle = "महीने में कुल कार्यदिवस",
                    valueText = "${settings.workDays} दिन",
                    onClick = { viewModel.showWorkDaysDialog.value = true },
                    modifier = Modifier.testTag("open_workdays_settings_btn")
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                SettingsOptionRow(
                    iconText = "🔄",
                    iconBg = AdvancePinkBg,
                    title = if (currentUser != null) "Account Switch / Logout" else "अकाउंट लॉगिन / रजिस्टर",
                    subtitle = currentUser?.name ?: "वर्तमान में ऑफलाइन मोड",
                    valueText = if (currentUser != null) "लॉगिन है" else "ऑफलाइन",
                    onClick = { viewModel.showAuth(true) },
                    modifier = Modifier.testTag("open_auth_settings_btn")
                )
            }
        }

        // Danger zone
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "⚠️ खतरनाक ज़ोन",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Button(
                onClick = { viewModel.showClearMonthDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = StatusRedBg),
                border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("clear_month_data_btn")
            ) {
                Text(
                    text = "🗑️ इस महीने का डेटा मिटाएं",
                    color = StatusRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.showClearAllDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = StatusRedBg),
                border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("clear_all_data_btn")
            ) {
                Text(
                    text = "⛔ सारा डेटा मिटाएं",
                    color = StatusRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun SettingsOptionRow(
    iconText: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconText, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextMuted)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = valueText, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "›", fontSize = 20.sp, color = TextMuted)
        }
    }
}
