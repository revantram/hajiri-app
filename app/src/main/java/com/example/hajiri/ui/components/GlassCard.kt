package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hajiri.ui.theme.LocalHajiriPalette

/**
 * Full-screen iPhone Liquid Glass ambient iridescent backdrop featuring soft
 * blurred luminous orbs so every translucent card, dialog, and button refracts real depth.
 */
@Composable
fun LiquidGlassAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    if (palette.isOled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            content = content
        )
        return
    }

    val orbAlphaPrimary = if (palette.isDark) 0.42f else 0.52f
    val orbAlphaSecondary = if (palette.isDark) 0.36f else 0.46f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.meshTopLeft.copy(alpha = if (palette.isDark) 0.65f else 0.48f),
                        palette.background,
                        palette.meshCenter.copy(alpha = if (palette.isDark) 0.55f else 0.40f),
                        palette.meshBottom.copy(alpha = if (palette.isDark) 0.62f else 0.46f)
                    )
                )
            )
    ) {
        // Blurred ambient liquid-glass light orbs floating behind translucent surfaces
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-40).dp)
                .blur(76.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.meshTopLeft.copy(alpha = orbAlphaPrimary),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .blur(76.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.meshTopRight.copy(alpha = orbAlphaSecondary),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 70.dp)
                .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.meshBottom.copy(alpha = orbAlphaPrimary),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        content()
    }
}

/**
 * Premium iPhone-inspired Liquid Glass card with translucent frosted surface,
 * clipped blurred luminous frost sheen, specular top/bottom edge reflections, and ambient shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    val resolvedBg = backgroundColor ?: palette.glassCard
    val resolvedMidBorder = borderColor ?: palette.glassBorderMid
    val resolvedElevation = if (palette.isOled) 0.dp else elevation
    val borderWidth = if (palette.isOled) 1.dp else 1.35.dp
    val ambientShadow = if (palette.isDark) Color(0x5C000000) else Color(0x1E0F172A)
    val spotShadow = when {
        palette.isOled -> Color.Transparent
        palette.isDark -> Color(0x5C1E1B4B)
        else -> palette.accent.copy(alpha = 0.20f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (resolvedElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = resolvedElevation,
                        shape = shape,
                        ambientColor = ambientShadow,
                        spotColor = spotShadow
                    )
                } else Modifier
            ),
        shape = shape,
        color = resolvedBg,
        border = BorderStroke(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.glassBorderTop,
                    resolvedMidBorder,
                    palette.glassBorderBottom
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    // Crisp inner top specular highlight line (iPhone Liquid Glass edge reflection)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(
                                    alpha = when {
                                        palette.isOled -> 0.18f
                                        palette.isDark -> 0.36f
                                        else -> 0.90f
                                    }
                                ),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.06f, 1.dp.toPx()),
                        end = Offset(size.width * 0.94f, 1.dp.toPx()),
                        strokeWidth = if (palette.isOled) 1.dp.toPx() else 1.25.dp.toPx()
                    )
                    // Subtle lower rim glass refraction
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (palette.isOled) {
                                    Color.White.copy(alpha = 0.06f)
                                } else {
                                    palette.accent.copy(alpha = if (palette.isDark) 0.25f else 0.16f)
                                },
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.14f, size.height - 1.dp.toPx()),
                        end = Offset(size.width * 0.86f, size.height - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            // Frosted blurred refraction sheen layer behind crisp card content
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (palette.isOled) {
                                listOf(
                                    palette.glassSheenTop,
                                    palette.glassSheenBottom,
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    palette.glassSheenTop,
                                    palette.glassSheenBottom,
                                    Color.Transparent,
                                    palette.accent.copy(alpha = if (palette.isDark) 0.08f else 0.04f)
                                )
                            }
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Translucent frosted Liquid Glass container for modal dialogs across the app.
 */
@Composable
fun LiquidGlassDialogCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    GlassCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = palette.glassCardHigh,
        elevation = 20.dp,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Primary iPhone Liquid Glass action button with translucent frosted fill,
 * internal blurred glossy specular refraction, luminous border, and ambient elevation glow.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    secondaryColor: Color = containerColor.copy(alpha = 0.82f),
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    val activeAlpha = if (enabled) 1f else 0.52f
    val topColor = containerColor.copy(alpha = 0.88f * activeAlpha)
    val bottomColor = secondaryColor.copy(alpha = 0.82f * activeAlpha)

    Surface(
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f * activeAlpha),
                    Color.White.copy(alpha = 0.22f * activeAlpha)
                )
            )
        ),
        modifier = modifier
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = containerColor.copy(alpha = 0.28f),
                spotColor = containerColor.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                ),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.72f * activeAlpha),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.10f, 1.dp.toPx()),
                        end = Offset(size.width * 0.90f, 1.dp.toPx()),
                        strokeWidth = 1.1.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Blurred translucent liquid highlight behind sharp button label
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.36f * activeAlpha),
                            0.52f to Color.White.copy(alpha = 0.06f * activeAlpha),
                            1.0f to Color.Transparent
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .heightIn(min = minHeight)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Secondary frosted iPhone Liquid Glass button with translucent surface,
 * blurred specular sheen, and sharp luminous glass rim.
 */
@Composable
fun LiquidGlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    val resolvedBorder = borderColor ?: if (palette.isOled) palette.glassBorderMid else palette.accent.copy(alpha = 0.48f)

    Surface(
        shape = shape,
        color = palette.glassCard.copy(alpha = if (enabled) 0.74f else 0.45f),
        border = BorderStroke(
            width = if (palette.isOled) 1.dp else 1.25.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.glassBorderTop,
                    resolvedBorder
                )
            )
        ),
        modifier = modifier
            .then(
                if (enabled && !palette.isOled) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        ambientColor = Color(0x16000000),
                        spotColor = resolvedBorder.copy(alpha = 0.24f)
                    )
                } else Modifier
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(
                                    alpha = when {
                                        palette.isOled -> 0.18f
                                        palette.isDark -> 0.32f
                                        else -> 0.85f
                                    }
                                ),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.10f, 1.dp.toPx()),
                        end = Offset(size.width * 0.90f, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to palette.glassSheenTop,
                            0.55f to Color.Transparent
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .heightIn(min = minHeight)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
