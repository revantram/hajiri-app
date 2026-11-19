package com.example.hajiri.ui.theme

import androidx.compose.ui.graphics.Color

// Default iPhone Liquid Glass - Light Theme Palette tokens (High-Contrast & Frosted Glassmorphism)
val LightBackground = Color(0xFFEAF0FA)
val LiquidMeshTopLeft = Color(0xFFC7D7FE)
val LiquidMeshTopRight = Color(0xFFFBCFE8)
val LiquidMeshCenter = Color(0xFFDDD6FE)
val LiquidMeshBottom = Color(0xFFBAE6FD)

val LiquidGlassCard = Color(0xC8FFFFFF)
val LiquidGlassCardHigh = Color(0xE6FFFFFF)
val LiquidGlassCardSubtle = Color(0xADFFFFFF)

val LiquidGlassBorder = Color(0xD9FFFFFF)
val LiquidGlassBorderSubtle = Color(0x8CFFFFFF)
val LiquidGlassBorderActive = Color(0x664F46E5)

val AccentIndigo = Color(0xFF4338CA)
val AccentIndigoLight = Color(0xFF6366F1)
val AccentPurple = Color(0xFF6D28D9)
val AccentViolet = Color(0xFF9333EA)

// High-Contrast Status Colors for Crisp Legibility on Liquid Glass
val StatusGreen = Color(0xFF047857)
val StatusGreenBright = Color(0xFF059669)
val StatusGreenBg = Color(0xFFD1FAE5)

val StatusRed = Color(0xFFBE123C)
val StatusRedBright = Color(0xFFE11D48)
val StatusRedBg = Color(0xFFFFE4E6)

val StatusYellow = Color(0xFFB45309)
val StatusYellowBright = Color(0xFFD97706)
val StatusYellowBg = Color(0xFFFEF3C7)

val StatusBlue = Color(0xFF0369A1)
val StatusBlueBright = Color(0xFF0284C7)
val StatusBlueBg = Color(0xFFE0F2FE)

val OtOrange = Color(0xFFC2410C)
val OtOrangeBg = Color(0xFFFFEDD5)

val AdvancePink = Color(0xFFBE185D)
val AdvancePinkBg = Color(0xFFFCE7F3)

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
    glassBorderMid = Color(0xCCFFFFFF),
    glassBorderBottom = Color(0x4D4338CA),
    glassSheenTop = Color.White.copy(alpha = 0.65f),
    glassSheenBottom = Color.White.copy(alpha = 0.14f),
    accent = AccentIndigo,
    accentSecondary = AccentPurple,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    divider = Color(0x260F172A),
    fieldUnfocusedBorder = Color(0x4D0F172A),
    fieldFocusedBg = Color(0xADFFFFFF),
    fieldUnfocusedBg = Color(0x80FFFFFF),
    segmentedTrackBg = Color(0xB8E2E8F0),
    progressTrackBg = Color(0xFFD5DFEE)
)

val OceanGlassPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFE6F5FF),
    meshTopLeft = Color(0xFF93C5FD),
    meshTopRight = Color(0xFF99F6E4),
    meshCenter = Color(0xFFBAE6FD),
    meshBottom = Color(0xFFA5F3FC),
    glassCard = Color(0xC8FFFFFF),
    glassCardHigh = Color(0xE8FFFFFF),
    glassBorderTop = Color(0xFAFFFFFF),
    glassBorderMid = Color(0xCCFFFFFF),
    glassBorderBottom = Color(0x4D0369A1),
    glassSheenTop = Color.White.copy(alpha = 0.68f),
    glassSheenBottom = Color(0x2EE0F2FE),
    accent = Color(0xFF0369A1),
    accentSecondary = Color(0xFF0F766E),
    textPrimary = Color(0xFF041222),
    textSecondary = Color(0xFF12304A),
    textMuted = Color(0xFF284B68),
    divider = Color(0x260369A1),
    fieldUnfocusedBorder = Color(0x4D0369A1),
    fieldFocusedBg = Color(0xB3FFFFFF),
    fieldUnfocusedBg = Color(0x80FFFFFF),
    segmentedTrackBg = Color(0xB8CBE8F8),
    progressTrackBg = Color(0xFFC5E4F5)
)

val RoseGoldPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFFFEDF2),
    meshTopLeft = Color(0xFFFBCFE8),
    meshTopRight = Color(0xFFFED7AA),
    meshCenter = Color(0xFFFCE7F3),
    meshBottom = Color(0xFFFDE68A).copy(alpha = 0.65f),
    glassCard = Color(0xCCFFFFFF),
    glassCardHigh = Color(0xEBFFFFFF),
    glassBorderTop = Color(0xFAFFFFFF),
    glassBorderMid = Color(0xCCFFFFFF),
    glassBorderBottom = Color(0x4DBE185D),
    glassSheenTop = Color.White.copy(alpha = 0.68f),
    glassSheenBottom = Color(0x2EFCE7F3),
    accent = Color(0xFFBE185D),
    accentSecondary = Color(0xFFC2410C),
    textPrimary = Color(0xFF1A0712),
    textSecondary = Color(0xFF3E182B),
    textMuted = Color(0xFF5C2C43),
    divider = Color(0x26BE185D),
    fieldUnfocusedBorder = Color(0x4DBE185D),
    fieldFocusedBg = Color(0xB3FFFFFF),
    fieldUnfocusedBg = Color(0x80FFFFFF),
    segmentedTrackBg = Color(0xB8F8D0E2),
    progressTrackBg = Color(0xFFF3C6DA)
)

val LiquidDarkPalette = HajiriPalette(
    isDark = true,
    background = Color(0xFF060A17),
    meshTopLeft = Color(0xFF251E6B),
    meshTopRight = Color(0xFF421557),
    meshCenter = Color(0xFF111B33),
    meshBottom = Color(0xFF0E3454),
    glassCard = Color(0xB815203D),
    glassCardHigh = Color(0xDE1C294D),
    glassBorderTop = Color(0x73FFFFFF),
    glassBorderMid = Color(0x36FFFFFF),
    glassBorderBottom = Color(0x66818CF8),
    glassSheenTop = Color.White.copy(alpha = 0.14f),
    glassSheenBottom = Color.White.copy(alpha = 0.03f),
    accent = Color(0xFF818CF8),
    accentSecondary = Color(0xFFC084FC),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE2E8F0),
    textMuted = Color(0xFFCBD5E1),
    divider = Color(0x33FFFFFF),
    fieldUnfocusedBorder = Color(0x59FFFFFF),
    fieldFocusedBg = Color(0x4D0F172A),
    fieldUnfocusedBg = Color(0x330F172A),
    segmentedTrackBg = Color(0xCC1E293B),
    progressTrackBg = Color(0xFF24324A)
)
