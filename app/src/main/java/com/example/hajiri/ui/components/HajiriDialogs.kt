package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.LiquidGlassBorder
import com.example.hajiri.ui.theme.LiquidGlassCardHigh
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.TextPrimary
import com.example.hajiri.ui.theme.TextSecondary

@Composable
fun SalaryDialog(
    initialSalary: Double,
    initialOtRate: Double,
    onDismiss: () -> Unit,
    onSave: (salary: Double, otRate: Double) -> Unit
) {
    var salaryText by remember { mutableStateOf(if (initialSalary > 0) initialSalary.toLong().toString() else "") }
    var otRateText by remember { mutableStateOf(if (initialOtRate > 0) initialOtRate.toLong().toString() else "50") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = LiquidGlassCardHigh),
            border = BorderStroke(1.2.dp, Color.White),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "💰 तनख्वाह सेटिंग",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("मासिक तनख्वाह (₹)") },
                    placeholder = { Text("जैसे: 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("salary_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = otRateText,
                    onValueChange = { otRateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ओवरटाइम दर (₹/घंटा)") },
                    placeholder = { Text("जैसे: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("ot_rate_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x330F172A))
                    ) {
                        Text("रद्द करें", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val sal = salaryText.toDoubleOrNull() ?: 0.0
                            val otr = otRateText.toDoubleOrNull() ?: 50.0
                            onSave(sal, otr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_salary_button")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkDaysDialog(
    initialWorkDays: Int,
    onDismiss: () -> Unit,
    onSave: (workDays: Int) -> Unit
) {
    var text by remember { mutableStateOf(initialWorkDays.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = LiquidGlassCardHigh),
            border = BorderStroke(1.2.dp, Color.White),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "📅 कार्यदिवस सेटिंग",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() } },
                    label = { Text("महीने में कुल काम के दिन") },
                    placeholder = { Text("जैसे: 26") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("workdays_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x330F172A))
                    ) {
                        Text("रद्द करें", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val wd = text.toIntOrNull() ?: 26
                            onSave(wd.coerceIn(1, 31))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_workdays_button")
                    ) {
                        Text("💾 सेव करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAdvanceDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, reason: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = LiquidGlassCardHigh),
            border = BorderStroke(1.2.dp, Color.White),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "💳 एडवांस / उधार जोड़ें",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("रकम (₹)") },
                    placeholder = { Text("जैसे: 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("advance_amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("कारण / विवरण") },
                    placeholder = { Text("जैसे: घर के लिए, दवाई, किराना...") },
                    modifier = Modifier.fillMaxWidth().testTag("advance_reason_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x330F172A))
                    ) {
                        Text("रद्द करें", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSave(amt, reasonText.ifBlank { "एडवांस" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_advance_button")
                    ) {
                        Text("💾 जोड़ें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String = "मिटाएं",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = LiquidGlassCardHigh),
            border = BorderStroke(1.2.dp, Color.White),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x24312E81))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x330F172A))
                    ) {
                        Text("रद्द करें", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentIndigo,
    unfocusedBorderColor = Color(0x2B0F172A),
    focusedLabelColor = AccentIndigo,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentIndigo,
    focusedContainerColor = Color(0x66FFFFFF),
    unfocusedContainerColor = Color(0x40FFFFFF)
)
