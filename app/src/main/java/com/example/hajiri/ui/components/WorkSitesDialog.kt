package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.model.WageMode
import com.example.hajiri.model.WorkSite
import com.example.hajiri.ui.theme.LocalHajiriPalette
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.util.DateUtils
import java.util.UUID

@Composable
fun WorkSitesDialog(
    sites: List<WorkSite>,
    activeSiteId: String,
    onSwitchSite: (String) -> Unit,
    onSaveSite: (WorkSite) -> Unit,
    onDeleteSite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHajiriPalette.current
    var showForm by remember { mutableStateOf(false) }
    var editingSiteId by remember { mutableStateOf<String?>(null) }
    var siteNameText by remember { mutableStateOf("") }
    var wageMode by remember { mutableStateOf(WageMode.DAILY) }
    var salaryText by remember { mutableStateOf("") }
    var dailyWageText by remember { mutableStateOf("") }
    var otRateText by remember { mutableStateOf("50") }
    var workDaysText by remember { mutableStateOf("26") }

    fun openAddForm() {
        editingSiteId = null
        siteNameText = ""
        wageMode = WageMode.DAILY
        salaryText = ""
        dailyWageText = ""
        otRateText = "50"
        workDaysText = "26"
        showForm = true
    }

    fun openEditForm(site: WorkSite) {
        editingSiteId = site.id
        siteNameText = site.name
        wageMode = WageMode.fromId(site.wageMode)
        salaryText = if (site.salary > 0) site.salary.toLong().toString() else ""
        dailyWageText = if (site.dailyWage > 0) site.dailyWage.toLong().toString() else ""
        otRateText = if (site.otRate > 0) site.otRate.toLong().toString() else "50"
        workDaysText = site.workDays.coerceIn(1, 31).toString()
        showForm = true
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
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏗️ काम / साइट मैनेजर",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    if (!showForm) {
                        TextButton(
                            onClick = { openAddForm() },
                            modifier = Modifier.testTag("add_new_site_btn")
                        ) {
                            Text(
                                text = "+ नई साइट",
                                color = palette.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text(
                    text = "अलग-अलग ठेकेदार या साइट की दिहाड़ी/वेतन सेट कर 1-टैप में स्विच करें।",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!showForm) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sites.forEach { site ->
                            val isActive = site.id == activeSiteId
                            val siteMode = WageMode.fromId(site.wageMode)
                            val rateLabel = if (siteMode == WageMode.DAILY) {
                                "₹${DateUtils.formatInr(site.dailyWage)}/दिन"
                            } else {
                                "₹${DateUtils.formatInr(site.salary)}/माह"
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isActive) palette.accent.copy(alpha = 0.13f) else palette.glassCard,
                                border = BorderStroke(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) palette.accent else palette.glassBorderMid
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitchSite(site.id) }
                                    .testTag("site_item_${site.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isActive) "✅ ${site.name}" else "🏗️ ${site.name}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isActive) palette.accent else palette.textPrimary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isActive) StatusGreen.copy(alpha = 0.14f) else palette.segmentedTrackBg
                                        ) {
                                            Text(
                                                text = if (isActive) "सक्रिय" else "चुनें",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) StatusGreen else palette.textSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${siteMode.labelHindi}: $rateLabel • OT ₹${DateUtils.formatInr(site.otRate)}/h",
                                            fontSize = 11.sp,
                                            color = palette.textSecondary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "✏️ एडिट",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.accent,
                                                modifier = Modifier
                                                    .clickable { openEditForm(site) }
                                                    .padding(4.dp)
                                            )
                                            if (sites.size > 1) {
                                                Text(
                                                    text = "🗑️",
                                                    fontSize = 12.sp,
                                                    color = StatusRed,
                                                    modifier = Modifier
                                                        .clickable { onDeleteSite(site.id) }
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("close_sites_dialog_btn")
                        ) {
                            Text("ठीक है", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = siteNameText,
                            onValueChange = { siteNameText = it },
                            label = { Text("साइट / ठेकेदार का नाम") },
                            placeholder = { Text("जैसे: रामपुर साइट / शर्मा ठेकेदार") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("site_name_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.segmentedTrackBg, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WageMode.entries.forEach { mode ->
                                val selected = wageMode == mode
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (selected) palette.accent else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { wageMode = mode }
                                ) {
                                    Text(
                                        text = mode.labelHindi,
                                        color = if (selected) Color.White else palette.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                        }

                        if (wageMode == WageMode.DAILY) {
                            OutlinedTextField(
                                value = dailyWageText,
                                onValueChange = { dailyWageText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("दिन की दर (₹ / दिन)") },
                                placeholder = { Text("जैसे: 700") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("site_daily_wage_input")
                            )
                        } else {
                            OutlinedTextField(
                                value = salaryText,
                                onValueChange = { salaryText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("मासिक वेतन (₹ / माह)") },
                                placeholder = { Text("जैसे: 18000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("site_salary_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = otRateText,
                                onValueChange = { otRateText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("OT दर ₹/घंटा", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = workDaysText,
                                onValueChange = { workDaysText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("कार्यदिवस/माह", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = outlinedFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showForm = false },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.fieldUnfocusedBorder)
                        ) {
                            Text("वापस", color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val finalId = editingSiteId ?: "site_${UUID.randomUUID().toString().take(8)}"
                                val site = WorkSite(
                                    id = finalId,
                                    name = siteNameText.trim().ifBlank { "नई साइट" },
                                    wageMode = wageMode.id,
                                    salary = salaryText.toDoubleOrNull() ?: 0.0,
                                    dailyWage = dailyWageText.toDoubleOrNull() ?: 0.0,
                                    otRate = otRateText.toDoubleOrNull() ?: 50.0,
                                    workDays = (workDaysText.toIntOrNull() ?: 26).coerceIn(1, 31)
                                )
                                onSaveSite(site)
                                showForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("save_site_btn")
                        ) {
                            Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
