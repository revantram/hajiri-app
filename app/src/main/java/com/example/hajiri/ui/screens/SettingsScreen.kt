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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.WageMode
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.theme.AdvancePinkBg
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.OtOrangeBg
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusGreenBg
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusRedBg
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.hajiri.util.AppUpdateChecker
import com.example.hajiri.util.DateUtils
import com.example.hajiri.util.UpdateCheckState
import com.example.hajiri.viewmodel.HajiriViewModel
import com.example.hajiri.viewmodel.SyncStatus
import com.example.hajiri.widget.HajiriWidgetManager

@Composable
fun SettingsScreen(
    viewModel: HajiriViewModel,
    googleEmail: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalHajiriPalette.current
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val autoUpdateEnabled by viewModel.autoUpdateCheckEnabled.collectAsState()
    val installedVersion by viewModel.installedVersionName.collectAsState()
    val updateRepoSlug by viewModel.updateRepoSlug.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val sitesList = viewModel.resolveSitesList(settings)
    val activeSite = viewModel.getActiveSite(settings)
    val activeWageMode = WageMode.fromId(activeSite.wageMode)
    val activeTheme = AppThemeStyle.fromId(settings.themeStyle)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminder(context, enabled = true, hourOfDay = reminderHour)
        } else {
            viewModel.showMessage("⚠️ रिमाइंडर के लिए नोटिफिकेशन अनुमति जरूरी है")
        }
    }

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
                color = palette.textPrimary
            )
        }

        // Cloud Sync Account Card
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name?.ifBlank { "हाजिरी यूजर" } ?: "हाजिरी यूजर",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        val subtitleEmail = currentUser?.email?.ifBlank { googleEmail } ?: googleEmail
                        if (subtitleEmail.isNotBlank()) {
                            Text(
                                text = subtitleEmail,
                                fontSize = 12.sp,
                                color = palette.textSecondary
                            )
                        }
                        val phone = currentUser?.phone.orEmpty()
                        if (phone.isNotBlank()) {
                            Text(
                                text = "📱 $phone",
                                fontSize = 12.sp,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val (badgeBg, badgeColor) = when (syncStatus) {
                        SyncStatus.OFFLINE_SAVED -> com.example.hajiri.ui.theme.OtOrangeBg to com.example.hajiri.ui.theme.OtOrange
                        SyncStatus.SYNCING_PENDING -> palette.accent.copy(alpha = 0.14f) to palette.accent
                        SyncStatus.SYNCED -> StatusGreenBg to StatusGreen
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(badgeColor, CircleShape)
                            )
                            Text(
                                text = when (syncStatus) {
                                    SyncStatus.OFFLINE_SAVED -> "ऑफ़लाइन सेव ✓"
                                    SyncStatus.SYNCING_PENDING -> "सिंक हो रहा..."
                                    SyncStatus.SYNCED -> "क्लाउड सिंक ✓"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }

                Text(
                    text = "आपकी सभी हाजिरी Firebase क्लाउड में सुरक्षित हैं। नया फोन लेने पर भी इसी Google अकाउंट से डेटा वापस मिलेगा।",
                    fontSize = 11.sp,
                    color = palette.textMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showProfileDialog.value = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_profile_btn")
                    ) {
                        Text(
                            text = "📱 नंबर/नाम बदलें",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                    }

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRedBg),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("logout_btn")
                    ) {
                        Text(
                            text = "🚪 लॉगआउट",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed
                        )
                    }
                }
            }
        }

        // One-Click Wage Mode Quick Switch Card
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "💼 वेतन गणना मोड (एक क्लिक से चुनें)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.segmentedTrackBg, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WageMode.entries.forEach { mode ->
                        val isSelected = activeWageMode == mode
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (isSelected) palette.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.updateSalarySettings(
                                        salary = settings.salary,
                                        dailyWage = settings.dailyWage,
                                        wageMode = mode,
                                        otRate = settings.otRate
                                    )
                                }
                                .testTag("settings_wagemode_${mode.id}_btn")
                        ) {
                            Text(
                                text = if (isSelected) "✓ ${mode.labelHindi}" else mode.labelHindi,
                                color = if (isSelected) Color.White else palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Settings group (Apple Settings Group style)
        GlassCard(contentPadding = 0.dp) {
            Column {
                val salarySummary = if (activeWageMode == WageMode.DAILY) {
                    if (settings.dailyWage > 0) "₹${DateUtils.formatInr(settings.dailyWage)}/दिन" else "सेट करें"
                } else {
                    if (settings.salary > 0) "₹${DateUtils.formatInr(settings.salary)}/माह" else "सेट करें"
                }

                SettingsOptionRow(
                    iconText = "💰",
                    iconBg = palette.accent.copy(alpha = 0.14f),
                    title = if (activeWageMode == WageMode.DAILY) "दिन की हाजिरी दर" else "मासिक वेतन सेटिंग",
                    subtitle = "${activeWageMode.labelHindi} • OT ₹${DateUtils.formatInr(settings.otRate)}/घंटा",
                    valueText = salarySummary,
                    onClick = { viewModel.showSalaryDialog.value = true },
                    modifier = Modifier.testTag("open_salary_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                val isAutoWorkDays = activeSite.workDays !in 1..31 || activeSite.workDays == 26
                SettingsOptionRow(
                    iconText = "📅",
                    iconBg = StatusGreenBg,
                    title = "काम के दिन",
                    subtitle = if (isAutoWorkDays) {
                        "महीने के पूरे दिन (28/29/30/31) अपने आप"
                    } else {
                        "महीने में कस्टम कार्यदिवस"
                    },
                    valueText = if (isAutoWorkDays) "ऑटो (30/31)" else "${activeSite.workDays} दिन",
                    onClick = { viewModel.showWorkDaysDialog.value = true },
                    modifier = Modifier.testTag("open_workdays_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                SettingsOptionRow(
                    iconText = "🎨",
                    iconBg = OtOrangeBg,
                    title = "ऐप थीम",
                    subtitle = "लाइट, ओशन ब्लू, रोज़ गोल्ड, डार्क",
                    valueText = "${activeTheme.emoji} ${activeTheme.labelHindi}",
                    onClick = { viewModel.showThemeDialog.value = true },
                    modifier = Modifier.testTag("open_theme_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                SettingsOptionRow(
                    iconText = "☁️",
                    iconBg = AdvancePinkBg,
                    title = "प्रोफ़ाइल व मोबाइल नंबर",
                    subtitle = currentUser?.phone?.ifBlank { "मोबाइल नंबर जोड़ें" } ?: "मोबाइल नंबर जोड़ें",
                    valueText = "एडिट",
                    onClick = { viewModel.showProfileDialog.value = true },
                    modifier = Modifier.testTag("open_profile_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                SettingsOptionRow(
                    iconText = "🏗️",
                    iconBg = palette.accent.copy(alpha = 0.14f),
                    title = "काम / साइट व अलग-अलग रेट",
                    subtitle = "कई ठेकेदार या साइट जोड़ें (${sitesList.size} साइट)",
                    valueText = activeSite.name.take(14),
                    onClick = { viewModel.showSitesDialog.value = true },
                    modifier = Modifier.testTag("open_sites_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                SettingsOptionRow(
                    iconText = "💼",
                    iconBg = OtOrangeBg,
                    title = "पिछले महीने के बाकी",
                    subtitle = "बकाया या आंशिक भुगतान (Partial Paid) दर्ज करें",
                    valueText = "देखें",
                    onClick = { viewModel.showPrevBalanceDialog.value = true },
                    modifier = Modifier.testTag("open_prev_balance_settings_btn")
                )

                HorizontalDivider(color = palette.divider)

                SettingsOptionRow(
                    iconText = "🧩",
                    iconBg = StatusGreenBg,
                    title = "होम स्क्रीन विजेट जोड़ें",
                    subtitle = "ऐप खोले बिना 1-टैप में P / A / HD लगाएं",
                    valueText = "पिन करें",
                    onClick = {
                        val requested = HajiriWidgetManager.requestPinWidget(context)
                        if (requested) {
                            viewModel.showMessage("🧩 होम स्क्रीन विजेट पिन करने का पॉपअप खोला गया")
                        } else {
                            viewModel.showMessage("📱 होम स्क्रीन दबाकर रखें → Widgets → हाजिरी जोड़ें")
                        }
                    },
                    modifier = Modifier.testTag("pin_widget_settings_btn")
                )
            }
        }

        // Daily Attendance Reminder Card
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 दैनिक हाजिरी रिमाइंडर",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "यदि आज की हाजिरी न लगी हो तो रोज़ याद दिलाएं",
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { wantEnable ->
                            if (!wantEnable) {
                                viewModel.setDailyReminder(context, enabled = false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setDailyReminder(context, enabled = true, hourOfDay = reminderHour)
                            }
                        },
                        modifier = Modifier.testTag("daily_reminder_switch")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.segmentedTrackBg, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(20 to "🌙 रात 8:00 बजे", 21 to "🌙 रात 9:00 बजे").forEach { (hour, label) ->
                        val selected = reminderHour == hour
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) palette.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (reminderEnabled) {
                                        viewModel.setDailyReminder(context, enabled = true, hourOfDay = hour)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        viewModel.setDailyReminder(context, enabled = false, hourOfDay = hour)
                                    } else {
                                        viewModel.setDailyReminder(context, enabled = true, hourOfDay = hour)
                                    }
                                }
                                .testTag("reminder_hour_${hour}_btn")
                        ) {
                            Text(
                                text = if (selected) "✓ $label" else label,
                                color = if (selected) Color.White else palette.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Auto Update Quick Card (Single unified section)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.showUpdateDialog.value = true }
                            .testTag("open_update_settings_btn")
                    ) {
                        Text(
                            text = "🔄 ऐप ऑटो अपडेट",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "वर्ज़न: v$installedVersion • GitHub: $updateRepoSlug (बदलें)",
                            fontSize = 11.sp,
                            color = palette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = autoUpdateEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setAutoUpdateCheckEnabled(context, enabled)
                        },
                        modifier = Modifier.testTag("auto_update_switch")
                    )
                }

                when (val st = updateState) {
                    is UpdateCheckState.UpdateAvailable -> {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(st.releaseInfo.downloadUrl)))
                                } catch (_: Exception) {
                                    viewModel.showMessage("⚠️ अपडेट लिंक खुल नहीं सका")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_one_click_update_btn")
                        ) {
                            Text(
                                text = "⬇️ नया अपडेट (${st.releaseInfo.latestTag}) — 1-Click डाउनलोड करें",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = { viewModel.checkForAppUpdate(context, userInitiated = true) },
                            enabled = st !is UpdateCheckState.Checking,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_update_btn")
                        ) {
                            Text(
                                text = if (st is UpdateCheckState.Checking) "⏳ अपडेट चेक हो रहा है..." else "🔄 1-Click अपडेट चेक करें",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val directUrl = AppUpdateChecker.buildLatestReleaseUrl(updateRepoSlug)
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)))
                                } catch (_: Exception) {
                                    viewModel.showMessage("⚠️ GitHub लिंक खुल नहीं सका")
                                }
                            },
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_github_direct_download_btn")
                        ) {
                            Text(
                                text = "🌐 GitHub से सीधा नया APK डाउनलोड करें",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Danger zone
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "⚠️ खतरनाक ज़ोन",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary,
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
    val palette = LocalHajiriPalette.current
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
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = palette.textMuted)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = valueText, fontSize = 13.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "›", fontSize = 20.sp, color = palette.textMuted)
        }
    }
}
