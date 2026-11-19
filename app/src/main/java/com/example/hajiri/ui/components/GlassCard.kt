package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Premium iPhone-inspired Liquid Glass card with frosted translucency,
 * specular top-edge highlight, inner refraction sheen, and soft ambient shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 10.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    val resolvedBg = backgroundColor ?: palette.glassCard
    val resolvedMidBorder = borderColor ?: palette.glassBorderMid
    val ambientShadow = if (palette.isDark) Color(0x4D000000) else Color(0x1A0F172A)
    val spotShadow = if (palette.isDark) Color(0x521E1B4B) else palette.accent.copy(alpha = 0.18f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = ambientShadow,
                spotColor = spotShadow
            ),
        shape = shape,
        color = resolvedBg,
        border = BorderStroke(
            width = 1.35.dp,
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
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to palette.glassSheenTop,
                        0.38f to palette.glassSheenBottom,
                        1.0f to Color.Transparent
                    )
                )
                .drawWithContent {
                    drawContent()
                    // Crisp inner top specular highlight line (iPhone Liquid Glass edge reflection)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.30f else 0.85f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.08f, 1.dp.toPx()),
                        end = Offset(size.width * 0.92f, 1.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Primary iPhone Liquid Glass action button with luminous glossy top sheen,
 * refractive glass border, and soft colored elevation shadow.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    secondaryColor: Color = containerColor.copy(alpha = 0.86f),
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    val activeAlpha = if (enabled) 1f else 0.55f
    val topColor = containerColor.copy(alpha = activeAlpha)
    val bottomColor = secondaryColor.copy(alpha = activeAlpha)

    Surface(
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f * activeAlpha),
                    Color.White.copy(alpha = 0.14f * activeAlpha)
                )
            )
        ),
        modifier = modifier
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                ambientColor = containerColor.copy(alpha = 0.25f),
                spotColor = containerColor.copy(alpha = 0.42f)
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
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.28f),
                        0.48f to Color.White.copy(alpha = 0.04f),
                        1.0f to Color.Transparent
                    )
                )
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Secondary frosted iPhone Liquid Glass button with translucent surface and sharp specular rim.
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
    val resolvedBorder = borderColor ?: palette.accent.copy(alpha = 0.45f)

    Surface(
        shape = shape,
        color = palette.glassCardHigh.copy(alpha = if (enabled) 0.82f else 0.5f),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.glassBorderTop,
                    resolvedBorder
                )
            )
        ),
        modifier = modifier
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = shape,
                ambientColor = Color(0x14000000),
                spotColor = resolvedBorder.copy(alpha = 0.22f)
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to palette.glassSheenTop,
                        0.5f to Color.Transparent
                    )
                )
                .heightIn(min = minHeight)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
