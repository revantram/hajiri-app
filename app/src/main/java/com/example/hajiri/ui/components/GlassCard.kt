package com.example.hajiri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hajiri.ui.theme.LocalHajiriPalette

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 6.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalHajiriPalette.current
    val resolvedBg = backgroundColor ?: palette.glassCard
    val resolvedMidBorder = borderColor ?: palette.glassBorderMid

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x14312E81)
            ),
        shape = shape,
        color = resolvedBg,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
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
                        colors = listOf(
                            palette.glassSheenTop,
                            palette.glassSheenBottom
                        )
                    )
                )
                .padding(contentPadding),
            content = content
        )
    }
}
