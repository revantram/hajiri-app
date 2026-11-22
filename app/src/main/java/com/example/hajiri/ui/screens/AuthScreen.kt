package com.example.hajiri.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.hajiri.data.attemptAutoSignIn
import com.example.hajiri.data.onGoogleSignInClicked
import com.example.hajiri.ui.components.GlassCard
import com.example.hajiri.ui.components.LiquidGlassAmbientBackdrop
import com.example.hajiri.ui.components.LiquidGlassButton
import com.example.hajiri.ui.theme.LocalHajiriPalette

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalHajiriPalette.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingSilent by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = coroutineScope
        )
    }

    LiquidGlassAmbientBackdrop(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                contentPadding = 26.dp
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(if (palette.isOled) 0.dp else 10.dp, CircleShape, spotColor = palette.accent.copy(alpha = 0.25f))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(palette.accent, palette.accentSecondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📋", fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "हाजिरी ऐप",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )

                Text(
                    text = "Firebase क्लाउड बैकअप के साथ सुरक्षित",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Cloud Benefits Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (palette.isOled) palette.statusGreenBg else palette.statusGreenBg.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, palette.statusGreen.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "☁️ कभी भी डेटा नहीं खोएगा:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.statusGreen
                        )
                        Text(
                            text = "• ऐप डिलीट होने या नया फोन लेने पर भी पूरी हाजिरी, OT और एडवांस वापस मिलेंगे।",
                            fontSize = 12.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "• आपका डेटा केवल आपके Google अकाउंट से सुरक्षित रहता है।",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.statusRedBg,
                        border = BorderStroke(1.dp, palette.statusRed.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = palette.statusRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                LiquidGlassButton(
                    onClick = {
                        errorMessage = null
                        isSigningIn = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isSigningIn = false
                            }
                        )
                    },
                    enabled = !isSigningIn && !isCheckingSilent,
                    containerColor = palette.accent,
                    secondaryColor = palette.accentSecondary,
                    shape = RoundedCornerShape(18.dp),
                    minHeight = 54.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_in_btn")
                ) {
                    if (isSigningIn || isCheckingSilent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCheckingSilent) "सिंक जांच हो रही है..." else "साइन इन हो रहा है...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.accent
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Google से साइन इन करें",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
