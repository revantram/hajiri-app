package com.example.hajiri.ui.theme

import androidx.compose.ui.graphics.Color

// Default iPhone Liquid Glass - Light Theme Palette tokens (Translucent Frosted Glassmorphism + High-Contrast Text)
val LightBackground = Color(0xFFE4ECFA)
val LiquidMeshTopLeft = Color(0xFFA5B4FC)
val LiquidMeshTopRight = Color(0xFFF9A8D4)
val LiquidMeshCenter = Color(0xFFC4B5FD)
val LiquidMeshBottom = Color(0xFF7DD3FC)

val LiquidGlassCard = Color(0xADFFFFFF)
val LiquidGlassCardHigh = Color(0xD4FFFFFF)
val LiquidGlassCardSubtle = Color(0x8FFFFFFF)

val LiquidGlassBorder = Color(0xE6FFFFFF)
val LiquidGlassBorderSubtle = Color(0x99FFFFFF)
val LiquidGlassBorderActive = Color(0x6E4F46E5)

val AccentIndigo = Color(0xFF4338CA)
val AccentIndigoLight = Color(0xFF6366F1)
val AccentPurple = Color(0xFF6D28D9)
val AccentViolet = Color(0xFF9333EA)

// High-Contrast Status Colors for Crisp Legibility on Liquid Glass
val StatusGreen = Color(0xFF047857)
val StatusGreenBright = Color(0xFF059669)
val StatusGreenBg = Color(0xCCD1FAE5)

val StatusRed = Color(0xFFBE123C)
val StatusRedBright = Color(0xFFE11D48)
val StatusRedBg = Color(0xCCFFE4E6)

val StatusYellow = Color(0xFFB45309)
val StatusYellowBright = Color(0xFFD97706)
val StatusYellowBg = Color(0xCCFEF3C7)

val StatusBlue = Color(0xFF0369A1)
val StatusBlueBright = Color(0xFF0284C7)
val StatusBlueBg = Color(0xCCE0F2FE)

val OtOrange = Color(0xFFC2410C)
val OtOrangeBg = Color(0xCCFFEDD5)

val AdvancePink = Color(0xFFBE185D)
val AdvancePinkBg = Color(0xCCFCE7F3)

// High-Contrast Deep Text Tokens (Dark & Razor-Sharp)
val TextPrimary = Color(0xFF050A14)
val TextSecondary = Color(0xFF1E293B)
val TextMuted = Color(0xFF334155)
val TextLight = Color(0xFFFFFFFF)

data class HajiriPalette(
    val isDark: Boolean,
    val background: Color,
    val meshTopLeft: Color,
    val meshTopRight: Color,
    val meshCenter: Color,
    val meshBottom: Color,
    val glassCard: Color,
    val glassCardHigh: Color,
    val glassBorderTop: Color,
    val glassBorderMid: Color,
    val glassBorderBottom: Color,
    val glassSheenTop: Color,
    val glassSheenBottom: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val fieldUnfocusedBorder: Color,
    val fieldFocusedBg: Color,
    val fieldUnfocusedBg: Color,
    val segmentedTrackBg: Color,
    val progressTrackBg: Color
)

val LiquidLightPalette = HajiriPalette(
    isDark = false,
    background = LightBackground,
    meshTopLeft = LiquidMeshTopLeft,
    meshTopRight = LiquidMeshTopRight,
    meshCenter = LiquidMeshCenter,
    meshBottom = LiquidMeshBottom,
    glassCard = LiquidGlassCard,
    glassCardHigh = LiquidGlassCardHigh,
    glassBorderTop = Color(0xFAFFFFFF),
    glassBorderMid = Color(0xC2FFFFFF),
    glassBorderBottom = Color(0x524338CA),
    glassSheenTop = Color.White.copy(alpha = 0.72f),
    glassSheenBottom = Color.White.copy(alpha = 0.16f),
    accent = AccentIndigo,
    accentSecondary = AccentPurple,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    divider = Color(0x260F172A),
    fieldUnfocusedBorder = Color(0x4D0F172A),
    fieldFocusedBg = Color(0xB8FFFFFF),
    fieldUnfocusedBg = Color(0x85FFFFFF),
    segmentedTrackBg = Color(0x99DCE6F4),
    progressTrackBg = Color(0xB3CBD8EB)
)

val OceanGlassPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFDEF2FF),
    meshTopLeft = Color(0xFF60A5FA),
    meshTopRight = Color(0xFF5EEAD4),
    meshCenter = Color(0xFF7DD3FC),
    meshBottom = Color(0xFF67E8F9),
    glassCard = Color(0xADFFFFFF),
    glassCardHigh = Color(0xD6FFFFFF),
    glassBorderTop = Color(0xFAFFFFFF),
    glassBorderMid = Color(0xC4FFFFFF),
    glassBorderBottom = Color(0x520369A1),
    glassSheenTop = Color.White.copy(alpha = 0.74f),
    glassSheenBottom = Color(0x33E0F2FE),
    accent = Color(0xFF0369A1),
    accentSecondary = Color(0xFF0F766E),
    textPrimary = Color(0xFF041222),
    textSecondary = Color(0xFF12304A),
    textMuted = Color(0xFF284B68),
    divider = Color(0x260369A1),
    fieldUnfocusedBorder = Color(0x4D0369A1),
    fieldFocusedBg = Color(0xBAFFFFFF),
    fieldUnfocusedBg = Color(0x88FFFFFF),
    segmentedTrackBg = Color(0x99CBE8F8),
    progressTrackBg = Color(0xB3BEDFF2)
)

val RoseGoldPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFFFE6EE),
    meshTopLeft = Color(0xFFF472B6),
    meshTopRight = Color(0xFFFDBA74),
    meshCenter = Color(0xFFF9A8D4),
    meshBottom = Color(0xFFFCD34D).copy(alpha = 0.72f),
    glassCard = Color(0xB3FFFFFF),
    glassCardHigh = Color(0xDBFFFFFF),
    glassBorderTop = Color(0xFAFFFFFF),
    glassBorderMid = Color(0xC7FFFFFF),
    glassBorderBottom = Color(0x52BE185D),
    glassSheenTop = Color.White.copy(alpha = 0.74f),
    glassSheenBottom = Color(0x33FCE7F3),
    accent = Color(0xFFBE185D),
    accentSecondary = Color(0xFFC2410C),
    textPrimary = Color(0xFF1A0712),
    textSecondary = Color(0xFF3E182B),
    textMuted = Color(0xFF5C2C43),
    divider = Color(0x26BE185D),
    fieldUnfocusedBorder = Color(0x4DBE185D),
    fieldFocusedBg = Color(0xBAFFFFFF),
    fieldUnfocusedBg = Color(0x88FFFFFF),
    segmentedTrackBg = Color(0x99F8D0E2),
    progressTrackBg = Color(0xB3F0BED4)
)

val LiquidDarkPalette = HajiriPalette(
    isDark = true,
    background = Color(0xFF050915),
    meshTopLeft = Color(0xFF3730A3),
    meshTopRight = Color(0xFF701A75),
    meshCenter = Color(0xFF1E3A8A),
    meshBottom = Color(0xFF0E7490),
    glassCard = Color(0x9C15203D),
    glassCardHigh = Color(0xC71C294D),
    glassBorderTop = Color(0x80FFFFFF),
    glassBorderMid = Color(0x3DFFFFFF),
    glassBorderBottom = Color(0x73818CF8),
    glassSheenTop = Color.White.copy(alpha = 0.18f),
    glassSheenBottom = Color.White.copy(alpha = 0.04f),
    accent = Color(0xFF818CF8),
    accentSecondary = Color(0xFFC084FC),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE2E8F0),
    textMuted = Color(0xFFCBD5E1),
    divider = Color(0x36FFFFFF),
    fieldUnfocusedBorder = Color(0x5CFFFFFF),
    fieldFocusedBg = Color(0x570F172A),
    fieldUnfocusedBg = Color(0x3B0F172A),
    segmentedTrackBg = Color(0xA61E293B),
    progressTrackBg = Color(0xB324324A)
)
