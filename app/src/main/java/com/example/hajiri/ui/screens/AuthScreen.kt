package com.example.hajiri.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.outlinedFieldColors
import com.example.hajiri.ui.theme.AccentIndigo
import com.example.hajiri.ui.theme.StatusGreen
import com.example.hajiri.ui.theme.StatusRed
import com.example.hajiri.ui.theme.StatusYellow
import com.example.hajiri.ui.theme.TextMuted
import com.example.hajiri.ui.theme.TextPrimary
import com.example.hajiri.ui.theme.TextSecondary
import com.example.hajiri.viewmodel.HajiriViewModel

@Composable
fun AuthScreen(
    viewModel: HajiriViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()

    var isRegisterTab by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var pinText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x73000000))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            contentPadding = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📋", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "हाजिरी ऐप",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "मोबाइल नंबर से Login करें — डेटा सुरक्षित रहेगा",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (currentUser != null) StatusGreen else StatusYellow, CircleShape)
                    )
                    Text(
                        text = if (currentUser != null) "लॉगिन है" else "लोकल स्टोरेज तैयार — Login करें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentUser != null) StatusGreen else StatusYellow
                    )
                }

                if (currentUser != null) {
                    Text(
                        text = "वर्तमान यूजर: ${currentUser?.name ?: ""} (${currentUser?.phone ?: ""})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("logout_btn")
                    ) {
                        Text("🚪 लॉगआउट करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { viewModel.showAuth(false) }) {
                        Text("बंद करें", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        TabButton(
                            title = "लॉगिन",
                            isSelected = !isRegisterTab,
                            modifier = Modifier.weight(1f),
                            onClick = { isRegisterTab = false }
                        )
                        TabButton(
                            title = "नया अकाउंट",
                            isSelected = isRegisterTab,
                            modifier = Modifier.weight(1f),
                            onClick = { isRegisterTab = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isRegisterTab) {
                        OutlinedTextField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = { Text("आपका नाम") },
                            placeholder = { Text("जैसे: Ramesh Kumar") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = outlinedFieldColors()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { if (it.length <= 10) phoneText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("मोबाइल नंबर") },
                        placeholder = { Text("10 अंक का नंबर") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("auth_phone_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) pinText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isRegisterTab) "नया PIN (4 अंक)" else "PIN (4 अंक)") },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("auth_pin_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isRegisterTab) {
                                viewModel.register(nameText, phoneText, pinText)
                            } else {
                                viewModel.login(phoneText, pinText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_btn")
                    ) {
                        Text(
                            text = if (isRegisterTab) "✅ अकाउंट बनाएं" else "🔓 लॉगिन करें",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🔌 बिना Login के उपयोग करें (डेटा सिर्फ इस फोन में)",
                        fontSize = 12.sp,
                        color = AccentIndigo,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.continueOffline() }
                            .padding(8.dp)
                            .testTag("auth_offline_btn")
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        modifier = modifier
            .then(
                if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(9.dp), spotColor = Color(0x1F000000))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 9.dp)
        )
    }
}
