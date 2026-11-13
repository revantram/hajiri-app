package com.example.hajiri.ui.theme

import androidx.compose.ui.graphics.Color

// Default iPhone 18 Liquid Glass - Light Theme Palette tokens
val LightBackground = Color(0xFFF3F5FA)
val LiquidMeshTopLeft = Color(0xFFE0E7FF)
val LiquidMeshTopRight = Color(0xFFFCE7F3)
val LiquidMeshCenter = Color(0xFFEDE9FE)
val LiquidMeshBottom = Color(0xFFE0F2FE)

val LiquidGlassCard = Color(0xE8FFFFFF)
val LiquidGlassCardHigh = Color(0xF5FFFFFF)
val LiquidGlassCardSubtle = Color(0xC7FFFFFF)

val LiquidGlassBorder = Color(0xB3FFFFFF)
val LiquidGlassBorderSubtle = Color(0x66FFFFFF)
val LiquidGlassBorderActive = Color(0x4D4F46E5)

val AccentIndigo = Color(0xFF4F46E5)
val AccentIndigoLight = Color(0xFF818CF8)
val AccentPurple = Color(0xFF7C3AED)
val AccentViolet = Color(0xFFA855F7)

// Status Colors optimized for Crisp Readability
val StatusGreen = Color(0xFF059669)
val StatusGreenBright = Color(0xFF10B981)
val StatusGreenBg = Color(0xFFD1FAE5)

val StatusRed = Color(0xFFE11D48)
val StatusRedBright = Color(0xFFF43F5E)
val StatusRedBg = Color(0xFFFFE4E6)

val StatusYellow = Color(0xFFD97706)
val StatusYellowBright = Color(0xFFF59E0B)
val StatusYellowBg = Color(0xFFFEF3C7)

val StatusBlue = Color(0xFF0284C7)
val StatusBlueBright = Color(0xFF0EA5E9)
val StatusBlueBg = Color(0xFFE0F2FE)

val OtOrange = Color(0xFFEA580C)
val OtOrangeBg = Color(0xFFFFEDD5)

val AdvancePink = Color(0xFFDB2777)
val AdvancePinkBg = Color(0xFFFCE7F3)

// Text tokens
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)
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
    glassBorderTop = Color.White,
    glassBorderMid = LiquidGlassBorder,
    glassBorderBottom = Color(0x334F46E5),
    glassSheenTop = Color.White.copy(alpha = 0.5f),
    glassSheenBottom = Color.White.copy(alpha = 0.15f),
    accent = AccentIndigo,
    accentSecondary = AccentPurple,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    divider = Color(0xFFF1F5F9),
    fieldUnfocusedBorder = Color(0x2B0F172A),
    fieldFocusedBg = Color(0x66FFFFFF),
    fieldUnfocusedBg = Color(0x40FFFFFF),
    segmentedTrackBg = Color(0xFFE2E8F0),
    progressTrackBg = Color(0xFFE2E8F0)
)

val OceanGlassPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFF0F9FF),
    meshTopLeft = Color(0xFFBAE6FD),
    meshTopRight = Color(0xFFCFFAFE),
    meshCenter = Color(0xFFE0F2FE),
    meshBottom = Color(0xFFCCFBF1),
    glassCard = Color(0xE8FFFFFF),
    glassCardHigh = Color(0xF7FFFFFF),
    glassBorderTop = Color.White,
    glassBorderMid = Color(0xB3FFFFFF),
    glassBorderBottom = Color(0x330284C7),
    glassSheenTop = Color.White.copy(alpha = 0.55f),
    glassSheenBottom = Color(0x26E0F2FE),
    accent = Color(0xFF0284C7),
    accentSecondary = Color(0xFF0D9488),
    textPrimary = Color(0xFF0C1E35),
    textSecondary = Color(0xFF335C7D),
    textMuted = Color(0xFF7CA0BE),
    divider = Color(0xFFE0F2FE),
    fieldUnfocusedBorder = Color(0x2B0284C7),
    fieldFocusedBg = Color(0x80FFFFFF),
    fieldUnfocusedBg = Color(0x4DFFFFFF),
    segmentedTrackBg = Color(0xFFD6EEF8),
    progressTrackBg = Color(0xFFD6EEF8)
)

val RoseGoldPalette = HajiriPalette(
    isDark = false,
    background = Color(0xFFFFF5F7),
    meshTopLeft = Color(0xFFFCE7F3),
    meshTopRight = Color(0xFFFFEDD5),
    meshCenter = Color(0xFFFDF2F8),
    meshBottom = Color(0xFFFDE68A).copy(alpha = 0.45f),
    glassCard = Color(0xEBFFFFFF),
    glassCardHigh = Color(0xF7FFFFFF),
    glassBorderTop = Color.White,
    glassBorderMid = Color(0xB3FFFFFF),
    glassBorderBottom = Color(0x33DB2777),
    glassSheenTop = Color.White.copy(alpha = 0.55f),
    glassSheenBottom = Color(0x26FCE7F3),
    accent = Color(0xFFDB2777),
    accentSecondary = Color(0xFFEA580C),
    textPrimary = Color(0xFF2D1220),
    textSecondary = Color(0xFF6B3F52),
    textMuted = Color(0xFFA7798E),
    divider = Color(0xFFFCE7F3),
    fieldUnfocusedBorder = Color(0x2BDB2777),
    fieldFocusedBg = Color(0x80FFFFFF),
    fieldUnfocusedBg = Color(0x4DFFFFFF),
    segmentedTrackBg = Color(0xFFF9DDE9),
    progressTrackBg = Color(0xFFF9DDE9)
)

val LiquidDarkPalette = HajiriPalette(
    isDark = true,
    background = Color(0xFF070B1A),
    meshTopLeft = Color(0xFF1E1B4B),
    meshTopRight = Color(0xFF31103F),
    meshCenter = Color(0xFF111827),
    meshBottom = Color(0xFF0F2942),
    glassCard = Color(0xCC131B33),
    glassCardHigh = Color(0xEB1A2442),
    glassBorderTop = Color(0x4DFFFFFF),
    glassBorderMid = Color(0x24FFFFFF),
    glassBorderBottom = Color(0x4D818CF8),
    glassSheenTop = Color.White.copy(alpha = 0.08f),
    glassSheenBottom = Color.White.copy(alpha = 0.02f),
    accent = Color(0xFF818CF8),
    accentSecondary = Color(0xFFA855F7),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    divider = Color(0x24FFFFFF),
    fieldUnfocusedBorder = Color(0x40FFFFFF),
    fieldFocusedBg = Color(0x330F172A),
    fieldUnfocusedBg = Color(0x220F172A),
    segmentedTrackBg = Color(0xFF1E293B),
    progressTrackBg = Color(0xFF1E293B)
)
